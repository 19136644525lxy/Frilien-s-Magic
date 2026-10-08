package yifei.frliliens.magic.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import yifei.frliliens.magic.FriliensMagic;
import yifei.frliliens.magic.attachment.LearnedSpellsData;
import yifei.frliliens.magic.attachment.ModAttachments;
import yifei.frliliens.magic.spell.Spell;
import yifei.frliliens.magic.spell.SpellRegistry;

/**
 * 基础能力施法网络包（NeoForge 1.21 CustomPayload）。
 *
 * <p>客户端按按键后发送法术 id，服务端校验学习状态后释放。
 * 基础能力不消耗魔力。
 */
public record CastBasicAbilityPayload(String spellId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CastBasicAbilityPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(FriliensMagic.MODID, "cast_basic"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CastBasicAbilityPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, CastBasicAbilityPayload::spellId,
                    CastBasicAbilityPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** 服务端处理：校验学习状态并释放法术。 */
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            Spell spell = SpellRegistry.get(spellId);
            if (spell == null) {
                return;
            }

            LearnedSpellsData learned = player.getData(ModAttachments.LEARNED_SPELLS);
            if (!learned.hasLearned(spellId)) {
                player.displayClientMessage(
                        Component.translatable("message.friliensmagic.spell_not_learned",
                                spell.getDisplayNameComponent()), true);
                return;
            }

            spell.cast(player.level(), player);
        });
    }

    /** 客户端发送施法请求到服务端。 */
    public static void send(String spellId) {
        PacketDistributor.sendToServer(new CastBasicAbilityPayload(spellId));
    }
}
