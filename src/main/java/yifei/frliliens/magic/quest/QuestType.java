package yifei.frliliens.magic.quest;

/**
 * 委托类型枚举。
 */
public enum QuestType {
    /** 击杀指定实体。 */
    KILL,
    /** 收集指定物品。 */
    COLLECT;

    /** 序列化名称（小写）。 */
    public String serialName() {
        return name().toLowerCase();
    }

    /** 从序列化字符串解析，容错返回 KILL。 */
    public static QuestType fromSerial(String name) {
        return "collect".equalsIgnoreCase(name) ? COLLECT : KILL;
    }
}
