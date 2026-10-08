package yifei.frliliens.magic.attachment;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import yifei.frliliens.magic.FriliensMagic;

/**
 * NeoForge 数据附件注册。
 *
 * <p>附件用于在实体上持久化存储数据（魔力、护盾状态等）。
 * {@code ShieldData} 挂在玩家实体上，配置了：
 * <ul>
 *   <li>{@code serialize} — 写入磁盘，死亡后保留</li>
 *   <li>{@code sync} — 自动同步到该玩家自己的客户端</li>
 *   <li>{@code copyOnDeath} — 复活后保留</li>
 * </ul>
 */
public final class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, FriliensMagic.MODID);

    /** 玩家护盾数据附件。 */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ShieldData>> SHIELD =
            ATTACHMENT_TYPES.register("shield", () -> AttachmentType.builder(() -> ShieldData.EMPTY)
                    .serialize(ShieldData.CODEC)
                    .sync(ShieldData.STREAM_CODEC)
                    .copyOnDeath()
                    .build());

    /** 玩家魔力数据附件。 */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ManaData>> MANA =
            ATTACHMENT_TYPES.register("mana", () -> AttachmentType.builder(() -> ManaData.INITIAL)
                    .serialize(ManaData.CODEC)
                    .sync(ManaData.STREAM_CODEC)
                    .copyOnDeath()
                    .build());

    /** 玩家已学法术数据附件。 */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<LearnedSpellsData>> LEARNED_SPELLS =
            ATTACHMENT_TYPES.register("learned_spells", () -> AttachmentType.builder(() -> LearnedSpellsData.EMPTY)
                    .serialize(LearnedSpellsData.CODEC)
                    .sync(LearnedSpellsData.STREAM_CODEC)
                    .copyOnDeath()
                    .build());

    /** 玩家当前选中的战斗法术附件。 */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SelectedSpellData>> SELECTED_SPELL =
            ATTACHMENT_TYPES.register("selected_spell", () -> AttachmentType.builder(() -> SelectedSpellData.EMPTY)
                    .serialize(SelectedSpellData.CODEC)
                    .sync(SelectedSpellData.STREAM_CODEC)
                    .copyOnDeath()
                    .build());

    /** 玩家飞行状态附件（死亡后清除）。 */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<FlightData>> FLIGHT =
            ATTACHMENT_TYPES.register("flight", () -> AttachmentType.builder(() -> FlightData.INACTIVE)
                    .serialize(FlightData.CODEC)
                    .sync(FlightData.STREAM_CODEC)
                    .build());

    private ModAttachments() {
    }
}
