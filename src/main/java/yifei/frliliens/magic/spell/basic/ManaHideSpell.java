package yifei.frliliens.magic.spell.basic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import yifei.frliliens.magic.attachment.ManaData;
import yifei.frliliens.magic.attachment.ModAttachments;
import yifei.frliliens.magic.effect.ModEffects;
import yifei.frliliens.magic.spell.AbstractSpell;

/**
 * 基础能力：魔力隐藏／限制。
 *
 * <p>给予魔力隐匿效果，使玩家不被生物索敌。
 * 持续时间随魔力上限提升。不消耗魔力，但有冷却。
 */
public class ManaHideSpell extends AbstractSpell {

    /** 基础持续时间（tick）。 */
    private static final int BASE_DURATION = 200;

    public ManaHideSpell() {
        super("mana_hide", 0);
    }

    @Override
    protected boolean doCast(Level level, Player caster) {
        // 持续时间随魔力上限增强：200 + maxMana/10 tick
        ManaData mana = caster.getData(ModAttachments.MANA);
        int duration = BASE_DURATION + mana.max() / 10;

        caster.addEffect(new MobEffectInstance(ModEffects.MANA_CONCEAL,
                duration, 0, false, false, true));

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.6F, 0.8F);
        }
        return true;
    }
}
