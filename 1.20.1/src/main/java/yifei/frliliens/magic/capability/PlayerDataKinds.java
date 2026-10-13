package yifei.frliliens.magic.capability;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraftforge.common.capabilities.Capability;

/**
 * 玩家数据的「标识 → 能力」注册表。
 *
 * <p>服务端挂载 capability、客户端接收同步包、重生拷贝数据，
 * 三处都需要「按标识找到对应的能力」，所以定义集中在这里，避免各写一份不一致。
 *
 * <h2>为什么用标识字符串</h2>
 * 同步包要在网络上传输「这是哪项数据」，能力实例本身不能序列化。
 * 用一个短字符串做键最省带宽，也便于日志排查。
 *
 * <p>标识同时用作 {@code AttachCapabilitiesEvent} 的 ResourceLocation 路径，
 * 所以必须是小写字母加下划线的合法路径。
 */
public final class PlayerDataKinds {

    /**
     * 一项玩家数据能力。
     *
     * <p>{@code I} 同时是 capability 的泛型参数、工厂的返回类型、
     * 与其能力声明的容器——这样从任一项都能推出另外两项，
     * 不必再传一个裸的 {@code Capability<?>} 导致泛型无法推断。
     */
    public interface Cap<I extends PlayerDataCapability<?, I>> {
        /** 能力声明。 */
        Capability<I> capability();

        /** 新实例工厂。 */
        Supplier<I> factory();
    }

    /** 一项数据的元数据。 */
    public record Kind<I extends PlayerDataCapability<?, I>>(String id, Cap<I> cap,
                                                             boolean keepOnDeath) {
    }

    /** 把「能力声明常量」和「构造器」绑成 {@link Cap}。 */
    private static <I extends PlayerDataCapability<?, I>> Cap<I> cap(Capability<I> capability,
                                                                     Supplier<I> factory) {
        return new Cap<>() {
            @Override
            public Capability<I> capability() {
                return capability;
            }

            @Override
            public Supplier<I> factory() {
                return factory;
            }
        };
    }

    /** 魔力：跨死亡保留。 */
    public static final String MANA = "mana";
    /** 护盾：跨死亡保留（1.21 版的 copyOnDeath 设定）。 */
    public static final String SHIELD = "shield";
    /** 已学法术：跨死亡保留。 */
    public static final String LEARNED_SPELLS = "learned_spells";
    /** 选中法术：跨死亡保留。 */
    public static final String SELECTED_SPELL = "selected_spell";
    /** 飞行状态：死亡后清除。 */
    public static final String FLIGHT = "flight";
    /** 玩家委托：死亡后清除（增加风险感，与 1.21 版一致）。 */
    public static final String QUEST = "quest";
    /** 村民委托标记与冷却：挂在村民实体上。 */
    public static final String QUEST_VILLAGER = "quest_villager";

    /** 全部玩家数据。顺序无关紧要，配对一律靠 {@link Kind#id()}。 */
    public static final List<Kind<?>> KINDS = List.of(
            new Kind<>(MANA,
                    cap(ModCapabilities.MANA, ModCapabilities.ManaCapability::new), true),
            new Kind<>(SHIELD,
                    cap(ModCapabilities.SHIELD, ModCapabilities.ShieldCapability::new), true),
            new Kind<>(LEARNED_SPELLS,
                    cap(ModCapabilities.LEARNED_SPELLS,
                            ModCapabilities.LearnedSpellsCapability::new), true),
            new Kind<>(SELECTED_SPELL,
                    cap(ModCapabilities.SELECTED_SPELL,
                            ModCapabilities.SelectedSpellCapability::new), true),
            new Kind<>(FLIGHT,
                    cap(ModCapabilities.FLIGHT, ModCapabilities.FlightCapability::new), false),
            new Kind<>(QUEST,
                    cap(ModCapabilities.QUEST, ModCapabilities.QuestCapability::new), false));

    /**
     * 挂在村民实体上的数据（不是玩家）。
     *
     * <p>单独一张表，因为挂载与重生拷贝的对象不同——
     * 村民不会重生，所以不需要 keepOnDeath 语义。
     */
    public static final List<Kind<?>> VILLAGER_KINDS = List.of(
            new Kind<>(QUEST_VILLAGER,
                    cap(ModCapabilities.QUEST_VILLAGER,
                            ModCapabilities.QuestVillagerCapability::new), false));

    /** 标识 → 元数据。客户端按标识查表时用。 */
    private static final Map<String, Kind<?>> BY_ID = buildIndex();

    /**
     * 建立标识索引。
     *
     * <p><b>必须同时收录 {@link #KINDS} 与 {@link #VILLAGER_KINDS}</b>——
     * 村民数据不在 KINDS 里，只遍历 KINDS 会让
     * {@code byId("quest_villager")} 返回 null，进而使
     * {@code PlayerDataHelper.setQuestVillager} 直接失败、委托村民永远标不上，
     * 表现为「右键村民没有任何反馈」。
     */
    private static Map<String, Kind<?>> buildIndex() {
        Map<String, Kind<?>> map = new LinkedHashMap<>();
        for (Kind<?> kind : KINDS) {
            putUnique(map, kind);
        }
        for (Kind<?> kind : VILLAGER_KINDS) {
            putUnique(map, kind);
        }
        return Map.copyOf(map);
    }

    private static void putUnique(Map<String, Kind<?>> map, Kind<?> kind) {
        Kind<?> previous = map.put(kind.id(), kind);
        if (previous != null) {
            throw new IllegalStateException("duplicate player data kind: " + kind.id());
        }
    }

    /** 按标识查找；未知标识返回 null（同步包可能来自更新的另一端）。 */
    public static Kind<?> byId(String id) {
        return BY_ID.get(id);
    }

    /** 该标识是否存在。 */
    public static boolean exists(String id) {
        return BY_ID.containsKey(id);
    }

    /**
     * 自检：确认两张表里的每一项都能被 {@link #byId} 找到。
     *
     * <p>加这道检查是因为 {@code buildIndex()} 曾经漏掉 {@link #VILLAGER_KINDS}，
     * 导致村民数据查不到标识、委托标记静默失败——这种错误编译期发现不了，
     * 症状又只是「功能没反应」，很难定位。启动时报错比线上排查强。
     *
     * @throws IllegalStateException 有项目未被索引
     */
    public static void verifyIndex() {
        for (Kind<?> kind : KINDS) {
            require(kind);
        }
        for (Kind<?> kind : VILLAGER_KINDS) {
            require(kind);
        }
    }

    private static void require(Kind<?> kind) {
        if (BY_ID.get(kind.id()) != kind) {
            throw new IllegalStateException(
                    "kind '" + kind.id() + "' is missing from the id index");
        }
    }

    private PlayerDataKinds() {
    }
}
