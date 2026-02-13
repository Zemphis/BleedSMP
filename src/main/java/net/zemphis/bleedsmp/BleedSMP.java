package net.zemphis.bleedsmp;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.zemphis.bleed.block.ModBlocks;
import net.zemphis.bleed.effect.ModEffects;
import net.zemphis.bleed.item.ContractItem;
import net.zemphis.bleed.item.ModItems;
import net.zemphis.bleed.screen.ModScreenHandlers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BleedSMP implements ModInitializer {
	public static final String MOD_ID = "bleedsmp";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.registerModItems();
		ModBlocks.registerModBlocks();
		ModScreenHandlers.registerScreenHandlers();
		ModEffects.registerEffects();

		ServerLivingEntityEvents.AFTER_DEATH.register((livingEntity, damageSource) -> {
			if (livingEntity instanceof ServerPlayerEntity player) {
				// any player dies
				ItemStack contractStack = new ItemStack(ModItems.TIER_I_CONTRACT);
				if (contractStack.getItem() instanceof ContractItem contractItem) {
					contractItem.setOwner(contractStack, player.getName().getString(), 1);
				}
				player.dropItem(contractStack, true, false);

				// target dies
				for (ServerPlayerEntity hunter : player.server.getPlayerManager().getPlayerList()) {
					if (isCurrentlyHunting(hunter)) {
						String targetName = getHunterTargetName(hunter);

						if (player.getName().getString().equals(targetName)) {
							endHunt(hunter, getHunterTier(hunter), true);
						}
					}
				}

				// hunter dies
				if (isCurrentlyHunting(player)) {
					int tier = getHunterTier(player);
					endHunt(player, tier, false);
					player.sendMessage(Text.literal("You died during the hunt! Contract failed.").formatted(Formatting.RED), false);

					if (tier == 2) {
						ItemStack t2Contract = new ItemStack(ModItems.TIER_II_CONTRACT);
						if (t2Contract.getItem() instanceof ContractItem contractItem) {
							contractItem.setOwner(t2Contract, player.getName().getString(), 2);
						}
						player.dropItem(t2Contract, true, false);
					}
				}

			}
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			long huntDuration = 18000;

			for (ServerPlayerEntity hunter : server.getPlayerManager().getPlayerList()) {
				if (isCurrentlyHunting(hunter)) {
					applyHuntBuffs(hunter, getHunterTier(hunter));
				}
			}
		});

		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (!world.isClient && player instanceof ServerPlayerEntity serverHunter) {
				if (entity instanceof ServerPlayerEntity target) {
					if (isCurrentlyHunting(serverHunter)) {
						String huntTarget = getHunterTargetName(serverHunter);
						if (target.getName().getString().equals(huntTarget)) {
							int tier = getHunterTier(serverHunter);
							if (tier == 3) {
								// bloodlust status effect
								serverHunter.addStatusEffect(new StatusEffectInstance(ModEffects.BLOODLUST, 100, 0, false, false, true));
								float damage = (float) serverHunter.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE);
//								float absorptionGain = damage; * 0.5F if needed for balance
								float currentAbsorption = serverHunter.getAbsorptionAmount();
								serverHunter.setAbsorptionAmount(Math.min(currentAbsorption + damage, 20.0f));

								serverHunter.getServerWorld().spawnParticles(
										ParticleTypes.HEART, serverHunter.getX(), serverHunter.getY() + 1.5,
										serverHunter.getZ(), 3,0.2,0.2,0.2, 0.1);

								// bleeding effect

							}
							applyTieredDebuffs(target, getHunterTier(serverHunter)); // on hit tiered debuffs
						}
					}
				}
			}

			return ActionResult.PASS;
		});

		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayerEntity disconnectedPlayer = handler.getPlayer();
			String disconnectedName = disconnectedPlayer.getName().getString();

			for (ServerPlayerEntity hunter : server.getPlayerManager().getPlayerList()) {
				if (isCurrentlyHunting(hunter) && getHunterTier(hunter) == 3) {
					if (getHunterTargetName(hunter).equals(disconnectedName)) {
						disconnectedPlayer.kill();
						LOGGER.info("Target {} logged out during a Tier III hunt and was penalized.", disconnectedName);

						endHunt(hunter, 3, true);
						hunter.sendMessage(Text.literal("Target combat logged! Hunt successful.").formatted(Formatting.GREEN), false);
					}
				}
			}
		});
	}

	private boolean isCurrentlyHunting(ServerPlayerEntity player) {
		NbtCompound nbt = new NbtCompound();
		player.writeCustomDataToNbt(nbt);

		if (!nbt.getBoolean("IsHunting")) return false;

		long startTime = nbt.getLong("HuntingStartTime");
		long currentTime = player.getWorld().getTime();

		if (currentTime > startTime + 18000) {
			nbt.putBoolean("IsHunting", false);
			player.readCustomDataFromNbt(nbt);
			return false;
		}
		return true;
	}

	private String getHunterTargetName(ServerPlayerEntity player) {
		NbtCompound nbt = new NbtCompound();
		player.readCustomDataFromNbt(nbt);
		return nbt.getString("HuntingTarget");
	}

	private int getHunterTier(ServerPlayerEntity player) {
		NbtCompound nbt = new NbtCompound();
		player.readCustomDataFromNbt(nbt);
		return nbt.getInt("HuntingTier");
	}

	private void applyHuntBuffs(ServerPlayerEntity hunter, int tier) {
		String targetName = getHunterTargetName(hunter);
		ServerPlayerEntity target = hunter.server.getPlayerManager().getPlayer(targetName);

		if (target != null && target.getWorld() == hunter.getWorld()) {
			double distanceSq = hunter.squaredDistanceTo(target);

			if (distanceSq < 1024) {
				hunter.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 25, 1, false, false, true));
				target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 25, 0, false, false, true));
			}
		}
	}

	private void applyTieredDebuffs(ServerPlayerEntity target, int tier) {
		if (tier == 1) {
			target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10, 0));
		} else if (tier == 2) {
			target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 10, 1));
			target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 10, 1));
		} else {
			//custom bleeding effect
		}
	}

	private void endHunt(ServerPlayerEntity hunter, int tier, boolean success) {
		NbtCompound nbt = new NbtCompound();
		hunter.writeCustomDataToNbt(nbt);

		nbt.putBoolean("IsHunting", false);
		nbt.putString("HuntingTarget", "");

		if (!success) {
			nbt.putLong("GlobalContractCooldown", hunter.getWorld().getTime() + (3 * 24000));
			if (tier >= 1) {
				// cooldown for 3 irl days
				if (tier >= 2) {
					// drop t2 contract on next death
					if (tier == 3) {
						// hearts loss
						var health = hunter.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH);
						if (health != null) {
							net.minecraft.util.Identifier penaltyId = net.minecraft.util.Identifier.of(MOD_ID, "t3_failure_penalty");

							health.removeModifier(penaltyId);
							health.addPersistentModifier(new net.minecraft.entity.attribute.EntityAttributeModifier(
									penaltyId, -4.0, // 2 hearts
									net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADD_VALUE));
						}
					}
				}
			}
		}

		if (tier == 3) {
			// consume contract
			for (int i = 0; i < hunter.getInventory().size(); i++) {
				ItemStack invStack = hunter.getInventory().getStack(i);
				if (invStack.getItem() instanceof ContractItem) {
					NbtComponent customData = invStack.get(DataComponentTypes.CUSTOM_DATA);
					if (customData != null && customData.copyNbt().getBoolean("IsActiveT3")) {
						invStack.setCount(0);
						hunter.sendMessage(Text.literal("The Tier III Contract has been consumed.").formatted(Formatting.GRAY), false);
						break;
					}
				}
			}
		}
		hunter.readCustomDataFromNbt(nbt);
	}
}