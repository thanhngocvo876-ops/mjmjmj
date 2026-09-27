package com.mjmod.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** 玩家受到攻击时有概率掉落的目标钥匙。 */
public class PlayerHairItem extends Item {
    public PlayerHairItem() {
        super(new Item.Properties().stacksTo(16));
    }

    /**
     * Each stack carries the target player's name for visual distinction. The
     * UUID stored by QianLiYuanXingFuItem remains the authoritative locator.
     */
    @Override
    public Component getName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(QianLiYuanXingFuItem.TAG_TARGET_PLAYER_NAME)) {
            String name = tag.getString(QianLiYuanXingFuItem.TAG_TARGET_PLAYER_NAME);
            if (!name.isBlank()) {
                return Component.literal(name + "的头发");
            }
        }
        return super.getName(stack);
    }
}
