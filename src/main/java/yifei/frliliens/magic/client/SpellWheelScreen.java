package yifei.frliliens.magic.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import yifei.frliliens.magic.FriliensMagic;
import yifei.frliliens.magic.attachment.LearnedSpellsData;
import yifei.frliliens.magic.attachment.ModAttachments;
import yifei.frliliens.magic.network.SelectSpellPayload;
import yifei.frliliens.magic.spell.Spell;
import yifei.frliliens.magic.spell.SpellRegistry;
import yifei.frliliens.magic.spell.StaffSpells;

/**
 * 法术选择轮盘。
 *
 * <p>每页最多 {@link #SPELLS_PER_PAGE} 个法术，超出部分通过滚轮翻页。
 * 鼠标移动按角度选中对应扇区，点击确认选择。
 */
public class SpellWheelScreen extends Screen {

    /** 每页最大法术数。 */
    private static final int SPELLS_PER_PAGE = 6;
    /** 轮盘内半径（基准）。 */
    private static final float BASE_RING_INNER = 24;
    /** 轮盘外半径（基准）。 */
    private static final float BASE_RING_OUTER = 90;
    /** 图标纹理尺寸（像素）。 */
    private static final int ICON_TEX_SIZE = 32;
    /** 图标渲染尺寸（基准）。 */
    private static final int BASE_ICON_SIZE = 36;
    /** 中心头像渲染尺寸（基准）。 */
    private static final int BASE_CENTER_SIZE = 36;

    /**
     * 法杖注册名 → 轮盘中心头像。
     *
     * <p>记录纹理路径与像素尺寸。<b>尺寸必须是 2 的幂</b>：Minecraft 对非 2 次幂的
     * 独立贴图生成 mipmap 会失败，表现为整张图渲染成空白。两张头像都已归一化成
     * 256×256（画师原图是 1045×1044 / 900×815），换图时请注意保持。
     *
     * <p>放在客户端而不是 {@link yifei.frliliens.magic.item.StaffItem} 里，
     * 是为了不让服务端代码引用客户端资源路径。
     */
    private record Portrait(String path, int texW, int texH) {
    }

    private static final Map<String, Portrait> PORTRAITS = Map.of(
            "frieren_staff", new Portrait("textures/gui/frieren_1.png", 256, 256),
            "phiren_staff", new Portrait("textures/gui/fern.png", 256, 256));

    /** 默认头像（未知法杖时）。 */
    private static final Portrait DEFAULT_PORTRAIT =
            new Portrait("textures/gui/frieren_1.png", 256, 256);

    /** 当前法杖的物品注册名。 */
    private final String staffId;
    /** 当前法杖对应的头像。 */
    private final Portrait portrait;

    /** 本把法杖支持、且玩家已学的战斗法术。 */
    private final List<Spell> learnedCombat = new ArrayList<>();
    /** 当前选中的扇区索引（页内）。 */
    private int selectedIndex = -1;
    /** 当前页码（从 0 开始）。 */
    private int currentPage = 0;

    /**
     * @param staffId 手持法杖的物品注册名，决定轮盘显示哪些法术与哪个头像
     */
    public SpellWheelScreen(String staffId) {
        super(Component.translatable("screen.friliensmagic.spell_wheel.title"));
        this.staffId = staffId;
        this.portrait = PORTRAITS.getOrDefault(staffId, DEFAULT_PORTRAIT);
    }

    @Override
    protected void init() {
        Player player = this.minecraft != null ? this.minecraft.player : null;
        if (player == null) {
            return;
        }
        LearnedSpellsData learned = player.getData(ModAttachments.LEARNED_SPELLS);
        // 只列出「本把法杖支持」且「已学会」的法术
        for (String id : StaffSpells.forStaff(staffId)) {
            if (learned.hasLearned(id)) {
                Spell spell = SpellRegistry.get(id);
                if (spell != null) {
                    learnedCombat.add(spell);
                }
            }
        }
    }

    /** 获取总页数。 */
    private int getTotalPages() {
        return Math.max(1, (int) Math.ceil((double) learnedCombat.size() / SPELLS_PER_PAGE));
    }

    /** 获取当前页的法术列表。 */
    private List<Spell> getPageSpells() {
        int from = currentPage * SPELLS_PER_PAGE;
        int to = Math.min(from + SPELLS_PER_PAGE, learnedCombat.size());
        return learnedCombat.subList(from, to);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 半透明背景
        guiGraphics.fill(0, 0, this.width, this.height, 0x66000000);

        int cx = this.width / 2;
        int cy = this.height / 2;

        if (learnedCombat.isEmpty()) {
            // 区分「这把法杖没有法术」和「有法术但还没学会」，否则提示会误导
            boolean staffHasSpells = !StaffSpells.forStaff(staffId).isEmpty();
            guiGraphics.drawCenteredString(this.font,
                    Component.translatable(staffHasSpells
                            ? "message.friliensmagic.no_spell_learned"
                            : "message.friliensmagic.no_spell_for_staff"),
                    cx, cy, 0xFFFFFF);
            return;
        }

        List<Spell> pageSpells = getPageSpells();
        int count = pageSpells.size();
        double radiansPerSpell = 2 * Math.PI / count;

        // 图标随法术数量动态缩放
        float iconScale = Math.max(0.65f, 1.0f - (count - 4) * 0.07f);
        int iconSize = (int) (BASE_ICON_SIZE * iconScale);

        // 根据鼠标角度计算选中扇区
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        selectedIndex = -1;
        if (dist > BASE_RING_INNER) {
            double angle = Math.atan2(dy, dx) + Math.PI / 2;
            if (angle < 0) {
                angle += 2 * Math.PI;
            }
            selectedIndex = (int) (angle / radiansPerSpell) % count;
        }

        // 绘制扇形背景
        drawRadialBackgrounds(guiGraphics, cx, cy, BASE_RING_INNER, BASE_RING_OUTER, pageSpells);
        // 绘制分割线
        drawDividingLines(guiGraphics, cx, cy, BASE_RING_INNER, BASE_RING_OUTER, count);

        // 中心头像（按法杖区分）
        ResourceLocation centerTex = ResourceLocation.fromNamespaceAndPath(
                FriliensMagic.MODID, portrait.path());
        drawScaledTexture(guiGraphics, centerTex, cx, cy, BASE_CENTER_SIZE,
                portrait.texW(), portrait.texH());

        // 绘制图标
        float iconRadius = (BASE_RING_INNER + BASE_RING_OUTER) / 2;
        for (int i = 0; i < count; i++) {
            double angle = -Math.PI / 2 + i * radiansPerSpell;
            int ix = cx + (int) (Math.cos(angle) * iconRadius);
            int iy = cy + (int) (Math.sin(angle) * iconRadius);

            boolean selected = (i == selectedIndex);
            int size = selected ? (int) (iconSize * 1.2) : iconSize;

            ResourceLocation iconTex = ResourceLocation.fromNamespaceAndPath(
                    FriliensMagic.MODID, "textures/gui/spell_" + pageSpells.get(i).getId() + ".png");

            drawScaledTexture(guiGraphics, iconTex, ix, iy, size, ICON_TEX_SIZE, ICON_TEX_SIZE);

            // 选中时绘制金色边框
            if (selected) {
                guiGraphics.fill(ix - size / 2 - 2, iy - size / 2 - 2,
                        ix + size / 2 + 2, iy - size / 2, 0xFFD4AF37);
                guiGraphics.fill(ix - size / 2 - 2, iy + size / 2,
                        ix + size / 2 + 2, iy + size / 2 + 2, 0xFFD4AF37);
                guiGraphics.fill(ix - size / 2 - 2, iy - size / 2,
                        ix - size / 2, iy + size / 2, 0xFFD4AF37);
                guiGraphics.fill(ix + size / 2, iy - size / 2,
                        ix + size / 2 + 2, iy + size / 2, 0xFFD4AF37);

                // 选中时显示法术名称
                guiGraphics.drawCenteredString(this.font,
                        pageSpells.get(i).getDisplayNameComponent(),
                        ix, iy + size / 2 + 6, 0xFFFFFF);
            }
        }

        // 多页时显示翻页提示和页码
        int totalPages = getTotalPages();
        if (totalPages > 1) {
            // 左右翻页箭头
            int arrowY = cy + (int) BASE_RING_OUTER + 20;
            String leftArrow = currentPage > 0 ? "<" : "  ";
            String rightArrow = currentPage < totalPages - 1 ? ">" : "  ";
            guiGraphics.drawCenteredString(this.font, Component.literal(leftArrow),
                    cx - 60, arrowY, 0xFFD4AF37);
            guiGraphics.drawCenteredString(this.font, Component.literal(rightArrow),
                    cx + 60, arrowY, 0xFFD4AF37);

            // 页码
            String pageText = (currentPage + 1) + " / " + totalPages;
            guiGraphics.drawCenteredString(this.font, Component.literal(pageText),
                    cx, arrowY, 0xFFD4AF37);

            // 滚轮提示
            guiGraphics.drawCenteredString(this.font,
                    Component.translatable("screen.friliensmagic.spell_wheel.scroll_hint"),
                    cx, arrowY + 14, 0x999999);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (getTotalPages() > 1) {
            if (scrollY > 0) {
                currentPage = Math.min(currentPage + 1, getTotalPages() - 1);
                return true;
            } else if (scrollY < 0) {
                currentPage = Math.max(currentPage - 1, 0);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 左右方向键翻页
        if (getTotalPages() > 1) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT) {
                currentPage = Math.max(currentPage - 1, 0);
                return true;
            } else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT) {
                currentPage = Math.min(currentPage + 1, getTotalPages() - 1);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * 以中心点绘制缩放纹理，用 poseStack.scale 缩放避免 UV 越界。
     *
     * <p>偏移量按缩放后的<b>实际</b>宽高算，而不是统一用 {@code drawSize}，
     * 否则非正方形贴图会偏离中心。
     */
    private void drawScaledTexture(GuiGraphics guiGraphics, ResourceLocation tex,
                                   int cx, int cy, int drawSize, int texW, int texH) {
        float scale = (float) drawSize / Math.max(texW, texH);
        float halfW = texW * scale / 2f;
        float halfH = texH * scale / 2f;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(cx - halfW, cy - halfH, 0);
        guiGraphics.pose().scale(scale, scale, 1);
        guiGraphics.blit(tex, 0, 0, 0, 0, texW, texH, texW, texH);
        guiGraphics.pose().popPose();
    }

    /** 绘制环形扇区背景。 */
    private void drawRadialBackgrounds(GuiGraphics guiGraphics, int cx, int cy,
                                       float ringInner, float ringOuter,
                                       List<Spell> pageSpells) {
        int count = pageSpells.size();
        int segments = count < 6 ? (count % 2 == 1 ? 15 : 12) : count * 2;
        float radiansPerSeg = 2 * Mth.PI / segments;
        float radiansPerSpell = 2 * Mth.PI / count;
        float quarter = Mth.HALF_PI;

        VertexConsumer vc = guiGraphics.bufferSource().getBuffer(RenderType.gui());
        Matrix4f mat = guiGraphics.pose().last().pose();

        for (int i = 0; i < segments; i++) {
            float begin = i * radiansPerSeg - (quarter + radiansPerSpell / 2);
            float end = (i + 1) * radiansPerSeg - (quarter + radiansPerSpell / 2);

            float x1i = Mth.cos(begin) * ringInner;
            float x2i = Mth.cos(end) * ringInner;
            float y1i = Mth.sin(begin) * ringInner;
            float y2i = Mth.sin(end) * ringInner;

            float x1o = Mth.cos(begin) * ringOuter;
            float x2o = Mth.cos(end) * ringOuter;
            float y1o = Mth.sin(begin) * ringOuter;
            float y2o = Mth.sin(end) * ringOuter;

            int spellIndex = (i * count) / segments;
            boolean highlighted = spellIndex == selectedIndex;

            float r = highlighted ? 0.85f : 0.08f;
            float g = highlighted ? 0.7f : 0.06f;
            float b = highlighted ? 0.4f : 0.12f;
            float a = highlighted ? 0.7f : 0.55f;

            vc.addVertex(mat, cx + x1i, cy + y1i, 0).setColor(r, g, b, a);
            vc.addVertex(mat, cx + x2i, cy + y2i, 0).setColor(r, g, b, a);
            vc.addVertex(mat, cx + x2o, cy + y2o, 0).setColor(r, g, b, 0f);
            vc.addVertex(mat, cx + x1o, cy + y1o, 0).setColor(r, g, b, 0f);
        }
    }

    /** 绘制扇区分割线。 */
    private void drawDividingLines(GuiGraphics guiGraphics, int cx, int cy,
                                   float ringInner, float ringOuter, int count) {
        if (count <= 1) {
            return;
        }
        float radiansPerSpell = 2 * Mth.PI / count;
        float quarter = Mth.HALF_PI;

        VertexConsumer vc = guiGraphics.bufferSource().getBuffer(RenderType.gui());
        Matrix4f mat = guiGraphics.pose().last().pose();

        for (int i = 0; i < count; i++) {
            float angle = i * radiansPerSpell - (quarter + radiansPerSpell / 2);

            float xi = Mth.cos(angle) * ringInner;
            float yi = Mth.sin(angle) * ringInner;
            float xo = Mth.cos(angle) * ringOuter * 1.1f;
            float yo = Mth.sin(angle) * ringOuter * 1.1f;

            float r = 0.83f, g = 0.69f, b = 0.44f;
            vc.addVertex(mat, cx + xi, cy + yi, 0).setColor(r, g, b, 1f);
            vc.addVertex(mat, cx + xi + 1, cy + yi + 1, 0).setColor(r, g, b, 1f);
            vc.addVertex(mat, cx + xo, cy + yo, 0).setColor(r, g, b, 0f);
            vc.addVertex(mat, cx + xo - 1, cy + yo - 1, 0).setColor(r, g, b, 0f);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int totalPages = getTotalPages();
            if (totalPages > 1) {
                int cx = this.width / 2;
                int cy = this.height / 2;
                int arrowY = cy + (int) BASE_RING_OUTER + 20;
                // 左箭头区域
                if (mouseX > cx - 75 && mouseX < cx - 45
                        && mouseY > arrowY - 6 && mouseY < arrowY + 6 && currentPage > 0) {
                    currentPage--;
                    return true;
                }
                // 右箭头区域
                if (mouseX > cx + 45 && mouseX < cx + 75
                        && mouseY > arrowY - 6 && mouseY < arrowY + 6
                        && currentPage < totalPages - 1) {
                    currentPage++;
                    return true;
                }
            }
            // 点击扇区选择法术
            if (selectedIndex >= 0) {
                List<Spell> pageSpells = getPageSpells();
                if (selectedIndex < pageSpells.size()) {
                    SelectSpellPayload.send(staffId, pageSpells.get(selectedIndex).getId());
                    this.onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
