package yifei.frliliens.magic.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 飞行状态数据。
 *
 * <p>存储玩家是否处于魔法飞行状态。
 * 不 {@code copyOnDeath}，死亡后自动清除。
 *
 * @param flying 是否处于魔法飞行
 */
public record FlightData(boolean flying) {

    /** 默认未飞行。 */
    public static final FlightData INACTIVE = new FlightData(false);

    /** Codec：持久化到磁盘。 */
    public static final Codec<FlightData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.fieldOf("flying").forGetter(FlightData::flying)
            ).apply(instance, FlightData::new));

    /** StreamCodec：网络同步到客户端。 */
    public static final StreamCodec<ByteBuf, FlightData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, FlightData::flying,
            FlightData::new
    );
}
