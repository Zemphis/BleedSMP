package net.zemphis.bleed.hunt;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.zemphis.bleed.components.HuntComponent;
import net.zemphis.bleed.components.ModComponents;

public class HuntManager {

    public static void startHunt(ServerPlayerEntity hunter, ServerPlayerEntity target, int tier) {
        HuntComponent hunt = ModComponents.HUNT.get(hunter);
        hunt.setHunting(true);
        hunt.setTargetName(target.getName().getString());
        hunt.setTier(tier);

        ModComponents.HUNT.sync(hunter);

        hunter.sendMessage(Text.literal("Target Acquired: " + target.getName().getString())
                .formatted(Formatting.GOLD), false);
    }

    public static void stopHunt(ServerPlayerEntity hunter, boolean success) {
        HuntComponent hunt = ModComponents.HUNT.get(hunter);
        hunt.setHunting(false);
        hunt.setTargetName("");
        hunt.setTier(0);

        ModComponents.HUNT.sync(hunter);

        if (success) {
            hunter.sendMessage(Text.literal("Contract Complete.").formatted(Formatting.GREEN), false);
        }
    }
}