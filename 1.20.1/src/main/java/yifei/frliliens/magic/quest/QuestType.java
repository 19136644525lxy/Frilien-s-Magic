package yifei.frliliens.magic.quest;

import java.util.Locale;

import com.mojang.serialization.Codec;

/**
 * 委托类型枚举。
 */
public enum QuestType {
    /** 击杀指定实体。 */
    KILL,
    /** 收集指定物品。 */
    COLLECT;

    /**
     * 数据包 JSON 里 {@code "type"} 字段的编解码器：只接受 kill / collect。
     *
     * <p>用 {@link com.mojang.serialization.MapCodec} 而不是 Codec，
     * 这样能直接参与 {@code RecordCodecBuilder.group(...)} 的链式构造。
     */
    public static final com.mojang.serialization.MapCodec<QuestType> CODEC =
            Codec.STRING.comapFlatMap(QuestType::parse, QuestType::serialName).fieldOf("type");

    /** 序列化名称（小写）。 */
    public String serialName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** 从序列化字符串解析，容错返回 KILL。 */
    public static QuestType fromSerial(String name) {
        return "collect".equalsIgnoreCase(name) ? COLLECT : KILL;
    }

    /**
     * 严格解析，供数据包使用——写错的值会报错而不是静默变成 KILL。
     *
     * @param name JSON 里的字符串
     * @return 解析结果或错误
     */
    private static com.mojang.serialization.DataResult<QuestType> parse(String name) {
        for (QuestType t : values()) {
            if (t.serialName().equalsIgnoreCase(name)) {
                return com.mojang.serialization.DataResult.success(t);
            }
        }
        return com.mojang.serialization.DataResult.error(
                () -> "Unknown quest type \"" + name + "\", expected kill or collect");
    }
}
