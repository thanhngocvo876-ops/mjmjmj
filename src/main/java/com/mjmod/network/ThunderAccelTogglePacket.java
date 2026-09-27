package com.mjmod.network;

import com.mjmod.skill.ThunderAccelSkillManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** B 键开关雷法加速。服务端自己算持续和冷却。 */
public record ThunderAccelTogglePacket() {
    public void encode(FriendlyByteBuf buf) {}

    public static ThunderAccelTogglePacket decode(FriendlyByteBuf buf) {
        return new ThunderAccelTogglePacket();
    }

    public static void handle(ThunderAccelTogglePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) ThunderAccelSkillManager.toggle(player);
        });
        context.setPacketHandled(true);
    }
}
