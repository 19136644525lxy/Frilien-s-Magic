package yifei.frliliens.magic.spell.basic;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import yifei.frliliens.magic.spell.AbstractSpell;

/**
 * 长距离魔法（被动）。
 *
 * <p>学习后自动生效，使攻击魔法（Zoltraak）的射程随玩家视距大幅提升。
 * 本身无主动施法效果，不消耗魔力，无需按键触发。
 */
public class LongRangeSpell extends AbstractSpell {

    public LongRangeSpell() {
        super("long_range", 0);
    }

    /** 被动法术，无主动效果。 */
    @Override
    protected boolean doCast(Level level, Player caster) {
        return false;
    }
}
