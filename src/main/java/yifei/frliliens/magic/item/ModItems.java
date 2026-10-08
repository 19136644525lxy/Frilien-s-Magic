package yifei.frliliens.magic.item;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import yifei.frliliens.magic.FriliensMagic;
import yifei.frliliens.magic.spell.SpellRegistry;

/**
 * Item registration for the mod.
 *
 * <p>Every entry here needs three matching resources under
 * {@code assets/friliensmagic/}: a model at {@code models/item/<name>.json},
 * a texture at {@code textures/item/<name>.png}, and a name in both
 * {@code lang/en_us.json} and {@code lang/zh_cn.json}.
 */
public final class ModItems {

    /** All mod items are registered under the mod's own namespace. */
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(FriliensMagic.MODID);

    // ===== 法杖 =====
    /** 芙莉莲的法杖 / Frieren's Staff - crescent head with a red gem. */
    public static final DeferredItem<StaffItem> FRIEREN_STAFF =
            ITEMS.registerItem("frieren_staff", StaffItem::new, new Item.Properties().stacksTo(1));

    /** 菲伦的法杖 / Phiren's Staff - bent, cloth-wrapped staff. */
    public static final DeferredItem<StaffItem> PHIREN_STAFF =
            ITEMS.registerItem("phiren_staff", StaffItem::new, new Item.Properties().stacksTo(1));

    // ===== 魔力药水（1/3/5 提升上限，2/4 恢复量递增）=====
    /** 初级魔力药水：恢复 100，上限 +100。 */
    public static final DeferredItem<ManaPotionItem> MANA_POTION_LV1 =
            ITEMS.registerItem("potion_lv1",
                    p -> new ManaPotionItem(p, 100, 100), new Item.Properties().stacksTo(16));

    /** 中级魔力药水：恢复 300。 */
    public static final DeferredItem<ManaPotionItem> MANA_POTION_LV2 =
            ITEMS.registerItem("potion_lv2",
                    p -> new ManaPotionItem(p, 300, 0), new Item.Properties().stacksTo(16));

    /** 高级魔力药水：恢复 500，上限 +300。 */
    public static final DeferredItem<ManaPotionItem> MANA_POTION_LV3 =
            ITEMS.registerItem("potion_lv3",
                    p -> new ManaPotionItem(p, 500, 300), new Item.Properties().stacksTo(16));

    /** 超级魔力药水：恢复 1000。 */
    public static final DeferredItem<ManaPotionItem> MANA_POTION_LV4 =
            ITEMS.registerItem("potion_lv4",
                    p -> new ManaPotionItem(p, 1000, 0), new Item.Properties().stacksTo(16));

    /** 终极魔力药水：恢复 2000，上限 +1000。 */
    public static final DeferredItem<ManaPotionItem> MANA_POTION_LV5 =
            ITEMS.registerItem("potion_lv5",
                    p -> new ManaPotionItem(p, 2000, 1000), new Item.Properties().stacksTo(16));

    // ===== 法术卷轴（按法术名命名，贴图沿用颜色命名）=====
    /** 攻击魔法 Zoltraak 卷轴（贴图：scroll_bluepurple）。 */
    public static final DeferredItem<SpellScrollItem> SCROLL_ZOLTRAAK =
            ITEMS.registerItem("scroll_zoltraak",
                    p -> new SpellScrollItem(p, SpellRegistry.get("zoltraak")),
                    new Item.Properties().stacksTo(16));

    /** 防御魔法卷轴（贴图：scroll_violet）。 */
    public static final DeferredItem<SpellScrollItem> SCROLL_DEFENSE =
            ITEMS.registerItem("scroll_defense",
                    p -> new SpellScrollItem(p, SpellRegistry.get("defense")),
                    new Item.Properties().stacksTo(16));

    /** 飞行魔法卷轴（贴图：scroll_skyblue）。 */
    public static final DeferredItem<SpellScrollItem> SCROLL_FLIGHT =
            ITEMS.registerItem("scroll_flight",
                    p -> new SpellScrollItem(p, SpellRegistry.get("flight")),
                    new Item.Properties().stacksTo(16));

    /** 破灭之雷 Judradjim 卷轴（贴图：scroll_gold）。 */
    public static final DeferredItem<SpellScrollItem> SCROLL_JUDRADJIM =
            ITEMS.registerItem("scroll_judradjim",
                    p -> new SpellScrollItem(p, SpellRegistry.get("judradjim")),
                    new Item.Properties().stacksTo(16));

    /** 地狱业火 Vollzanbel 卷轴（贴图：scroll_orange）。 */
    public static final DeferredItem<SpellScrollItem> SCROLL_VOLLZANBEL =
            ITEMS.registerItem("scroll_vollzanbel",
                    p -> new SpellScrollItem(p, SpellRegistry.get("vollzanbel")),
                    new Item.Properties().stacksTo(16));

    /** 破坏黑光卷轴（贴图：scroll_silver）。 */
    public static final DeferredItem<SpellScrollItem> SCROLL_BLACK_LIGHT =
            ITEMS.registerItem("scroll_black_light",
                    p -> new SpellScrollItem(p, SpellRegistry.get("black_light")),
                    new Item.Properties().stacksTo(16));

    /** 魔力挤压 Mana Strike 卷轴（贴图：scroll_pink）。 */
    public static final DeferredItem<SpellScrollItem> SCROLL_MANA_STRIKE =
            ITEMS.registerItem("scroll_mana_strike",
                    p -> new SpellScrollItem(p, SpellRegistry.get("mana_strike")),
                    new Item.Properties().stacksTo(16));

    /** 封印魔法卷轴（贴图：scroll_mint）。 */
    public static final DeferredItem<SpellScrollItem> SCROLL_SEAL =
            ITEMS.registerItem("scroll_seal",
                    p -> new SpellScrollItem(p, SpellRegistry.get("seal")),
                    new Item.Properties().stacksTo(16));

    /** 爆破魔法卷轴（贴图：scroll_cream）。 */
    public static final DeferredItem<SpellScrollItem> SCROLL_BLAST =
            ITEMS.registerItem("scroll_blast",
                    p -> new SpellScrollItem(p, SpellRegistry.get("blast")),
                    new Item.Properties().stacksTo(16));

    /** 刨冰卷轴。 */
    public static final DeferredItem<SpellScrollItem> SCROLL_ICE =
            ITEMS.registerItem("scroll_ice",
                    p -> new SpellScrollItem(p, SpellRegistry.get("shaved_ice")),
                    new Item.Properties().stacksTo(16));

    // ===== 食物 =====
    /** 刨冰：恢复饥饿，给予速度提升。 */
    public static final DeferredItem<ShavedIceItem> SHAVED_ICE =
            ITEMS.registerItem("shaved_ice", ShavedIceItem::new,
                    new Item.Properties().stacksTo(64));

    private ModItems() {
    }
}
