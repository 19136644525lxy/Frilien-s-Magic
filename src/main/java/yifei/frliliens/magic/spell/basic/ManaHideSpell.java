package yifei.frliliens.magic.spell.basic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import yifei.frliliens.magic.spell.AbstractSpell;

/**
 * 基础能力：魔力隐藏／限制。
 *
 * <p>隐藏自身魔力，给予隐身效果以避开侦测。
 * 不消耗魔力，但有冷却。
 */
public class ManaHideSpell extends AbstractSpell {

    public ManaHideSpell() {
        super("mana_hide", 0);
    }

    @Override
    protected boolean doCast(Level level, Player caster) {
        caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,
                200, 0, false, false, true));

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.6F, 0.8F);
        }
        return true;
    }
}
