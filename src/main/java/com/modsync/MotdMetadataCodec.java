package com.modsync;

import net.minecraft.client.multiplayer.ServerData;

public final class MotdMetadataCodec {
    private static final char START = '\u2063';
    private static final char END = '\u2064';
    private static final char ZERO = '\u200B';
    private static final char ONE = '\u200C';

    private MotdMetadataCodec() {
    }

    public static String appendHiddenHttpPort(String visibleMotd, int port) {
        return stripHiddenMetadata(visibleMotd) + encodePort(port);
    }

    public static int extractHttpPort(ServerData serverData) {
        if (serverData == null) {
            return -1;
        }

        int fromStatus = extractHttpPort(serverData.status == null ? null : serverData.status.getString());
        if (fromStatus > 0) {
            return fromStatus;
        }

        return extractHttpPort(serverData.motd == null ? null : serverData.motd.getString());
    }

    public static int extractHttpPort(String text) {
        if (text == null || text.isEmpty()) {
            return -1;
        }

        int start = text.indexOf(START);
        int end = text.indexOf(END, start + 1);
        if (start < 0 || end <= start + 1) {
            return -1;
        }

        StringBuilder binary = new StringBuilder();
        for (int i = start + 1; i < end; i++) {
            char c = text.charAt(i);
            if (c == ZERO) {
                binary.append('0');
            } else if (c == ONE) {
                binary.append('1');
            }
        }

        if (binary.isEmpty()) {
            return -1;
        }

        try {
            int port = Integer.parseInt(binary.toString(), 2);
            return port >= 1 && port <= 65535 ? port : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public static String stripHiddenMetadata(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        int start = text.indexOf(START);
        if (start < 0) {
            return text;
        }

        int end = text.indexOf(END, start + 1);
        if (end < 0) {
            return text.substring(0, start);
        }

        return text.substring(0, start) + text.substring(end + 1);
    }

    private static String encodePort(int port) {
        String binary = Integer.toBinaryString(port);
        StringBuilder builder = new StringBuilder(binary.length() + 2);
        builder.append(START);
        for (int i = 0; i < binary.length(); i++) {
            builder.append(binary.charAt(i) == '0' ? ZERO : ONE);
        }
        builder.append(END);
        return builder.toString();
    }
}
