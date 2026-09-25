package io.github.lyne324.mapscript.network;

import io.github.lyne324.mapscript.MapScript;
import io.github.lyne324.mapscript.MapState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class MapStatePacket {
    public final List<MapState.Icon> icons;
    public final List<MapState.Shape> shapes;
    public final List<MapState.EntityRule> entityRules;
    public final String rotationMode;
    public final float fixedRotation;
    public final double offsetX;
    public final double offsetZ;

    private MapStatePacket(List<MapState.Icon> icons, List<MapState.Shape> shapes, List<MapState.EntityRule> entityRules,
                           String rotationMode,
                           float fixedRotation, double offsetX, double offsetZ) {
        this.icons = icons;
        this.shapes = shapes;
        this.entityRules = entityRules;
        this.rotationMode = rotationMode;
        this.fixedRotation = fixedRotation;
        this.offsetX = offsetX;
        this.offsetZ = offsetZ;
    }

    public static MapStatePacket from(MapState state, net.minecraft.server.level.ServerPlayer player) {
        return new MapStatePacket(
                state.icons.values().stream().filter(icon -> state.visibleTo(icon, player)).toList(),
                state.shapes.values().stream().filter(shape -> state.scopeMatches(shape.visibleTo(), player)).toList(),
                List.copyOf(state.entityRules.values()),
                state.rotationMode, state.fixedRotation, state.offsetX, state.offsetZ
        );
    }

    public static void encode(MapStatePacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.icons.size());
        for (MapState.Icon icon : packet.icons) {
            buf.writeUtf(icon.id());
            buf.writeDouble(icon.x());
            buf.writeDouble(icon.z());
            buf.writeUtf(icon.iconKey());
            buf.writeFloat(icon.scale());
            buf.writeFloat(icon.rotation());
            buf.writeUtf(icon.visibleTo() == null ? "" : icon.visibleTo());
        }
        buf.writeVarInt(packet.shapes.size());
        for (MapState.Shape shape : packet.shapes) {
            buf.writeUtf(shape.id());
            buf.writeUtf(shape.type());
            buf.writeVarInt(shape.points().size());
            for (MapState.Point point : shape.points()) {
                buf.writeDouble(point.x());
                buf.writeDouble(point.z());
            }
            buf.writeInt(shape.color());
            buf.writeFloat(shape.fillAlpha());
            buf.writeFloat(shape.lineWidth());
            buf.writeUtf(shape.visibleTo() == null ? "" : shape.visibleTo());
        }
        buf.writeVarInt(packet.entityRules.size());
        for (MapState.EntityRule rule : packet.entityRules) {
            buf.writeUtf(rule.target());
            buf.writeBoolean(rule.visible());
            buf.writeUtf(rule.overrideIconKey() == null ? "" : rule.overrideIconKey());
            buf.writeUtf(rule.visibleTo() == null ? "" : rule.visibleTo());
        }
        buf.writeUtf(packet.rotationMode);
        buf.writeFloat(packet.fixedRotation);
        buf.writeDouble(packet.offsetX);
        buf.writeDouble(packet.offsetZ);
    }

    public static MapStatePacket decode(FriendlyByteBuf buf) {
        List<MapState.Icon> icons = new ArrayList<>();
        for (int i = 0; i < buf.readVarInt(); i++) {
            icons.add(new MapState.Icon(buf.readUtf(), buf.readDouble(), buf.readDouble(), buf.readUtf(),
                    buf.readFloat(), buf.readFloat(), buf.readUtf()));
        }
        List<MapState.Shape> shapes = new ArrayList<>();
        for (int i = 0; i < buf.readVarInt(); i++) {
            String id = buf.readUtf();
            String type = buf.readUtf();
            List<MapState.Point> points = new ArrayList<>();
            for (int j = 0; j < buf.readVarInt(); j++) {
                points.add(new MapState.Point(buf.readDouble(), buf.readDouble()));
            }
            shapes.add(new MapState.Shape(id, type, points, buf.readInt(), buf.readFloat(), buf.readFloat(), buf.readUtf()));
        }
        List<MapState.EntityRule> entityRules = new ArrayList<>();
        for (int i = 0; i < buf.readVarInt(); i++) {
            entityRules.add(new MapState.EntityRule(buf.readUtf(), buf.readBoolean(), buf.readUtf(), buf.readUtf()));
        }
        return new MapStatePacket(icons, shapes, entityRules, buf.readUtf(), buf.readFloat(), buf.readDouble(), buf.readDouble());
    }

    public static void handle(MapStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientState.apply(packet));
        context.setPacketHandled(true);
    }
}
