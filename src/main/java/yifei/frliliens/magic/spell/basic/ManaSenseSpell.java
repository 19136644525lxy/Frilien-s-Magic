package yifei.frliliens.magic.spell.basic;

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
 * 基础能力：魔力探知。
 *
 * <p>感知周围 16 格内的实体，给予发光效果以便追踪。
 * 不消耗魔力，但有冷却。
 */
public class ManaSenseSpell extends AbstractSpell {

    public ManaSenseSpell() {
        super("mana_sense", 0);
    }

    @Override
    protected boolean doCast(Level level, Player caster) {
        AABB area = caster.getBoundingBox().inflate(16.0);
        int count = 0;

        for (Entity entity : level.getEntities(caster, area)) {
            if (entity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.GLOWING,
                        200, 0, false, false, true));
                count++;
            }
        }

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.2F);
        }

        caster.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(
                        "message.friliensmagic.mana_sense_result", count), true);
        return true;
    }
}
