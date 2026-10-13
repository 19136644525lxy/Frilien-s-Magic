package yifei.frliliens.magic.quest;

import net.minecraft.nbt.CompoundTag;

/**
 * 玩家委托数据。
 *
 * <p>以不可变 record 存储。死亡后不保留，增加风险感。
 *
 * <p><b>与 1.21 版的差异</b>：那边用 {@code Codec} + {@code StreamCodec}
 * 交给附件系统自动序列化与同步；1.20.1 没有附件，改成手写 NBT
 * （{@link #save}/{@link #load}），网络同步由 capability 的同步包负责。
 *
 * @param questId      委托模板 id（空字符串表示无委托）
 * @param type         委托类型（KILL/COLLECT）
 * @param targetId     目标注册 id（如 "minecraft:zombie"）
 * @param required     需要数量
 * @param progress     当前进度
 * @param acceptedAt   接受时间（游戏 tick）
 * @param pending      true=提议中未接受；false=已接受
 * @param lastQuestId  上一个委托模板 id（避免连续重复）
 * @param villagerUuid 委托来源村民 UUID（空字符串表示无）
 */
public record QuestData(
        String questId,
        QuestType type,
        String targetId,
        int required,
        int progress,
        long acceptedAt,
        boolean pending,
        String lastQuestId,
        String villagerUuid
) {

    /** 空委托。 */
    public static final QuestData EMPTY =
            new QuestData("", QuestType.KILL, "", 0, 0, 0, false, "", "");

    private static final String TAG_QUEST_ID = "questId";
    private static final String TAG_TYPE = "type";
    private static final String TAG_TARGET = "targetId";
    private static final String TAG_REQUIRED = "required";
    private static final String TAG_PROGRESS = "progress";
    private static final String TAG_ACCEPTED_AT = "acceptedAt";
    private static final String TAG_PENDING = "pending";
    private static final String TAG_LAST_QUEST = "lastQuestId";
    private static final String TAG_VILLAGER = "villagerUuid";

    /** 写入 NBT。 */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_QUEST_ID, questId);
        tag.putString(TAG_TYPE, type.serialName());
        tag.putString(TAG_TARGET, targetId);
        tag.putInt(TAG_REQUIRED, required);
        tag.putInt(TAG_PROGRESS, progress);
        tag.putLong(TAG_ACCEPTED_AT, acceptedAt);
        tag.putBoolean(TAG_PENDING, pending);
        tag.putString(TAG_LAST_QUEST, lastQuestId);
        tag.putString(TAG_VILLAGER, villagerUuid);
        return tag;
    }

    /** 从 NBT 读取；缺 questId 视为空委托。 */
    public static QuestData load(CompoundTag tag) {
        if (tag == null || !tag.contains(TAG_QUEST_ID)) {
            return EMPTY;
        }
        return new QuestData(
                tag.getString(TAG_QUEST_ID),
                QuestType.fromSerial(tag.getString(TAG_TYPE)),
                tag.getString(TAG_TARGET),
                tag.getInt(TAG_REQUIRED),
                tag.getInt(TAG_PROGRESS),
                tag.getLong(TAG_ACCEPTED_AT),
                tag.getBoolean(TAG_PENDING),
                tag.getString(TAG_LAST_QUEST),
                tag.getString(TAG_VILLAGER));
    }

    /** 是否有任何委托（含 pending）。 */
    public boolean hasQuest() {
        return !questId.isEmpty();
    }

    /** 是否为已接受的进行中委托。 */
    public boolean isActive() {
        return hasQuest() && !pending;
    }

    /** 是否为提议中待接受。 */
    public boolean isPending() {
        return hasQuest() && pending;
    }

    /** 是否完成。 */
    public boolean isComplete() {
        return isActive() && progress >= required;
    }

    /** 委托是否来自指定村民。 */
    public boolean isFromVillager(java.util.UUID uuid) {
        return uuid.toString().equals(villagerUuid);
    }

    /** 接受委托：清除 pending 标记。 */
    public QuestData accept() {
        return new QuestData(questId, type, targetId, required, progress,
                acceptedAt, false, lastQuestId, villagerUuid);
    }

    /** 更新进度。 */
    public QuestData withProgress(int newProgress) {
        return new QuestData(questId, type, targetId, required, newProgress,
                acceptedAt, pending, lastQuestId, villagerUuid);
    }

    /** 清除委托，保留 lastQuestId 避免连续重复。 */
    public QuestData clear() {
        return new QuestData("", QuestType.KILL, "", 0, 0, 0, false, questId, "");
    }
}
