package com.mjmod.registry;

import com.mjmod.MJMod;
import com.mjmod.item.OnionItem;
import com.mjmod.item.VoidTearsHelmetItem;
import com.mjmod.item.QianLiYuanXingFuItem;
import com.mjmod.item.PlayerHairItem;
import com.mjmod.item.ThunderAccelTechniqueItem;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 物品注册表
 *
 * 所有新物品都要在这里"登记"，游戏启动时才会真正存在。
 * DeferredRegister 是 Forge 推荐的做法（延迟注册，不会出错）。
 */
public class ModItems {

    /** 物品注册表（modid 用于区分是哪个 mod 的物品） */
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MJMod.MODID);

    /** 洋葱：不可食用，用于将虚空之泪主动激发到右眼。 */
    public static final RegistryObject<Item> ONION =
            ITEMS.register("onion", OnionItem::new);

    /** 虚空之泪权限物品：右键激活三分钟技能使用权，不再占用头盔栏。 */
    public static final RegistryObject<Item> VOID_TEARS_HELMET =
                    ITEMS.register("void_tears_helmet", () ->
                    new VoidTearsHelmetItem());

    /** 千里远行符：主手消耗品，副手掉落物作为跨生物目标钥匙。 */
    public static final RegistryObject<Item> QIANLI_YUANXING_FU =
            ITEMS.register("qianli_yuanxing_fu", QianLiYuanXingFuItem::new);

    /** 玩家受到攻击时概率掉落，记录掉落玩家 UUID。 */
    public static final RegistryObject<Item> PLAYER_HAIR =
            ITEMS.register("player_hair", PlayerHairItem::new);

    /** 雷法功法：右键切换雷法加速。 */
    public static final RegistryObject<Item> THUNDER_ACCEL_TECHNIQUE =
            ITEMS.register("thunder_accel_technique", ThunderAccelTechniqueItem::new);

}
