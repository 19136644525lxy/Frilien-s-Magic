package yifei.frliliens.magic.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import yifei.frliliens.magic.FriliensMagic;
import yifei.frliliens.magic.FriliensMagicClient;
import yifei.frliliens.magic.attachment.ModAttachments;
import yifei.frliliens.magic.attachment.ShieldData;
import yifei.frliliens.magic.config.Config;

/**
 * 护盾客户端渲染器。
 *
 * <p>双层渲染：
 * <ol>
 *   <li>填充球 — 自定义 shader（菲涅尔边缘光 + 能量噪声），半透明能量底色</li>
 *   <li>六边形图案 — 测地线球的对偶多面体（六边形 + 12 个五边形），
 *       每个多边形内缩留出间隙，配合加法混合与能量流动动画</li>
 * </ol>
 * 颜色随护盾剩余强度从蓝渐变到红。
 */
@EventBusSubscriber(modid = FriliensMagic.MODID, value = Dist.CLIENT)
public final class ShieldRenderer {

    /** 填充球经纬度细分数。 */
    private static final int LAT_BANDS = 20;
    private static final int LON_BANDS = 20;
    /** 测地线球细分级别（0~3，越大六边形越多越小）。 */
    private static final int GEO_SUBDIVISIONS = 2;
    /** 六边形内缩比例（0~1），留出间隙形成网格感。 */
    private static final float HEX_INSET = 0.82F;

    private ShieldRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        ShaderInstance shader = FriliensMagicClient.shieldShader;
        if (shader == null) {
            return;
        }

        Vec3 cameraPos = event.getCamera().getPosition();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float gameTime = (mc.level.getGameTime() + partialTick) / 20.0F;

        mc.level.entitiesForRendering().forEach(entity -> {
            ShieldData shield = entity.getData(ModAttachments.SHIELD);
            if (shield.isActive()) {
                renderShield(event.getPoseStack(), entity, shield, cameraPos, partialTick, gameTime);
            }
        });
    }

    private static void renderShield(PoseStack poseStack, Entity entity, ShieldData shield,
                                     Vec3 cameraPos, float partialTick, float gameTime) {
        Minecraft mc = Minecraft.getInstance();
        // 第一人称本地玩家：降低透明度 + 只渲染背面半球，避免遮挡视线
        boolean firstPerson = (mc.player == entity) && mc.options.getCameraType().isFirstPerson();
        // 用实体视线方向作为摄像机视线方向（第一人称本地玩家时一致）
        Vec3 look = entity.getLookAngle();
        Vector3f lookDir = new Vector3f((float) look.x, (float) look.y, (float) look.z);

        double radius = Config.SHIELD_RADIUS.getAsDouble();

        Vec3 center = new Vec3(
                Mth.lerp(partialTick, entity.xOld, entity.getX()),
                Mth.lerp(partialTick, entity.yOld, entity.getY()) + entity.getBbHeight() * 0.5,
                Mth.lerp(partialTick, entity.zOld, entity.getZ()));

        double relX = center.x - cameraPos.x;
        double relY = center.y - cameraPos.y;
        double relZ = center.z - cameraPos.z;

        // 球心相对摄像机的偏移（用于第一人称背面半球判断）
        Vector3f relToCam = new Vector3f((float) relX, (float) relY, (float) relZ);

        // 颜色随强度：满→蓝，空→红
        float percent = shield.strengthPercent();
        float r = Mth.lerp(1.0F - percent, 0.15F, 0.85F);
        float g = Mth.lerp(1.0F - percent, 0.45F, 0.25F);
        float b = Mth.lerp(1.0F - percent, 0.95F, 0.35F);

        // 方案 A：第一人称大幅降低透明度
        float sphereAlpha = firstPerson ? 0.15F : 0.55F;

        poseStack.pushPose();
        poseStack.translate(relX, relY, relZ);

        // 第一层：半透明能量球（底色）
        drawShieldSphere(poseStack, radius, r, g, b, sphereAlpha, gameTime,
                firstPerson, lookDir, relToCam);
        // 第二层：六边形图案
        drawHexagons(poseStack, radius, r, g, b, gameTime,
                firstPerson, lookDir, relToCam);

        poseStack.popPose();
    }

    // ===== 第一层：填充能量球 =====

    private static void drawShieldSphere(PoseStack poseStack, double radius,
                                         float r, float g, float b, float alpha, float gameTime,
                                         boolean firstPerson, Vector3f lookDir, Vector3f relToCam) {
        ShaderInstance shader = FriliensMagicClient.shieldShader;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(() -> shader);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515); // GL_LESS
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        if (shader.GAME_TIME != null) {
            shader.GAME_TIME.set(gameTime);
        }

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        Matrix4f matrix = poseStack.last().pose();

        for (int lat = 0; lat < LAT_BANDS; lat++) {
            double theta1 = lat * Math.PI / LAT_BANDS;
            double theta2 = (lat + 1) * Math.PI / LAT_BANDS;
            for (int lon = 0; lon < LON_BANDS; lon++) {
                double phi1 = lon * 2.0 * Math.PI / LON_BANDS;
                double phi2 = (lon + 1) * 2.0 * Math.PI / LON_BANDS;

                Vector3f v1 = sphereVertex(radius, theta1, phi1);
                Vector3f v2 = sphereVertex(radius, theta1, phi2);
                Vector3f v3 = sphereVertex(radius, theta2, phi2);
                Vector3f v4 = sphereVertex(radius, theta2, phi1);

                // 方案 B：第一人称只渲染摄像机后方的半球，前方跳过避免挡视线
                if (firstPerson) {
                    Vector3f quadCenter = new Vector3f(
                            (v1.x + v2.x + v3.x + v4.x) * 0.25F,
                            (v1.y + v2.y + v3.y + v4.y) * 0.25F,
                            (v1.z + v2.z + v3.z + v4.z) * 0.25F);
                    // quadCenter 是球心空间，加上球心相对摄像机的偏移得到相对摄像机的方向
                    float dot = (quadCenter.x + relToCam.x) * lookDir.x
                            + (quadCenter.y + relToCam.y) * lookDir.y
                            + (quadCenter.z + relToCam.z) * lookDir.z;
                    if (dot > 0.0F) {
                        continue; // 在前半球，跳过
                    }
                }

                Vector3f n1 = v1.normalize(new Vector3f());
                Vector3f n2 = v2.normalize(new Vector3f());
                Vector3f n3 = v3.normalize(new Vector3f());
                Vector3f n4 = v4.normalize(new Vector3f());

                putVertex(buffer, matrix, v1, n1, r, g, b, alpha);
                putVertex(buffer, matrix, v3, n3, r, g, b, alpha);
                putVertex(buffer, matrix, v2, n2, r, g, b, alpha);

                putVertex(buffer, matrix, v1, n1, r, g, b, alpha);
                putVertex(buffer, matrix, v4, n4, r, g, b, alpha);
                putVertex(buffer, matrix, v3, n3, r, g, b, alpha);
            }
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static void putVertex(BufferBuilder buffer, Matrix4f matrix, Vector3f pos,
                                  Vector3f normal, float r, float g, float b, float a) {
        buffer.addVertex(matrix, pos.x, pos.y, pos.z)
                .setColor(r, g, b, a)
                .setNormal(normal.x, normal.y, normal.z);
    }

    // ===== 第二层：六边形图案（测地线球对偶面） =====

    /**
     * 渲染测地线球对偶多面体的面（六边形 + 五边形）。
     *
     * <p>每个多边形向中心内缩留出间隙，形成蜂巢网格外观；
     * 配合普通 alpha 混合与能量流动动画，呈现六边形能量场。
     */
    private static void drawHexagons(PoseStack poseStack, double radius,
                                     float r, float g, float b, float gameTime,
                                     boolean firstPerson, Vector3f lookDir, Vector3f relToCam) {
        GeodesicSphere sphere = new GeodesicSphere((float) radius, GEO_SUBDIVISIONS);
        List<List<Vector3f>> polygons = sphere.getDualPolygons();

        RenderSystem.enableBlend();
        // 普通 alpha 混合，避免与填充球叠加过曝
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();

        for (List<Vector3f> poly : polygons) {
            if (poly.size() < 3) {
                continue;
            }
            // 多边形中心
            Vector3f center = average(poly);

            // 方案 B：第一人称只渲染摄像机后方的多边形
            if (firstPerson) {
                float dot = (center.x + relToCam.x) * lookDir.x
                        + (center.y + relToCam.y) * lookDir.y
                        + (center.z + relToCam.z) * lookDir.z;
                if (dot > 0.0F) {
                    continue;
                }
            }

            // 能量流动：基于多边形中心位置 + 时间
            float flow = (Mth.sin(gameTime * 2.0F + center.y * 1.2F + center.x * 0.7F) + 1.0F) * 0.5F;
            float brightness = 0.5F + flow * 0.3F;
            // 方案 A：第一人称六边形也降低透明度
            float polyAlpha = firstPerson ? 0.10F + flow * 0.10F : 0.20F + flow * 0.20F;

            float lr = Mth.clamp(r * brightness, 0.0F, 1.0F);
            float lg = Mth.clamp(g * brightness, 0.0F, 1.0F);
            float lb = Mth.clamp(b * brightness, 0.0F, 1.0F);

            // 内缩顶点
            Vector3f[] inset = insetPolygon(poly, center, HEX_INSET);

            // 以中心为顶点做扇形三角化
            for (int i = 0; i < inset.length; i++) {
                Vector3f p0 = inset[i];
                Vector3f p1 = inset[(i + 1) % inset.length];

                buffer.addVertex(matrix, center.x, center.y, center.z).setColor(lr, lg, lb, polyAlpha);
                buffer.addVertex(matrix, p0.x, p0.y, p0.z).setColor(lr, lg, lb, polyAlpha);
                buffer.addVertex(matrix, p1.x, p1.y, p1.z).setColor(lr, lg, lb, polyAlpha);
            }
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /** 计算一组点的平均值（中心）。 */
    private static Vector3f average(List<Vector3f> points) {
        float sx = 0, sy = 0, sz = 0;
        for (Vector3f p : points) {
            sx += p.x;
            sy += p.y;
            sz += p.z;
        }
        float n = points.size();
        return new Vector3f(sx / n, sy / n, sz / n);
    }

    /**
     * 将多边形各顶点向中心内缩，返回新的顶点数组。
     */
    private static Vector3f[] insetPolygon(List<Vector3f> poly, Vector3f center, float factor) {
        Vector3f[] result = new Vector3f[poly.size()];
        for (int i = 0; i < poly.size(); i++) {
            Vector3f p = poly.get(i);
            // p' = center + (p - center) * factor
            result[i] = new Vector3f(
                    center.x + (p.x - center.x) * factor,
                    center.y + (p.y - center.y) * factor,
                    center.z + (p.z - center.z) * factor);
        }
        return result;
    }

    private static Vector3f sphereVertex(double radius, double theta, double phi) {
        float x = (float) (radius * Math.sin(theta) * Math.cos(phi));
        float y = (float) (radius * Math.cos(theta));
        float z = (float) (radius * Math.sin(theta) * Math.sin(phi));
        return new Vector3f(x, y, z);
    }
}
