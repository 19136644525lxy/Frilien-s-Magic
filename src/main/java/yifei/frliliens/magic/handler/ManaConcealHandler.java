package yifei.frliliens.magic.handler;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import yifei.frliliens.magic.effect.ModEffects;

/**
 * 魔力隐匿反索敌处理。
 *
 * <p>携带 {@link ModEffects#MANA_CONCEAL} 效果的玩家不会被生物索敌：
 * <ul>
 *   <li>将生物对该玩家的可见度降为 0，使其无法被侦测（降低仇恨范围）</li>
 *   <li>阻止生物切换目标到该玩家</li>
 *   <li>清除已锁定该玩家的生物目标</li>
 * </ul>
 */
public class ManaConcealHandler {

    /** 将隐匿玩家对生物的可见度降为 0，使其无法被侦测。 */
    @SubscribeEvent
    public void onLivingVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (event.getEntity() instanceof Player player && hasConceal(player)) {
            event.modifyVisibility(0.0);
        }
    }

    /** 阻止生物把目标切换为隐匿中的玩家。 */
    @SubscribeEvent
    public void onLivingChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity newTarget = event.getNewAboutToBeSetTarget();
        if (newTarget instanceof Player player && hasConceal(player)) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    /** 清除已锁定隐匿玩家的生物目标。 */
    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        LivingEntity target = mob.getTarget();
        if (target instanceof Player player && hasConceal(player)) {
            mob.setTarget(null);
        }
    }

    private static boolean hasConceal(Player player) {
        return player.hasEffect(ModEffects.MANA_CONCEAL);
    }
}
