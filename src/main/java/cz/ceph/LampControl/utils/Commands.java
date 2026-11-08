/*
	Code has been adapted from jacklink01.
	Code is modified by Ceph.
	GNU General Public License version 3 (GPLv3)
*/
package cz.ceph.LampControl.utils;

import cz.ceph.LampControl.LampControl;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class Commands implements CommandExecutor {

    private final LampControl main;

    public Commands(LampControl main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length >= 2) {
            sender.sendMessage(MessagesManager.PREFIX + MessagesManager.TOO_MANY_ARGUMENTS.toString());
            return true;
        }
        if (args.length == 1 && !args[0].equalsIgnoreCase("off")) {
            sender.sendMessage(ChatColor.YELLOW + "-- " + LampControl.pluginInfo.getName() + " v" + LampControl.pluginInfo.getVersion() + " --");
            sender.sendMessage(ChatColor.RED + "/lamp or //lamp - Will turn on selected lamps and shelves");
            sender.sendMessage(ChatColor.RED + "/lamp off or //lamp off - Will turn off selected lamps and shelves");
            return true;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(MessagesManager.PREFIX + MessagesManager.CONSOLE.toString());
            return true;
        }
        if (!p.hasPermission("lampcontrol.worldedit") && !p.isOp()) {
            p.sendMessage(MessagesManager.PREFIX + MessagesManager.NO_PERMS.toString());
            return true;
        }
        if (Bukkit.getPluginManager().getPlugin("WorldEdit") == null
                && Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") == null) {
            p.sendMessage(MessagesManager.PREFIX + MessagesManager.NO_WORLDEDIT.toString());
            return true;
        }

        boolean turnOn = args.length == 0;
        // WorldEdit classes are only touched here, so the plugin still loads without WorldEdit.
        List<Block> blocks = WorldEditHook.selectedBlocks(p);
        if (blocks == null) {
            p.sendMessage(MessagesManager.PREFIX + MessagesManager.NO_SELECTION.toString());
            return true;
        }

        BlockSwitcher switcher = main.getSwitcher();
        int affected = 0;
        for (Block block : blocks) {
            boolean lamp = BlockSwitcher.isLamp(block);
            boolean shelf = LampControl.controlShelves && BlockSwitcher.isShelf(block);
            if (!lamp && !shelf) continue;
            if (BlockSwitcher.isOn(block) == turnOn) continue;
            if (lamp) switcher.switchLamp(block, turnOn);
            else switcher.switchShelf(block, turnOn);
            affected++;
        }

        if (affected < 1) {
            p.sendMessage(MessagesManager.PREFIX + MessagesManager.NO_LAMPS_AFFECTED.toString());
        } else {
            MessagesManager msg = turnOn ? MessagesManager.ON_LAMPS : MessagesManager.OFF_LAMPS;
            p.sendMessage(MessagesManager.PREFIX + msg.toString().replace("%affected", String.valueOf(affected)));
        }
        return true;
    }
}
