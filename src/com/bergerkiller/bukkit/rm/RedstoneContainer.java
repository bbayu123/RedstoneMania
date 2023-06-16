package com.bergerkiller.bukkit.rm;

import com.bergerkiller.bukkit.rm.element.Component;

public class RedstoneContainer {
    public Component value;
    private RedstoneMap map;

    public RedstoneContainer(RedstoneMap map) {
        this.map = map;
    }

    /**
     * Sets the Redstone value
     * 
     * @param value to set to
     * @return The input Value
     */
    public <T extends Component> T setValue(T value) {
        this.value = value;
        map.setValue(this, value);
        return value;
    }
}
