package com.mjmod.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import com.mjmod.skill.VoidTearsSkillManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * 虚空之泪法宝权限消耗品。
 *
 * The registry id/class name intentionally retain the old "helmet" wording so
 * existing worlds and commands can still resolve the item. It is an ordinary,
 * single-use item now: right-clicking consumes it and permanently grants the
 * player's Void Tears artifact permission. Onion activation is a separate step
 * and is responsible for making the model visible for three minutes.
 */
public class VoidTearsHelmetItem extends Item {
    public VoidTearsHelmetItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }
        if (VoidTearsSkillManager.hasPermission(serverPlayer)) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "\u00a77你已经拥有虚空之泪法宝权限。请使用洋葱显形。"));
            return InteractionResultHolder.success(stack);
        }

        // The item is a consumable unlock token, not an equippable helmet.
        serverPlayer.getPersistentData().putBoolean(
                VoidTearsSkillManager.NBT_VOID_TEARS_PERMISSION, true);
        VoidTearsSkillManager.awardVoidTearsAdvancements(serverPlayer,
                VoidTearsSkillManager.getVoidTearsLevel(serverPlayer));
        com.mjmod.network.MJNetwork.sendVoidTearsData(serverPlayer);
        if (!serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, serverPlayer.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, 0.8F, 1.35F);
        serverPlayer.sendSystemMessage(Component.literal(
                "\u00a7b\u00a7l已获得虚空之泪法宝权限！\u00a7r\u00a77 使用洋葱可显形并激活技能。"));
        return InteractionResultHolder.consume(stack);
    }
}
