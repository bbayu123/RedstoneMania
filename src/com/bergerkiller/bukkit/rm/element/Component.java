package com.bergerkiller.bukkit.rm.element;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.block.Block;

import com.bergerkiller.bukkit.rm.RedstoneMania;
import com.bergerkiller.bukkit.rm.circuit.CircuitBase;

/**
 * A redstone component
 * 
 * @author bergerkiller
 * @author bbayu123
 *
 */
public abstract class Component {
    /**
     * The current power state
     */
    private boolean powered = false;
    /**
     * The ID of the redstone component
     */
    private int id = -1;
    /**
     * The delay between input and output
     */
    private int delay = 0;
    /**
     * The circuit that this redstone component belongs to
     */
    private CircuitBase circuit;

    /**
     * A set of components that provide input to this component
     */
    public Set<Component> inputs = new HashSet<>();
    /**
     * A set of components that this component outputs to
     */
    public Set<Component> outputs = new HashSet<>();
    /**
     * The maximum allowed updates per tick
     */
    private final int burnoutValue = 5;
    /**
     * The relative location offset of this component
     */
    private short x, z;

    /**
     * The current countdown of the delay
     */
    private int setdelay = 0;
    /**
     * The power state to set after the current delay
     */
    private boolean setpowered = false;
    /**
     * Whether there currently is power from the inputs
     */
    private boolean inputpower = false;
    /**
     * The current counter for the burnout
     */
    private int burnoutCounter = burnoutValue;

    /**
     * Called every tick to update the state of this component
     */
    public void onTick() {
        if (setdelay > 0) {
            if (--setdelay == 0) {
                this.setPowered(setpowered, false);
            }
        }
        burnoutCounter = burnoutValue;
    }

    /**
     * Updates the outputs using the inputs, returns if this element changed
     * 
     * @return if this element has changed
     */
    public final boolean update() {
        // we don't have to update inactive elements!
        if (isDisabled()) {
            if (getType() != 3) {
                return false;
            }
        }
        // check if the opposite is the new result
        boolean hasinput = false;
        for (Component input : inputs) {
            if (input.hasPower()) {
                hasinput = true;
                break;
            }
        }
        if (inputpower && !hasinput) {
            inputpower = false;
            this.setPowered(false, true);
        } else if (!inputpower && hasinput) {
            inputpower = true;
            this.setPowered(true, true);
        } else {
            return false;
        }
        return true;
    }

    /**
     * Called when the power state has changed
     */
    public void onPowerChange() {
        for (Component output : outputs) {
            output.update();
        }
    }

    /**
     * Find any output component that is directly connected to one of the input component
     * 
     * @return the output component that is found, or null if none is found
     */
    public final Component findDirectConnection() {
        if (getDelay() == 0 && inputs.size() > 0 && outputs.size() > 0) {
            for (Component input : inputs) {
                if (input.getDelay() == 0 && outputs.contains(input)) {
                    return input;
                }
            }
        }
        return null;
    }

    /**
     * Transfers inputs and outputs to another component
     * 
     * @param to the component to transfer to
     */
    public final void transfer(Component to) {
        for (Component input : inputs) {
            input.outputs.remove(this);
            if (to != input) {
                input.outputs.add(to);
                to.inputs.add(input);
            }
        }
        for (Component output : outputs) {
            output.inputs.remove(this);
            if (to != output) {
                output.inputs.add(to);
                to.outputs.add(output);
            }
        }
        inputs.clear();
        outputs.clear();
    }

    /**
     * Connect this component's output to another component's input
     * 
     * @param redstone the component to connect to
     */
    public final void connectTo(Component redstone) {
        if (redstone == this) return;
        outputs.add(redstone);
        redstone.inputs.add(this);
    }

    /**
     * Disconnect this component from another component
     * 
     * @param redstone the component to disconnect from
     */
    public final void disconnect(Component redstone) {
        inputs.remove(redstone);
        outputs.remove(redstone);
        redstone.inputs.remove(this);
        redstone.outputs.remove(this);
    }

    /**
     * Disconnect this component's output from another component's input
     * 
     * @param redstone the component to disconnect from
     */
    public final void disconnectFrom(Component redstone) {
        if (redstone == this) return;
        outputs.remove(redstone);
        redstone.inputs.remove(this);
    }

    /**
     * Disconnect all connected components
     */
    public final void disconnectAll() {
        for (Component r : inputs) {
            r.outputs.remove(this);
        }
        for (Component r : outputs) {
            r.inputs.remove(this);
        }
        inputs.clear();
        outputs.clear();
    }

    /**
     * Gets whether this component is connected to (outputs to) another component
     * 
     * @param redstone the component to check connectivity
     * @return if this component is connected to the given component
     */
    public final boolean isConnectedTo(Component redstone) {
        return outputs.contains(redstone);
    }

    /**
     * Gets whether this component is connected with another component (bidirectional)
     * 
     * @param redstone the component to check connectivity
     * @return if this component is connected with the given component
     */
    public final boolean isConnected(Component redstone) {
        return isConnectedTo(redstone) && redstone.isConnectedTo(this);
    }

    /**
     * Disables this element: inputs are instantly redirected to the outputs Result: this element has no role. All inputs
     * and outputs are cleared
     */
    public final void disable() {
        for (Component input : inputs) {
            input.outputs.remove(this);
            for (Component output : outputs) {
                input.connectTo(output);
            }
        }
        for (Component output : outputs) {
            output.inputs.remove(this);
            for (Component input : inputs) {
                input.connectTo(output);
            }
        }
        inputs.clear();
        outputs.clear();
    }

    /**
     * Gets whether this component is disabled
     * 
     * @return if this component is disabled
     */
    public final boolean isDisabled() {
        return inputs.size() == 0 && outputs.size() == 0;
    }

    /**
     * Gets whether the component is powered by an input or not
     * 
     * @return if the component is powered
     */
    public final boolean isPowered() {
        return powered;
    }

    /**
     * Sets the power state of this component
     * 
     * @param powered the power state of this component
     */
    public final void setPowered(boolean powered) {
        this.powered = powered;
        inputpower = powered;
    }

    /**
     * Sets the power state of this component
     * 
     * @param powered  the power state of this component
     * @param usedelay whether to use delay or not
     */
    private void setPowered(boolean powered, boolean usedelay) {
        int delay = getDelay();
        if (usedelay && delay > 0) {
            if (setdelay == 0) {
                setpowered = powered;
                setdelay = delay;
            }
        } else if (burnoutCounter > 0) {
            --burnoutCounter;
            if (powered) {
                if (!this.powered) {
                    this.powered = true;
                    onPowerChange();
                }
                if (!inputpower) {
                    this.setPowered(false, true);
                }
            } else if (!inputpower && this.powered) {
                this.powered = false;
                onPowerChange();
            }
        }
    }

    /**
     * Gets if the component is powering outputs or not
     * 
     * @return if the component has power for output
     */
    public boolean hasPower() {
        return isPowered();
    }

    /**
     * Gets the ID of this component
     * 
     * @return the ID of this component
     */
    public final int getId() {
        return id;
    }

    /**
     * Sets the ID of this component
     * 
     * @param id the ID of this component
     */
    public final void setId(int id) {
        this.id = id;
    }

    /**
     * Gets the delay on this component
     * 
     * @return the delay on this component
     */
    public int getDelay() {
        return delay;
    }

    /**
     * Sets the delay on this component
     * 
     * @param delay the delay on this component
     */
    public void setDelay(int delay) {
        this.delay = delay;
    }

    /**
     * Gets the circuit that this component belongs to
     * 
     * @return the circuit that this component belongs to
     */
    public CircuitBase getCircuit() {
        return circuit;
    }

    /**
     * Sets the circuit that this component belongs to
     * 
     * @param circuit the circuit that this component belongs to
     */
    public void setCircuit(CircuitBase circuit) {
        this.circuit = circuit;
    }

    /**
     * Gets the x-coordinate of this component
     * 
     * @return the x-coordinate of this component
     */
    public final short getX() {
        return x;
    }

    /**
     * Gets the z-coordinate of this component
     * 
     * @return the z-coordinate of this component
     */
    public final short getZ() {
        return z;
    }

    /**
     * Sets the position of this component from the original block
     * 
     * @param block the block that this component is derived from
     */
    public void setPosition(Block block) {
        this.setPosition(block.getX(), block.getZ());
    }

    /**
     * Sets the position of this component from x- and z-coordinates
     * 
     * @param x the x-coordinate of this component
     * @param z the z-coordinate of this component
     */
    public void setPosition(int x, int z) {
        this.x = (short) x;
        this.z = (short) z;
    }

    /**
     * Gets the current type of the component
     * 
     * @return the type of the component
     */
    public abstract byte getType();

    /**
     * Checks if the component is of a certain type
     * 
     * @param types a list of types to check against
     * @return if the component type matches one of the types in the list
     */
    public final boolean isType(int... types) {
        for (int type : types) {
            if (getType() == type) return true;
        }
        return false;
    }

    /**
     * Copies the data from another component and sets it to this component
     * 
     * @param source the component to copy data from
     */
    public void setData(Component source) {
        id = source.id;
        delay = source.delay;
        powered = source.powered;
        setdelay = source.setdelay;
        setpowered = source.setpowered;
    }

    /**
     * Load an instance of this component from a data stream
     * 
     * @param stream the data stream to read from
     * @throws IOException if there is a problem reading from the data stream
     */
    public void loadInstance(DataInputStream stream) throws IOException {
        powered = stream.readBoolean();
        if (delay > 0) {
            setdelay = stream.readInt();
            if (setdelay > 0) {
                setpowered = stream.readBoolean();
            }
        }
    }

    /**
     * Save an instance of this component to a data stream
     * 
     * @param stream the data stream to write to
     * @throws IOException if there is a problem writing to the data stream
     */
    public void saveInstance(DataOutputStream stream) throws IOException {
        stream.writeBoolean(powered);
        if (delay > 0) {
            stream.writeInt(setdelay);
            if (setdelay > 0) {
                stream.writeBoolean(setpowered);
            }
        }
    }

    /**
     * Save this component's details to a data stream
     * 
     * @param stream the data stream to write to
     * @throws IOException if there is a problem writing to the data stream
     */
    public void saveTo(DataOutputStream stream) throws IOException {
        stream.write(getType());
        // init
        stream.writeShort(x);
        stream.writeShort(z);
        stream.writeBoolean(powered);
        stream.writeInt(delay);
        if (this instanceof Port) {
            stream.writeUTF(((Port) this).name);
        }
    }

    /**
     * Load a component from a data stream
     * 
     * @param stream the data stream to read from
     * @return the component that was loaded
     * @throws IOException if there is a problem reading from the data stream
     */
    public static Component loadFrom(DataInputStream stream) throws IOException {
        byte type = stream.readByte();
        Component rval;
        if (type == 0) {
            rval = new Wire();
        } else if (type == 1) {
            rval = new Inverter();
        } else if (type == 2) {
            rval = new Repeater();
        } else if (type == 3) {
            rval = new Port();
        } else {
            rval = new Wire();
            RedstoneMania.plugin.log(Level.SEVERE, "Unknown redstone type: " + type);
        }
        // init
        rval.x = stream.readShort();
        rval.z = stream.readShort();
        rval.powered = stream.readBoolean();
        rval.delay = stream.readInt();
        if (rval instanceof Port) {
            ((Port) rval).name = stream.readUTF();
        }
        return rval;
    }

    @Override
    public Component clone() {
        Component r;
        if (this instanceof Inverter) {
            r = new Inverter();
        } else if (this instanceof Repeater) {
            r = new Repeater();
        } else if (this instanceof Port) {
            r = new Port();
            ((Port) r).name = ((Port) this).name;
        } else {
            r = new Wire();
        }
        r.setData(this);
        return r;
    }

    @Override
    public String toString() {
        if (this instanceof Inverter) {
            return "[Inverter " + id + "]";
        } else if (this instanceof Repeater) {
            return "[Repeater " + id + "]";
        } else if (this instanceof Port) {
            return "[Port '" + ((Port) this).name + "' " + id + "]";
        } else {
            return "[Wire " + id + "]";
        }
    }

}
