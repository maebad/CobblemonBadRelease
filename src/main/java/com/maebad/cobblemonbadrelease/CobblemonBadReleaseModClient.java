package com.maebad.cobblemonbadrelease;

import com.cobblemon.mod.common.CobblemonNetwork;
import com.cobblemon.mod.common.api.storage.pc.PCPosition;
import com.cobblemon.mod.common.client.gui.pc.PCGUI;
import com.cobblemon.mod.common.client.gui.pc.StorageWidget;
import com.cobblemon.mod.common.net.messages.server.storage.pc.ReleasePCPokemonPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Client-side initializer for the CobblemonBadReleaseMod.
 * Adds release-on-right-click functionality, toggle overlay,
 * and persistence of toggle state in config.
 */
public class CobblemonBadReleaseModClient implements ClientModInitializer {

    // ----- Ajouts pour le toggle sur H -----
    private static KeyMapping toggleKey;
    public static boolean modEnabled = true;
    // ----------------------------------------

    // Emplacement du fichier de config dans config/cobblemonpcmod.properties
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("cobblemonpcmod.properties");

    @Override
    public void onInitializeClient() {
        // Charger la config dès le démarrage
        loadConfig();

        // Registration du GUI PC release-on-right-click
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof PCGUI pcScreen) {
                ScreenMouseEvents.beforeMouseClick(pcScreen).register((s, mouseX, mouseY, button) -> {
                    if (!modEnabled) return; // Skip if toggled off
                    if (button == 1) { // Clic droit
                        try {
                            Field storageField = pcScreen.getClass().getDeclaredField("storageWidget");
                            storageField.setAccessible(true);
                            StorageWidget storageWidget = (StorageWidget) storageField.get(pcScreen);

                            Method getSelectedPokemon = storageWidget.getClass().getDeclaredMethod("getSelectedPokemon");
                            getSelectedPokemon.setAccessible(true);
                            Method getSelectedPosition = storageWidget.getClass().getDeclaredMethod("getSelectedPosition");
                            getSelectedPosition.setAccessible(true);

                            Object selectedPokemon = getSelectedPokemon.invoke(storageWidget);
                            PCPosition pos = (PCPosition) getSelectedPosition.invoke(storageWidget);
                            UUID id = (UUID) selectedPokemon.getClass().getMethod("getUuid").invoke(selectedPokemon);

                            ReleasePCPokemonPacket pkt = new ReleasePCPokemonPacket(id, pos);
                            CobblemonNetwork.INSTANCE.sendToServer(pkt);

                            // Reset selection & hide confirm dialog
                            try {
                                Method reset = storageWidget.getClass().getDeclaredMethod("resetSelected");
                                reset.setAccessible(true);
                                reset.invoke(storageWidget);

                                Method setConfirm = storageWidget.getClass().getDeclaredMethod("setDisplayConfirmRelease", boolean.class);
                                setConfirm.setAccessible(true);
                                setConfirm.invoke(storageWidget, false);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        // --- Toggle activation/désactivation ---
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.cobblemonpcmod.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.cobblemonpcmod.main"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.consumeClick()) {
                modEnabled = !modEnabled;
                saveConfig();
                if (client.player != null) {
                    client.player.displayClientMessage(
                        Component.literal("CobblemonBadRelease " + (modEnabled ? "activé" : "désactivé")),
                        false
                    );
                }
            }
        });

        // Overlay HUD
        HudRenderCallback.EVENT.register((gui, tickDelta) -> {
            if (modEnabled) drawCenteredText(gui);
        });
    }

    // --- Config methods ---
    private static void loadConfig() {
        Properties props = new Properties();
        if (Files.exists(CONFIG_PATH)) {
            try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                props.load(in);
                modEnabled = Boolean.parseBoolean(props.getProperty("modEnabled", Boolean.toString(modEnabled)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void saveConfig() {
        Properties props = new Properties();
        props.setProperty("modEnabled", Boolean.toString(modEnabled));
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
                props.store(out, "Configuration de CobblemonBadReleaseMod");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- HUD text ---
    private void drawCenteredText(GuiGraphics gui) {
        Minecraft mc = Minecraft.getInstance();
        String message = "CobblemonBadRelease activé (H pour désactiver)";
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int textWidth = mc.font.width(message);
        int x = (screenWidth - textWidth) / 2;
        int y = 10;
        gui.drawString(mc.font, message, x, y, 0xFFFF00, false);
    }
}