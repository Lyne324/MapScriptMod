package io.github.lyne324.mapscript;

import io.github.lyne324.mapscript.network.NetworkHandler;
import io.github.lyne324.mapscript.network.MapStatePacket;
import io.github.lyne324.mapscript.script.ScriptCommand;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.level.ServerPlayer;

@Mod(MapScript.MOD_ID)
public final class MapScript {
    public static final String MOD_ID = "mapscript";
    public static final MapState STATE = new MapState();

    public MapScript() {
        NetworkHandler.init();
        MinecraftForge.EVENT_BUS.addListener(MapScript::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(MapScript::serverTick);
        MinecraftForge.EVENT_BUS.addListener(MapScript::playerLoggedIn);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientSetup::init);
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        ScriptCommand.register(event.getDispatcher());
    }

    private static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.getServer().getTickCount() % 2 == 0) {
            STATE.runCallbacks(event.getServer());
            STATE.sync(event.getServer());
        }

        private static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                NetworkHandler.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        MapStatePacket.from(STATE, player)
                );
            }
        }
    }
}
