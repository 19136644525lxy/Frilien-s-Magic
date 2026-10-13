package yifei.frliliens.magic.attachment;

import net.minecraft.nbt.CompoundTag;

/**
 * 飞行状态数据。
 *
 * <p>存储玩家是否处于魔法飞行状态。
 * 不跨死亡保留 —— 死亡后 capability 走默认值（未飞行）。
 *
 * @param flying 是否处于魔法飞行
 */
public record FlightData(boolean flying) {

    /** 默认未飞行。 */
    public static final FlightData INACTIVE = new FlightData(false);

    private static final String TAG_FLYING = "flying";

    /** 写入 NBT。 */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(TAG_FLYING, flying);
        return tag;
    }

    /** 从 NBT 读取。 */
    public static FlightData load(CompoundTag tag) {
        if (tag == null) {
            return INACTIVE;
        }
        return new FlightData(tag.getBoolean(TAG_FLYING));
    }
}
