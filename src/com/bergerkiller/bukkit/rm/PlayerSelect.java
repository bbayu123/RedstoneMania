package com.bergerkiller.bukkit.rm;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.BlockLocation;
import com.bergerkiller.bukkit.common.collections.BlockMap;
import com.bergerkiller.bukkit.common.utils.MaterialUtil;

public class PlayerSelect {
    private static HashMap<String, PlayerSelect> selections = new HashMap<>();

    public static PlayerSelect get(Player player) {
        PlayerSelect ps = selections.get(player.getName());
        if (ps == null) {
            ps = new PlayerSelect();
            selections.put(player.getName(), ps);
        }
        return ps;
    }

    private BlockLocation selectedblock;
    private BlockMap<Integer> delays = new BlockMap<>();
    private HashMap<String, BlockLocation> portnames = new HashMap<>();
    public int clickdelay = -1;

    /**
     * Gets a map of all blocks vs. a delay that is selected
     * 
     * @return Block delays
     */
    public BlockMap<Integer> getDelays() {
        return delays;
    }

    /**
     * Sets the tick delay for the currently selected block
     * 
     * @param delay in ticks to set
     */
    public void setDelay(int delay) {
        delays.put(selectedblock, delay);
    }

    /**
     * Clears all the block delays set
     */
    public void clearDelays() {
        delays.clear();
    }

    public Map<String, BlockLocation> getPorts() {
        return portnames;
    }

    public void setPort(String name) {
        portnames.put(name, selectedblock);
    }

    public void clearPorts() {
        portnames.clear();
    }

    public void set(Location l) {
        this.set(l.getBlock());
    }

    public void set(Block b) {
        selectedblock = new BlockLocation(b);
    }

    public boolean setDelay() {
        if (clickdelay >= 0 && isDelayable()) {
            this.setDelay(clickdelay);
            return true;
        } else {
            return false;
        }
    }

    public Block getBlock() {
        if (selectedblock == null) {
            return null;
        }
        return selectedblock.getBlock();
    }

    public Material getType() {
        Block b = getBlock();
        if (b == null) {
            return Material.AIR;
        }
        return b.getType();
    }

    public boolean isDelayable() {
        Material type = getType();
        return MaterialUtil.ISDIODE.get(type) || MaterialUtil.ISREDSTONETORCH.get(type);
    }
}
