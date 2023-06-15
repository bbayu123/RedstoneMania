package com.bergerkiller.bukkit.rm.circuit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import com.bergerkiller.bukkit.common.config.DataReader;
import com.bergerkiller.bukkit.common.config.DataWriter;
import com.bergerkiller.bukkit.rm.element.Port;
import com.bergerkiller.bukkit.rm.element.Redstone;

public abstract class CircuitBase {
    public String name;
    public Redstone[] elements;
    public CircuitInstance[] subcircuits;
    private HashMap<String, Port> ports = new HashMap<>();
    private boolean initialized = false;
    private boolean loaded = false;
    private boolean saved = false;

    /**
     * Ticks all the elements in this Circuit
     */
    public void onTick() {
        for (Redstone r : elements) {
            r.onTick();
        }
        for (CircuitInstance ci : subcircuits) {
            ci.onTick();
        }
    }

    public void initialize() {
        this.initialize(true);
    }

    private void initialize(boolean generateIds) {
        ports.clear();
        for (Redstone r : elements) {
            r.setCircuit(this);
            if (r instanceof Port) {
                ports.put(((Port) r).name, (Port) r);
            }
        }
        for (CircuitBase c : subcircuits) {
            c.initialize(false);
        }
        if (generateIds) {
            generateIds(0);
        }
        initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public Port getPort(String name) {
        return ports.get(name);
    }

    public Collection<Port> getPorts() {
        return ports.values();
    }

    public void fixDirectConnections() {
        this.fixDirectConnections(true);
    }

    private boolean fixDirectConnections(boolean checksub) {
        boolean changed = false;
        boolean hasDirectConnections = true;
        while (hasDirectConnections) {
            hasDirectConnections = false;
            for (Redstone r : elements) {
                Redstone direct = r.findDirectConnection();
                if (direct == null) continue;
                hasDirectConnections = true;
                changed = true;
                // remove which: direct or r?
                if (direct instanceof Port) {
                    if (r instanceof Port) {
                        // which port is better?
                        if (direct.getCircuit() == this) {
                            // direct is top-level, this one is most important
                            if (r.getCircuit() == this) {
                                // r is also top level?! Oh oh! Let's just de-link them for good grace!
                                r.disconnect(direct);
                            } else {
                                r.disable();
                            }
                        } else {
                            direct.disable();
                        }
                    } else {
                        r.disable();
                    }
                } else {
                    direct.disable();
                }
            }
        }
        if (checksub) {
            for (CircuitBase c : subcircuits) {
                if (c.fixDirectConnections(false)) changed = true;
            }
        }
        if (changed) {
            return this.fixDirectConnections(checksub);
        } else {
            return false;
        }
    }

    public Redstone getElement(int id) {
        if (id >= elements.length) {
            for (CircuitBase sub : subcircuits) {
                for (Redstone r : sub.elements) {
                    if (r.getId() == id) return r;
                }
            }
            return null;
        } else {
            return elements[id];
        }
    }

    public Redstone getElement(Redstone guide) {
        return this.getElement(guide.getId());
    }

    public void removeElement(Redstone element) {
        for (int i = 0; i < elements.length; i++) {
            if (elements[i] == element) {
                this.removeElement(i);
                return;
            }
        }
    }

    private void removeElement(int index) {
        Redstone[] newElements = new Redstone[elements.length - 1];
        for (int i = 0; i < index; i++) {
            newElements[i] = elements[i];
        }
        for (int i = index; i < newElements.length; i++) {
            newElements[i] = elements[i + 1];
        }
        elements = newElements;
    }

    /**
     * Generates a list of elements which do not require another circuit to exist Removes all dependencies and internal
     * ports at the cost of a large schematic
     * 
     * @return
     */
    public Redstone[] getIndependentElements() {
        return this.getIndependentElements(true);
    }

    private Redstone[] getIndependentElements(boolean main) {
        ArrayList<Redstone> elements = new ArrayList<>();
        for (Redstone r : this.elements) {
            if (r != null && !r.isDisabled()) {
                if (!main && r instanceof Port) {
                    Redstone old = r;
                    r = new Redstone();
                    r.setData(old);
                }
                elements.add(r);
            }
        }
        for (CircuitBase cb : subcircuits) {
            for (Redstone r : cb.getIndependentElements()) {
                elements.add(r);
            }
        }
        return elements.toArray(new Redstone[0]);
    }

    public Circuit getIndependentCircuit() {
        Circuit c = new Circuit();
        c.elements = this.getIndependentElements();
        c.subcircuits = new CircuitInstance[0];
        c.initialize();
        return c;
    }

    private int generateIds(int startindex) {
        for (Redstone r : elements) {
            r.setId(startindex);
            startindex++;
        }
        for (CircuitBase cb : subcircuits) {
            startindex = cb.generateIds(startindex);
        }
        return startindex;
    }

    public File getFile() {
        // to be overridden
        return null;
    }

    public String getFullName() {
        return name;
    }

    public final boolean isSaved() {
        return getFile().exists();
    }

    public final boolean load() {
        return this.load(getFile());
    }

    public final boolean save() {
        return this.save(getFile());
    }

    public final boolean load(File file) {
        loaded = false;
        new DataReader(file) {
            @Override
            public void read(DataInputStream stream) throws IOException {
                CircuitBase.this.load(stream);
                loaded = true;
            }
        }.read();
        return loaded;
    }

    public final boolean save(File file) {
        saved = false;
        new DataWriter(file) {
            @Override
            public void write(DataOutputStream stream) throws IOException {
                CircuitBase.this.save(stream);
                saved = true;
            }
        }.write();
        return saved;
    }

    /**
     * Loads this Circuit using the stream specified
     * 
     * @param stream to read from
     * @throws IOException
     */
    public abstract void load(DataInputStream stream) throws IOException;

    /**
     * Saves this Circuit to the stream specified
     * 
     * @param stream to write to
     * @throws IOException
     */
    public abstract void save(DataOutputStream stream) throws IOException;
}
