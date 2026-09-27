package com.mjmod.network;

import com.mjmod.client.gui.LeechDialogueScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server-to-client galgame-style dialogue overlay for the leech demon. */
public record LeechDialoguePacket(int entityId, String speaker, String line, int affection) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeUtf(speaker, 128);
        buf.writeUtf(line, 2048);
        buf.writeVarInt(affection);
    }

    public static LeechDialoguePacket decode(FriendlyByteBuf buf) {
        return new LeechDialoguePacket(buf.readVarInt(), buf.readUtf(128),
                buf.readUtf(2048), buf.readVarInt());
    }

    public static void handle(LeechDialoguePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof LeechDialogueScreen screen
                    && screen.getLeechEntityId() == msg.entityId()) {
                screen.updateDialogue(msg.speaker(), msg.line(), msg.affection());
            } else {
                minecraft.setScreen(new LeechDialogueScreen(msg.entityId(), msg.speaker(),
                        msg.line(), msg.affection()));
            }
        }));
        context.setPacketHandled(true);
    }
}
