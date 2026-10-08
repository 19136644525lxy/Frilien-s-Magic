package yifei.frliliens.magic.spell.combat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import yifei.frliliens.magic.config.Config;
import yifei.frliliens.magic.spell.AbstractSpell;
import yifei.frliliens.magic.spell.defense.ShieldManager;

/**
 * 防御魔法。
 *
 * <p>激活护盾，吸收伤害。复用现有 {@link ShieldManager}。
 */
public class DefenseSpell extends AbstractSpell {

    public DefenseSpell() {
        super("defense", 80);
    }

    @Override
    protected boolean doCast(Level level, Player caster) {
        int maxStrength = Config.SHIELD_MAX_STRENGTH.getAsInt();
        return ShieldManager.activateShield(caster, maxStrength);
    }
}
