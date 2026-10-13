package yifei.frliliens.magic.attachment;

import net.minecraft.nbt.CompoundTag;

/**
 * 玩家魔力数据。
 *
 * <p>以不可变 record 存储，通过 {@link #withCurrent}/{@link #withMax} 派生新实例。
 *
 * <p><b>与 1.21 NeoForge 版的差异</b>：那边用 {@code Codec} + {@code StreamCodec}
 * 交给 {@code AttachmentType} 自动序列化与同步；1.20.1 没有附件系统也没有数据组件，
 * 所以这里改成手写 NBT 读写（{@link #save}/{@link #load}），
 * 网络同步由 capability 包里的同步包负责。
 *
 * @param current 当前魔力值
 * @param max     魔力上限（受硬上限 {@link #HARD_MAX} 约束，后续可由物品提升）
 */
public record ManaData(int current, int max) {

    /** 魔力硬上限，任何来源都不能超过此值。 */
    public static final int HARD_MAX = 9_999_999;

    /** 初始魔力：当前值与上限均为 1000。 */
    public static final ManaData INITIAL = new ManaData(1000, 1000);

    private static final String TAG_CURRENT = "current";
    private static final String TAG_MAX = "max";

    /** 写入 NBT。 */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_CURRENT, current);
        tag.putInt(TAG_MAX, max);
        return tag;
    }

    /** 从 NBT 读取；缺失或非法字段回退初始值。 */
    public static ManaData load(CompoundTag tag) {
        if (tag == null || !tag.contains(TAG_MAX)) {
            return INITIAL;
        }
        int max = tag.getInt(TAG_MAX);
        if (max <= 0) {
            max = INITIAL.max();
        }
        return new ManaData(tag.getInt(TAG_CURRENT), max);
    }

    /** 魔力百分比（0.0 ~ 1.0）。 */
    public float percent() {
        return max <= 0 ? 0.0F : Math.min(1.0F, (float) current / max);
    }

    /** 魔力是否已满。 */
    public boolean isFull() {
        return current >= max;
    }

    public ManaData withCurrent(int newCurrent) {
        return new ManaData(Math.max(0, Math.min(max, newCurrent)), this.max);
    }

    public ManaData withMax(int newMax) {
        int clamped = Math.max(1, Math.min(HARD_MAX, newMax));
        return new ManaData(Math.min(this.current, clamped), clamped);
    }

    /**
     * 消耗魔力，返回消耗后的新实例。
     *
     * @param amount 要消耗的魔力量
     * @return 若魔力足够，返回消耗后的实例；若不足，返回原值（不消耗）
     */
    public ManaData consume(int amount) {
        if (amount <= 0 || current < amount) {
            return this;
        }
        return new ManaData(current - amount, max);
    }

    /**
     * 恢复魔力，返回恢复后的新实例。
     *
     * @param amount 要恢复的魔力量
     */
    public ManaData regen(int amount) {
        if (amount <= 0) {
            return this;
        }
        return new ManaData(Math.min(max, current + amount), max);
    }
}
