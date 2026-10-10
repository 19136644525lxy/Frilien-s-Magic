package yifei.frliliens.magic.handler;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import yifei.frliliens.magic.attachment.ModAttachments;
import yifei.frliliens.magic.config.Config;
import yifei.frliliens.magic.quest.QuestData;
import yifei.frliliens.magic.quest.QuestType;

/**
 * 委托进度追踪。
 *
 * <p>职责：
 * <ul>
 *   <li>击杀追踪：LivingDeathEvent 监听玩家击杀匹配的实体</li>
 *   <li>收集检查：PlayerTickEvent.Post 周期检查背包物品数量</li>
 * </ul>
 */
public class QuestProgressHandler {

    /** 击杀追踪。 */
    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        Entity source = event.getSource().getEntity();
        if (!(source instanceof ServerPlayer player)) {
            return;
        }

        QuestData quest = player.getData(ModAttachments.QUEST);
        if (!quest.isActive() || quest.type() != QuestType.KILL) {
            return;
        }
        if (quest.isComplete()) {
            return;
        }

        // 匹配实体类型
        ResourceLocation killedId = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        if (!quest.targetId().equals(killedId.toString())) {
            return;
        }

        // 增加进度
        int newProgress = quest.progress() + 1;
        player.setData(ModAttachments.QUEST, quest.withProgress(newProgress));

        if (newProgress >= quest.required()) {
            player.sendSystemMessage(Component.translatable("quest.friliensmagic.info.complete"));
        }
    }

    /**
     * 收集检查：周期检查背包。
     * 每 100 tick 检查一次，减少性能开销。
     */
    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        int interval = Config.QUEST_COLLECT_CHECK_INTERVAL.getAsInt();
        if (player.tickCount % interval != 0) {
            return;
        }

        if (!(player instanceof ServerPlayer sp)) {
            return;
        }

        QuestData quest = sp.getData(ModAttachments.QUEST);
        if (!quest.isActive() || quest.type() != QuestType.COLLECT) {
            return;
        }
        if (quest.isComplete()) {
            return;
        }

        // 统计物品数量
        int count = countItems(sp, quest.targetId());
        int newProgress = Math.min(count, quest.required());
        if (newProgress != quest.progress()) {
            sp.setData(ModAttachments.QUEST, quest.withProgress(newProgress));
            if (newProgress >= quest.required()) {
                sp.sendSystemMessage(Component.translatable("quest.friliensmagic.info.complete"));
            }
        }
    }

    /** 统计玩家背包中指定物品数量。 */
    private static int countItems(ServerPlayer player, String itemId) {
        Item target = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(target)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
