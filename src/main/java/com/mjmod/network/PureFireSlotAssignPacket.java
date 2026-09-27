package com.mjmod.network;

import com.mjmod.skill.PureFireSkillManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Pure-fire slot configuration packet: assigns a specific skillId to slotIndex.
 */
public record PureFireSlotAssignPacket(int slotIndex, int skillId) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(slotIndex);
        buf.writeVarInt(skillId);
    }

    public static PureFireSlotAssignPacket decode(FriendlyByteBuf buf) {
        return new PureFireSlotAssignPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(PureFireSlotAssignPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            PureFireSkillManager.assignSkillToSlot(player, msg.slotIndex(), msg.skillId());
        });
        context.setPacketHandled(true);
    }
}
