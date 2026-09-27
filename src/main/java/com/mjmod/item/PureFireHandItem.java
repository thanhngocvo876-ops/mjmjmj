package com.mjmod.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A bound visual item used by pure-fire skill 2.  The two registry entries are
 * intentionally separate so vanilla first-person/third-person item rendering
 * owns the hand-bone transform instead of a detached player RenderLayer.
 */
public final class PureFireHandItem extends Item {
    private final boolean mainHand;

    public PureFireHandItem(boolean mainHand) {
        super(new Item.Properties().stacksTo(1).fireResistant());
        this.mainHand = mainHand;
    }

    public boolean isMainHandItem() {
        return mainHand;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
