package com.bergerkiller.bukkit.rm.element;

/**
 * A redstone inverter
 * <p>
 * An inverter is of type 1
 * 
 * @author bergerkiller
 *
 */
public class Inverter extends Component {

    @Override
    public boolean hasPower() {
        return !isPowered();
    }

    @Override
    public byte getType() {
        return 1;
    }
}
