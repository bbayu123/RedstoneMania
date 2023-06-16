package com.bergerkiller.bukkit.rm.element;

/**
 * A redstone wire
 * <p>
 * A wire is of type 0.
 * 
 * @author bbayu123
 *
 */
public class Wire extends Component {
    @Override
    public byte getType() {
        return 0;
    }

    @Override
    protected boolean determinePower(boolean mainPowered, boolean sidePowered) {
        return mainPowered;
    }
}
