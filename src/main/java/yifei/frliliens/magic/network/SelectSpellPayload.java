package yifei.frliliens.magic.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import yifei.frliliens.magic.FriliensMagic;
import yifei.frliliens.magic.attachment.LearnedSpellsData;
import yifei.frliliens.magic.attachment.ModAttachments;
import yifei.frliliens.magic.attachment.SelectedSpellData;
import yifei.frliliens.magic.item.StaffItem;
import yifei.frliliens.magic.spell.Spell;
import yifei.frliliens.magic.spell.SpellRegistry;
import yifei.frliliens.magic.spell.StaffSpells;

/**
 * 选择法术网络包。
 *
 * <p>客户端在轮盘中选择法术后发送到服务端。服务端做三重校验：
 * 法术存在、玩家已学会、<b>手持的正是该法杖且该法杖支持此法术</b>。
 * 最后一条是防伪造的关键——归属表在客户端只用于显示，判定必须放在服务端。
 *
 * @param staffId 法杖的物品注册名
 * @param spellId 法术 id；空字符串表示清除该法杖的选择
 */
public record SelectSpellPayload(String staffId, String spellId) implements CustomPacketPayload {

    public static final Type<SelectSpellPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(FriliensMagic.MODID, "select_spell"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectSpellPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, SelectSpellPayload::staffId,
                    ByteBufCodecs.STRING_UTF8, SelectSpellPayload::spellId,
                    SelectSpellPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** 服务端处理：校验后更新该法杖的选中法术。 */
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            // 确认玩家手上拿的确实是这把法杖
            if (!isHolding(player, staffId)) {
                return;
            }

            // 空字符串表示清除该法杖的选择
            if (spellId.isEmpty()) {
                updateSelection(player, staffId, "");
                return;
            }

            Spell spell = SpellRegistry.get(spellId);
            if (spell == null) {
                return;
            }

            if (!StaffSpells.allows(staffId, spellId)) {
                player.displayClientMessage(
                        Component.translatable("message.friliensmagic.spell_wrong_staff",
                                spell.getDisplayNameComponent()),
                        true);
                return;
            }

            LearnedSpellsData learned = player.getData(ModAttachments.LEARNED_SPELLS);
            if (!learned.hasLearned(spellId)) {
                player.displayClientMessage(
                        Component.translatable("message.friliensmagic.spell_not_learned",
                                spell.getDisplayNameComponent()), true);
                return;
            }

            updateSelection(player, staffId, spellId);
        });
    }

    /** 玩家主手或副手是否持有指定法杖。 */
    private static boolean isHolding(ServerPlayer player, String staffId) {
        return matches(player.getMainHandItem(), staffId)
                || matches(player.getOffhandItem(), staffId);
    }

    private static boolean matches(ItemStack stack, String staffId) {
        return stack.getItem() instanceof StaffItem
                && staffId.equals(stack.getItem().builtInRegistryHolder().key().location().getPath());
    }

    private static void updateSelection(ServerPlayer player, String staffId, String spellId) {
        SelectedSpellData current = player.getData(ModAttachments.SELECTED_SPELL);
        player.setData(ModAttachments.SELECTED_SPELL, current.select(staffId, spellId));
        // setData 已自动同步（附件配置了 .sync），无需再调 syncData
    }

    /** 客户端发送选择请求。 */
    public static void send(String staffId, String spellId) {
        PacketDistributor.sendToServer(new SelectSpellPayload(staffId, spellId));
    }
}
