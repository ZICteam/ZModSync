package com.modsync;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class ModSyncCommands {
    private ModSyncCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("modsync")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("reload")
                        .executes(context -> reload(context.getSource())))
                .then(Commands.literal("status")
                        .executes(context -> status(context.getSource())))
                .then(Commands.literal("manifest")
                        .then(Commands.literal("refresh")
                                .executes(context -> refreshManifest(context.getSource()))))
                .then(Commands.literal("handshake")
                        .then(Commands.literal("allow")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .executes(context -> allowHandshake(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "player")
                                        ))))
                        .then(Commands.literal("kick")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .executes(context -> kickHandshake(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "player")
                                        )))))
                .then(Commands.literal("config")
                        .then(Commands.literal("set")
                                .then(Commands.literal("http_port")
                                        .then(Commands.argument("port", IntegerArgumentType.integer(1, 65535))
                                                .executes(context -> setHttpPort(
                                                        context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "port")
                                                ))))
                                .then(Commands.literal("public_http_base_url")
                                        .then(Commands.argument("url", StringArgumentType.greedyString())
                                                .executes(context -> setPublicHttpBaseUrl(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "url")
                                                ))))
                                .then(Commands.literal("handshake_timeout_seconds")
                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(5, 600))
                                                .executes(context -> setHandshakeTimeoutSeconds(
                                                        context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "seconds")
                                                ))))
                                .then(Commands.literal("auto_kick_on_handshake_timeout")
                                        .then(Commands.argument("enabled", StringArgumentType.word())
                                                .executes(context -> setAutoKickOnHandshakeTimeout(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "enabled")
                                                )))))));
    }

    private static int reload(CommandSourceStack source) {
        ModSync.reloadServerRuntime("command reload");
        source.sendSuccess(() -> Component.literal("ModSync reloaded."), true);
        return 1;
    }

    private static int refreshManifest(CommandSourceStack source) {
        ModSync.reloadServerRuntime("command manifest refresh");
        source.sendSuccess(() -> Component.literal("ModSync manifest refreshed."), true);
        return 1;
    }

    private static int setHttpPort(CommandSourceStack source, int port) {
        try {
            ConfigManager.setServerHttpPortAndSave(port);
            ModSync.reloadServerRuntime("command set http_port");
            source.sendSuccess(() -> Component.literal("ModSync server_http_port set to " + port + "."), true);
            return 1;
        } catch (Exception exception) {
            source.sendFailure(Component.literal("Failed to set ModSync server_http_port: " + exception.getMessage()));
            return 0;
        }
    }

    private static int setPublicHttpBaseUrl(CommandSourceStack source, String url) {
        try {
            ConfigManager.setPublicHttpBaseUrlAndSave(url);
            ModSync.reloadServerRuntime("command set public_http_base_url");
            source.sendSuccess(() -> Component.literal("ModSync public_http_base_url set to " + ConfigManager.publicHttpBaseUrl() + "."), true);
            return 1;
        } catch (Exception exception) {
            source.sendFailure(Component.literal("Failed to set ModSync public_http_base_url: " + exception.getMessage()));
            return 0;
        }
    }

    private static int status(CommandSourceStack source) {
        ManifestData manifest = ModSync.getLastManifest();
        int manifestEntries = manifest == null ? 0 : manifest.getEntries().size();
        long generatedAt = manifest == null ? 0L : manifest.getGeneratedAt();

        source.sendSuccess(() -> Component.literal("ModSync status:"), false);
        source.sendSuccess(() -> Component.literal("  http_enabled = " + ConfigManager.enableHttpServer()), false);
        source.sendSuccess(() -> Component.literal("  http_bind = " + ConfigManager.serverHttpBind()), false);
        source.sendSuccess(() -> Component.literal("  http_port = " + ConfigManager.serverHttpPort()), false);
        source.sendSuccess(() -> Component.literal("  public_http_base_url = " + (ConfigManager.publicHttpBaseUrl().isBlank() ? "<empty>" : ConfigManager.publicHttpBaseUrl())), false);
        source.sendSuccess(() -> Component.literal("  handshake_timeout_seconds = " + ConfigManager.handshakeTimeoutSeconds()), false);
        source.sendSuccess(() -> Component.literal("  auto_kick_on_handshake_timeout = " + ConfigManager.autoKickOnHandshakeTimeout()), false);
        source.sendSuccess(() -> Component.literal("  manifest_entries = " + manifestEntries), false);
        source.sendSuccess(() -> Component.literal("  manifest_generated_at = " + generatedAt), false);
        return 1;
    }

    private static int allowHandshake(CommandSourceStack source, String playerName) {
        boolean allowed = NetworkHandler.allowTimedOutHandshake(source.getServer(), playerName);
        if (!allowed) {
            source.sendFailure(Component.literal("No timed-out ModSync handshake is waiting for admin decision for player " + playerName + "."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Allowed player " + playerName + " to stay after ModSync handshake timeout."), true);
        return 1;
    }

    private static int kickHandshake(CommandSourceStack source, String playerName) {
        boolean kicked = NetworkHandler.kickTimedOutHandshake(source.getServer(), playerName);
        if (!kicked) {
            source.sendFailure(Component.literal("No timed-out ModSync handshake is waiting for admin decision for player " + playerName + "."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Kicked player " + playerName + " after ModSync handshake timeout."), true);
        return 1;
    }

    private static int setHandshakeTimeoutSeconds(CommandSourceStack source, int seconds) {
        try {
            ConfigManager.setHandshakeTimeoutSecondsAndSave(seconds);
            ModSync.reloadServerRuntime("command set handshake_timeout_seconds");
            source.sendSuccess(() -> Component.literal("ModSync handshake_timeout_seconds set to " + seconds + "."), true);
            return 1;
        } catch (Exception exception) {
            source.sendFailure(Component.literal("Failed to set ModSync handshake_timeout_seconds: " + exception.getMessage()));
            return 0;
        }
    }

    private static int setAutoKickOnHandshakeTimeout(CommandSourceStack source, String enabledRaw) {
        boolean enabled;
        if ("true".equalsIgnoreCase(enabledRaw) || "false".equalsIgnoreCase(enabledRaw)) {
            enabled = Boolean.parseBoolean(enabledRaw);
        } else {
            source.sendFailure(Component.literal("Value must be true or false."));
            return 0;
        }

        try {
            ConfigManager.setAutoKickOnHandshakeTimeoutAndSave(enabled);
            ModSync.reloadServerRuntime("command set auto_kick_on_handshake_timeout");
            source.sendSuccess(() -> Component.literal("ModSync auto_kick_on_handshake_timeout set to " + enabled + "."), true);
            return 1;
        } catch (Exception exception) {
            source.sendFailure(Component.literal("Failed to set ModSync auto_kick_on_handshake_timeout: " + exception.getMessage()));
            return 0;
        }
    }
}
