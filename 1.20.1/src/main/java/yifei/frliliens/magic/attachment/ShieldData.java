package yifei.frliliens.magic.attachment;

import net.minecraft.nbt.CompoundTag;

/**
 * 防御魔法的护盾数据。
 *
 * <p>以不可变 record 存储，通过 {@link #withActive} 等方法派生新实例。
 * 持久化改为手写 NBT（1.20.1 没有 Codec 数据组件体系）。
 *
 * @param active      护盾是否激活
 * @param strength    当前护盾强度（HP）
 * @param maxStrength 护盾最大强度
 */
public record ShieldData(boolean active, int strength, int maxStrength) {

    /** 默认空护盾（未激活、0 强度）。 */
    public static final ShieldData EMPTY = new ShieldData(false, 0, 0);

    private static final String TAG_ACTIVE = "active";
    private static final String TAG_STRENGTH = "strength";
    private static final String TAG_MAX = "max_strength";

    /** 写入 NBT。 */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(TAG_ACTIVE, active);
        tag.putInt(TAG_STRENGTH, strength);
        tag.putInt(TAG_MAX, maxStrength);
        return tag;
    }

    /** 从 NBT 读取。 */
    public static ShieldData load(CompoundTag tag) {
        if (tag == null) {
            return EMPTY;
        }
        return new ShieldData(tag.getBoolean(TAG_ACTIVE), tag.getInt(TAG_STRENGTH),
                tag.getInt(TAG_MAX));
    }

    /** 护盾是否真正处于激活且有强度。 */
    public boolean isActive() {
        return active && strength > 0;
    }

    /** 护盾强度百分比（0.0 ~ 1.0）。 */
    public float strengthPercent() {
        return maxStrength <= 0 ? 0.0F : Math.min(1.0F, (float) strength / maxStrength);
    }

    public ShieldData withActive(boolean newActive) {
        return new ShieldData(newActive, this.strength, this.maxStrength);
    }

    public ShieldData withStrength(int newStrength) {
        return new ShieldData(this.active, Math.max(0, newStrength), this.maxStrength);
    }

    /**
     * 消耗护盾强度，返回消耗后的新实例。
     *
     * @param amount 要消耗的强度
     * @return 若强度足够，返回消耗后的实例；若不足，返回强度清零且未激活的实例
     */
    public ShieldData consume(int amount) {
        if (amount <= 0) {
            return this;
        }
        int remaining = this.strength - amount;
        if (remaining <= 0) {
            return new ShieldData(false, 0, this.maxStrength);
        }
        return new ShieldData(this.active, remaining, this.maxStrength);
    }
}
