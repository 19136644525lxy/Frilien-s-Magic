package yifei.frliliens.magic.quest;

import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import yifei.frliliens.magic.attachment.ModAttachments;

/**
 * 委托命令注册与处理。
 *
 * <p>命令树：{@code /friliensmagic quest <accept|decline|claim|abandon> [villagerUuid]}
 * <p>原理：ClickEvent.RUN_COMMAND 触发服务端命令，天然防伪造。
 */
public final class QuestCommand {

    /** 注册命令树。 */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("friliensmagic")
                .then(Commands.literal("quest")
                        .then(Commands.literal("accept")
                                .then(Commands.argument("villager", StringArgumentType.string())
                                        .executes(QuestCommand::accept)))
                        .then(Commands.literal("decline")
                                .then(Commands.argument("villager", StringArgumentType.string())
                                        .executes(QuestCommand::decline)))
                        .then(Commands.literal("claim")
                                .then(Commands.argument("villager", StringArgumentType.string())
                                        .executes(QuestCommand::claim)))
                        .then(Commands.literal("abandon")
                                .executes(QuestCommand::abandon))));
    }

    /** 接受委托。 */
    private static int accept(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        QuestData quest = player.getData(ModAttachments.QUEST);

        if (!quest.isPending()) {
            player.sendSystemMessage(Component.translatable("quest.friliensmagic.info.no_pending"));
            return 0;
        }

        // 校验村民
        String uuidStr = StringArgumentType.getString(ctx, "villager");
        Villager villager = findVillager(player, uuidStr);
        if (villager == null) {
            player.sendSystemMessage(Component.translatable("quest.friliensmagic.villager_not_found"));
            return 0;
        }

        // 接受（冷却在领奖时设置，而非接受时）
        QuestManager.accept(player);
        return 1;
    }

    /** 拒绝委托。 */
    private static int decline(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        QuestManager.decline(player);
        return 1;
    }

    /** 领取奖励。 */
    private static int claim(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String uuidStr = StringArgumentType.getString(ctx, "villager");
        java.util.UUID uuid;
        try {
            uuid = java.util.UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            player.sendSystemMessage(Component.translatable("quest.friliensmagic.villager_not_found"));
            return 0;
        }
        if (!QuestManager.claim(player, uuid)) {
            player.sendSystemMessage(Component.translatable("quest.friliensmagic.info.no_quest"));
        }
        return 1;
    }

    /** 放弃委托。 */
    private static int abandon(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        QuestManager.abandon(player);
        return 1;
    }

    /**
     * 通过 UUID 在玩家附近查找委托村民。
     * 4 重校验：村民存在 + 是委托村民 + 距离 ≤8 格。
     */
    private static Villager findVillager(ServerPlayer player, String uuidStr) {
        try {
            UUID uuid = UUID.fromString(uuidStr);
            Entity entity = player.serverLevel().getEntity(uuid);
            if (!(entity instanceof Villager villager)) {
                return null;
            }
            if (!villager.getData(ModAttachments.QUEST_VILLAGER).isQuestGiver()) {
                return null;
            }
            // 距离检查（防远程执行）
            if (player.distanceToSqr(villager) > 64.0) {
                return null;
            }
            return villager;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** 命令注册事件入口。 */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }
}
