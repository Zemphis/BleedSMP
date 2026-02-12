package net.zemphis.bleed.screen;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Items;
import net.zemphis.bleed.block.ModBlocks;
import net.minecraft.item.Item;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.world.World;
import net.zemphis.bleed.item.ModItems;

public class ContractScreenHandler extends ScreenHandler {
    private final CraftingInventory input = new CraftingInventory(this, 3, 3);
    private final CraftingResultInventory result = new CraftingResultInventory();
    private final ScreenHandlerContext context;
    private final PlayerEntity player;

    public ContractScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
    }

    public ContractScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(ModScreenHandlers.CONTRACT_SCREEN_HANDLER, syncId);
        this.context = context;
        this.player = playerInventory.player;

        // 1. Output Slot
        this.addSlot(new Slot(this.result, 0, 124, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }

            @Override
            public void onTakeItem(PlayerEntity player, ItemStack stack) {
                // Decrement all 9 items in the grid by 1
                for (int i = 0; i < 9; ++i) {
                    ItemStack itemStack = ContractScreenHandler.this.input.getStack(i);
                    if (!itemStack.isEmpty()) {
                        itemStack.decrement(1);
                    }
                }
                // Refresh grid
                ContractScreenHandler.this.onContentChanged(ContractScreenHandler.this.input);
                super.onTakeItem(player, stack);
            }
        });

        // 2. 3x3 Crafting Grid
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                this.addSlot(new Slot(this.input, j + i * 3, 30 + j * 18, 17 + i * 18));
            }
        }

        // 3. Player Inventory & Hotbar
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public void onContentChanged(Inventory inventory) {
        this.context.run((world, pos) -> {
            updateResult(this, world, this.player, this.input, this.result);
        });
    }

    protected static void updateResult(ScreenHandler handler, World world, PlayerEntity player, CraftingInventory inventory, CraftingResultInventory resultInventory) {
        if (world.isClient) return;

        // Tier II Contract
        boolean isTier2Pattern = inventory.getStack(0).isOf(Items.DIAMOND_BLOCK) &&
                inventory.getStack(1).isOf(Items.DIAMOND_BLOCK) &&
                inventory.getStack(2).isOf(Items.DIAMOND_BLOCK) &&
                inventory.getStack(3).isOf(ModItems.TIER_I_CONTRACT) &&
                inventory.getStack(4).isOf(Items.DIAMOND_BLOCK) &&
                inventory.getStack(5).isOf(ModItems.TIER_I_CONTRACT) &&
                inventory.getStack(6).isOf(Items.DIAMOND_BLOCK) &&
                inventory.getStack(7).isOf(Items.DIAMOND_BLOCK) &&
                inventory.getStack(8).isOf(Items.DIAMOND_BLOCK);


        if (isTier2Pattern) {
                //check nbt match
                if (areContractsCompatible(inventory.getStack(3), inventory.getStack(5))) {
                    ItemStack result = new ItemStack(ModItems.TIER_II_CONTRACT);
                    copyOwnerData(inventory.getStack(3), result);
                    resultInventory.setStack(0, result);
                    return;
                }
        }

        // Tier III Contract
        boolean isTier3Pattern = inventory.getStack(0).isOf(Items.NETHERITE_INGOT) &&
                inventory.getStack(1).isOf(Items.NETHERITE_INGOT) &&
                inventory.getStack(2).isOf(Items.NETHERITE_INGOT) &&
                inventory.getStack(3).isOf(ModItems.TIER_II_CONTRACT) &&
                inventory.getStack(4).isOf(Items.NETHERITE_INGOT) &&
                inventory.getStack(5).isOf(ModItems.TIER_II_CONTRACT) &&
                inventory.getStack(6).isOf(Items.NETHERITE_INGOT) &&
                inventory.getStack(7).isOf(Items.NETHERITE_INGOT) &&
                inventory.getStack(8).isOf(Items.NETHERITE_INGOT);

        if (isTier3Pattern) {
            //check nbt match
            if (areContractsCompatible(inventory.getStack(3), inventory.getStack(5))) {
                ItemStack result = new ItemStack(ModItems.TIER_III_CONTRACT);
                copyOwnerData(inventory.getStack(3), result);
                resultInventory.setStack(0, result);
                return;
            }
        }

        resultInventory.setStack(0, ItemStack.EMPTY);
    }

    private static boolean areContractsCompatible(ItemStack stack1, ItemStack stack2) {
        String owner1 = getContractOwner(stack1);
        String owner2 = getContractOwner(stack2);
        return owner1.equals(owner2);
    }

    private static String getContractOwner(ItemStack stack) {
        NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbtComponent != null) {
            return nbtComponent.copyNbt().getString("Owner");
        }
        return "";
    }

    private static void copyOwnerData(ItemStack source, ItemStack target) {
        NbtComponent sourceData = source.get(DataComponentTypes.CUSTOM_DATA);
        if (sourceData != null) {
            target.set(DataComponentTypes.CUSTOM_DATA, sourceData);
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return canUse(this.context, player, ModBlocks.CONTRACT_TABLE);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);

        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (invSlot < 10) {
                // If item is in the 3x3 Grid or Output (Slots 0-9), move to Player Inventory
                if (!this.insertItem(originalStack, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // If item is in Player Inventory, move to 3x3 Grid (Slots 0-9)
                if (!this.insertItem(originalStack, 0, 9, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }

            if (originalStack.getCount() == newStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTakeItem(player, originalStack);
        }

        return newStack;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.context.run((world, pos) -> {
            this.dropInventory(player, this.input);
        });
    }
}