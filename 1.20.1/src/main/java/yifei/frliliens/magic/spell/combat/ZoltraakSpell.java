package yifei.frliliens.magic.spell.combat;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

import yifei.frliliens.magic.capability.PlayerDataHelper;
import yifei.frliliens.magic.config.Config;
import yifei.frliliens.magic.damage.ModDamageTypes;
import yifei.frliliens.magic.network.ModNetwork;
import yifei.frliliens.magic.spell.AbstractSpell;

/**
 * 攻击魔法 Zoltraak。
 *
 * <p>发射一道蓝白渐变激光，对路径上的所有敌人造成穿透伤害。
 * 射程随玩家视距变化；若已学习「长距离魔法」被动，射程进一步提升。
 *
 * <p>视觉效果是<b>三层尘埃粒子</b>构成的圆柱光柱（与主分支一致）：
 * 核心高密度亮白、柱壁环形蓝白、外晕大范围淡蓝。
 *
 * <h2>与主分支的唯一差异：粒子发包方式</h2>
 * 主分支用 {@code ServerLevel#sendParticles}，而 1.20.1 的该方法有个
 * <b>32 格剔除</b>——距离玩家超过 32 格的位置会被静默丢弃：
 *
 * <pre>{@code
 * if (blockpos.closerToCenterThan(new Vec3(x, y, z), p_8638_ ? 512.0D : 32.0D))
 * }</pre>
 *
 * 而本魔法的射程在学习「长距离魔法」后可达 200 格以上，于是 85% 的粒子
 * 根本到不了客户端（实测日志：{@code attempted=17291 accepted=1979}）。
 *
 * <p>改用 {@link ModNetwork#sendParticlePacket} 直接构造
 * {@code ClientboundLevelParticlesPacket}（{@code overrideLimiter = true}）
 * 发给同维度玩家——这个做法参照本仓库另一个模组
 * {@code F:\Work\Twelve Talismans} 的 {@code SmartParticleDispatcher}，
 * 它正是用 {@code overrideLimiter = true} 绕开该剔除，且实测有效。
 */
public class ZoltraakSpell extends AbstractSpell {

    public ZoltraakSpell() {
        super("zoltraak", 50);
    }

    @Override
    protected boolean doCast(Level level, Player caster) {
        // 计算射程：学习长距离魔法后射程 = 视距 * 倍率，倍率随魔力上限增强
        double range = Config.ZOLTRAAK_RANGE.get();
        boolean hasLongRange = PlayerDataHelper.learnedSpells(caster)
                .hasLearned("long_range");
        if (hasLongRange) {
            int renderDistance = level.getServer() != null
                    ? level.getServer().getPlayerList().getViewDistance()
                    : 10;
            int maxMana = PlayerDataHelper.mana(caster).max();
            // 基础倍率 + 每 manaPerStep 点魔力上限 +1 倍
            double baseMultiplier = Config.ZOLTRAAK_RANGE_MULTIPLIER.get();
            double manaPerStep = Math.max(1.0, Config.ZOLTRAAK_MANA_PER_MULTIPLIER.get());
            double multiplier = baseMultiplier + maxMana / manaPerStep;
            range = renderDistance * multiplier;
        }

        double damageRadius = Config.ZOLTRAAK_DAMAGE_RADIUS.get();
        float damage = Config.ZOLTRAAK_DAMAGE.get().floatValue();

        Vec3 eyePos = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        Vec3 endPos = eyePos.add(look.scale(range));

        // 穿透模式：不进行方块碰撞检测，激光穿透一切方块和实体
        double actualRange = range;

        // 对路径上的实体造成魔法伤害
        DamageSource source = ModDamageTypes.magic(caster);
        int hitCount = 0;
        Vec3 dir = look.normalize();
        for (Entity entity : level.getEntities(caster,
                new AABB(eyePos, endPos).inflate(damageRadius))) {
            if (!(entity instanceof LivingEntity living) || entity == caster) {
                continue;
            }
            // 点到射线的距离判断是否在激光路径上
            Vec3 toEntity = entity.position().add(0, entity.getBbHeight() / 2, 0).subtract(eyePos);
            double proj = toEntity.dot(dir);
            if (proj < 0 || proj > actualRange) {
                continue;
            }
            double perpDist = toEntity.subtract(dir.scale(proj)).length();
            if (perpDist <= damageRadius) {
                living.hurt(source, damage);
                hitCount++;
            }
        }

        // 渲染蓝白渐变激光（粒子）
        if (level instanceof ServerLevel serverLevel) {
            spawnLaser(serverLevel, eyePos, endPos, actualRange);
            serverLevel.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5F, 2.0F);
        }

        return true;
    }

    /** 光柱视觉半径（圆柱体）。纯视觉，不参与伤害判定。 */
    private static final double BEAM_RADIUS = 0.6;

    /** 沿光柱方向的采样密度（粒子批数/格）。与主分支一致。 */
    private static final double STEPS_PER_BLOCK = 8.0;

    // ===== LOD 降密度（参照 Twelve Talismans 的 SmartParticleDispatcher）=====
    //
    // 主分支的射程通常只有 30 格（240 步 ≈ 2400 次发包），可以承受；
    // 但学会「长距离魔法」后射程可达 200 格以上，按 8 步/格 会变成
    // 1700 步、1.7 万次发包，单次施放就能拖垮帧率。
    // 所以按距离降低密度：近处保持完整观感，远处只保留轮廓。

    /** 这个距离以内保持全密度（格）。 */
    private static final double LOD_NEAR = 10.0;
    /** 这个距离以内半密度（格）。 */
    private static final double LOD_MID = 30.0;

    /**
     * 沿射线生成圆柱体蓝白光柱。
     *
     * <p>由三层构成：
     * <ul>
     *   <li>核心：高密度亮白粒子，形成光柱实体感</li>
     *   <li>柱壁：环形分布的蓝白粒子，勾勒圆柱轮廓</li>
     *   <li>外晕：大范围低透明度蓝色粒子，营造光晕</li>
     * </ul>
     *
     * @param level  服务端世界
     * @param start  起点（玩家眼部）
     * @param end    终点
     * @param length 射线长度
     */
    private void spawnLaser(ServerLevel level, Vec3 start, Vec3 end, double length) {
        Vec3 dir = end.subtract(start).normalize();
        // 计算垂直于光柱方向的两个正交基向量，用于构建圆柱截面
        Vec3 up = Math.abs(dir.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = dir.cross(up).normalize();
        Vec3 orthoUp = dir.cross(right).normalize();

        // 沿光柱方向的采样步长
        int steps = Math.max(1, (int) (length * STEPS_PER_BLOCK));

        DustParticleOptions coreParticle = new DustParticleOptions(
                new Vector3f(0.95F, 0.97F, 1.0F), 2.0F);
        DustParticleOptions shellParticle = new DustParticleOptions(
                new Vector3f(0.5F, 0.7F, 1.0F), 1.2F);
        DustParticleOptions glowParticle = new DustParticleOptions(
                new Vector3f(0.3F, 0.5F, 1.0F), 0.7F);

        int sent = 0;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double distance = length * t;
            Vec3 center = start.add(dir.scale(distance));

            // LOD：按到玩家的距离决定这一批要不要发
            if (distance > LOD_MID && i % 3 != 0) {
                continue;       // 30 格以上：1/3 密度
            }
            if (distance > LOD_NEAR && i % 2 != 0) {
                continue;       // 10~30 格：半密度
            }

            // 核心：中心高密度亮白粒子
            ModNetwork.sendParticlePacket(level, coreParticle,
                    center.x, center.y, center.z, 3, 0.08F, 0.08F, 0.08F, 0.0F);
            sent += 3;

            // 柱壁：环形分布 6 个粒子，形成圆柱截面轮廓
            for (int a = 0; a < 6; a++) {
                double angle = (a / 6.0) * Math.PI * 2 + t * 3.0;
                Vec3 ringOffset = right.scale(Math.cos(angle) * BEAM_RADIUS)
                        .add(orthoUp.scale(Math.sin(angle) * BEAM_RADIUS));
                Vec3 ringPos = center.add(ringOffset);
                ModNetwork.sendParticlePacket(level, shellParticle,
                        ringPos.x, ringPos.y, ringPos.z, 1, 0.04F, 0.04F, 0.04F, 0.0F);
                sent++;
            }

            // 外晕：每 2 步生成一次大范围蓝色光晕粒子
            if (i % 2 == 0) {
                ModNetwork.sendParticlePacket(level, glowParticle,
                        center.x, center.y, center.z, 2,
                        (float) (BEAM_RADIUS * 1.2), (float) (BEAM_RADIUS * 1.2),
                        (float) (BEAM_RADIUS * 1.2), 0.0F);
                sent += 2;
            }
        }

        yifei.frliliens.magic.FriliensMagic.LOGGER.debug(
                "[zoltraak] laser: range={} steps={} particles sent={}",
                String.format("%.1f", length), steps, sent);
    }
}
