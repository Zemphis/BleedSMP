package net.zemphis.bleed.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class BloodlustEffect extends StatusEffect {
    public BloodlustEffect() {
        super(StatusEffectCategory.BENEFICIAL, 0xFF0000);
    }
}