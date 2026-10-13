package yifei.frliliens.magic.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import yifei.frliliens.magic.FriliensMagic;

/**
 * 客户端初始化。
 *
 * <p><b>为什么不是 {@code @Mod(dist = Dist.CLIENT)}</b>：
 * 1.20.1 的 {@code @Mod} 注解只有 {@code value} 与 {@code bus} 两个成员，
 * <b>没有</b> {@code dist}（已用字节码核对）——那是后续版本才加的。
 * 所以这里做成普通类，由 {@link FriliensMagic} 用
 * {@code DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)} 在客户端才调用，
 * 保证专用服务端不会加载本类（否则会因引用客户端类而崩溃）。
 *
 * <p>mod 总线事件通过 {@link #register(IEventBus)} 注册；
 * 游戏总线事件（客户端 tick）见 {@link ClientTickHandler}。
 */
public final class FriliensMagicClient {

    /** 护盾自定义着色器实例，由 {@link RegisterShadersEvent} 回调赋值。 */
    public static ShaderInstance shieldShader;

    private FriliensMagicClient() {
    }

    /** 在客户端注册全部 mod 总线监听与游戏总线监听。 */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(FriliensMagicClient::onClientSetup);
        modEventBus.addListener(FriliensMagicClient::onRegisterShaders);
        modEventBus.addListener(FriliensMagicClient::onRegisterGuiOverlays);
        modEventBus.addListener(FriliensMagicClient::onRegisterKeys);
        modEventBus.addListener(FriliensMagicClient::onRegisterRenderers);

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(ClientTickHandler.class);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        FriliensMagic.LOGGER.info("Frilien's Magic client setup (1.20.1 Forge)");
    }

    /**
     * 注册自定义护盾着色器。
     *
     * <p>着色器资源位于 {@code assets/friliensmagic/shaders/core/shield.*}。
     * 使用 {@code POSITION_COLOR_NORMAL} 顶点格式以支持菲涅尔边缘光计算。
     */
    private static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            ResourceProvider provider = event.getResourceProvider();
            event.registerShader(
                    new ShaderInstance(provider, "friliensmagic:shield",
                            DefaultVertexFormat.POSITION_COLOR_NORMAL),
                    shader -> shieldShader = shader);
        } catch (Exception e) {
            FriliensMagic.LOGGER.error("Failed to register shield shader", e);
        }
    }

    /**
     * 注册 HUD 覆盖层（魔力条）。
     *
     * <p>1.20.1 用 {@code IGuiOverlay} 函数式接口，签名比 1.21 的 GuiLayer 多两个参数
     * （{@code ForgeGui} 与屏幕宽高），宽高正好省掉自己取值。
     */
    private static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        // 两点都与 1.21 不同：
        //   1. 覆盖层 id 是普通 String，不是 ResourceLocation
        //   2. IGuiOverlay 的方法比 GuiLayer 多两个参数（ForgeGui 与屏幕宽高），
        //      方法引用对不上，所以写成显式 lambda
        event.registerAboveAll("mana_bar",
                (gui, guiGraphics, partialTick, screenWidth, screenHeight) ->
                        ManaBarRenderer.render(gui, guiGraphics, partialTick,
                                screenWidth, screenHeight));
    }

    /** 注册按键映射（mod 总线事件）。 */
    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        // 魔力条
        event.register(ManaBarKeyBindings.EDIT_KEY);
        event.register(ManaBarKeyBindings.TOGGLE_KEY);
        // 基础能力与法术轮盘（长距离魔法为被动，无按键）
        event.register(ModKeyBindings.MANA_SENSE_KEY);
        event.register(ModKeyBindings.MANA_HIDE_KEY);
        event.register(ModKeyBindings.SPELL_WHEEL_KEY);
    }

    /**
     * 注册自定义实体的渲染器。
     *
     * <p><b>不注册的话委托村民会完全不可见</b>（实体在服务端存在、能交互，
     * 但客户端没有渲染器可画）。这里复用原版村民渲染器，
     * 因为 {@code QuestVillagerEntity} 只是换了职业。
     */
    private static void onRegisterRenderers(
            net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(yifei.frliliens.magic.entity.ModEntities.QUEST_VILLAGER.get(),
                net.minecraft.client.renderer.entity.VillagerRenderer::new);
    }

    /**
     * 客户端 tick：统一处理按键。
     *
     * <p>1.20.1 的客户端 tick 事件是嵌套类 {@code TickEvent.ClientTickEvent}，
     * 没有 {@code Post} 子类，靠 {@code phase} 区分，所以必须判 {@code END}
     * 否则一条 tick 里会被调用两次，按键响应翻倍。
     */
    public static final class ClientTickHandler {

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
            if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
                return;
            }
            ManaBarKeyBindings.handleInput();
            ModKeyBindings.handleInput();
        }

        private ClientTickHandler() {
        }
    }
}
