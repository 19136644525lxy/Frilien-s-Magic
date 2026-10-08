package yifei.frliliens.magic.item;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import yifei.frliliens.magic.FriliensMagic;
import yifei.frliliens.magic.attachment.ManaData;
import yifei.frliliens.magic.attachment.ModAttachments;

/**
 * 魔力药水。
 *
 * <p>右键饮用恢复魔力，特定等级可永久提升魔力上限。
 * 饮用后物品消耗 1 个。
 *
 * <p>数值设计：1/3/5 级提升上限，2/4 级仅恢复且恢复量递增。
 */
public class ManaPotionItem extends Item {

    private final int restore;
    private final int maxIncrease;

    /**
     * @param properties  物品属性
     * @param restore     饮用后恢复的魔力值
     * @param maxIncrease 饮用后永久提升的魔力上限（0 表示不提升）
     */
    public ManaPotionItem(Properties properties, int restore, int maxIncrease) {
        super(properties);
        this.restore = restore;
        this.maxIncrease = maxIncrease;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return stack;
        }

        if (!level.isClientSide) {
            ManaData mana = player.getData(ModAttachments.MANA);
            ManaData newMana = mana.regen(this.restore);

            // 1/3/5 级永久提升上限
            if (this.maxIncrease > 0) {
                newMana = newMana.withMax(newMana.max() + this.maxIncrease);
            }

            player.setData(ModAttachments.MANA, newMana);
            player.syncData(ModAttachments.MANA);

            FriliensMagic.LOGGER.info("Mana potion used: restore={}, maxIncrease={}, before={}/{}, after={}/{}",
                    this.restore, this.maxIncrease, mana.current(), mana.max(),
                    newMana.current(), newMana.max());

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.5F,
                    0.8F + level.random.nextFloat() * 0.4F);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.friliensmagic.mana_potion.restore", this.restore));
        if (this.maxIncrease > 0) {
            tooltip.add(Component.translatable("tooltip.friliensmagic.mana_potion.max_increase", this.maxIncrease));
        }
    }
}
