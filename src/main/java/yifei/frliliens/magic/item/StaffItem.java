package yifei.frliliens.magic.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import yifei.frliliens.magic.attachment.ManaData;
import yifei.frliliens.magic.attachment.ModAttachments;
import yifei.frliliens.magic.attachment.SelectedSpellData;
import yifei.frliliens.magic.spell.Spell;
import yifei.frliliens.magic.spell.SpellRegistry;

/**
 * 法杖物品。
 *
 * <p>右键释放玩家当前选中的战斗法术（通过法术轮盘选择），消耗对应魔力。
 * 未选中法术时提示玩家使用轮盘选择。
 */
public class StaffItem extends Item {

    public StaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 冷却中则不响应
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (level.isClientSide) {
            return InteractionResultHolder.consume(stack);
        }

        // 读取当前选中的法术
        SelectedSpellData selected = player.getData(ModAttachments.SELECTED_SPELL);
        if (selected.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.friliensmagic.no_spell_selected"), true);
            return InteractionResultHolder.fail(stack);
        }

        Spell spell = SpellRegistry.get(selected.spellId());
        if (spell == null) {
            player.setData(ModAttachments.SELECTED_SPELL, SelectedSpellData.EMPTY);
            player.syncData(ModAttachments.SELECTED_SPELL);
            return InteractionResultHolder.fail(stack);
        }

        // 校验魔力
        ManaData mana = player.getData(ModAttachments.MANA);
        int cost = spell.getManaCost();
        if (mana.current() < cost) {
            player.displayClientMessage(
                    Component.translatable("message.friliensmagic.mana_not_enough",
                            spell.getDisplayNameComponent(), cost), true);
            return InteractionResultHolder.fail(stack);
        }

        // 释放法术
        boolean success = spell.cast(level, player);
        if (success) {
            player.setData(ModAttachments.MANA, mana.consume(cost));
            player.syncData(ModAttachments.MANA);
            player.getCooldowns().addCooldown(this, 20);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.6F, 1.6F);
        }

        return InteractionResultHolder.consume(stack);
    }
}
