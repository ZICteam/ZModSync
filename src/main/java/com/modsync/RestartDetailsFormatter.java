package com.modsync;

import net.minecraft.client.gui.Font;

import java.util.ArrayList;
import java.util.List;

public final class RestartDetailsFormatter {
    private RestartDetailsFormatter() {
    }

    public static List<String> buildLines(Font font, int maxWidth, int maxLines) {
        List<RestartState.RestartChange> changes = RestartState.snapshotChanges();
        List<String> lines = new ArrayList<>();
        int remaining = Math.max(0, maxLines);

        for (RestartState.RestartChange change : changes) {
            if (remaining <= 0) {
                break;
            }

            String actionKey = change.type() == RestartState.ChangeType.DELETED
                    ? "modsync.restart.change.deleted"
                    : "modsync.restart.change.downloaded";
            String line = LanguageManager.get(actionKey) + ": "
                    + change.category().name().toLowerCase() + "/"
                    + change.relativePath();

            for (String wrapped : wrap(font, line, maxWidth)) {
                if (remaining <= 0) {
                    break;
                }
                lines.add(wrapped);
                remaining--;
            }
        }

        int hiddenCount = Math.max(0, changes.size() - countRepresentedChanges(lines, changes, font, maxWidth, maxLines));
        if (hiddenCount > 0 && maxLines > 0) {
            if (lines.size() == maxLines) {
                lines.remove(lines.size() - 1);
            }
            lines.add(String.format(LanguageManager.get("modsync.restart.more"), hiddenCount));
        }

        return lines;
    }

    private static int countRepresentedChanges(List<String> lines,
                                               List<RestartState.RestartChange> changes,
                                               Font font,
                                               int maxWidth,
                                               int maxLines) {
        int represented = 0;
        int usedLines = 0;
        for (RestartState.RestartChange change : changes) {
            String actionKey = change.type() == RestartState.ChangeType.DELETED
                    ? "modsync.restart.change.deleted"
                    : "modsync.restart.change.downloaded";
            String line = LanguageManager.get(actionKey) + ": "
                    + change.category().name().toLowerCase() + "/"
                    + change.relativePath();
            int lineCount = wrap(font, line, maxWidth).size();
            if (usedLines + lineCount > maxLines) {
                break;
            }
            usedLines += lineCount;
            represented++;
        }
        return represented;
    }

    private static List<String> wrap(Font font, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String remaining = text == null ? "" : text;
        int safeWidth = Math.max(20, maxWidth);
        while (!remaining.isEmpty()) {
            String part = font.plainSubstrByWidth(remaining, safeWidth);
            if (part.isEmpty()) {
                break;
            }
            lines.add(part);
            remaining = remaining.substring(part.length()).stripLeading();
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }
}
