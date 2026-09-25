package io.github.lyne324.mapscript.network;

import io.github.lyne324.mapscript.MapScript;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class NetworkHandler {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MapScript.MOD_ID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals
    );

    private NetworkHandler() {}

    public static void init() {
        CHANNEL.registerMessage(0, MapStatePacket.class, MapStatePacket::encode, MapStatePacket::decode,
                MapStatePacket::handle);
    }
}
