package yifei.frliliens.magic.loot;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * 维度战利品注入。
 *
 * <p>按维度把对应等级的法术卷轴注入到该维度的箱子战利品中。
 * 三级分布：主世界（初级）/ 下界（中级）/ 末地（高级）。
 */
public class DimensionLootModifier extends LootModifier {

    public static final MapCodec<DimensionLootModifier> CODEC = RecordCodecBuilder.mapCodec(
            inst -> inst.group(
                    IGlobalLootModifier.LOOT_CONDITIONS_CODEC.fieldOf("conditions")
                            .forGetter(m -> m.conditions),
                    ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension")
                            .forGetter(m -> m.dimension),
                    com.mojang.serialization.Codec.INT.fieldOf("min_count")
                            .forGetter(m -> m.minCount),
                    com.mojang.serialization.Codec.INT.fieldOf("max_count")
                            .forGetter(m -> m.maxCount),
                    com.mojang.serialization.Codec.FLOAT.fieldOf("chance")
                            .forGetter(m -> m.chance),
                    ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items")
                            .forGetter(m -> m.items))
                    .apply(inst, DimensionLootModifier::new));

    /** 目标维度。 */
    private final ResourceKey<Level> dimension;
    /** 每次最少注入数量。 */
    private final int minCount;
    /** 每次最多注入数量。 */
    private final int maxCount;
    /** 注入概率（0.0 ~ 1.0）。 */
    private final float chance;
    /** 候选物品池，随机抽取。 */
    private final List<ItemStack> items;

    public DimensionLootModifier(LootItemCondition[] conditions,
                                 ResourceKey<Level> dimension,
                                 int minCount, int maxCount, float chance,
                                 List<ItemStack> items) {
        super(conditions);
        this.dimension = dimension;
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.chance = chance;
        this.items = items;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot,
                                                  LootContext context) {
        // 仅注入箱子战利品（有方块状态参数），跳过实体掉落等
        if (context.getParamOrNull(LootContextParams.BLOCK_STATE) == null) {
            return generatedLoot;
        }
        // 维度匹配
        var level = context.getLevel();
        if (level.dimension() != dimension) {
            return generatedLoot;
        }
        // 概率判定
        if (context.getRandom().nextFloat() > chance) {
            return generatedLoot;
        }

        // 随机数量
        int count = minCount + context.getRandom().nextInt(Math.max(1, maxCount - minCount + 1));
        for (int i = 0; i < count; i++) {
            ItemStack item = items.get(context.getRandom().nextInt(items.size()));
            generatedLoot.add(item.copy());
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
