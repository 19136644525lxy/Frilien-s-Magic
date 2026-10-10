package yifei.frliliens.magic.effect;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import yifei.frliliens.magic.FriliensMagic;

/**
 * 自定义效果注册。
 */
public final class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, FriliensMagic.MODID);

    /** 魔力隐匿：让玩家不被生物索敌。 */
    public static final DeferredHolder<MobEffect, ManaConcealEffect> MANA_CONCEAL =
            EFFECTS.register("mana_conceal", ManaConcealEffect::new);

    private ModEffects() {
    }
}
