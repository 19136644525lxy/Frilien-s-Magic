package yifei.frliliens.magic.spell.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import yifei.frliliens.magic.spell.AbstractSpell;

/**
 * 封印魔法。
 *
 * <p>将周围敌人封印使其无法行动（缓慢 + 失明 + 挖掘疲劳）。
 */
public class SealSpell extends AbstractSpell {

    public SealSpell() {
        super("seal", 90);
    }

    @Override
    protected boolean doCast(Level level, Player caster) {
        AABB area = caster.getBoundingBox().inflate(6.0);
        int count = 0;

        for (Entity entity : level.getEntities(caster, area)) {
            if (entity instanceof LivingEntity living && entity != caster) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        160, 4, false, true, true));
                living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,
                        160, 0, false, true, true));
                living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,
                        160, 3, false, true, true));
                count++;
            }
        }

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    caster.getX(), caster.getY() + 1, caster.getZ(),
                    30, 3.0, 2.0, 3.0, 0.05);
            serverLevel.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 0.5F);
        }
        return true;
    }
}
