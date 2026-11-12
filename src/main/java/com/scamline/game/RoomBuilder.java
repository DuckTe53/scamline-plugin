package com.scamline.game;

import com.scamline.ScamLinePlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 8 oda ve bir koridor kurar. Odalar X ekseninde 250 blok arayla dizilir,
 * koridor odaların 250 blok ilerisinde (Z ekseninde) durur.
 * Koordinatlar config.yml dosyasına yazılır.
 */
public final class RoomBuilder {

    public static final int SPACING = 250;
    private static final int ROOM_COUNT = 8;
    private static final int ROOM_W = 5; // X
    private static final int ROOM_D = 4; // Z
    private static final int ROOM_H = 5; // Y

    private final ScamLinePlugin plugin;

    public RoomBuilder(ScamLinePlugin plugin) {
        this.plugin = plugin;
    }

    /** origin: komutu çalıştıran oyuncunun konumu. Zemin, oyuncunun ayağının bir blok altıdır. */
    public void build(Location origin) {
        World world = origin.getWorld();
        int ox = origin.getBlockX();
        int floorY = origin.getBlockY() - 1;
        int oz = origin.getBlockZ();

        List<String> roomEntries = new ArrayList<>();
        for (int i = 0; i < ROOM_COUNT; i++) {
            int bx = ox + i * SPACING;
            buildRoom(world, bx, floorY, oz);
            roomEntries.add(String.format(Locale.US, "%s,%.1f,%.1f,%.1f,180",
                    world.getName(), bx + 2.5, floorY + 1.0, oz + 2.5));
        }

        int cz = oz + SPACING;
        buildCorridor(world, ox, floorY, cz);
        String corridor = String.format(Locale.US, "%s,%.1f,%.1f,%.1f,0",
                world.getName(), ox + 0.5, floorY + 1.0, cz + 0.5);

        plugin.getConfig().set("rooms", roomEntries);
        plugin.getConfig().set("corridor", corridor);
        plugin.saveConfig();
    }

    private void buildRoom(World w, int bx, int floorY, int bz) {
        for (int x = bx - 1; x <= bx + ROOM_W; x++) {
            for (int z = bz - 1; z <= bz + ROOM_D; z++) {
                boolean wall = x == bx - 1 || x == bx + ROOM_W || z == bz - 1 || z == bz + ROOM_D;
                set(w, x, floorY, z, Material.GRAY_CONCRETE);
                set(w, x, floorY + ROOM_H + 1, z, Material.LIGHT_GRAY_CONCRETE);
                for (int y = floorY + 1; y <= floorY + ROOM_H; y++) {
                    set(w, x, y, z, wall ? Material.GRAY_CONCRETE : Material.AIR);
                }
            }
        }
    }

    private void buildCorridor(World w, int cx, int floorY, int cz) {
        int reach = 14;
        for (int x = cx - reach; x <= cx + reach; x++) {
            for (int z = cz - reach; z <= cz + reach; z++) {
                boolean center = Math.abs(x - cx) <= 4 && Math.abs(z - cz) <= 4;
                boolean armX = Math.abs(z - cz) <= 1 && Math.abs(x - cx) <= reach;
                boolean armZ = Math.abs(x - cx) <= 1 && Math.abs(z - cz) <= reach;
                boolean floor = center || armX || armZ;

                set(w, x, floorY, z, floor ? Material.LIGHT_GRAY_CONCRETE : Material.AIR);
                for (int y = floorY + 1; y <= floorY + 4; y++) {
                    set(w, x, y, z, Material.AIR);
                }
            }
        }
    }

    private static void set(World w, int x, int y, int z, Material material) {
        w.getBlockAt(x, y, z).setType(material, false);
    }
}
