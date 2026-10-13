package yifei.frliliens.magic.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import yifei.frliliens.magic.FriliensMagic;

/**
 * 创造模式物品栏。
 *
 * <p>与 1.21 版一致的 5 个分类：法杖 / 卷轴 / 药水 / 食物 / 杂项。
 *
 * <h2>与 1.21 版的差异</h2>
 * <ul>
 *   <li>{@code DeferredHolder} → {@link RegistryObject}</li>
 *   <li>{@code withTabsBefore} 的参数在 1.20.1 是
 *       {@link net.minecraft.resources.ResourceKey}<{@link CreativeModeTab}>，
 *       而 1.21 接受其它分类的翻译键</li>
 *   <li>物品栏标题仍用原版惯例键 {@code itemGroup.<modid>.<name>}</li>
 * </ul>
 */
public final class ModCreativeTabs {

    /** 物品栏注册器。 */
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FriliensMagic.MODID);

    /** 法杖：紧跟在原版「战斗」栏之后。 */
    public static final RegistryObject<CreativeModeTab> STAFFS = TABS.register("staffs",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + FriliensMagic.MODID + ".staffs"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> new ItemStack(ModItems.FRIEREN_STAFF.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.FRIEREN_STAFF.get());
                        output.accept(ModItems.PHIREN_STAFF.get());
                    })
                    .build());

    /** 法术卷轴。 */
    public static final RegistryObject<CreativeModeTab> SCROLLS = TABS.register("scrolls",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + FriliensMagic.MODID + ".scrolls"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> new ItemStack(ModItems.SCROLL_ZOLTRAAK.get()))
                    .displayItems((params, output) -> {
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
    public static final RegistryObject<CreativeModeTab> POTIONS = TABS.register("potions",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + FriliensMagic.MODID + ".potions"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> new ItemStack(ModItems.MANA_POTION_LV1.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.MANA_POTION_LV1.get());
                        output.accept(ModItems.MANA_POTION_LV2.get());
                        output.accept(ModItems.MANA_POTION_LV3.get());
                        output.accept(ModItems.MANA_POTION_LV4.get());
                        output.accept(ModItems.MANA_POTION_LV5.get());
                    })
                    .build());

    /** 食物。 */
    public static final RegistryObject<CreativeModeTab> FOOD = TABS.register("food",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + FriliensMagic.MODID + ".food"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> new ItemStack(ModItems.SHAVED_ICE.get()))
                    .displayItems((params, output) -> output.accept(ModItems.SHAVED_ICE.get()))
                    .build());

    /** 杂项（刷怪蛋等）。 */
    public static final RegistryObject<CreativeModeTab> UTILITY = TABS.register("utility",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + FriliensMagic.MODID + ".utility"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> new ItemStack(ModItems.QUEST_VILLAGER_SPAWN_EGG.get()))
                    .displayItems((params, output) ->
                            output.accept(ModItems.QUEST_VILLAGER_SPAWN_EGG.get()))
                    .build());

    private ModCreativeTabs() {
    }
}
