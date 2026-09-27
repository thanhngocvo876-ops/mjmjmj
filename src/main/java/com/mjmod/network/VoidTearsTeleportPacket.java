package com.mjmod.network;

import com.mjmod.skill.VoidTearsSkillManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client request from the skill-3 coordinate screen. */
public record VoidTearsTeleportPacket(String dimension, double x, double y,
                                      double z, boolean emergency, boolean shiftCast) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dimension == null ? "" : dimension, 256);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeBoolean(emergency);
        buf.writeBoolean(shiftCast);
    }

    public static VoidTearsTeleportPacket decode(FriendlyByteBuf buf) {
        return new VoidTearsTeleportPacket(buf.readUtf(256), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(VoidTearsTeleportPacket msg,
                              Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            VoidTearsSkillManager.teleportToCoordinates(player, msg.dimension(),
                    msg.x(), msg.y(), msg.z(), msg.emergency(), msg.shiftCast());
        });
        context.setPacketHandled(true);
    }
}
