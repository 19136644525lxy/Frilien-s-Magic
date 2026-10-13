package yifei.frliliens.magic.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;

/**
 * 屏幕中央标题自适应缩放（1.20.1 版）。
 *
 * <p>原版 {@code Gui.render} 把主标题写死为 4 倍、副标题 2 倍缩放，
 * 长中文标题在低分辨率或较大的 GUI 缩放下会超出屏幕两侧。
 * 1.20.1 中标题渲染内联在 render 方法里，故注入点为 render 方法内
 * 唯二的两次 {@code PoseStack.scale}（ordinal 0/1）。
 *
 * <p>标题文本不通过 {@code @Shadow} 读取私有字段，而是在
 * {@code setTitle}/{@code setSubtitle}/{@code clear} 调用时缓存到
 * {@code @Unique} 字段——生产环境为 SRG 命名，{@code @Shadow} 字段
 * 依赖 refmap 中的字段映射，规避映射缺失导致启动崩溃的风险。
 * 缩放公式与 1.21.1 NeoForge 版完全一致：
 * 大屏保持原版观感（上限 4 / 2 倍），小屏自动缩小到留 10% 边距。
 */
@Mixin(Gui.class)
public abstract class GuiMixin {

    /** 标题最大占用屏幕宽度比例，两侧共留 10% 边距。 */
    @Unique
    private static final float FRILIENSMAGIC_MAX_WIDTH_RATIO = 0.9F;

    /** 通过 setTitle/setSubtitle 拦截缓存的标题文本（clear 时清空）。 */
    @Unique
    private Component friliensmagic$cachedTitle;

    @Unique
    private Component friliensmagic$cachedSubtitle;

    /** 本帧实际使用的主标题 / 副标题缩放，render HEAD 注入时重算。 */
    @Unique
    private float friliensmagic$titleScale = 4.0F;

    @Unique
    private float friliensmagic$subtitleScale = 2.0F;

    @Inject(method = "setTitle", at = @At("HEAD"))
    private void friliensmagic$onSetTitle(Component title, CallbackInfo ci) {
        this.friliensmagic$cachedTitle = title;
    }

    @Inject(method = "setSubtitle", at = @At("HEAD"), require = 0)
    private void friliensmagic$onSetSubtitle(Component subtitle, CallbackInfo ci) {
        this.friliensmagic$cachedSubtitle = subtitle;
    }

    @Inject(method = "clear", at = @At("HEAD"), require = 0)
    private void friliensmagic$onClear(CallbackInfo ci) {
        this.friliensmagic$cachedTitle = null;
        this.friliensmagic$cachedSubtitle = null;
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void friliensmagic$computeAdaptiveScale(CallbackInfo ci) {
        int guiWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        this.friliensmagic$titleScale = friliensmagic$fit(this.friliensmagic$cachedTitle, guiWidth, 4.0F);
        // 副标题与主标题保持原版 1:2 的比例，同时自身也要放得下
        this.friliensmagic$subtitleScale = Math.min(
                this.friliensmagic$titleScale * 0.5F,
                friliensmagic$fit(this.friliensmagic$cachedSubtitle, guiWidth, 2.0F));
    }

    /** 替换主标题写死的 scale(4, 4, 4)。 */
    @ModifyArgs(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V",
                    ordinal = 0))
    private void friliensmagic$applyTitleScale(Args args) {
        friliensmagic$setUniformScale(args, this.friliensmagic$titleScale);
    }

    /** 替换副标题写死的 scale(2, 2, 2)；副标题可能为 null，故不强制存在。 */
    @ModifyArgs(method = "render",
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
    private static float friliensmagic$fit(Component text, int guiWidth, float baseScale) {
        if (text == null) {
            return baseScale;
        }
        int textWidth = Minecraft.getInstance().font.width(text);
        if (textWidth <= 0) {
            return baseScale;
        }
        return Math.min(baseScale, guiWidth * FRILIENSMAGIC_MAX_WIDTH_RATIO / textWidth);
    }
}
