package com.mjmod.network;

import com.mjmod.client.ClientEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server-authoritative pure-fire state used by the client HUD, talent screen and pose. */
public record PureFireStatusPacket(boolean permission, boolean meditating,
                                   boolean casting, boolean releasing, boolean handForm,
                                   int proficiency, int selectedSlot, int selectedScheme,
                                   boolean bodyProtection, int meditationProgress,
                                   int talentPoints, int meditationLayers,
                                   long meditationCooldownRemaining, int unlockedMask,
                                   int[] slotSkills) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(permission);
        buf.writeBoolean(meditating);
        buf.writeBoolean(casting);
        buf.writeBoolean(releasing);
        buf.writeBoolean(handForm);
        buf.writeVarInt(proficiency);
        buf.writeVarInt(selectedSlot);
        buf.writeVarInt(selectedScheme);
        buf.writeBoolean(bodyProtection);
        buf.writeVarInt(meditationProgress);
        buf.writeVarInt(talentPoints);
        buf.writeVarInt(meditationLayers);
        buf.writeVarLong(meditationCooldownRemaining);
        buf.writeVarInt(unlockedMask);
        int len = slotSkills != null ? slotSkills.length : 8;
        buf.writeVarInt(len);
        for (int i = 0; i < len; i++) {
            buf.writeVarInt(slotSkills != null && i < slotSkills.length ? slotSkills[i] : 0);
        }
    }

    public static PureFireStatusPacket decode(FriendlyByteBuf buf) {
        boolean perm = buf.readBoolean();
        boolean med = buf.readBoolean();
        boolean cast = buf.readBoolean();
        boolean rel = buf.readBoolean();
        boolean hand = buf.readBoolean();
        int prof = buf.readVarInt();
        int slot = buf.readVarInt();
        int scheme = buf.readVarInt();
        boolean body = buf.readBoolean();
        int medProg = buf.readVarInt();
        int talent = buf.readVarInt();
        int layers = buf.readVarInt();
        long cd = buf.readVarLong();
        int mask = buf.readVarInt();
        int len = buf.readVarInt();
        int[] skills = new int[len];
        for (int i = 0; i < len; i++) {
            skills[i] = buf.readVarInt();
        }
        return new PureFireStatusPacket(perm, med, cast, rel, hand, prof, slot, scheme,
                body, medProg, talent, layers, cd, mask, skills);
    }

    public static void handle(PureFireStatusPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientEvents.setPureFireStatus(msg.permission(), msg.meditating(),
                        msg.casting(), msg.releasing(), msg.handForm(), msg.proficiency(), msg.selectedSlot(),
                        msg.selectedScheme(), msg.bodyProtection(), msg.meditationProgress(),
                        msg.talentPoints(), msg.meditationLayers(), msg.meditationCooldownRemaining(),
                        msg.unlockedMask(), msg.slotSkills())));
        context.setPacketHandled(true);
    }
}

