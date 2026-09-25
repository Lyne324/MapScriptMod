package io.github.lyne324.mapscript.script;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Arrays;

public final class ScriptCommand {
    private ScriptCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("minimap")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("script")
                        .then(Commands.literal("run")
                                .then(Commands.argument("script_id", StringArgumentType.word())
                                        .executes(context -> run(context.getSource(),
                                                StringArgumentType.getString(context, "script_id"), new String[0]))
                                        .then(Commands.argument("args", StringArgumentType.greedyString())
                                                .executes(context -> run(context.getSource(),
                                                        StringArgumentType.getString(context, "script_id"),
                                                        StringArgumentType.getString(context, "args").split("\\s+"))))))));
    }

    private static int run(CommandSourceStack source, String scriptId, String[] args) {
        try {
            LuaScriptEngine.run(source.getServer(), scriptId, Arrays.asList(args));
            source.sendSuccess(() -> Component.literal("MapScript executed: " + scriptId), true);
            return 1;
        } catch (Exception error) {
            source.sendFailure(Component.literal("MapScript failed: " + error.getMessage()));
            return 0;
        }
    }
}
