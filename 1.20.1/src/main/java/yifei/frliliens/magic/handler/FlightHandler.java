package yifei.frliliens.magic.handler;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;

import yifei.frliliens.magic.capability.PlayerDataHelper;
import yifei.frliliens.magic.attachment.FlightData;

/**
 * 飞行魔法处理器。
 *
 * <p>维持创造飞行状态，切换法术不会掉落，死亡后自动清除。
 */
public class FlightHandler {

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
        FlightData flight = PlayerDataHelper.flight(player);
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
        PlayerDataHelper.setFlight(player, FlightData.INACTIVE);
        var abilities = player.getAbilities();
        abilities.mayfly = false;
        abilities.flying = false;
        player.onUpdateAbilities();
        if (player instanceof ServerPlayer sp) {
            PlayerDataHelper.syncFlight(sp);
        }
    }
}
