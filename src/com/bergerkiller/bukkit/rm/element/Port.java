package com.bergerkiller.bukkit.rm.element;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;

import org.bukkit.block.Block;

import com.bergerkiller.bukkit.common.BlockLocation;

public class Port extends Redstone {

    public static Port get(Block at) {
        PhysicalPort pp = PhysicalPort.get(at);
        if (pp == null) return null;
        return pp.port;
    }

    public String name;
    public HashSet<PhysicalPort> locations = new HashSet<>();
    private boolean leverpowered = false;
    public boolean ignoreNext = false; // prevents infinite loops because of levers

    /**
     * Updates the leverpowered state
     * 
     * @return If the powered state got changed
     */
    public boolean updateLeverPower() {
        return this.updateLeverPower(null);
    }

    public boolean updateLeverPower(PhysicalPort ignore) {
        if (isPowered()) {
            leverpowered = false;
        } else {
            if (leverpowered) {
                for (PhysicalPort p : locations) {
                    if (p.isLeverPowered()) return false;
                }
                setLeverPowered(false);
                this.onPowerChange(ignore);
                return true;
            } else {
                for (PhysicalPort p : locations) {
                    if (p.isLeverPowered()) {
                        setLeverPowered(true);
                        this.onPowerChange(ignore);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasPower() {
        return leverpowered || super.hasPower();
    }

    public void onPowerChange(PhysicalPort ignore) {
        if (ignore == null) {
            this.onPowerChange();
        } else {
            for (PhysicalPort p : locations) {
                if (p != ignore) p.updateLevers();
            }
            super.onPowerChange();
        }
    }

    @Override
    public void onPowerChange() {
        if (!ignoreNext) {
            ignoreNext = true;
            for (PhysicalPort p : locations) {
                p.updateLevers();
            }
        }
        super.onPowerChange();
        ignoreNext = false;
    }

    public boolean isLeverPowered() {
        return leverpowered;
    }

    public void setLeverPowered(boolean powered) {
        leverpowered = powered;
    }

    @Override
    public byte getType() {
        return 3;
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
        stream.writeShort(locations.size());
        for (PhysicalPort pp : locations) {
            stream.writeUTF(pp.position.world);
            stream.writeInt(pp.position.x);
            stream.writeByte(pp.position.y);
            stream.writeInt(pp.position.z);
            stream.writeBoolean(pp.isLeverPowered());
        }
    }

}
