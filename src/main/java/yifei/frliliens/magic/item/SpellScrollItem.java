package yifei.frliliens.magic.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import yifei.frliliens.magic.attachment.LearnedSpellsData;
import yifei.frliliens.magic.attachment.ModAttachments;
import yifei.frliliens.magic.spell.Spell;

import java.util.List;

/**
 * 法术卷轴。
 *
 * <p>右键学习卷轴记载的法术。学习成功后消耗卷轴，已学习则提示。
 * 学习后可通过法杖（战斗魔法）或按键（基础能力）施放。
 *
 * <p>遵循依赖倒置：依赖 {@link Spell} 抽象，通过构造参数绑定具体法术。
 */
public class SpellScrollItem extends Item {

    private final Spell spell;

    public SpellScrollItem(Properties properties, Spell spell) {
        super(properties);
        this.spell = spell;
    }

    public Spell getSpell() {
        return spell;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.consume(stack);
        }

        LearnedSpellsData learned = player.getData(ModAttachments.LEARNED_SPELLS);

        // 已学习
        if (learned.hasLearned(spell.getId())) {
            player.displayClientMessage(
                    Component.translatable("message.friliensmagic.spell_already_learned",
                            spell.getDisplayNameComponent()), true);
            return InteractionResultHolder.fail(stack);
        }

        // 学习法术
        player.setData(ModAttachments.LEARNED_SPELLS, learned.learn(spell.getId()));
        player.syncData(ModAttachments.LEARNED_SPELLS);

        player.displayClientMessage(
                Component.translatable("message.friliensmagic.spell_learned",
                        spell.getDisplayNameComponent()), true);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.5F);

        // 消耗卷轴（创造模式除外）
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.friliensmagic.spell_name",
                spell.getDisplayNameComponent()));
        if (spell.getManaCost() > 0) {
            tooltipComponents.add(Component.translatable("tooltip.friliensmagic.mana_cost",
                    spell.getManaCost()));
        }
        tooltipComponents.add(Component.translatable("tooltip.friliensmagic.scroll_hint"));
    }
}
