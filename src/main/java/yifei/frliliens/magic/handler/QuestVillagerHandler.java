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
        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }
        if (event.getLevel().isClientSide()) {
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

        // 标记为委托村民
        villager.setData(ModAttachments.QUEST_VILLAGER, data.markAsQuestGiver());
        villager.setCustomName(Component.translatable("entity.friliensmagic.quest_villager"));
        villager.setCustomNameVisible(true);

        // 持续发光（穿墙可见轮廓）
        villager.setGlowingTag(true);

        // 不计入生物刷新上限：设置持久化，防止原版因 mob cap 而清除
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

        // 冷却检查
        long currentTick = player.serverLevel().getGameTime();
        if (data.isOnCooldown(currentTick)) {
            player.sendSystemMessage(Component.translatable("quest.friliensmagic.npc.cooldown"));
            return;
        }

        QuestMessages.sendQuestDialog(player, villager);
    }
}
