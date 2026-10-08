package yifei.frliliens.magic.spell.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import yifei.frliliens.magic.spell.AbstractSpell;

/**
 * 破灭之雷 Judradjim。
 *
 * <p>在视线落点召唤多道闪电，造成范围雷电伤害。
 */
public class LightningSpell extends AbstractSpell {

    public LightningSpell() {
        super("judradjim", 120);
    }

    @Override
    protected boolean doCast(Level level, Player caster) {
        HitResult hit = caster.pick(30.0, 0.0F, false);
        BlockPos pos;
        if (hit.getType() == HitResult.Type.BLOCK) {
            pos = ((BlockHitResult) hit).getBlockPos();
        } else {
            pos = caster.blockPosition().relative(caster.getDirection(), 5);
        }

        // 在目标点周围召唤 3 道闪电
        for (int i = 0; i < 3; i++) {
            int ox = (int) (caster.getRandom().nextGaussian() * 2);
            int oz = (int) (caster.getRandom().nextGaussian() * 2);
            BlockPos strikePos = pos.offset(ox, 0, oz);

            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
            if (lightning != null) {
                lightning.moveTo(strikePos.getX() + 0.5, strikePos.getY(),
                        strikePos.getZ() + 0.5);
                lightning.setCause((net.minecraft.server.level.ServerPlayer) caster);
                level.addFreshEntity(lightning);
            }
        }

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
        return true;
    }
}
