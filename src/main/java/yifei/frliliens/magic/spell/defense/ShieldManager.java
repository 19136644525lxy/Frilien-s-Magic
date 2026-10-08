package yifei.frliliens.magic.spell.defense;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import yifei.frliliens.magic.attachment.ModAttachments;
import yifei.frliliens.magic.attachment.ShieldData;

/**
 * 护盾业务逻辑层。
 *
 * <p>封装护盾的激活、消耗强度、破碎等操作，统一通过附件读写数据。
 * 所有写操作只在服务端执行；附件的 {@code sync} 会自动把状态推到客户端。
 */
public final class ShieldManager {

    private ShieldManager() {
    }

    /**
     * 激活目标的护盾。
     *
     * @param entity      目标实体（通常是玩家）
     * @param maxStrength 护盾最大强度
     * @return 是否成功激活
     */
    public static boolean activateShield(Entity entity, int maxStrength) {
        if (entity == null || entity.level().isClientSide) {
            return false;
        }
        ShieldData data = entity.getData(ModAttachments.SHIELD);
        ShieldData newData = new ShieldData(true, maxStrength, maxStrength);
        entity.setData(ModAttachments.SHIELD, newData);
        entity.syncData(ModAttachments.SHIELD);
        return true;
    }

    /**
     * 取消目标的护盾（不破碎，静默关闭）。
     */
    public static void deactivateShield(Entity entity) {
        if (entity == null || entity.level().isClientSide) {
            return;
        }
        ShieldData data = entity.getData(ModAttachments.SHIELD);
        if (data.active()) {
            entity.setData(ModAttachments.SHIELD, data.withActive(false));
            entity.syncData(ModAttachments.SHIELD);
        }
    }

    /**
     * 目标是否拥有激活的护盾。
     */
    public static boolean hasActiveShield(Entity entity) {
        if (entity == null) {
            return false;
        }
        return entity.getData(ModAttachments.SHIELD).isActive();
    }

    /**
     * 吸收伤害。
     *
     * <p>优先用护盾抵挡，护盾强度不足时剩余伤害穿透。
     *
     * @param entity 受击实体
     * @param damage 原始伤害
     * @return 穿透护盾后的剩余伤害（>= 0）
     */
    public static float absorbDamage(LivingEntity entity, float damage) {
        if (entity.level().isClientSide || damage <= 0) {
            return damage;
        }
        ShieldData data = entity.getData(ModAttachments.SHIELD);
        if (!data.isActive()) {
            return damage;
        }

        int absorbed = (int) Math.min(damage, data.strength());
        ShieldData newData = data.consume(absorbed);
        entity.setData(ModAttachments.SHIELD, newData);
        entity.syncData(ModAttachments.SHIELD);

        // 受击视觉反馈：在受击位置生成粒子
        spawnImpactParticles(entity);

        // 护盾破碎
        if (!newData.isActive() && data.isActive()) {
            onShieldBroken(entity);
        }

        return damage - absorbed;
    }

    /** 护盾破碎时的处理：音效 + 粒子 + 法杖冷却。 */
    private static void onShieldBroken(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 pos = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
        // 破碎粒子爆发
        serverLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                pos.x, pos.y, pos.z,
                12, 0.5, 0.5, 0.5, 0.05);
        serverLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.GLOW,
                pos.x, pos.y, pos.z,
                20, 0.8, 0.8, 0.8, 0.0);

        serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                net.minecraft.sounds.SoundEvents.RESPAWN_ANCHOR_DEPLETE,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    /** 在护盾表面生成受击粒子。 */
    private static void spawnImpactParticles(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 pos = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
        serverLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.END_ROD,
                pos.x, pos.y, pos.z,
                8, 0.4, 0.4, 0.4, 0.01);
    }
}
