package com.modsync;

import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.EditServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModSyncMultiplayerScreen extends Screen {
    private static final int PANEL_WIDTH = 214;
    private static final int FOOTER_HEIGHT = 36;
    private static final int LIST_TOP = 44;
    private static final int ROW_HEIGHT = 54;
    private static final int ICON_SIZE = 32;
    private static final int LIST_SIDE_PADDING = 6;
    private static final int LIST_SCROLLBAR_WIDTH = 6;
    private static final Map<String, IconTexture> ICON_CACHE = new ConcurrentHashMap<>();

    private final Screen parent;

    private ServerList serverList;
    private ServerStatusPinger pinger;
    private ServerBrowserList listWidget;

    private Button connectButton;
    private Button downloadButton;
    private Button editButton;
    private Button deleteButton;
    private Button upButton;
    private Button downButton;
    private Button refreshButton;
    private Button addButton;
    private Button directButton;
    private Button backButton;

    public ModSyncMultiplayerScreen(Screen parent) {
        super(Component.translatable("menu.multiplayer"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        boolean firstInit = serverList == null;
        if (firstInit) {
            serverList = new ServerList(minecraft);
            serverList.load();
        }
        if (pinger == null) {
            pinger = new ServerStatusPinger();
        }

        buildLayout();
        reloadEntries(null);
    }

    private void buildLayout() {
        clearWidgets();

        int listWidth = Math.max(250, width - PANEL_WIDTH - 52);
        int listLeft = 24;
        int listRight = listLeft + listWidth;
        int listBottom = height - FOOTER_HEIGHT - 14;

        listWidget = new ServerBrowserList(minecraft, listWidth, listBottom - LIST_TOP, LIST_TOP, listBottom, ROW_HEIGHT, listLeft);
        addRenderableWidget(listWidget);

        int panelX = listRight + 18;
        int buttonWidth = PANEL_WIDTH - 20;
        int y = LIST_TOP;

        connectButton = addRenderableWidget(Button.builder(Component.translatable("selectServer.select"), button -> connectSelected())
                .bounds(panelX, y, buttonWidth, 20)
                .build());
        y += 24;
        downloadButton = addRenderableWidget(Button.builder(LanguageManager.component("modsync.download_button"), button -> downloadSelected())
                .bounds(panelX, y, buttonWidth, 20)
                .build());
        y += 24;
        refreshButton = addRenderableWidget(Button.builder(LanguageManager.component("modsync.refresh"), button -> refreshServers())
                .bounds(panelX, y, buttonWidth, 20)
                .build());
        y += 32;
        addButton = addRenderableWidget(Button.builder(Component.translatable("selectServer.add"), button -> addServer())
                .bounds(panelX, y, buttonWidth, 20)
                .build());
        y += 24;
        directButton = addRenderableWidget(Button.builder(Component.translatable("selectServer.direct"), button -> directConnect())
                .bounds(panelX, y, buttonWidth, 20)
                .build());
        y += 24;
        editButton = addRenderableWidget(Button.builder(Component.translatable("selectServer.edit"), button -> editSelected())
                .bounds(panelX, y, buttonWidth, 20)
                .build());
        y += 24;
        deleteButton = addRenderableWidget(Button.builder(Component.translatable("selectServer.delete"), button -> deleteSelected())
                .bounds(panelX, y, buttonWidth, 20)
                .build());
        y += 24;
        upButton = addRenderableWidget(Button.builder(Component.literal("^"), button -> moveSelected(-1))
                .bounds(panelX, y, buttonWidth / 2 - 2, 20)
                .build());
        downButton = addRenderableWidget(Button.builder(Component.literal("v"), button -> moveSelected(1))
                .bounds(panelX + buttonWidth / 2 + 2, y, buttonWidth / 2 - 2, 20)
                .build());

        backButton = addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                .bounds(panelX, height - 30, buttonWidth, 20)
                .build());

        updateButtons();
    }

    private void reloadEntries(String selectedIp) {
        if (listWidget == null) {
            return;
        }

        listWidget.clearAllEntries();
        ServerEntry selectedEntry = null;
        for (int i = 0; i < serverList.size(); i++) {
            ServerData serverData = serverList.get(i);
            ServerEntry entry = listWidget.addServer(serverData, i);
            if (selectedIp != null && selectedIp.equals(serverData.ip)) {
                selectedEntry = entry;
            }
        }

        if (selectedEntry != null) {
            listWidget.setSelected(selectedEntry);
        } else if (serverList.size() > 0 && !listWidget.children().isEmpty()) {
            listWidget.setSelected(listWidget.children().get(0));
        }
        updateButtons();
    }

    private void refreshServers() {
        if (pinger != null) {
            pinger.removeAll();
        }
        for (int i = 0; i < serverList.size(); i++) {
            ServerData serverData = serverList.get(i);
            ServerSyncStatusCache.markDirty(serverData);
            ServerSyncStatusCache.markChecking(serverData);
            serverData.pinged = false;
            serverData.ping = -1L;
            serverData.version = Component.literal("...");
            serverData.motd = Component.empty();
            try {
                pinger.pingServer(serverData, () -> {
                    ServerSyncStatusCache.requestRefresh(serverData);
                });
            } catch (Exception exception) {
                LoggerUtils.warn("Failed to ping server " + serverData.ip + ": " + exception.getMessage());
            }
        }
    }

    private void connectSelected() {
        ServerEntry entry = listWidget.getSelected();
        if (entry == null) {
            return;
        }
        ServerSyncStatusCache.markDirty(entry.serverData);
        PreJoinSyncManager.startForServer(entry.serverData, this, true, false);
    }

    private void downloadSelected() {
        ServerEntry entry = listWidget.getSelected();
        if (entry == null) {
            return;
        }
        ServerSyncStatusCache.markDirty(entry.serverData);
        PreJoinSyncManager.startForServer(entry.serverData, this, false);
    }

    private void addServer() {
        ServerData serverData = new ServerData(LanguageManager.get("modsync.server_name_default"), "", false);
        openEditor(serverData, accepted -> {
            if (accepted) {
                serverList.add(serverData, false);
                serverList.save();
                reloadEntries(serverData.ip);
            }
            minecraft.setScreen(this);
        });
    }

    private void directConnect() {
        ServerData serverData = new ServerData(LanguageManager.get("modsync.direct_server_default"), "", false);
        minecraft.setScreen(new DirectJoinServerScreen(this, accepted -> {
            if (accepted) {
                ServerSyncStatusCache.markDirty(serverData);
                PreJoinSyncManager.startForServer(serverData, this, true, false);
            } else {
                minecraft.setScreen(this);
            }
        }, serverData));
    }

    private void editSelected() {
        ServerEntry entry = listWidget.getSelected();
        if (entry == null) {
            return;
        }

        ServerData edited = new ServerData(entry.serverData.name, entry.serverData.ip, entry.serverData.isLan());
        edited.copyFrom(entry.serverData);
        int selectedIndex = entry.index;
        openEditor(edited, accepted -> {
            if (accepted) {
                serverList.replace(selectedIndex, edited);
                serverList.save();
                reloadEntries(edited.ip);
            }
            minecraft.setScreen(this);
        });
    }

    private void deleteSelected() {
        ServerEntry entry = listWidget.getSelected();
        if (entry == null) {
            return;
        }

        serverList.remove(entry.serverData);
        serverList.save();
        reloadEntries(null);
    }

    private void moveSelected(int direction) {
        ServerEntry entry = listWidget.getSelected();
        if (entry == null) {
            return;
        }

        int target = entry.index + direction;
        if (target < 0 || target >= serverList.size()) {
            return;
        }

        serverList.swap(entry.index, target);
        serverList.save();
        reloadEntries(entry.serverData.ip);
    }

    private void openEditor(ServerData serverData, BooleanConsumer consumer) {
        minecraft.setScreen(new EditServerScreen(this, consumer, serverData));
    }

    private void updateButtons() {
        if (connectButton == null) {
            return;
        }

        ServerEntry entry = listWidget == null ? null : listWidget.getSelected();
        boolean hasSelection = entry != null;
        connectButton.active = hasSelection;
        downloadButton.active = hasSelection;
        editButton.active = hasSelection;
        deleteButton.active = hasSelection;
        upButton.active = hasSelection && entry.index > 0;
        downButton.active = hasSelection && entry.index < serverList.size() - 1;
    }

    @Override
    public void tick() {
        super.tick();
        if (pinger != null) {
            pinger.tick();
        }
    }

    @Override
    public void removed() {
        super.removed();
        if (pinger != null) {
            pinger.removeAll();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);

        int listWidth = Math.max(250, width - PANEL_WIDTH - 52);
        int listLeft = 24;
        int listRight = listLeft + listWidth;
        int listBottom = height - FOOTER_HEIGHT - 14;
        int panelX = listRight + 18;

        guiGraphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFF);

        guiGraphics.fill(listLeft - 4, LIST_TOP - 4, listRight + 4, listBottom + 4, 0x66101010);
        guiGraphics.fill(panelX - 6, LIST_TOP - 4, width - 18, height - 10, 0x66101010);

        if (serverList.size() == 0) {
            guiGraphics.drawCenteredString(font, LanguageManager.component("modsync.multiplayer.empty"), listLeft + listWidth / 2, height / 2 - 20, 0xCFCFCF);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        drawDetails(guiGraphics, panelX);
    }

    private void drawDetails(GuiGraphics guiGraphics, int panelX) {
        ServerEntry entry = listWidget.getSelected();
        int contentX = panelX + 8;
        int y = downButton.getY() + downButton.getHeight() + 14;
        int maxWidth = PANEL_WIDTH - 34;

        guiGraphics.drawString(font, LanguageManager.component("modsync.multiplayer.details"), contentX, y, 0xFFFFFF, false);
        y += 16;

        if (entry == null) {
            guiGraphics.drawString(font, LanguageManager.component("modsync.multiplayer.no_selection"), contentX, y, 0xAFAFAF, false);
            return;
        }

        ServerData serverData = entry.serverData;
        ServerSyncStatusCache.SyncState state = ServerSyncStatusCache.getStatus(serverData);

        guiGraphics.drawString(font, trim(serverData.name, maxWidth), contentX, y, 0xFFFFFF, false);
        y += 12;
        guiGraphics.drawString(font, trim(serverData.ip, maxWidth), contentX, y, 0x9F9F9F, false);
        y += 16;
        y = drawDetailValue(guiGraphics, contentX, y, LanguageManager.component("modsync.multiplayer.ping"), formatPing(serverData), 0xFFFFFF);
        y = drawDetailValue(guiGraphics, contentX, y, LanguageManager.component("modsync.multiplayer.version"), trim(formatVersion(serverData), maxWidth), 0xFFFFFF);
        y = drawDetailValue(guiGraphics, contentX, y, LanguageManager.component("modsync.multiplayer.sync"), statusText(state), statusTextColor(state));
        for (FormattedLine line : wrapDescription(serverData, maxWidth)) {
            guiGraphics.drawString(font, line.text, contentX, y, line.color, false);
            y += 10;
            if (y > backButton.getY() - 14) {
                break;
            }
        }
    }

    private int drawDetailValue(GuiGraphics guiGraphics, int x, int y, Component label, String value, int valueColor) {
        guiGraphics.drawString(font, label, x, y, 0xD7D7D7, false);
        y += 10;
        guiGraphics.drawString(font, trim(value, PANEL_WIDTH - 42), x + 8, y, valueColor, false);
        return y + 16;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            connectSelected();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private String trim(String text, int maxWidth) {
        return font.plainSubstrByWidth(text == null ? "" : text, Math.max(8, maxWidth));
    }

    private static String formatPing(ServerData serverData) {
        if (!serverData.pinged || serverData.ping < 0L) {
            return "--";
        }
        return serverData.ping + " ms";
    }

    private static String formatVersion(ServerData serverData) {
        if (serverData.version == null) {
            return "--";
        }
        String text = serverData.version.getString();
        return text == null || text.isBlank() ? "--" : text;
    }

    private static String formatDescription(ServerData serverData) {
        if (serverData.status != null && !serverData.status.getString().isBlank()) {
            return serverData.status.getString();
        }
        if (serverData.motd != null && !serverData.motd.getString().isBlank()) {
            return serverData.motd.getString();
        }
        return "";
    }

    private List<FormattedLine> wrapDescription(ServerData serverData, int maxWidth) {
        List<FormattedLine> lines = new ArrayList<>();
        boolean hasStatus = serverData.status != null && !serverData.status.getString().isBlank();
        int color = hasStatus ? 0xE36A6A : 0xBFBFBF;
        String text = formatDescription(serverData);
        int chunkWidth = Math.max(24, maxWidth);
        while (!text.isEmpty()) {
            String line = font.plainSubstrByWidth(text, chunkWidth);
            if (line.isEmpty()) {
                break;
            }
            lines.add(new FormattedLine(line, color));
            text = text.substring(line.length()).stripLeading();
        }
        return lines;
    }

    private static int statusColor(ServerSyncStatusCache.SyncState state) {
        return switch (state) {
            case UNKNOWN -> 0xFF3A3A3A;
            case SYNCED -> 0xFF275D37;
            case OUTDATED, ERROR -> 0xFF7A2626;
            case CHECKING -> 0xFF6B612D;
        };
    }

    private static int statusTextColor(ServerSyncStatusCache.SyncState state) {
        return switch (state) {
            case UNKNOWN -> 0xB8B8B8;
            case SYNCED -> 0x84F0A1;
            case OUTDATED, ERROR -> 0xFF8A8A;
            case CHECKING -> 0xF0DE84;
        };
    }

    private static String statusBadge(ServerSyncStatusCache.SyncState state) {
        return switch (state) {
            case UNKNOWN -> "--";
            case SYNCED -> "OK";
            case OUTDATED -> "DL";
            case ERROR -> "ERR";
            case CHECKING -> "...";
        };
    }

    private static String statusText(ServerSyncStatusCache.SyncState state) {
        return switch (state) {
            case UNKNOWN -> LanguageManager.get("modsync.status.unknown");
            case SYNCED -> LanguageManager.get("modsync.status.synced");
            case OUTDATED -> LanguageManager.get("modsync.status.outdated");
            case ERROR -> LanguageManager.get("modsync.status.error");
            case CHECKING -> LanguageManager.get("modsync.status.checking");
        };
    }

    private record FormattedLine(String text, int color) {
    }

    private final class ServerBrowserList extends ObjectSelectionList<ServerEntry> {
        private final int left;

        private ServerBrowserList(Minecraft minecraft, int width, int height, int top, int bottom, int itemHeight, int left) {
            super(minecraft, width, height, top, bottom, itemHeight);
            this.left = left;
            setLeftPos(left);
            setRenderBackground(false);
            setRenderTopAndBottom(false);
        }

        @Override
        public int getRowLeft() {
            return left + LIST_SIDE_PADDING;
        }

        @Override
        public int getRowWidth() {
            return width - LIST_SIDE_PADDING * 2 - LIST_SCROLLBAR_WIDTH - 8;
        }

        @Override
        protected int getScrollbarPosition() {
            return left + width - LIST_SCROLLBAR_WIDTH - 6;
        }

        private ServerEntry addServer(ServerData serverData, int index) {
            ServerEntry entry = new ServerEntry(serverData, index);
            addEntry(entry);
            return entry;
        }

        private void clearAllEntries() {
            clearEntries();
        }
    }

    private final class ServerEntry extends ObjectSelectionList.Entry<ServerEntry> {
        private final ServerData serverData;
        private final int index;

        private ServerEntry(ServerData serverData, int index) {
            this.serverData = serverData;
            this.index = index;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean hovered, float partialTick) {
            boolean selected = listWidget.getSelected() == this;
            int border = selected ? 0xFFDFC684 : 0xFF303030;
            int background = hovered ? 0xB01A1A1A : 0x90101010;
            guiGraphics.fill(left, top, left + rowWidth, top + rowHeight - 2, background);
            guiGraphics.fill(left, top, left + rowWidth, top + 1, border);
            guiGraphics.fill(left, top + rowHeight - 3, left + rowWidth, top + rowHeight - 2, border);
            guiGraphics.fill(left, top, left + 1, top + rowHeight - 2, border);
            guiGraphics.fill(left + rowWidth - 1, top, left + rowWidth, top + rowHeight - 2, border);

            ServerSyncStatusCache.SyncState syncState = ServerSyncStatusCache.getStatus(serverData);
            int badgeWidth = 30;
            int badgeX = left + rowWidth - badgeWidth - 18;
            int badgeY = top + 8;
            guiGraphics.fill(badgeX, badgeY, badgeX + badgeWidth, badgeY + 14, statusColor(syncState));
            guiGraphics.drawCenteredString(font, statusBadge(syncState), badgeX + badgeWidth / 2, badgeY + 3, 0xFFFFFF);

            int metaRight = badgeX - 18;
            String pingText = formatPing(serverData);
            String versionText = trim(formatVersion(serverData), 96);
            int pingX = metaRight - font.width(pingText);
            int versionX = metaRight - font.width(versionText);
            guiGraphics.drawString(font, pingText, pingX, top + 8, 0xD7D7D7, false);
            guiGraphics.drawString(font, versionText, versionX, top + 22, 0x9F9F9F, false);

            int iconX = left + 10;
            int iconY = top + 10;
            drawServerIcon(guiGraphics, serverData, iconX, iconY);

            int textLeft = iconX + ICON_SIZE + 10;
            int textWidth = rowWidth - 212;
            guiGraphics.drawString(font, trim(serverData.name, textWidth), textLeft, top + 8, 0xFFFFFF, false);
            guiGraphics.drawString(font, trim(serverData.ip, textWidth), textLeft, top + 22, 0xA6A6A6, false);

            String desc = trim(formatDescription(serverData), textWidth);
            if (!desc.isBlank()) {
                int descColor = serverData.status != null && !serverData.status.getString().isBlank() ? 0xE36A6A : 0xBFBFBF;
                guiGraphics.drawString(font, desc, textLeft, top + 36, descColor, false);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            listWidget.setSelected(this);
            updateButtons();
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public Component getNarration() {
            ServerSyncStatusCache.SyncState state = ServerSyncStatusCache.getStatus(serverData);
            return Component.literal(serverData.name + " " + statusText(state)).withStyle(ChatFormatting.WHITE);
        }
    }

    private void drawServerIcon(GuiGraphics guiGraphics, ServerData serverData, int x, int y) {
        guiGraphics.fill(x - 1, y - 1, x + ICON_SIZE + 1, y + ICON_SIZE + 1, 0xFF202020);

        ResourceLocation texture = getServerIcon(serverData);
        if (texture != null) {
            guiGraphics.blit(texture, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            return;
        }

        guiGraphics.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, 0xFF2A2A2A);
        String initials = initials(serverData.name);
        guiGraphics.drawCenteredString(font, initials, x + ICON_SIZE / 2, y + 12, 0xD8D8D8);
    }

    private ResourceLocation getServerIcon(ServerData serverData) {
        byte[] iconBytes = serverData.getIconBytes();
        if (iconBytes == null || iconBytes.length == 0) {
            return null;
        }

        String key = serverData.ip;
        int hash = java.util.Arrays.hashCode(iconBytes);
        IconTexture cached = ICON_CACHE.get(key);
        if (cached != null && cached.hash == hash) {
            return cached.location;
        }

        try {
            NativeImage image = NativeImage.read(iconBytes);
            ResourceLocation location = minecraft.getTextureManager().register("modsync_server_" + Math.abs(key.hashCode()), new DynamicTexture(image));
            ICON_CACHE.put(key, new IconTexture(hash, location));
            return location;
        } catch (IOException exception) {
            return null;
        }
    }

    private static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String trimmed = name.trim();
        return trimmed.substring(0, Math.min(2, trimmed.length())).toUpperCase();
    }

    private record IconTexture(int hash, ResourceLocation location) {
    }
}
