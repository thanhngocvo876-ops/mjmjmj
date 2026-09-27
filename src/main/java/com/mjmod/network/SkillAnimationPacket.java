package com.mjmod.network;

import com.mjmod.client.ClientEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端统一通知客户端播放技能施法动画。 */
public record SkillAnimationPacket(int skillId, int durationTicks) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(skillId);
        buf.writeVarInt(durationTicks);
    }

    public static SkillAnimationPacket decode(FriendlyByteBuf buf) {
        return new SkillAnimationPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(SkillAnimationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientEvents.showSkillAnimation(msg.skillId(), msg.durationTicks())));
        context.setPacketHandled(true);
    }
}
