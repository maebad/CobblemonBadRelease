package com.maebad.cobblemonpcmod;

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
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Client-side initializer for the CobblemonPCMod.
 * Adds release-on-right-click functionality and a toggle overlay.
 */
public class CobblemonPCModClient implements ClientModInitializer {

    // ----- Ajouts pour le toggle sur H -----
    private static KeyMapping toggleKey;
    public static boolean modEnabled = true;
    // ----------------------------------------

    @Override
    public void onInitializeClient() {
        // Existing: registration du GUI PC release-on-right-click
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
                            // —————— Annulation de la sélection pour éviter le second clic ——————
                            try {
                            // 1) Réinitialise la sélection
                            Method reset = storageWidget.getClass()
                            .getDeclaredMethod("resetSelected");
                            reset.setAccessible(true);
                            reset.invoke(storageWidget);

                            // 2) Désactive l'affichage de la boîte de confirmation de relâche
                            Method setConfirm = storageWidget.getClass()
                            .getDeclaredMethod("setDisplayConfirmRelease", boolean.class);
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

        // --- Début injection: Toggle activation/désactivation ---
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.cobblemonpcmod.toggle",        // Clé de traduction
            InputConstants.Type.KEYSYM,            // Type clavier
            GLFW.GLFW_KEY_H,                      // Touche H
            "category.cobblemonpcmod.main"      // Catégorie
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.consumeClick()) {
                modEnabled = !modEnabled;
            }
        });

        HudRenderCallback.EVENT.register((gui, tickDelta) -> {
            if (modEnabled) {
                drawCenteredText(gui);
            }
        });
        // --- Fin injection ---
    }

    /**
     * Dessine le texte "CobblemonPCMod activé" centré en haut de l'écran en jaune.
     */
    private void drawCenteredText(GuiGraphics gui) {
        Minecraft mc = Minecraft.getInstance();
        String message = "CobblemonPCMod activé (H)";
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int textWidth = mc.font.width(message);
        int x = (screenWidth - textWidth) / 2;
        int y = 10;
        gui.drawString(mc.font, message, x, y, 0xFFFF00, false);
    }
}