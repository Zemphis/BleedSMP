package net.zemphis.bleed.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.zemphis.bleedsmp.BleedSMP;

public class ModItems {

    public static final Item TIER_I_CONTRACT = registerItem("tier_i_contract", new ContractItem(new Item.Settings()));
    public static final Item TIER_II_CONTRACT = registerItem("tier_ii_contract", new ContractItem(new Item.Settings()));
    public static final Item TIER_III_CONTRACT = registerItem("tier_iii_contract", new ContractItem(new Item.Settings()));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(BleedSMP.MOD_ID, name), item);
    }

    public static void registerModItems() {
        BleedSMP.LOGGER.info("Registering Mod Items for " + BleedSMP.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.add(TIER_I_CONTRACT);
            entries.add(TIER_II_CONTRACT);
            entries.add(TIER_III_CONTRACT);
        });
    }
}
