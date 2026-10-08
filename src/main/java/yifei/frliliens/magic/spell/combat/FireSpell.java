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
        Vec3 target = caster.position().add(look.scale(8.0));

        // 生成火焰效果云（龙息效果）
        AreaEffectCloud cloud = new AreaEffectCloud(EntityType.AREA_EFFECT_CLOUD, level);
        cloud.setPos(target.x, target.y, target.z);
        cloud.setRadius(3.0F);
        cloud.setDuration(100);
        cloud.setRadiusPerTick(-0.02F);
        cloud.setWaitTime(0);
        level.addFreshEntity(cloud);

        // 在目标点周围点燃方块
        if (level instanceof ServerLevel serverLevel) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
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
