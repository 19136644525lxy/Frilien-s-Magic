package yifei.frliliens.magic.quest;

import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import yifei.frliliens.magic.FriliensMagic;

/**
 * 数据包委托池的载入通报。
 *
 * <p>目标：
 * <ul>
 *   <li>一次 {@code /reload} 只通报<b>一次</b>，而不是每个命名空间刷一条</li>
 *   <li>只通报<b>新出现</b>的命名空间——数据包没变就不吵</li>
 *   <li>把数据包删掉再放回来，会重新通报</li>
 * </ul>
 *
 * <p>实现方式：{@link #markLoaded} 在重载线程记下本次扫到的命名空间，
 * {@link #tick} 在服务器 tick 时对比上一次、把差异聚合后广播。
 * 延后一 tick 是有意的：那时重载已彻底结束，池子必然就绪。
 *
 * <h2>翻译</h2>
 * 通报用<b>固定</b>翻译键，与命名空间无关：
 * <pre>friliensmagic.quest_pool_loaded</pre>
 * 带两个参数：{@code %1$s} 是数据包数量，{@code %2$s} 是命名空间列表。
 *
 * <p>数据包作者把翻译放进 {@code assets/<任意命名空间>/lang/<语言>.json} 即可，
 * 因为客户端会遍历<b>所有</b>命名空间收集语言文件。放在示例里的做法：
 * <pre>assets/你的命名空间/lang/zh_cn.json
 * { "friliensmagic.quest_pool_loaded": "§b已载入 %1$s 个委托数据包：§f%2$s" }</pre>
 *
 * <p>客户端没装该数据包（比如服务端专用数据包）时翻译拿不到，用明文兜底，
 * 不会显示生硬的翻译键。
 */
public final class QuestPoolAnnouncer {

    /** 通报用的固定翻译键。 */
    public static final String KEY = "friliensmagic.quest_pool_loaded";

    /** 上一次重载扫到的、有贡献的命名空间。 */
    private static volatile Set<String> lastLoaded = Set.of();

    /** 本次重载扫到的命名空间，等待 tick 时处理。 */
    private static volatile Set<String> pending = null;

    private QuestPoolAnnouncer() {
    }

    /**
     * 由重载监听器调用：记下本次扫到的命名空间。
     *
     * <p>重载线程调用，只做一次 volatile 写。
     */
    static void markLoaded(Set<String> namespaces) {
        pending = Set.copyOf(namespaces);
    }

    /**
     * 由服务器 tick 调用：若有待处理的通报就发出去。
     *
     * @param server 服务器实例
     */
    public static void tick(MinecraftServer server) {
        Set<String> current = pending;
        if (current == null) {
            return;
        }
        pending = null;

        Set<String> previous = lastLoaded;
        lastLoaded = current;

        if (current.isEmpty()) {
            // 数据包把池子清空了？池子会回退内置默认值，这里不必吵
            if (!previous.isEmpty()) {
                FriliensMagic.LOGGER.info("All quest pool datapacks removed (reverted to built-in defaults)");
            }
            return;
        }

        Set<String> added = newlyLoaded(current, previous);
        if (added.isEmpty()) {
            FriliensMagic.LOGGER.debug("Quest pools reloaded, no namespace change: {}", current);
            return;
        }

        FriliensMagic.LOGGER.info("Loaded {} quest datapack(s): {}", added.size(), added);
        broadcast(server, added);
    }

    /**
     * 纯逻辑：本次相对上次「新出现」的命名空间。
     *
     * <p>抽成独立方法是为了能直接做单元式验证——它是「只通报一次」这条需求的核心，
     * 应该能脱离服务器单独测。
     *
     * @param current  本次扫到的
     * @param previous 上次扫到的
     * @return current 中不在 previous 里的部分（保持 current 的顺序）
     */
    public static Set<String> newlyLoaded(Set<String> current, Set<String> previous) {
        Set<String> added = new LinkedHashSet<>(current);
        added.removeAll(previous);
        return added;
    }

    /** 广播给所有在线玩家。 */
    private static void broadcast(MinecraftServer server, Set<String> namespaces) {
        Component message = buildMessage(namespaces);
        // 同时记一条日志：无人在线时也能确认通报确实发生了
        FriliensMagic.LOGGER.info("Quest load announcement: {}", message.getString());
        for (var player : server.getPlayerList().getPlayers()) {
            player.displayClientMessage(message, false);
        }
    }

    /** 组装通报文本。 */
    private static Component buildMessage(Set<String> namespaces) {
        String joined = String.join("、", namespaces);
        String fallback = "Loaded " + namespaces.size() + " quest datapack(s): " + joined;
        return Component.translatable(KEY, namespaces.size(), joined, fallback);
    }

    /** 供自检/调试读取当前已记录的命名空间。 */
    public static Set<String> loadedNamespaces() {
        return lastLoaded;
    }

    /** 服务器关闭时清空，避免跨世界残留。 */
    public static void reset() {
        lastLoaded = Set.of();
        pending = null;
    }
}
