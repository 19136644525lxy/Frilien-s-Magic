package yifei.frliliens.magic.quest;

import java.util.List;

import yifei.frliliens.magic.quest.QuestPoolEntry.Reward;
import yifei.frliliens.magic.quest.QuestPoolEntry.Target;

/**
 * 模组的默认委托池。
 *
 * <p>这些是「开箱即用」的默认值，数据包作者可以按 {@code id} 覆盖其中任意一条，
 * 也可以追加新条目。放在 Java 里而不是自带数据包，是为了保证：
 * <ul>
 *   <li>数据包替换整个命名空间时，默认值依然可用（不会因为缺文件而空池）</li>
 *   <li>加载失败时能回退到这里</li>
 * </ul>
 *
 * <p>条目 id 是覆盖用的键，改动时请保持稳定——改 id 等于让已有数据包失效。
 */
public final class BuiltinQuestPools {

    private BuiltinQuestPools() {
    }

    /** 默认击杀目标。 */
    public static List<Target> killTargets() {
        return List.of(
                // 普通：常见敌对生物，数量多
                kill("zombie", "minecraft:zombie", 10, 5, 15),
                kill("skeleton", "minecraft:skeleton", 10, 5, 12),
                kill("spider", "minecraft:spider", 8, 5, 10),
                kill("creeper", "minecraft:creeper", 8, 3, 8),
                kill("husk", "minecraft:husk", 6, 5, 12),
                kill("stray", "minecraft:stray", 6, 5, 10),
                kill("cave_spider", "minecraft:cave_spider", 5, 3, 8),
                // 罕见：中等难度，数量适中
                kill("enderman", "minecraft:enderman", 5, 3, 6),
                kill("witch", "minecraft:witch", 5, 2, 4),
                kill("drowned", "minecraft:drowned", 4, 3, 8),
                kill("pillager", "minecraft:pillager", 4, 3, 8),
                kill("vindicator", "minecraft:vindicator", 3, 2, 5),
                kill("blaze", "minecraft:blaze", 4, 2, 5),
                kill("magma_cube", "minecraft:magma_cube", 3, 3, 8),
                // 稀有：高难度，数量少（大委托）
                kill("wither_skeleton", "minecraft:wither_skeleton", 2, 1, 4),
                kill("ghast", "minecraft:ghast", 2, 1, 3),
                kill("evoker", "minecraft:evoker", 1, 1, 2),
                kill("ravager", "minecraft:ravager", 1, 1, 2),
                kill("warden", "minecraft:warden", 1, 1, 1));
    }

    /** 默认收集目标。 */
    public static List<Target> collectTargets() {
        return List.of(
                // 普通：常见材料，数量多
                collect("iron_ingot", "minecraft:iron_ingot", 10, 8, 32),
                collect("coal", "minecraft:coal", 10, 16, 48),
                collect("redstone", "minecraft:redstone", 8, 16, 40),
                collect("copper_ingot", "minecraft:copper_ingot", 6, 8, 24),
                collect("leather", "minecraft:leather", 6, 8, 20),
                collect("feather", "minecraft:feather", 5, 8, 16),
                collect("string", "minecraft:string", 5, 8, 24),
                collect("bone", "minecraft:bone", 5, 8, 24),
                collect("gunpowder", "minecraft:gunpowder", 4, 4, 12),
                // 罕见：中等材料
                collect("gold_ingot", "minecraft:gold_ingot", 5, 4, 16),
                collect("lapis_lazuli", "minecraft:lapis_lazuli", 4, 8, 24),
                collect("quartz", "minecraft:quartz", 4, 8, 24),
                collect("ender_pearl", "minecraft:ender_pearl", 3, 2, 6),
                collect("blaze_rod", "minecraft:blaze_rod", 3, 2, 6),
                collect("book", "minecraft:book", 3, 4, 12),
                collect("glass", "minecraft:glass", 3, 8, 24),
                // 稀有：珍贵材料（大委托）
                collect("diamond", "minecraft:diamond", 2, 1, 5),
                collect("emerald", "minecraft:emerald", 2, 2, 6),
                collect("netherite_scrap", "minecraft:netherite_scrap", 1, 1, 2),
                collect("ancient_debris", "minecraft:ancient_debris", 1, 1, 2),
                collect("shulker_shell", "minecraft:shulker_shell", 1, 1, 3),
                collect("totem_of_undying", "minecraft:totem_of_undying", 1, 1, 1));
    }

    /** 默认奖励池。 */
    public static List<Reward> rewards() {
        return List.of(
                // 低级药水（权重高，常见，数量多）
                reward("potion_lv1", "friliensmagic:potion_lv1", 2, 5, 30),
                reward("potion_lv2", "friliensmagic:potion_lv2", 2, 5, 25),
                // 中级药水 + 原版矿物（中等权重）
                reward("potion_lv3", "friliensmagic:potion_lv3", 1, 3, 15),
                reward("iron_ingot", "minecraft:iron_ingot", 4, 12, 15),
                reward("gold_ingot", "minecraft:gold_ingot", 2, 6, 12),
                reward("redstone", "minecraft:redstone", 8, 16, 10),
                reward("diamond", "minecraft:diamond", 1, 3, 8),
                reward("emerald", "minecraft:emerald", 2, 5, 8),
                reward("lapis_lazuli", "minecraft:lapis_lazuli", 4, 12, 8),
                // 高级药水（低权重，数量提升）
                reward("potion_lv4", "friliensmagic:potion_lv4", 1, 3, 6),
                reward("potion_lv5", "friliensmagic:potion_lv5", 1, 2, 3),
                // 卷轴（低权重）
                reward("scroll_zoltraak", "friliensmagic:scroll_zoltraak", 1, 1, 4),
                reward("scroll_defense", "friliensmagic:scroll_defense", 1, 1, 4),
                reward("scroll_flight", "friliensmagic:scroll_flight", 1, 1, 4),
                reward("scroll_mana_strike", "friliensmagic:scroll_mana_strike", 1, 1, 3),
                reward("scroll_seal", "friliensmagic:scroll_seal", 1, 1, 3),
                reward("scroll_blast", "friliensmagic:scroll_blast", 1, 1, 3),
                reward("scroll_ice", "friliensmagic:scroll_ice", 1, 1, 3),
                reward("scroll_float", "friliensmagic:scroll_float", 1, 1, 3),
                reward("scroll_clean", "friliensmagic:scroll_clean", 1, 1, 3),
                // 稀有卷轴（极低权重）
                reward("scroll_judradjim", "friliensmagic:scroll_judradjim", 1, 1, 1),
                reward("scroll_vollzanbel", "friliensmagic:scroll_vollzanbel", 1, 1, 1),
                reward("scroll_black_light", "friliensmagic:scroll_black_light", 1, 1, 1));
    }

    private static Target kill(String id, String target, int weight, int min, int max) {
        return new Target(id, QuestType.KILL, target, weight, min, max);
    }

    private static Target collect(String id, String target, int weight, int min, int max) {
        return new Target(id, QuestType.COLLECT, target, weight, min, max);
    }

    private static Reward reward(String id, String item, int min, int max, int weight) {
        return new Reward(id, item, min, max, weight);
    }
}
