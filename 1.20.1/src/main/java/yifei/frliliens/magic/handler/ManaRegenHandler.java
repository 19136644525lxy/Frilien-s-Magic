package yifei.frliliens.magic.handler;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;

import yifei.frliliens.magic.capability.PlayerDataHelper;
import yifei.frliliens.magic.attachment.ManaData;

/**
 * 魔力自然恢复处理器。
 *
 * <p>每 5 秒（100 tick）恢复 1 点魔力，仅在服务端生效。
 */
public class ManaRegenHandler {

    /** 恢复间隔（tick），5 秒。 */
    private static final int REGEN_INTERVAL = 100;
    /** 每次恢复量。 */
    private static final int REGEN_AMOUNT = 1;

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 1.20.1 没有 Post 子类，必须判 phase 否则一条 tick 触发两次
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide) {
            return;
        }
        // 只在间隔到达时处理
        if (player.tickCount % REGEN_INTERVAL != 0) {
            return;
        }
        ManaData mana = PlayerDataHelper.mana(player);
        if (mana.current() >= mana.max()) {
            return;
        }
        int newCurrent = Math.min(mana.current() + REGEN_AMOUNT, mana.max());
        PlayerDataHelper.setMana(player, new ManaData(newCurrent, mana.max()));
        PlayerDataHelper.syncMana(player);
    }
}
