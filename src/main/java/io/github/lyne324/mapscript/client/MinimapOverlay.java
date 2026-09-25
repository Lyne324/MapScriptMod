package io.github.lyne324.mapscript.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.lyne324.mapscript.ClientSetup;
import io.github.lyne324.mapscript.MapState;
import io.github.lyne324.mapscript.network.ClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

public final class MinimapOverlay {
    private static final int SIZE = 160;
    private static final int MARGIN = 8;
    private static final int BACKGROUND = 0xB0101010;

    private MinimapOverlay() {}

    public static void render(GuiGraphics graphics) {
        if (!ClientSetup.visible) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return;
        }
        int left = graphics.guiWidth() - SIZE - MARGIN;
        int top = MARGIN;
        graphics.fill(left, top, left + SIZE, top + SIZE, BACKGROUND);
        drawGrid(graphics, left, top);

        double centerX = player.getX() + ClientState.offsetX;
        double centerZ = player.getZ() + ClientState.offsetZ;
        float rotation = ClientState.rotationMode.equals("player_facing")
                ? player.getYRot() : ClientState.fixedRotation;

        for (MapState.Shape shape : ClientState.shapes) {
            drawShape(graphics, shape, left, top, centerX, centerZ, rotation);
        }
        for (MapState.Icon icon : ClientState.icons) {
            int[] position = mapPosition(icon.x(), icon.z(), left, top, centerX, centerZ, rotation);
            int iconSize = Math.max(3, Math.round(5 * icon.scale()));
            int color = iconColor(icon.iconKey());
            graphics.fill(position[0] - iconSize, position[1] - iconSize,
                    position[0] + iconSize + 1, position[1] + iconSize + 1, color);
        }
        for (Player other : minecraft.level.players()) {
            if (other == player || !isVisible(other)) {
                continue;
            }
            int[] position = mapPosition(other.getX(), other.getZ(), left, top, centerX, centerZ, rotation);
            graphics.fill(position[0] - 3, position[1] - 3, position[0] + 4, position[1] + 4,
                    iconColor(iconFor(other)));
        }
        int[] playerPosition = mapPosition(player.getX(), player.getZ(), left, top, centerX, centerZ, rotation);
        graphics.fill(playerPosition[0] - 3, playerPosition[1] - 3,
                playerPosition[0] + 4, playerPosition[1] + 4, 0xFF55FF55);
        graphics.drawString(minecraft.font, "MapScript", left + 5, top + 5, 0xFFFFFFFF, true);
    }

    private static void drawGrid(GuiGraphics graphics, int left, int top) {
        for (int i = 1; i < 8; i++) {
            int x = left + i * SIZE / 8;
            int y = top + i * SIZE / 8;
            graphics.fill(x, top, x + 1, top + SIZE, 0x303F3F3F);
            graphics.fill(left, y, left + SIZE, y + 1, 0x303F3F3F);
        }
        graphics.fill(left, top, left + SIZE, top + 1, 0xFF808080);
        graphics.fill(left, top + SIZE - 1, left + SIZE, top + SIZE, 0xFF808080);
        graphics.fill(left, top, left + 1, top + SIZE, 0xFF808080);
        graphics.fill(left + SIZE - 1, top, left + SIZE, top + SIZE, 0xFF808080);
    }

    private static void drawShape(GuiGraphics graphics, MapState.Shape shape, int left, int top,
                                  double centerX, double centerZ, float rotation) {
        for (int i = 1; i < shape.points().size(); i++) {
            int[] a = mapPosition(shape.points().get(i - 1).x(), shape.points().get(i - 1).z(),
                    left, top, centerX, centerZ, rotation);
            int[] b = mapPosition(shape.points().get(i).x(), shape.points().get(i).z(),
                    left, top, centerX, centerZ, rotation);
            drawLine(graphics, a[0], a[1], b[0], b[1], 0xFF000000 | shape.color());
        }
    }

    private static void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps == 0) {
            graphics.fill(x1, y1, x1 + 1, y1 + 1, color);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / steps;
            int y = y1 + (y2 - y1) * i / steps;
            graphics.fill(x, y, x + 2, y + 2, color);
        }
    }

    private static int[] mapPosition(double x, double z, int left, int top,
                                      double centerX, double centerZ, float rotation) {
        double scale = 4.0;
        double dx = (x - centerX) / scale;
        double dz = (z - centerZ) / scale;
        double radians = Math.toRadians(rotation);
        double rx = dx * Math.cos(radians) - dz * Math.sin(radians);
        double rz = dx * Math.sin(radians) + dz * Math.cos(radians);
        return new int[]{(int) (left + SIZE / 2.0 + rx), (int) (top + SIZE / 2.0 + rz)};
    }

    private static int iconColor(String key) {
        return switch (key.toLowerCase()) {
            case "spike", "bomb" -> 0xFFFF5555;
            case "ally", "friend" -> 0xFF55AAFF;
            case "enemy" -> 0xFFFF5555;
            default -> 0xFFFFFF55;
        };
    }

    private static boolean isVisible(Player player) {
        MapState.EntityRule rule = ruleFor(player);
        return rule == null || rule.visible();
    }

    private static String iconFor(Player player) {
        MapState.EntityRule rule = ruleFor(player);
        return rule == null || rule.overrideIconKey().isBlank() ? "ally" : rule.overrideIconKey();
    }

    private static MapState.EntityRule ruleFor(Player player) {
        for (MapState.EntityRule rule : ClientState.entityRules) {
            if (rule.target().equals(player.getUUID().toString())
                    || rule.target().equals(player.getGameProfile().getName())) {
                return rule;
            }
            if (rule.target().startsWith("team:") && player.getTeam() != null
                    && rule.target().substring("team:".length()).equals(player.getTeam().getName())) {
                return rule;
            }
        }
        return null;
    }
}
