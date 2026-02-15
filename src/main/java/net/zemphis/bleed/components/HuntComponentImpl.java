package net.zemphis.bleed.components;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import org.ladysnake.cca.api.v3.component.Component;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;


public class HuntComponentImpl implements HuntComponent {
    private boolean hunting = false;
    private String targetName = "";
    private int tier = 0;

    @Override public boolean isHunting() { return hunting; }
    @Override public void setHunting(boolean hunting) { this.hunting = hunting; }

    @Override public String getTargetName() { return targetName; }
    @Override public void setTargetName(String name) { this.targetName = name; }

    @Override public int getTier() { return tier; }
    @Override public void setTier(int tier) { this.tier = tier; }

    @Override
    public void readData(ReadView view) {
        this.hunting = view.getBoolean("isHunting", false);
        this.targetName = view.getString("targetName", "");
        this.tier = view.getInt("tier", 0);
    }

    @Override
    public void writeData(WriteView view) {
        view.putBoolean("isHunting", hunting);
        view.putString("targetName", targetName);
        view.putInt("tier", tier);
    }
}