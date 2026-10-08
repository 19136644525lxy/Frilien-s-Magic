package yifei.frliliens.magic.spell.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import yifei.frliliens.magic.spell.AbstractSpell;

/**
 * 魔力挤压 Mana Strike。
 *
 * <p>将魔力压缩至极限，短时间内大幅提升近战伤害与速度。
 */
public class ManaStrikeSpell extends AbstractSpell {

    public ManaStrikeSpell() {
        super("mana_strike", 100);
    }

    @Override
    protected boolean doCast(Level level, Player caster) {
        // 力量提升 + 速度提升 + 急迫，模拟魔力压缩的近战强化
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 2,
                false, true, true));
        caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1,
                false, true, true));
        caster.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 200, 1,
                false, true, true));

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.8F, 0.7F);
        }
        return true;
    }
}
