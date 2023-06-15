package com.bergerkiller.bukkit.rm.element;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.logging.Level;

import org.bukkit.block.Block;

import com.bergerkiller.bukkit.rm.RedstoneMania;
import com.bergerkiller.bukkit.rm.circuit.CircuitBase;

public class Redstone {
    private int setdelay = 0;
    private boolean setpowered = false;
    private boolean powered = false;
    private boolean inputpower = false;
    private int id = -1;
    private int delay = 0;
    private CircuitBase circuit;
    public HashSet<Redstone> inputs = new HashSet<>();
    public HashSet<Redstone> outputs = new HashSet<>();
    private final int burnoutValue = 5; // Sets the maximum allowed updates/tick
    private int burnoutCounter = burnoutValue;
    private short x, z;

    public void onTick() {
        if (setdelay > 0) {
            if (--setdelay == 0) {
                this.setPowered(setpowered, false);
            }
        }
        burnoutCounter = burnoutValue;
    }

    public void setPosition(Block block) {
        this.setPosition(block.getX(), block.getZ());
    }

    public void setPosition(int x, int z) {
        this.x = (short) x;
        this.z = (short) z;
    }

    public void setCircuit(CircuitBase circuit) {
        this.circuit = circuit;
    }

    public CircuitBase getCircuit() {
        return circuit;
    }

    /**
     * Updates the outputs using the inputs, returns if this element changed
     * 
     * @return
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
        for (Redstone input : inputs) {
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

    public final void setPowered(boolean powered) {
        this.powered = powered;
        inputpower = powered;
    }

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

    public void onPowerChange() {
        for (Redstone output : outputs) {
            output.update();
        }
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

    public final boolean isPowered() {
        return powered;
    }

    public boolean hasPower() {
        return isPowered();
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public int getDelay() {
        return delay;
    }

    public final short getX() {
        return x;
    }

    public final short getZ() {
        return z;
    }

    public final int getId() {
        return id;
    }

    public final void setId(int id) {
        this.id = id;
    }

    public final boolean isType(int... types) {
        for (int type : types) {
            if (getType() == type) return true;
        }
        return false;
    }

    public byte getType() {
        return 0;
    }

    public void setData(Redstone source) {
        id = source.id;
        delay = source.delay;
        powered = source.powered;
        setdelay = source.setdelay;
        setpowered = source.setpowered;
    }

    @Override
    public Redstone clone() {
        Redstone r;
        if (this instanceof Inverter) {
            r = new Inverter();
        } else if (this instanceof Repeater) {
            r = new Repeater();
        } else if (this instanceof Port) {
            r = new Port();
            ((Port) r).name = ((Port) this).name;
        } else {
            r = new Redstone();
        }
        r.setData(this);
        return r;
    }

    public final void transfer(Redstone to) {
        for (Redstone input : inputs) {
            input.outputs.remove(this);
            if (to != input) {
                input.outputs.add(to);
                to.inputs.add(input);
            }
        }
        for (Redstone output : outputs) {
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
     * Disables this element: inputs are instantly redirected to the outputs Result: this element has no role. All inputs
     * and outputs are cleared
     */
    public final void disable() {
        for (Redstone input : inputs) {
            input.outputs.remove(this);
            for (Redstone output : outputs) {
                input.connectTo(output);
            }
        }
        for (Redstone output : outputs) {
            output.inputs.remove(this);
            for (Redstone input : inputs) {
                input.connectTo(output);
            }
        }
        inputs.clear();
        outputs.clear();
    }

    public final Redstone findDirectConnection() {
        if (getDelay() == 0 && inputs.size() > 0 && outputs.size() > 0) {
            for (Redstone input : inputs) {
                if (input.getDelay() == 0 && outputs.contains(input)) {
                    return input;
                }
            }
        }
        return null;
    }

    public final void connect(Redstone redstone) {
        connectTo(redstone);
        redstone.connectTo(this);
    }

    public final void connectTo(Redstone redstone) {
        if (redstone == this) return;
        outputs.add(redstone);
        redstone.inputs.add(this);
    }

    public final void disconnect(Redstone redstone) {
        inputs.remove(redstone);
        outputs.remove(redstone);
        redstone.inputs.remove(this);
        redstone.outputs.remove(this);
    }

    public final void disconnect() {
        for (Redstone r : inputs) {
            r.outputs.remove(this);
        }
        for (Redstone r : outputs) {
            r.inputs.remove(this);
        }
        inputs.clear();
        outputs.clear();
    }

    public final boolean isConnectedTo(Redstone redstone) {
        return outputs.contains(redstone);
    }

    public final boolean isConnected(Redstone redstone) {
        return isConnectedTo(redstone) && redstone.isConnectedTo(this);
    }

    public final boolean isDisabled() {
        return inputs.size() == 0 && outputs.size() == 0;
    }

    public void loadInstance(DataInputStream stream) throws IOException {
        powered = stream.readBoolean();
        if (delay > 0) {
            setdelay = stream.readInt();
            if (setdelay > 0) {
                setpowered = stream.readBoolean();
            }
        }
    }

    public void saveInstance(DataOutputStream stream) throws IOException {
        stream.writeBoolean(powered);
        if (delay > 0) {
            stream.writeInt(setdelay);
            if (setdelay > 0) {
                stream.writeBoolean(setpowered);
            }
        }
    }

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

    public static Redstone loadFrom(DataInputStream stream) throws IOException {
        byte type = stream.readByte();
        Redstone rval;
        if (type == 0) {
            rval = new Redstone();
        } else if (type == 1) {
            rval = new Inverter();
        } else if (type == 2) {
            rval = new Repeater();
        } else if (type == 3) {
            rval = new Port();
        } else {
            rval = new Redstone();
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

}
