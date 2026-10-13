package yifei.frliliens.magic.world;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 服务器社群信息的默认文案。
 *
 * <p>和欢迎语分开：欢迎语是<b>一次性</b>的（首次进入存档，标题形式），
 * 社群信息是<b>每次进入都发</b>的（聊天栏）。
 *
 * <p>默认文案走<b>翻译键</b>，由客户端按自己的语言渲染。链接用可点击组件
 * （点击打开浏览器），网址本身作为链接文字显示——这是跨语言都不需要翻译的做法。
 *
 * <h2>为什么一个键一行</h2>
 * 原版语言文件里的 {@code \n} 是<b>字面</b>的反斜杠加 n，翻译时不会被转成换行，
 * 整段会挤在一行显示。所以这里拆成三个键、每行一个，翻译文件里不出现换行符。
 *
 * <p>服主一般会改成自己服务器的信息，用
 * {@code /friliensmagic welcome join set <文本>} 覆盖即可（改后不再走翻译键）。
 */
public final class JoinInfo {

    /** 第一行：问候语。 */
    public static final String KEY_LINE1 = "friliensmagic.join.line1";
    /** 第二行：模组仓库（带一个链接参数）。 */
    public static final String KEY_LINE2 = "friliensmagic.join.line2";
    /** 第三行：Discord 邀请（带一个链接参数）。 */
    public static final String KEY_LINE3 = "friliensmagic.join.line3";

    /** 本模组仓库地址。 */
    public static final String REPO_URL =
            "https://github.com/19136644525lxy/Frilien-s-Magic.git";

    /** Discord 邀请地址。 */
    public static final String DISCORD_URL = "https://discord.gg/JGpKs6GDFC";

    private JoinInfo() {
    }

    /**
     * 构造默认（可本地化）的社群信息，每行一个独立组件。
     *
     * @return 逐行的组件列表，直接逐条发给玩家
     */
    public static List<Component> defaultMessage() {
        return List.of(
                Component.translatable(KEY_LINE1),
                Component.translatable(KEY_LINE2, link(REPO_URL)),
                Component.translatable(KEY_LINE3, link(DISCORD_URL)));
    }

    /** 把字面文本里的网址转成可点击链接；没有网址则原样返回。 */
    public static Component linkify(String text) {
        if (!text.contains(REPO_URL) && !text.contains(DISCORD_URL)) {
            return Component.literal(text);
        }
        // 用占位符替换，避免替换后的文本再次被匹配
        String replaced = text.replace(REPO_URL, "\u0000REPO\u0000")
                .replace(DISCORD_URL, "\u0000DISCORD\u0000");

        MutableComponent out = Component.empty();
        for (String part : replaced.split("\u0000", -1)) {
            if ("REPO".equals(part)) {
                out.append(link(REPO_URL));
            } else if ("DISCORD".equals(part)) {
                out.append(link(DISCORD_URL));
            } else if (!part.isEmpty()) {
                out.append(Component.literal(part));
            }
        }
        return out;
    }

    private static MutableComponent link(String url) {
        return Component.literal(url).withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url)));
    }
}
