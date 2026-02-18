package net.zemphis.bleed.components;

import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

public interface HuntComponent extends Component, AutoSyncedComponent {
    boolean isHunting();
    void setHunting(boolean hunting);

    String getTargetName();
    void setTargetName(String name);

    int getTier();
    void setTier(int tier);

    long getStartTime();
    void setStartTime(long ticks);

    long getLastFailure();
    void setLastFailure(long ticks);

    boolean shouldDropT2();
    void setT2Drop(boolean value);
}