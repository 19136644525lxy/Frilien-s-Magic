package yifei.frliliens.magic.loot;

import com.mojang.serialization.Codec;

import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import yifei.frliliens.magic.FriliensMagic;

/**
 * 全局战利品修改器注册。
 *
 * <p>与 1.21 版的差异：注册表从
 * {@code NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS} 换成
 * {@link ForgeRegistries#GLOBAL_LOOT_MODIFIER_SERIALIZERS}，
 * 注册的值类型是 {@link Codec}（1.21 那边叫「序列化器」但同样是 Codec）。
 */
public final class ModLootModifiers {

    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> GLM =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    FriliensMagic.MODID);

    /** 按维度注入卷轴的战利品修改器。 */
    public static final RegistryObject<Codec<DimensionLootModifier>> DIMENSION =
            GLM.register("dimension", () -> DimensionLootModifier.CODEC);

    private ModLootModifiers() {
    }
}
