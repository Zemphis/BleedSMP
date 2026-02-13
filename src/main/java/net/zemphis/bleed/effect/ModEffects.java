package net.zemphis.bleed.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.zemphis.bleed.effect.BleedingEffect;
import net.zemphis.bleed.effect.BloodlustEffect;

public class ModEffects {
    public static final RegistryEntry<StatusEffect> BLOODLUST = register("bloodlust", new BloodlustEffect());
    public static final RegistryEntry<StatusEffect> BLEEDING = register("bleeding", new BleedingEffect());

    private static RegistryEntry<StatusEffect> register(String name, StatusEffect effect) {
        return Registry.registerReference(Registries.STATUS_EFFECT, Identifier.of("bleedsmp", name), effect);
    }

    public static void registerEffects() {

    }
}