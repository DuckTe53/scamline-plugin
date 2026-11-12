package com.scamline.game;

import com.scamline.ScamLinePlugin;
import com.scamline.stage.ArrowRouletteStage;
import com.scamline.stage.CostumeStage;
import com.scamline.stage.GuessStage;
import com.scamline.stage.ShareStealStage;
import com.scamline.stage.Stage;
import com.scamline.stage.VoteStage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class GameManager {

    private final ScamLinePlugin plugin;
    private final RoomManager rooms;
    private final ScoreManager scores;
    private final SkinManager skins;

    private final RoomBuilder builder;
    private final Set<UUID> participants = new LinkedHashSet<>();
    private final List<Stage> pool = new ArrayList<>();
    private final Deque<Stage> queue = new ArrayDeque<>();
    private Stage current;
    private boolean running;
    private boolean stageView = true;
    private BukkitTask scoreboardTask;

    public GameManager(ScamLinePlugin plugin, RoomManager rooms, ScoreManager scores, SkinManager skins) {
        this.plugin = plugin;
        this.rooms = rooms;
        this.scores = scores;
        this.skins = skins;
        this.builder = new RoomBuilder(plugin);

        pool.add(new ShareStealStage(this));
        pool.add(new CostumeStage(this));
        pool.add(new GuessStage(this, GuessStage.Kind.NUMBER));
        pool.add(new GuessStage(this, GuessStage.Kind.COLOR));
        pool.add(new VoteStage(this, false));
        pool.add(new VoteStage(this, true));
        pool.add(new ArrowRouletteStage(this));
    }

    // ---- erişim metotları ----

    public ScamLinePlugin plugin() { return plugin; }
    public RoomManager rooms() { return rooms; }
    public ScoreManager scores() { return scores; }
    public SkinManager skins() { return skins; }
    public boolean isRunning() { return running; }
    public Stage current() { return current; }

    public boolean isParticipant(UUID id) {
        return participants.contains(id);
    }

    public List<Player> activePlayers() {
        List<Player> list = new ArrayList<>();
        for (UUID id : participants) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) list.add(p);
        }
        return list;
    }

    public void announce(Component message) {
        for (Player p : activePlayers()) p.sendMessage(message);
    }

    // ---- oyun akışı ----

    public void start(CommandSender sender) {
        if (running) {
            sender.sendMessage(Component.text("Oyun zaten çalışıyor.", NamedTextColor.RED));
            return;
        }
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        int minPlayers = plugin.getConfig().getInt("min-players", 1);
        if (online.size() < minPlayers || online.size() > 8) {
            sender.sendMessage(Component.text(
                    "Oyun " + minPlayers + "-8 oyuncu ile başlar.", NamedTextColor.RED));
            return;
        }
        // Odaları her başlatmada, komutu veren oyuncunun olduğu yerde kur
        Location base = sender instanceof Player p
                ? p.getLocation()
                : Bukkit.getWorlds().get(0).getSpawnLocation();
        sender.sendMessage(Component.text("Odalar kuruluyor, lütfen bekle...", NamedTextColor.GRAY));
        builder.build(base);
        rooms.reload();

        if (!rooms.isReady(online.size())) {
            sender.sendMessage(Component.text("Odalar kurulamadı, config.yml ve sunucu konsolunu kontrol et.", NamedTextColor.RED));
            return;
        }

        running = true;
        participants.clear();
        for (Player p : online) participants.add(p.getUniqueId());
        scores.reset();

        queue.clear();
        List<Stage> order = new ArrayList<>(pool);
        Collections.shuffle(order);
        queue.addAll(order);

        for (Player p : online) {
            p.setGameMode(GameMode.ADVENTURE);
            skins.apply(p, skins.defaultSkin());
        }
        rooms.assign(online);

        announce(Component.text("Scam Line başladı! Etaplar rastgele sırada.", NamedTextColor.GOLD));
        startScoreboard();
        nextStage();
    }

    public void nextStage() {
        if (!running) {
            sendAll("Oyun başlamadı.");
            return;
        }
        if (current != null) {
            sendAll("Önce /scamline etapbitir kullan.");
            return;
        }
        Stage next = queue.poll();
        if (next == null) {
            finish();
            return;
        }

        current = next;
        scores.clearStage();
        Title title = Title.title(
                Component.text(next.name(), NamedTextColor.GOLD),
                Component.empty(),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(500)));
        for (Player p : activePlayers()) p.showTitle(title);

        // Oda etapları için oyuncular önce odalarına döner (koridor etapları kendi ışınlanır)
        if (!(next instanceof VoteStage) && !(next instanceof ArrowRouletteStage)) {
            for (Player p : activePlayers()) rooms.sendToRoom(p);
        }

        next.start();
        renderBoard();
    }

    public void endStage() {
        if (current == null) {
            sendAll("Aktif bir etap yok.");
            return;
        }
        Stage ended = current;
        ended.end();
        current = null;

        // Her etap bitince herkes koridora gider
        Location corridor = rooms.corridor();
        for (Player p : activePlayers()) p.teleport(corridor);

        List<UUID> winners = scores.settle(participants);
        announce(Component.text("Etap bitti: " + ended.name(), NamedTextColor.GOLD));
        if (winners.isEmpty()) {
            announce(Component.text("Bu etapta puan kazanan olmadı.", NamedTextColor.GRAY));
        } else {
            announce(Component.text("Etap kazananı: " + joinNames(winners) + " (+1 puan)", NamedTextColor.GREEN));
        }
        renderBoard();

        if (queue.isEmpty()) {
            finish();
        } else {
            announce(Component.text("Sıradaki etap için /scamline yenietap", NamedTextColor.GRAY));
        }
    }

    /** /scamline kurulum: oyuncunun olduğu yerden odaları ve koridoru kurar. */
    public void buildRooms(Player player) {
        if (running) {
            player.sendMessage(Component.text("Oyun çalışırken kurulum yapılamaz.", NamedTextColor.RED));
            return;
        }
        player.sendMessage(Component.text("Odalar kuruluyor, biraz sürebilir...", NamedTextColor.GRAY));
        builder.build(player.getLocation());
        rooms.reload();
        player.sendMessage(Component.text("Kurulum tamam: 8 oda ve koridor 250 blok arayla kuruldu.", NamedTextColor.GREEN));
    }

    public void stop() {
        if (!running) return;
        if (current != null) {
            current.end();
            current = null;
        }
        announce(Component.text("Oyun durduruldu.", NamedTextColor.RED));
        teardown();
    }

    public void onPlayerLeave(UUID id) {
        if (!participants.contains(id)) return;
        participants.remove(id);
        if (current != null) current.onPlayerLeave(id);
    }

    // ---- tablo ----

    private void startScoreboard() {
        long period = plugin.getConfig().getLong("scoreboard-rotate-seconds", 25) * 20L;
        if (scoreboardTask != null) scoreboardTask.cancel();
        renderBoard();
        scoreboardTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            stageView = !stageView;
            renderBoard();
        }, period, period);
    }

    private void renderBoard() {
        scores.render(participants, current == null ? "-" : current.name(), stageView);
    }

    // ---- bitiş ----

    private void finish() {
        if (!participants.isEmpty()) {
            double best = participants.stream().mapToDouble(scores::total).max().orElse(0);
            List<UUID> winners = participants.stream()
                    .filter(id -> scores.total(id) == best)
                    .toList();
            announce(Component.text("OYUN BİTTİ!", NamedTextColor.GOLD));
            List<UUID> ranked = new ArrayList<>(participants);
            ranked.sort(Comparator.comparingDouble(scores::total).reversed());
            for (UUID id : ranked) {
                announce(Component.text(ScoreManager.nameOf(id) + ": " + ScoreManager.fmt(scores.total(id)) + " puan"));
            }
            announce(Component.text("Kazanan: " + joinNames(winners), NamedTextColor.GOLD));
        }
        teardown();
    }

    private void teardown() {
        running = false;
        if (scoreboardTask != null) scoreboardTask.cancel();
        scoreboardTask = null;

        rooms.clearChoiceFrames();
        rooms.forget();

        World spawnWorld = Bukkit.getWorlds().get(0);
        for (Player p : activePlayers()) {
            p.setGameMode(GameMode.SURVIVAL);
            skins.apply(p, skins.defaultSkin());
            p.teleport(spawnWorld.getSpawnLocation());
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        queue.clear();
        current = null;
        participants.clear();
    }

    // ---- yardımcılar ----

    private void sendAll(String text) {
        announce(Component.text(text, NamedTextColor.RED));
    }

    private String joinNames(List<UUID> ids) {
        List<String> names = new ArrayList<>();
        for (UUID id : ids) names.add(ScoreManager.nameOf(id));
        return String.join(", ", names);
    }
}
