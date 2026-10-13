package yifei.frliliens.magic.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 存档级的欢迎语数据。
 *
 * <p>存在主世界的 {@code data/} 里（{@link SavedData}），所以是<b>每个存档独立</b>的：
 * 同一个玩家在不同存档可以各自看到一次欢迎语。
 *
 * <h2>文案的三层优先级</h2>
 * <ol>
 *   <li><b>单玩家自定义</b>（{@code players}）——某个人单独设的文本</li>
 *   <li><b>全局自定义</b>（{@code customMessage}）——服主设的、所有人共用的文本</li>
 *   <li><b>全局默认</b>（{@link #KEY_DEFAULT}）——<b>翻译键</b>，由客户端按自己的语言渲染</li>
 * </ol>
 *
 * <p>默认值用翻译键而不是写死的字符串，是因为服务端拿不到客户端的语言设置：
 * 只有发翻译键过去、让客户端解析，非中文玩家才能看到自己语言的欢迎语。
 *
 * <h2>文本里的换行</h2>
 * 命令里敲不了真换行，所以自定义文本用字面 {@code \n} 表示，
 * 由 {@link #lines(String)} 还原成多行。
 */
public class WelcomeData extends SavedData {

    /** 存档里的文件名：{@code data/friliensmagic_welcome.dat}。 */
    public static final String FILE_ID = "friliensmagic_welcome";

    /** 默认欢迎语的翻译键。 */
    public static final String KEY_DEFAULT = "friliensmagic.welcome.default";

    /** 消息里用来表示换行的字面写法。 */
    private static final String NEWLINE_ESCAPE = "\\n";

    private static final String KEY_CUSTOM = "customMessage";
    private static final String KEY_DISABLED = "disabled";
    private static final String KEY_PLAYERS = "players";
    private static final String KEY_UUID = "uuid";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_WELCOMED = "welcomed";
    private static final String KEY_PLAYER_DISABLED = "playerDisabled";
    private static final String KEY_JOIN_CUSTOM = "joinCustom";
    private static final String KEY_JOIN_DISABLED = "joinDisabled";

    /** 全局自定义文本；为空表示走默认翻译键。 */
    private String customMessage = "";
    /** 全局是否关闭提示。 */
    private boolean disabled = false;
    /** 单玩家自定义文本。键为玩家 UUID 字符串。 */
    private final Map<String, String> players = new HashMap<>();
    /** 被单独关闭提示的玩家。 */
    private final java.util.Set<UUID> playerDisabled = new java.util.HashSet<>();
    /** 已经提示过的玩家，用来实现「首次进入才提示」。 */
    private final java.util.Set<UUID> welcomed = new java.util.HashSet<>();

    /** 社群信息的全局自定义文本；为空表示走默认翻译键。 */
    private String joinCustom = "";
    /** 是否关闭社群信息。 */
    private boolean joinDisabled = false;

    public WelcomeData() {
    }

    /** 全新的存档数据。 */
    public static WelcomeData create() {
        return new WelcomeData();
    }

    /** 从 NBT 读取。 */
    public static WelcomeData load(CompoundTag tag) {
        WelcomeData d = new WelcomeData();

        if (tag.contains(KEY_CUSTOM, Tag.TAG_STRING)) {
            d.customMessage = tag.getString(KEY_CUSTOM);
        }
        d.disabled = tag.getBoolean(KEY_DISABLED);

        if (tag.contains(KEY_JOIN_CUSTOM, Tag.TAG_STRING)) {
            d.joinCustom = tag.getString(KEY_JOIN_CUSTOM);
        }
        d.joinDisabled = tag.getBoolean(KEY_JOIN_DISABLED);

        ListTag list = tag.getList(KEY_PLAYERS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String uuid = entry.getString(KEY_UUID);
            String msg = entry.getString(KEY_MESSAGE);
            if (!uuid.isEmpty() && !msg.isEmpty()) {
                d.players.put(uuid, msg);
            }
        }

        ListTag welcomed = tag.getList(KEY_WELCOMED, Tag.TAG_STRING);
        for (int i = 0; i < welcomed.size(); i++) {
            try {
                d.welcomed.add(UUID.fromString(welcomed.getString(i)));
            } catch (IllegalArgumentException ignored) {
                // 单个 UUID 坏掉不影响其它记录
            }
        }

        ListTag playerDisabled = tag.getList(KEY_PLAYER_DISABLED, Tag.TAG_STRING);
        for (int i = 0; i < playerDisabled.size(); i++) {
            try {
                d.playerDisabled.add(UUID.fromString(playerDisabled.getString(i)));
            } catch (IllegalArgumentException ignored) {
                // 同上
            }
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putString(KEY_CUSTOM, customMessage);
        tag.putBoolean(KEY_DISABLED, disabled);
        tag.putString(KEY_JOIN_CUSTOM, joinCustom);
        tag.putBoolean(KEY_JOIN_DISABLED, joinDisabled);

        ListTag list = new ListTag();
        players.forEach((uuid, msg) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString(KEY_UUID, uuid);
            entry.putString(KEY_MESSAGE, msg);
            list.add(entry);
        });
        tag.put(KEY_PLAYERS, list);

        ListTag welcomed = new ListTag();
        for (UUID id : this.welcomed) {
            welcomed.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
        }
        tag.put(KEY_WELCOMED, welcomed);

        ListTag playerDisabled = new ListTag();
        for (UUID id : this.playerDisabled) {
            playerDisabled.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
        }
        tag.put(KEY_PLAYER_DISABLED, playerDisabled);
        return tag;
    }

    // ===== 全局文案 =====

    /** 全局是否关闭提示。 */
    public boolean isDisabled() {
        return disabled;
    }

    /** 全局自定义文本（不含单玩家覆盖）。 */
    public String customMessage() {
        return customMessage;
    }

    /** 全局文本是否为空。 */
    public boolean isCustomEmpty() {
        return customMessage == null || customMessage.isBlank();
    }

    /** 全局是否已改成自定义文本。 */
    public boolean hasCustom() {
        return !isCustomEmpty();
    }

    /** 设置全局自定义文本；空串表示回到默认翻译键。 */
    public void setCustomMessage(String message) {
        this.customMessage = message == null ? "" : message;
        this.disabled = false;
        setDirty();
    }

    /** 关闭全局提示。 */
    public void disable() {
        this.disabled = true;
        setDirty();
    }

    /** 恢复默认（走翻译键）。 */
    public void resetToDefault() {
        this.customMessage = "";
        this.disabled = false;
        setDirty();
    }

    // ===== 社群信息（每次进入都发，与欢迎语独立）=====

    /** 社群信息是否已关闭。 */
    public boolean isJoinDisabled() {
        return joinDisabled;
    }

    /** 社群信息的自定义文本；为空表示走默认翻译键。 */
    public String joinCustom() {
        return joinCustom;
    }

    /** 社群信息是否已改成自定义文本。 */
    public boolean hasJoinCustom() {
        return joinCustom != null && !joinCustom.isBlank();
    }

    /** 设置社群信息自定义文本；空串表示回到默认翻译键。 */
    public void setJoinCustom(String message) {
        this.joinCustom = message == null ? "" : message;
        this.joinDisabled = false;
        setDirty();
    }

    /** 关闭社群信息。 */
    public void disableJoin() {
        this.joinDisabled = true;
        setDirty();
    }

    /** 社群信息恢复默认（走翻译键）。 */
    public void resetJoin() {
        this.joinCustom = "";
        this.joinDisabled = false;
        setDirty();
    }

    // ===== 单玩家文案 =====

    /** 该玩家是否有自定义文本。 */
    public boolean hasPlayerMessage(UUID playerId) {
        return players.containsKey(playerId.toString());
    }

    /** 该玩家的自定义文本；没有则返回空串。 */
    public String playerMessage(UUID playerId) {
        return players.getOrDefault(playerId.toString(), "");
    }

    /** 该玩家是否被单独关闭了提示。 */
    public boolean isPlayerDisabled(UUID playerId) {
        return playerDisabled.contains(playerId);
    }

    /**
     * 设置某玩家的自定义文本。
     *
     * <p>传空串表示清除自定义并回到上级文案（不是「关闭」，关闭用
     * {@link #disablePlayer}）。
     */
    public void setPlayerMessage(UUID playerId, String message) {
        playerDisabled.remove(playerId);
        if (message == null || message.isBlank()) {
            players.remove(playerId.toString());
        } else {
            players.put(playerId.toString(), message);
        }
        setDirty();
    }

    /** 单独关闭某玩家的提示。 */
    public void disablePlayer(UUID playerId) {
        players.remove(playerId.toString());
        if (playerDisabled.add(playerId)) {
            setDirty();
        }
    }

    /** 清除某玩家的自定义文本（回到上级）。 */
    public void clearPlayerMessage(UUID playerId) {
        boolean changed = players.remove(playerId.toString()) != null;
        changed |= playerDisabled.remove(playerId);
        if (changed) {
            setDirty();
        }
    }

    /** 单玩家自定义条数。 */
    public int playerMessageCount() {
        return players.size();
    }

    /** 全部有自定义文本的玩家 UUID。 */
    public List<String> playerMessageKeys() {
        return new ArrayList<>(players.keySet());
    }

    // ===== 首次提示判定 =====

    /** 该玩家是否已经提示过。 */
    public boolean hasWelcomed(UUID playerId) {
        return welcomed.contains(playerId);
    }

    /** 标记为已提示。 */
    public void markWelcomed(UUID playerId) {
        if (welcomed.add(playerId)) {
            setDirty();
        }
    }

    /** 清空已提示记录，让所有玩家下次进入时重新看到提示。 */
    public void clearWelcomed() {
        welcomed.clear();
        setDirty();
    }

    /** 清除单个玩家的已提示记录。 */
    public void clearWelcomed(UUID playerId) {
        if (welcomed.remove(playerId)) {
            setDirty();
        }
    }

    /** 已提示过的玩家数。 */
    public int welcomedCount() {
        return welcomed.size();
    }

    // ===== 文本处理 =====

    /** 把文本拆成多行（还原字面 {@code \n}）。 */
    public static List<String> lines(String message) {
        if (message == null || message.isEmpty()) {
            return List.of();
        }
        return java.util.Arrays.stream(
                        message.split(java.util.regex.Pattern.quote(NEWLINE_ESCAPE)))
                .toList();
    }

    // ===== 获取 =====

    /**
     * 取当前存档的欢迎语数据，不存在则创建。
     *
     * <p>固定挂在主世界：{@link SavedData} 是按维度分目录存的，
     * 而欢迎语是全局的，挂在主世界可避免每个维度各存一份。
     *
     * @param server 服务器实例
     */
    public static WelcomeData get(MinecraftServer server) {
        var level = server.overworld();
        // 1.20.1 的签名是 computeIfAbsent(反序列化函数, 构造器, 名称)，
        // 没有 1.21 的 SavedData.Factory 包装（那是 1.21 才加的）。
        return level.getDataStorage().computeIfAbsent(
                WelcomeData::load, WelcomeData::create, FILE_ID);
    }
}
