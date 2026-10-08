package yifei.frliliens.magic.spell.defense;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import yifei.frliliens.magic.FriliensMagic;
import yifei.frliliens.magic.attachment.ModAttachments;
import yifei.frliliens.magic.attachment.ShieldData;
import yifei.frliliens.magic.config.Config;

/**
 * 护盾事件处理器。
 *
 * <p>职责：
 * <ul>
 *   <li>拦截 {@link LivingIncomingDamageEvent}，用护盾吸收伤害</li>
 *   <li>拦截弹射物（{@link Projectile}），命中护盾时偏转并消耗强度</li>
 * </ul>
 *
 * <p>注册在游戏事件总线（{@code NeoForge.EVENT_BUS}）上。
 */
@EventBusSubscriber(modid = FriliensMagic.MODID)
public final class ShieldEventHandler {

    /** 已在本 tick 处理过的弹射物，避免重复偏转。 */
    private static final ConcurrentHashMap<UUID, Long> DEFLECTED = new ConcurrentHashMap<>();

    private ShieldEventHandler() {
    }

    /**
     * 伤害拦截：有护盾时优先吸收。
     */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) {
            return;
        }
        // 虚空伤害不拦截，防止利用护盾卡出世界
        if (event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }

        if (!ShieldManager.hasActiveShield(target)) {
            return;
        }

        float remaining = ShieldManager.absorbDamage(target, event.getAmount());
        event.setAmount(remaining);
    }

    /**
     * 弹射物偏转：每 tick 检测附近的护盾并反弹弹射物。
     */
    @SubscribeEvent
    public static void onProjectileTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Projectile projectile)) {
            return;
        }
        Level level = projectile.level();
        if (level.isClientSide) {
            return;
        }

        long gameTime = level.getGameTime();
        UUID id = projectile.getUUID();
        Long last = DEFLECTED.get(id);
        if (last != null && last == gameTime) {
            return;
        }

        Vec3 projPos = projectile.position();
        double searchR = 8.0;
        AABB box = AABB.ofSize(projPos, searchR * 2, searchR * 2, searchR * 2);

        List<Entity> candidates = level.getEntities(projectile, box,
                e -> e != projectile.getOwner() && ShieldManager.hasActiveShield(e));

        for (Entity target : candidates) {
            ShieldData shield = target.getData(ModAttachments.SHIELD);
            if (!shield.isActive()) {
                continue;
            }
            Vec3 center = target.position().add(0, target.getBbHeight() * 0.5, 0);
            double radius = Config.SHIELD_RADIUS.getAsDouble();
            double dist = projPos.distanceTo(center);

            // 当前位置已在护盾内，或下一帧将进入
            Vec3 nextPos = projPos.add(projectile.getDeltaMovement());
            if (dist <= radius || nextPos.distanceTo(center) <= radius) {
                deflectProjectile(projectile, target, center, radius);
                DEFLECTED.put(id, gameTime);
                // 定期清理，防止内存泄漏
                if (DEFLECTED.size() > 500) {
                    DEFLECTED.entrySet().removeIf(e -> gameTime - e.getValue() > 100);
                }
                return;
            }
        }
    }

    /** 弹射物偏转：改变速度方向、消耗护盾强度。 */
    private static void deflectProjectile(Projectile projectile, Entity target, Vec3 center, double radius) {
        Vec3 projPos = projectile.position();
        Vec3 deflectDir = projPos.subtract(center).normalize();
        double speed = projectile.getDeltaMovement().length();
        projectile.setDeltaMovement(deflectDir.scale(speed * 0.8));
        projectile.hasImpulse = true;

        // 转移所有者，避免反弹后仍伤害原目标
        if (projectile.getOwner() != target) {
            projectile.setOwner(target);
        }

        // 消耗 1 点护盾强度
        ShieldData data = target.getData(ModAttachments.SHIELD);
        ShieldData newData = data.consume(1);
        target.setData(ModAttachments.SHIELD, newData);
        target.syncData(ModAttachments.SHIELD);

        if (!newData.isActive() && data.isActive()) {
            // 护盾已被 consume 设为未激活，播放破碎效果
            if (target.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                Vec3 pos = target.position().add(0, target.getBbHeight() * 0.5, 0);
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                        pos.x, pos.y, pos.z, 12, 0.5, 0.5, 0.5, 0.05);
                sl.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.PLAYERS, 1.0F, 0.6F);
            }
        }

        // 偏转音效
        target.level().playSound(null, projPos.x, projPos.y, projPos.z,
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.5F);
    }
}
