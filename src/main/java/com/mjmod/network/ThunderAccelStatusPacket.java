package com.mjmod.network;

import com.mjmod.client.ClientEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** 把谁开着雷法同步到客户端，残影和隐身只看这个。 */
public record ThunderAccelStatusPacket(UUID playerId, boolean active, int remaining, int cooldown) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerId);
        buf.writeBoolean(active);
        buf.writeVarInt(remaining);
        buf.writeVarInt(cooldown);
    }

    public static ThunderAccelStatusPacket decode(FriendlyByteBuf buf) {
        return new ThunderAccelStatusPacket(buf.readUUID(), buf.readBoolean(),
                buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(ThunderAccelStatusPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientEvents.setThunderAccel(msg.playerId(), msg.active(),
                        msg.remaining(), msg.cooldown())));
        context.setPacketHandled(true);
    }
}
