package net.zemphis.bleed.hunt;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.zemphis.bleed.components.HuntComponent;
import net.zemphis.bleed.components.ModComponents;

import java.util.Objects;

public class HuntManager {

    public static void startHunt(ServerPlayerEntity hunter, ServerPlayerEntity target, int tier) {
        HuntComponent hunt = ModComponents.HUNT.get(hunter);
        hunt.setHunting(true);
        hunt.setTargetName(target.getName().getString());
        hunt.setTier(tier);

        hunt.setStartTime(Objects.requireNonNull(hunter.getEntityWorld().getServer()).getTicks());

        ModComponents.HUNT.sync(hunter);

        hunter.sendMessage(Text.literal("Target Acquired: " + target.getName().getString())
                .formatted(Formatting.GOLD), false);
    }

    public static void stopHunt(ServerPlayerEntity hunter, boolean success) {
        HuntComponent data = ModComponents.HUNT.get(hunter);

        if (!data.isHunting()) return;

        int tier = data.getTier();
        String targetName = data.getTargetName();

        if (success) {
            handleSuccess(hunter, targetName, tier);
        } else {
            handleFailure(hunter, tier);
        }

        // Component reset
        HuntComponent hunt = ModComponents.HUNT.get(hunter);
        hunt.setHunting(false);
        hunt.setTargetName("");
        hunt.setTier(0);
        ModComponents.HUNT.sync(hunter);
    }

    private static void handleFailure(ServerPlayerEntity hunter, int tier) {
        HuntComponent data = ModComponents.HUNT.get(hunter);
        if (tier == 1) {
            long currentTicks = Objects.requireNonNull(hunter.getEntityWorld().getServer()).getTicks();
            data.setLastFailure(currentTicks);
            hunter.sendMessage(Text.literal("Failed, 30m cooldown").formatted(Formatting.RED));

        } else if (tier == 2) {
            data.setT2Drop(true);

            hunter.sendMessage(Text.literal("Tier 2 failed, t2 dropped on next death").formatted(Formatting.RED));
        } else if (tier == 3) {
            var health = hunter.getAttributeInstance(EntityAttributes.MAX_HEALTH);
            if (health != null) {
                double currentBaseHealth = health.getBaseValue();
                health.setBaseValue(Math.max(2.0, currentBaseHealth - 4.0));
            }
            hunter.sendMessage(Text.literal("T3 Hunt failed").formatted(Formatting.RED));
        }
        ModComponents.HUNT.sync(hunter);
    }

    private static void handleSuccess(ServerPlayerEntity hunter, String targetName, int tier) {
        hunter.sendMessage(Text.literal("Contract completed.").formatted(Formatting.GREEN));

        if (tier == 3) {
            var server = hunter.getEntityWorld().getServer();
            if (server == null) return;

            // server announcement
            ServerPlayerEntity target = hunter.getEntityWorld().getServer().getPlayerManager().getPlayer(targetName);
            if (target != null) {
                Text announcement = Text.literal(target.getName().getString()
                                .formatted(Formatting.WHITE, Formatting.BOLD))
                        .append(Text.literal(" has been eliminated by ")
                                .formatted(Formatting.GRAY))
                        .append(Text.literal(hunter.getName().getString())
                                .formatted(Formatting.GOLD, Formatting.BOLD))
                        .append(Text.literal(" in a Tier 3 Hunt!")
                                .formatted(Formatting.GRAY));

                // sound
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    player.playSound(SoundEvents.ENTITY_WITHER_SPAWN, 1, 0.5f);
                }

//                net.minecraft.entity.LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(, target.getEntityWorld()); problem for future me

                server.getPlayerManager().broadcast(announcement, false);

                // ban
                target.networkHandler.disconnect(Text.literal("You were eliminated in a Tier 3 Hunt."));
                var banList = hunter.getEntityWorld().getServer().getPlayerManager().getUserBanList();
                net.minecraft.server.PlayerConfigEntry configEntry = new net.minecraft.server.PlayerConfigEntry(target.getGameProfile());
                BannedPlayerEntry banEntry = new BannedPlayerEntry(
                        configEntry,
                        new java.util.Date(),
                        "Hunt System",
                        null,
                        "Eliminated in a Tier 3 Hunt."
                );

                banList.add(banEntry);
                target.networkHandler.disconnect(Text.literal("You were eliminated."));
            }
        }
    }
}