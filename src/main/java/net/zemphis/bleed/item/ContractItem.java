package net.zemphis.bleed.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.zemphis.bleed.hunt.HuntManager;
import net.zemphis.bleed.hunt.HuntUtils;
import net.zemphis.bleedsmp.BleedSMP;

import java.util.Objects;
import java.util.function.Consumer;

public class ContractItem extends Item {
    public ContractItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient()) return ActionResult.PASS;

        if (!(user instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
        if (HuntUtils.isHunting(serverPlayer)) {
                user.sendMessage(Text.literal("You are already on a hunt!").formatted(Formatting.RED), true);
                return ActionResult.FAIL;
        }


        String targetName = getOwner(stack);

        // Signing logic
        if (targetName.isEmpty()) {
            setOwner(stack, user.getName().getString(), 1);
            user.sendMessage(Text.literal("Contract signed for target: " + user.getName().getString()), true);
            return ActionResult.SUCCESS;
        }

        // Self check
        if (user.getName().getString().equals(targetName)) {
            user.sendMessage(Text.literal("Contract is yours").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        // Activation logic
        long currentTime = world.getTime();
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound nbt = data != null ? data.copyNbt() : new NbtCompound();

        long lastUsed = nbt.getLong("LastUsedTime").orElse(0L);
        long cooldownTicks = 7 * 24000L;

        ServerPlayerEntity targetPlayer = Objects.requireNonNull(serverPlayer.getEntityWorld().getServer()).getPlayerManager().getPlayer(targetName);

        if (lastUsed != 0 && currentTime < lastUsed + cooldownTicks) {
            long remainingDays = ((lastUsed + cooldownTicks) - currentTime) / 24000L;
            user.sendMessage(Text.literal("Contract on cooldown. " + remainingDays + " days left.").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        } else if (targetPlayer == null) {
            user.sendMessage(Text.literal("Target is not online!").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        if (getTier(stack) == 3) {
            nbt.putBoolean("IsActiveT3", true);
        }

        HuntManager.startHunt(serverPlayer, targetPlayer, getTier(stack));

        nbt.putLong("LastUsedTime", currentTime);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

        user.sendMessage(Text.literal("Hunt started: Target is " + targetName).formatted(Formatting.GOLD), true);

        return ActionResult.SUCCESS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent data, Consumer<Text> tooltip, TooltipType type) {
        String owner = getOwner(stack);
        if (!owner.isEmpty()) {
            tooltip.accept(Text.literal("Bound to " + owner).formatted(Formatting.RED));
        } else {
            tooltip.accept(Text.literal("Unbound").formatted(Formatting.GRAY));
        }
        super.appendTooltip(stack, context, data, tooltip, type);
    }

    public void setOwner(ItemStack stack, String ownerName, int tier) {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("Owner", ownerName);
        nbt.putInt("Tier", tier);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
    }

    public String getOwner(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data != null) {
            return data.copyNbt().getString("Owner").orElse("");
        }
        return "";
    }

    public int getTier(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data != null) {
            return data.copyNbt().getInt("Tier").orElse(1);
        }
        return 1;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}