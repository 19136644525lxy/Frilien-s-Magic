package yifei.frliliens.magic.spell;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 法术接口。
 *
 * <p>所有可释放的法术均实现此接口。每个法术定义自身的魔力消耗与释放逻辑。
 * 基础能力（魔力探知等）不消耗魔力，可返回 {@code 0}。
 *
 * <p>遵循单一职责：每个法术类只负责自身效果，魔力消耗校验由调用方统一处理。
 */
public interface Spell {

    /** 法术唯一标识，用于注册与序列化。 */
    String getId();

    /** 法术显示名称翻译键，格式为 {@code spell.<modid>.<id>}。 */
    String getDisplayName();

    /** 获取已翻译的显示名称组件。 */
    Component getDisplayNameComponent();

    /** 释放该法术所需魔力。基础能力返回 0。 */
    int getManaCost();

    /** 法杖释放后的冷却时间（tick）。默认 20 tick（1 秒）。 */
    default int getCooldown() {
        return 20;
    }

    /**
     * 释放法术。
     *
     * @param level  世界
     * @param caster 施法者
     * @return 是否施法成功
     */
    boolean cast(Level level, Player caster);
}
