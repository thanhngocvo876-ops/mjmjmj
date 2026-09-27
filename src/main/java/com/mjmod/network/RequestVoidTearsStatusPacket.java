package com.mjmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client request for the current Void Tears status. */
public final class RequestVoidTearsStatusPacket {
    public void encode(FriendlyByteBuf buf) {}

    public static RequestVoidTearsStatusPacket decode(FriendlyByteBuf buf) {
        return new RequestVoidTearsStatusPacket();
    }

    public static void handle(RequestVoidTearsStatusPacket msg,
                               Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) MJNetwork.sendVoidTearsData(player);
        });
        context.setPacketHandled(true);
    }
}
