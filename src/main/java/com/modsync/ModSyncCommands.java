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
        source.sendSuccess(() -> Component.literal("  manifest_entries = " + manifestEntries), false);
        source.sendSuccess(() -> Component.literal("  manifest_generated_at = " + generatedAt), false);
        source.sendSuccess(() -> Component.literal("  refresh_initial_delay_minutes = " + ModSync.getManifestRefreshInitialDelayMinutes()), false);
        source.sendSuccess(() -> Component.literal("  refresh_period_minutes = " + ModSync.getManifestRefreshPeriodMinutes()), false);
        return 1;
    }
}
