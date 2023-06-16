package com.bergerkiller.bukkit.rm.circuit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import com.bergerkiller.bukkit.rm.element.Component;
import com.bergerkiller.bukkit.rm.element.Port;

public class CircuitInstance extends CircuitBase {
    public Circuit source;
    public boolean isMain = false;

    public CircuitInstance(Circuit source, String name) {
        this.source = source;
        this.name = name;
    }

    public boolean updateAlive() {
        for (Port p : this.getPorts()) {
            if (p.locations.size() > 0) {
                return false;
            }
        }
        // no physical ports - dead
        this.source.removeInstance(this.name);
        return true;
    }

    public void update() {
        for (Component r : this.elements) {
            r.update();
            r.onPowerChange();
        }
        for (CircuitInstance ci : this.subcircuits) {
            ci.update();
        }
    }

    @Override
    public File getFile() {
        return new File(this.source.getInstanceFolder() + File.separator + this.name + ".instance");
    }

    @Override
    public String getFullName() {
        return this.source.name + "." + this.name;
    }

    @Override
    public void load(DataInputStream dis) throws IOException {
        for (Component r : this.elements) {
            r.loadInstance(dis);
        }
        for (CircuitInstance c : this.subcircuits) {
            c.load(dis);
        }
        this.initialize();
    }

    @Override
    public void save(DataOutputStream dos) throws IOException {
        for (Component r : this.elements) {
            r.saveInstance(dos);
        }
        for (CircuitInstance c : this.subcircuits) {
            c.save(dos);
        }
    }

    @Override
    public String toString() {
        return String.format("CircuitInstance [isMain=%s] : %s", isMain, super.toString());
    }
}
