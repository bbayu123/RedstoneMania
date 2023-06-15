package com.bergerkiller.bukkit.rm;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockRedstoneEvent;

import com.bergerkiller.bukkit.common.MaterialTypeProperty;
import com.bergerkiller.bukkit.common.utils.BlockUtil;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.FaceUtil;
import com.bergerkiller.bukkit.common.utils.LogicUtil;

public class Util {
    public static final MaterialTypeProperty ISSOLID = new MaterialTypeProperty(Material.STONE, Material.COBBLESTONE, Material.GRASS, Material.DIRT, Material.OAK_WOOD,
            Material.SPRUCE_WOOD, Material.BIRCH_WOOD, Material.JUNGLE_WOOD, Material.ACACIA_WOOD, Material.DARK_OAK_WOOD, Material.OAK_LOG, Material.SPRUCE_LOG,
            Material.BIRCH_LOG, Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.NETHERRACK, Material.IRON_BLOCK, Material.GOLD_BLOCK,
            Material.DIAMOND_BLOCK, Material.SAND, Material.SANDSTONE, Material.BEDROCK, Material.REDSTONE_ORE, Material.COAL_ORE, Material.DIAMOND_ORE, Material.IRON_ORE,
            Material.GOLD_ORE, Material.STONE_BRICKS, Material.BRICK, Material.CLAY, Material.LAPIS_BLOCK, Material.LAPIS_ORE, Material.SPONGE, Material.SNOW,
            Material.BROWN_MUSHROOM_BLOCK, Material.RED_MUSHROOM_BLOCK);

    /**
     * Checks whether one block is attached to another
     * 
     * @param block that is attachable
     * @param to    block to check against
     * @return True if the block is attached to the to block, False if not
     */
    public static boolean isAttached(Block block, Block to) {
        return BlockUtil.equals(BlockUtil.getAttachedBlock(block), to);
    }

    /**
     * Strips a name from all file-path unsupported characters
     * 
     * @param name to fix
     * @return Fixed name
     */
    public static String fixName(String name) {
        StringBuilder newName = new StringBuilder(name.length());
        for (char c : name.toCharArray()) {
            if (LogicUtil.containsChar(c, '>', '<', '|', '^', '/', '\\', ':', '?', '"', '*')) {
                newName.append(' ');
            } else {
                newName.append(c);
            }
        }
        return newName.toString();
    }

    /**
     * Toggles the powered state of a block to perform a certain action
     * 
     * @param mainblock to toggle
     * @param toggled   state to set to
     */
    public static void setBlock(Block mainblock, boolean toggled) {
        if (mainblock != null) {
            for (BlockFace face : FaceUtil.ATTACHEDFACESDOWN) {
                Block b = mainblock.getRelative(face);
                Material type = b.getType();
                if (type == Material.LEVER) {
                    if (Util.isAttached(b, mainblock)) {
                        BlockUtil.setLever(b, toggled);
                    }
                }
            }
            BlockRedstoneEvent event = new BlockRedstoneEvent(mainblock, mainblock.getBlockPower(), toggled ? 15 : 0);
            CommonUtil.callEvent(event);
        }
    }
}