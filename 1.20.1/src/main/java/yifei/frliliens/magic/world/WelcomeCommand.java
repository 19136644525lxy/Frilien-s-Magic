package yifei.frliliens.magic.world;

import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.RegisterCommandsEvent;

/**
 * 欢迎语命令。
 *
 * <pre>
 * /friliensmagic welcome get [玩家]               查看（全局，或某玩家）
 * /friliensmagic welcome set &lt;文本&gt;                设置全局自定义文本
 * /friliensmagic welcome set &lt;玩家&gt; &lt;文本&gt;         设置某个玩家专属文本
 * /friliensmagic welcome reset [玩家]             恢复默认（走翻译键）
 * /friliensmagic welcome clear [玩家]             关闭提示
 * /friliensmagic welcome forget [玩家]            清空「已提示」记录
 * </pre>
 *
 * <h2>文案优先级</h2>
 * 单玩家自定义 → 全局自定义 → 全局默认（<b>翻译键</b>，客户端按自己语言渲染）。
 *
 * <h2>中文与空格</h2>
 * 参数用 {@link StringArgumentType#greedyString()} 而不是 {@code string()}：
 * {@code string()} 遇到空格就截断，而欢迎语常带空格和标点。
 * 中文不需要任何额外处理——客户端把命令文本原样编码成 UTF-8 发送，
 * 服务端 Brigadier 按字符读取。
 *
 * <h2>换行</h2>
 * 命令里敲不了真换行，用字面 {@code \n} 表示，发送时还原成多行。
 *
 * <p>需要权限等级 2（管理员）。
 */
public final class WelcomeCommand {

    /** 命令回复的翻译键前缀。 */
    private static final String MSG = "friliensmagic.welcome.cmd.";

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("friliensmagic")
                .then(Commands.literal("welcome")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("get")
                                .executes(WelcomeCommand::getGlobal)
                                .then(playerArg("player").executes(WelcomeCommand::getPlayer)))
                        .then(Commands.literal("set")
                                // set <玩家> <文本> —— 必须先于 set <文本> 注册，
                                // 否则 "set 玩家名 文本" 会被当成 set <文本> 整串吃掉
                                .then(playerArg("player")
                                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                                .executes(WelcomeCommand::setPlayer)))
                                .then(Commands.argument("text", StringArgumentType.greedyString())
                                        .executes(WelcomeCommand::setGlobal)))
                        .then(Commands.literal("reset")
                                .executes(WelcomeCommand::resetGlobal)
                                .then(playerArg("player").executes(WelcomeCommand::resetPlayer)))
                        .then(Commands.literal("clear")
                                .executes(WelcomeCommand::clearGlobal)
                                .then(playerArg("player").executes(WelcomeCommand::clearPlayer)))
                        .then(Commands.literal("forget")
                                .executes(WelcomeCommand::forgetAll)
                                .then(playerArg("player").executes(WelcomeCommand::forgetPlayer)))
                        // 社群信息（每次进入都发，与欢迎语独立）
                        .then(Commands.literal("join")
                                .then(Commands.literal("get").executes(WelcomeCommand::joinGet))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                                .executes(WelcomeCommand::joinSet)))
                                .then(Commands.literal("reset").executes(WelcomeCommand::joinReset))
                                .then(Commands.literal("clear").executes(WelcomeCommand::joinClear)))));
    }

    /** 「目标玩家」参数：用 word 而不是 string，以便后面再接 greedy 文本。 */
    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> playerArg(
            String name) {
        return Commands.argument(name, StringArgumentType.word());
    }

    // ===== get =====

    private static int getGlobal(CommandContext<CommandSourceStack> ctx) {
        WelcomeData data = WelcomeData.get(server(ctx));
        if (data.isDisabled()) {
            reply(ctx, MSG + "disabled");
            return 1;
        }
        if (data.hasCustom()) {
            reply(ctx, MSG + "global_custom", data.lines(data.customMessage()).size(),
                    data.welcomedCount());
            for (String line : WelcomeData.lines(data.customMessage())) {
                reply(ctx, MSG + "line", line);
            }
        } else {
            reply(ctx, MSG + "global_default", data.welcomedCount());
            // 把默认文案也回显一份，方便管理员确认
            reply(ctx, MSG + "line", Component.translatable(WelcomeData.KEY_DEFAULT));
        }
        if (data.playerMessageCount() > 0) {
            reply(ctx, MSG + "player_count", data.playerMessageCount());
        }
        return 1;
    }

    private static int getPlayer(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer target = resolvePlayer(ctx);
        if (target == null) {
            return 0;
        }
        WelcomeData data = WelcomeData.get(server(ctx));
        if (data.isPlayerDisabled(target.getUUID())) {
            reply(ctx, MSG + "player_disabled", target.getDisplayName());
        } else if (data.hasPlayerMessage(target.getUUID())) {
            reply(ctx, MSG + "player_custom", target.getDisplayName(),
                    data.lines(data.playerMessage(target.getUUID())).size());
            for (String line : WelcomeData.lines(data.playerMessage(target.getUUID()))) {
                reply(ctx, MSG + "line", line);
            }
        } else {
            reply(ctx, MSG + "player_inherit", target.getDisplayName());
        }
        return 1;
    }

    // ===== set =====

    private static int setGlobal(CommandContext<CommandSourceStack> ctx) {
        String text = StringArgumentType.getString(ctx, "text");
        if (text.isBlank()) {
            reply(ctx, MSG + "empty_use_clear");
            return 0;
        }
        WelcomeData.get(server(ctx)).setCustomMessage(text);
        reply(ctx, MSG + "set_global", text);
        return 1;
    }

    private static int setPlayer(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer target = resolvePlayer(ctx);
        if (target == null) {
            return 0;
        }
        String text = StringArgumentType.getString(ctx, "text");
        if (text.isBlank()) {
            reply(ctx, MSG + "empty_use_clear");
            return 0;
        }
        WelcomeData.get(server(ctx)).setPlayerMessage(target.getUUID(), text);
        reply(ctx, MSG + "set_player", target.getDisplayName(), text);
        return 1;
    }

    // ===== reset =====

    private static int resetGlobal(CommandContext<CommandSourceStack> ctx) {
        WelcomeData.get(server(ctx)).resetToDefault();
        reply(ctx, MSG + "reset_global");
        // 顺带回显默认文案的翻译键，让管理员知道玩家看到的是什么
        reply(ctx, MSG + "line", Component.translatable(WelcomeData.KEY_DEFAULT));
        return 1;
    }

    private static int resetPlayer(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer target = resolvePlayer(ctx);
        if (target == null) {
            return 0;
        }
        WelcomeData.get(server(ctx)).clearPlayerMessage(target.getUUID());
        reply(ctx, MSG + "reset_player", target.getDisplayName());
        return 1;
    }

    // ===== clear =====

    private static int clearGlobal(CommandContext<CommandSourceStack> ctx) {
        WelcomeData.get(server(ctx)).disable();
        reply(ctx, MSG + "clear_global");
        return 1;
    }

    private static int clearPlayer(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer target = resolvePlayer(ctx);
        if (target == null) {
            return 0;
        }
        WelcomeData.get(server(ctx)).disablePlayer(target.getUUID());
        reply(ctx, MSG + "clear_player", target.getDisplayName());
        return 1;
    }

    // ===== forget =====

    private static int forgetAll(CommandContext<CommandSourceStack> ctx) {
        WelcomeData data = WelcomeData.get(server(ctx));
        int before = data.welcomedCount();
        data.clearWelcomed();
        reply(ctx, MSG + "forget_all", before);
        return 1;
    }

    private static int forgetPlayer(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer target = resolvePlayer(ctx);
        if (target == null) {
            return 0;
        }
        WelcomeData.get(server(ctx)).clearWelcomed(target.getUUID());
        reply(ctx, MSG + "forget_player", target.getDisplayName());
        return 1;
    }

    // ===== 社群信息（join 子命令）=====

    private static int joinGet(CommandContext<CommandSourceStack> ctx) {
        WelcomeData data = WelcomeData.get(server(ctx));
        if (data.isJoinDisabled()) {
            reply(ctx, MSG + "join_disabled");
        } else if (data.hasJoinCustom()) {
            reply(ctx, MSG + "join_custom", data.lines(data.joinCustom()).size());
            for (String line : WelcomeData.lines(data.joinCustom())) {
                reply(ctx, MSG + "line", line);
            }
        } else {
            reply(ctx, MSG + "join_default");
            for (Component line : JoinInfo.defaultMessage()) {
                reply(ctx, MSG + "line", line);
            }
        }
        return 1;
    }

    private static int joinSet(CommandContext<CommandSourceStack> ctx) {
        String text = StringArgumentType.getString(ctx, "text");
        if (text.isBlank()) {
            reply(ctx, MSG + "empty_use_clear");
            return 0;
        }
        WelcomeData.get(server(ctx)).setJoinCustom(text);
        reply(ctx, MSG + "join_set", text);
        reply(ctx, MSG + "join_link_hint");
        return 1;
    }

    private static int joinReset(CommandContext<CommandSourceStack> ctx) {
        WelcomeData.get(server(ctx)).resetJoin();
        reply(ctx, MSG + "join_reset");
        for (Component line : JoinInfo.defaultMessage()) {
            reply(ctx, MSG + "line", line);
        }
        return 1;
    }

    private static int joinClear(CommandContext<CommandSourceStack> ctx) {
        WelcomeData.get(server(ctx)).disableJoin();
        reply(ctx, MSG + "join_clear");
        return 1;
    }

    // ===== 辅助 =====

    /**
     * 解析「目标玩家」参数。
     *
     * <p>只接受在线玩家——不在线的改不了，也没必要为此引入离线档案系统。
     */
    private static ServerPlayer resolvePlayer(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "player");
        ServerPlayer target = server(ctx).getPlayerList().getPlayerByName(name);
        if (target == null) {
            reply(ctx, MSG + "player_not_found", name);
        }
        return target;
    }

    private static MinecraftServer server(CommandContext<CommandSourceStack> ctx) {
        return ctx.getSource().getServer();
    }

    /** 发送一条翻译过的回复。 */
    private static void reply(CommandContext<CommandSourceStack> ctx, String key, Object... args) {
        ctx.getSource().sendSuccess(() -> Component.translatable(key, args), false);
    }
}
