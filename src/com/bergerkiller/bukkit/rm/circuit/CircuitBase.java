package com.bergerkiller.bukkit.rm.circuit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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
        for (Component r : this.elements) {
            r.onTick();
        }
        for (CircuitInstance ci : this.subcircuits) {
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
        this.ports.clear();
        for (Component r : this.elements) {
            r.setCircuit(this);
            if (r instanceof Port) {
                this.ports.put(((Port) r).name, (Port) r);
            }
        }
        for (CircuitBase c : this.subcircuits) {
            c.initialize(false);
        }
        if (generateIds) {
            this.generateIds(0);
        }
        this.initialized = true;
    }

    /**
     * Gets whether the circuit is initialized
     * 
     * @return if the circuit is initialised
     */
    public boolean isInitialized() {
        return this.initialized;
    }

    /**
     * Gets a port given the name of the port
     * 
     * @param name the port
     * @return
     */
    public Port getPort(String name) {
        return this.ports.get(name);
    }

    /**
     * Gets all ports in this circuits
     * 
     * @return a collection of ports
     */
    public Collection<Port> getPorts() {
        return this.ports.values();
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
            for (Component r : this.elements) {
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
            for (CircuitBase c : this.subcircuits) {
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
        if (id < this.elements.length) {
            return this.elements[id];
        } else {
            for (CircuitBase sub : this.subcircuits) {
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
        for (int i = 0; i < this.elements.length; i++) {
            if (this.elements[i] == element) {
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
        Component[] newElements = new Component[this.elements.length - 1];
        for (int i = 0; i < index; i++) {
            newElements[i] = this.elements[i];
        }
        for (int i = index; i < newElements.length; i++) {
            newElements[i] = this.elements[i + 1];
        }
        this.elements = newElements;
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
        for (CircuitBase cb : this.subcircuits) {
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
        for (Component r : this.elements) {
            r.setId(startindex);
            startindex++;
        }
        for (CircuitBase cb : this.subcircuits) {
            startindex = cb.generateIds(startindex);
        }
        return startindex;
    }

    public abstract File getFile();

    public String getFullName() {
        return this.name;
    }

    public final boolean isSaved() {
        return this.getFile().exists();
    }

    public final boolean load() {
        return this.load(this.getFile());
    }

    public final boolean save() {
        return this.save(this.getFile());
    }

    public final boolean load(File file) {
        this.loaded = false;
        new DataReader(file) {
            @Override
            public void read(DataInputStream stream) throws IOException {
                CircuitBase.this.load(stream);
                CircuitBase.this.loaded = true;
            }
        }.read();
        return this.loaded;
    }

    public final boolean save(File file) {
        this.saved = false;
        new DataWriter(file) {
            @Override
            public void write(DataOutputStream stream) throws IOException {
                CircuitBase.this.save(stream);
                CircuitBase.this.saved = true;
            }
        }.write();
        return this.saved;
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

    @Override
    public String toString() {
        return String.format("CircuitBase [name=%s, elements=%s, subcircuits=%s, ports=%s, initialized=%s, loaded=%s, saved=%s, links=%s]", name, Arrays.toString(elements),
                Arrays.toString(subcircuits), ports, initialized, loaded, saved, showLinks());
    }

    private String showLinks() {
        Set<String> set = new HashSet<>();
        for (Component c1 : this.elements) {
            for (Component c2 : c1.mainInputs) {
                set.add(String.format("%s --> %s", c2, c1));
            }
            for (Component c2 : c1.sideInputs) {
                set.add(String.format("%s --| %s", c2, c1));
            }
            for (Component c2 : c1.mainOutputs) {
                set.add(String.format("%s --> %s", c1, c2));
            }
            for (Component c2 : c1.sideOutputs) {
                set.add(String.format("%s --| %s", c1, c2));
            }
        }
        return set.toString();
    }
}
