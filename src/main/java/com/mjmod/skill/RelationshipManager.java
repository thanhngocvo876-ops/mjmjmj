package com.mjmod.skill;

import com.mjmod.entity.ShenQingShuiZhiYaoEntity;
import com.mjmod.network.MJNetwork;
import com.mjmod.registry.ModEffects;
import com.mjmod.registry.ModSounds;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;

/** Persistent relationship and appearance state used by the leech demon. */
public final class RelationshipManager {
    private RelationshipManager() {}

    public static final String NBT_AFFECTION = "mjmod_shuizhiyao_affection";
    public static final String NBT_APPEARANCE = "mjmod_appearance_type";
    public static final String NBT_APPEARANCE_LOCKED = "mjmod_appearance_locked";
    public static final String NBT_DIALOGUE_MUSIC = "mjmod_leech_dialogue_music";
    public static final String NBT_BLOOD_TECHNIQUE = "mjmod_blood_exchange_technique";
    public static final String NBT_DIVINE_BLOOD_PERMISSION = "mjmod_oriental_divine_blood_permission";
    public static final String APPEARANCE_HANDSOME = "handsome";
    public static final String APPEARANCE_UGLY = "ugly";
    private static final ResourceLocation HANDSOME_ADVANCEMENT =
            new ResourceLocation("mjmod", "appearance/handsome");
    private static final ResourceLocation UGLY_ADVANCEMENT =
            new ResourceLocation("mjmod", "appearance/ugly");

    public static int getAffection(Player player) {
        return Math.max(0, Math.min(100,
                player.getPersistentData().getInt(NBT_AFFECTION)));
    }

    public static void setAffection(ServerPlayer player, int value) {
        int clamped = Math.max(0, Math.min(100, value));
        player.getPersistentData().putInt(NBT_AFFECTION, clamped);
        if (clamped >= 100 && !hasBloodTechnique(player)) {
            grantBloodTechnique(player, true);
        }
        MJNetwork.sendVoidTearsData(player);
    }

    public static int addAffection(ServerPlayer player, ShenQingShuiZhiYaoEntity leech,
                                   int baseAmount, String action) {
        ensureAppearance(player);
        if (isUgly(player) && player.getRandom().nextFloat() < 0.40F) {
            sendDialogue(player, leech, "……今天不想和你说话。", getAffection(player));
            return getAffection(player);
        }
        int amount = Math.max(0, baseAmount);
        if (isHandsome(player)) amount *= 2;
        int old = getAffection(player);
        int next = Math.min(100, old + amount);
        player.getPersistentData().putInt(NBT_AFFECTION, next);
        String line = action + "（好感度 +" + amount + "，当前 " + next + "/100）";
        if (next >= 100 && !hasBloodTechnique(player)) {
            grantBloodTechnique(player, false);
            line += "\n水蛭妖将换血功传授给了你！";
        } else if (isHandsome(player) && player.getRandom().nextFloat() < 0.10F) {
            grantBloodTechnique(player, false);
            line += "\n你的气质让她破例传授了换血功！";
        }
        sendDialogue(player, leech, line, next);
        return next;
    }

    public static void sendDialogue(ServerPlayer player, ShenQingShuiZhiYaoEntity leech,
                                    String line, int affection) {
        MJNetwork.sendLeechDialogue(player, leech.getId(),
                "某不知名深情水蛭妖", line, affection);
    }

    /** Starts the streamed song only for the initial right-click interaction. */
    public static void startDialogueMusic(ServerPlayer player) {
        if (isDialogueMusicEnabled(player)) {
            player.playNotifySound(ModSounds.LEECH_DIALOGUE.get(), SoundSource.MUSIC,
                    0.42F, 1.0F);
        }
    }

    public static boolean isDialogueMusicEnabled(Player player) {
        var data = player.getPersistentData();
        return !data.contains(NBT_DIALOGUE_MUSIC) || data.getBoolean(NBT_DIALOGUE_MUSIC);
    }

    public static void setDialogueMusic(ServerPlayer player, boolean enabled) {
        player.getPersistentData().putBoolean(NBT_DIALOGUE_MUSIC, enabled);
    }

    public static boolean hasBloodTechnique(Player player) {
        return player.getPersistentData().getBoolean(NBT_BLOOD_TECHNIQUE);
    }

    public static void grantBloodTechnique(ServerPlayer player, boolean command) {
        player.getPersistentData().putBoolean(NBT_BLOOD_TECHNIQUE, true);
        player.sendSystemMessage(Component.literal(command
                ? "§d已获得换血功使用权限。"
                : "§d你学会了换血功！按 X 对低血量东方一族或玩家施展。"));
    }

    public static boolean hasDivineBloodPermission(Player player) {
        return player.getPersistentData().getBoolean(NBT_DIVINE_BLOOD_PERMISSION);
    }

    public static void grantDivineBloodPermission(ServerPlayer player) {
        player.getPersistentData().putBoolean(NBT_DIVINE_BLOOD_PERMISSION, true);
        // Divine-blood permission is stored independently from relationship data.
    }

    public static boolean isHandsome(Player player) {
        return APPEARANCE_HANDSOME.equals(player.getPersistentData().getString(NBT_APPEARANCE));
    }

    public static boolean isUgly(Player player) {
        return APPEARANCE_UGLY.equals(player.getPersistentData().getString(NBT_APPEARANCE));
    }

    /** Assigns the permanent 60/40 appearance once, awards it, then only restores its effect. */
    public static void ensureAppearance(ServerPlayer player) {
        var data = player.getPersistentData();
        String appearance = data.getString(NBT_APPEARANCE);
        if (!APPEARANCE_HANDSOME.equals(appearance) && !APPEARANCE_UGLY.equals(appearance)) {
            if (hasAdvancement(player, HANDSOME_ADVANCEMENT)) {
                appearance = APPEARANCE_HANDSOME;
            } else if (hasAdvancement(player, UGLY_ADVANCEMENT)) {
                appearance = APPEARANCE_UGLY;
            } else {
                appearance = player.getRandom().nextFloat() < 0.60F
                        ? APPEARANCE_HANDSOME : APPEARANCE_UGLY;
            }
            data.putString(NBT_APPEARANCE, appearance);
        }
        if (!data.getBoolean(NBT_APPEARANCE_LOCKED)) {
            awardAppearanceAdvancement(player, appearance);
            data.putBoolean(NBT_APPEARANCE_LOCKED, true);
        }
        if (isHandsome(player)) {
            player.addEffect(new MobEffectInstance(ModEffects.HANDSOME.get(),
                    Integer.MAX_VALUE, 0, false, false, true));
            player.removeEffect(ModEffects.UGLY.get());
        } else {
            player.addEffect(new MobEffectInstance(ModEffects.UGLY.get(),
                    Integer.MAX_VALUE, 0, false, false, true));
            player.removeEffect(ModEffects.HANDSOME.get());
        }
    }

    private static boolean hasAdvancement(ServerPlayer player, ResourceLocation id) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(id);
        return advancement != null && player.getAdvancements()
                .getOrStartProgress(advancement).isDone();
    }

    private static void awardAppearanceAdvancement(ServerPlayer player, String appearance) {
        ResourceLocation id = APPEARANCE_HANDSOME.equals(appearance)
                ? HANDSOME_ADVANCEMENT : UGLY_ADVANCEMENT;
        Advancement advancement = player.server.getAdvancements().getAdvancement(id);
        if (advancement != null) player.getAdvancements().award(advancement, "locked");
    }

    public static void copyPlayerData(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
        var oldData = oldPlayer.getPersistentData();
        var newData = newPlayer.getPersistentData();
        if (oldData.contains(NBT_AFFECTION)) newData.putInt(NBT_AFFECTION, oldData.getInt(NBT_AFFECTION));
        if (oldData.contains(NBT_APPEARANCE)) newData.putString(NBT_APPEARANCE, oldData.getString(NBT_APPEARANCE));
        if (oldData.contains(NBT_APPEARANCE_LOCKED)) newData.putBoolean(NBT_APPEARANCE_LOCKED,
                oldData.getBoolean(NBT_APPEARANCE_LOCKED));
        if (oldData.contains(NBT_DIALOGUE_MUSIC)) newData.putBoolean(NBT_DIALOGUE_MUSIC,
                oldData.getBoolean(NBT_DIALOGUE_MUSIC));
        if (oldData.contains(NBT_BLOOD_TECHNIQUE)) newData.putBoolean(NBT_BLOOD_TECHNIQUE,
                oldData.getBoolean(NBT_BLOOD_TECHNIQUE));
        if (oldData.contains(NBT_DIVINE_BLOOD_PERMISSION)) newData.putBoolean(NBT_DIVINE_BLOOD_PERMISSION,
                oldData.getBoolean(NBT_DIVINE_BLOOD_PERMISSION));
    }
}
