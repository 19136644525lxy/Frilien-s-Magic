package yifei.frliliens.magic.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;

import yifei.frliliens.magic.FriliensMagic;

/**
 * 魔力条相关按键映射。
 *
 * <p>两个按键：
 * <ul>
 *   <li>{@code EDIT_KEY} — 切换拖动编辑模式（默认未绑定）</li>
 *   <li>{@code TOGGLE_KEY} — 切换魔力条显隐（默认未绑定）</li>
 * </ul>
 *
 * <p>按键<b>注册</b>在 {@code RegisterKeyMappingsEvent}（mod 总线）；
 * 按键<b>检测</b>统一在 {@code FriliensMagicClient#onClientTick} 里做，
 * 用 {@link KeyMapping#consumeClick()} 保证每按一次只触发一次。
 *
 * <p><b>与 1.21 版的差异</b>：那边用注解自行监听 {@code ClientTickEvent.Post}。
 * 1.20.1 的该类事件是嵌套的 {@code TickEvent.ClientTickEvent} 且没有 {@code Post}
 * 子类，所以改为由客户端主类集中注册。
 */
public final class ManaBarKeyBindings {

    private static final String CATEGORY = "key.categories." + FriliensMagic.MODID;

    /** 切换拖动编辑模式（默认未绑定）。 */
    public static final KeyMapping EDIT_KEY = new KeyMapping(
            "key." + FriliensMagic.MODID + ".mana_edit",
            InputConstants.UNKNOWN.getValue(),
            CATEGORY);

    /** 切换魔力条显隐（默认未绑定）。 */
    public static final KeyMapping TOGGLE_KEY = new KeyMapping(
            "key." + FriliensMagic.MODID + ".mana_toggle",
            InputConstants.UNKNOWN.getValue(),
            CATEGORY);

    /** 处理一条 tick 里的按键。由客户端主类的 tick 回调调用。 */
    public static void handleInput() {
        while (EDIT_KEY.consumeClick()) {
            ManaBarRenderer.openEditScreen();
        }
        while (TOGGLE_KEY.consumeClick()) {
            ManaBarRenderer.toggleVisible();
        }
    }

    private ManaBarKeyBindings() {
    }
}
