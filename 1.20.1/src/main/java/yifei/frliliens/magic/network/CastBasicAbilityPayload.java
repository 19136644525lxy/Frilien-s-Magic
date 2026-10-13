package yifei.frliliens.magic.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import yifei.frliliens.magic.FriliensMagic;
import yifei.frliliens.magic.attachment.LearnedSpellsData;
import yifei.frliliens.magic.capability.PlayerDataHelper;
import yifei.frliliens.magic.spell.Spell;
import yifei.frliliens.magic.spell.SpellRegistry;

/**
 * 基础能力施法包（客户端 → 服务端）。
 *
 * <p>客户端按按键后发送法术 id，服务端校验学习状态后释放。
 * 基础能力不消耗魔力。
 *
 * <p><b>与 1.21 版的差异</b>：那边是 {@code CustomPacketPayload} + {@code StreamCodec}，
 * 1.20.1 改成 {@link FriendlyByteBuf} 手写编解码 + {@code SimpleChannel}。
 * 载荷本身很简单（一个字符串），手写反而更直白。
 *
 * <p><b>安全性</b>：方向在注册时限定为 {@code PLAY_TO_SERVER}，
 * 服务端还会校验玩家确实学会了该法术，所以客户端伪造包也放不出没学的法术。
 */
public class CastBasicAbilityPayload {

    /** 要释放的法术 id。 */
    private final String spellId;

    public CastBasicAbilityPayload(String spellId) {
        this.spellId = spellId;
    }

    public String spellId() {
        return spellId;
    }

    // ===== 编解码 =====

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(spellId);
    }

    public static CastBasicAbilityPayload decode(FriendlyByteBuf buf) {
        return new CastBasicAbilityPayload(buf.readUtf());
    }

    /** 服务端处理：校验学习状态并释放法术。 */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        // 注册时用 consumerMainThread，这里已在主线程
        ServerPlayer player = ctx.get().getSender();
        if (player == null) {
            return;
        }

        Spell spell = SpellRegistry.get(spellId);
        if (spell == null) {
            return;
        }

        LearnedSpellsData learned = PlayerDataHelper.learnedSpells(player);
        if (!learned.hasLearned(spellId)) {
            player.displayClientMessage(
                    Component.translatable("message.friliensmagic.spell_not_learned",
                            spell.getDisplayNameComponent()), true);
            return;
        }

        spell.cast(player.level(), player);
    }

    /** 客户端发送施法请求到服务端。 */
    public static void send(String spellId) {
        ModNetwork.CHANNEL.sendToServer(new CastBasicAbilityPayload(spellId));
    }

    @Override
    public String toString() {
        return "CastBasicAbilityPayload[" + FriliensMagic.MODID + ":" + spellId + "]";
    }
}
