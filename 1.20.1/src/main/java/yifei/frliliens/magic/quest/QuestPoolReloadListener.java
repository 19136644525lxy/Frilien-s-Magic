package yifei.frliliens.magic.quest;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import yifei.frliliens.magic.FriliensMagic;
import yifei.frliliens.magic.quest.QuestPoolEntry.Reward;
import yifei.frliliens.magic.quest.QuestPoolEntry.Target;

/**
 * 从数据包加载委托池。
 *
 * <p>路径：{@code data/<命名空间>/friliensmagic/quest_pools/{kill,collect,rewards}.json}
 * <br>格式：{@code {"entries": [ {...}, {...} ]}}
 *
 * <p>内置默认值先加载，数据包条目按 {@code id} 覆盖同名项、追加新项。
 * 解析细节与容错见 {@link QuestPoolReader}。
 *
 * <p>池为空时回退内置默认值并记日志——宁可跑默认内容，也不让委托系统瘫痪。
 * 载入通报交给 {@link QuestPoolAnnouncer}，它会在 reload 结束后聚合广播一次。
 */
public class QuestPoolReloadListener implements PreparableReloadListener {

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager,
                                          ProfilerFiller prepProfiler, ProfilerFiller reloadProfiler,
                                          Executor background, Executor gameThread) {
        // 解析放到后台线程，避免数据包很大时卡主线程
        return CompletableFuture
                .supplyAsync(() -> parse(manager), background)
                .thenCompose(barrier::wait)
                .thenAcceptAsync(this::publish, gameThread);
    }

    /** 解析结果：三个池的条目 + 有贡献的命名空间。 */
    private record Parsed(QuestPoolReader.PoolResult<Target> kill,
                          QuestPoolReader.PoolResult<Target> collect,
                          QuestPoolReader.PoolResult<Reward> rewards) {
    }

    /** 后台线程：读文件、解析、与内置合并。 */
    private Parsed parse(ResourceManager manager) {
        var kill = QuestPoolReader.read(manager, "kill.json", Target.CODEC.codec(),
                QuestType.KILL, BuiltinQuestPools.killTargets(), Target::id, "kill");
        var collect = QuestPoolReader.read(manager, "collect.json", Target.CODEC.codec(),
                QuestType.COLLECT, BuiltinQuestPools.collectTargets(), Target::id, "collect");
        var rewards = QuestPoolReader.read(manager, "rewards.json", Reward.CODEC.codec(),
                null, BuiltinQuestPools.rewards(), Reward::id, "rewards");
        return new Parsed(kill, collect, rewards);
    }

    /** 主线程：原子替换池快照，并登记待通报的命名空间。 */
    private void publish(Parsed parsed) {
        List<Target> kill = parsed.kill().entries();
        List<Target> collect = parsed.collect().entries();
        List<Reward> rewards = parsed.rewards().entries();

        // 兜底：空池会让随机选取抛异常，回退内置默认值
        boolean fellBack = false;
        if (kill.isEmpty()) {
            kill = BuiltinQuestPools.killTargets();
            fellBack = true;
        }
        if (collect.isEmpty()) {
            collect = BuiltinQuestPools.collectTargets();
            fellBack = true;
        }
        if (rewards.isEmpty()) {
            rewards = BuiltinQuestPools.rewards();
            fellBack = true;
        }
        if (fellBack) {
            FriliensMagic.LOGGER.warn("Quest pool is empty, falling back to built-in defaults"
                + " (check whether a datapack removed too much)");
        }

        QuestRegistry.setPools(
                WeightedPool.of(kill, Target::weight),
                WeightedPool.of(collect, Target::weight),
                WeightedPool.of(rewards, Reward::weight));

        FriliensMagic.LOGGER.info("Loaded quest pools: {} kill, {} collect, {} reward entries",
                kill.size(), collect.size(), rewards.size());

        // 只把「真的贡献了条目」的命名空间登记为待通报，避免只放了个空文件也通报
        Set<String> namespaces = QuestPoolReader.union(
                parsed.kill(), parsed.collect(), parsed.rewards());
        QuestPoolAnnouncer.markLoaded(namespaces);
    }
}
