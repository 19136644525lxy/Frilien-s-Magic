package yifei.frliliens.magic.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import yifei.frliliens.magic.FriliensMagic;
import yifei.frliliens.magic.attachment.ManaData;
import yifei.frliliens.magic.attachment.ModAttachments;
import yifei.frliliens.magic.config.Config;

/**
 * 魔力条 HUD 渲染器。
 *
 * <p>功能：
 * <ul>
 *   <li>左上角默认显示魔力条（位置可通过编辑界面拖动）</li>
 *   <li>显隐可通过按键切换</li>
 *   <li>位置与显隐状态持久化到配置文件</li>
 * </ul>
 *
 * <p>编辑模式通过打开 {@link ManaBarEditScreen} 实现（释放鼠标以便拖动）。
 */
public final class ManaBarRenderer {

    // 纹理资源
    private static final ResourceLocation FRAME =
            ResourceLocation.fromNamespaceAndPath(FriliensMagic.MODID, "textures/gui/mana_progress_bar_frame.png");
    private static final ResourceLocation FILL =
            ResourceLocation.fromNamespaceAndPath(FriliensMagic.MODID, "textures/gui/magic_progress_bar.png");

    // 纹理总尺寸
    private static final int TEX_SIZE = 2048;

    // 外框 content bbox
    private static final int FRAME_U = 24;
    private static final int FRAME_V = 740;
    private static final int FRAME_U_W = 1880;
    private static final int FRAME_V_H = 776;

    // 外框内部凹槽（相对 content bbox）：左180, 上117, 宽1685, 高389
    private static final int SLOT_LEFT = 180;
    private static final int SLOT_TOP = 117;
    private static final int SLOT_W = 1685;
    private static final int SLOT_H = 389;

    // 填充条纹理内容区域：x145~1703, y860~1188
    private static final int FILL_U = 145;
    private static final int FILL_V = 860;
    private static final int FILL_U_W = 1628;
    private static final int FILL_V_H = 328;

    // 基准尺寸（scale=1.0 时）
    private static final int BASE_WIDTH = 160;
    private static final int BASE_HEIGHT = BASE_WIDTH * FRAME_V_H / FRAME_U_W;

    // 填充条目标高度：凹槽高度的 93%（少量纵向拉伸，不溢出）
    private static final int FILL_TEX_H = SLOT_H * 93 / 100;
    // 垂直居中于凹槽
    private static final int FILL_TEX_TOP = SLOT_TOP + (SLOT_H - FILL_TEX_H) / 2;

    private ManaBarRenderer() {
    }

    /** 当前缩放比例。 */
    public static float getScale() {
        return Config.MANA_BAR_SCALE.get().floatValue();
    }

    /** 魔力条渲染宽度（含缩放）。 */
    public static int getBarWidth() {
        return Math.round(BASE_WIDTH * getScale());
    }

    /** 魔力条渲染高度（含缩放）。 */
    public static int getBarHeight() {
        return Math.round(BASE_HEIGHT * getScale());
    }

    /** 切换魔力条显隐。 */
    public static void toggleVisible() {
        Config.MANA_BAR_VISIBLE.set(!Config.MANA_BAR_VISIBLE.get());
        saveConfig();
    }

    /** 打开魔力条位置编辑界面（释放鼠标以便拖动）。 */
    public static void openEditScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ManaBarEditScreen) {
            mc.setScreen(null);
        } else {
            mc.setScreen(new ManaBarEditScreen());
        }
    }

    /**
     * HUD 层渲染回调。
     */
    public static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();

        // 编辑界面打开时不在 HUD 层重复渲染
        if (mc.screen instanceof ManaBarEditScreen) {
            return;
        }
        if (!Config.MANA_BAR_VISIBLE.get()) {
            return;
        }

        LocalPlayer player = mc.player;
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();

        int barWidth = getBarWidth();
        int barHeight = getBarHeight();
        int x = Mth.clamp(Config.MANA_BAR_X.get(), 0, Math.max(0, screenWidth - barWidth));
        int y = Mth.clamp(Config.MANA_BAR_Y.get(), 0, Math.max(0, screenHeight - barHeight));

        ManaData mana = player != null ? player.getData(ModAttachments.MANA) : ManaData.INITIAL;
        renderBar(guiGraphics, x, y, mana, mc.font);
    }

    /**
     * 渲染魔力条本体（填充条 + 外框 + 文字），供 HUD 层和编辑界面共用。
     */
    public static void renderBar(GuiGraphics guiGraphics, int x, int y, ManaData mana, Font font) {
        float scale = getScale();
        int barWidth = getBarWidth();
        int barHeight = getBarHeight();
        float percent = mana.percent();

        // 填充条尺寸（按缩放）
        int fillOffsetX = Math.round(SLOT_LEFT * barWidth / (float) FRAME_U_W);
        int fillOffsetY = Math.round(FILL_TEX_TOP * barHeight / (float) FRAME_V_H);
        int fillWidth = Math.round(FILL_U_W * barWidth / (float) FRAME_U_W);
        int fillHeight = Math.round(FILL_TEX_H * barHeight / (float) FRAME_V_H);

        // 1) 填充条：宽度按百分比裁剪
        int fillDrawWidth = Math.max(0, Math.round(fillWidth * percent));
        int fillSrcWidth = Math.max(0, Math.round(FILL_U_W * percent));
        if (fillDrawWidth > 0 && fillSrcWidth > 0) {
            guiGraphics.blit(
                    FILL,
                    x + fillOffsetX, y + fillOffsetY,
                    fillDrawWidth, fillHeight,
                    FILL_U, FILL_V,
                    fillSrcWidth, FILL_V_H,
                    TEX_SIZE, TEX_SIZE);
        }

        // 2) 外框
        guiGraphics.blit(
                FRAME,
                x, y,
                barWidth, barHeight,
                FRAME_U, FRAME_V,
                FRAME_U_W, FRAME_V_H,
                TEX_SIZE, TEX_SIZE);

        // 3) 文字（可配置开关，随 GUI 缩放）
        if (Config.MANA_BAR_SHOW_TEXT.get()) {
            String text = mana.current() + " / " + mana.max();
            float textScale = getScale();
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x + barWidth / 2.0F, y + barHeight / 2.0F, 0);
            guiGraphics.pose().scale(textScale, textScale, 1.0F);
            guiGraphics.drawString(font, text, -font.width(text) / 2, -font.lineHeight / 2, 0xFFFFFF, true);
            guiGraphics.pose().popPose();
        }
    }

    private static void saveConfig() {
        try {
            Config.SPEC.save();
        } catch (Exception e) {
            FriliensMagic.LOGGER.error("Failed to save mana bar config", e);
        }
    }
}
