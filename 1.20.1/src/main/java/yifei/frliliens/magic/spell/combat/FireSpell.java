package yifei.frliliens.magic.spell.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import yifei.frliliens.magic.config.Config;
import yifei.frliliens.magic.spell.AbstractSpell;

/**
 * 地狱业火 Vollzanbel。
 *
 * <p>在目标位置制造持续火焰区域，造成范围火焰伤害。
 */
public class FireSpell extends AbstractSpell {

    public FireSpell() {
        super("vollzanbel", 150);
    }

    @Override
    protected boolean doCast(Level level, Player caster) {
        Vec3 look = caster.getLookAngle();
        Vec3 target = caster.position().add(look.scale(Config.VOLLZANBEL_RANGE.get()));

        // 生成火焰效果云（龙息效果）
        float radius = Config.VOLLZANBEL_RADIUS.get().floatValue();
        int duration = Config.VOLLZANBEL_DURATION.get();
        AreaEffectCloud cloud = new AreaEffectCloud(EntityType.AREA_EFFECT_CLOUD, level);
        cloud.setPos(target.x, target.y, target.z);
        cloud.setRadius(radius);
        cloud.setDuration(duration);
        cloud.setRadiusPerTick(-radius / Math.max(1, duration));
        cloud.setWaitTime(0);
        level.addFreshEntity(cloud);

        // 在目标点周围点燃方块
        if (level instanceof ServerLevel serverLevel) {
            int fireRadius = Config.VOLLZANBEL_FIRE_RADIUS.get();
            for (int dx = -fireRadius; dx <= fireRadius; dx++) {
                for (int dz = -fireRadius; dz <= fireRadius; dz++) {
                    BlockPos firePos = BlockPos.containing(target).offset(dx, 0, dz);
                    if (serverLevel.getBlockState(firePos).isAir()
                            && !serverLevel.getBlockState(firePos.below()).isAir()) {
                        serverLevel.setBlockAndUpdate(firePos,
                                net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
                    }
                }
            }
            serverLevel.playSound(null, target.x, target.y, target.z,
                    SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
        return true;
    }
}
