package com.mjmod.network;

import com.mjmod.client.ClientEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server-to-client synchronization for the Void Tears status screen. */
public record VoidTearsStatusPacket(int tragicHeart, boolean permission) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(tragicHeart);
        buf.writeBoolean(permission);
    }

    public static VoidTearsStatusPacket decode(FriendlyByteBuf buf) {
        return new VoidTearsStatusPacket(buf.readInt(), buf.readBoolean());
    }

    public static void handle(VoidTearsStatusPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientEvents.setVoidTearsData(msg.tragicHeart(), msg.permission())));
        context.setPacketHandled(true);
    }
}
