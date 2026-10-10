package yifei.frliliens.magic;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.minecraft.world.item.CreativeModeTabs;

import yifei.frliliens.magic.attachment.ModAttachments;
import yifei.frliliens.magic.config.Config;
import yifei.frliliens.magic.effect.ModEffects;
import yifei.frliliens.magic.handler.FlightHandler;
import yifei.frliliens.magic.handler.ManaConcealHandler;
import yifei.frliliens.magic.handler.ManaRegenHandler;
import yifei.frliliens.magic.handler.QuestProgressHandler;
import yifei.frliliens.magic.handler.QuestVillagerHandler;
import yifei.frliliens.magic.quest.QuestCommand;
import yifei.frliliens.magic.item.ModCreativeTabs;
import yifei.frliliens.magic.item.ModItems;
import yifei.frliliens.magic.loot.ModLootModifiers;
import yifei.frliliens.magic.network.CastBasicAbilityPayload;
import yifei.frliliens.magic.network.SelectSpellPayload;

/**
 * Mod entry point.
 */
@Mod(FriliensMagic.MODID)
public class FriliensMagic {

    /** Must match the {@code modId} in {@code META-INF/neoforge.mods.toml}. */
    public static final String MODID = "friliensmagic";

    public static final Logger LOGGER = LogUtils.getLogger();

    public FriliensMagic(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onRegisterPayloads);

        // hand each DeferredRegister to the mod event bus so its contents get registered
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModLootModifiers.GLM.register(modEventBus);
        ModEffects.EFFECTS.register(modEventBus);

        modEventBus.addListener(this::addCreative);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new ManaRegenHandler());
        NeoForge.EVENT_BUS.register(new FlightHandler());
        NeoForge.EVENT_BUS.register(new ManaConcealHandler());
        NeoForge.EVENT_BUS.register(new QuestVillagerHandler());
        NeoForge.EVENT_BUS.register(new QuestProgressHandler());
        NeoForge.EVENT_BUS.register(new QuestCommand());

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Frilien's Magic common setup");
        if (Config.LOG_STAVES.getAsBoolean()) {
            LOGGER.info("staff items: {}, {}",
                    ModItems.FRIEREN_STAFF.getId(), ModItems.PHIREN_STAFF.getId());
        }
    }

    /** 注册网络包 payload handler。 */
    private void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(MODID)
                .playToServer(CastBasicAbilityPayload.TYPE,
                        CastBasicAbilityPayload.STREAM_CODEC,
                        CastBasicAbilityPayload::handle)
                .playToServer(SelectSpellPayload.TYPE,
                        SelectSpellPayload.STREAM_CODEC,
                        SelectSpellPayload::handle);
    }

    /** The staves are held items, so mirror them into the vanilla combat tab. */
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.FRIEREN_STAFF);
            event.accept(ModItems.PHIREN_STAFF);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Frilien's Magic loaded on the server");
    }
}
