package com.mjmod.network;

import com.mjmod.skill.VoidTearsSkillManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** 虚空之泪 R/Z 独立输入包：action 0=切换，1=释放；shift 用于二技能防御扩展。 */
public record VoidTearsSkillPacket(int action, int skill, boolean shift) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(action);
        buf.writeInt(skill);
        buf.writeBoolean(shift);
    }
    public static VoidTearsSkillPacket decode(FriendlyByteBuf buf) {
        return new VoidTearsSkillPacket(buf.readInt(), buf.readInt(), buf.readBoolean());
    }
    public static void handle(VoidTearsSkillPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (msg.action() == 0) VoidTearsSkillManager.switchSkill(player, msg.skill());
            else if (msg.action() == 1) {
                VoidTearsSkillManager.cast(player, msg.skill(), msg.shift());
            }
        });
        context.setPacketHandled(true);
    }
}
