package io.github.lyne324.mapscript;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.lyne324.mapscript.client.MinimapOverlay;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

public final class ClientSetup {
    public static KeyMapping toggleKey;
    public static boolean visible = true;

    private ClientSetup() {}

    public static void init() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientSetup::registerKeyMappings);
        MinecraftForge.EVENT_BUS.addListener(ClientSetup::clientTick);
        MinecraftForge.EVENT_BUS.addListener((RenderGuiOverlayEvent.Post event) -> {
            if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type()) {
                MinimapOverlay.render(event.getGuiGraphics());
            }
        });
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        toggleKey = new KeyMapping("key.mapscript.toggle", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M, "key.categories.mapscript");
        event.register(toggleKey);
    }

    private static void clientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END && toggleKey != null) {
            while (toggleKey.consumeClick()) {
                visible = !visible;
            }
        }
    }
}
