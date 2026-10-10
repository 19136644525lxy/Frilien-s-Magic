package yifei.frliliens.magic.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import yifei.frliliens.magic.FriliensMagic;

/**
 * 自定义实体注册表。
 */
public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, FriliensMagic.MODID);

    /** 委托村民（流浪的魔法使）。 */
    public static final DeferredHolder<EntityType<?>, EntityType<QuestVillagerEntity>> QUEST_VILLAGER =
            ENTITY_TYPES.register("quest_villager", () ->
                    EntityType.Builder.of(QuestVillagerEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.95f)
                            .clientTrackingRange(10)
                            .build("quest_villager"));

    private ModEntities() {
    }
}
