package yifei.frliliens.magic.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import yifei.frliliens.magic.FriliensMagic;
import yifei.frliliens.magic.capability.PlayerDataCapability;

/**
 * 网络层。
 *
 * <p>1.20.1 Forge 用 {@link SimpleChannel} + 手写编解码，
 * 而 1.21 NeoForge 用 {@code CustomPacketPayload} + {@code RegisterPayloadHandlersEvent}——
 * 两套完全不同的 API，无法机械替换。
 *
 * <p><b>用统一的同步包而不是每个数据类一个包</b>：
 * 本模组有 7 项玩家数据（魔力、护盾、已学法术、选中法术、飞行、委托、村民标记），
 * 每项写一套「包类 + 编码器 + 解码器 + 处理器」会有大量重复样板。
 * 由于这些数据都能完整表达成 NBT，所以只用一个
 * {@link SyncPlayerDataPacket}（能力标识 + NBT）即可覆盖全部，
 * 客户端按标识查表还原。
 *
 * <p>对应的 1.21 写法是 {@code player.syncData(ModAttachments.MANA)}——
 * 那边由附件系统自动同步；1.20.1 必须显式发包。
 */
public final class ModNetwork {

    /** 协议版本。两端不一致时 Forge 会拒绝连接，便于排查版本错配。 */
    private static final String PROTOCOL_VERSION = "1";

    /** 唯一的频道。 */
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(FriliensMagic.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    /** 包 id 分配。集中在这里，避免撞号。 */
    private static final int ID_SYNC_PLAYER_DATA = 0;
    private static final int ID_CAST_BASIC_ABILITY = 1;
    private static final int ID_SELECT_SPELL = 2;

    private static boolean registered;

    /** 注册全部包。在 {@code FMLCommonSetupEvent} 里调用一次。 */
    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        // 服务端 → 客户端：玩家数据同步
        CHANNEL.messageBuilder(SyncPlayerDataPacket.class, ID_SYNC_PLAYER_DATA,
                        NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncPlayerDataPacket::encode)
                .decoder(SyncPlayerDataPacket::decode)
                .consumerMainThread(SyncPlayerDataPacket::handle)
                .add();

        // 客户端 → 服务端：基础能力施法
        CHANNEL.messageBuilder(CastBasicAbilityPayload.class, ID_CAST_BASIC_ABILITY,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(CastBasicAbilityPayload::encode)
                .decoder(CastBasicAbilityPayload::decode)
                .consumerMainThread(CastBasicAbilityPayload::handle)
                .add();

        // 客户端 → 服务端：选择法术
        CHANNEL.messageBuilder(SelectSpellPayload.class, ID_SELECT_SPELL,
                        NetworkDirection.PLAY_TO_SERVER)
                .encoder(SelectSpellPayload::encode)
                .decoder(SelectSpellPayload::decode)
                .consumerMainThread(SelectSpellPayload::handle)
                .add();

        FriliensMagic.LOGGER.info("network registered: 3 packets, protocol {}", PROTOCOL_VERSION);
    }

    // ===== 发送辅助 =====

    /**
     * 把某项玩家数据同步给该玩家自己的客户端。
     *
     * <p>对应 1.21 的 {@code player.syncData(...)}。
     * 只在服务端调用；客户端调用是空操作。
     *
     * @param player 目标玩家
     * @param kind   数据标识（见 {@link PlayerDataKinds}）
     * @param cap    数据能力实例
     */
    public static void syncToPlayer(ServerPlayer player, String kind,
                                    PlayerDataCapability<?, ?> cap) {
        if (player == null || cap == null) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncPlayerDataPacket(kind, cap.serializeNBT()));
    }

    /**
     * 把某项玩家数据同步给所有能看见该玩家的人。
     *
     * <p>用于护盾这类<b>别人也要看到</b>的数据——只发给自己会导致
     * 其他玩家看不到护盾渲染。
     */
    public static void syncToTracking(ServerPlayer player, String kind,
                                      PlayerDataCapability<?, ?> cap) {
        if (player == null || cap == null) {
            return;
        }
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new SyncPlayerDataPacket(kind, cap.serializeNBT()));
    }

    /**
     * 给该维度的所有玩家发一个原版粒子包。
     *
     * <p><b>为什么不用 {@code ServerLevel#sendParticles}</b>：
     * 1.20.1 的实现里有个 <b>32 格剔除</b>，距离玩家超过 32 格的位置会被静默丢弃：
     *
     * <pre>{@code
     * if (blockpos.closerToCenterThan(new Vec3(x, y, z), p_8638_ ? 512.0D : 32.0D))
     * }</pre>
     *
     * 本模组的法术射程可达 200 格以上，于是绝大部分粒子根本到不了客户端
     * （实测 {@code attempted=17291 accepted=1979}）。
     *
     * <p><b>修法参照本仓库另一个模组</b> {@code F:\Work\Twelve Talismans}
     * 的 {@code SmartParticleDispatcher#dispatch}：直接构造
     * {@code ClientboundLevelParticlesPacket} 并把 {@code overrideLimiter} 设为
     * {@code true}，再逐个 {@code ServerPlayer#connection#send}。
     * 那个模组用同样方式在同样的整合包里粒子显示正常。
     *
     * @param level   世界（服务端）
     * @param options 粒子类型与参数
     * @param x       坐标
     * @param y       坐标
     * @param z       坐标
     * @param count   粒子数量
     * @param xDist   散布半径（X）
     * @param yDist   散布半径（Y）
     * @param zDist   散布半径（Z）
     * @param speed   初速度
     */
    public static void sendParticlePacket(net.minecraft.server.level.ServerLevel level,
                                         net.minecraft.core.particles.ParticleOptions options,
                                         double x, double y, double z,
                                         int count,
                                         float xDist, float yDist, float zDist, float speed) {
        if (level == null) {
            return;
        }
        net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket packet =
                new net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket(
                        options, true, x, y, z, xDist, yDist, zDist, speed, count);
        for (ServerPlayer sp : level.players()) {
            if (sp.level() == level) {
                sp.connection.send(packet);
            }
        }
    }

    /** 便于处理器里回复上下文（Brigadier 风格）。 */
    private ModNetwork() {
    }
}
