package com.mjmod.network;

import com.mjmod.skill.VoidTearsSkillManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Coordinate-point UI actions: 0=request, 1=select, 2=delete, 3=add current,
 * 4=clear selection.
 */
public record VoidTearsPointsActionPacket(int action, String uid) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(action);
        buf.writeUtf(uid == null ? "" : uid, 128);
    }

    public static VoidTearsPointsActionPacket decode(FriendlyByteBuf buf) {
        return new VoidTearsPointsActionPacket(buf.readVarInt(), buf.readUtf(128));
    }

    public static void handle(VoidTearsPointsActionPacket msg,
                              Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (!VoidTearsSkillManager.hasPermission(player)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "\u00a7c需要先使用虚空之泪消耗品获得法宝权限。"));
                return;
            }
            switch (msg.action()) {
                case 0 -> { }
                case 1 -> VoidTearsSkillManager.selectCoordinatePoint(player, msg.uid());
                case 2 -> VoidTearsSkillManager.deleteCoordinatePoint(player, msg.uid());
                case 3 -> VoidTearsSkillManager.addCoordinatePoint(player);
                case 4 -> VoidTearsSkillManager.selectCoordinatePoint(player, "");
                default -> { return; }
            }
            MJNetwork.sendVoidTearsPoints(player);
        });
        context.setPacketHandled(true);
    }
}
