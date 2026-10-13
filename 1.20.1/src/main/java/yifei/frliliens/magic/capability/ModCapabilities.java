package yifei.frliliens.magic.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

import yifei.frliliens.magic.attachment.FlightData;
import yifei.frliliens.magic.attachment.LearnedSpellsData;
import yifei.frliliens.magic.attachment.ManaData;
import yifei.frliliens.magic.attachment.SelectedSpellData;
import yifei.frliliens.magic.attachment.ShieldData;
import yifei.frliliens.magic.quest.QuestData;
import yifei.frliliens.magic.quest.QuestVillagerData;

/**
 * 全部玩家数据 capability 的声明与注册。
 *
 * <p>Forge 1.20.1 的 capability 用 {@link CapabilityManager#get} 配
 * {@link CapabilityToken} 拿实例——token 是空类，靠泛型擦除后的类型匹配，
 * 所以每个能力必须有自己的 token，不能共用一个。
 *
 * <p><b>与 1.21 NeoForge 的对应关系</b>：
 * <pre>
 *   AttachmentType&lt;ManaData&gt;  ->  Capability&lt;ManaData&gt; + ManaCapability
 *   player.getData(MANA)      ->  player.getCapability(MOD_CAPS.MANA).orElse(ManaData.INITIAL)
 *   player.setData(MANA, v)   ->  player.getCapability(..).ifPresent(c -&gt; c.setValue(v))
 * </pre>
 */
public final class ModCapabilities {

    // ===== 能力声明 =====

    /** 玩家魔力。 */
    public static final Capability<ManaCapability> MANA =
            CapabilityManager.get(new CapabilityToken<>() {
            });

    /** 玩家护盾。 */
    public static final Capability<ShieldCapability> SHIELD =
            CapabilityManager.get(new CapabilityToken<>() {
            });

    /** 玩家已学法术。 */
    public static final Capability<LearnedSpellsCapability> LEARNED_SPELLS =
            CapabilityManager.get(new CapabilityToken<>() {
            });

    /** 玩家当前选中的战斗法术。 */
    public static final Capability<SelectedSpellCapability> SELECTED_SPELL =
            CapabilityManager.get(new CapabilityToken<>() {
            });

    /** 玩家飞行状态。 */
    public static final Capability<FlightCapability> FLIGHT =
            CapabilityManager.get(new CapabilityToken<>() {
            });

    /** 玩家委托数据。 */
    public static final Capability<QuestCapability> QUEST =
            CapabilityManager.get(new CapabilityToken<>() {
            });

    /** 村民的委托标记与冷却（挂在村民实体上，不是玩家）。 */
    public static final Capability<QuestVillagerCapability> QUEST_VILLAGER =
            CapabilityManager.get(new CapabilityToken<>() {
            });

    /**
     * 注册能力类型。
     *
     * <p>必须在 mod 事件总线上监听 {@link RegisterCapabilitiesEvent}。
     * 只注册「接口类型」，具体实例由 {@link AttachPlayerCapabilities} 挂到实体上。
     */
    public static void register(RegisterCapabilitiesEvent event) {
        event.register(ManaCapability.class);
        event.register(ShieldCapability.class);
        event.register(LearnedSpellsCapability.class);
        event.register(SelectedSpellCapability.class);
        event.register(FlightCapability.class);
        event.register(QuestCapability.class);
        event.register(QuestVillagerCapability.class);
    }

    private ModCapabilities() {
    }

    // ===== 各能力的实现 =====

    /** 魔力 capability。 */
    public static class ManaCapability extends PlayerDataCapability<ManaData, ManaCapability> {
        public ManaCapability() {
            super(ManaData.INITIAL);
        }

        @Override
        protected Capability<ManaCapability> capability() {
            return MANA;
        }

        @Override
        protected ManaData read(CompoundTag tag) {
            return ManaData.load(tag);
        }

        @Override
        protected void write(ManaData value, CompoundTag tag) {
            tag.merge(value.save());
        }

        @Override
        public ManaData defaultValue() {
            return ManaData.INITIAL;
        }
    }

    /** 护盾 capability。 */
    public static class ShieldCapability extends PlayerDataCapability<ShieldData, ShieldCapability> {
        public ShieldCapability() {
            super(ShieldData.EMPTY);
        }

        @Override
        protected Capability<ShieldCapability> capability() {
            return SHIELD;
        }

        @Override
        protected ShieldData read(CompoundTag tag) {
            return ShieldData.load(tag);
        }

        @Override
        protected void write(ShieldData value, CompoundTag tag) {
            tag.merge(value.save());
        }

        @Override
        public ShieldData defaultValue() {
            return ShieldData.EMPTY;
        }
    }

    /** 已学法术 capability。 */
    public static class LearnedSpellsCapability extends PlayerDataCapability<LearnedSpellsData, LearnedSpellsCapability> {
        public LearnedSpellsCapability() {
            super(LearnedSpellsData.EMPTY);
        }

        @Override
        protected Capability<LearnedSpellsCapability> capability() {
            return LEARNED_SPELLS;
        }

        @Override
        protected LearnedSpellsData read(CompoundTag tag) {
            return LearnedSpellsData.load(tag);
        }

        @Override
        protected void write(LearnedSpellsData value, CompoundTag tag) {
            tag.merge(value.save());
        }

        @Override
        public LearnedSpellsData defaultValue() {
            return LearnedSpellsData.EMPTY;
        }
    }

    /** 选中法术 capability。 */
    public static class SelectedSpellCapability extends PlayerDataCapability<SelectedSpellData, SelectedSpellCapability> {
        public SelectedSpellCapability() {
            super(SelectedSpellData.EMPTY);
        }

        @Override
        protected Capability<SelectedSpellCapability> capability() {
            return SELECTED_SPELL;
        }

        @Override
        protected SelectedSpellData read(CompoundTag tag) {
            return SelectedSpellData.load(tag);
        }

        @Override
        protected void write(SelectedSpellData value, CompoundTag tag) {
            tag.merge(value.save());
        }

        @Override
        public SelectedSpellData defaultValue() {
            return SelectedSpellData.EMPTY;
        }
    }

    /** 飞行 capability。 */
    public static class FlightCapability extends PlayerDataCapability<FlightData, FlightCapability> {
        public FlightCapability() {
            super(FlightData.INACTIVE);
        }

        @Override
        protected Capability<FlightCapability> capability() {
            return FLIGHT;
        }

        @Override
        protected FlightData read(CompoundTag tag) {
            return FlightData.load(tag);
        }

        @Override
        protected void write(FlightData value, CompoundTag tag) {
            tag.merge(value.save());
        }

        @Override
        public FlightData defaultValue() {
            return FlightData.INACTIVE;
        }
    }

    /** 玩家委托 capability。 */
    public static class QuestCapability extends PlayerDataCapability<QuestData, QuestCapability> {
        public QuestCapability() {
            super(QuestData.EMPTY);
        }

        @Override
        protected Capability<QuestCapability> capability() {
            return QUEST;
        }

        @Override
        protected QuestData read(CompoundTag tag) {
            return QuestData.load(tag);
        }

        @Override
        protected void write(QuestData value, CompoundTag tag) {
            tag.merge(value.save());
        }

        @Override
        public QuestData defaultValue() {
            return QuestData.EMPTY;
        }
    }

    /**
     * 村民委托 capability。
     *
     * <p>挂的是村民实体而不是玩家，但序列化机制完全相同，
     * 所以复用 {@link PlayerDataCapability} 基类。
     */
    public static class QuestVillagerCapability
            extends PlayerDataCapability<QuestVillagerData, QuestVillagerCapability> {
        public QuestVillagerCapability() {
            super(QuestVillagerData.EMPTY);
        }

        @Override
        protected Capability<QuestVillagerCapability> capability() {
            return QUEST_VILLAGER;
        }

        @Override
        protected QuestVillagerData read(CompoundTag tag) {
            return QuestVillagerData.load(tag);
        }

        @Override
        protected void write(QuestVillagerData value, CompoundTag tag) {
            tag.merge(value.save());
        }

        @Override
        public QuestVillagerData defaultValue() {
            return QuestVillagerData.EMPTY;
        }
    }
}
