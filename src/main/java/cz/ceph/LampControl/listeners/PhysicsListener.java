package cz.ceph.LampControl.listeners;

import cz.ceph.LampControl.LampControl;
import cz.ceph.LampControl.utils.BlockSwitcher;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Keeps lamps, rails and shelves switched on by hand in their "on" state:
 * without this, vanilla turns them off on the next neighbour update because
 * they receive no redstone signal.
 */
public class PhysicsListener implements Listener {
    private final LampControl plugin;

    public PhysicsListener(LampControl plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPhysics(BlockPhysicsEvent e) {
        Block block = e.getBlock();
        Material type = block.getType();
        if (type != Material.REDSTONE_LAMP && type != Material.POWERED_RAIL && !BlockSwitcher.isShelf(block)) return;
        if (!BlockSwitcher.isOn(block)) return;
        if (plugin.getSwitcher().isManual(block)) e.setCancelled(true);
    }

    // A new block placed where a hand-switched block used to be must not inherit the marker.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (e.getBlockReplacedState().getType() == e.getBlockPlaced().getType()) return; // our own build check
        plugin.getSwitcher().setManual(e.getBlockPlaced(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        plugin.getSwitcher().setManual(block, false);
        if (BlockSwitcher.isShelf(block) && BlockSwitcher.isOn(block)) {
            BlockFace facing = BlockSwitcher.facingOf(block);
            if (facing == null) return;
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getSwitcher().relinkNeighbours(block, facing));
        }
    }
}
