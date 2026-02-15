package net.zemphis.bleedsmp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
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
					contractItem.setOwner(contractStack, player.getName().getString(), 1);
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
			}
		});

		// ===================== SERVER TICK EVENT =====================
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayerEntity hunter : server.getPlayerManager().getPlayerList()) {
				if (!HuntUtils.isHunting(hunter)) continue;

				String targetName = HuntUtils.getTargetName(hunter);
				ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetName);

				if (target == null) {
					HuntManager.stopHunt(hunter, false);
					hunter.sendMessage(
							Text.literal("Your target is no longer online. Hunt cancelled.").formatted(Formatting.RED),
							false
					);
					continue;
				}

				applyHuntBuffs(hunter, HuntUtils.getTier(hunter));
			}
		});

		// ===================== ATTACK EVENT =====================
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (!world.isClient() && player instanceof ServerPlayerEntity serverHunter) {
				if (entity instanceof ServerPlayerEntity target && HuntUtils.isHunting(serverHunter)) {
					String huntTarget = HuntUtils.getTargetName(serverHunter);

					if (target.getName().getString().equals(huntTarget)) {
						int tier = HuntUtils.getTier(serverHunter);

						// Tier 3 hunter buffs
						if (tier == 3) {
							serverHunter.addStatusEffect(
									new StatusEffectInstance(ModEffects.BLOODLUST, 100, 0, false, false, true)
							);

							float damage = (float) serverHunter.getAttributeValue(
									net.minecraft.entity.attribute.EntityAttributes.ATTACK_DAMAGE
							);
							serverHunter.setAbsorptionAmount(Math.min(serverHunter.getAbsorptionAmount() + damage, 20f));

							serverHunter.getEntityWorld().spawnParticles(
									ParticleTypes.HEART,
									serverHunter.getX(), serverHunter.getY() + 1.5, serverHunter.getZ(),
									3, 0.2, 0.2, 0.2, 0.1
							);
						}

						applyTieredDebuffs(target, tier);
					}
				}
			}
			return ActionResult.PASS;
		});
	}

	// ===================== BUFFS =====================
	private void applyHuntBuffs(ServerPlayerEntity hunter, int tier) {
		var server = hunter.getEntityWorld().getServer();
		if (server == null) return;

		ServerPlayerEntity target = Objects.requireNonNull(hunter.getEntityWorld().getServer()).getPlayerManager().getPlayer(HuntUtils.getTargetName(hunter));

		if (target != null && target.getEntityWorld() == hunter.getEntityWorld()) {
			if (hunter.squaredDistanceTo(target) < 1024) {
				hunter.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 5, 0, false, false, true));
				target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 5, 0, false, false, true));
			}
		}
	}

	private void applyTieredDebuffs(ServerPlayerEntity target, int tier) {
		if (tier == 1) {
			target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10, 0));
		} else if (tier == 2) {
			target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 10, 1));
			target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 10, 1));
		}
	}
}