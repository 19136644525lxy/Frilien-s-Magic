package yifei.frliliens.magic.quest;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;

import yifei.frliliens.magic.attachment.ModAttachments;

/**
 * 委托聊天 UI 构建。
 *
 * <p>通过 {@link ClickEvent.Action#RUN_COMMAND} 在聊天框中创建可点击按钮，
 * 玩家点击后执行对应命令完成委托操作。
 */
public final class QuestMessages {

    private QuestMessages() {
    }

    /**
     * 发送委托对话（右键村民时触发）。
     * 根据 QuestData 状态决定显示哪种对话。
     */
    public static void sendQuestDialog(ServerPlayer player, Villager villager) {
        QuestData quest = player.getData(ModAttachments.QUEST);

        if (!quest.hasQuest()) {
            // 无委托 → 生成新委托提议
            offerNewQuest(player, villager);
        } else if (quest.isPending()) {
            // 已有 pending → 重新显示
            sendQuestOffer(player, villager, quest);
        } else if (quest.isComplete()) {
            // 已完成 → 领取奖励
            sendClaimPrompt(player, villager);
        } else {
            // 进行中 → 进度
            sendQuestProgress(player, quest);
        }
    }

    /** 生成新委托提议。 */
    private static void offerNewQuest(ServerPlayer player, Villager villager) {
        String excludeId = player.getData(ModAttachments.QUEST).lastQuestId();
        // 从 lastQuestId 中提取 targetId（格式为 "kill:minecraft:zombie"）
        String excludeTarget = "";
        if (!excludeId.isEmpty() && excludeId.contains(":")) {
            int firstColon = excludeId.indexOf(':');
            excludeTarget = excludeId.substring(firstColon + 1);
        }
        QuestData pending = QuestRegistry.generateQuest(player.serverLevel(), excludeTarget);
        player.setData(ModAttachments.QUEST, pending);
        sendQuestOffer(player, villager, pending);
    }

    /** 发送委托提议 + 可点击按钮。 */
    private static void sendQuestOffer(ServerPlayer player, Villager villager, QuestData quest) {
        player.sendSystemMessage(Component.translatable("quest.friliensmagic.npc.greeting"));
        player.sendSystemMessage(buildQuestDescription(quest));

        String uuid = villager.getUUID().toString();
        MutableComponent buttons = Component.empty()
                .append(createButton("quest.friliensmagic.button.accept",
                        "/friliensmagic quest accept " + uuid,
                        "quest.friliensmagic.button.accept.hover"))
                .append(Component.literal("  "))
                .append(createButton("quest.friliensmagic.button.decline",
                        "/friliensmagic quest decline " + uuid,
                        "quest.friliensmagic.button.decline.hover"));
        player.sendSystemMessage(buttons);
    }

    /** 发送完成提示 + 领取按钮。 */
    private static void sendClaimPrompt(ServerPlayer player, Villager villager) {
        QuestData quest = player.getData(ModAttachments.QUEST);
        player.sendSystemMessage(Component.translatable("quest.friliensmagic.info.complete"));
        player.sendSystemMessage(buildQuestDescription(quest));

        MutableComponent buttons = Component.empty()
                .append(createButton("quest.friliensmagic.button.claim",
                        "/friliensmagic quest claim",
                        "quest.friliensmagic.button.claim.hover"))
                .append(Component.literal("  "))
                .append(createButton("quest.friliensmagic.button.abandon",
                        "/friliensmagic quest abandon",
                        "quest.friliensmagic.button.abandon.hover"));
        player.sendSystemMessage(buttons);
    }

    /** 发送进度信息。 */
    private static void sendQuestProgress(ServerPlayer player, QuestData quest) {
        player.sendSystemMessage(buildQuestDescription(quest));
        player.sendSystemMessage(Component.translatable("quest.friliensmagic.info.progress",
                quest.progress(), quest.required()));

        MutableComponent buttons = Component.empty()
                .append(createButton("quest.friliensmagic.button.abandon",
                        "/friliensmagic quest abandon",
                        "quest.friliensmagic.button.abandon.hover"));
        player.sendSystemMessage(buttons);
    }

    /** 构建委托描述组件。 */
    private static Component buildQuestDescription(QuestData quest) {
        Component targetName = resolveTargetName(quest);
        String key = quest.type() == QuestType.KILL
                ? "quest.friliensmagic.type.kill"
                : "quest.friliensmagic.type.collect";
        return Component.translatable(key, targetName, quest.required());
    }

    /** 解析目标名称组件（实体或物品）。 */
    private static Component resolveTargetName(QuestData quest) {
        ResourceLocation rl = ResourceLocation.parse(quest.targetId());
        if (quest.type() == QuestType.KILL) {
            var entityType = BuiltInRegistries.ENTITY_TYPE.get(rl);
            return entityType.getDescription();
        } else {
            Item item = BuiltInRegistries.ITEM.get(rl);
            return item.getDescription();
        }
    }

    /** 创建带 ClickEvent 的可点击按钮组件。 */
    private static Component createButton(String labelKey, String command, String hoverKey) {
        return Component.translatable(labelKey)
                .withStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.translatable(hoverKey)))
                        .withColor(ChatFormatting.YELLOW)
                        .withBold(true));
    }
}
