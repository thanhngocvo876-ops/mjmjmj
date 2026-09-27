package com.mjmod.network;

import com.mjmod.skill.PureFireSkillManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Pure-fire input: 0=switch one of eight slots, 1=activate selected skill,
 * 2=stop skill 1, 3=meditate toggle, 4=orb pulse, 5=cycle body form,
 * 6=begin body-form selection, 7=confirm on Shift release,
 * 8=unlock talent node (slot=skillId), 9=assign skill to slot (slot=slotIndex, reverse=(boolean/aux param or data passed via slot/reverse)). */
public record PureFireSkillPacket(int action, int slot, boolean reverse) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(action);
        buf.writeVarInt(slot);
        buf.writeBoolean(reverse);
    }

    public static PureFireSkillPacket decode(FriendlyByteBuf buf) {
        return new PureFireSkillPacket(buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(PureFireSkillPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            switch (msg.action()) {
                case 0 -> PureFireSkillManager.switchSlot(player, msg.slot(), msg.reverse());
                case 1 -> PureFireSkillManager.activateSelectedSkill(player, msg.reverse());
                case 2 -> {
                    PureFireSkillManager.stopCasting(player);
                    PureFireSkillManager.stopFireBallCasting(player);
                }
                case 3 -> PureFireSkillManager.toggleMeditation(player);
                case 4 -> {} // 原光球小游戏已废弃
                case 5 -> PureFireSkillManager.cycleScheme(player, msg.reverse());
                case 6 -> PureFireSkillManager.beginPureYangFormSelection(player);
                case 7 -> PureFireSkillManager.confirmPureYangFormSelection(player);
                case 8 -> PureFireSkillManager.buyTalentSkill(player, msg.slot());
                case 9 -> PureFireSkillManager.assignSkillToSlot(player, msg.slot(), msg.reverse() ? 1 : 0);
            }
        });
        context.setPacketHandled(true);
    }
}
