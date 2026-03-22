package com.modsync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class LoggerUtils {
    private static final Logger LOGGER = Logger.getLogger("ModSync");
    private static volatile boolean initialized;

    private LoggerUtils() {
    }

    public static synchronized void init(Path logPath, boolean logToFile) {
        if (initialized) {
            return;
        }

        LOGGER.setUseParentHandlers(true);
        LOGGER.setLevel(Level.INFO);

        if (logToFile) {
            try {
                Files.createDirectories(logPath.toAbsolutePath().normalize().getParent());
                FileHandler handler = new FileHandler(logPath.toAbsolutePath().normalize().toString(), true);
                handler.setFormatter(new Formatter() {
                    @Override
                    public String format(LogRecord record) {
                        return "[" + record.getLevel() + "] " + record.getMessage() + System.lineSeparator();
                    }
                });
                LOGGER.addHandler(handler);
            } catch (IOException exception) {
                LOGGER.log(Level.WARNING, "Unable to initialize file logger: " + exception.getMessage(), exception);
            }
        }

        initialized = true;
    }

    public static void info(String message) {
        SyncLogBuffer.append(message);
        LOGGER.info(message);
    }

    public static void warn(String message) {
        SyncLogBuffer.append("WARN: " + message);
        LOGGER.warning(message);
    }

    public static void error(String message, Throwable throwable) {
        SyncLogBuffer.append("ERROR: " + message + (throwable == null ? "" : " - " + throwable.getMessage()));
        LOGGER.log(Level.SEVERE, message, throwable);
    }
}
