package yifei.frliliens.magic.spell.defense;

import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import yifei.frliliens.magic.attachment.ShieldData;
import yifei.frliliens.magic.capability.PlayerDataHelper;
import yifei.frliliens.magic.config.Config;

/**
 * 护盾事件处理器。
 *
 * <p>职责：
 * <ul>
 *   <li>拦截 {@link LivingHurtEvent}，用护盾吸收伤害</li>
 *   <li>拦截 {@link ProjectileImpactEvent}，命中护盾时偏转弹射物</li>
 * </ul>
 *
 * <h2>与 1.21 版的差异</h2>
 * <ol>
 *   <li>伤害事件从 {@code LivingIncomingDamageEvent} 换成 {@link LivingHurtEvent}。
 *       两者都在无敌帧判定之后，语义等价。</li>
 *   <li>弹射物偏转从「每 tick 用 {@code EntityTickEvent} 做距离判定」
 *       改成「用 {@link ProjectileImpactEvent} 在命中瞬间处理」。
 *       后者是实际改进：不必每 tick 遍历附近实体，也不会漏掉
 *       高速弹射物穿透护盾的情况。</li>
 * </ol>
 */
public final class ShieldEventHandler {

    private ShieldEventHandler() {
    }

    /**
     * 伤害拦截：有护盾时优先吸收。
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) {
            return;
        }
        // 无视无敌帧的伤害不拦截，防止利用护盾卡出世界
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        // 护盾目前只挂在玩家身上
        if (!(target instanceof Player)) {
            return;
        }

        if (!ShieldManager.hasActiveShield(target)) {
            return;
        }

        float remaining = ShieldManager.absorbDamage(target, event.getAmount());
        event.setAmount(remaining);
    }

    /**
     * 弹射物偏转：命中护盾时反弹。
     *
     * <p>只在命中瞬间判定一次，比每 tick 距离检测更准也更省。
     */
    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        Level level = projectile.level();
        if (level.isClientSide) {
            return;
        }

        Vec3 projPos = projectile.position();
        double searchR = 8.0;
        AABB box = AABB.ofSize(projPos, searchR * 2, searchR * 2, searchR * 2);

        List<Entity> candidates = level.getEntities(projectile, box,
                e -> e != projectile.getOwner() && ShieldManager.hasActiveShield(e));

        for (Entity target : candidates) {
            ShieldData shield = PlayerDataHelper.shield(target);
            if (!shield.isActive()) {
                continue;
            }
            Vec3 center = target.position().add(0, target.getBbHeight() * 0.5, 0);
            double radius = Config.SHIELD_RADIUS.get();

            // 命中点（或当前位置）在护盾球体内
            Vec3 hit = event.getRayTraceResult().getLocation();
            if (hit.distanceTo(center) > radius && projPos.distanceTo(center) > radius) {
                continue;
            }

            deflect(projectile, target, center);
            // 让弹射物停在这里<b>且不造成伤害</b>——它已经被护盾弹开了。
            // 1.20.1 的 setCanceled 已废弃，官方建议改用 setImpactResult。
            event.setImpactResult(ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE);
            return;
        }
    }

    /** 弹射物偏转：改变速度方向、消耗护盾强度。 */
    private static void deflect(Projectile projectile, Entity target, Vec3 center) {
        Vec3 projPos = projectile.position();
        Vec3 dir = projPos.subtract(center);
        // 正好在球心时方向为零向量，给一个朝上的默认方向，避免 NaN
        Vec3 deflectDir = dir.lengthSqr() < 1.0E-6 ? new Vec3(0, 1, 0) : dir.normalize();
        double speed = Math.max(projectile.getDeltaMovement().length(), 0.2);
        projectile.setDeltaMovement(deflectDir.scale(speed * 0.8));
        projectile.hasImpulse = true;

        // 转移所有者，避免反弹后仍伤害原目标
        if (projectile.getOwner() != target) {
            projectile.setOwner(target);
        }

        // 消耗 1 点护盾强度（内部会处理破碎与同步）
        if (target instanceof Player player) {
            ShieldManager.consumeOne(target.level(), player);
        }

        // 偏转音效与粒子
        if (target.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(ParticleTypes.END_ROD, projPos.x, projPos.y, projPos.z,
                    6, 0.2, 0.2, 0.2, 0.02);
        }
        target.level().playSound(null, projPos.x, projPos.y, projPos.z,
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.5F);
    }
}
