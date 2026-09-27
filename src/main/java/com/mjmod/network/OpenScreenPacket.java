package com.mjmod.network;

import com.mjmod.client.gui.MJScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenScreenPacket {

    public void encode(FriendlyByteBuf buf) {
    }

    public static OpenScreenPacket decode(FriendlyByteBuf buf) {
        return new OpenScreenPacket();
    }

    public static void handle(OpenScreenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> Minecraft.getInstance().setScreen(new MJScreen())));
        context.setPacketHandled(true);
    }
}
