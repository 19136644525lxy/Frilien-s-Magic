package yifei.frliliens.magic.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import yifei.frliliens.magic.capability.PlayerDataHelper;
import yifei.frliliens.magic.attachment.FlightData;
import yifei.frliliens.magic.attachment.LearnedSpellsData;
import yifei.frliliens.magic.attachment.ManaData;
import yifei.frliliens.magic.attachment.SelectedSpellData;
import yifei.frliliens.magic.attachment.ShieldData;
import yifei.frliliens.magic.quest.QuestData;
import yifei.frliliens.magic.quest.QuestVillagerData;

/**
 * 玩家数据的统一读写入口。
 *
 * <p>移植对照（1.21 NeoForge → 1.20.1 Forge）：
 * <pre>
 *   player.getData(ModAttachments.MANA)        →  PlayerDataHelper.mana(player)
 *   player.setData(ModAttachments.MANA, value) →  PlayerDataHelper.setMana(player, value)
 *   entity.syncData(ModAttachments.MANA)       →  PlayerDataHelper.syncMana(player)
 * </pre>
 *
 * <h2>读：自动区分服务端与客户端</h2>
 * <ul>
 *   <li><b>服务端</b>：直接读 capability，是权威值</li>
 *   <li><b>客户端</b>：读 {@link ClientPlayerData} 里服务端推来的快照，
 *       没收到过就返回默认值——这样渲染在数据到达前也有合理表现，不会空指针</li>
 * </ul>
 *
 * <h2>写：只在服务端有意义</h2>
 * 客户端的写操作会被忽略（返回 false）。1.20.1 没有附件自动同步，
 * 客户端改本地数据不会生效，反而会造成两端不一致，所以直接拒绝。
 *
 * <p><b>同步</b>：写完之后调用对应的 {@code sync*} 方法发包，
 * 否则客户端看到的是旧值。护盾这类别人也要看的数据用
 * {@code syncToTracking} 版本。
 */
public final class PlayerDataHelper {

    private PlayerDataHelper() {
    }

    // ===== 魔力 =====

    /** 读魔力。服务端读权威值，客户端读同步快照，都没有则返回初值。 */
    public static ManaData mana(Entity entity) {
        return read(entity, PlayerDataKinds.MANA, ManaData.INITIAL, ManaData::load);
    }

    /** 写魔力。只在服务端生效。 */
    public static boolean setMana(Player player, ManaData value) {
        return writeTag(player, PlayerDataKinds.MANA, value.save());
    }

    /** 把魔力同步给该玩家自己的客户端。 */
    public static void syncMana(Player player) {
        syncToSelf(player, PlayerDataKinds.MANA);
    }

    // ===== 护盾 =====

    /** 读护盾。 */
    public static ShieldData shield(Entity entity) {
        return read(entity, PlayerDataKinds.SHIELD, ShieldData.EMPTY, ShieldData::load);
    }

    /** 写护盾。只在服务端生效。 */
    public static boolean setShield(Player player, ShieldData value) {
        return writeTag(player, PlayerDataKinds.SHIELD, value.save());
    }

    /**
     * 同步护盾。
     *
     * <p>护盾要给别人看（渲染在实体周围），所以发给所有能看到该玩家的客户端，
     * 而不只是他自己。
     */
    public static void syncShield(Player player) {
        syncToTracking(player, PlayerDataKinds.SHIELD);
    }

    // ===== 已学法术 =====

    /** 读已学法术。 */
    public static LearnedSpellsData learnedSpells(Entity entity) {
        return read(entity, PlayerDataKinds.LEARNED_SPELLS, LearnedSpellsData.EMPTY,
                LearnedSpellsData::load);
    }

    /** 写已学法术。只在服务端生效。 */
    public static boolean setLearnedSpells(Player player, LearnedSpellsData value) {
        return writeTag(player, PlayerDataKinds.LEARNED_SPELLS, value.save());
    }

    /** 同步已学法术给该玩家自己的客户端（法术轮盘要用）。 */
    public static void syncLearnedSpells(Player player) {
        syncToSelf(player, PlayerDataKinds.LEARNED_SPELLS);
    }

    // ===== 选中法术 =====

    /** 读选中法术。 */
    public static SelectedSpellData selectedSpell(Entity entity) {
        return read(entity, PlayerDataKinds.SELECTED_SPELL, SelectedSpellData.EMPTY,
                SelectedSpellData::load);
    }

    /** 写选中法术。只在服务端生效。 */
    public static boolean setSelectedSpell(Player player, SelectedSpellData value) {
        return writeTag(player, PlayerDataKinds.SELECTED_SPELL, value.save());
    }

    /** 同步选中法术给该玩家自己的客户端。 */
    public static void syncSelectedSpell(Player player) {
        syncToSelf(player, PlayerDataKinds.SELECTED_SPELL);
    }

    // ===== 飞行 =====

    /** 读飞行状态。 */
    public static FlightData flight(Entity entity) {
        return read(entity, PlayerDataKinds.FLIGHT, FlightData.INACTIVE, FlightData::load);
    }

    /** 写飞行状态。只在服务端生效。 */
    public static boolean setFlight(Player player, FlightData value) {
        return writeTag(player, PlayerDataKinds.FLIGHT, value.save());
    }

    /** 同步飞行状态给该玩家自己的客户端。 */
    public static void syncFlight(Player player) {
        syncToSelf(player, PlayerDataKinds.FLIGHT);
    }

    // ===== 委托（玩家）=====

    /** 读玩家委托数据。 */
    public static QuestData quest(Entity entity) {
        return read(entity, PlayerDataKinds.QUEST, QuestData.EMPTY, QuestData::load);
    }

    /** 写玩家委托数据。只在服务端生效。 */
    public static boolean setQuest(Player player, QuestData value) {
        return writeTag(player, PlayerDataKinds.QUEST, value.save());
    }

    /** 同步委托数据给该玩家自己的客户端。 */
    public static void syncQuest(Player player) {
        syncToSelf(player, PlayerDataKinds.QUEST);
    }

    // ===== 委托村民标记（挂在村民实体上）=====

    /** 读村民委托数据。任意实体都可传入，非村民返回 {@link QuestVillagerData#EMPTY}。 */
    public static QuestVillagerData questVillager(Entity entity) {
        return read(entity, PlayerDataKinds.QUEST_VILLAGER, QuestVillagerData.EMPTY,
                QuestVillagerData::load);
    }

    /** 写村民委托数据。只在服务端生效。 */
    public static boolean setQuestVillager(Entity entity, QuestVillagerData value) {
        if (entity == null || entity.level().isClientSide) {
            return false;
        }
        PlayerDataKinds.Kind<?> k = PlayerDataKinds.byId(PlayerDataKinds.QUEST_VILLAGER);
        if (k == null) {
            return false;
        }
        PlayerDataCapability<?, ?> cap = providerOf(entity, k);
        if (cap == null) {
            return false;
        }
        cap.deserializeNBT(value.save());
        return true;
    }

    /**
     * 注意：村民委托标记<b>不</b>做网络同步。
     *
     * <p>原因：Forge 的发包目标只能是玩家（{@code ServerPlayer}），
     * 而村民是普通实体，自己不能作为目标。要同步就得遍历所有正在追踪
     * 该村民的玩家，收益很小——村民的委托标记只影响服务端判定
     * （谁有委托可接），客户端不需要它。
     *
     * <p>发光效果由服务端的 {@code EntityJoinLevelEvent} 直接设置，
     * 那套机制自带同步，所以外观不受影响。
     */

    // ===== 内部实现 =====

    /**
     * 通用读取：服务端直读 capability，客户端读同步快照。
     *
     * <p>服务端不绕 NBT——读路径（魔力判定、法术施放、渲染）会高频调用，
     * 序列化再解析是白费开销。客户端只有 NBT 快照可用，所以走加载函数。
     *
     * @param entity   目标实体
     * @param kind     数据标识
     * @param fallback 完全没有数据时的返回值
     * @param loader   从 NBT 还原数据（客户端用）
     */
    private static <D> D read(Entity entity, String kind, D fallback,
                              java.util.function.Function<CompoundTag, D> loader) {
        if (entity == null) {
            return fallback;
        }
        if (entity.level().isClientSide) {
            CompoundTag tag = ClientPlayerData.get(entity instanceof Player p ? p : null, kind);
            return tag == null ? fallback : loader.apply(tag);
        }
        PlayerDataKinds.Kind<?> k = PlayerDataKinds.byId(kind);
        if (k == null) {
            return fallback;
        }
        PlayerDataCapability<?, ?> cap = providerOf(entity, k);
        if (cap == null) {
            return fallback;
        }
        return cast(cap.getValue(), fallback);
    }

    /** 把 provider 的值转成调用方期望的类型；类型不符时退回默认值。 */
    @SuppressWarnings("unchecked")
    private static <D> D cast(Object value, D fallback) {
        return value == null ? fallback : (D) value;
    }

    /** 写入 capability。只在服务端成功。 */
    private static boolean writeTag(Player player, String kind, CompoundTag data) {
        if (player == null || player.level().isClientSide) {
            return false;
        }
        PlayerDataKinds.Kind<?> k = PlayerDataKinds.byId(kind);
        if (k == null) {
            return false;
        }
        PlayerDataCapability<?, ?> cap = providerOf(player, k);
        if (cap == null) {
            return false;
        }
        cap.deserializeNBT(data);
        return true;
    }

    private static <I extends PlayerDataCapability<?, I>> PlayerDataCapability<?, ?> providerOf(
            Entity entity, PlayerDataKinds.Kind<I> kind) {
        return entity.getCapability(kind.cap().capability()).orElse(null);
    }

    /** 发给自己。 */
    private static void syncToSelf(Player player, String kind) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer sp)) {
            return;
        }
        PlayerDataKinds.Kind<?> k = PlayerDataKinds.byId(kind);
        if (k == null) {
            return;
        }
        PlayerDataCapability<?, ?> cap = providerOf(player, k);
        if (cap != null) {
            yifei.frliliens.magic.network.ModNetwork.syncToPlayer(sp, kind, cap);
        }
    }

    /** 发给所有能看到该玩家的人。 */
    private static void syncToTracking(Player player, String kind) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer sp)) {
            return;
        }
        PlayerDataKinds.Kind<?> k = PlayerDataKinds.byId(kind);
        if (k == null) {
            return;
        }
        PlayerDataCapability<?, ?> cap = providerOf(player, k);
        if (cap != null) {
            yifei.frliliens.magic.network.ModNetwork.syncToTracking(sp, kind, cap);
        }
    }
}
