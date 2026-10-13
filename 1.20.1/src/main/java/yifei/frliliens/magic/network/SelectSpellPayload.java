package yifei.frliliens.magic.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import yifei.frliliens.magic.attachment.LearnedSpellsData;
import yifei.frliliens.magic.attachment.SelectedSpellData;
import yifei.frliliens.magic.capability.PlayerDataHelper;
import yifei.frliliens.magic.item.StaffItem;
import yifei.frliliens.magic.spell.Spell;
import yifei.frliliens.magic.spell.SpellRegistry;
import yifei.frliliens.magic.spell.StaffSpells;

/**
 * 选择法术包（客户端 → 服务端）。
 *
 * <p>客户端在轮盘中选择法术后发送到服务端。服务端做三重校验：
 * 法术存在、玩家已学会、<b>手持的正是该法杖且该法杖支持此法术</b>。
 * 最后一条是防伪造的关键——归属表在客户端只用于显示，判定必须放在服务端。
 *
 * <p><b>与 1.21 版的差异</b>：{@code CustomPacketPayload} → 手写编解码；
 * 另外那边选中法术靠附件自动同步，这边写完必须显式发包，否则客户端轮盘看到的还是旧选择。
 */
public class SelectSpellPayload {

    private final String staffId;
    private final String spellId;

    public SelectSpellPayload(String staffId, String spellId) {
        this.staffId = staffId;
        this.spellId = spellId;
    }

    // ===== 编解码 =====

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(staffId);
        buf.writeUtf(spellId);
    }

    public static SelectSpellPayload decode(FriendlyByteBuf buf) {
        return new SelectSpellPayload(buf.readUtf(), buf.readUtf());
    }

    /** 服务端处理：校验后更新该法杖的选中法术。 */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) {
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

        LearnedSpellsData learned = PlayerDataHelper.learnedSpells(player);
        if (!learned.hasLearned(spellId)) {
            player.displayClientMessage(
                    Component.translatable("message.friliensmagic.spell_not_learned",
                            spell.getDisplayNameComponent()), true);
            return;
        }

        updateSelection(player, staffId, spellId);
    }

    /** 玩家主手或副手是否持有指定法杖。 */
    private static boolean isHolding(ServerPlayer player, String staffId) {
        return matches(player.getMainHandItem(), staffId)
                || matches(player.getOffhandItem(), staffId);
    }

    private static boolean matches(ItemStack stack, String staffId) {
        // 1.20.1 用 BuiltInRegistries 取注册名；1.21 的 builtInRegistryHolder 在这版不可用
        String actual = StaffItem.staffIdOf(stack);
        return actual != null && actual.equals(staffId);
    }

    private static void updateSelection(ServerPlayer player, String staffId, String spellId) {
        SelectedSpellData current = PlayerDataHelper.selectedSpell(player);
        if (PlayerDataHelper.setSelectedSpell(player, current.select(staffId, spellId))) {
            // 1.20.1 的 capability 不会自动同步，必须显式发包
            PlayerDataHelper.syncSelectedSpell(player);
        }
    }

    /** 客户端发送选择请求。 */
    public static void send(String staffId, String spellId) {
        ModNetwork.CHANNEL.sendToServer(new SelectSpellPayload(staffId, spellId));
    }
}
