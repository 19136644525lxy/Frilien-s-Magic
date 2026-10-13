package yifei.frliliens.magic;

import com.mojang.logging.LogUtils;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;

import org.slf4j.Logger;

import yifei.frliliens.magic.capability.AttachPlayerCapabilities;
import yifei.frliliens.magic.capability.ModCapabilities;
import yifei.frliliens.magic.capability.PlayerDataKinds;
import yifei.frliliens.magic.config.Config;
import yifei.frliliens.magic.effect.ModEffects;
import yifei.frliliens.magic.entity.ModEntities;
import yifei.frliliens.magic.handler.FlightHandler;
import yifei.frliliens.magic.handler.ManaConcealHandler;
import yifei.frliliens.magic.handler.ManaRegenHandler;
import yifei.frliliens.magic.handler.PlayerDataSyncHandler;
import yifei.frliliens.magic.handler.QuestAnnounceHandler;
import yifei.frliliens.magic.handler.QuestProgressHandler;
import yifei.frliliens.magic.handler.QuestVillagerHandler;
import yifei.frliliens.magic.handler.WelcomeHandler;
import yifei.frliliens.magic.item.ModCreativeTabs;
import yifei.frliliens.magic.item.ModItems;
import yifei.frliliens.magic.loot.ModLootModifiers;
import yifei.frliliens.magic.network.ModNetwork;
import yifei.frliliens.magic.quest.QuestCommand;
import yifei.frliliens.magic.quest.QuestIntegrityCheck;
import yifei.frliliens.magic.quest.QuestPoolReloadListener;
import yifei.frliliens.magic.spell.defense.ShieldEventHandler;
import yifei.frliliens.magic.world.WelcomeCommand;

/**
 * 主类（1.20.1 Forge）。
 *
 * <p>这是移植的骨架阶段：注册物品与创造模式物品栏，
 * 并用 {@link FMLCommonSetupEvent} 打印自检信息，确认环境可用。
 * 法术、委托、魔力、护盾等模块后续分阶段移植。
 *
 * <p>与 1.21 NeoForge 版的写法差异：
 * <ul>
 *   <li>构造参数是 {@link FMLJavaModLoadingContext}，用 {@code context.getModEventBus()} 拿总线</li>
 *   <li>游戏事件总线是 {@code MinecraftForge.EVENT_BUS}（NeoForge 版是 {@code NeoForge.EVENT_BUS}）</li>
 *   <li>配置用 {@code context.registerConfig(...)} 注册</li>
 * </ul>
 */
@Mod(FriliensMagic.MODID)
public class FriliensMagic {
    /** 模组 id，全项目唯一引用点。 */
    public static final String MODID = "friliensmagic";

    /** 日志。 */
    public static final Logger LOGGER = LogUtils.getLogger();

    public FriliensMagic(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // 注册各类 DeferredRegister，顺序无所谓
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        ModEffects.EFFECTS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModLootModifiers.GLM.register(modEventBus);

        // 本类同时监听 mod 总线与游戏总线
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onRegisterAttributes);
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(ModCapabilities::register);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new AttachPlayerCapabilities());
        MinecraftForge.EVENT_BUS.register(new ManaRegenHandler());
        MinecraftForge.EVENT_BUS.register(new FlightHandler());
        MinecraftForge.EVENT_BUS.register(new ManaConcealHandler());
        MinecraftForge.EVENT_BUS.register(new QuestVillagerHandler());
        MinecraftForge.EVENT_BUS.register(new QuestProgressHandler());
        MinecraftForge.EVENT_BUS.register(new QuestAnnounceHandler());
        MinecraftForge.EVENT_BUS.register(new WelcomeHandler());
        MinecraftForge.EVENT_BUS.register(new PlayerDataSyncHandler());
        MinecraftForge.EVENT_BUS.register(new QuestCommand());
        MinecraftForge.EVENT_BUS.register(new WelcomeCommand());

        // 护盾的伤害拦截与弹射物偏转（处理器内部是静态订阅方法）
        MinecraftForge.EVENT_BUS.register(ShieldEventHandler.class);

        // 客户端初始化。必须用 DistExecutor 隔开：直接引用客户端类
        // 会让专用服务端在加载本类时就去解析那些类，从而崩在 NoClassDefFoundError。
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> yifei.frliliens.magic.client.FriliensMagicClient.register(modEventBus));

        // 注册配置。1.20.1 通过 FMLJavaModLoadingContext 注册（1.21 是 modContainer）
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    /**
     * 注册数据包重载监听器：委托池在每次数据包重载（启动、{@code /reload}）时重建。
     *
     * <p>这样数据包作者改完 JSON 用 {@code /reload} 就能生效，不必重启游戏。
     * 1.20.1 的 {@code AddReloadListenerEvent#addListener} 接受
     * {@code PreparableReloadListener}，与 1.21 一致。
     */
    @SubscribeEvent
    public void onAddReloadListener(net.minecraftforge.event.AddReloadListenerEvent event) {
        event.addListener(new QuestPoolReloadListener());
    }

    /** 公共初始化：注册网络、打印自检信息。 */
    private void commonSetup(final FMLCommonSetupEvent event) {
        // 网络频道必须在 setup 阶段注册，太晚会漏掉早期包
        ModNetwork.register();

        LOGGER.info("Frilien's Magic common setup (1.20.1 Forge)");

        // 注意：配置值必须在这里之后才读。ConfigValue 在配置加载前 get() 会抛异常，
        // 所以不能在静态初始化阶段缓存配置值。
        if (Config.LOG_STAVES.get()) {
            LOGGER.info("staff items: {}, {}",
                    ForgeRegistries.ITEMS.getKey(ModItems.FRIEREN_STAFF.get()),
                    ForgeRegistries.ITEMS.getKey(ModItems.PHIREN_STAFF.get()));
        }

        LOGGER.info("registered {} items, {} player data kinds, {} config entries",
                ModItems.ITEMS.getEntries().size(),
                PlayerDataKinds.KINDS.size() + PlayerDataKinds.VILLAGER_KINDS.size(),
                Config.SPEC.getValues().size());
        LOGGER.info("creative tabs: {}", ModCreativeTabs.TABS.getEntries().size());

        // 自检：两张数据表都要能被标识索引到（曾经漏掉村民表导致委托系统静默失效）
        PlayerDataKinds.verifyIndex();
        LOGGER.info("player data kind index verified ({} player + {} villager)",
                PlayerDataKinds.KINDS.size(), PlayerDataKinds.VILLAGER_KINDS.size());
    }

    /**
     * 把法杖也放进原版「战斗」物品栏。
     *
     * <p>本模组有自己的物品栏（{@code ModCreativeTabs}），
     * 但把法杖同时放进战斗栏更符合玩家习惯——找武器时会先看那一栏。
     */
    private void addCreative(net.minecraftforge.event.BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == net.minecraft.world.item.CreativeModeTabs.COMBAT) {
            event.accept(ModItems.FRIEREN_STAFF);
            event.accept(ModItems.PHIREN_STAFF);
        }
    }

    /**
     * 注册自定义实体的属性。
     *
     * <p><b>这一项不注册会让游戏崩</b>：Mob 子类必须在加载期注册属性，
     * 否则生成实体时抛「unsupported attribute」。
     * 沿用原版村民的属性即可——{@code QuestVillagerEntity} 只是换了职业。
     */
    private void onRegisterAttributes(
            net.minecraftforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(ModEntities.QUEST_VILLAGER.get(),
                net.minecraft.world.entity.npc.Villager.createAttributes().build());
    }

    /** 服务端开始启动。 */
    @SubscribeEvent
    public void onServerStarting(
            net.minecraftforge.event.server.ServerStartingEvent event) {
        LOGGER.info("Frilien's Magic loaded on the server");
    }

    /**
     * 服务端完全就绪后跑一次数据自检，核对代码里手写的资源 id 是否真实存在。
     *
     * <p>必须在 {@code ServerStartedEvent}（而不是 setup 阶段）——
     * 那时服务端实例已就绪，才拿得到注册表与数据包内容。
     */
    @SubscribeEvent
    public void onServerStarted(net.minecraftforge.event.server.ServerStartedEvent event) {
        QuestIntegrityCheck.run(event.getServer());
    }
}
