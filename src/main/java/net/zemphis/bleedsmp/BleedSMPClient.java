package net.zemphis.bleedsmp;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.zemphis.bleed.screen.ContractScreen;
import net.zemphis.bleed.screen.ModScreenHandlers;

public class BleedSMPClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.CONTRACT_SCREEN_HANDLER, ContractScreen::new);
    }
}
