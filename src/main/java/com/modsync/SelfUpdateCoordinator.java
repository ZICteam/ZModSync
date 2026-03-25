package com.modsync;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SelfUpdateCoordinator {
    private static final AtomicBoolean SHUTDOWN_HOOK_REGISTERED = new AtomicBoolean(false);

    private SelfUpdateCoordinator() {
    }

    public static void initShutdownHook() {
        if (!SHUTDOWN_HOOK_REGISTERED.compareAndSet(false, true)) {
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(SelfUpdateCoordinator::launchPendingUpdaterSafely, "modsync-self-update-hook"));
    }

    public static SelfUpdatePlan planForEntry(ManifestEntry entry, Path defaultTarget) {
        return planForEntry(entry, defaultTarget, detectCurrentModJarPath());
    }

    static SelfUpdatePlan planForEntry(ManifestEntry entry, Path defaultTarget, Path currentJarPath) {
        if (!isSelfUpdateEntry(entry) || defaultTarget == null || currentJarPath == null) {
            return new SelfUpdatePlan(false, defaultTarget, null, null, null);
        }

        Path normalizedDefaultTarget = defaultTarget.toAbsolutePath().normalize();
        Path normalizedCurrentJarPath = currentJarPath.toAbsolutePath().normalize();
        Path clientStateDir = deriveClientStateDir(normalizedCurrentJarPath);
        if (clientStateDir == null) {
            return new SelfUpdatePlan(false, defaultTarget, null, null, null);
        }
        Path stagedJarPath = clientStateDir
                .resolve("self-update")
                .resolve(normalizedDefaultTarget.getFileName().toString() + ".pending")
                .normalize();

        return new SelfUpdatePlan(true, stagedJarPath, normalizedCurrentJarPath, normalizedDefaultTarget, stagedJarPath);
    }

    public static void registerDownloadedUpdate(SelfUpdatePlan plan) {
        if (plan == null || !plan.active()) {
            return;
        }

        try {
            FileUtils.ensureParentExists(pendingPlanPath());
            Properties properties = new Properties();
            properties.setProperty("currentJarPath", plan.currentJarPath().toString());
            properties.setProperty("targetJarPath", plan.targetJarPath().toString());
            properties.setProperty("stagedJarPath", plan.stagedJarPath().toString());
            try (var writer = Files.newBufferedWriter(pendingPlanPath(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                properties.store(writer, "ModSync self update");
            }
            LoggerUtils.info("Prepared staged ModSync self-update for next restart: " + plan.targetJarPath().getFileName());
        } catch (IOException exception) {
            LoggerUtils.error("Failed to prepare staged ModSync self-update", exception);
        }
    }

    static PendingSelfUpdate loadPendingUpdate() {
        Path planFile = pendingPlanPath();
        if (!Files.exists(planFile)) {
            return null;
        }

        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(planFile, StandardCharsets.UTF_8)) {
            properties.load(reader);
            Path currentJarPath = Path.of(properties.getProperty("currentJarPath", "")).toAbsolutePath().normalize();
            Path targetJarPath = Path.of(properties.getProperty("targetJarPath", "")).toAbsolutePath().normalize();
            Path stagedJarPath = Path.of(properties.getProperty("stagedJarPath", "")).toAbsolutePath().normalize();
            if (currentJarPath.toString().isBlank() || targetJarPath.toString().isBlank() || stagedJarPath.toString().isBlank()) {
                return null;
            }
            return new PendingSelfUpdate(currentJarPath, targetJarPath, stagedJarPath, planFile);
        } catch (Exception exception) {
            LoggerUtils.warn("Failed to load pending ModSync self-update: " + exception.getMessage());
            return null;
        }
    }

    static boolean isSelfUpdateEntry(ManifestEntry entry) {
        return entry != null
                && entry.getCategory() == CategoryType.MOD
                && FileUtils.isProtectedModFileName(entry.getFileName());
    }

    private static Path deriveClientStateDir(Path currentJarPath) {
        if (currentJarPath == null) {
            return null;
        }
        Path modsDir = currentJarPath.getParent();
        if (modsDir == null) {
            return null;
        }
        Path gameDir = modsDir.getParent();
        if (gameDir == null) {
            return null;
        }
        return gameDir.resolve(".modsync").normalize();
    }

    static Path detectCurrentModJarPath() {
        try {
            var codeSource = ModSync.class.getProtectionDomain().getCodeSource();
            if (codeSource == null || codeSource.getLocation() == null) {
                return null;
            }
            Path path = Path.of(codeSource.getLocation().toURI()).toAbsolutePath().normalize();
            if (Files.isRegularFile(path) && path.getFileName().toString().toLowerCase().endsWith(".jar")) {
                return path;
            }
            return null;
        } catch (URISyntaxException | IllegalArgumentException exception) {
            LoggerUtils.warn("Failed to resolve current ModSync jar path: " + exception.getMessage());
            return null;
        }
    }

    private static void launchPendingUpdaterSafely() {
        try {
            PendingSelfUpdate pending = loadPendingUpdate();
            if (pending == null || !Files.exists(pending.stagedJarPath())) {
                return;
            }
            Path script = createLauncherScript(pending);
            List<String> command = buildLauncherCommand(script, pending);
            new ProcessBuilder(command).start();
        } catch (Exception exception) {
            LoggerUtils.warn("Failed to launch staged ModSync self-update: " + exception.getMessage());
        }
    }

    private static Path createLauncherScript(PendingSelfUpdate pending) throws IOException {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        Path script = FileUtils.modSyncClientDir()
                .resolve("self-update")
                .resolve(windows ? "apply-self-update.cmd" : "apply-self-update.sh")
                .normalize();
        FileUtils.ensureParentExists(script);
        String content = windows ? buildWindowsScript() : buildUnixScript();
        Files.writeString(script, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        if (!windows) {
            script.toFile().setExecutable(true);
        }
        return script;
    }

    private static List<String> buildLauncherCommand(Path script, PendingSelfUpdate pending) {
        String pid = Long.toString(ProcessHandle.current().pid());
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        if (windows) {
            return List.of("cmd", "/c", script.toString(),
                    pid,
                    pending.currentJarPath().toString(),
                    pending.targetJarPath().toString(),
                    pending.stagedJarPath().toString(),
                    pending.planFile().toString());
        }
        return List.of("/bin/sh", script.toString(),
                pid,
                pending.currentJarPath().toString(),
                pending.targetJarPath().toString(),
                pending.stagedJarPath().toString(),
                pending.planFile().toString());
    }

    private static String buildUnixScript() {
        return """
                #!/bin/sh
                PID="$1"
                CURRENT_JAR="$2"
                TARGET_JAR="$3"
                STAGED_JAR="$4"
                PLAN_FILE="$5"
                while kill -0 "$PID" 2>/dev/null; do
                  sleep 1
                done
                mkdir -p "$(dirname "$TARGET_JAR")"
                if [ "$CURRENT_JAR" != "$TARGET_JAR" ] && [ -f "$CURRENT_JAR" ]; then
                  rm -f "$CURRENT_JAR"
                fi
                rm -f "$TARGET_JAR"
                mv -f "$STAGED_JAR" "$TARGET_JAR"
                rm -f "$PLAN_FILE"
                """;
    }

    private static String buildWindowsScript() {
        return """
                @echo off
                set PID=%1
                set CURRENT_JAR=%~2
                set TARGET_JAR=%~3
                set STAGED_JAR=%~4
                set PLAN_FILE=%~5
                :waitloop
                tasklist /FI "PID eq %PID%" 2>NUL | find "%PID%" >NUL
                if not errorlevel 1 (
                  timeout /t 1 /nobreak >NUL
                  goto waitloop
                )
                powershell -NoProfile -ExecutionPolicy Bypass -Command ^
                  "$current=$env:CURRENT_JAR; $target=$env:TARGET_JAR; $staged=$env:STAGED_JAR; $plan=$env:PLAN_FILE; " ^
                  "New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null; " ^
                  "if ($current -ne $target -and (Test-Path $current)) { Remove-Item -Force $current }; " ^
                  "if (Test-Path $target) { Remove-Item -Force $target }; " ^
                  "Move-Item -Force $staged $target; " ^
                  "if (Test-Path $plan) { Remove-Item -Force $plan }"
                """;
    }

    static Path pendingPlanPath() {
        return FileUtils.modSyncClientDir().resolve("self-update").resolve("pending-update.properties").normalize();
    }

    record SelfUpdatePlan(boolean active,
                          Path downloadTargetPath,
                          Path currentJarPath,
                          Path targetJarPath,
                          Path stagedJarPath) {
    }

    record PendingSelfUpdate(Path currentJarPath,
                             Path targetJarPath,
                             Path stagedJarPath,
                             Path planFile) {
    }
}
