package yifei.frliliens.magic.item;

import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import yifei.frliliens.magic.FriliensMagic;
import yifei.frliliens.magic.entity.ModEntities;
import yifei.frliliens.magic.spell.SpellRegistry;

/**
 * 物品注册。
 *
 * <p>每个条目都需要 {@code assets/friliensmagic/} 下三样配套资源：
 * {@code models/item/<name>.json}、{@code textures/item/<name>.png}，
 * 以及 {@code lang/en_us.json} 与 {@code lang/zh_cn.json} 里的名称。
 *
 * <h2>与 1.21 版的差异</h2>
 * <ul>
 *   <li>{@code DeferredRegister.Items} / {@code DeferredItem} → 通用的
 *       {@link DeferredRegister} + {@link RegistryObject}</li>
 *   <li>没有 {@code registerItem(..., properties)} 那种便利重载，
 *       物品属性直接在工厂里构造</li>
 *   <li>刷怪蛋用 {@code ForgeSpawnEggItem}（1.21 是 {@code DeferredSpawnEggItem}）</li>
 * </ul>
 */
public final class ModItems {

    /** 物品注册器，注册到 mod 事件总线。 */
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FriliensMagic.MODID);

    // ===== 法杖 =====

    /** 芙莉莲的法杖：月牙形杖头配红色宝石。 */
    public static final RegistryObject<StaffItem> FRIEREN_STAFF =
            ITEMS.register("frieren_staff",
                    () -> new StaffItem(new Item.Properties().stacksTo(1)));

    /** 菲伦的法杖：弯曲的缠布法杖。 */
    public static final RegistryObject<StaffItem> PHIREN_STAFF =
            ITEMS.register("phiren_staff",
                    () -> new StaffItem(new Item.Properties().stacksTo(1)));

    // ===== 魔力药水（1/3/5 提升上限，2/4 恢复量递增）=====

    /** 初级魔力药水：恢复 100，上限 +100。 */
    public static final RegistryObject<ManaPotionItem> MANA_POTION_LV1 =
            ITEMS.register("potion_lv1",
                    () -> new ManaPotionItem(new Item.Properties().stacksTo(16), 100, 100));

    /** 中级魔力药水：恢复 300。 */
    public static final RegistryObject<ManaPotionItem> MANA_POTION_LV2 =
            ITEMS.register("potion_lv2",
                    () -> new ManaPotionItem(new Item.Properties().stacksTo(16), 300, 0));

    /** 高级魔力药水：恢复 500，上限 +300。 */
    public static final RegistryObject<ManaPotionItem> MANA_POTION_LV3 =
            ITEMS.register("potion_lv3",
                    () -> new ManaPotionItem(new Item.Properties().stacksTo(16), 500, 300));

    /** 超级魔力药水：恢复 1000。 */
    public static final RegistryObject<ManaPotionItem> MANA_POTION_LV4 =
            ITEMS.register("potion_lv4",
                    () -> new ManaPotionItem(new Item.Properties().stacksTo(16), 1000, 0));

    /** 终极魔力药水：恢复 2000，上限 +1000。 */
    public static final RegistryObject<ManaPotionItem> MANA_POTION_LV5 =
            ITEMS.register("potion_lv5",
                    () -> new ManaPotionItem(new Item.Properties().stacksTo(16), 2000, 1000));

    // ===== 法术卷轴 =====

    /** 攻击魔法 Zoltraak 卷轴。 */
    public static final RegistryObject<SpellScrollItem> SCROLL_ZOLTRAAK =
            ITEMS.register("scroll_zoltraak",
                    () -> new SpellScrollItem(new Item.Properties().stacksTo(16),
                            SpellRegistry.get("zoltraak")));

    /** 防御魔法卷轴。 */
    public static final RegistryObject<SpellScrollItem> SCROLL_DEFENSE =
            ITEMS.register("scroll_defense",
                    () -> new SpellScrollItem(new Item.Properties().stacksTo(16),
                            SpellRegistry.get("defense")));

    /** 飞行魔法卷轴。 */
    public static final RegistryObject<SpellScrollItem> SCROLL_FLIGHT =
            ITEMS.register("scroll_flight",
                    () -> new SpellScrollItem(new Item.Properties().stacksTo(16),
                            SpellRegistry.get("flight")));

    /** 破灭之雷 Judradjim 卷轴。 */
    public static final RegistryObject<SpellScrollItem> SCROLL_JUDRADJIM =
            ITEMS.register("scroll_judradjim",
                    () -> new SpellScrollItem(new Item.Properties().stacksTo(16),
                            SpellRegistry.get("judradjim")));

    /** 地狱业火 Vollzanbel 卷轴。 */
    public static final RegistryObject<SpellScrollItem> SCROLL_VOLLZANBEL =
            ITEMS.register("scroll_vollzanbel",
                    () -> new SpellScrollItem(new Item.Properties().stacksTo(16),
                            SpellRegistry.get("vollzanbel")));

    /** 破坏黑光卷轴。 */
    public static final RegistryObject<SpellScrollItem> SCROLL_BLACK_LIGHT =
            ITEMS.register("scroll_black_light",
                    () -> new SpellScrollItem(new Item.Properties().stacksTo(16),
                            SpellRegistry.get("black_light")));

    /** 魔力挤压 Mana Strike 卷轴。 */
    public static final RegistryObject<SpellScrollItem> SCROLL_MANA_STRIKE =
            ITEMS.register("scroll_mana_strike",
                    () -> new SpellScrollItem(new Item.Properties().stacksTo(16),
                            SpellRegistry.get("mana_strike")));

    /** 封印魔法卷轴。 */
    public static final RegistryObject<SpellScrollItem> SCROLL_SEAL =
            ITEMS.register("scroll_seal",
                    () -> new SpellScrollItem(new Item.Properties().stacksTo(16),
                            SpellRegistry.get("seal")));

    /** 爆破魔法卷轴。 */
    public static final RegistryObject<SpellScrollItem> SCROLL_BLAST =
            ITEMS.register("scroll_blast",
                    () -> new SpellScrollItem(new Item.Properties().stacksTo(16),
                            SpellRegistry.get("blast")));

    /** 刨冰卷轴。 */
    public static final RegistryObject<SpellScrollItem> SCROLL_ICE =
            ITEMS.register("scroll_ice",
                    () -> new SpellScrollItem(new Item.Properties().stacksTo(16),
                            SpellRegistry.get("shaved_ice")));

    /** 漂浮魔法卷轴。 */
    public static final RegistryObject<SpellScrollItem> SCROLL_FLOAT =
            ITEMS.register("scroll_float",
                    () -> new SpellScrollItem(new Item.Properties().stacksTo(16),
                            SpellRegistry.get("float")));

    /** 清洁魔法卷轴。 */
    public static final RegistryObject<SpellScrollItem> SCROLL_CLEAN =
            ITEMS.register("scroll_clean",
                    () -> new SpellScrollItem(new Item.Properties().stacksTo(16),
                            SpellRegistry.get("clean")));

    // ===== 食物 =====

    /** 刨冰：恢复饥饿，给予速度提升。 */
    public static final RegistryObject<ShavedIceItem> SHAVED_ICE =
            ITEMS.register("shaved_ice",
                    () -> new ShavedIceItem(new Item.Properties().stacksTo(64)));

    // ===== 刷怪蛋 =====

    /** 委托村民刷怪蛋：直接生成已标记的委托村民。 */
    public static final RegistryObject<ForgeSpawnEggItem> QUEST_VILLAGER_SPAWN_EGG =
            ITEMS.register("quest_villager_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.QUEST_VILLAGER,
                            0x9370DB, 0xFFD700, new Item.Properties()));

    private ModItems() {
    }
}
