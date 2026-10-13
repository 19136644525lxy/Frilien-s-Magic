package yifei.frliliens.magic.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 屏幕中央标题自适应缩放。
 *
 * <p>原版 {@code Gui.renderTitle} 把主标题写死为 4 倍、副标题 2 倍缩放，
 * 长中文标题（如「欢迎来到辛逝纪1314年，尊敬的魔法使」）在低分辨率或较大的
 * GUI 缩放下会超出屏幕两侧。这里在每帧渲染前按「文本未缩放宽度」与
 * 「GUI 可用宽度」的比值重算缩放：
 * <ul>
 *     <li>分辨率变化：GUI 空间宽度随之变化，缩放自动重算；</li>
 *     <li>界面尺寸（GUI 缩放选项）变化：guiWidth 变化，同样自动适配；</li>
 *     <li>大屏保持原版观感（主标题上限 4 倍），小屏自动缩小到留 10% 边距。</li>
 * </ul>
 */
@Mixin(Gui.class)
public abstract class GuiMixin {

    /** 标题最大占用屏幕宽度比例，两侧共留 10% 边距。 */
    @Unique
    private static final float FRILIENSMAGIC_MAX_WIDTH_RATIO = 0.9F;

    @Shadow
    private Minecraft minecraft;

    @Shadow
    private Component title;

    @Shadow
    private Component subtitle;

    /** 本帧实际使用的主标题 / 副标题缩放，HEAD 注入时重算。 */
    @Unique
    private float friliensmagic$titleScale = 4.0F;

    @Unique
    private float friliensmagic$subtitleScale = 2.0F;

    @Inject(method = "renderTitle", at = @At("HEAD"))
    private void friliensmagic$computeAdaptiveScale(GuiGraphics guiGraphics,
                                                    DeltaTracker deltaTracker,
                                                    CallbackInfo ci) {
        this.friliensmagic$titleScale = friliensmagic$fit(this.title, guiGraphics.guiWidth(), 4.0F);
        // 副标题与主标题保持原版 1:2 的比例，同时自身也要放得下
        this.friliensmagic$subtitleScale = Math.min(
                this.friliensmagic$titleScale * 0.5F,
                friliensmagic$fit(this.subtitle, guiGraphics.guiWidth(), 2.0F));
    }

    /** 替换主标题写死的 scale(4, 4, 4)。 */
    @ModifyArgs(method = "renderTitle",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V",
                    ordinal = 0))
    private void friliensmagic$applyTitleScale(Args args) {
        friliensmagic$setUniformScale(args, this.friliensmagic$titleScale);
    }

    /** 替换副标题写死的 scale(2, 2, 2)；副标题可能为 null，故不强制存在。 */
    @ModifyArgs(method = "renderTitle",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V",
                    ordinal = 1),
            require = 0)
    private void friliensmagic$applySubtitleScale(Args args) {
        friliensmagic$setUniformScale(args, this.friliensmagic$subtitleScale);
    }

    @Unique
    private static void friliensmagic$setUniformScale(Args args, float scale) {
        args.set(0, scale);
        args.set(1, scale);
        args.set(2, scale);
    }

    /**
     * 计算自适应缩放：不超过原版基准；屏幕放不下时按宽度比值缩小（不设下限，保证始终完整显示）。
     *
     * @param text      待渲染文本，null 或空宽度时返回基准值
     * @param guiWidth  GUI 坐标系下的屏幕宽度（已随分辨率 / GUI 缩放变化）
     * @param baseScale 原版基准缩放（主标题 4，副标题 2）
     */
    @Unique
    private float friliensmagic$fit(Component text, int guiWidth, float baseScale) {
        if (text == null) {
            return baseScale;
        }
        int textWidth = this.minecraft.font.width(text);
        if (textWidth <= 0) {
            return baseScale;
        }
        return Math.min(baseScale, guiWidth * FRILIENSMAGIC_MAX_WIDTH_RATIO / textWidth);
    }
}
