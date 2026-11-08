package cz.ceph.LampControl.utils;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Isolated so that WorldEdit classes are only loaded when WorldEdit is installed. */
final class WorldEditHook {

    private WorldEditHook() {
    }

    /** @return blocks of the player's current selection, or null if there is none. */
    static List<Block> selectedBlocks(Player player) {
        com.sk89q.worldedit.entity.Player wePlayer = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(wePlayer);
        com.sk89q.worldedit.world.World weWorld = session.getSelectionWorld();
        if (weWorld == null) return null;

        Region region;
        try {
            region = session.getSelection(weWorld);
        } catch (IncompleteRegionException e) {
            return null;
        }

        World world = BukkitAdapter.adapt(weWorld);
        List<Block> blocks = new ArrayList<>();
        for (BlockVector3 v : region) {
            blocks.add(world.getBlockAt(v.x(), v.y(), v.z()));
        }
        return blocks;
    }
}
