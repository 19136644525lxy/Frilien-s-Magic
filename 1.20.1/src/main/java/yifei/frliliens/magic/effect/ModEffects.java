package yifei.frliliens.magic.effect;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import yifei.frliliens.magic.FriliensMagic;

/**
 * 自定义效果注册。
 *
 * <p>与 1.21 版的差别只有类型名：{@code DeferredHolder} → {@link RegistryObject}，
 * 以及 {@code DeferredRegister.create(Registries.MOB_EFFECT, ...)}
 * （1.20.1 用原版注册表键，不是 NeoForge 的注册表对象）。
 */
public final class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, FriliensMagic.MODID);

    /** 魔力隐匿：让玩家不被生物索敌。 */
    public static final RegistryObject<ManaConcealEffect> MANA_CONCEAL =
            EFFECTS.register("mana_conceal", ManaConcealEffect::new);

    private ModEffects() {
    }
}
