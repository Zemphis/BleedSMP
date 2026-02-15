package net.zemphis.bleed.util;

import net.minecraft.server.network.ServerPlayerEntity;
import java.util.Set;

public class PlayerState {

    // Helper to get the target name from tags
    public static String getHuntTarget(ServerPlayerEntity player) {
        for (String tag : player.getCommandTags()) {
            if (tag.startsWith("hunt_target:")) {
                return tag.substring("hunt_target:".length());
            }
        }
        return "";
    }

    // Helper to set the target name
    public static void setHuntTarget(ServerPlayerEntity player, String targetName) {
        // Remove old target tag first
        String oldTarget = getHuntTarget(player);
        if (!oldTarget.isEmpty()) {
            player.removeCommandTag("hunt_target:" + oldTarget);
        }
        // Add new one
        if (!targetName.isEmpty()) {
            player.addCommandTag("hunt_target:" + targetName);
        }
    }

    // Helper to get tier
    public static int getHuntTier(ServerPlayerEntity player) {
        for (String tag : player.getCommandTags()) {
            if (tag.startsWith("hunt_tier:")) {
                try {
                    return Integer.parseInt(tag.substring("hunt_tier:".length()));
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    // Helper to set tier
    public static void setHuntTier(ServerPlayerEntity player, int tier) {
        int oldTier = getHuntTier(player);
        if (oldTier != 0) {
            player.removeCommandTag("hunt_tier:" + oldTier);
        }
        if (tier > 0) {
            player.addCommandTag("hunt_tier:" + tier);
        }
    }

    // Helper to check/set hunting status
    public static boolean isHunting(ServerPlayerEntity player) {
        return player.getCommandTags().contains("bleedsmp_hunting");
    }

    public static void setHunting(ServerPlayerEntity player, boolean hunting) {
        if (hunting) {
            player.addCommandTag("bleedsmp_hunting");
        } else {
            player.removeCommandTag("bleedsmp_hunting");
        }
    }
}