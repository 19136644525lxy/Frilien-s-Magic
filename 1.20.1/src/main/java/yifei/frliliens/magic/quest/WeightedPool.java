package yifei.frliliens.magic.quest;

import java.util.Arrays;
import java.util.List;

import net.minecraft.util.RandomSource;

/**
 * 委托池的不可变快照。
 *
 * <p>设计要点（性能相关）：
 * <ul>
 *   <li><b>不可变</b>——reload 时构建一个全新的实例，然后原子替换引用，
 *       读者永远看到完整数据，不会读到「清空了一半」的池子。</li>
 *   <li><b>预计算前缀和</b>——按权重随机从线性累加变成二分查找，
 *       且运行时零分配（旧实现每次都要新建一个过滤后的 List）。</li>
 *   <li>用数组而非 {@code List}，避免装箱与迭代器开销。</li>
 * </ul>
 *
 * @param entries 条目数组（按权重选择）
 * @param prefix  前缀和数组，{@code prefix[i]} 是前 i+1 个条目的权重累计
 * @param total   权重总和
 * @param <T>     条目类型
 */
public record WeightedPool<T>(T[] entries, int[] prefix, int total) {

    /** 空池。 */
    @SuppressWarnings("unchecked")
    public static <T> WeightedPool<T> empty() {
        return new WeightedPool<>((T[]) new Object[0], new int[0], 0);
    }

    /**
     * 由条目列表构建，权重取自 {@code weigher}。
     *
     * <p>权重会取下限 1，避免 0 权重条目导致 {@code nextInt(0)} 抛异常。
     */
    public static <T> WeightedPool<T> of(List<T> list, java.util.function.ToIntFunction<T> weigher) {
        if (list.isEmpty()) {
            return empty();
        }
        @SuppressWarnings("unchecked")
        T[] entries = (T[]) new Object[list.size()];
        int[] prefix = new int[list.size()];
        int running = 0;
        for (int i = 0; i < list.size(); i++) {
            T e = list.get(i);
            entries[i] = e;
            running += Math.max(1, weigher.applyAsInt(e));
            prefix[i] = running;
        }
        return new WeightedPool<>(entries, prefix, running);
    }

    /** 是否没有条目。 */
    public boolean isEmpty() {
        return entries.length == 0;
    }

    /** 条目数量。 */
    public int size() {
        return entries.length;
    }

    /** 全部条目（只读视图）。 */
    public List<T> asList() {
        return List.of(entries);
    }

    /**
     * 按权重随机取一个条目。
     *
     * <p>用二分查找定位：{@code prefix} 是递增的，找到第一个
     * {@code prefix[i] > roll} 的 i 即可。
     *
     * @param random 随机源
     * @return 选中的条目；池为空时返回 {@code null}
     */
    public T pick(RandomSource random) {
        if (total <= 0) {
            return null;
        }
        int roll = random.nextInt(total);
        int idx = Arrays.binarySearch(prefix, roll);
        // binarySearch 未命中时返回 -(插入点) - 1，插入点即第一个 > roll 的下标
        return entries[idx >= 0 ? idx : -idx - 1];
    }

    /**
     * 按权重随机取一个条目，排除指定 id（用于避免连续重复）。
     *
     * <p>做法是「重掷」而不是新建过滤列表：委托池很小、排除项最多一个，
     * 几次重掷就能命中，而且运行时零分配。重掷上限防止池里只有一条时死循环。
     *
     * @param random   随机源
     * @param excludeId 要排除的条目 id，可为 null
     * @param idOf     取条目 id 的函数
     * @return 选中的条目；池为空时返回 {@code null}
     */
    public T pickExcluding(RandomSource random, String excludeId,
                           java.util.function.Function<T, String> idOf) {
        if (isEmpty()) {
            return null;
        }
        if (excludeId == null || entries.length == 1) {
            return pick(random);
        }
        for (int attempt = 0; attempt < 8; attempt++) {
            T candidate = pick(random);
            if (candidate != null && !excludeId.equals(idOf.apply(candidate))) {
                return candidate;
            }
        }
        // 池子里几乎全是同一个 id，兜底返回第一个不匹配的
        for (T e : entries) {
            if (!excludeId.equals(idOf.apply(e))) {
                return e;
            }
        }
        return pick(random);
    }
}
