package net.zemphis.bleed.components;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import org.ladysnake.cca.api.v3.component.Component;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;


public class HuntComponentImpl implements HuntComponent, AutoSyncedComponent {
    private boolean hunting = false;
    private String targetName = "";
    private int tier = 0;
    private long startTime = 0L;
    private long lastFailure = -1L;
    private boolean t2Drop = false;

    @Override public boolean isHunting() { return hunting; }
    @Override public void setHunting(boolean hunting) { this.hunting = hunting; }

    @Override public String getTargetName() { return targetName; }
    @Override public void setTargetName(String name) { this.targetName = name; }

    @Override public int getTier() { return tier; }
    @Override public void setTier(int tier) { this.tier = tier; }


    @Override public long getStartTime() { return startTime; }
    @Override public void setStartTime(long ticks) { this.startTime = ticks; }

    @Override public boolean shouldDropT2() { return t2Drop; }
    @Override public void setT2Drop(boolean value) { this.t2Drop = value; }

    @Override public long getLastFailure() { return lastFailure; }
    @Override public void setLastFailure(long ticks) { this.lastFailure = ticks; }

    @Override
    public void readData(ReadView view) {
        this.hunting = view.getBoolean("isHunting", false);
        this.targetName = view.getString("targetName", "");
        this.tier = view.getInt("tier", 0);
        this.startTime = view.getLong("startTime", 0L);
        this.t2Drop = view.getBoolean("t2Drop", false);
        this.lastFailure = view.getLong("lastFailure", -1L);
    }

    @Override
    public void writeData(WriteView view) {
        view.putBoolean("isHunting", hunting);
        view.putString("targetName", targetName);
        view.putInt("tier", tier);
        view.putLong("startTime", startTime);
        view.putBoolean("t2Drop", t2Drop);
        view.putLong("lastFailure", lastFailure);
    }
}