package yifei.frliliens.magic.spell.combat;

import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import yifei.frliliens.magic.spell.AbstractSpell;

/**
 * 漂浮魔法。
 *
 * <p>生活魔法：让周围 5 格内的实体和掉落物漂浮起来（悬浮效果）。
 * 可用于拾取高空掉落物或短暂牵制敌人。
 */
public class FloatSpell extends AbstractSpell {

    /** 作用半径。 */
    private static final double RADIUS = 5.0;
    /** 悬浮持续时间（tick）。 */
    private static final int DURATION = 60;

    public FloatSpell() {
        super("float", 40);
    }

    @Override
    protected boolean doCast(Level level, Player caster) {
        AABB area = caster.getBoundingBox().inflate(RADIUS);
        List<Entity> entities = level.getEntities(caster, area);

        int affected = 0;
        for (Entity entity : entities) {
            // 掉落物直接向上施加速度
            if (!entity.isAlive()) {
                continue;
            }
            if (entity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.LEVITATION, DURATION, 0,
                        false, true, true));
            } else {
                // 非生物实体（物品、经验球等）给一个向上的速度
                entity.setDeltaMovement(entity.getDeltaMovement().add(0, 0.4, 0));
                entity.hurtMarked = true;
            }
            affected++;
        }

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.5F, 1.8F);
        }
        return affected > 0 || true; // 空放也算成功（消耗魔力）
    }
}
