package io.github.lyne324.mapscript.network;

import io.github.lyne324.mapscript.MapState;

import java.util.List;

public final class ClientState {
    public static List<MapState.Icon> icons = List.of();
    public static List<MapState.Shape> shapes = List.of();
    public static List<MapState.EntityRule> entityRules = List.of();
    public static String rotationMode = "north_up";
    public static float fixedRotation;
    public static double offsetX;
    public static double offsetZ;

    private ClientState() {}

    public static void apply(MapStatePacket packet) {
        icons = packet.icons;
        shapes = packet.shapes;
        entityRules = packet.entityRules;
        rotationMode = packet.rotationMode;
        fixedRotation = packet.fixedRotation;
        offsetX = packet.offsetX;
        offsetZ = packet.offsetZ;
    }
}
