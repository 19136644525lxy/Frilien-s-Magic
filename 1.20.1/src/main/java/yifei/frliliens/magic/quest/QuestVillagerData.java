package yifei.frliliens.magic.quest;

import net.minecraft.nbt.CompoundTag;

/**
 * 村民委托数据。
 *
 * <p>标记一个村民是否为委托村民，并记录冷却到期时间。
 * 与 1.21 版的差异同 {@link QuestData}——手写 NBT 而非 Codec。
 *
 * @param isQuestGiver  是否为委托村民
 * @param cooldownUntil 冷却到期时间（tick）
 */
public record QuestVillagerData(boolean isQuestGiver, long cooldownUntil) {

    /** 非委托村民。 */
    public static final QuestVillagerData EMPTY = new QuestVillagerData(false, 0);

    private static final String TAG_QUEST_GIVER = "isQuestGiver";
    private static final String TAG_COOLDOWN = "cooldownUntil";

    /** 写入 NBT。 */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(TAG_QUEST_GIVER, isQuestGiver);
        tag.putLong(TAG_COOLDOWN, cooldownUntil);
        return tag;
    }

    /** 从 NBT 读取。 */
    public static QuestVillagerData load(CompoundTag tag) {
        if (tag == null) {
            return EMPTY;
        }
        return new QuestVillagerData(tag.getBoolean(TAG_QUEST_GIVER),
                tag.getLong(TAG_COOLDOWN));
    }

    /** 是否在冷却中。 */
    public boolean isOnCooldown(long currentTick) {
        return isQuestGiver && currentTick < cooldownUntil;
    }

    /** 标记为委托村民。 */
    public QuestVillagerData markAsQuestGiver() {
        return new QuestVillagerData(true, cooldownUntil);
    }

    /** 设置冷却。 */
    public QuestVillagerData withCooldown(long until) {
        return new QuestVillagerData(true, until);
    }
}
