package net.zemphis.bleed.hunt;

import net.minecraft.server.network.ServerPlayerEntity;
import net.zemphis.bleed.components.ModComponents;

public class HuntUtils {

    public static boolean isHunting(ServerPlayerEntity player) {
        return ModComponents.HUNT.get(player).isHunting();
    }

    public static String getTargetName(ServerPlayerEntity player) {
        return ModComponents.HUNT.get(player).getTargetName();
    }

    public static int getTier(ServerPlayerEntity player) {
        return ModComponents.HUNT.get(player).getTier();
    }
}