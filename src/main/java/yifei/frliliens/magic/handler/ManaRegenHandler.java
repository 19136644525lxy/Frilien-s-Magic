package yifei.frliliens.magic.handler;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import yifei.frliliens.magic.attachment.ManaData;
import yifei.frliliens.magic.attachment.ModAttachments;

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
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        // 只在间隔到达时处理
        if (player.tickCount % REGEN_INTERVAL != 0) {
            return;
        }
        ManaData mana = player.getData(ModAttachments.MANA);
        if (mana.current() >= mana.max()) {
            return;
        }
        int newCurrent = Math.min(mana.current() + REGEN_AMOUNT, mana.max());
        player.setData(ModAttachments.MANA, new ManaData(newCurrent, mana.max()));
        player.syncData(ModAttachments.MANA);
    }
}
