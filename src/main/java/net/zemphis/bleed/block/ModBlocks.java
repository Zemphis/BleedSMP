package net.zemphis.bleed.block;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.zemphis.bleedsmp.BleedSMP;

public class ModBlocks {
    public static final Block CONTRACT_TABLE = registerBlock("contract_table", new ContractTableBlock(AbstractBlock.Settings.create().strength(50.0f, 2.0f).requiresTool().sounds(BlockSoundGroup.NETHER_BRICKS))); // custom block implementation

    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, Identifier.of(BleedSMP.MOD_ID, name), block);
    }

    private static void registerBlockItem(String name, Block block) {
        Registry.register(Registries.ITEM, Identifier.of(BleedSMP.MOD_ID, name), new BlockItem(block, new Item.Settings()));
    }

    public static  void registerModBlocks() {
        BleedSMP.LOGGER.info("Registering Mod Blocks for " + BleedSMP.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(ModBlocks.CONTRACT_TABLE);
        });
    }
}
