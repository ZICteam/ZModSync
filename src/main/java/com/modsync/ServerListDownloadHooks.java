package com.modsync;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Field;

public final class ServerListDownloadHooks {
    private ServerListDownloadHooks() {
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof JoinMultiplayerScreen joinScreen)) {
            return;
        }

        event.setNewScreen(new ModSyncMultiplayerScreen(extractParent(joinScreen)));
    }

    private static Screen extractParent(JoinMultiplayerScreen joinScreen) {
        try {
            for (Field field : JoinMultiplayerScreen.class.getDeclaredFields()) {
                if (Screen.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Object value = field.get(joinScreen);
                    if (value instanceof Screen screen) {
                        return screen;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
