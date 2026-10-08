package yifei.frliliens.magic.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

/**
 * 刨冰食物。
 *
 * <p>食用恢复 4 点饥饿值，并给予 30 秒速度提升效果（夏日清凉）。
 */
public class ShavedIceItem extends Item {

    public ShavedIceItem(Properties properties) {
        super(properties.food(new FoodProperties.Builder()
                .nutrition(4)
                .saturationModifier(0.3F)
                .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0), 1.0F)
                .build()));
    }
}
