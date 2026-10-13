package yifei.frliliens.magic.handler;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.TickEvent;

import yifei.frliliens.magic.quest.QuestPoolAnnouncer;

/**
 * 委托数据包载入通报的驱动。
 *
 * <p>重载监听器只记下"本次扫到哪些命名空间"，真正的通报放在这里、
 * 延后一 tick 发出。这样能保证：
 * <ul>
 *   <li>所有重载监听器都已跑完，池子必然就绪</li>
 *   <li>一次 {@code /reload} 只发一条聚合消息，而不是每个命名空间刷一条</li>
 * </ul>
 */
public final class QuestAnnounceHandler {

    /** 每 tick 检查一次是否有待发的通报。 */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        // 1.20.1 没有 Post 子类，必须判 phase 否则一条 tick 触发两次
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        QuestPoolAnnouncer.tick(event.getServer());
    }

    /** 服务器关闭时清空记录，避免跨世界残留。 */
    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        QuestPoolAnnouncer.reset();
    }
}
