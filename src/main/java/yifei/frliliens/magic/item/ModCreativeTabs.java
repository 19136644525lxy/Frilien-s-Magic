package yifei.frliliens.magic.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import yifei.frliliens.magic.FriliensMagic;

/** 模组创造物品栏，按种类分页。 */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FriliensMagic.MODID);

    /** 法杖与装备。 */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> STAFFS =
            TABS.register("staffs", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.friliensmagic.staffs"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.FRIEREN_STAFF.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.FRIEREN_STAFF.get());
                        output.accept(ModItems.PHIREN_STAFF.get());
                    })
                    .build());

    /** 法术卷轴。 */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SCROLLS =
            TABS.register("scrolls", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.friliensmagic.scrolls"))
                    .icon(() -> ModItems.SCROLL_ZOLTRAAK.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.SCROLL_ZOLTRAAK.get());
                        output.accept(ModItems.SCROLL_DEFENSE.get());
                        output.accept(ModItems.SCROLL_FLIGHT.get());
                        output.accept(ModItems.SCROLL_JUDRADJIM.get());
                        output.accept(ModItems.SCROLL_VOLLZANBEL.get());
                        output.accept(ModItems.SCROLL_BLACK_LIGHT.get());
                        output.accept(ModItems.SCROLL_MANA_STRIKE.get());
                        output.accept(ModItems.SCROLL_SEAL.get());
                        output.accept(ModItems.SCROLL_BLAST.get());
                        output.accept(ModItems.SCROLL_ICE.get());
                        output.accept(ModItems.SCROLL_FLOAT.get());
                        output.accept(ModItems.SCROLL_CLEAN.get());
                    })
                    .build());

    /** 魔力药水。 */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> POTIONS =
            TABS.register("potions", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.friliensmagic.potions"))
                    .icon(() -> ModItems.MANA_POTION_LV1.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.MANA_POTION_LV1.get());
                        output.accept(ModItems.MANA_POTION_LV2.get());
                        output.accept(ModItems.MANA_POTION_LV3.get());
                        output.accept(ModItems.MANA_POTION_LV4.get());
                        output.accept(ModItems.MANA_POTION_LV5.get());
                    })
                    .build());

    /** 食物。 */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FOOD =
            TABS.register("food", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.friliensmagic.food"))
                    .icon(() -> ModItems.SHAVED_ICE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.SHAVED_ICE.get());
                    })
                    .build());

    /** 杂项工具（刷怪蛋等）。 */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> UTILITY =
            TABS.register("utility", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.friliensmagic.utility"))
                    .icon(() -> ModItems.QUEST_VILLAGER_SPAWN_EGG.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.QUEST_VILLAGER_SPAWN_EGG.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
