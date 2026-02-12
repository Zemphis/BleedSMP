package net.zemphis.bleed.screen;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.zemphis.bleedsmp.BleedSMP;

public class ModScreenHandlers {
    // We use the standard ScreenHandlerType since it's a stateless table (no BlockEntity data needed to be sent)
    public static final ScreenHandlerType<ContractScreenHandler> CONTRACT_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(BleedSMP.MOD_ID, "contract_table"),
                    new ScreenHandlerType<>(ContractScreenHandler::new, FeatureFlags.VANILLA_FEATURES));

    public static void registerScreenHandlers() {
        BleedSMP.LOGGER.info("Registering Screen Handlers for " + BleedSMP.MOD_ID);
    }
}