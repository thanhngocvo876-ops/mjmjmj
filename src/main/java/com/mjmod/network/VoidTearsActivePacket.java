package com.mjmod.network;

import com.mjmod.client.ClientEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record VoidTearsActivePacket(boolean active) {
    public void encode(FriendlyByteBuf buf) { buf.writeBoolean(active); }
    public static VoidTearsActivePacket decode(FriendlyByteBuf buf) { return new VoidTearsActivePacket(buf.readBoolean()); }
    public static void handle(VoidTearsActivePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientEvents.setVoidTearsActive(msg.active())));
        context.setPacketHandled(true);
    }
}
