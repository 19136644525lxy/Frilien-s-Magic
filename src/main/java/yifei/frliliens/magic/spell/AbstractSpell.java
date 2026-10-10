package yifei.frliliens.magic.spell;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import yifei.frliliens.magic.FriliensMagic;

/**
 * 法术抽象基类。
 *
 * <p>封装通用字段（id、魔力消耗），子类只需实现 {@link #doCast}。
 * 显示名称通过翻译键 {@code spell.<modid>.<id>} 自动生成。
 */
public abstract class AbstractSpell implements Spell {

    private final String id;
    private final String translationKey;
    private final int manaCost;
    private final int cooldown;

    protected AbstractSpell(String id, int manaCost) {
        this(id, manaCost, 20);
    }

    protected AbstractSpell(String id, int manaCost, int cooldown) {
        this.id = id;
        this.translationKey = "spell." + FriliensMagic.MODID + "." + id;
        this.manaCost = manaCost;
        this.cooldown = cooldown;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return translationKey;
    }

    @Override
    public Component getDisplayNameComponent() {
        return Component.translatable(translationKey);
    }

    @Override
    public int getManaCost() {
        return manaCost;
    }

    @Override
    public int getCooldown() {
        return cooldown;
    }

    @Override
    public boolean cast(Level level, Player caster) {
        if (level.isClientSide) {
            return false;
        }
        return doCast(level, caster);
    }

    /**
     * 实际施法逻辑，仅在服务端调用。
     *
     * @return 是否施法成功
     */
    protected abstract boolean doCast(Level level, Player caster);
}
