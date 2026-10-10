package yifei.frliliens.magic.quest;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * 委托动态生成与奖励池。
 *
 * <p>使用 {@link CopyOnWriteArrayList} 保证线程安全。
 * 委托目标按权重随机选取，数量在目标指定范围内随机生成，
 * 实现灵活的「大事小事」委托组合。
 */
public final class QuestRegistry {

    /** 击杀目标池。 */
    private static final CopyOnWriteArrayList<QuestTarget> KILL_POOL = new CopyOnWriteArrayList<>();
    /** 收集目标池。 */
    private static final CopyOnWriteArrayList<QuestTarget> COLLECT_POOL = new CopyOnWriteArrayList<>();
    /** 击杀池总权重。 */
    private static final AtomicInteger KILL_TOTAL_WEIGHT = new AtomicInteger(0);
    /** 收集池总权重。 */
    private static final AtomicInteger COLLECT_TOTAL_WEIGHT = new AtomicInteger(0);

    /** 奖励池列表。 */
    private static final CopyOnWriteArrayList<RewardEntry> REWARD_POOL = new CopyOnWriteArrayList<>();
    /** 奖励总权重。 */
    private static final AtomicInteger REWARD_TOTAL_WEIGHT = new AtomicInteger(0);

    static {
        initKillPool();
        initCollectPool();
        initRewards();
    }

    private QuestRegistry() {
    }

    // ===== 击杀目标池 =====

    private static void initKillPool() {
        // 普通：常见敌对生物，数量多
        addTarget(KILL_POOL, KILL_TOTAL_WEIGHT, "minecraft:zombie", 10, 5, 15);
        addTarget(KILL_POOL, KILL_TOTAL_WEIGHT, "minecraft:skeleton", 10, 5, 12);
        addTarget(KILL_POOL, KILL_TOTAL_WEIGHT, "minecraft:spider", 8, 5, 10);
        addTarget(KILL_POOL, KILL_TOTAL_WEIGHT, "minecraft:creeper", 8, 3, 8);
        addTarget(KILL_POOL, KILL_TOTAL_WEIGHT, "minecraft:husk", 6, 5, 12);
        addTarget(KILL_POOL, KILL_TOTAL_WEIGHT, "minecraft:stray", 6, 5, 10);
        addTarget(KILL_POOL, KILL_TOTAL_WEIGHT, "minecraft:cave_spider", 5, 3, 8);
        // 罕见：中等难度，数量适中
        addTarget(KILL_POOL, KILL_TOTAL_WEIGHT, "minecraft:enderman", 5, 3, 6);
        addTarget(KILL_POOL, KILL_TOTAL_WEIGHT, "minecraft:witch", 5, 2, 4);
        addTarget(KILL_POOL, KILL_TOTAL_WEIGHT, "minecraft:drowned", 4, 3, 8);
        addTarget(KILL_POOL, KILL_TOTAL_WEIGHT, "minecraft:pillager", 4, 3, 8);
        addTarget(KILL_POOL, KILL_TOTAL_WEIGHT, "minecraft:vindicator", 3, 2, 5);
        addTarget(KILL_POOL, KILL_TOTAL_WEIGHT, "minecraft:blaze", 4, 2, 5);
        addTarget(KILL_POOL, KILL_TOTAL_WEIGHT, "minecraft:magma_cube", 3, 3, 8);
        // 稀有：高难度，数量少（大委托）
        addTarget(KILL_POOL, KILL_TOTAL_WEIGHT, "minecraft:wither_skeleton", 2, 1, 4);
        addTarget(KILL_POOL, KILL_TOTAL_WEIGHT, "minecraft:ghast", 2, 1, 3);
        addTarget(KILL_POOL, KILL_TOTAL_WEIGHT, "minecraft:evoker", 1, 1, 2);
        addTarget(KILL_POOL, KILL_TOTAL_WEIGHT, "minecraft:ravager", 1, 1, 2);
        addTarget(KILL_POOL, KILL_TOTAL_WEIGHT, "minecraft:warden", 1, 1, 1);
    }

    // ===== 收集目标池 =====

    private static void initCollectPool() {
        // 普通：常见材料，数量多
        addTarget(COLLECT_POOL, COLLECT_TOTAL_WEIGHT, "minecraft:iron_ingot", 10, 8, 32);
        addTarget(COLLECT_POOL, COLLECT_TOTAL_WEIGHT, "minecraft:coal", 10, 16, 48);
        addTarget(COLLECT_POOL, COLLECT_TOTAL_WEIGHT, "minecraft:redstone", 8, 16, 40);
        addTarget(COLLECT_POOL, COLLECT_TOTAL_WEIGHT, "minecraft:copper_ingot", 6, 8, 24);
        addTarget(COLLECT_POOL, COLLECT_TOTAL_WEIGHT, "minecraft:leather", 6, 8, 20);
        addTarget(COLLECT_POOL, COLLECT_TOTAL_WEIGHT, "minecraft:feather", 5, 8, 16);
        addTarget(COLLECT_POOL, COLLECT_TOTAL_WEIGHT, "minecraft:string", 5, 8, 24);
        addTarget(COLLECT_POOL, COLLECT_TOTAL_WEIGHT, "minecraft:bone", 5, 8, 24);
        addTarget(COLLECT_POOL, COLLECT_TOTAL_WEIGHT, "minecraft:gunpowder", 4, 4, 12);
        // 罕见：中等材料
        addTarget(COLLECT_POOL, COLLECT_TOTAL_WEIGHT, "minecraft:gold_ingot", 5, 4, 16);
        addTarget(COLLECT_POOL, COLLECT_TOTAL_WEIGHT, "minecraft:lapis_lazuli", 4, 8, 24);
        addTarget(COLLECT_POOL, COLLECT_TOTAL_WEIGHT, "minecraft:quartz", 4, 8, 24);
        addTarget(COLLECT_POOL, COLLECT_TOTAL_WEIGHT, "minecraft:ender_pearl", 3, 2, 6);
        addTarget(COLLECT_POOL, COLLECT_TOTAL_WEIGHT, "minecraft:blaze_rod", 3, 2, 6);
        addTarget(COLLECT_POOL, COLLECT_TOTAL_WEIGHT, "minecraft:book", 3, 4, 12);
        addTarget(COLLECT_POOL, COLLECT_TOTAL_WEIGHT, "minecraft:glass", 3, 8, 24);
        // 稀有：珍贵材料（大委托）
        addTarget(COLLECT_POOL, COLLECT_TOTAL_WEIGHT, "minecraft:diamond", 2, 1, 5);
        addTarget(COLLECT_POOL, COLLECT_TOTAL_WEIGHT, "minecraft:emerald", 2, 2, 6);
        addTarget(COLLECT_POOL, COLLECT_TOTAL_WEIGHT, "minecraft:netherite_scrap", 1, 1, 2);
        addTarget(COLLECT_POOL, COLLECT_TOTAL_WEIGHT, "minecraft:ancient_debris", 1, 1, 2);
        addTarget(COLLECT_POOL, COLLECT_TOTAL_WEIGHT, "minecraft:shulker_shell", 1, 1, 3);
        addTarget(COLLECT_POOL, COLLECT_TOTAL_WEIGHT, "minecraft:totem_of_undying", 1, 1, 1);
    }

    private static void addTarget(CopyOnWriteArrayList<QuestTarget> pool,
                                   AtomicInteger totalWeight,
                                   String targetId, int weight, int minCount, int maxCount) {
        pool.add(new QuestTarget(targetId, weight, minCount, maxCount));
        totalWeight.addAndGet(weight);
    }

    // ===== 动态委托生成 =====

    /**
     * 动态生成一个委托，排除 lastTargetId 避免连续重复。
     * 原理：50% 概率选击杀/收集，从对应池中按权重选取目标，
     * 在目标的 minCount~maxCount 范围内随机生成需求量。
     */
    public static QuestData generateQuest(ServerLevel level, String lastTargetId) {
        // 随机委托类型（50/50）
        boolean isKill = level.getRandom().nextBoolean();
        CopyOnWriteArrayList<QuestTarget> pool = isKill ? KILL_POOL : COLLECT_POOL;
        AtomicInteger totalWeight = isKill ? KILL_TOTAL_WEIGHT : COLLECT_TOTAL_WEIGHT;
        QuestType type = isKill ? QuestType.KILL : QuestType.COLLECT;

        // 按权重选取目标（排除上次）
        QuestTarget target = rollTarget(pool, totalWeight.get(), level, lastTargetId);

        // 随机生成需求量
        int required = target.rollCount(level);

        // questId 使用 targetId 作为标识（用于不重复逻辑）
        String questId = type.serialName() + ":" + target.targetId();

        return new QuestData(questId, type, target.targetId(), required, 0,
                level.getGameTime(), true, lastTargetId);
    }

    /** 按权重随机选取目标，排除 excludeTargetId。 */
    private static QuestTarget rollTarget(CopyOnWriteArrayList<QuestTarget> pool, int totalWeight,
                                          ServerLevel level, String excludeTargetId) {
        // 过滤排除项
        var filtered = pool.stream()
                .filter(t -> !t.targetId().equals(excludeTargetId))
                .toList();
        if (filtered.isEmpty()) {
            return pool.get(level.getRandom().nextInt(pool.size()));
        }
        int newTotal = filtered.stream().mapToInt(QuestTarget::weight).sum();
        int roll = level.getRandom().nextInt(newTotal);
        int accumulated = 0;
        for (QuestTarget t : filtered) {
            accumulated += t.weight();
            if (roll < accumulated) {
                return t;
            }
        }
        return filtered.get(0);
    }

    // ===== 奖励池 =====

    private static void initRewards() {
        // 低级药水（权重高，常见）
        addReward("friliensmagic:potion_lv1", 1, 2, 30);
        addReward("friliensmagic:potion_lv2", 1, 2, 25);
        // 中级药水 + 原版矿物（中等权重）
        addReward("friliensmagic:potion_lv3", 1, 1, 15);
        addReward("minecraft:iron_ingot", 4, 12, 15);
        addReward("minecraft:gold_ingot", 2, 6, 12);
        addReward("minecraft:redstone", 8, 16, 10);
        addReward("minecraft:diamond", 1, 3, 8);
        addReward("minecraft:emerald", 2, 5, 8);
        addReward("minecraft:lapis_lazuli", 4, 12, 8);
        // 高级药水（低权重）
        addReward("friliensmagic:potion_lv4", 1, 1, 6);
        addReward("friliensmagic:potion_lv5", 1, 1, 3);
        // 卷轴（低权重）
        addReward("friliensmagic:scroll_zoltraak", 1, 1, 4);
        addReward("friliensmagic:scroll_defense", 1, 1, 4);
        addReward("friliensmagic:scroll_flight", 1, 1, 4);
        addReward("friliensmagic:scroll_mana_strike", 1, 1, 3);
        addReward("friliensmagic:scroll_seal", 1, 1, 3);
        addReward("friliensmagic:scroll_blast", 1, 1, 3);
        addReward("friliensmagic:scroll_ice", 1, 1, 3);
        addReward("friliensmagic:scroll_float", 1, 1, 3);
        addReward("friliensmagic:scroll_clean", 1, 1, 3);
        // 稀有卷轴（极低权重）
        addReward("friliensmagic:scroll_judradjim", 1, 1, 1);
        addReward("friliensmagic:scroll_vollzanbel", 1, 1, 1);
        addReward("friliensmagic:scroll_black_light", 1, 1, 1);
    }

    private static void addReward(String itemId, int minCount, int maxCount, int weight) {
        REWARD_POOL.add(new RewardEntry(itemId, minCount, maxCount, weight));
        REWARD_TOTAL_WEIGHT.addAndGet(weight);
    }

    /** 按权重随机一个奖励条目。 */
    public static RewardEntry rollReward(ServerLevel level) {
        int roll = level.getRandom().nextInt(REWARD_TOTAL_WEIGHT.get());
        int accumulated = 0;
        for (RewardEntry entry : REWARD_POOL) {
            accumulated += entry.weight();
            if (roll < accumulated) {
                return entry;
            }
        }
        return REWARD_POOL.get(0);
    }

    // ===== 嵌套 record =====

    /**
     * 委托目标池条目。
     *
     * @param targetId 目标注册 id（实体或物品）
     * @param weight   权重（越高越常见）
     * @param minCount 最少需求量
     * @param maxCount 最多需求量
     */
    public record QuestTarget(String targetId, int weight, int minCount, int maxCount) {
        /** 随机生成需求量。 */
        public int rollCount(ServerLevel level) {
            return minCount + level.getRandom().nextInt(maxCount - minCount + 1);
        }
    }

    /**
     * 奖励池条目。
     *
     * @param itemId    物品注册 id
     * @param minCount  最少数量
     * @param maxCount  最多数量
     * @param weight    权重
     */
    public record RewardEntry(String itemId, int minCount, int maxCount, int weight) {
        /** 按随机源生成物品栈。 */
        public ItemStack createStack(ServerLevel level) {
            int count = minCount + level.getRandom().nextInt(maxCount - minCount + 1);
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
            return new ItemStack(item, count);
        }
    }
}
