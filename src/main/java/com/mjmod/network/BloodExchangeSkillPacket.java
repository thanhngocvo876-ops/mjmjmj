package com.mjmod.network;

import com.mjmod.skill.BloodExchangeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** X 键专用的换血功施法请求。 */
public record BloodExchangeSkillPacket() {
    public void encode(FriendlyByteBuf buf) {}

    public static BloodExchangeSkillPacket decode(FriendlyByteBuf buf) {
        return new BloodExchangeSkillPacket();
    }

    public static void handle(BloodExchangeSkillPacket msg,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) BloodExchangeManager.tryCastFromKey(player);
        });
        context.setPacketHandled(true);
    }
}
