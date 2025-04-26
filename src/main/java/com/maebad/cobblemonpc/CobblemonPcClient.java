package com.maebad.cobblemonpc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

/**
 * Single right-click instantly releases a Pokémon.
 * Strategy:
 *   • disable confirmation dialog
 *   • simulate left‑click to select
 *   • on next tick, delete the selected slot via ClientPC
 */
public class CobblemonPcClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen.getClass().getName().equals("com.cobblemon.mod.common.client.gui.pc.PCGUI")) {
                ScreenMouseEvents.beforeMouseClick(screen).register((s, mouseX, mouseY, button) -> {
                    if (button == 1) quickRelease(s, mouseX, mouseY);
                });
            }
        });
    }

    private void quickRelease(Screen pcGui, double x, double y) {
        try {
            Object sw = pcGui.getClass().getDeclaredMethod("getStorageWidget").invoke(pcGui);

            // 1) disable confirmation
            sw.getClass().getDeclaredMethod("setDisplayConfirmRelease", boolean.class)
              .invoke(sw, false);

            // 2) left-click to select
            var click = sw.getClass().getDeclaredMethod("method_25402",
                                                        double.class, double.class, int.class);
            click.invoke(sw, x, y, 0);

            // 3) schedule deletion on next client tick
            MinecraftClient.getInstance().execute(() -> {
                try {
                    Object selectedPos = sw.getClass().getDeclaredMethod("getSelectedPosition").invoke(sw);
                    if (selectedPos == null) return;

                    Object pc = pcGui.getClass().getDeclaredMethod("getPc").invoke(pcGui);
                    Class<?> pokemonCls = Class.forName("com.cobblemon.mod.common.pokemon.Pokemon");
                    pc.getClass().getDeclaredMethod("set", selectedPos.getClass(), pokemonCls)
                      .invoke(pc, selectedPos, null);

                    // refresh UI
                    sw.getClass().getDeclaredMethod("resetSelected").invoke(sw);
                    sw.getClass().getDeclaredMethod("resetStorageSlots").invoke(sw);
                } catch (Exception ignored) { }
            });

        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }
}