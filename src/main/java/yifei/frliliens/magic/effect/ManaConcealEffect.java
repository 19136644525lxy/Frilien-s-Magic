package yifei.frliliens.magic.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 魔力隐匿效果。
 *
 * <p>携带此效果的玩家对周围生物不可见，不会被索敌。
 * 效果本身不做 tick 处理，反索敌逻辑由事件系统完成。
 */
public class ManaConcealEffect extends MobEffect {

    public ManaConcealEffect() {
        // 有益效果，紫色（与魔力主题一致）
        super(MobEffectCategory.BENEFICIAL, 0xA88FD6);
    }
}
