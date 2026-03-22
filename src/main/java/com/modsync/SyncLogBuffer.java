package com.modsync;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class SyncLogBuffer {
    private static final int MAX_LINES = 200;
    private static final Deque<String> LINES = new ArrayDeque<>();

    private SyncLogBuffer() {
    }

    public static synchronized void clear() {
        LINES.clear();
    }

    public static synchronized void append(String line) {
        LINES.addLast(line);
        while (LINES.size() > MAX_LINES) {
            LINES.removeFirst();
        }
    }

    public static synchronized List<String> snapshot() {
        return new ArrayList<>(LINES);
    }
}
