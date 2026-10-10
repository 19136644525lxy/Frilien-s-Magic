package yifei.frliliens.magic.handler;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import yifei.frliliens.magic.attachment.ModAttachments;
import yifei.frliliens.magic.config.Config;
import yifei.frliliens.magic.entity.QuestVillagerEntity;
import yifei.frliliens.magic.quest.QuestData;
import yifei.frliliens.magic.quest.QuestMessages;
import yifei.frliliens.magic.quest.QuestVillagerData;

/**
 * 委托村民事件处理。
 *
 * <p>职责：
 * <ul>
 *   <li>村民刷新时概率标记为委托村民（EntityJoinLevelEvent）</li>
 *   <li>右键委托村民时拦截原版交易，显示委托对话（PlayerInteractEvent）</li>
 * </ul>
 */
public class QuestVillagerHandler {

    /**
     * 村民加入世界时概率标记为委托村民。
     * 原理：EntityJoinLevelEvent 在实体加入世界时触发（含区块加载）。
     * 附件有 serialize 所以持久化，不会重复标记。
     */
    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        // 自定义实体（刷怪蛋生成）——必定标记
        if (event.getEntity() instanceof QuestVillagerEntity qve) {
            QuestVillagerData data = qve.getData(ModAttachments.QUEST_VILLAGER);
            if (!data.isQuestGiver()) {
                markAsQuestVillager(qve, data);
            }
            return;
        }

        // 原版村民——概率标记
        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }

        QuestVillagerData data = villager.getData(ModAttachments.QUEST_VILLAGER);
        if (data.isQuestGiver()) {
            return; // 已标记
        }

        // 仅对傻子村民（Nitwit）生效
        if (villager.getVillagerData().getProfession() != VillagerProfession.NITWIT) {
            return;
        }

        // 概率判定
        float chance = (float) Config.QUEST_VILLAGER_CHANCE.getAsDouble();
        if (villager.level().random.nextFloat() > chance) {
            return;
        }

        markAsQuestVillager(villager, data);
    }

    /** 统一标记委托村民属性。 */
    private void markAsQuestVillager(Villager villager, QuestVillagerData data) {
        villager.setData(ModAttachments.QUEST_VILLAGER, data.markAsQuestGiver());
        villager.setCustomName(Component.translatable("entity.friliensmagic.quest_villager"));
        villager.setCustomNameVisible(true);
        villager.setGlowingTag(true);
        villager.setPersistenceRequired();
    }

    /**
     * 右键委托村民：拦截原版交易，显示委托对话。
     * 原理：cancel 后原版交易界面不会打开。
     */
    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Villager villager)) {
            return;
        }

        QuestVillagerData data = villager.getData(ModAttachments.QUEST_VILLAGER);
        if (!data.isQuestGiver()) {
            return;
        }

        // 拦截原版交易
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);

        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
            return;
        }

        // 冷却检查：无委托或待定委托来自其他村民时需要检查（会生成新委托）
        QuestData quest = player.getData(ModAttachments.QUEST);
        boolean needNewQuest = !quest.hasQuest()
                || (quest.isPending() && !quest.isFromVillager(villager.getUUID()));
        if (needNewQuest) {
            long currentTick = player.serverLevel().getGameTime();
            if (data.isOnCooldown(currentTick)) {
                player.sendSystemMessage(Component.translatable("quest.friliensmagic.npc.cooldown"));
                return;
            }
        }

        QuestMessages.sendQuestDialog(player, villager);
    }
}
