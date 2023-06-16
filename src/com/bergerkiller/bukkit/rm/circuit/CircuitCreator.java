package com.bergerkiller.bukkit.rm.circuit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.BlockLocation;
import com.bergerkiller.bukkit.common.collections.BlockMap;
import com.bergerkiller.bukkit.common.utils.BlockUtil;
import com.bergerkiller.bukkit.common.utils.FaceUtil;
import com.bergerkiller.bukkit.common.utils.MaterialUtil;
import com.bergerkiller.bukkit.rm.PlayerSelect;
import com.bergerkiller.bukkit.rm.RedstoneContainer;
import com.bergerkiller.bukkit.rm.RedstoneMania;
import com.bergerkiller.bukkit.rm.RedstoneMap;
import com.bergerkiller.bukkit.rm.Util;
import com.bergerkiller.bukkit.rm.element.Component;
import com.bergerkiller.bukkit.rm.element.Inverter;
import com.bergerkiller.bukkit.rm.element.PhysicalPort;
import com.bergerkiller.bukkit.rm.element.Port;
import com.bergerkiller.bukkit.rm.element.Repeater;
import com.bergerkiller.bukkit.rm.element.SolidComponent;
import com.bergerkiller.bukkit.rm.element.Wire;

/**
 * Creates new Circuit instances from player-selected areas on the world
 */
public class CircuitCreator {
    private static final int TORCH_DELAY = 2; // Tick delay of a Redstone torch
    private Player by;
    private RedstoneMap map = new RedstoneMap();
    private ArrayList<Component> items = new ArrayList<>();
    private HashMap<String, CircuitInstance> subcircuits = new HashMap<>();
    private ArrayList<Block> ports = new ArrayList<>();
    private BlockMap<Integer> delays = new BlockMap<>();

    public CircuitCreator(Player by, PlayerSelect from) {
        this.by = by;
        // prepare the ports, items and delays
        this.delays.putAll(from.getDelays());
        for (Map.Entry<String, BlockLocation> entry : from.getPorts().entrySet()) {
            Port p = new Port();
            p.name = entry.getKey();
            Block b = entry.getValue().getBlock();
            this.ports.add(b);
            this.map.get(b).setValue(p).setPosition(entry.getValue().x, entry.getValue().z);
            this.items.add(p);
        }
    }

    /**
     * Creates and saves a new Circuit instance from the information in this Circuit Creator
     * 
     * @return new Circuit instance
     */
    public Circuit create() {
        // generate circuit for ALL ports
        for (Block p : this.ports) {
            this.createComponent(this.map.get(p).value, p, Material.REDSTONE_WIRE);
        }
        // Set the position offset so the circuit will be nicely centered at 0x0
        double midx = 0;
        double midz = 0;
        for (Component r : this.items) {
            midx += r.getX() / this.items.size();
            midz += r.getZ() / this.items.size();
        }
        for (Component r : this.items) {
            r.setPosition(r.getX() - (int) midx, r.getZ() - (int) midz);
        }

        // save
        Circuit c = new Circuit();
        c.elements = this.items.toArray(new Component[0]);
        c.subcircuits = this.subcircuits.values().toArray(new CircuitInstance[0]);
        c.initialize();
        return c;
    }

    private int getDelay(Block b, Material type) {
        BlockLocation pos = new BlockLocation(b);
        if (this.delays.containsKey(pos)) {
            return this.delays.get(pos);
        } else if (MaterialUtil.ISREDSTONETORCH.get(type)) {
            return TORCH_DELAY;
        } else if (MaterialUtil.ISDIODE.get(type)) {
            return ((org.bukkit.block.data.type.Repeater) b.getBlockData()).getDelay() * TORCH_DELAY;
        } else {
            return 0;
        }
    }

    private void msg(String message) {
        this.by.sendMessage(ChatColor.YELLOW + message);
    }

    /**
     * Removes the Redstone from one position and places it again in to
     * 
     * @param from Redstone to transfer
     * @param to   Redstone to replace
     */
    private void transfer(Component from, Component to) {
        if (from != to) {
            this.map.merge(from, to);
            from.transfer(to);
            this.items.remove(from);
        }
    }

    /**
     * Creates the information of a single Block
     * 
     * @param block to create
     * @return Redstone Container of the resulting block
     */
    private RedstoneContainer create(Block block) {
        Material type = block.getType();
        RedstoneContainer m = this.map.get(block);
        if (m.value != null) {
            return m;
        }

        if (MaterialUtil.ISREDSTONETORCH.get(type)) {
            // Creates an inverter
            m.setValue(new Inverter()).setPosition(block);
            m.value.setPowered(((Lightable) block.getBlockData()).isLit(), false);
            m.value.setDelay(this.getDelay(block, type));
            this.items.add(m.value);
            this.createInverter((Inverter) m.value, block, type);
        } else if (MaterialUtil.ISDIODE.get(type)) {
            // Creates a repeater
            org.bukkit.block.data.type.Repeater blockData = (org.bukkit.block.data.type.Repeater) block.getBlockData();
            m.setValue(new Repeater()).setPosition(block);
            m.value.setPowered(blockData.isPowered(), blockData.isLocked());
            m.value.setDelay(this.getDelay(block, type));
            this.items.add(m.value);
            this.createRepeater((Repeater) m.value, block, type);
        } else if (type == Material.REDSTONE_WIRE) {
            // Creates a wire
            m.setValue(new Wire()).setPosition(block);
            m.value.setPowered(((AnaloguePowerable) block.getBlockData()).getPower() > 0, false);
            this.items.add(m.value);
            this.createComponent(m.value, block, type);
        } else if (type == Material.LEVER) {
            // Creates a port
            Port searchport = Port.get(block);
            if (searchport != null) {
                CircuitBase base = searchport.getCircuit();
                if (base == null) {
                    RedstoneMania.plugin.log(Level.SEVERE, "[Creation] Failed to obtain circuit from port '" + searchport.name + "'!");
                } else {
                    // Create a new circuit instance
                    CircuitInstance cb = (CircuitInstance) base;
                    String fullname = cb.getFullName();
                    CircuitInstance ci = this.subcircuits.get(fullname);
                    if (ci == null) {
                        ci = cb.source.createInstance();
                        this.subcircuits.put(fullname, ci);
                    }
                    if (ci == null) {
                        RedstoneMania.plugin.log(Level.SEVERE, "[Creation] Failed to convert circuit '" + base.getFullName() + "'!");
                    } else {
                        // get the ports of the found circuit
                        Collection<Port> realports = base.getPorts();
                        for (Port realport : realports) {
                            Port port = ci.getPort(realport.name);
                            if (port == null) {
                                RedstoneMania.plugin.log(Level.WARNING, "[Creation] Failed to find port '" + realport.name + "' in circuit '" + ci.getFullName() + "'!");
                            } else {
                                port.setPowered(realport.isPowered(), false);
                                boolean outofreach = false;
                                for (PhysicalPort pp : realport.locations) {
                                    Block at = pp.position.getBlock();
                                    if (at == null) {
                                        outofreach = true;
                                    } else {
                                        for (BlockFace leverface : FaceUtil.ATTACHEDFACES) {
                                            Block lever = at.getRelative(leverface);
                                            if (lever.getType() == Material.LEVER) {
                                                this.map.get(lever).setValue(port).setPosition(lever.getX(), lever.getZ());
                                                this.createPort(port, lever, Material.LEVER);
                                            }
                                        }
                                    }
                                }
                                if (outofreach) {
                                    this.msg("One or more ports of '" + ci.getFullName() + "' are out of reach!");
                                }
                            }
                        }
                    }
                }
            }
        } else if (Util.ISSOLID.get(type)) {
            this.createSolid(m.setValue(new SolidComponent(block)), block, type);
        }
        return m;
    }

    private void createPort(Port redstone, Block lever, Material type) {
        for (BlockFace face : FaceUtil.ATTACHEDFACESDOWN) {
            Block b = lever.getRelative(face);
            Material btype = b.getType();
            if (btype == Material.REDSTONE_WIRE) {
                if (face == BlockFace.DOWN) {
                    redstone.connectTo(this.create(b).value);
                } else {
                    Component other = this.create(b).value;
                    redstone.connectTo(other);
                    other.connectTo(redstone);
                }
            } else if (MaterialUtil.ISREDSTONETORCH.get(btype)) {
                this.create(b).value.connectTo(redstone);
            }
        }
    }

    private void createInverter(Inverter redstone, Block inverter, Material type) {
        for (BlockFace face : FaceUtil.ATTACHEDFACESDOWN) {
            Block b = inverter.getRelative(face);
            Material btype = b.getType();
            if (btype == Material.REDSTONE_WIRE) {
                redstone.connectTo(this.create(b).value);
            } else if (btype == Material.REPEATER) {
                if (face != BlockFace.DOWN) {
                    // connected to the input?
                    BlockFace facing = BlockUtil.getFacing(b);
                    if (facing == face) {
                        redstone.connectTo(this.create(b).value);
                    }
                }
            }
        }
        Block above = inverter.getRelative(BlockFace.UP);
        Material abovetype = above.getType();
        if (Util.ISSOLID.get(abovetype)) {
            this.create(above);
        }
        this.create(BlockUtil.getAttachedBlock(inverter));
    }

    private void createRepeater(Repeater redstone, Block repeater, Material type) {
        BlockFace facing = BlockUtil.getFacing(repeater);
        Block output = repeater.getRelative(facing);
        Material outputtype = output.getType();
        if (outputtype == Material.REDSTONE_WIRE) {
            // connect this repeater to wire
            redstone.connectTo(this.create(output).value);
        } else if (MaterialUtil.ISDIODE.get(outputtype)) {
            BlockFace oface = BlockUtil.getFacing(output);
            if (facing == oface) {
                // Same facing
                redstone.connectTo(this.create(output).value);
            } else if (facing != oface.getOppositeFace()) {
                // Side ways facing
                redstone.connectToSide(this.create(output).value);
            }
        } else if (Util.ISSOLID.get(outputtype)) {
            this.create(output);
        }
        Block input = repeater.getRelative(facing.getOppositeFace());
        Material inputtype = repeater.getType();
        if (inputtype == Material.REDSTONE_WIRE) {
            // connect this repeater to wire
            this.create(input).value.connectTo(redstone);
        } else if (MaterialUtil.ISDIODE.get(inputtype)) {
            BlockFace oface = BlockUtil.getFacing(input);
            if (facing == oface) {
                this.create(input).value.connectTo(redstone);
            }
        } else if (Util.ISSOLID.get(inputtype)) {
            this.create(input);
        }
    }

    private Component connectComponent(Block wire, Component redstone) {
        RedstoneContainer m = this.map.get(wire);
        if (m.value == redstone) {
            return redstone;
        }
        if (m.value == null) {
            m.setValue(redstone);
            // added block to this wire
            this.createComponent(redstone, wire, Material.REDSTONE_WIRE);
            return redstone;
        } else {
            // merge the two wires
            if (redstone instanceof Port) {
                if (m.value instanceof Port) {
                    Port p1 = (Port) redstone;
                    Port p2 = (Port) m.value;
                    this.msg("Port '" + p1.name + "' merged with port '" + p2.name + "'!");
                }
                this.transfer(m.value, redstone);
                return redstone;
            } else {
                this.transfer(redstone, m.value);
                return m.value;
            }
        }
    }

    private void createComponent(Component redstone, Block wire, Material type) {
        // wire - first find all nearby elements
        Block abovewire = wire.getRelative(BlockFace.UP);
        Material abovetype = abovewire.getType();
        for (BlockFace face : FaceUtil.AXIS) {
            Block b = wire.getRelative(face);
            Material btype = b.getType();
            if (btype == Material.REDSTONE_WIRE) {
                // same wire
                redstone = this.connectComponent(b, redstone);
            } else if (btype == Material.AIR) {
                // wire below?
                Block below = b.getRelative(BlockFace.DOWN);
                if (below.getType() == Material.REDSTONE_WIRE) {
                    redstone = this.connectComponent(below, redstone);
                }
            } else if (MaterialUtil.ISREDSTONETORCH.get(btype)) {
                // this wire receives input from this torch
                this.create(b); // we assume that the torch handles direct wire connection
            } else if (MaterialUtil.ISDIODE.get(btype)) {
                // powering or receiving power
                BlockFace facing = BlockUtil.getFacing(b);
                if (facing == face) {
                    // wire powers repeater
                    redstone.connectTo(this.create(b).value);
                } else if (facing.getOppositeFace() == face) {
                    // repeater powers wire
                    this.create(b); // we assume that the repeater handles direct wire connections
                }
            } else if (btype == Material.LEVER) {
                // let the port handle this
                this.create(b);
            } else if (abovetype == Material.AIR && Util.ISSOLID.get(btype)) {
                // wire on top?
                Block above = b.getRelative(BlockFace.UP);
                if (above.getType() == Material.REDSTONE_WIRE) {
                    redstone = this.connectComponent(above, redstone);
                }
                this.create(b);
            }
        }
        // Lever above?
        if (abovetype == Material.LEVER) {
            this.create(abovewire);
        }
        // update the block this wire sits on
        this.create(wire.getRelative(BlockFace.DOWN));
        // a torch above this wire?
        Block above = wire.getRelative(BlockFace.UP);
        if (MaterialUtil.ISREDSTONETORCH.get(above)) this.create(above);
    }

    private void createSolid(SolidComponent comp, Block block, Material type) {
        // create block data
        RedstoneContainer[] inputs = new RedstoneContainer[comp.inputs.size()];
        RedstoneContainer[] outputs = new RedstoneContainer[comp.outputs.size()];
        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = this.create(comp.inputs.get(i));
        }
        for (int i = 0; i < outputs.length; i++) {
            outputs[i] = this.create(comp.outputs.get(i));
        }
        // connect inputs with outputs
        for (RedstoneContainer input : inputs) {
            for (RedstoneContainer output : outputs) {
                if (input.value.isType(0, 3)) {
                    if (output.value.isType(0, 3)) {
                        // a wire does NOT power other wires!
                        continue;
                    }
                }
                input.value.connectTo(output.value);
            }
        }
    }
}
