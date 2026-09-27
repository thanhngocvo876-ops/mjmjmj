package com.mjmod.network;

import com.mjmod.client.ClientEvents;
import com.mjmod.skill.VoidTearsSkillManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Server → client snapshot of saved cross-dimensional teleport points. */
public record VoidTearsPointsDataPacket(List<VoidTearsSkillManager.CoordinatePoint> points,
                                        String selectedUid) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(points.size());
        for (VoidTearsSkillManager.CoordinatePoint point : points) {
            buf.writeUtf(point.uid(), 128);
            buf.writeUtf(point.dimension(), 256);
            buf.writeDouble(point.x());
            buf.writeDouble(point.y());
            buf.writeDouble(point.z());
        }
        buf.writeUtf(selectedUid == null ? "" : selectedUid, 128);
    }

    public static VoidTearsPointsDataPacket decode(FriendlyByteBuf buf) {
        int count = Math.max(0, Math.min(buf.readVarInt(), 64));
        List<VoidTearsSkillManager.CoordinatePoint> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            points.add(new VoidTearsSkillManager.CoordinatePoint(
                    buf.readUtf(128), buf.readUtf(256), buf.readDouble(),
                    buf.readDouble(), buf.readDouble()));
        }
        return new VoidTearsPointsDataPacket(points, buf.readUtf(128));
    }

    public static void handle(VoidTearsPointsDataPacket msg,
                              Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientEvents.setVoidTearsPoints(msg.points(), msg.selectedUid())));
        context.setPacketHandled(true);
    }
}
