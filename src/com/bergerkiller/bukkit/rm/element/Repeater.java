package com.bergerkiller.bukkit.rm.element;

/**
 * A redstone repeater
 * <p>
 * A repeater is of type 2
 * 
 * @author bergerkiller
 *
 */
public class Repeater extends Component {

    @Override
    public byte getType() {
        return 2;
    }

    @Override
    protected boolean determinePower(boolean mainPowered, boolean sidePowered) {
        return sidePowered ? powered : mainPowered;
    }
}
