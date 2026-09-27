package com.mjmod.network;

import com.mjmod.entity.ShenQingShuiZhiYaoEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client choices from the custom leech-demon dialogue screen. */
public record LeechInteractionPacket(int entityId, String action) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeUtf(action, 16);
    }

    public static LeechInteractionPacket decode(FriendlyByteBuf buf) {
        return new LeechInteractionPacket(buf.readVarInt(), buf.readUtf(16));
    }

    public static void handle(LeechInteractionPacket msg,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            var entity = player.serverLevel().getEntity(msg.entityId());
            if (!(entity instanceof ShenQingShuiZhiYaoEntity leech)) return;
            if ("close".equals(msg.action())) {
                leech.endDialogue(player.getUUID());
                return;
            }
            if (!leech.isAlive() || player.distanceToSqr(leech) > 64.0D) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§c水蛭妖已经离开或距离太远。"));
                return;
            }
            leech.beginDialogue(player);
            leech.performDialogueChoice(player, msg.action());
        });
        context.setPacketHandled(true);
    }
}
