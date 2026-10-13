package yifei.frliliens.magic.quest;

import java.util.List;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;

import yifei.frliliens.magic.FriliensMagic;
import yifei.frliliens.magic.quest.QuestPoolEntry.Reward;
import yifei.frliliens.magic.quest.QuestPoolEntry.Target;

/**
 * 委托池的运行期入口。
 *
 * <p>池内容由 {@link QuestPoolReloadListener} 在数据包重载时填充：
 * 内置默认值来自 {@link BuiltinQuestPools}，数据包条目按 id 覆盖或追加。
 *
 * <p><b>线程安全</b>：三个池都用 {@code volatile} 持有不可变快照
 * （{@link WeightedPool}），重载时构建全新实例再原子替换引用，
 * 读者要么看到旧快照、要么看到新快照，不会看到中间状态。读路径无锁。
 *
 * <p>初始值是内置默认值，所以即使数据包还没加载、或加载失败，委托依然可用。
 */
public final class QuestRegistry {

    private static volatile WeightedPool<Target> killPool =
            WeightedPool.of(BuiltinQuestPools.killTargets(), Target::weight);
    private static volatile WeightedPool<Target> collectPool =
            WeightedPool.of(BuiltinQuestPools.collectTargets(), Target::weight);
    private static volatile WeightedPool<Reward> rewardPool =
            WeightedPool.of(BuiltinQuestPools.rewards(), Reward::weight);

    private QuestRegistry() {
    }

    /** 由重载监听器调用：原子替换三个池。 */
    static void setPools(WeightedPool<Target> kill, WeightedPool<Target> collect,
                         WeightedPool<Reward> rewards) {
        killPool = kill;
        collectPool = collect;
        rewardPool = rewards;
    }

    // ===== 供自检与调试读取 =====

    /** 全部击杀目标。 */
    public static List<Target> allKillTargets() {
        return killPool.asList();
    }

    /** 全部收集目标。 */
    public static List<Target> allCollectTargets() {
        return collectPool.asList();
    }

    /** 全部奖励条目。 */
    public static List<Reward> allRewards() {
        return rewardPool.asList();
    }

    // ===== 动态委托生成 =====

    /**
     * 动态生成一个委托，排除 lastTargetId 避免连续重复。
     *
     * <p>50% 概率选击杀/收集，从对应池按权重选取目标，
     * 在目标的 min~max 范围内随机生成需求量。
     *
     * @param level         服务端世界（提供随机源）
     * @param lastTargetId  上次的委托目标 id，用于避免连续重复；可为 null
     * @param villagerUuid  发布委托的村民
     * @return 委托数据
     */
    public static QuestData generateQuest(ServerLevel level, String lastTargetId,
                                          UUID villagerUuid) {
        boolean isKill = level.getRandom().nextBoolean();
        WeightedPool<Target> pool = isKill ? killPool : collectPool;

        Target target = pool.pickExcluding(level.getRandom(), lastTargetId, Target::id);
        if (target == null) {
            // 池为空理论上不会发生（重载时会回退默认值），这里只是防御
            FriliensMagic.LOGGER.error("委托池为空，无法生成委托");
            return null;
        }

        int required = target.rollCount(level.getRandom());
        String questId = target.type().serialName() + ":" + target.target();

        return new QuestData(questId, target.type(), target.target(), required, 0,
                level.getGameTime(), true, lastTargetId, villagerUuid.toString());
    }

    // ===== 奖励 =====

    /**
     * 按权重随机一个奖励条目。
     *
     * @param level 服务端世界
     * @return 奖励条目；池为空时返回 {@code null}
     */
    public static Reward rollReward(ServerLevel level) {
        return rewardPool.pick(level.getRandom());
    }
}
