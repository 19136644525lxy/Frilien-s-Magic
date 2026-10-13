package yifei.frliliens.magic.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import yifei.frliliens.magic.FriliensMagic;
import yifei.frliliens.magic.item.StaffItem;
import yifei.frliliens.magic.network.CastBasicAbilityPayload;

/**
 * 模组按键映射。
 *
 * <p>包含基础能力按键和法术轮盘按键，默认均未绑定，需玩家自行设置。
 *
 * <p><b>与 1.21 版的差异</b>：那边用注解监听 {@code ClientTickEvent.Post}；
 * 1.20.1 统一由 {@code FriliensMagicClient#onClientTick} 调用 {@link #handleInput()}。
 */
public final class ModKeyBindings {

    private static final String CATEGORY = "key.categories." + FriliensMagic.MODID;

    /** 魔力探知（默认未绑定）。 */
    public static final KeyMapping MANA_SENSE_KEY = new KeyMapping(
            "key." + FriliensMagic.MODID + ".mana_sense",
            InputConstants.UNKNOWN.getValue(),
            CATEGORY);

    /** 魔力隐藏／限制（默认未绑定）。 */
    public static final KeyMapping MANA_HIDE_KEY = new KeyMapping(
            "key." + FriliensMagic.MODID + ".mana_hide",
            InputConstants.UNKNOWN.getValue(),
            CATEGORY);

    /** 法术选择轮盘（默认未绑定）。 */
    public static final KeyMapping SPELL_WHEEL_KEY = new KeyMapping(
            "key." + FriliensMagic.MODID + ".spell_wheel",
            InputConstants.UNKNOWN.getValue(),
            CATEGORY);

    /** 处理一条 tick 里的按键。由客户端主类的 tick 回调调用。 */
    public static void handleInput() {
        while (MANA_SENSE_KEY.consumeClick()) {
            CastBasicAbilityPayload.send("mana_sense");
        }
        while (MANA_HIDE_KEY.consumeClick()) {
            CastBasicAbilityPayload.send("mana_hide");
        }
        while (SPELL_WHEEL_KEY.consumeClick()) {
            openSpellWheel();
        }
    }

    /** 打开法术选择轮盘。手持哪把法杖就显示哪把的轮盘。 */
    private static void openSpellWheel() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }

        // 识别手持法杖：优先主手，其次副手
        String staffId = StaffItem.staffIdOf(mc.player.getMainHandItem());
        if (staffId == null) {
            staffId = StaffItem.staffIdOf(mc.player.getOffhandItem());
        }
        if (staffId == null) {
            mc.player.displayClientMessage(
                    Component.translatable("message.friliensmagic.staff_required")
                            .withStyle(ChatFormatting.YELLOW),
                    true);
            return;
        }

        mc.setScreen(new SpellWheelScreen(staffId));
    }

    private ModKeyBindings() {
    }
}
