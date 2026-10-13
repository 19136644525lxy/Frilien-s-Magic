package yifei.frliliens.magic.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import yifei.frliliens.magic.FriliensMagic;
import yifei.frliliens.magic.capability.ClientPlayerData;

/**
 * 客户端玩家数据缓存的维护。
 *
 * <p>1.20.1 的 capability 不会自动同步，客户端要主动缓存服务端推来的快照
 * （见 {@link ClientPlayerData}）。这里负责在进出世界时建立与清空缓存。
 *
 * <p>用 {@code value = Dist.CLIENT} 限定只在客户端加载——
 * 专用服务端没有这个类，避免引用客户端专属类型。
 */
@Mod.EventBusSubscriber(modid = FriliensMagic.MODID, value = Dist.CLIENT)
public final class ClientDataCacheHandler {

    /** 进入世界：记下本地玩家。 */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        ClientPlayerData.setLocalPlayer(event.getEntity());
    }

    /** 切换世界 / 重生：本地玩家实例变了，刷新引用。 */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.Clone event) {
        // 事件在两端都会触发，这里只处理客户端一侧
        if (event.getEntity().level().isClientSide) {
            ClientPlayerData.setLocalPlayer(event.getEntity());
        }
    }

    /** 退出世界：清空缓存，避免残留到下一个存档。 */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ClientPlayerData.clearAll();
    }

    private ClientDataCacheHandler() {
    }
}
