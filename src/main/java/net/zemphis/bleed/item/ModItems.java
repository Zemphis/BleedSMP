package net.zemphis.bleed.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.zemphis.bleedsmp.BleedSMP;

public class ModItems {

    public static final Item TIER_I_CONTRACT = new ContractItem(createSettings("tier_i_contract"));
    public static final Item TIER_II_CONTRACT = new ContractItem(createSettings("tier_ii_contract"));
    public static final Item TIER_III_CONTRACT = new ContractItem(createSettings("tier_iii_contract"));

    private static Item registerItem(String name, Item item) {
        Identifier id = Identifier.of(BleedSMP.MOD_ID, name);
        return Registry.register(Registries.ITEM, id, item);
    }

    public static void registerModItems() {
        registerItem("tier_i_contract", TIER_I_CONTRACT);
        registerItem("tier_ii_contract", TIER_II_CONTRACT);
        registerItem("tier_iii_contract", TIER_III_CONTRACT);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.add(TIER_I_CONTRACT);
            entries.add(TIER_II_CONTRACT);
            entries.add(TIER_III_CONTRACT);
        });
    }

    private static Item.Settings createSettings(String id) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(BleedSMP.MOD_ID, id));
        return new Item.Settings().registryKey(key);
    }
}
