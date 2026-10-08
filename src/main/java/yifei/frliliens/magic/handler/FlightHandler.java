package yifei.frliliens.magic.handler;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import yifei.frliliens.magic.attachment.FlightData;
import yifei.frliliens.magic.attachment.ModAttachments;

/**
 * 飞行魔法处理器。
 *
 * <p>维持创造飞行状态，切换法术不会掉落，死亡后自动清除。
 */
public class FlightHandler {

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        FlightData flight = player.getData(ModAttachments.FLIGHT);
        if (flight.flying()) {
            var abilities = player.getAbilities();
            abilities.mayfly = true;
            abilities.flying = true;
            player.onUpdateAbilities();
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        // 死亡复活后清除飞行状态
        player.setData(ModAttachments.FLIGHT, FlightData.INACTIVE);
        var abilities = player.getAbilities();
        abilities.mayfly = false;
        abilities.flying = false;
        player.onUpdateAbilities();
        if (player instanceof ServerPlayer sp) {
            sp.syncData(ModAttachments.FLIGHT);
        }
    }
}
