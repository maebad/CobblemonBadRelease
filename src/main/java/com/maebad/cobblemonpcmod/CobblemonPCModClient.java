package com.maebad.cobblemonpcmod;

import com.cobblemon.mod.common.CobblemonNetwork;
import com.cobblemon.mod.common.api.storage.pc.PCPosition;
import com.cobblemon.mod.common.client.gui.pc.PCGUI;
import com.cobblemon.mod.common.client.gui.pc.StorageWidget;
import com.cobblemon.mod.common.net.messages.server.storage.pc.ReleasePCPokemonPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

public class CobblemonPCModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof PCGUI pcScreen) {
                ScreenMouseEvents.beforeMouseClick(pcScreen).register((s, mouseX, mouseY, button) -> {
                    if (button == 1) { // Clic droit

                        try {
                            // Accès à storageWidget
                            Field storageField = pcScreen.getClass().getDeclaredField("storageWidget");
                            storageField.setAccessible(true);
                            StorageWidget storageWidget = (StorageWidget) storageField.get(pcScreen);

                            // Méthodes privées
                            Method getSelectedPokemon = storageWidget.getClass().getDeclaredMethod("getSelectedPokemon");
                            getSelectedPokemon.setAccessible(true);

                            Method getSelectedPosition = storageWidget.getClass().getDeclaredMethod("getSelectedPosition");
                            getSelectedPosition.setAccessible(true);

                            // Appels
                            Object selectedPokemon = getSelectedPokemon.invoke(storageWidget);
                            PCPosition pos = (PCPosition) getSelectedPosition.invoke(storageWidget);

                            // Extraire l'UUID
                            Method getUuidMethod = selectedPokemon.getClass().getMethod("getUuid");
                            UUID id = (UUID) getUuidMethod.invoke(selectedPokemon);

                            // Packet de release
                            ReleasePCPokemonPacket pkt = new ReleasePCPokemonPacket(id, pos);
                            CobblemonNetwork.INSTANCE.sendToServer(pkt);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }
}