package yifei.frliliens.magic.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 防御魔法的护盾数据。
 *
 * <p>以不可变 record 存储，通过 {@link #withActive} 等方法派生新实例，
 * 配合 NeoForge {@code AttachmentType} 的自动序列化与客户端同步。
 *
 * @param active      护盾是否激活
 * @param strength    当前护盾强度（HP）
 * @param maxStrength 护盾最大强度
 */
public record ShieldData(boolean active, int strength, int maxStrength) {

    /** 默认空护盾（未激活、0 强度）。 */
    public static final ShieldData EMPTY = new ShieldData(false, 0, 0);

    /** Codec：持久化到磁盘。 */
    public static final Codec<ShieldData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("active").forGetter(ShieldData::active),
            Codec.INT.fieldOf("strength").forGetter(ShieldData::strength),
            Codec.INT.fieldOf("max_strength").forGetter(ShieldData::maxStrength)
    ).apply(instance, ShieldData::new));

    /** StreamCodec：网络同步到客户端。 */
    public static final StreamCodec<ByteBuf, ShieldData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ShieldData::active,
            ByteBufCodecs.VAR_INT, ShieldData::strength,
            ByteBufCodecs.VAR_INT, ShieldData::maxStrength,
            ShieldData::new
    );

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
