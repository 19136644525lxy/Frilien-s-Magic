package yifei.frliliens.magic.spell.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import yifei.frliliens.magic.damage.ModDamageTypes;
import yifei.frliliens.magic.spell.AbstractSpell;

/**
 * 破坏黑光。
 *
 * <p>释放黑色光柱，对前方直线范围内所有实体造成穿透伤害，
 * 可直接击破防御魔法。
 */
public class BlackLightSpell extends AbstractSpell {

    public BlackLightSpell() {
        super("black_light", 200);
    }

    @Override
    protected boolean doCast(Level level, Player caster) {
        // 前方 10 格、宽度 2 格的 AABB
        AABB area = caster.getBoundingBox()
                .expandTowards(caster.getLookAngle().scale(10.0))
                .inflate(2.0);

        DamageSource source = ModDamageTypes.magic(caster);
        int hitCount = 0;

        for (Entity entity : level.getEntities(caster, area)) {
            if (entity instanceof LivingEntity living && entity != caster) {
                living.hurt(source, 25.0F);
                hitCount++;
            }
        }

        if (level instanceof ServerLevel serverLevel) {
            // 黑色光柱粒子
            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH,
                    caster.getX() + caster.getLookAngle().x * 5,
                    caster.getY() + caster.getEyeHeight(),
                    caster.getZ() + caster.getLookAngle().z * 5,
                    40, 1.0, 0.5, 1.0, 0.02);
            serverLevel.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.8F, 0.6F);
        }
        return hitCount >= 0;
    }
}
