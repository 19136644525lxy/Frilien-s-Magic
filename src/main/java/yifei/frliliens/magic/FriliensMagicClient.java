package yifei.frliliens.magic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import yifei.frliliens.magic.client.ModKeyBindings;
import yifei.frliliens.magic.client.ManaBarKeyBindings;
import yifei.frliliens.magic.client.ManaBarRenderer;
import yifei.frliliens.magic.entity.ModEntities;
import yifei.frliliens.magic.network.CastBasicAbilityPayload;

/**
 * 客户端专用入口，不会在专用服务器加载。
 *
 * <p>MOD 总线事件（{@link FMLClientSetupEvent}、{@link RegisterShadersEvent}、
 * {@link RegisterGuiLayersEvent}）在构造函数中通过 {@link IEventBus#addListener}
 * 手动注册，避免使用已弃用的 {@code @EventBusSubscriber(bus = MOD)}。
 */
@Mod(value = FriliensMagic.MODID, dist = Dist.CLIENT)
public class FriliensMagicClient {

    /** 护盾自定义着色器实例，由 {@link RegisterShadersEvent} 回调赋值。 */
    public static ShaderInstance shieldShader;

    public FriliensMagicClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onRegisterShaders);
        modEventBus.addListener(this::onRegisterGuiLayers);
        modEventBus.addListener(this::onRegisterKeys);
        modEventBus.addListener(this::onRegisterRenderers);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        FriliensMagic.LOGGER.info("HELLO FROM CLIENT SETUP");
        FriliensMagic.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    /**
     * 注册自定义护盾着色器。
     *
     * <p>着色器资源位于 {@code assets/friliensmagic/shaders/core/shield.*}。
     * 使用 {@code POSITION_COLOR_NORMAL} 顶点格式以支持菲涅尔边缘光计算。
     */
    private void onRegisterShaders(RegisterShadersEvent event) {
        try {
            ResourceProvider provider = event.getResourceProvider();
            event.registerShader(
                    new ShaderInstance(provider, "friliensmagic:shield", DefaultVertexFormat.POSITION_COLOR_NORMAL),
                    shader -> shieldShader = shader);
        } catch (Exception e) {
            FriliensMagic.LOGGER.error("Failed to register shield shader", e);
        }
    }

    /**
     * 注册 HUD 层（魔力条）。
     *
     * <p>{@code RegisterGuiLayersEvent} 为 mod 总线事件，在客户端初始化时触发一次，
     * 用于向游戏 HUD 注册自定义渲染层。
     */
    private void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(FriliensMagic.MODID, "mana_bar"),
                ManaBarRenderer::render);
    }

    /** 注册按键映射（MOD 总线事件）。 */
    private void onRegisterKeys(RegisterKeyMappingsEvent event) {
        // 魔力条
        event.register(ManaBarKeyBindings.EDIT_KEY);
        event.register(ManaBarKeyBindings.TOGGLE_KEY);
        // 基础能力与法术轮盘（长距离魔法为被动，无按键）
        event.register(ModKeyBindings.MANA_SENSE_KEY);
        event.register(ModKeyBindings.MANA_HIDE_KEY);
        event.register(ModKeyBindings.SPELL_WHEEL_KEY);
    }

    /** 注册自定义实体渲染器（MOD 总线事件）。 */
    private void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // 委托村民复用原版村民渲染器
        event.registerEntityRenderer(ModEntities.QUEST_VILLAGER.get(), VillagerRenderer::new);
    }
}
