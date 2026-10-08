package yifei.frliliens.magic.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import yifei.frliliens.magic.FriliensMagic;

/**
 * 魔力条相关按键映射。
 *
 * <p>两个按键：
 * <ul>
 *   <li>{@code editKey} — 切换拖动编辑模式（默认未绑定）</li>
 *   <li>{@code toggleKey} — 切换魔力条显隐（默认未绑定）</li>
 * </ul>
 *
 * <p>按键注册在 {@link FriliensMagicClient} 的 MOD 总线完成；
 * 按键检测走游戏总线 {@link ClientTickEvent}，用 {@link KeyMapping#consumeClick()}
 * 保证每按一次只触发一次。
 */
@EventBusSubscriber(modid = FriliensMagic.MODID, value = Dist.CLIENT)
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

    private ManaBarKeyBindings() {
    }

    /** 每 tick 检测按键（游戏总线事件）。 */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (EDIT_KEY.consumeClick()) {
            ManaBarRenderer.openEditScreen();
        }
        while (TOGGLE_KEY.consumeClick()) {
            ManaBarRenderer.toggleVisible();
        }
    }
}
