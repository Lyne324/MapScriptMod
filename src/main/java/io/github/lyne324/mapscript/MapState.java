package io.github.lyne324.mapscript;

import io.github.lyne324.mapscript.network.MapStatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class MapState {
    public final Map<String, Icon> icons = new LinkedHashMap<>();
    public final Map<String, Shape> shapes = new LinkedHashMap<>();
    public final Map<String, EntityRule> entityRules = new LinkedHashMap<>();
    private final List<Consumer<MinecraftServer>> tickCallbacks = new ArrayList<>();
    public String rotationMode = "north_up";
    public float fixedRotation;
    public double offsetX;
    public double offsetZ;
    private boolean dirty = true;

    public void markDirty() {
        dirty = true;
    }

    public void addTickCallback(Consumer<MinecraftServer> callback) {
        tickCallbacks.add(callback);
    }

    public void runCallbacks(MinecraftServer server) {
        for (Consumer<MinecraftServer> callback : List.copyOf(tickCallbacks)) {
            callback.accept(server);
        }
    }

    public void sync(MinecraftServer server) {
        if (!dirty) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            NetworkHandler.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    MapStatePacket.from(this, player)
            );
        }
        dirty = false;
    }

    public boolean visibleTo(Icon icon, ServerPlayer player) {
        return scopeMatches(icon.visibleTo, player);
    }

    public boolean scopeMatches(String scope, ServerPlayer player) {
        if (scope == null || scope.isBlank() || scope.equals("all")) {
            return true;
        }
        if (scope.startsWith("player:")) {
            return scope.substring("player:".length()).equals(player.getUUID().toString())
                    || scope.substring("player:".length()).equals(player.getGameProfile().getName());
        }
        if (scope.startsWith("team:")) {
            PlayerTeam team = player.getScoreboard().getPlayersTeam(player.getGameProfile().getName());
            return team != null && team.getName().equals(scope.substring("team:".length()));
        }
        return false;
    }

    public record Icon(String id, double x, double z, String iconKey, float scale, float rotation, String visibleTo) {}
    public record Shape(String id, String type, List<Point> points, int color, float fillAlpha, float lineWidth, String visibleTo) {}
    public record Point(double x, double z) {}
    public record EntityRule(String target, boolean visible, String overrideIconKey, String visibleTo) {}
}
