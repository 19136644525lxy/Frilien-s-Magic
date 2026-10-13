package yifei.frliliens.magic.quest;

import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 委托池的条目定义与 JSON 编解码。
 *
 * <p>数据包路径：{@code data/<命名空间>/friliensmagic/quest_pools/{kill,collect,rewards}.json}
 *
 * <p>合并规则：模组内置条目先加载，数据包条目按 {@link #id()} <b>覆盖</b>同名条目，
 * 没有同名则追加。所以数据包只需写要改的部分。
 */
public final class QuestPoolEntry {

    /** 数量范围与权重都要做基本校验，避免运行时 nextInt(0) 之类的问题。 */
    private static final Codec<Integer> COUNT = Codec.intRange(1, 1_000_000);
    private static final Codec<Integer> WEIGHT = Codec.intRange(1, 1_000_000);

    private QuestPoolEntry() {
    }

    /**
     * 委托目标（击杀或收集）。
     *
     * @param id      条目 id，数据包覆盖时的键。建议用短横线小写，如 {@code zombie}
     * @param type    目标类型，{@code kill} 或 {@code collect}
     * @param target  目标注册 id，击杀填实体、收集填物品
     * @param weight  权重，越高越常见
     * @param minCount 最少需求量
     * @param maxCount 最多需求量
     */
    public record Target(String id, QuestType type, String target, int weight,
                         int minCount, int maxCount) {

        private static final MapCodec<Target> RAW = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Codec.STRING.fieldOf("id").forGetter(Target::id),
                QuestType.CODEC.forGetter(Target::type),
                Codec.STRING.fieldOf("target").forGetter(Target::target),
                WEIGHT.optionalFieldOf("weight", 1).forGetter(Target::weight),
                COUNT.fieldOf("min").forGetter(Target::minCount),
                COUNT.fieldOf("max").forGetter(Target::maxCount)
        ).apply(inst, Target::new));

        /**
         * 对外编解码器。
         *
         * <p><b>与 1.21 版的差异</b>：那边用 {@code RAW.validate(...)} 把
         * {@code min <= max} 的校验挂进 codec；1.20.1 用的 DataFixerUpper 6.0.8
         * <b>没有</b> {@code validate}（那是 DFU 10.x 才加的），
         * 所以校验改由 {@code QuestPoolReader} 在解析后逐条做——
         * 那里能给出更清楚的日志（指明是哪个文件的第几条）。
         */
        public static final MapCodec<Target> CODEC = RAW;

        /** 在 [min, max] 内随机一个需求量。 */
        public int rollCount(RandomSource random) {
            return minCount + random.nextInt(maxCount - minCount + 1);
        }
    }

    /**
     * 奖励条目。
     *
     * @param id       条目 id，数据包覆盖时的键
     * @param item     物品注册 id
     * @param minCount 最少数量
     * @param maxCount 最多数量
     * @param weight   权重
     */
    public record Reward(String id, String item, int minCount, int maxCount, int weight) {

        private static final MapCodec<Reward> RAW = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Codec.STRING.fieldOf("id").forGetter(Reward::id),
                Codec.STRING.fieldOf("item").forGetter(Reward::item),
                COUNT.fieldOf("min").forGetter(Reward::minCount),
                COUNT.fieldOf("max").forGetter(Reward::maxCount),
                WEIGHT.optionalFieldOf("weight", 1).forGetter(Reward::weight)
        ).apply(inst, Reward::new));

        /** 对外编解码器。校验说明见 {@link Target#CODEC}。 */
        public static final MapCodec<Reward> CODEC = RAW;

        /**
         * 按随机数量生成物品栈。
         *
         * <p>物品 id 不存在时返回 {@link ItemStack#EMPTY}，调用方可以跳过，
         * 不至于发出一个空物品。
         */
        public ItemStack createStack(RandomSource random) {
            ResourceLocation key = ResourceLocation.tryParse(item);
            if (key == null) {
                return ItemStack.EMPTY;
            }
            Item resolved = BuiltInRegistries.ITEM.get(key);
            if (resolved == Items.AIR) {
                return ItemStack.EMPTY;
            }
            int count = minCount + random.nextInt(maxCount - minCount + 1);
            return new ItemStack(resolved, count);
        }
    }

    /** 校验 min <= max。 */
    private static <T> DataResult<T> validateRange(T value) {
        int min;
        int max;
        if (value instanceof Target t) {
            min = t.minCount();
            max = t.maxCount();
        } else if (value instanceof Reward r) {
            min = r.minCount();
            max = r.maxCount();
        } else {
            return DataResult.success(value);
        }
        if (min > max) {
            return DataResult.error(() -> "min (" + min + ") is greater than max (" + max + ")");
        }
        return DataResult.success(value);
    }

    /** 供自检使用的 id 提取函数。 */
    public static final Function<Target, String> TARGET_ID = Target::id;

    /** 供自检使用的 id 提取函数。 */
    public static final Function<Reward, String> REWARD_ID = Reward::id;
}
