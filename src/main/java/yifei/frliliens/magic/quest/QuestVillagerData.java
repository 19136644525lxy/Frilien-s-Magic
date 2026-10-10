package yifei.frliliens.magic.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 村民委托数据附件。
 *
 * <p>标记一个村民是否为委托村民，并记录冷却到期时间。
 * 附件随村民实体存档自然持久化，不需要 copyOnDeath。
 *
 * @param isQuestGiver  是否为委托村民
 * @param cooldownUntil 冷却到期时间（tick）
 */
public record QuestVillagerData(boolean isQuestGiver, long cooldownUntil) {

    /** 非委托村民。 */
    public static final QuestVillagerData EMPTY = new QuestVillagerData(false, 0);

    /** Codec：持久化到磁盘。 */
    public static final Codec<QuestVillagerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("isQuestGiver").forGetter(QuestVillagerData::isQuestGiver),
            Codec.LONG.fieldOf("cooldownUntil").forGetter(QuestVillagerData::cooldownUntil)
    ).apply(instance, QuestVillagerData::new));

    /** StreamCodec：网络同步。 */
    public static final StreamCodec<ByteBuf, QuestVillagerData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, QuestVillagerData::isQuestGiver,
            ByteBufCodecs.VAR_LONG, QuestVillagerData::cooldownUntil,
            QuestVillagerData::new);

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
