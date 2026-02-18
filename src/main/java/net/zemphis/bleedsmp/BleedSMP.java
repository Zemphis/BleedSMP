package net.zemphis.bleedsmp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.zemphis.bleed.block.ModBlocks;
import net.zemphis.bleed.components.ModComponents;
import net.zemphis.bleed.effect.ModEffects;
import net.zemphis.bleed.item.ContractItem;
import net.zemphis.bleed.item.ModItems;
import net.zemphis.bleed.screen.ModScreenHandlers;
import net.zemphis.bleed.hunt.HuntManager;
import net.zemphis.bleed.hunt.HuntUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class BleedSMP implements ModInitializer {

	public static final String MOD_ID = "bleedsmp";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.registerModItems();
		ModBlocks.registerModBlocks();
		ModScreenHandlers.registerScreenHandlers();
		ModEffects.registerEffects();

		// ===================== DEATH EVENT =====================
		ServerLivingEntityEvents.AFTER_DEATH.register((livingEntity, damageSource) -> {
			if (livingEntity instanceof ServerPlayerEntity player) {

				// Drop contract
				ItemStack contractStack = new ItemStack(ModItems.TIER_I_CONTRACT);
				if (contractStack.getItem() instanceof ContractItem contractItem) {
					contractItem.setOwner(contractStack, player.getName().getString());
				}
				player.dropItem(contractStack, true, false);

				// Notify hunters using HuntUtils
				Objects.requireNonNull(player.getEntityWorld().getServer()).getPlayerManager().getPlayerList().forEach(hunter -> {
					if (HuntUtils.isHunting(hunter) &&
							HuntUtils.getTargetName(hunter).equals(player.getName().getString())) {
						HuntManager.stopHunt(hunter, true);
					}
				});

				// Hunter dies
				if (HuntUtils.isHunting(player)) {
					HuntManager.stopHunt(player, false);
					player.sendMessage(
							Text.literal("You died during the hunt! Contract failed.").formatted(Formatting.RED),
							false
					);
				}

				if (HuntUtils.shouldDropT2(player)) {
					player.dropItem(new ItemStack(ModItems.TIER_II_CONTRACT), true, false);
					ModComponents.HUNT.get(player).setT2Drop(false); // Debt cleared
					ModComponents.HUNT.sync(player);
				}
			}
		});

		// ===================== SERVER TICK EVENT =====================
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayerEntity hunter : server.getPlayerManager().getPlayerList()) {
				if (!HuntUtils.isHunting(hunter)) continue;

				String targetName = HuntUtils.getTargetName(hunter);
				ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetName);

				long huntDuration = 12000L;
				long startTicks = ModComponents.HUNT.get(hunter).getStartTime();
				long currentTicks = server.getTicks();

				if (currentTicks - startTicks >= huntDuration) {
					HuntManager.stopHunt(hunter, false);
					hunter.sendMessage(Text.literal("Time's up! The target survived. Hunt Failed.")
							.formatted(Formatting.RED), false);
					continue;
				}
			}
		});

		// ===================== DISCONNECT EVENT =====================
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayerEntity player = handler.getPlayer();

			// hunter log out
			if (HuntUtils.isHunting(player)) {
				HuntManager.stopHunt(player, false);
			}

			// target log out
			server.getPlayerManager().getPlayerList().forEach(hunter -> {
				if (HuntUtils.isHunting(hunter) && HuntUtils.getTargetName(hunter).equals(player.getName().getString())) {
					HuntManager.stopHunt(player, true);
					hunter.sendMessage(Text.literal("Target logged out. Hunt successful.").formatted(Formatting.GRAY));
				}
			});
		});

		// ===================== ATTACK EVENT =====================
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (!world.isClient() && player instanceof ServerPlayerEntity serverHunter) {
				if (entity instanceof ServerPlayerEntity target && HuntUtils.isHunting(serverHunter)) {
					String huntTarget = HuntUtils.getTargetName(serverHunter);

					if (entity instanceof ServerPlayerEntity victim && HuntUtils.isHunting(serverHunter)) {
						int tier = HuntUtils.getTier(serverHunter);
						serverHunter.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20, 0, false, false, true));
						victim.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 400, 0, false, false, true));

						if (tier >= 2) {
							victim.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 1));
						}

						serverHunter.getEntityWorld().spawnParticles(
								ParticleTypes.SWEEP_ATTACK,
								victim.getX(), victim.getY() + 1.0, victim.getZ(),
								1, 0, 0, 0, 0
						);
					}
				}
			}
			return ActionResult.PASS;
		});

		// ===================== COMMAND EVENT =====================
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("huntadmin")
							.requires(source ->
									source.getPermissions().hasPermission(
											new Permission.Level(PermissionLevel.GAMEMASTERS)
									)
							)

							.then(CommandManager.literal("clearLockout")
									.then(CommandManager.argument("target", EntityArgumentType.player())
											.executes(context -> {
												ServerPlayerEntity target =
														EntityArgumentType.getPlayer(context, "target");

												var data = ModComponents.HUNT.get(target);
												data.setLastFailure(-1L);
												ModComponents.HUNT.sync(target);

												context.getSource().sendFeedback(
														() -> Text.literal("Cleared hunt lockout for "
																+ target.getName().getString()),
														true
												);
												return 1;
											})
									)
							)

							.then(CommandManager.literal("clearDebt")
									.then(CommandManager.argument("target", EntityArgumentType.player())
											.executes(context -> {
												ServerPlayerEntity target =
														EntityArgumentType.getPlayer(context, "target");

												var data = ModComponents.HUNT.get(target);
												data.setT2Drop(false);
												ModComponents.HUNT.sync(target);

												context.getSource().sendFeedback(
														() -> Text.literal("Cleared Tier 2 debt for "
																+ target.getName().getString()),
														true
												);
												return 1;
											})
									)
							)

							.then(CommandManager.literal("stopHunt")
									.then(CommandManager.argument("target", EntityArgumentType.player())
											.executes(context -> {
												ServerPlayerEntity target =
														EntityArgumentType.getPlayer(context, "target");

												HuntManager.stopHunt(target, true);

												context.getSource().sendFeedback(
														() -> Text.literal("Force-stopped hunt for "
																+ target.getName().getString()),
														true
												);
												return 1;
											})
									)
							)
			);
		});

	}
}