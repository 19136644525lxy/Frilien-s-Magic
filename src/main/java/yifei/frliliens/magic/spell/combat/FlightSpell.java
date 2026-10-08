package yifei.frliliens.magic.spell.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import yifei.frliliens.magic.attachment.FlightData;
import yifei.frliliens.magic.attachment.ModAttachments;
import yifei.frliliens.magic.spell.AbstractSpell;

/**
 * 飞行魔法。
 *
 * <p>给予施法者创造模式飞行能力，持续到死亡。
 * 切换其他法术不会取消飞行，只有死亡后需要重新施法。
 */
public class FlightSpell extends AbstractSpell {

    public FlightSpell() {
        super("flight", 60);
    }

    @Override
    protected boolean doCast(Level level, Player caster) {
        var abilities = caster.getAbilities();
        abilities.mayfly = true;
        abilities.flying = true;
        caster.onUpdateAbilities();

        // 存储飞行状态，由 FlightHandler 维持
        caster.setData(ModAttachments.FLIGHT, new FlightData(true));
        if (caster instanceof ServerPlayer sp) {
            sp.syncData(ModAttachments.FLIGHT);
        }

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.4F, 1.5F);
        }
        return true;
    }
}
