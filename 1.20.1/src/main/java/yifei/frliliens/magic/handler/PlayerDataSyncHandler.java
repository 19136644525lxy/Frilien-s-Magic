package yifei.frliliens.magic.handler;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import yifei.frliliens.magic.capability.PlayerDataHelper;
import yifei.frliliens.magic.capability.PlayerDataKinds;

/**
 * 玩家进服时把全部玩家数据推给客户端。
 *
 * <p><b>为什么需要</b>：1.20.1 的 capability 不会自动同步，客户端只在
 * 服务端「主动发包」时才有数据。如果不做这次初始同步，玩家刚进游戏时：
 * <ul>
 *   <li>魔力条要等到第一次自然回复（最多 5 秒）才显示数值</li>
 *   <li>法术轮盘看到的是空选择</li>
 *   <li>护盾（如果已有）不会渲染</li>
 * </ul>
 *
 * <p>1.21 版的附件带 {@code sync()}，登录时由附件系统自动推送，所以那边不需要这一步。
 */
public final class PlayerDataSyncHandler {

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // 逐个推一遍。顺序无所谓——客户端按标识存，互不覆盖。
        PlayerDataHelper.syncMana(player);
        PlayerDataHelper.syncShield(player);
        PlayerDataHelper.syncLearnedSpells(player);
        PlayerDataHelper.syncSelectedSpell(player);
        PlayerDataHelper.syncFlight(player);
        PlayerDataHelper.syncQuest(player);
    }

    /** 供自检使用：需要初始同步的数据项数量。 */
    public static int syncedKindCount() {
        return PlayerDataKinds.KINDS.size();
    }
}
