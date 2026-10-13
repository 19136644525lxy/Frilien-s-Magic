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
 *
 * <p><b>数值来源</b>：魔力消耗与冷却由 {@link SpellTuning} 从 {@code Config} 按 id
 * 读取。构造参数里的数字只是「默认值文档」——改它们不会影响运行，要调数值请改
 * {@code Config} 里同名法术的条目。
 */
public abstract class AbstractSpell implements Spell {

    private final String id;
    private final String translationKey;
    /** 构造时声明的默认魔力消耗，仅供参考。 */
    private final int defaultManaCost;
    /** 构造时声明的默认冷却，仅供参考。 */
    private final int defaultCooldown;

    /**
     * @param id       法术 id
     * @param manaCost 默认魔力消耗（对应 {@code Config} 中的同名字段）
     */
    protected AbstractSpell(String id, int manaCost) {
        this(id, manaCost, 20);
    }

    /**
     * @param id       法术 id
     * @param manaCost 默认魔力消耗
     * @param cooldown 默认冷却（tick）
     */
    protected AbstractSpell(String id, int manaCost, int cooldown) {
        this.id = id;
        this.translationKey = "spell." + FriliensMagic.MODID + "." + id;
        this.defaultManaCost = manaCost;
        this.defaultCooldown = cooldown;
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

    /** 运行时魔力消耗，来自配置。 */
    @Override
    public int getManaCost() {
        return SpellTuning.manaCost(id);
    }

    /** 运行时冷却（tick），来自配置。 */
    @Override
    public int getCooldown() {
        return SpellTuning.cooldown(id);
    }

    /** 构造时声明的默认魔力消耗。 */
    public int getDefaultManaCost() {
        return defaultManaCost;
    }

    /** 构造时声明的默认冷却（tick）。 */
    public int getDefaultCooldown() {
        return defaultCooldown;
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
