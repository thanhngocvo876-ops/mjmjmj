package com.mjmod.item;

import com.mjmod.skill.VoidTearsSkillManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 不可食用的洋葱催化物。右键时不会进入食用动画，而是尝试放到
 * 虚空之泪的右眼位置并激活其主动状态。
 */
public class OnionItem extends Item {
    public OnionItem() {
        super(new Item.Properties().stacksTo(1).durability(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Do not start a use/eat animation on the client. The server owns the
        // activation and inventory change, then the normal player sync updates
        // the client.
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!VoidTearsSkillManager.hasPermission(serverPlayer)) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "\u00a7c需要先使用虚空之泪消耗品获得法宝权限。"));
            return InteractionResultHolder.fail(stack);
        }

        if (VoidTearsSkillManager.isActive(serverPlayer)) {
            long seconds = (VoidTearsSkillManager.getRemainingActiveTicks(serverPlayer) + 19L) / 20L;
            serverPlayer.sendSystemMessage(Component.literal(
                    "\u00a77虚空之泪已经被洋葱激发，剩余 " + seconds + " 秒。"));
            return InteractionResultHolder.success(stack);
        }

        VoidTearsSkillManager.activate(serverPlayer);
        if (!serverPlayer.getAbilities().instabuild)
            stack.hurtAndBreak(1, serverPlayer, p -> p.broadcastBreakEvent(hand));

        level.playSound(null, serverPlayer.blockPosition(), SoundEvents.PLAYER_BURP,
                SoundSource.PLAYERS, 0.7F, 1.8F);
        serverPlayer.sendSystemMessage(Component.literal(
                "\u00a7b\u00a7l虚空之泪已激发！\u00a7r\u00a77 效果持续 3 分钟。"));
        return InteractionResultHolder.success(stack);
    }
}
