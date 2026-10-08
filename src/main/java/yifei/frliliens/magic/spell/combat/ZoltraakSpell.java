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

import yifei.frliliens.magic.attachment.ModAttachments;
import yifei.frliliens.magic.damage.ModDamageTypes;
import yifei.frliliens.magic.spell.AbstractSpell;

/**
 * 攻击魔法 Zoltraak。
 *
 * <p>发射一道蓝白渐变激光，对路径上的所有敌人造成穿透伤害。
 * 射程随玩家视距变化；若已学习「长距离魔法」被动，射程进一步提升。
 */
public class ZoltraakSpell extends AbstractSpell {

    /** 基础射程（未学习长距离魔法时）。 */
    private static final double BASE_RANGE = 30.0;

    /** 激光伤害（无视护甲，穿透一切）。 */
    private static final float DAMAGE = 520.0F;

    public ZoltraakSpell() {
        super("zoltraak", 50);
    }

    @Override
    protected boolean doCast(Level level, Player caster) {
        // 计算射程：学习长距离魔法后射程 = 视距 * 16，否则基础射程
        double range = BASE_RANGE;
        boolean hasLongRange = caster.getData(ModAttachments.LEARNED_SPELLS)
                .hasLearned("long_range");
        if (hasLongRange) {
            int renderDistance = level.getServer() != null
                    ? level.getServer().getPlayerList().getViewDistance()
                    : 10;
            range = renderDistance * 16.0;
        }

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
                new AABB(eyePos, endPos).inflate(DAMAGE_RADIUS))) {
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
            if (perpDist <= DAMAGE_RADIUS) {
                living.hurt(source, DAMAGE);
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

    /** 光柱视觉半径（圆柱体）。 */
    private static final double BEAM_RADIUS = 0.6;

    /** 伤害判定半径（路径上此距离内的实体均受伤）。 */
    private static final double DAMAGE_RADIUS = 5.0;

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

        // 沿光柱方向的采样步长（密度 8/格）
        int steps = Math.max(1, (int) (length * 8));

        DustParticleOptions coreParticle = new DustParticleOptions(
                new Vector3f(0.95F, 0.97F, 1.0F), 2.0F);
        DustParticleOptions shellParticle = new DustParticleOptions(
                new Vector3f(0.5F, 0.7F, 1.0F), 1.2F);
        DustParticleOptions glowParticle = new DustParticleOptions(
                new Vector3f(0.3F, 0.5F, 1.0F), 0.7F);

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Vec3 center = start.add(dir.scale(length * t));

            // 核心：中心高密度亮白粒子
            level.sendParticles(coreParticle, center.x, center.y, center.z,
                    3, 0.08, 0.08, 0.08, 0);

            // 柱壁：环形分布 6 个粒子，形成圆柱截面轮廓
            for (int a = 0; a < 6; a++) {
                double angle = (a / 6.0) * Math.PI * 2 + t * 3.0;
                Vec3 ringOffset = right.scale(Math.cos(angle) * BEAM_RADIUS)
                        .add(orthoUp.scale(Math.sin(angle) * BEAM_RADIUS));
                Vec3 ringPos = center.add(ringOffset);
                level.sendParticles(shellParticle, ringPos.x, ringPos.y, ringPos.z,
                        1, 0.04, 0.04, 0.04, 0);
            }

            // 外晕：每 2 步生成一次大范围蓝色光晕粒子
            if (i % 2 == 0) {
                level.sendParticles(glowParticle, center.x, center.y, center.z,
                        2, BEAM_RADIUS * 1.2, BEAM_RADIUS * 1.2, BEAM_RADIUS * 1.2, 0);
            }
        }
    }
}
