package com.scamline.game;

import com.scamline.ScamLinePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RoomManager {

    private final ScamLinePlugin plugin;
    private final List<Location> rooms = new ArrayList<>();
    private final Map<UUID, Location> roomOf = new HashMap<>();
    private final List<Entity> frames = new ArrayList<>();
    private Location corridor;

    public RoomManager(ScamLinePlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        rooms.clear();
        for (String s : plugin.getConfig().getStringList("rooms")) {
            Location l = parseLocation(s);
            if (l != null) rooms.add(l);
        }
        corridor = parseLocation(plugin.getConfig().getString("corridor", ""));
    }

    public int roomCount() {
        return rooms.size();
    }

    public Location corridor() {
        return corridor.clone();
    }

    public void assign(List<Player> players) {
        roomOf.clear();
        List<Location> shuffled = new ArrayList<>(rooms);
        Collections.shuffle(shuffled);
        for (int i = 0; i < players.size(); i++) {
            Location room = shuffled.get(i);
            roomOf.put(players.get(i).getUniqueId(), room.clone());
            players.get(i).teleport(room);
        }
    }

    public void forget() {
        roomOf.clear();
    }

    public void spawnChoiceFrames(Player player) {
        Location room = roomOf.get(player.getUniqueId());
        if (room == null) return;
        Vector shareOffset = parseVector(plugin.getConfig().getString("frames.share-offset", "-2,1,0"));
        Vector stealOffset = parseVector(plugin.getConfig().getString("frames.steal-offset", "2,1,0"));
        spawnFrame(room.clone().add(shareOffset), Material.GREEN_CONCRETE, "scamline_share");
        spawnFrame(room.clone().add(stealOffset), Material.RED_CONCRETE, "scamline_steal");
    }

    private void spawnFrame(Location loc, Material material, String tag) {
        ItemFrame frame = loc.getWorld().spawn(loc, ItemFrame.class, f -> {
            f.setFacingDirection(BlockFace.SOUTH, true);
            f.setVisible(false);
            f.setFixed(true);
            f.setItem(new ItemStack(material), false);
            f.addScoreboardTag(tag);
        });
        frames.add(frame);
    }

    public void clearChoiceFrames() {
        frames.forEach(Entity::remove);
        frames.clear();
    }

    private Location parseLocation(String s) {
        if (s == null || s.isBlank()) return null;
        String[] p = s.split(",");
        World world = Bukkit.getWorld(p[0].trim());
        if (world == null) {
            plugin.getLogger().warning("Dünya bulunamadı: " + p[0]);
            return null;
        }
        float yaw = p.length > 4 ? Float.parseFloat(p[4].trim()) : 0f;
        return new Location(world,
                Double.parseDouble(p[1].trim()),
                Double.parseDouble(p[2].trim()),
                Double.parseDouble(p[3].trim()),
                yaw, 0f);
    }

    private static Vector parseVector(String s) {
        String[] p = s.split(",");
        return new Vector(
                Double.parseDouble(p[0].trim()),
                Double.parseDouble(p[1].trim()),
                Double.parseDouble(p[2].trim()));
    }
}
