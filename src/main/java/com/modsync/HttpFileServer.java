package com.modsync;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public final class HttpFileServer {
    private static final HttpFileServer INSTANCE = new HttpFileServer();

    private final Map<String, ManifestEntry> approvedEntries = new ConcurrentHashMap<>();
    private HttpServer server;

    private HttpFileServer() {
    }

    public static HttpFileServer getInstance() {
        return INSTANCE;
    }

    public synchronized void start() {
        if (!ConfigManager.enableHttpServer() || server != null) {
            return;
        }

        try {
            server = HttpServer.create(new InetSocketAddress(ConfigManager.serverHttpBind(), ConfigManager.serverHttpPort()), 0);
            server.createContext("/manifest", new ManifestHandler());
            server.createContext("/files", new FileHandler());
            server.setExecutor(Executors.newFixedThreadPool(Math.max(2, ConfigManager.downloadThreads())));
            server.start();
            LoggerUtils.info("HTTP file server started on " + ConfigManager.serverHttpBind() + ":" + ConfigManager.serverHttpPort());
        } catch (IOException exception) {
            LoggerUtils.error("Failed to start HTTP file server", exception);
        }
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            LoggerUtils.info("HTTP file server stopped");
        }
    }

    public void updateManifest(ManifestData data) {
        approvedEntries.clear();
        for (ManifestEntry entry : data.getEntries()) {
            approvedEntries.put(entry.getIdentityKey(), entry.copy());
        }
    }

    private static void respond(HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }

    static ManifestEntry resolveApprovedEntry(String rawPath, Map<String, ManifestEntry> approvedEntries) {
        String prefix = "/files/";
        if (rawPath == null || !rawPath.startsWith(prefix)) {
            return null;
        }

        String subPath = rawPath.substring(prefix.length());
        int slashIndex = subPath.indexOf('/');
        if (slashIndex <= 0) {
            throw new IllegalArgumentException("Malformed file request path: " + rawPath);
        }

        CategoryType category = CategoryType.fromHttpSegment(subPath.substring(0, slashIndex));
        String relativePath = HttpPathCodec.decodeRelativePath(subPath.substring(slashIndex + 1));
        return approvedEntries.get(category.name() + ":" + relativePath);
    }

    private final class FileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                respond(exchange, 405);
                return;
            }

            String path = exchange.getRequestURI().getRawPath();
            if (path == null || !path.startsWith("/files/")) {
                respond(exchange, 404);
                return;
            }

            try {
                ManifestEntry approved = resolveApprovedEntry(path, approvedEntries);
                if (approved == null) {
                    respond(exchange, 404);
                    return;
                }

                Path file = FileUtils.resolveSafeChild(
                        FileUtils.resolveServerSourceRoot(approved.getCategory()),
                        approved.getRelativePath()
                );
                if (!Files.exists(file)) {
                    respond(exchange, 404);
                    return;
                }

                exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
                exchange.sendResponseHeaders(200, Files.size(file));
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    Files.copy(file, outputStream);
                }
            } catch (Exception exception) {
                LoggerUtils.warn("Rejected HTTP file request: " + exception.getMessage());
                respond(exchange, 400);
            }
        }
    }

    private final class ManifestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                respond(exchange, 405);
                return;
            }

            String hostHeader = exchange.getRequestHeaders().getFirst("Host");
            String host = hostHeader == null ? "127.0.0.1" : ServerManifestHttpHandler.extractHost(hostHeader);
            byte[] payload = ServerManifestHttpHandler.manifestBytes(host);

            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, payload.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(payload);
            }
        }
    }
}
