package yifei.frliliens.magic.loot;

import com.mojang.serialization.MapCodec;

import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import yifei.frliliens.magic.FriliensMagic;

import java.util.function.Supplier;

/**
 * 全局战利品修改器注册。
 */
public final class ModLootModifiers {

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLM =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    FriliensMagic.MODID);

    /** 按维度注入卷轴的战利品修改器。 */
    public static final Supplier<MapCodec<DimensionLootModifier>> DIMENSION =
            GLM.register("dimension", () -> DimensionLootModifier.CODEC);

    private ModLootModifiers() {
    }
}
