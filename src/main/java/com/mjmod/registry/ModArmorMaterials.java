package com.mjmod.registry;

import com.mjmod.MJMod;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Items;

/** 虚空之泪使用独立材质，耐久与铁甲相近但不消耗修复材料。 */
public final class ModArmorMaterials {
    private ModArmorMaterials() {}

    public static final ArmorMaterial VOID_TEARS = new ArmorMaterial() {
        private final int[] DURABILITY = {11, 16, 15, 13};
        private final int[] DEFENSE = {0, 0, 0, 2};

        @Override public int getDurabilityForType(net.minecraft.world.item.ArmorItem.Type type) { return DURABILITY[type.getSlot().getIndex()] * 15; }
        @Override public int getDefenseForType(net.minecraft.world.item.ArmorItem.Type type) { return DEFENSE[type.getSlot().getIndex()]; }
        @Override public int getEnchantmentValue() { return 18; }
        @Override public net.minecraft.sounds.SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_CHAIN; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(Items.IRON_INGOT); }
        @Override public String getName() { return MJMod.MODID + ":void_tears"; }
        @Override public float getToughness() { return 1.0F; }
        @Override public float getKnockbackResistance() { return 0.0F; }
    };
}
