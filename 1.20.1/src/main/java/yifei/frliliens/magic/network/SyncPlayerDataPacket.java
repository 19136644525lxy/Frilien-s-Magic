package yifei.frliliens.magic.network;

import java.util.function.Supplier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import yifei.frliliens.magic.capability.ClientPlayerData;
import yifei.frliliens.magic.capability.PlayerDataKinds;

/**
 * 玩家数据同步包（服务端 → 客户端）。
 *
 * <p>载荷是「数据标识 + NBT」，覆盖本模组全部玩家数据，
 * 所以不需要为魔力和护盾等各写一个包类。
 *
 * <p><b>方向限制</b>：注册时指定了 {@code PLAY_TO_CLIENT}，
 * Forge 会在处理前断言方向，客户端无法伪造这个包发给服务端。
 */
public class SyncPlayerDataPacket {

    /** 数据标识，见 {@link PlayerDataKinds}。 */
    private final String kind;

    /** 完整数据快照。 */
    private final CompoundTag data;

    public SyncPlayerDataPacket(String kind, CompoundTag data) {
        this.kind = kind;
        this.data = data;
    }

    // ===== 编解码 =====

    /** 编码：标识用普通字符串，数据用 NBT 复合标签。 */
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(kind);
        buf.writeNbt(data);
    }

    /** 解码。 */
    public static SyncPlayerDataPacket decode(FriendlyByteBuf buf) {
        String kind = buf.readUtf();
        CompoundTag data = buf.readNbt();
        return new SyncPlayerDataPacket(kind, data == null ? new CompoundTag() : data);
    }

    /**
     * 处理：在主线程把数据灌进客户端本地的能力实例。
     *
     * <p>客户端不校验「这个数据是否该存在」——服务端已经限定方向，
     * 而标识也是有限的枚举集合，未知标识直接忽略并记一条日志。
     */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        // 注册时用了 consumerMainThread，这里已在主线程，且包已被标记为已处理
        ClientPlayerData.applyFromServer(kind, data);
    }

    /** 便于排查问题的可读形式。 */
    @Override
    public String toString() {
        return "SyncPlayerDataPacket[" + kind + "]";
    }
}
