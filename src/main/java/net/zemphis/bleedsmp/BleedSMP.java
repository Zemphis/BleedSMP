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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.zemphis.bleed.block.ModBlocks;
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

		ServerLivingEntityEvents.AFTER_DEATH.register((livingEntity, damageSource) -> {
			if (livingEntity instanceof ServerPlayerEntity player) {
				ItemStack contractStack = new ItemStack(ModItems.TIER_I_CONTRACT);


				if (contractStack.getItem() instanceof ContractItem contractItem) {
					contractItem.setOwner(contractStack, player.getName().getString(), 1);
				}

				player.dropItem(contractStack, true, false);

				for (ServerPlayerEntity hunter : player.server.getPlayerManager().getPlayerList()) {
					if (isCurrentlyHunting(hunter)) {
						String targetName = getHunterTargetName(hunter);

						if (player.getName().getString().equals(targetName)) {
							endHunt(hunter, getHunterTier(player), true);
						}
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
			if (!world.isClient && entity instanceof ServerPlayerEntity target && player instanceof ServerPlayerEntity serverHunter) {
				if (isCurrentlyHunting(serverHunter)) {
					String huntTarget = getHunterTargetName(serverHunter);
					if (target.getName().getString().equals(huntTarget)) {
						applyTieredDebuffs(target, getHunterTier(serverHunter));
					}
				}
			}

			return ActionResult.PASS;
		});
	}

	private boolean isCurrentlyHunting(ServerPlayerEntity player) {
		NbtCompound nbt = new NbtCompound();
		player.readCustomDataFromNbt(nbt);

		if (!nbt.getBoolean("IsHunting")) return false;

		long startTime = nbt.getLong("HuntingStartTime");
		long currentTime = player.getWorld().getTime();

		if (currentTime > startTime + 18000) {
			nbt.putBoolean("IsHunting", false);
			player.writeCustomDataToNbt(nbt);
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

				if (tier == 3) {
					// bloodlust later
				}
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
		hunter.readCustomDataFromNbt(nbt);

		nbt.putBoolean("IsHunting", false);
		nbt.putString("HuntingTarget", "");

		if (!success) {
			if (tier >= 1) {
				// cooldown for 3 irl days
				if (tier >= 2) {
					// drop t2 contract on death
					if (tier == 3) {
						//consume contract and lose 2 permanent hearts
					}
				}
			}
		}

		if (success && tier == 3) {
			//consume contract
		}
		hunter.writeCustomDataToNbt(nbt);
	}
}