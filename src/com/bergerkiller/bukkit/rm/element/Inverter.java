package com.bergerkiller.bukkit.rm.element;

public class Inverter extends Redstone {

    @Override
    public boolean hasPower() {
        return !isPowered();
    }

    @Override
    public byte getType() {
        return 1;
    }
}
