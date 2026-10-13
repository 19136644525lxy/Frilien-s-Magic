package yifei.frliliens.magic.spell.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import yifei.frliliens.magic.item.ModItems;
import yifei.frliliens.magic.spell.AbstractSpell;

/**
 * 变出刨冰。
 *
 * <p>生活魔法：凭空生成刨冰放入背包。
 * 虽非战斗魔法，但可通过法杖施放。
 */
public class ShavedIceSpell extends AbstractSpell {

    public ShavedIceSpell() {
        super("shaved_ice", 10);
    }

    @Override
    protected boolean doCast(Level level, Player caster) {
        // 给玩家背包放入刨冰
        ItemStack shavedIce = new ItemStack(ModItems.SHAVED_ICE.get());
        if (!caster.getInventory().add(shavedIce)) {
            // 背包满则丢在地上
            caster.drop(shavedIce, false);
        }

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.3F, 2.0F);
        }
        return true;
    }
}
