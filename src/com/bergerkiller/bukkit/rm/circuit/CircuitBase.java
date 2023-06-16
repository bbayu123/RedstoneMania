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
import com.bergerkiller.bukkit.rm.element.Component;
import com.bergerkiller.bukkit.rm.element.Port;

/**
 * An abstract circuit
 * 
 * @author bergerkiller
 *
 */
public abstract class CircuitBase {
    /**
     * The name of the circuit
     */
    public String name;
    /**
     * The components of the circuit
     */
    public Component[] elements;
    /**
     * The sub-circuits within this circuit
     */
    public CircuitInstance[] subcircuits;
    /**
     * The ports of the circuit
     */
    private HashMap<String, Port> ports = new HashMap<>();

    /**
     * Whether the circuit is initialized or not
     */
    private boolean initialized = false;
    /**
     * Whether the circuit is loaded or not
     */
    private boolean loaded = false;
    /**
     * Whether the circuit is saved or not
     */
    private boolean saved = false;

    /**
     * Ticks all the elements in this Circuit
     */
    public void onTick() {
        for (Component r : elements) {
            r.onTick();
        }
        for (CircuitInstance ci : subcircuits) {
            ci.onTick();
        }
    }

    /**
     * Initializes the circuit
     */
    public void initialize() {
        this.initialize(true);
    }

    /**
     * Initializes the circuit
     * 
     * @param generateIds whether to generate IDs or not
     */
    private void initialize(boolean generateIds) {
        ports.clear();
        for (Component r : elements) {
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

    /**
     * Gets whether the circuit is initialized
     * 
     * @return if the circuit is initialised
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Gets a port given the name of the port
     * 
     * @param name the port
     * @return
     */
    public Port getPort(String name) {
        return ports.get(name);
    }

    /**
     * Gets all ports in this circuits
     * 
     * @return a collection of ports
     */
    public Collection<Port> getPorts() {
        return ports.values();
    }

    /**
     * Fixes direct connections
     */
    public void fixDirectConnections() {
        this.fixDirectConnections(true);
    }

    /**
     * Fixes direct connections
     * 
     * @param checksub whether to check sub-circuits or not
     * @return if the circuit has been altered due to the fixing or not
     */
    private boolean fixDirectConnections(boolean checksub) {
        boolean changed = false;
        boolean hasDirectConnections = true;
        while (hasDirectConnections) {
            hasDirectConnections = false;
            for (Component r : elements) {
                Component direct = r.findDirectConnection();
                if (direct == null) continue;
                hasDirectConnections = true;
                changed = true;
                // remove which: direct or r?
                if (!(direct instanceof Port)) {
                    direct.disable();
                } else if (!(r instanceof Port)) {
                    r.disable();
                } else {
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

    /**
     * Gets the element with the specified ID
     * 
     * @param id the ID of the element
     * @return the element
     */
    public Component getElement(int id) {
        if (id < elements.length) {
            return elements[id];
        } else {
            for (CircuitBase sub : subcircuits) {
                for (Component r : sub.elements) {
                    if (r.getId() == id) return r;
                }
            }
            return null;
        }
    }

    /**
     * Gets the element that matches the provide component
     * 
     * @param guide the component to match
     * @return the element
     */
    public Component getElement(Component guide) {
        return this.getElement(guide.getId());
    }

    /**
     * Removes an element matching the provide component
     * 
     * @param element the component to be removed
     */
    public void removeElement(Component element) {
        for (int i = 0; i < elements.length; i++) {
            if (elements[i] == element) {
                this.removeElement(i);
                return;
            }
        }
    }

    /**
     * Removes an element at an index
     * 
     * @param index the index to remove
     */
    private void removeElement(int index) {
        Component[] newElements = new Component[elements.length - 1];
        for (int i = 0; i < index; i++) {
            newElements[i] = elements[i];
        }
        for (int i = index; i < newElements.length; i++) {
            newElements[i] = elements[i + 1];
        }
        elements = newElements;
    }

    /**
     * Generates a list of elements which do not require another circuit to exist
     * <p>
     * Removes all dependencies and internal ports at the cost of a large schematic
     * 
     * @return the list of elements
     */
    public Component[] getIndependentElements() {
        return this.getIndependentElements(true);
    }

    private Component[] getIndependentElements(boolean main) {
        ArrayList<Component> elements = new ArrayList<>();
        for (Component r : this.elements) {
            if (r != null && !r.isDisabled()) {
                if (!main && r instanceof Port) {
                    r = r.clone();
                }
                elements.add(r);
            }
        }
        for (CircuitBase cb : subcircuits) {
            for (Component r : cb.getIndependentElements()) {
                elements.add(r);
            }
        }
        return elements.toArray(new Component[0]);
    }

    public Circuit getIndependentCircuit() {
        Circuit c = new Circuit();
        c.elements = this.getIndependentElements();
        c.subcircuits = new CircuitInstance[0];
        c.initialize();
        return c;
    }

    private int generateIds(int startindex) {
        for (Component r : elements) {
            r.setId(startindex);
            startindex++;
        }
        for (CircuitBase cb : subcircuits) {
            startindex = cb.generateIds(startindex);
        }
        return startindex;
    }

    public abstract File getFile();

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
