package com.scamline.game;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ScoreManager {

    private final Map<UUID, Double> stagePoints = new HashMap<>();
    private final Map<UUID, Double> totalPoints = new HashMap<>();

    public void reset() {
        stagePoints.clear();
        totalPoints.clear();
    }

    public void clearStage() {
        stagePoints.clear();
    }

    public void addStage(UUID id, double amount) {
        stagePoints.merge(id, amount, Double::sum);
    }

    public double stage(UUID id) {
        return stagePoints.getOrDefault(id, 0.0);
    }

    public double total(UUID id) {
        return totalPoints.getOrDefault(id, 0.0);
    }

    /**
     * Etap puanlarını toplama ekler. En yüksek etap puanına sahip oyuncular
     * (eşitlik dahil) toplam puana +1 alır. Kazananların listesini döner.
     */
    public List<UUID> settle(Collection<UUID> players) {
        double max = 0;
        for (UUID id : players) max = Math.max(max, stage(id));

        List<UUID> winners = new ArrayList<>();
        if (max > 0) {
            for (UUID id : players) {
                if (stage(id) == max) winners.add(id);
            }
        }
        for (UUID id : players) totalPoints.merge(id, stage(id), Double::sum);
        for (UUID id : winners) totalPoints.merge(id, 1.0, Double::sum);

        stagePoints.clear();
        return winners;
    }

    public void render(Collection<UUID> players, String stageName, boolean stageView) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective(
                "scamline", Criteria.DUMMY, Component.text("SCAM LINE", NamedTextColor.GOLD));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank());

        List<String> lines = new ArrayList<>();
        if (stageView) {
            lines.add("§eEtap: §f" + stageName);
            for (UUID id : players) lines.add("§f" + nameOf(id) + " §7" + fmt(stage(id)));
        } else {
            lines.add("§6Toplam Puan");
            for (UUID id : players) lines.add("§f" + nameOf(id) + " §7" + fmt(total(id)));
        }

        for (int i = 0; i < lines.size(); i++) {
            String entry = entry(i);
            Team team = board.registerNewTeam("line" + i);
            team.addEntry(entry);
            team.prefix(LegacyComponentSerializer.legacySection().deserialize(lines.get(i)));
            objective.getScore(entry).setScore(lines.size() - i);
        }

        for (Player p : Bukkit.getOnlinePlayers()) p.setScoreboard(board);
    }

    // Her satır için benzersiz, görünmez bir giriş adı üretir
    private static String entry(int i) {
        ChatColor[] codes = ChatColor.values();
        return codes[i % codes.length].toString().repeat(1 + i / codes.length);
    }

    public static String nameOf(UUID id) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(id);
        return op.getName() != null ? op.getName() : "?";
    }

    public static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.US, "%.2f", v);
    }
}
