package com.bergerkiller.bukkit.rm.circuit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import com.bergerkiller.bukkit.rm.element.Port;
import com.bergerkiller.bukkit.rm.element.Component;

public class CircuitInstance extends CircuitBase {
    public Circuit source;
    public boolean isMain = false;

    public CircuitInstance(Circuit source, String name) {
        this.source = source;
        this.name = name;
    }

    public boolean updateAlive() {
        for (Port p : getPorts()) {
            if (p.locations.size() > 0) {
                return false;
            }
        }
        // no physical ports - dead
        source.removeInstance(name);
        return true;
    }

    public void update() {
        for (Component r : elements) {
            r.update();
            r.onPowerChange();
        }
        for (CircuitInstance ci : subcircuits) {
            ci.update();
        }
    }

    @Override
    public File getFile() {
        return new File(source.getInstanceFolder() + File.separator + name + ".instance");
    }

    @Override
    public String getFullName() {
        return source.name + "." + name;
    }

    @Override
    public void load(DataInputStream dis) throws IOException {
        for (Component r : elements) {
            r.loadInstance(dis);
        }
        for (CircuitInstance c : subcircuits) {
            c.load(dis);
        }
        initialize();
    }

    @Override
    public void save(DataOutputStream dos) throws IOException {
        for (Component r : elements) {
            r.saveInstance(dos);
        }
        for (CircuitInstance c : subcircuits) {
            c.save(dos);
        }
    }
}
