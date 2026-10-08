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
import yifei.frliliens.magic.attachment.SelectedSpellData;
import yifei.frliliens.magic.spell.Spell;
import yifei.frliliens.magic.spell.SpellRegistry;

/**
 * 选择法术网络包。
 *
 * <p>客户端在轮盘中选择法术后发送到服务端，服务端校验是否已学习并更新选中状态。
 */
public record SelectSpellPayload(String spellId) implements CustomPacketPayload {

    public static final Type<SelectSpellPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(FriliensMagic.MODID, "select_spell"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectSpellPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, SelectSpellPayload::spellId,
                    SelectSpellPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** 服务端处理：校验学习状态并更新选中法术。 */
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            // 空字符串表示取消选择
            if (spellId.isEmpty()) {
                player.setData(ModAttachments.SELECTED_SPELL, SelectedSpellData.EMPTY);
                player.syncData(ModAttachments.SELECTED_SPELL);
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

            player.setData(ModAttachments.SELECTED_SPELL, new SelectedSpellData(spellId));
            player.syncData(ModAttachments.SELECTED_SPELL);
        });
    }

    /** 客户端发送选择请求。 */
    public static void send(String spellId) {
        PacketDistributor.sendToServer(new SelectSpellPayload(spellId));
    }
}
