package com.scamline.stage;

import com.scamline.game.GameManager;
import com.scamline.game.ScoreManager;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Ok ruleti. Oyuncuların canı yarım kalp. Yay bir oyuncuda dolaşır.
 * Her ok atışında şans artar (%12, %25, %33, %50, %100). Öldürme olunca şans %12'ye döner.
 * Ok isabet edip ölmezse yay isabet eden oyuncuya geçer.
 * Yayı yere atan kişi kendine ok atmış olur. Ölmezse yay kendisinde kalır.
 * Son kalan oyuncu +1 puan alır.
 */
public final class ArrowRouletteStage implements Stage {

    private static final double[] CHANCES = {0.12, 0.25, 0.33, 0.50, 1.00};

    private final GameManager game;
    private final Set<UUID> alive = new LinkedHashSet<>();
    private UUID holder;
    private int shots;
    private BossBar bar;
    private boolean finished;

    public ArrowRouletteStage(GameManager game) {
        this.game = game;
    }

    @Override
    public String name() {
        return "Ok Ruleti";
    }

    @Override
    public void start() {
        finished = false;
        shots = 0;
        holder = null;
        alive.clear();

        Location arena = game.rooms().corridor();
        arena.getWorld().setPVP(true);

        for (Player p : game.activePlayers()) {
            alive.add(p.getUniqueId());
            p.teleport(arena);
            p.setGameMode(GameMode.ADVENTURE);
            p.setHealth(1.0); // yarım kalp
        }

        bar = BossBar.bossBar(Component.empty(), 0.12f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
        for (Player p : game.activePlayers()) p.showBossBar(bar);
        refreshBar();

        giveBowTo(randomAlive());
    }

    /** Sağ tıklamayla yapılan atış değil, yay isabeti. Olay dinleyicisi çağırır. */
    public void onArrowHit(Player shooter, Player victim) {
        if (finished) return;
        if (!shooter.getUniqueId().equals(holder)) return;
        if (!alive.contains(victim.getUniqueId()) || victim.equals(shooter)) return;

        double chance = currentChance();
        shots++;
        if (Math.random() < chance) {
            victim.setHealth(0); // PlayerDeathEvent -> onDeath
        } else {
            giveBowTo(victim.getUniqueId());
        }
        refreshBar();
    }

    /** Yay yere atılınca çağrılır. Atılan okun kendisine isabet ettiği anlamına gelir. */
    public void onSelfShot(Player player) {
        if (finished) return;
        if (!player.getUniqueId().equals(holder)) return;

        double chance = currentChance();
        shots++;
        if (Math.random() < chance) {
            player.setHealth(0);
        }
        // Ölmediyse yay zaten iptal edilen drop ile kendisinde kalır.
        refreshBar();
    }

    /** Oyuncu öldüğünde veya oyundan çıktığında çağrılır. */
    public void onDeath(UUID id) {
        if (finished || !alive.remove(id)) return;

        shots = 0;
        if (id.equals(holder)) {
            holder = null;
            giveBowTo(randomAlive());
        }
        game.announce(Component.text(ScoreManager.nameOf(id) + " elendi!", NamedTextColor.RED));
        refreshBar();
        checkWinner();
    }

    @Override
    public void onPlayerLeave(UUID id) {
        onDeath(id);
    }

    @Override
    public void end() {
        finished = true;
        Location arena = game.rooms().corridor();
        arena.getWorld().setPVP(false);

        for (Player p : game.activePlayers()) {
            if (bar != null) p.hideBossBar(bar);
            p.getInventory().remove(Material.BOW);
            p.getInventory().remove(Material.ARROW);
            p.setGameMode(GameMode.ADVENTURE);
            p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getValue());
        }

        alive.clear();
        holder = null;
        shots = 0;
        bar = null;
    }

    private void checkWinner() {
        if (alive.size() != 1) return;
        UUID winner = alive.iterator().next();
        finished = true;
        game.scores().addStage(winner, 1);
        game.announce(Component.text(ScoreManager.nameOf(winner) + " son kalan oldu! +1 puan", NamedTextColor.GOLD));
    }

    private double currentChance() {
        return CHANCES[Math.min(shots, CHANCES.length - 1)];
    }

    private void refreshBar() {
        if (bar == null) return;
        double chance = currentChance();
        bar.name(Component.text("Şans %" + (int) Math.round(chance * 100), NamedTextColor.YELLOW));
        bar.progress((float) chance);
    }

    private UUID randomAlive() {
        List<UUID> list = new ArrayList<>(alive);
        if (list.isEmpty()) return null;
        Collections.shuffle(list);
        return list.get(0);
    }

    private void giveBowTo(UUID id) {
        if (id == null) return;

        for (UUID other : alive) {
            Player p = Bukkit.getPlayer(other);
            if (p != null) p.getInventory().remove(Material.BOW);
        }

        Player p = Bukkit.getPlayer(id);
        if (p == null) return;

        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();
        meta.addEnchant(Enchantment.INFINITY, 1, true); // ok tükenmesin
        bow.setItemMeta(meta);

        p.getInventory().addItem(bow);
        p.getInventory().addItem(new ItemStack(Material.ARROW, 1));
        holder = id;
        p.sendMessage(Component.text("Yay sende! Yayı yere atarsan kendine ateş etmiş olursun.",
                NamedTextColor.YELLOW));
    }
}
