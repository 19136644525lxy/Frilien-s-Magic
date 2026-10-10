package yifei.frliliens.magic.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 玩家委托数据。
 *
 * <p>以不可变 record 存储，配合 NeoForge {@code AttachmentType} 自动序列化与客户端同步。
 * 死亡后不保留（无 copyOnDeath），增加风险感。
 *
 * @param questId     委托模板 id（空字符串表示无委托）
 * @param type        委托类型（KILL/COLLECT）
 * @param targetId    目标注册 id（如 "minecraft:zombie"）
 * @param required    需要数量
 * @param progress    当前进度
 * @param acceptedAt  接受时间（游戏 tick）
 * @param pending     true=提议中未接受；false=已接受
 * @param lastQuestId 上一个委托模板 id（避免连续重复）
 */
public record QuestData(
        String questId,
        QuestType type,
        String targetId,
        int required,
        int progress,
        long acceptedAt,
        boolean pending,
        String lastQuestId
) {

    /** 空委托。 */
    public static final QuestData EMPTY = new QuestData("", QuestType.KILL, "", 0, 0, 0, false, "");

    /** Codec：持久化到磁盘。 */
    public static final Codec<QuestData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("questId").forGetter(QuestData::questId),
            Codec.STRING.fieldOf("type").forGetter(q -> q.type.serialName()),
            Codec.STRING.fieldOf("targetId").forGetter(QuestData::targetId),
            Codec.INT.fieldOf("required").forGetter(QuestData::required),
            Codec.INT.fieldOf("progress").forGetter(QuestData::progress),
            Codec.LONG.fieldOf("acceptedAt").forGetter(QuestData::acceptedAt),
            Codec.BOOL.fieldOf("pending").forGetter(QuestData::pending),
            Codec.STRING.fieldOf("lastQuestId").forGetter(QuestData::lastQuestId)
    ).apply(instance, (qid, t, tid, req, prog, at, pend, last) ->
            new QuestData(qid, QuestType.fromSerial(t), tid, req, prog, at, pend, last)));

    /** StreamCodec：网络同步（手动实现，因字段数超过 composite 上限）。 */
    public static final StreamCodec<ByteBuf, QuestData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public QuestData decode(ByteBuf buf) {
            return new QuestData(
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    QuestType.fromSerial(ByteBufCodecs.STRING_UTF8.decode(buf)),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf));
        }

        @Override
        public void encode(ByteBuf buf, QuestData q) {
            ByteBufCodecs.STRING_UTF8.encode(buf, q.questId());
            ByteBufCodecs.STRING_UTF8.encode(buf, q.type.serialName());
            ByteBufCodecs.STRING_UTF8.encode(buf, q.targetId());
            ByteBufCodecs.VAR_INT.encode(buf, q.required());
            ByteBufCodecs.VAR_INT.encode(buf, q.progress());
            ByteBufCodecs.VAR_LONG.encode(buf, q.acceptedAt());
            ByteBufCodecs.BOOL.encode(buf, q.pending());
            ByteBufCodecs.STRING_UTF8.encode(buf, q.lastQuestId());
        }
    };

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

    /** 接受委托：清除 pending 标记。 */
    public QuestData accept() {
        return new QuestData(questId, type, targetId, required, progress, acceptedAt, false, lastQuestId);
    }

    /** 更新进度。 */
    public QuestData withProgress(int newProgress) {
        return new QuestData(questId, type, targetId, required, newProgress, acceptedAt, pending, lastQuestId);
    }

    /** 清除委托，保留 lastQuestId 避免连续重复。 */
    public QuestData clear() {
        return new QuestData("", QuestType.KILL, "", 0, 0, 0, false, questId);
    }
}
