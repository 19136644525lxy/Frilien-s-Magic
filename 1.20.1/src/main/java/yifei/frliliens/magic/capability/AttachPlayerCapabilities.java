package yifei.frliliens.magic.capability;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import yifei.frliliens.magic.FriliensMagic;

/**
 * 把玩家数据 capability 挂到玩家实体上。
 *
 * <p>Forge 1.20.1 没有「注册即自动挂载」的机制，必须监听
 * {@link AttachCapabilitiesEvent} 手动添加，并在玩家重生时拷贝。
 *
 * <p><b>三个必须处理的时机</b>：
 * <ol>
 *   <li>{@link #onAttachCapabilities} —— 实体创建时挂载</li>
 *   <li>{@link #onPlayerClone} —— 死亡重生 / 跨维度时把旧数据搬到新实体，
 *       否则会清空魔力与已学法术</li>
 *   <li>同时 {@code invalidateCaps()} 掉旧实体的句柄，
 *       否则 {@link net.minecraftforge.common.util.LazyOptional} 泄漏</li>
 * </ol>
 *
 * <p><b>哪些数据跨死亡保留</b>：由 {@link PlayerDataKinds.Kind#keepOnDeath()} 决定——
 * 魔力、护盾、已学法术、选中法术保留；飞行状态不保留。
 * 跨维度传送（非死亡）则全部保留。
 */
public final class AttachPlayerCapabilities {

    private static final String NS = FriliensMagic.MODID;

    // ===== 事件 =====

    /** 实体创建时挂载全部数据 capability。 */
    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();
        if (entity instanceof Player) {
            for (PlayerDataKinds.Kind<?> kind : PlayerDataKinds.KINDS) {
                event.addCapability(id(kind.id()), kind.cap().factory().get());
            }
        } else if (entity instanceof net.minecraft.world.entity.npc.Villager) {
            // 委托村民的标记与冷却挂在村民身上
            for (PlayerDataKinds.Kind<?> kind : PlayerDataKinds.VILLAGER_KINDS) {
                event.addCapability(id(kind.id()), kind.cap().factory().get());
            }
        }
    }

    /** 玩家死亡重生 / 跨维度时拷贝数据。 */
    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        Player oldPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();
        boolean died = event.isWasDeath();

        // 先按标识取旧实体上的 provider，避免依赖两个列表的下标对齐
        Map<String, PlayerDataCapability<?, ?>> old = collect(oldPlayer);

        // 旧实体的句柄必须失效，否则一直持有引用
        oldPlayer.invalidateCaps();

        int copied = 0;
        for (PlayerDataKinds.Kind<?> kind : PlayerDataKinds.KINDS) {
            if (died && !kind.keepOnDeath()) {
                continue;
            }
            PlayerDataCapability<?, ?> from = old.get(kind.id());
            if (from == null) {
                FriliensMagic.LOGGER.warn("player data '{}' not found on old entity", kind.id());
                continue;
            }
            PlayerDataCapability<?, ?> to = providerOf(newPlayer, kind);
            if (to == null) {
                FriliensMagic.LOGGER.warn("player data '{}' missing on new entity", kind.id());
                continue;
            }
            // 走一趟序列化往返，复用各自的校验逻辑（例如魔力上限钳制），
            // 也不让新旧实体共享任何可变状态
            copyUntyped(from, to);
            copied++;
        }

        FriliensMagic.LOGGER.debug("player respawn (death={}): copied {} data capabilities",
                died, copied);
    }

    // ===== 内部 =====

    /** 收集某实体身上全部本模组的 provider，键为标识。 */
    private static Map<String, PlayerDataCapability<?, ?>> collect(Player player) {
        Map<String, PlayerDataCapability<?, ?>> out = new LinkedHashMap<>();
        for (PlayerDataKinds.Kind<?> kind : PlayerDataKinds.KINDS) {
            PlayerDataCapability<?, ?> provider = providerOf(player, kind);
            if (provider != null) {
                out.put(kind.id(), provider);
            }
        }
        return out;
    }

    private static <I extends PlayerDataCapability<?, I>> PlayerDataCapability<?, ?> providerOf(
            Entity entity, PlayerDataKinds.Kind<I> kind) {
        return entity.getCapability(kind.cap().capability()).orElse(null);
    }

    /** 泛型通配下的序列化往返：类型由各自 provider 自己保证。 */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void copyUntyped(PlayerDataCapability from, PlayerDataCapability to) {
        to.deserializeNBT(from.serializeNBT());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(NS, path);
    }

    /** 由主类实例化后注册到游戏事件总线。 */
    public AttachPlayerCapabilities() {
    }
}
