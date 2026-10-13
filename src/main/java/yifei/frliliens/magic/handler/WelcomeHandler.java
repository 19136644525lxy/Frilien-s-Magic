package yifei.frliliens.magic.handler;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import yifei.frliliens.magic.config.Config;
import yifei.frliliens.magic.world.JoinInfo;
import yifei.frliliens.magic.world.WelcomeData;

/**
 * 玩家进入存档时的两段提示。
 *
 * <h2>① 社群信息 —— 每次进入都发（聊天栏）</h2>
 * 模组仓库与 Discord 邀请，属于「服务器公告」性质，所以每次都发。
 * 默认走翻译键，链接是可点击组件。
 *
 * <h2>② 欢迎语 —— 只在首次进入时显示（标题）</h2>
 * 屏幕中央的标题，一次性。聊天栏的消息容易被冲掉，标题更适合入场致意。
 * 「首次」按<b>存档</b>判定，记录存在主世界的 {@code data/} 里，随存档走。
 *
 * <h2>文案来源与语言</h2>
 * 两者都是三层：单玩家自定义（仅欢迎语）→ 全局自定义 → 全局默认翻译键。
 * 自定义文本是服主写死的字面文本，原样发送；<b>只有默认走翻译键</b>，
 * 由客户端按自己的语言渲染——服务端拿不到客户端语言设置。
 */
public final class WelcomeHandler {

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        WelcomeData data = WelcomeData.get(player.server);

        sendJoinInfo(player, data);
        sendWelcome(player, data);
    }

    // ===== ① 社群信息：每次进入 =====

    private static void sendJoinInfo(ServerPlayer player, WelcomeData data) {
        if (data.isJoinDisabled()) {
            return;
        }
        if (data.hasJoinCustom()) {
            for (String line : WelcomeData.lines(data.joinCustom())) {
                // 自定义文本里若含网址，也转成可点击链接
                player.displayClientMessage(JoinInfo.linkify(line), false);
            }
            return;
        }
        // 默认：每行一个独立组件，逐条发送（语言文件里不含换行符）
        for (Component line : JoinInfo.defaultMessage()) {
            player.displayClientMessage(line, false);
        }
    }

    // ===== ② 欢迎语：仅首次 =====

    private static void sendWelcome(ServerPlayer player, WelcomeData data) {
        if (data.hasWelcomed(player.getUUID())) {
            return;   // 这个存档里已经看过
        }

        // 单玩家设置优先
        if (data.isPlayerDisabled(player.getUUID())) {
            return;
        }
        if (data.hasPlayerMessage(player.getUUID())) {
            showLines(player, WelcomeData.lines(data.playerMessage(player.getUUID())));
            data.markWelcomed(player.getUUID());
            return;
        }

        // 再看全局
        if (data.isDisabled()) {
            return;                 // 不标记，之后打开还能看到
        }
        if (data.hasCustom()) {
            showLines(player, WelcomeData.lines(data.customMessage()));
            data.markWelcomed(player.getUUID());
            return;
        }

        // 最后是默认：发翻译键，客户端按自己的语言渲染
        show(player, Component.translatable(WelcomeData.KEY_DEFAULT), null);
        data.markWelcomed(player.getUUID());
    }

    /** 显示多行字面文本：第一行标题，其余副标题。 */
    private static void showLines(ServerPlayer player, List<String> lines) {
        if (lines.isEmpty()) {
            return;
        }
        Component title = Component.literal(lines.get(0));
        Component subtitle = lines.size() > 1
                ? Component.literal(String.join("\n", lines.subList(1, lines.size())))
                : null;
        show(player, title, subtitle);
    }

    private static void show(ServerPlayer player, Component title, Component subtitle) {
        // 必须先设时长再发文本：顺序反了会沿用上一次的时长（或默认值）
        player.connection.send(new ClientboundSetTitlesAnimationPacket(
                Config.WELCOME_TITLE_FADE_IN.getAsInt(),
                Config.WELCOME_TITLE_STAY.getAsInt(),
                Config.WELCOME_TITLE_FADE_OUT.getAsInt()));

        if (subtitle != null) {
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        }
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }
}
