package yifei.frliliens.magic.item;

import net.minecraft.core.registries.BuiltInRegistries;
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
import yifei.frliliens.magic.spell.StaffSpells;

/**
 * 法杖物品。
 *
 * <p>右键释放<b>本把法杖</b>当前选中的战斗法术（通过法术轮盘选择），消耗对应魔力。
 * 每把法杖各自记录选中法术，且只能施放 {@link StaffSpells} 中归属于它的法术。
 */
public class StaffItem extends Item {

    public StaffItem(Properties properties) {
        super(properties);
    }

    /**
     * 取物品栈对应法杖的注册名（如 {@code frieren_staff}）。
     *
     * <p>这是全模组识别「手持的是哪把法杖」的唯一入口，客户端选法术、
     * 服务端做校验都走这里。
     *
     * @param stack 物品栈
     * @return 注册名；不是法杖时返回 {@code null}
     */
    public static String staffIdOf(ItemStack stack) {
        if (!(stack.getItem() instanceof StaffItem)) {
            return null;
        }
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key == null ? null : key.getPath();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 冷却中则不响应
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        String staffId = staffIdOf(stack);
        if (staffId == null) {
            return InteractionResultHolder.fail(stack);
        }

        if (level.isClientSide) {
            return InteractionResultHolder.consume(stack);
        }

        // 读取「本把法杖」选中的法术
        SelectedSpellData selected = player.getData(ModAttachments.SELECTED_SPELL);
        String spellId = selected.spellFor(staffId);
        if (spellId.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.friliensmagic.no_spell_selected")
                            .withStyle(net.minecraft.ChatFormatting.YELLOW),
                    true);
            return InteractionResultHolder.fail(stack);
        }

        Spell spell = SpellRegistry.get(spellId);
        if (spell == null) {
            // 法术已不存在（例如被移除），清掉这条选择
            player.setData(ModAttachments.SELECTED_SPELL, selected.select(staffId, ""));
            return InteractionResultHolder.fail(stack);
        }

        // 归属校验：客户端轮盘已按法杖过滤，这里是服务端兜底
        if (!StaffSpells.allows(staffId, spellId)) {
            player.displayClientMessage(
                    Component.translatable("message.friliensmagic.spell_wrong_staff",
                            spell.getDisplayNameComponent())
                            .withStyle(net.minecraft.ChatFormatting.RED),
                    true);
            return InteractionResultHolder.fail(stack);
        }

        // 校验魔力
        ManaData mana = player.getData(ModAttachments.MANA);
        int cost = spell.getManaCost();
        if (mana.current() < cost) {
            player.displayClientMessage(
                    Component.translatable("message.friliensmagic.mana_not_enough",
                            spell.getDisplayNameComponent(), cost)
                            .withStyle(net.minecraft.ChatFormatting.RED),
                    true);
            return InteractionResultHolder.fail(stack);
        }

        // 释放法术
        boolean success = spell.cast(level, player);
        if (success) {
            player.setData(ModAttachments.MANA, mana.consume(cost));
            player.getCooldowns().addCooldown(this, spell.getCooldown());
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.6F, 1.6F);
        }

        return InteractionResultHolder.consume(stack);
    }
}
