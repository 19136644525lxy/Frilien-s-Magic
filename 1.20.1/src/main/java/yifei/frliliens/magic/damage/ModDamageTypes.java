package yifei.frliliens.magic.damage;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

import yifei.frliliens.magic.FriliensMagic;

/**
 * 模组自定义伤害类型。
 *
 * <p>提供「魔法伤害」类型，用于所有法术造成的伤害。
 * 通过 {@link #magic(Entity)} 获取带有施法者信息的 {@link DamageSource}。
 */
public final class ModDamageTypes {

    /** 魔法伤害类型的资源键。 */
    public static final ResourceKey<DamageType> MAGIC = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(FriliensMagic.MODID, "magic"));

    private ModDamageTypes() {
    }

    /**
     * 创建由指定实体施放的魔法伤害源。
     *
     * @param attacker 施法者
     * @return 魔法伤害源；若伤害类型未注册则回退到通用伤害
     */
    public static DamageSource magic(Entity attacker) {
        Holder<DamageType> holder = attacker.level().damageSources().generic().typeHolder();
        var registry = attacker.level().registryAccess().registry(Registries.DAMAGE_TYPE);
        if (registry.isPresent()) {
            var found = registry.get().getHolder(MAGIC);
            if (found.isPresent()) {
                holder = found.get();
            }
        }
        return new DamageSource(holder, attacker);
    }
}
