package net.zemphis.bleed.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class BleedingEffect extends StatusEffect {
    public BleedingEffect() {
        super(StatusEffectCategory.HARMFUL, 0x990000);
    }
    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        // Run logic every 20 ticks (1 second)
        return duration % 20 == 0;
    }

    @Override
    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        entity.damage(entity.getDamageSources().magic(), 1.0f + amplifier);
        return true;
    }

}
