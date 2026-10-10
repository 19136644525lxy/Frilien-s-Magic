package yifei.frliliens.magic.quest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import yifei.frliliens.magic.attachment.ModAttachments;
import yifei.frliliens.magic.config.Config;

/**
 * 委托服务层。
 *
 * <p>负责委托的接受、放弃、完成、领奖等业务逻辑。
 * 数据通过 {@link ModAttachments#QUEST} 附件持久化。
 */
public final class QuestManager {

    private QuestManager() {
    }

    /** 玩家是否有进行中的委托（含 pending）。 */
    public static boolean hasQuest(ServerPlayer player) {
        return player.getData(ModAttachments.QUEST).hasQuest();
    }

    /** 接受 pending 委托。 */
    public static boolean accept(ServerPlayer player) {
        QuestData quest = player.getData(ModAttachments.QUEST);
        if (!quest.isPending()) {
            player.sendSystemMessage(Component.translatable("quest.friliensmagic.info.no_pending"));
            return false;
        }
        player.setData(ModAttachments.QUEST, quest.accept());
        player.sendSystemMessage(Component.translatable("quest.friliensmagic.accepted"));
        return true;
    }

    /** 拒绝 pending 委托。 */
    public static void decline(ServerPlayer player) {
        QuestData quest = player.getData(ModAttachments.QUEST);
        if (!quest.isPending()) {
            return;
        }
        player.setData(ModAttachments.QUEST, quest.clear());
        player.sendSystemMessage(Component.translatable("quest.friliensmagic.declined"));
    }

    /** 放弃当前委托。 */
    public static void abandon(ServerPlayer player) {
        QuestData quest = player.getData(ModAttachments.QUEST);
        if (!quest.isActive()) {
            return;
        }
        player.setData(ModAttachments.QUEST, quest.clear());
        player.sendSystemMessage(Component.translatable("quest.friliensmagic.abandoned"));
    }

    /**
     * 领取奖励。对于 COLLECT 类型，先校验并扣除物品。
     */
    public static boolean claim(ServerPlayer player, java.util.UUID villagerUuid) {
        QuestData quest = player.getData(ModAttachments.QUEST);
        if (!quest.isComplete()) {
            return false;
        }

        // 校验委托来源村民
        if (!quest.isFromVillager(villagerUuid)) {
            player.sendSystemMessage(Component.translatable("quest.friliensmagic.info.wrong_villager"));
            return false;
        }

        // COLLECT 类型：校验并扣除物品
        if (quest.type() == QuestType.COLLECT) {
            if (!removeItems(player, quest.targetId(), quest.required())) {
                player.sendSystemMessage(Component.translatable("quest.friliensmagic.items_insufficient"));
                return false;
            }
        }

        // 随机奖励
        QuestRegistry.RewardEntry reward = QuestRegistry.rollReward(player.serverLevel());
        ItemStack rewardStack = reward.createStack(player.serverLevel());

        // 先保存显示信息（add 会修改原栈导致 count 归零）
        Component rewardName = rewardStack.getHoverName();
        int rewardCount = rewardStack.getCount();

        // 给予奖励（背包满则掉落地上）
        if (!player.getInventory().add(rewardStack)) {
            player.drop(rewardStack, false);
        }

        // 提示
        player.sendSystemMessage(Component.translatable("quest.friliensmagic.reward_received",
                rewardName, rewardCount));

        // 音效
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.2F);

        // 设置村民冷却（领奖后才开始冷却，而非接受时）
        setVillagerCooldown(player, villagerUuid);

        // 清除委托（保留 lastQuestId 避免连续重复）
        player.setData(ModAttachments.QUEST, quest.clear());
        return true;
    }

    /**
     * 从背包扣除指定物品。
     *
     * @return true=扣除成功，false=数量不足
     */
    private static boolean removeItems(ServerPlayer player, String itemId, int amount) {
        Item target = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
        // 先检查总量
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(target)) {
                total += stack.getCount();
            }
        }
        if (total < amount) {
            return false;
        }
        // 扣除
        int remaining = amount;
        for (ItemStack stack : player.getInventory().items) {
            if (remaining <= 0) {
                break;
            }
            if (stack.is(target)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
        player.getInventory().setChanged();
        return true;
    }

    /** 设置村民冷却。 */
    public static void setVillagerCooldown(ServerPlayer player, java.util.UUID villagerUuid) {
        var entity = player.serverLevel().getEntity(villagerUuid);
        if (!(entity instanceof net.minecraft.world.entity.npc.Villager villager)) {
            return;
        }
        long tick = player.serverLevel().getGameTime();
        int cooldownTicks = Config.QUEST_VILLAGER_COOLDOWN.getAsInt();
        QuestVillagerData vd = villager.getData(ModAttachments.QUEST_VILLAGER);
        villager.setData(ModAttachments.QUEST_VILLAGER, vd.withCooldown(tick + cooldownTicks));
    }
}
