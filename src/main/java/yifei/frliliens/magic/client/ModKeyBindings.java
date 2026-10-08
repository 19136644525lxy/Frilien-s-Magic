package yifei.frliliens.magic.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import yifei.frliliens.magic.FriliensMagic;
import yifei.frliliens.magic.network.CastBasicAbilityPayload;

/**
 * 模组按键映射。
 *
 * <p>包含基础能力按键和法术轮盘按键，默认均未绑定，需玩家自行设置。
 */
@EventBusSubscriber(modid = FriliensMagic.MODID, value = Dist.CLIENT)
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

    private ModKeyBindings() {
    }

    /** 每 tick 检测按键并触发对应行为。 */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
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

    /** 打开法术选择轮盘。 */
    private static void openSpellWheel() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        mc.setScreen(new SpellWheelScreen());
    }
}
