package com.bergerkiller.bukkit.rm.circuit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;

import com.bergerkiller.bukkit.rm.RedstoneMania;
import com.bergerkiller.bukkit.rm.element.PhysicalPort;
import com.bergerkiller.bukkit.rm.element.Port;
import com.bergerkiller.bukkit.rm.element.Component;

public class Circuit extends CircuitBase {
    private HashMap<String, CircuitInstance> instances = new HashMap<>();

    @Override
    public File getFile() {
        return CircuitProvider.getCircuitFile(name);
    }

    public File getInstanceFolder() {
        File file = new File(CircuitProvider.getInstancesFolder() + File.separator + name);
        file.mkdirs();
        return file;
    }

    public CircuitInstance getInstance(String name) {
        return instances.get(name);
    }

    public Collection<CircuitInstance> getInstances() {
        return instances.values();
    }

    private CircuitInstance createInstance(boolean main) {
        CircuitInstance c = new CircuitInstance(this, "");
        // Set dependencies
        c.subcircuits = new CircuitInstance[subcircuits.length];
        for (int i = 0; i < c.subcircuits.length; i++) {
            c.subcircuits[i] = subcircuits[i].source.createInstance();
        }
        // Clone the data
        c.elements = new Component[elements.length];
        for (int i = 0; i < elements.length; i++) {
            c.elements[i] = elements[i].clone();
        }
        // Perform some ID generation to match the element ID's
        c.initialize();
        // Link the elements
        for (int i = 0; i < c.elements.length; i++) {
            Component from = elements[i];
            Component to = c.elements[i];
            if (from.getId() != to.getId()) {
                RedstoneMania.plugin.log(Level.SEVERE, "Failed to make a new instance of '" + name + "': ID out of sync!");
                return null;
            } else {
                for (Component input : from.inputs) {
                    Component element = c.getElement(input);
                    if (element == null) {
                        RedstoneMania.plugin.log(Level.SEVERE, "Failed to create a new instance of '" + name + "': input element ID mismatch!");
                        return null;
                    } else {
                        element.connectTo(to);
                    }
                }
                for (Component output : from.outputs) {
                    Component element = c.getElement(output);
                    if (element == null) {
                        RedstoneMania.plugin.log(Level.SEVERE, "Failed to create a new instance of '" + name + "': output element ID mismatch!");
                        return null;
                    } else {
                        to.connectTo(element);
                    }
                }
            }
        }
        // Fix direct connections
        c.fixDirectConnections();
        return c;
    }

    public String findNewInstanceName() {
        StringBuilder nameBuilder = new StringBuilder(2);
        String newName = "";
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            nameBuilder.setLength(0);
            nameBuilder.append(i);
            newName = nameBuilder.toString();
            if (!instances.containsKey(newName)) {
                break;
            }
        }
        return newName;
    }

    public CircuitInstance createInstance() {
        return this.createInstance(false);
    }

    public CircuitInstance createInstance(String name) {
        CircuitInstance c = getInstance(name);
        if (c == null) {
            c = this.createInstance(true);
            c.name = name;
            instances.put(name, c);
        }
        return c;
    }

    public CircuitInstance removeInstance(String name) {
        CircuitInstance ci = instances.remove(name);
        if (ci != null) {
            for (Port p : ci.getPorts()) {
                for (PhysicalPort pp : p.locations) {
                    PhysicalPort.remove(pp);
                }
            }
        }
        File sourcefile = ci.getFile();
        if (sourcefile.exists()) sourcefile.delete();
        return ci;
    }

    @Override
    public void load(DataInputStream dis) throws IOException {
        // Read the sub-circuit dependencies
        subcircuits = new CircuitInstance[dis.readShort()];
        for (int i = 0; i < subcircuits.length; i++) {
            String cname = dis.readUTF();
            Circuit c = CircuitProvider.get(cname);
            if (c == null) {
                throw new RuntimeException("Circuit dependency not found: " + cname);
            } else {
                subcircuits[i] = c.createInstance();
            }
        }
        // Read the circuit data
        elements = new Component[dis.readShort()];
        for (int i = 0; i < elements.length; i++) {
            elements[i] = Component.loadFrom(dis);
        }
        initialize();
        // Connect elements
        for (Component r : elements) {
            int inputcount = dis.readShort();
            for (int i = 0; i < inputcount; i++) {
                Component input = this.getElement(dis.readInt());
                if (input == null) {
                    throw new IOException("Redstone element has a missing input!");
                } else {
                    input.connectTo(r);
                }
            }
            int outputcount = dis.readShort();
            for (int i = 0; i < outputcount; i++) {
                Component output = this.getElement(dis.readInt());
                if (output == null) {
                    throw new IOException("Redstone element has a missing output!");
                } else {
                    r.connectTo(output);
                }
            }
        }
    }

    @Override
    public void save(DataOutputStream dos) throws IOException {
        // Write circuit dependencies
        dos.writeShort(subcircuits.length);
        for (CircuitInstance c : subcircuits) {
            dos.writeUTF(c.source.name);
        }
        // Write the circuit data
        dos.writeShort(elements.length);
        for (Component r : elements) {
            r.saveTo(dos);
        }
        // Write connections
        for (Component r : elements) {
            dos.writeShort(r.inputs.size());
            for (Component input : r.inputs) {
                dos.writeInt(input.getId());
            }
            dos.writeShort(r.outputs.size());
            for (Component output : r.outputs) {
                dos.writeInt(output.getId());
            }
        }
    }

    public String getNewInstanceName() {
        int index = instances.size();
        String name = String.valueOf(index);
        while (getInstance(name) != null) {
            index++;
            name = String.valueOf(index);
        }
        return name;
    }
}
