package com.bergerkiller.bukkit.rm.element;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;

import org.bukkit.block.Block;

import com.bergerkiller.bukkit.common.BlockLocation;

public class Port extends Component {

    public static Port get(Block at) {
        PhysicalPort pp = PhysicalPort.get(at);
        if (pp == null) return null;
        return pp.port;
    }

    public String name;
    public HashSet<PhysicalPort> locations = new HashSet<>();
    private boolean leverpowered = false;
    public boolean ignoreNext = false; // prevents infinite loops because of levers

    @Override
    public byte getType() {
        return 3;
    }

    @Override
    protected boolean determinePower(boolean mainPowered, boolean sidePowered) {
        return mainPowered;
    }

    @Override
    public boolean hasPower() {
        return this.leverpowered || super.hasPower();
    }

    @Override
    public void onPowerChange() {
        if (!this.ignoreNext) {
            this.ignoreNext = true;
            for (PhysicalPort p : this.locations) {
                p.updateLevers();
            }
        }
        super.onPowerChange();
        this.ignoreNext = false;
    }

    public void onPowerChange(PhysicalPort ignore) {
        if (ignore == null) {
            this.onPowerChange();
        } else {
            for (PhysicalPort p : this.locations) {
                if (p != ignore) p.updateLevers();
            }
            super.onPowerChange();
        }
    }

    /**
     * Updates the leverpowered state
     * 
     * @return If the powered state got changed
     */
    public boolean updateLeverPower() {
        return this.updateLeverPower(null);
    }

    public boolean updateLeverPower(PhysicalPort ignore) {
        if (this.isPowered()) {
            this.leverpowered = false;
        } else {
            if (this.leverpowered) {
                for (PhysicalPort p : this.locations) {
                    if (p.isLeverPowered()) return false;
                }
                this.setLeverPowered(false);
                this.onPowerChange(ignore);
                return true;
            } else {
                for (PhysicalPort p : this.locations) {
                    if (p.isLeverPowered()) {
                        this.setLeverPowered(true);
                        this.onPowerChange(ignore);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isLeverPowered() {
        return this.leverpowered;
    }

    public void setLeverPowered(boolean powered) {
        this.leverpowered = powered;
    }

    public PhysicalPort addPhysical(Block at) {
        return this.addPhysical(new BlockLocation(at));
    }

    public PhysicalPort addPhysical(BlockLocation at) {
        return new PhysicalPort(this, at);
    }

    @Override
    public void loadInstance(DataInputStream stream) throws IOException {
        super.loadInstance(stream);
        int loccount = stream.readShort();
        for (int pi = 0; pi < loccount; pi++) {
            BlockLocation at = new BlockLocation(stream.readUTF(), stream.readInt(), stream.readByte(), stream.readInt());
            this.addPhysical(at).setLeverPowered(stream.readBoolean());
        }
    }

    @Override
    public void saveInstance(DataOutputStream stream) throws IOException {
        super.saveInstance(stream);
        stream.writeShort(this.locations.size());
        for (PhysicalPort pp : this.locations) {
            stream.writeUTF(pp.position.world);
            stream.writeInt(pp.position.x);
            stream.writeByte(pp.position.y);
            stream.writeInt(pp.position.z);
            stream.writeBoolean(pp.isLeverPowered());
        }
    }

    @Override
    public void saveTo(DataOutputStream stream) throws IOException {
        super.saveTo(stream);
        stream.writeUTF(this.name);
    }

    @Override
    public void loadFrom(DataInputStream stream) throws IOException {
        super.loadFrom(stream);
        this.name = stream.readUTF();
    }
}
