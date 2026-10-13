package yifei.frliliens.magic.spell.defense;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import yifei.frliliens.magic.attachment.ShieldData;
import yifei.frliliens.magic.capability.PlayerDataHelper;

/**
 * 护盾业务逻辑层。
 *
 * <p>封装护盾的激活、消耗强度、破碎等操作。
 * 所有写操作只在服务端执行，写完后显式发包同步到客户端
 * （1.20.1 没有附件自动同步，这是与 1.21 版最大的差别）。
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
        if (!(entity instanceof net.minecraft.world.entity.player.Player player)) {
            return false;
        }
        if (entity.level().isClientSide) {
            return false;
        }
        ShieldData newData = new ShieldData(true, maxStrength, maxStrength);
        if (!PlayerDataHelper.setShield(player, newData)) {
            return false;
        }
        PlayerDataHelper.syncShield(player);
        return true;
    }

    /**
     * 取消目标的护盾（不破碎，静默关闭）。
     */
    public static void deactivateShield(Entity entity) {
        if (!(entity instanceof net.minecraft.world.entity.player.Player player)) {
            return;
        }
        if (entity.level().isClientSide) {
            return;
        }
        ShieldData data = PlayerDataHelper.shield(player);
        if (data.active() && PlayerDataHelper.setShield(player, data.withActive(false))) {
            PlayerDataHelper.syncShield(player);
        }
    }

    /**
     * 目标是否拥有激活的护盾。
     */
    public static boolean hasActiveShield(Entity entity) {
        if (entity == null) {
            return false;
        }
        return PlayerDataHelper.shield(entity).isActive();
    }

    /**
     * 吸收伤害。
     *
     * <p>优先用护盾抵挡，护盾强度不足时剩余伤害穿透。
     *
     * @param entity 受击实体（必须是玩家，护盾数据挂在玩家上）
     * @param damage 原始伤害
     * @return 穿透护盾后的剩余伤害（>= 0）
     */
    public static float absorbDamage(LivingEntity entity, float damage) {
        if (entity.level().isClientSide || damage <= 0) {
            return damage;
        }
        if (!(entity instanceof net.minecraft.world.entity.player.Player player)) {
            // 护盾目前只挂在玩家身上；其它实体不拦截
            return damage;
        }

        ShieldData data = PlayerDataHelper.shield(player);
        if (!data.isActive()) {
            return damage;
        }

        int absorbed = (int) Math.min(damage, data.strength());
        ShieldData newData = data.consume(absorbed);
        if (!PlayerDataHelper.setShield(player, newData)) {
            return damage;
        }
        PlayerDataHelper.syncShield(player);

        // 受击视觉反馈：在受击位置生成粒子
        spawnImpactParticles(entity);

        // 护盾破碎
        if (!newData.isActive() && data.isActive()) {
            onShieldBroken(entity);
        }

        return damage - absorbed;
    }

    /** 护盾破碎时的处理：音效 + 粒子。 */
    private static void onShieldBroken(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 pos = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
        // 破碎粒子爆发
        serverLevel.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z,
                12, 0.5, 0.5, 0.5, 0.05);
        serverLevel.sendParticles(ParticleTypes.GLOW, pos.x, pos.y, pos.z,
                20, 0.8, 0.8, 0.8, 0.0);

        serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    /** 在护盾表面生成受击粒子。 */
    private static void spawnImpactParticles(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 pos = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
        serverLevel.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z,
                8, 0.4, 0.4, 0.4, 0.01);
    }

    /**
     * 消耗 1 点护盾强度（弹射物偏转用）。
     *
     * @return 是否成功消耗
     */
    public static boolean consumeOne(Level level, net.minecraft.world.entity.player.Player player) {
        if (level.isClientSide) {
            return false;
        }
        ShieldData data = PlayerDataHelper.shield(player);
        if (!data.isActive()) {
            return false;
        }
        ShieldData newData = data.consume(1);
        if (!PlayerDataHelper.setShield(player, newData)) {
            return false;
        }
        PlayerDataHelper.syncShield(player);

        if (!newData.isActive() && data.isActive()) {
            onShieldBroken(player);
        }
        return true;
    }
}
