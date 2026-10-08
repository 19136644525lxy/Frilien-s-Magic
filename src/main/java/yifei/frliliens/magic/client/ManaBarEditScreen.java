package yifei.frliliens.magic.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import yifei.frliliens.magic.FriliensMagic;
import yifei.frliliens.magic.attachment.ManaData;
import yifei.frliliens.magic.attachment.ModAttachments;
import yifei.frliliens.magic.config.Config;

/**
 * 魔力条编辑界面。
 *
 * <p>功能：
 * <ul>
 *   <li>拖动魔力条调整位置</li>
 *   <li>鼠标滚轮缩放魔力条</li>
 *   <li>按钮切换魔力值文字显示</li>
 *   <li>按 ESC 退出并保存</li>
 * </ul>
 */
public final class ManaBarEditScreen extends Screen {

    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public ManaBarEditScreen() {
        super(Component.translatable("screen.friliensmagic.mana_bar_edit.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 不画默认背景，保持游戏画面清晰
    }

    @Override
    protected void init() {
        // 文字显示开关按钮
        boolean showText = Config.MANA_BAR_SHOW_TEXT.get();
        addRenderableWidget(Button.builder(
                buildTextToggleMessage(showText),
                btn -> {
                    boolean newVal = !Config.MANA_BAR_SHOW_TEXT.get();
                    Config.MANA_BAR_SHOW_TEXT.set(newVal);
                    btn.setMessage(buildTextToggleMessage(newVal));
                    saveConfig();
                })
                .bounds(this.width - 90, 10, 80, 20)
                .build());

        // 重置缩放按钮
        addRenderableWidget(Button.builder(
                Component.translatable("screen.friliensmagic.mana_bar_edit.reset_size"),
                btn -> {
                    Config.MANA_BAR_SCALE.set(1.0D);
                    saveConfig();
                })
                .bounds(this.width - 90, 35, 80, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int screenWidth = this.width;
        int screenHeight = this.height;

        int barWidth = ManaBarRenderer.getBarWidth();
        int barHeight = ManaBarRenderer.getBarHeight();

        // 读取位置并 clamp
        int x = Mth.clamp(Config.MANA_BAR_X.get(), 0, Math.max(0, screenWidth - barWidth));
        int y = Mth.clamp(Config.MANA_BAR_Y.get(), 0, Math.max(0, screenHeight - barHeight));

        // 渲染魔力条
        ManaData mana = minecraft.player != null
                ? minecraft.player.getData(ModAttachments.MANA)
                : ManaData.INITIAL;
        ManaBarRenderer.renderBar(guiGraphics, x, y, mana, minecraft.font);

        // 提示文字
        Component hint = Component.translatable("screen.friliensmagic.mana_bar_edit.hint");
        int textX = screenWidth / 2 - minecraft.font.width(hint) / 2;
        guiGraphics.drawString(minecraft.font, hint, textX, 20, 0xFFFFFF, true);

        // 当前缩放比例（保留两位小数）
        String scaleText = String.format("%.2f", Config.MANA_BAR_SCALE.get());
        Component scaleInfo = Component.translatable("screen.friliensmagic.mana_bar_edit.scale", scaleText);
        guiGraphics.drawString(minecraft.font, scaleInfo, 10, 10, 0xFFFFAA, true);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int x = Config.MANA_BAR_X.get();
        int y = Config.MANA_BAR_Y.get();
        int barWidth = ManaBarRenderer.getBarWidth();
        int barHeight = ManaBarRenderer.getBarHeight();
        if (mouseX >= x && mouseX <= x + barWidth && mouseY >= y && mouseY <= y + barHeight) {
            dragging = true;
            dragOffsetX = (int) mouseX - x;
            dragOffsetY = (int) mouseY - y;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!dragging || button != 0) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        int barWidth = ManaBarRenderer.getBarWidth();
        int barHeight = ManaBarRenderer.getBarHeight();
        int newX = Mth.clamp((int) mouseX - dragOffsetX, 0, this.width - barWidth);
        int newY = Mth.clamp((int) mouseY - dragOffsetY, 0, this.height - barHeight);
        Config.MANA_BAR_X.set(newX);
        Config.MANA_BAR_Y.set(newY);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            dragging = false;
            saveConfig();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // 滚轮缩放：每格 0.05
        double current = Config.MANA_BAR_SCALE.get();
        double next = Mth.clamp(current + scrollY * 0.05D, 0.5D, 3.0D);
        if (next != current) {
            Config.MANA_BAR_SCALE.set(next);
            saveConfig();
        }
        return true;
    }

    @Override
    public void onClose() {
        saveConfig();
        super.onClose();
    }

    /** 构建文字开关按钮的消息组件。 */
    private Component buildTextToggleMessage(boolean showText) {
        return Component.translatable("screen.friliensmagic.mana_bar_edit.text_toggle",
                Component.translatable(showText
                        ? "screen.friliensmagic.mana_bar_edit.on"
                        : "screen.friliensmagic.mana_bar_edit.off"));
    }

    private void saveConfig() {
        try {
            Config.SPEC.save();
        } catch (Exception e) {
            FriliensMagic.LOGGER.error("Failed to save mana bar config", e);
        }
    }
}
