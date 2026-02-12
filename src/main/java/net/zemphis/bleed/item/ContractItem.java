package net.zemphis.bleed.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import javax.xml.crypto.Data;
import java.util.List;

public class ContractItem extends Item {
    public ContractItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient) return TypedActionResult.pass(stack);

        String targetName = getOwner(stack);

        // Signing logic
        if (targetName.isEmpty()) {
            setOwner(stack, user.getName().getString(), 1);
            user.sendMessage(Text.literal("Contract signed for target: " + user.getName().getString()), true);
            return TypedActionResult.success(stack);
        }

        if (user.getName().getString().equals(targetName)) {
            user.sendMessage(Text.literal("Contract is yours"), true);
            return TypedActionResult.fail(stack);
        }

        // Activation logic
        long currentTime = world.getTime();
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound nbt = data != null ? data.copyNbt() : new NbtCompound();

        long lastUsed = nbt.getLong("LastUsedTime");
        long cooldownTicks = 7*24000;

        if (currentTime < lastUsed + cooldownTicks) {
            long remainingDays = ((lastUsed + cooldownTicks) - currentTime) / 24000;
            user.sendMessage(Text.literal("Contract on cooldown. " + remainingDays + " days left.").formatted(Formatting.RED), true);
            return TypedActionResult.fail(stack);
        }

        startHunt(user, targetName, getTier(stack), currentTime);

        nbt.putLong("LastUsedTime", currentTime);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

        user.sendMessage(Text.literal("Hunt started: Target is " + targetName).formatted(Formatting.GOLD), true);
        return TypedActionResult.success(stack);
    }

    private void startHunt(PlayerEntity hunter, String target, int tier, long startTime) {
        if (hunter instanceof ServerPlayerEntity serverPlayer) {
            NbtCompound hunterNbt = new NbtCompound();
            hunterNbt.putString("HuntingTarget", target);
            hunterNbt.putInt("HuntingTier", tier);
            hunterNbt.putLong("HuntingStartTime", startTime);
            hunterNbt.putBoolean("IsHunting", true);

            serverPlayer.writeCustomDataToNbt(hunterNbt);
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, net.minecraft.item.tooltip.TooltipType type) {
        String owner = getOwner(stack);
        if (!owner.isEmpty()) {
            tooltip.add(Text.literal("Bound to " + owner).formatted(Formatting.RED));
        } else {
            tooltip.add(Text.literal("Unbound").formatted(Formatting.GRAY));
        }
        super.appendTooltip(stack, context, tooltip, type);
    }

    public void setOwner(ItemStack stack, String ownerName, int tier) {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("Owner", ownerName);
        nbt.putInt("Tier", tier);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true); // sneaky zemphis watermark
    }

    public String getOwner(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data != null) {
            return data.copyNbt().getString("Owner");
        }
        return "";
    }

    public int getTier(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data != null) {
            return data.copyNbt().getInt("Tier");
        }
        return 1;
    }


    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
