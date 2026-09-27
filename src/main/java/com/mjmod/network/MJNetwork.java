package com.mjmod.network;

import com.mjmod.MJMod;
import com.mjmod.skill.VoidTearsSkillManager;
import com.mjmod.skill.PureFireSkillManager;
import com.mjmod.skill.ThunderAccelSkillManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Network channels for the mod's gameplay systems. */
public final class MJNetwork {
    private MJNetwork() {}
    private static final String PROTOCOL_VERSION = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MJMod.MODID, "main"), () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    public static void register() {
        CHANNEL.registerMessage(0, OpenScreenPacket.class,
                OpenScreenPacket::encode, OpenScreenPacket::decode, OpenScreenPacket::handle);
        CHANNEL.registerMessage(1, VoidTearsActivePacket.class,
                VoidTearsActivePacket::encode, VoidTearsActivePacket::decode,
                VoidTearsActivePacket::handle);
        CHANNEL.registerMessage(2, VoidTearsSkillPacket.class,
                VoidTearsSkillPacket::encode, VoidTearsSkillPacket::decode,
                VoidTearsSkillPacket::handle);
        CHANNEL.registerMessage(3, SkillAnimationPacket.class,
                SkillAnimationPacket::encode, SkillAnimationPacket::decode,
                SkillAnimationPacket::handle);
        CHANNEL.registerMessage(4, VoidTearsPointsActionPacket.class,
                VoidTearsPointsActionPacket::encode, VoidTearsPointsActionPacket::decode,
                VoidTearsPointsActionPacket::handle);
        CHANNEL.registerMessage(5, VoidTearsPointsDataPacket.class,
                VoidTearsPointsDataPacket::encode, VoidTearsPointsDataPacket::decode,
                VoidTearsPointsDataPacket::handle);
        CHANNEL.registerMessage(6, VoidTearsTeleportPacket.class,
                VoidTearsTeleportPacket::encode, VoidTearsTeleportPacket::decode,
                VoidTearsTeleportPacket::handle);
        CHANNEL.registerMessage(7, LeechDialoguePacket.class,
                LeechDialoguePacket::encode, LeechDialoguePacket::decode,
                LeechDialoguePacket::handle);
        CHANNEL.registerMessage(8, BloodExchangeSkillPacket.class,
                BloodExchangeSkillPacket::encode, BloodExchangeSkillPacket::decode,
                BloodExchangeSkillPacket::handle);
        CHANNEL.registerMessage(9, LeechInteractionPacket.class,
                LeechInteractionPacket::encode, LeechInteractionPacket::decode,
                LeechInteractionPacket::handle);
        CHANNEL.registerMessage(10, VoidTearsStatusPacket.class,
                VoidTearsStatusPacket::encode, VoidTearsStatusPacket::decode,
                VoidTearsStatusPacket::handle);
        CHANNEL.registerMessage(11, RequestVoidTearsStatusPacket.class,
                RequestVoidTearsStatusPacket::encode, RequestVoidTearsStatusPacket::decode,
                RequestVoidTearsStatusPacket::handle);
        CHANNEL.registerMessage(12, PureFireSkillPacket.class,
                PureFireSkillPacket::encode, PureFireSkillPacket::decode,
                PureFireSkillPacket::handle);
        CHANNEL.registerMessage(13, PureFireStatusPacket.class,
                PureFireStatusPacket::encode, PureFireStatusPacket::decode,
                PureFireStatusPacket::handle);
        CHANNEL.registerMessage(14, PureFireSlotAssignPacket.class,
                PureFireSlotAssignPacket::encode, PureFireSlotAssignPacket::decode,
                PureFireSlotAssignPacket::handle);
        CHANNEL.registerMessage(15, ThunderAccelTogglePacket.class,
                ThunderAccelTogglePacket::encode, ThunderAccelTogglePacket::decode,
                ThunderAccelTogglePacket::handle);
        CHANNEL.registerMessage(16, ThunderAccelStatusPacket.class,
                ThunderAccelStatusPacket::encode, ThunderAccelStatusPacket::decode,
                ThunderAccelStatusPacket::handle);
    }

    public static void sendOpenScreen(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenScreenPacket());
    }

    public static void sendVoidTearsActive(ServerPlayer player, boolean active) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new VoidTearsActivePacket(active));
    }

    public static void sendVoidTearsSkill(int action, int skill) {
        sendVoidTearsSkill(action, skill, false);
    }

    public static void sendVoidTearsSkill(int action, int skill, boolean shift) {
        CHANNEL.sendToServer(new VoidTearsSkillPacket(action, skill, shift));
    }

    public static void sendBloodExchange() {
        CHANNEL.sendToServer(new BloodExchangeSkillPacket());
    }

    public static void sendLeechInteraction(int entityId, String action) {
        CHANNEL.sendToServer(new LeechInteractionPacket(entityId, action));
    }

    public static void requestVoidTearsPoints() {
        CHANNEL.sendToServer(new VoidTearsPointsActionPacket(0, ""));
    }

    public static void selectVoidTearsPoint(String uid) {
        CHANNEL.sendToServer(new VoidTearsPointsActionPacket(1, uid));
    }

    public static void deleteVoidTearsPoint(String uid) {
        CHANNEL.sendToServer(new VoidTearsPointsActionPacket(2, uid));
    }

    public static void addVoidTearsPoint() {
        CHANNEL.sendToServer(new VoidTearsPointsActionPacket(3, ""));
    }

    public static void clearVoidTearsPointSelection() {
        CHANNEL.sendToServer(new VoidTearsPointsActionPacket(4, ""));
    }

    public static void sendVoidTearsPoints(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new VoidTearsPointsDataPacket(
                        VoidTearsSkillManager.getCoordinatePoints(player),
                        VoidTearsSkillManager.getSelectedPointUid(player)));
    }

    public static void sendVoidTearsTeleport(String dimension, double x, double y, double z) {
        sendVoidTearsTeleport(dimension, x, y, z, false);
    }

    public static void sendVoidTearsTeleport(String dimension, double x, double y, double z,
                                             boolean shiftCast) {
        CHANNEL.sendToServer(new VoidTearsTeleportPacket(
                dimension, x, y, z, false, shiftCast));
    }

    public static void sendVoidTearsEmergencyTeleport() {
        sendVoidTearsEmergencyTeleport(false);
    }

    public static void sendVoidTearsEmergencyTeleport(boolean shiftCast) {
        CHANNEL.sendToServer(new VoidTearsTeleportPacket(
                "", 0.0D, 0.0D, 0.0D, true, shiftCast));
    }

    public static void sendSkillAnimation(ServerPlayer player, int skillId, int durationTicks) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SkillAnimationPacket(skillId, durationTicks));
    }

    public static void sendLeechDialogue(ServerPlayer player, int entityId,
                                         String speaker, String line, int affection) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new LeechDialoguePacket(entityId, speaker, line, affection));
    }

    public static void sendVoidTearsData(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new VoidTearsStatusPacket(VoidTearsSkillManager.get悲情之心(player),
                        VoidTearsSkillManager.hasPermission(player)));
    }

    public static void requestVoidTearsData() {
        CHANNEL.sendToServer(new RequestVoidTearsStatusPacket());
    }

    public static void sendPureFireSkill(int action, int slot, boolean reverse) {
        CHANNEL.sendToServer(new PureFireSkillPacket(action, slot, reverse));
    }

    public static void sendPureFireSlotAssign(int slotIndex, int skillId) {
        CHANNEL.sendToServer(new PureFireSlotAssignPacket(slotIndex, skillId));
    }

    public static void sendPureFireStatus(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new PureFireStatusPacket(
                        PureFireSkillManager.hasPermission(player),
                        PureFireSkillManager.isMeditating(player),
                        PureFireSkillManager.isCasting(player),
                        PureFireSkillManager.isReleasing(player),
                        PureFireSkillManager.isHandFormActive(player),
                        PureFireSkillManager.getProficiency(player),
                        PureFireSkillManager.getSelectedSlot(player),
                        PureFireSkillManager.getSelectedScheme(player),
                        PureFireSkillManager.isPureYangBodyProtectionActive(player),
                        PureFireSkillManager.getMeditationProgress(player),
                        PureFireSkillManager.getTalentPoints(player),
                        PureFireSkillManager.getMeditationLayers(player),
                        PureFireSkillManager.getMeditationCooldownRemaining(player),
                        PureFireSkillManager.getUnlockedMask(player),
                        PureFireSkillManager.getEquippedSkills(player)));
    }

    public static void sendThunderAccelToggle() {
        CHANNEL.sendToServer(new ThunderAccelTogglePacket());
    }

    public static void sendThunderAccelStatus(ServerPlayer player) {
        ThunderAccelStatusPacket packet = new ThunderAccelStatusPacket(
                player.getUUID(),
                ThunderAccelSkillManager.isActive(player),
                ThunderAccelSkillManager.remainingTicks(player),
                ThunderAccelSkillManager.cooldownTicks(player));
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet);
    }
}
