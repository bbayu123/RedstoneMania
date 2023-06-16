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
    protected boolean powered = false;
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
     * A set of components that provide main input to this component
     */
    public Set<Component> mainInputs = new HashSet<>();
    /**
     * A set of components that provide side input to this component
     */
    public Set<Component> sideInputs = new HashSet<>();
    /**
     * A set of components that this component outputs to the main input
     */
    public Set<Component> mainOutputs = new HashSet<>();
    /**
     * A set of components that this component outputs to the side input
     */
    public Set<Component> sideOutputs = new HashSet<>();
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
     * The main power state to set after the current delay
     */
    private boolean setMainPowered = false;
    /**
     * The side power state to set after the current delay
     */
    private boolean setSidePowered = false;
    /**
     * Whether there currently is power from the main inputs
     */
    private boolean mainInputPower = false;
    /**
     * Whether there currently is power from the side inputs
     */
    private boolean sideInputPower = false;
    /**
     * The current counter for the burnout
     */
    private int burnoutCounter = this.burnoutValue;

    /**
     * Called every tick to update the state of this component
     */
    public void onTick() {
        if (this.setdelay > 0) {
            if (--this.setdelay == 0) {
                this.setPowered(this.setMainPowered, this.setSidePowered, false);
            }
        }
        this.burnoutCounter = this.burnoutValue;
    }

    /**
     * Updates the outputs using the inputs, returns if this element changed
     * 
     * @return if this element has changed
     */
    public final boolean update() {
        // we don't have to update inactive elements!
        if (this.isDisabled()) {
            if (this.getType() != 3) {
                return false;
            }
        }
        // check if the opposite is the new result
        boolean hasmaininput = false;
        for (Component input : this.mainInputs) {
            if (input.hasPower()) {
                hasmaininput = true;
                break;
            }
        }
        boolean hassideinput = false;
        for (Component input : this.sideInputs) {
            if (input.hasPower()) {
                hassideinput = true;
                break;
            }
        }
        if ((this.mainInputPower ^ hasmaininput) || (this.sideInputPower ^ hassideinput)) {
            this.mainInputPower = hasmaininput;
            this.sideInputPower = hassideinput;
            this.setPowered(hasmaininput, hassideinput, true);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Called when the power state has changed
     */
    public void onPowerChange() {
        for (Component output : this.mainOutputs) {
            output.update();
        }
        for (Component output : this.sideOutputs) {
            output.update();
        }
    }

    /**
     * Find any output component that is directly connected to one of the input component
     * 
     * @return the output component that is found, or null if none is found
     */
    public final Component findDirectConnection() {
        if (this.getDelay() == 0 && this.mainInputs.size() > 0 && this.mainOutputs.size() > 0 && this.sideOutputs.size() > 0) {
            for (Component input : this.mainInputs) {
                if (input.getDelay() == 0 && this.mainOutputs.contains(input)) {
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
        for (Component input : this.mainInputs) {
            input.mainOutputs.remove(this);
            if (to != input) {
                input.mainOutputs.add(to);
                to.mainInputs.add(input);
            }
        }
        for (Component input : this.sideInputs) {
            input.sideOutputs.remove(this);
            if (to != input) {
                input.sideOutputs.add(to);
                to.sideInputs.add(input);
            }
        }
        for (Component output : this.mainOutputs) {
            output.mainInputs.remove(this);
            if (to != output) {
                output.mainInputs.add(to);
                to.mainOutputs.add(output);
            }
        }
        for (Component output : this.sideOutputs) {
            output.sideInputs.remove(this);
            if (to != output) {
                output.sideInputs.add(to);
                to.sideOutputs.add(output);
            }
        }
        this.mainInputs.clear();
        this.sideInputs.clear();
        this.mainOutputs.clear();
        this.sideOutputs.clear();
    }

    /**
     * Connect this component's output to another component's main input
     * 
     * @param redstone the component to connect to
     */
    public final void connectTo(Component redstone) {
        if (redstone == this) return;
        this.mainOutputs.add(redstone);
        redstone.mainInputs.add(this);
    }

    /**
     * Connect this component's output to another component's side input
     * 
     * @param redstone the component to connect to
     */
    public final void connectToSide(Component redstone) {
        if (redstone == this) return;
        this.sideOutputs.add(redstone);
        redstone.sideInputs.add(this);
    }

    /**
     * Disconnect this component from another component
     * 
     * @param redstone the component to disconnect from
     */
    public final void disconnect(Component redstone) {
        this.disconnectFrom(redstone);
        redstone.disconnectFrom(this);
    }

    /**
     * Disconnect this component's output from another component's input
     * 
     * @param redstone the component to disconnect from
     */
    public final void disconnectFrom(Component redstone) {
        if (redstone == this) return;
        this.mainOutputs.remove(redstone);
        this.sideOutputs.remove(redstone);
        redstone.mainInputs.remove(this);
        redstone.sideInputs.remove(this);
    }

    /**
     * Disconnect all connected components
     */
    public final void disconnectAll() {
        for (Component r : this.mainInputs) {
            r.mainOutputs.remove(this);
        }
        for (Component r : this.sideInputs) {
            r.sideOutputs.remove(this);
        }
        for (Component r : this.mainOutputs) {
            r.mainInputs.remove(this);
        }
        for (Component r : this.sideOutputs) {
            r.sideInputs.remove(this);
        }
        this.mainInputs.clear();
        this.sideInputs.clear();
        this.mainOutputs.clear();
        this.sideOutputs.clear();
    }

    /**
     * Gets whether this component is connected to (outputs to) another component
     * 
     * @param redstone the component to check connectivity
     * @return if this component is connected to the given component
     */
    public final boolean isConnectedTo(Component redstone) {
        return this.mainOutputs.contains(redstone) || this.sideOutputs.contains(redstone);
    }

    /**
     * Gets whether this component is connected with another component (bidirectional)
     * 
     * @param redstone the component to check connectivity
     * @return if this component is connected with the given component
     */
    public final boolean isConnected(Component redstone) {
        return this.isConnectedTo(redstone) && redstone.isConnectedTo(this);
    }

    /**
     * Disables this element: inputs are instantly redirected to the outputs Result: this element has no role. All inputs
     * and outputs are cleared
     */
    public final void disable() {
        for (Component input : this.mainInputs) {
            input.mainOutputs.remove(this);
            for (Component output : this.mainOutputs) {
                input.connectTo(output);
            }
        }
        for (Component input : this.sideInputs) {
            input.sideOutputs.remove(this);
            for (Component output : this.sideOutputs) {
                input.connectToSide(output);
            }
        }
        for (Component output : this.mainOutputs) {
            output.mainInputs.remove(this);
            for (Component input : this.mainInputs) {
                input.connectTo(output);
            }
        }
        for (Component output : this.sideOutputs) {
            output.sideInputs.remove(this);
            for (Component input : this.sideInputs) {
                input.connectToSide(output);
            }
        }
        this.mainInputs.clear();
        this.sideInputs.clear();
        this.mainOutputs.clear();
        this.sideOutputs.clear();
    }

    /**
     * Gets whether this component is disabled
     * 
     * @return if this component is disabled
     */
    public final boolean isDisabled() {
        return this.mainInputs.size() == 0 && this.sideInputs.size() == 0 && this.mainOutputs.size() == 0 && this.sideOutputs.size() == 0;
    }

    /**
     * Gets whether the component is powered by an input or not
     * 
     * @return if the component is powered
     */
    public final boolean isPowered() {
        return this.mainInputPower || this.sideInputPower;
    }

    /**
     * Sets the power state of this component
     * 
     * @param powered the power state of this component
     */
    public final void setPowered(boolean mainPowered, boolean sidePowered) {
        this.powered = mainPowered;
        this.mainInputPower = mainPowered;
        this.sideInputPower = sidePowered;
    }

    /**
     * Sets the power state of this component
     * 
     * @param mainPowered the main power state of this component
     * @param sidePowered the side power state of this component
     * @param usedelay    whether to use delay or not
     */
    private void setPowered(boolean mainPowered, boolean sidePowered, boolean usedelay) {
        int delay = this.getDelay();
        if (usedelay && delay > 0) {
            if (this.setdelay == 0) {
                this.setMainPowered = mainPowered;
                this.setSidePowered = sidePowered;
                this.setdelay = delay;
            }
            return;
        }

        if (this.burnoutCounter > 0) {
            --this.burnoutCounter;

            // If no main input power
            boolean shouldPower = this.determinePower(mainPowered, sidePowered);
            if (shouldPower ^ this.powered) {
                this.powered = shouldPower;
                this.onPowerChange();
            }

            if ((this.mainInputPower ^ mainPowered) || (this.sideInputPower ^ sidePowered)) {
                this.setPowered(this.mainInputPower, this.sideInputPower, usedelay);
            }
        }
    }

    protected abstract boolean determinePower(boolean mainPowered, boolean sidePowered);

    /**
     * Gets if the component is powering outputs or not
     * 
     * @return if the component has power for output
     */
    public boolean hasPower() {
        return this.powered;
    }

    /**
     * Gets the ID of this component
     * 
     * @return the ID of this component
     */
    public final int getId() {
        return this.id;
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
        return this.delay;
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
        return this.circuit;
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
        return this.x;
    }

    /**
     * Gets the z-coordinate of this component
     * 
     * @return the z-coordinate of this component
     */
    public final short getZ() {
        return this.z;
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
            if (this.getType() == type) return true;
        }
        return false;
    }

    /**
     * Copies the data from another component and sets it to this component
     * 
     * @param source the component to copy data from
     */
    public void setData(Component source) {
        this.id = source.id;
        this.delay = source.delay;
        this.powered = source.powered;
        this.setdelay = source.setdelay;
        this.setMainPowered = source.setMainPowered;
    }

    /**
     * Load an instance of this component from a data stream
     * 
     * @param stream the data stream to read from
     * @throws IOException if there is a problem reading from the data stream
     */
    public void loadInstance(DataInputStream stream) throws IOException {
        this.powered = stream.readBoolean();
        if (this.delay > 0) {
            this.setdelay = stream.readInt();
            if (this.setdelay > 0) {
                this.setMainPowered = stream.readBoolean();
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
        stream.writeBoolean(this.powered);
        if (this.delay > 0) {
            stream.writeInt(this.setdelay);
            if (this.setdelay > 0) {
                stream.writeBoolean(this.setMainPowered);
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
        stream.write(this.getType());
        // init
        stream.writeShort(this.x);
        stream.writeShort(this.z);
        stream.writeBoolean(this.powered);
        stream.writeInt(this.delay);
        if (this instanceof Port) {
            stream.writeUTF(((Port) this).name);
        }
    }

    /**
     * Loads this component's details from a data stream
     * 
     * @param stream the data stream to read from
     * @throws IOException if there is a problem reading from the data stream
     */
    public void loadFrom(DataInputStream stream) throws IOException {
        this.x = stream.readShort();
        this.z = stream.readShort();
        this.powered = stream.readBoolean();
        this.delay = stream.readInt();
    }

    /**
     * Load a component from a data stream
     * 
     * @param stream the data stream to read from
     * @return the component that was loaded
     * @throws IOException if there is a problem reading from the data stream
     */
    public static Component loadComponent(DataInputStream stream) throws IOException {
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
        rval.loadFrom(stream);
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
            return "[Inverter " + this.id + "]";
        } else if (this instanceof Repeater) {
            return "[Repeater " + this.id + "]";
        } else if (this instanceof Port) {
            return "[Port '" + ((Port) this).name + "' " + this.id + "]";
        } else {
            return "[Wire " + this.id + "]";
        }
    }

}
