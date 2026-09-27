package com.mjmod.item;

import com.mjmod.skill.ThunderAccelSkillManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 雷法功法：右键切换雷法加速，不消耗物品。 */
public class ThunderAccelTechniqueItem extends Item {
    public ThunderAccelTechniqueItem() {
        super(new Item.Properties().stacksTo(1).fireResistant());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            ThunderAccelSkillManager.toggle(serverPlayer);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.fail(stack);
    }
}
