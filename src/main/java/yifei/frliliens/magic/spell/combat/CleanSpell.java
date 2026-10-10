package yifei.frliliens.magic.spell.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import yifei.frliliens.magic.attachment.ManaData;
import yifei.frliliens.magic.attachment.ModAttachments;
import yifei.frliliens.magic.item.StaffItem;
import yifei.frliliens.magic.spell.AbstractSpell;

/**
 * 清洁魔法（菲伦专属）。
 *
 * <p>生活魔法：将身上穿戴的盔甲「洗干净」，即修复全部耐久。
 * 魔力消耗与冷却随盔甲总缺失耐久动态变化——耐久越低，消耗越高、冷却越长。
 *
 * <p>由于消耗是动态的，{@link #getManaCost()} 与 {@link #getCooldown()} 均返回 0，
 * 由 {@link #doCast} 内部自行计算并扣除魔力、设置法杖冷却。
 */
public class CleanSpell extends AbstractSpell {

    /** 基础魔力消耗（无损坏时）。 */
    private static final int BASE_MANA_COST = 50;
    /** 每点缺失耐久额外消耗的魔力。 */
    private static final int MANA_PER_DAMAGE = 2;
    /** 基础冷却（tick，无损坏时）。 */
    private static final int BASE_COOLDOWN = 100;
    /** 每点缺失耐久额外增加的冷却（tick）。 */
    private static final int COOLDOWN_PER_DAMAGE = 2;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST,
            EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public CleanSpell() {
        // 固定值返回 0，实际消耗在 doCast 中动态计算
        super("clean", 0, 0);
    }

    @Override
    protected boolean doCast(Level level, Player caster) {
        // 统计所有盔甲的总缺失耐久
        int totalDamage = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = caster.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.isDamageableItem()) {
                continue;
            }
            totalDamage += stack.getDamageValue();
        }

        // 没有可修复的盔甲
        if (totalDamage <= 0) {
            return false;
        }

        // 动态计算消耗与冷却
        int manaCost = BASE_MANA_COST + totalDamage * MANA_PER_DAMAGE;
        int cooldown = BASE_COOLDOWN + totalDamage * COOLDOWN_PER_DAMAGE;

        // 校验魔力
        ManaData mana = caster.getData(ModAttachments.MANA);
        if (mana.current() < manaCost) {
            caster.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "message.friliensmagic.mana_not_enough",
                            getDisplayNameComponent(), manaCost)
                            .withStyle(net.minecraft.ChatFormatting.RED),
                    true);
            return false;
        }

        // 扣除魔力
        caster.setData(ModAttachments.MANA, mana.consume(manaCost));

        // 修复全部盔甲
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = caster.getItemBySlot(slot);
            if (stack.isDamageableItem()) {
                stack.setDamageValue(0);
            }
        }

        // 设置法杖冷却（跳过 StaffItem 默认的 0 冷却）
        StaffItem staff = findHeldStaff(caster);
        if (staff != null) {
            caster.getCooldowns().addCooldown(staff, cooldown);
        }

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.AMBIENT_UNDERWATER_EXIT, SoundSource.PLAYERS, 0.6F, 1.2F);
        }
        return true;
    }

    /** 获取玩家手中持有的法杖（主手或副手）。 */
    private static StaffItem findHeldStaff(Player player) {
        if (player.getMainHandItem().getItem() instanceof StaffItem s) {
            return s;
        }
        if (player.getOffhandItem().getItem() instanceof StaffItem s) {
            return s;
        }
        return null;
    }
}
