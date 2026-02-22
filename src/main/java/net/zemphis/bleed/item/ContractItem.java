package net.zemphis.bleed.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.zemphis.bleed.components.ModComponents;
import net.zemphis.bleed.hunt.HuntManager;
import net.zemphis.bleed.hunt.HuntUtils;

import java.util.Objects;
import java.util.function.Consumer;

public class ContractItem extends Item {
    private final int contractTier;

    public ContractItem(Settings settings, int tier) {
        super(settings);
        this.contractTier = tier;
    }

    public int getTier() {
        return this.contractTier;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient() || !(user instanceof ServerPlayerEntity hunter)) return ActionResult.PASS;

        long currentTicks = hunter.getEntityWorld().getServer().getTicks();
        long lastFail = ModComponents.HUNT.get(hunter).getLastFailure();
        if (lastFail >= 0 && currentTicks < lastFail + 36000L) { // 30 mins * 60s * 20 ticks
            hunter.sendMessage(Text.literal("Contract lockout!").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        // Hunt state check and time remaining
        if (HuntUtils.isHunting(hunter)) {
            long huntDuration = 12000L;
            long startTicks = ModComponents.HUNT.get(hunter).getStartTime();
            long elapsedTicks = currentTicks - startTicks;
            long remainingTicks = huntDuration - elapsedTicks;

            if (remainingTicks > 0) {
                long totalSeconds = Math.max(0, remainingTicks / 20);
                long minutes = totalSeconds / 60;
                long seconds = totalSeconds % 60;

                String timeRemaining = String.format("%d:%02d", minutes, seconds);
                user.sendMessage(Text.literal("You are already on a hunt! Time Remaining " +  timeRemaining).formatted(Formatting.RED), true);
            } else {
                HuntManager.stopHunt(hunter, false);
            }
                return ActionResult.FAIL;
        }

        String targetName = getOwner(stack);

        // Signing logic for testing
        if (targetName.isEmpty()) {
            setOwner(stack, user.getName().getString());
            user.sendMessage(Text.literal("Contract signed for target: " + user.getName().getString()), true);
            return ActionResult.SUCCESS;
        }

        // Self check
        if (user.getName().getString().equals(targetName)) {
            user.sendMessage(Text.literal("Contract is yours").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        // Target status check
        ServerPlayerEntity target = Objects.requireNonNull(hunter.getEntityWorld().getServer()).getPlayerManager().getPlayer(targetName);
        if (target == null) {
            user.sendMessage(Text.literal("Target is not online!").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        HuntManager.startHunt(hunter, target,((ContractItem) stack.getItem()).getTier());

        if (((ContractItem) stack.getItem()).getTier() == 3) { // tier 3 contract consume logic fix
            stack.decrement(1);
        }

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

    public void setOwner(ItemStack stack, String ownerName) {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("Owner", ownerName);
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