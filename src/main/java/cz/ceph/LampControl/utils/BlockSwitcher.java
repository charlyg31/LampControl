/*
	Code has been adapted from richie3366's LumosMaxima.
	Code is modified by Ceph.
	GNU General Public License version 3 (GPLv3)
*/
package cz.ceph.LampControl.utils;

import cz.ceph.LampControl.LampControl;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.Rail;
import org.bukkit.block.data.SideChaining;
import org.bukkit.block.data.type.RedstoneRail;
import org.bukkit.block.data.type.Shelf;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Switches lamps, powered rails and shelves without a redstone signal.
 * <p>
 * Blocks switched on by hand are remembered in the chunk's persistent data so the
 * physics listener can stop vanilla from turning them back off on the next block update.
 */
public class BlockSwitcher {

    /** Vanilla connects at most 3 powered shelves. */
    private static final int MAX_SHELF_CHAIN = 3;
    /** Safety bound when walking along a row of shelves. */
    private static final int MAX_SHELF_SCAN = 64;
    /** Same range as the original plugin when powering a line of rails. */
    private static final int RAIL_RANGE = 8;

    private final LampControl plugin;

    public BlockSwitcher(LampControl plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ block kinds

    public static boolean isLamp(Block block) {
        return block.getType() == Material.REDSTONE_LAMP;
    }

    public static boolean isPoweredRail(Block block) {
        return block.getType() == Material.POWERED_RAIL;
    }

    public static boolean isShelf(Block block) {
        return block.getBlockData() instanceof Shelf;
    }

    /** True if the block is a lamp/rail/shelf currently in its "on" state. */
    public static boolean isOn(Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof Shelf shelf) return shelf.isPowered();
        if (data instanceof RedstoneRail rail) return rail.isPowered();
        if (data instanceof Lightable lamp && block.getType() == Material.REDSTONE_LAMP) return lamp.isLit();
        return false;
    }

    // ------------------------------------------------------------------ lamps

    public void switchLamp(Block block, boolean light) {
        if (!(block.getBlockData() instanceof Lightable lamp)) return;
        lamp.setLit(light);
        block.setBlockData(lamp, false);
        setManual(block, light);
    }

    // ------------------------------------------------------------------ rails

    public void switchRail(Block block, boolean power) {
        if (!(block.getBlockData() instanceof RedstoneRail rail)) return;
        rail.setPowered(power);
        block.setBlockData(rail, false);
        setManual(block, power);
    }

    /** Powers the clicked rail and the unpowered rails in line with it, like the original plugin. */
    public void switchRailLine(Block block) {
        for (BlockFace side : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            for (int i = 1; i <= RAIL_RANGE; i++) {
                Block next = block.getRelative(side, i);
                if (!(next.getBlockData() instanceof RedstoneRail rail) || rail.isPowered()) break;
                if (!isStraightAlong(rail, side)) break;
                switchRail(next, true);
            }
        }
        switchRail(block, true);
    }

    private static boolean isStraightAlong(RedstoneRail rail, BlockFace side) {
        Rail.Shape shape = rail.getShape();
        boolean northSouth = side == BlockFace.NORTH || side == BlockFace.SOUTH;
        return northSouth
                ? shape == Rail.Shape.NORTH_SOUTH || shape == Rail.Shape.ASCENDING_NORTH || shape == Rail.Shape.ASCENDING_SOUTH
                : shape == Rail.Shape.EAST_WEST || shape == Rail.Shape.ASCENDING_EAST || shape == Rail.Shape.ASCENDING_WEST;
    }

    // ------------------------------------------------------------------ shelves

    /**
     * Turns a shelf's powered look on/off and reconnects it with its neighbours
     * (powered shelves facing the same way chain together, 3 max).
     */
    public void switchShelf(Block block, boolean power) {
        if (!(block.getBlockData() instanceof Shelf shelf)) return;
        BlockFace facing = shelf.getFacing();

        shelf.setPowered(power);
        shelf.setSideChain(SideChaining.ChainPart.UNCONNECTED);
        block.setBlockData(shelf, false);
        setManual(block, power);

        if (power) relinkRow(block, facing);
        else relinkNeighbours(block, facing);
    }

    /** A shelf left the row (turned off or broken): the shelves on each side form their own chains. */
    public void relinkNeighbours(Block block, BlockFace facing) {
        BlockFace left = leftOf(facing);
        Block l = block.getRelative(left);
        Block r = block.getRelative(left.getOppositeFace());
        if (isPoweredShelfFacing(l, facing)) relinkRow(l, facing);
        if (isPoweredShelfFacing(r, facing)) relinkRow(r, facing);
    }

    /** Recomputes side_chain for the whole row of powered shelves containing {@code start}. */
    private void relinkRow(Block start, BlockFace facing) {
        BlockFace left = leftOf(facing);
        BlockFace right = left.getOppositeFace();

        Block leftmost = start;
        for (int i = 0; i < MAX_SHELF_SCAN; i++) {
            Block next = leftmost.getRelative(left);
            if (!isPoweredShelfFacing(next, facing)) break;
            leftmost = next;
        }

        List<Block> row = new ArrayList<>();
        Block cursor = leftmost;
        for (int i = 0; i < MAX_SHELF_SCAN * 2 && isPoweredShelfFacing(cursor, facing); i++) {
            row.add(cursor);
            cursor = cursor.getRelative(right);
        }

        for (int from = 0; from < row.size(); from += MAX_SHELF_CHAIN) {
            int size = Math.min(MAX_SHELF_CHAIN, row.size() - from);
            for (int j = 0; j < size; j++) {
                SideChaining.ChainPart part;
                if (size == 1) part = SideChaining.ChainPart.UNCONNECTED;
                else if (j == 0) part = SideChaining.ChainPart.LEFT;
                else if (j == size - 1) part = SideChaining.ChainPart.RIGHT;
                else part = SideChaining.ChainPart.CENTER;
                setChain(row.get(from + j), part);
            }
        }
    }

    private static void setChain(Block block, SideChaining.ChainPart part) {
        if (!(block.getBlockData() instanceof Shelf shelf) || shelf.getSideChain() == part) return;
        shelf.setSideChain(part);
        block.setBlockData(shelf, false);
    }

    private static boolean isPoweredShelfFacing(Block block, BlockFace facing) {
        return block.getBlockData() instanceof Shelf shelf && shelf.isPowered() && shelf.getFacing() == facing;
    }

    /**
     * Left-hand side for a player standing in front of a shelf, looking at it.
     * (A shelf facing SOUTH is looked at towards NORTH, so its left is WEST.)
     */
    private static BlockFace leftOf(BlockFace facing) {
        return switch (facing) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> BlockFace.EAST;
        };
    }

    // ------------------------------------------------------------------ "switched by hand" marker

    private NamespacedKey keyFor(Block block) {
        return new NamespacedKey(plugin, "m_" + block.getX() + "_" + block.getY() + "_" + block.getZ());
    }

    public boolean isManual(Block block) {
        return block.getChunk().getPersistentDataContainer().has(keyFor(block), PersistentDataType.BYTE);
    }

    public void setManual(Block block, boolean manual) {
        PersistentDataContainer pdc = block.getChunk().getPersistentDataContainer();
        NamespacedKey key = keyFor(block);
        if (manual) pdc.set(key, PersistentDataType.BYTE, (byte) 1);
        else if (pdc.has(key, PersistentDataType.BYTE)) pdc.remove(key);
    }
}
