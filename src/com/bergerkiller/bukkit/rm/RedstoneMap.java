package com.bergerkiller.bukkit.rm;

import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.World;
import org.bukkit.block.Block;

import com.bergerkiller.bukkit.common.BlockLocation;
import com.bergerkiller.bukkit.common.collections.BlockMap;
import com.bergerkiller.bukkit.rm.element.Component;

/**
 * Maps Redstone instances to blocks in (possibly) multiple worlds
 */
public class RedstoneMap {
    private BlockMap<RedstoneContainer> blocks = new BlockMap<>();
    private HashMap<Component, HashSet<RedstoneContainer>> maps = new HashMap<>();

    public RedstoneContainer get(Block block) {
        return this.get(new BlockLocation(block));
    }

    public RedstoneContainer get(World world, int x, int y, int z) {
        return this.get(new BlockLocation(world, x, y, z));
    }

    public RedstoneContainer get(BlockLocation block) {
        RedstoneContainer m = this.blocks.get(block);
        if (m == null) {
            m = new RedstoneContainer(this);
            this.blocks.put(block, m);
        }
        return m;
    }

    public HashSet<RedstoneContainer> getMaps(Component redstone) {
        HashSet<RedstoneContainer> map = this.maps.get(redstone);
        if (map == null) {
            map = new HashSet<>();
            this.maps.put(redstone, map);
        }
        return map;
    }

    public void merge(Component from, Component to) {
        HashSet<RedstoneContainer> rmaps = this.getMaps(from);
        for (RedstoneContainer map : rmaps) {
            this.setValue(map, to);
        }
        this.maps.remove(from);
    }

    protected void setValue(RedstoneContainer map, Component value) {
        this.getMaps(value).add(map);
    }
}
