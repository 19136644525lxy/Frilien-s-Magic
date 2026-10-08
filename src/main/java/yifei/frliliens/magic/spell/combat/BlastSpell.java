package yifei.frliliens.magic.spell.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import yifei.frliliens.magic.damage.ModDamageTypes;
import yifei.frliliens.magic.spell.AbstractSpell;

/**
 * 爆破魔法。
 *
 * <p>在视线前方引爆，破坏地形并对范围内实体造成穿透护甲的魔法伤害。
 */
public class BlastSpell extends AbstractSpell {

    /** 爆炸半径。 */
    private static final float RADIUS = 6.0F;

    /** 伤害值。 */
    private static final float DAMAGE = 200.0F;

    public BlastSpell() {
        super("blast", 70);
    }

    @Override
    protected boolean doCast(Level level, Player caster) {
        Vec3 look = caster.getLookAngle();
        Vec3 target = caster.position().add(look.scale(6.0));

        // 原版爆炸：破坏地形 + 对实体造成自定义魔法伤害
        level.explode(caster,
                ModDamageTypes.magic(caster),
                null,
                target.x, target.y, target.z,
                RADIUS, false, Level.ExplosionInteraction.BLOCK);

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, target.x, target.y, target.z,
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0F, 0.8F);
        }
        return true;
    }
}
