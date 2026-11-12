/*
	Code has been adapted from jacklink01's LampControl.
	Code is modified by Ceph.
	GNU General Public License version 3 (GPLv3)
*/
package cz.ceph.LampControl.listeners;

import cz.ceph.LampControl.LampControl;
import cz.ceph.LampControl.utils.BlockSwitcher;
import cz.ceph.LampControl.utils.MessagesManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class InteractListener implements Listener {
    private final LampControl plugin;

    public InteractListener(LampControl plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND) return;
        Block block = e.getClickedBlock();
        if (block == null) return;

        Player player = e.getPlayer();
        if (player.isSneaking()) return;

        // Everything is done with an empty hand. Sneaking is left to vanilla (returned above),
        // so players can still take their items back from a shelf.
        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) return;
        if (LampControl.usePermissions && !player.hasPermission("lampcontrol.use")) return;

        if (BlockSwitcher.isShelf(block)) {
            if (LampControl.controlShelves) handleShelf(e, block);
        } else if (BlockSwitcher.isLamp(block)) {
            handleLamp(e, block);
        } else if (BlockSwitcher.isPoweredRail(block)) {
            if (LampControl.controlRails) handleRail(e, block);
        }
    }

    private void handleLamp(PlayerInteractEvent e, Block block) {
        boolean on = BlockSwitcher.isOn(block);
        if (on && !LampControl.toggleLamps) return;
        e.setCancelled(true);
        if (!canBuild(e.getPlayer(), block)) return;

        plugin.getSwitcher().switchLamp(block, !on);
        afterUse(e.getPlayer(), block, !on);
    }

    private void handleRail(PlayerInteractEvent e, Block block) {
        boolean on = BlockSwitcher.isOn(block);
        if (on && !LampControl.toggleLamps) return;
        e.setCancelled(true);
        if (!canBuild(e.getPlayer(), block)) return;

        if (on) plugin.getSwitcher().switchRail(block, false);
        else plugin.getSwitcher().switchRailLine(block);
        afterUse(e.getPlayer(), block, !on);
    }

    private void handleShelf(PlayerInteractEvent e, Block block) {
        boolean on = BlockSwitcher.isOn(block);
        if (on && !LampControl.toggleLamps) return;
        e.setCancelled(true); // don't take an item off the shelf / don't swap the hotbar
        if (!canBuild(e.getPlayer(), block)) return;

        plugin.getSwitcher().switchShelf(block, !on);
        afterUse(e.getPlayer(), block, !on);
    }

    /** Asks protection plugins (WorldGuard, GriefPrevention…) whether the player may build here. */
    private boolean canBuild(Player player, Block block) {
        BlockPlaceEvent check = new BlockPlaceEvent(block, block.getState(), block,
                new ItemStack(block.getType()), player, true, EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(check);
        if (check.isCancelled() || !check.canBuild()) {
            player.sendMessage(MessagesManager.PREFIX + MessagesManager.NO_PERMS.toString());
            return false;
        }
        return true;
    }

    private void afterUse(Player player, Block block, boolean turnedOn) {
        block.getWorld().playSound(block.getLocation(), Sound.UI_BUTTON_CLICK, 0.5F, turnedOn ? 1F : 0F);
    }
}
