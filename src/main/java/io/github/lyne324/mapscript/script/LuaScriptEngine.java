package io.github.lyne324.mapscript.script;

import io.github.lyne324.mapscript.MapScript;
import io.github.lyne324.mapscript.MapState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class LuaScriptEngine {
    private LuaScriptEngine() {}

    public static void run(MinecraftServer server, String scriptId, List<String> args) throws IOException {
        ResourceLocation location = scriptId.contains(":")
                ? ResourceLocation.tryParse(scriptId)
                : new ResourceLocation(MapScript.MOD_ID, scriptId);
        if (location == null) {
            throw new IOException("Invalid script id: " + scriptId);
        }
        var resource = server.getResourceManager().getResource(
                new ResourceLocation(location.getNamespace(), "minimap_scripts/" + location.getPath() + ".lua")
        ).orElseThrow(() -> new IOException("Script not found: " + location));

        Globals globals = JsePlatform.standardGlobals();
        globals.set("io", LuaValue.NIL);
        globals.set("os", LuaValue.NIL);
        globals.set("debug", LuaValue.NIL);
        globals.set("package", LuaValue.NIL);
        globals.set("require", LuaValue.NIL);
        globals.set("dofile", LuaValue.NIL);
        globals.set("loadfile", LuaValue.NIL);
        globals.set("Minimap", api(server));
        LuaValue chunk;
        try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
            chunk = globals.load(reader, location.toString());
            chunk.call();
        } catch (LuaError error) {
            throw new IOException("Lua error in " + location + ": " + error.getMessage(), error);
        }
    }

    private static LuaTable api(MinecraftServer server) {
        LuaTable api = new LuaTable();
        api.set("setIcon", new SetIconFunction());
        api.set("removeIcon", new RemoveIconFunction());
        api.set("drawShape", new DrawShapeFunction());
        api.set("removeShape", new RemoveShapeFunction());
        api.set("setRotationMode", new SetRotationFunction());
        api.set("setOffset", new SetOffsetFunction());
        api.set("setEntityVisibility", new SetEntityVisibilityFunction(server));
        api.set("onTick", new OnTickFunction(server));
        return api;
    }

    private static String scope(LuaValue opts) {
        return opts.istable() ? opts.get("visibleTo").optjstring("") : "";
    }

    private static final class SetIconFunction extends VarArgFunction {
        @Override public Varargs invoke(Varargs args) {
            String id = args.arg(1).checkjstring();
            double x = args.arg(2).checkdouble();
            double z = args.arg(3).checkdouble();
            String key = args.arg(4).checkjstring();
            LuaValue opts = args.arg(5);
            float scale = (float) (opts.istable() ? opts.get("scale").optdouble(1.0) : 1.0);
            float rotation = (float) (opts.istable() ? opts.get("rotation").optdouble(0.0) : 0.0);
            MapScript.STATE.icons.put(id, new MapState.Icon(id, x, z, key, scale, rotation, scope(opts)));
            MapScript.STATE.markDirty();
            return LuaValue.NIL;
        }
    }

    private static final class RemoveIconFunction extends VarArgFunction {
        @Override public Varargs invoke(Varargs args) {
            MapScript.STATE.icons.remove(args.arg(1).checkjstring());
            MapScript.STATE.markDirty();
            return LuaValue.NIL;
        }
    }

    private static final class DrawShapeFunction extends VarArgFunction {
        @Override public Varargs invoke(Varargs args) {
            String id = args.arg(1).checkjstring();
            String type = args.arg(2).checkjstring();
            LuaValue pointsValue = args.arg(3).checktable();
            LuaValue style = args.arg(4);
            List<MapState.Point> points = new ArrayList<>();
            for (int i = 1; !pointsValue.get(i).isnil(); i++) {
                LuaValue point = pointsValue.get(i).checktable();
                points.add(new MapState.Point(point.get("x").checkdouble(), point.get("z").checkdouble()));
            }
            int color = parseColor(style.istable() ? style.get("color").optjstring("#FFFFFF") : "#FFFFFF");
            float alpha = (float) (style.istable() ? style.get("fillAlpha").optdouble(0.3) : 0.3);
            float width = (float) (style.istable() ? style.get("lineWidth").optdouble(2.0) : 2.0);
            MapScript.STATE.shapes.put(id, new MapState.Shape(id, type, points, color, alpha, width, scope(style)));
            MapScript.STATE.markDirty();
            return LuaValue.NIL;
        }

        private static int parseColor(String value) {
            String hex = value.startsWith("#") ? value.substring(1) : value;
            try {
                return Integer.parseInt(hex, 16) & 0xFFFFFF;
            } catch (NumberFormatException ignored) {
                return 0xFFFFFF;
            }
        }
    }

    private static final class RemoveShapeFunction extends VarArgFunction {
        @Override public Varargs invoke(Varargs args) {
            MapScript.STATE.shapes.remove(args.arg(1).checkjstring());
            MapScript.STATE.markDirty();
            return LuaValue.NIL;
        }
    }

    private static final class SetRotationFunction extends VarArgFunction {
        @Override public Varargs invoke(Varargs args) {
            String mode = args.arg(2).checkjstring().toLowerCase(Locale.ROOT);
            MapScript.STATE.rotationMode = mode;
            if (args.narg() >= 3 && args.arg(3).isnumber()) {
                MapScript.STATE.fixedRotation = (float) args.arg(3).checkdouble();
            }
            MapScript.STATE.markDirty();
            return LuaValue.NIL;
        }
    }

    private static final class SetOffsetFunction extends VarArgFunction {
        @Override public Varargs invoke(Varargs args) {
            MapScript.STATE.offsetX = args.arg(2).checkdouble();
            MapScript.STATE.offsetZ = args.arg(3).checkdouble();
            MapScript.STATE.markDirty();
            return LuaValue.NIL;
        }
    }

    private static final class SetEntityVisibilityFunction extends VarArgFunction {
        private final MinecraftServer server;
        private SetEntityVisibilityFunction(MinecraftServer server) { this.server = server; }

        @Override public Varargs invoke(Varargs args) {
            String target = args.arg(1).checkjstring();
            boolean visible = args.arg(2).checkboolean();
            String icon = args.narg() >= 3 && !args.arg(3).isnil() ? args.arg(3).checkjstring() : "";
            MapScript.STATE.entityRules.put(target, new MapState.EntityRule(target, visible, icon, ""));
            MapScript.STATE.markDirty();
            return LuaValue.NIL;
        }
    }

    private static final class OnTickFunction extends VarArgFunction {
        private final MinecraftServer server;
        private OnTickFunction(MinecraftServer server) { this.server = server; }

        @Override public Varargs invoke(Varargs args) {
            LuaValue callback = args.arg(1).checkfunction();
            MapScript.STATE.addTickCallback(ignored -> callback.call());
            return LuaValue.NIL;
        }
    }
}
