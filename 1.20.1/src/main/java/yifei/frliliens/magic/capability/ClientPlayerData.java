package yifei.frliliens.magic.capability;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import yifei.frliliens.magic.FriliensMagic;

/**
 * 客户端侧的玩家数据缓存。
 *
 * <p>1.20.1 Forge 的 capability <b>不会</b>自动同步到客户端，
 * 所以客户端拿不到服务端的真实数据（魔力、护盾等都只在服务端权威）。
 * 这里保存服务端通过 {@link yifei.frliliens.magic.network.SyncPlayerDataPacket}
 * 推过来的快照，供渲染与界面读取。
 *
 * <p>对应 1.21 的写法是直接 {@code player.getData(ModAttachments.MANA)}——
 * 那边附件带 {@code sync()}，客户端本地就有数据。
 *
 * <p><b>为什么按 UUID 建表而不是只存自己</b>：护盾渲染需要显示<b>其他玩家</b>的护盾，
 * 服务端会把护盾数据发给所有能看见该玩家的人，所以客户端要能存多份。
 *
 * <p><b>内存管理</b>：玩家离开视野后数据不会被自动清理。
 * 每个条目只有几十字节，且玩家数量有限，所以不做主动回收；
 * 登出时由 {@link #clearAll()} 清空（单人游戏等同于退出存档）。
 */
public final class ClientPlayerData {

    /** UUID → (标识 → 数据快照)。只在客户端被填充。 */
    private static final Map<UUID, Map<String, CompoundTag>> CACHE = new ConcurrentHashMap<>();

    /** 玩家自己的 UUID。服务端推自己的数据时用来定位；未登录时为 null。 */
    private static volatile UUID localPlayerId;

    private ClientPlayerData() {
    }

    /** 记下本地玩家的 UUID（进入世界时调用）。 */
    public static void setLocalPlayer(Player player) {
        localPlayerId = player == null ? null : player.getUUID();
    }

    /** 取本地玩家的 UUID。 */
    public static UUID localPlayerId() {
        return localPlayerId;
    }

    /** 清空全部缓存（退出世界时调用）。 */
    public static void clearAll() {
        CACHE.clear();
        localPlayerId = null;
    }

    /**
     * 应用服务端推来的数据。
     *
     * <p>由 {@code SyncPlayerDataPacket} 在主线程调用。
     * 未知标识直接忽略——可能是更新的另一端发来的（协议版本一致时不该发生），
     * 记一条日志便于排查。
     *
     * @param kind 数据标识
     * @param data 完整快照
     */
    public static void applyFromServer(String kind, CompoundTag data) {
        if (!PlayerDataKinds.exists(kind)) {
            FriliensMagic.LOGGER.warn("ignoring sync packet for unknown player data '{}'", kind);
            return;
        }
        UUID id = localPlayerId;
        if (id == null) {
            // 还没进入世界就收到同步包，说明时序异常
            FriliensMagic.LOGGER.debug("sync packet for '{}' arrived before local player was set",
                    kind);
            return;
        }
        CACHE.computeIfAbsent(id, k -> new ConcurrentHashMap<>()).put(kind, data);
    }

    /**
     * 读某项数据。
     *
     * <p>没有收到过同步包时返回 null，调用方应当退回默认值——
     * 例如 {@code ManaData.INITIAL}。这样渲染在数据到达前也有合理表现。
     *
     * @param player 目标玩家
     * @param kind   数据标识
     * @return 数据快照；没有则 null
     */
    public static CompoundTag get(Player player, String kind) {
        if (player == null) {
            return null;
        }
        Map<String, CompoundTag> byKind = CACHE.get(player.getUUID());
        return byKind == null ? null : byKind.get(kind);
    }

    /** 是否已经收到过某项数据。 */
    public static boolean has(Player player, String kind) {
        return get(player, kind) != null;
    }

    /** 便于诊断：当前缓存了多少个玩家的数据。 */
    public static int cachedPlayerCount() {
        return CACHE.size();
    }
}
