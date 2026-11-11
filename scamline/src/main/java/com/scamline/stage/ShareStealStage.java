package com.scamline.stage;

import com.scamline.game.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ShareStealStage implements Stage {

    private final GameManager game;
    private final Map<UUID, UUID> partners = new HashMap<>();
    private final Map<UUID, Boolean> pending = new HashMap<>(); // true = paylaş, false = çal
    private BukkitTask reshuffleTask;

    public ShareStealStage(GameManager game) {
        this.game = game;
    }

    @Override
    public String name() {
        return "Paylaş ya da Çal";
    }

    @Override
    public void start() {
        for (Player p : game.activePlayers()) game.rooms().spawnChoiceFrames(p);
        pair();
        long minutes = game.plugin().getConfig().getLong("share-reshuffle-minutes", 3);
        long period = minutes * 60L * 20L;
        reshuffleTask = Bukkit.getScheduler().runTaskTimer(game.plugin(), this::pair, period, period);
    }

    private void pair() {
        partners.clear();
        pending.clear();

        List<Player> list = new ArrayList<>(game.activePlayers());
        Collections.shuffle(list);
        for (int i = 0; i + 1 < list.size(); i += 2) {
            Player a = list.get(i);
            Player b = list.get(i + 1);
            partners.put(a.getUniqueId(), b.getUniqueId());
            partners.put(b.getUniqueId(), a.getUniqueId());
            showPartner(a, b);
            showPartner(b, a);
        }
        if (list.size() % 2 == 1) {
            list.get(list.size() - 1).sendMessage(
                    Component.text("Bu turda eşleşemedin, sıradaki eşleşmeyi bekle.", NamedTextColor.GRAY));
        }
    }

    private void showPartner(Player viewer, Player partner) {
        viewer.showTitle(Title.title(
                Component.text(partner.getName(), NamedTextColor.YELLOW),
                Component.text("partnerin", NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(4), Duration.ofMillis(500))));
    }

    /** Çerçeveye tıklayınca çağrılır. share = true ise yeşil (paylaş), false ise kırmızı (çal). */
    public void choose(Player player, boolean share) {
        UUID id = player.getUniqueId();
        if (!game.isParticipant(id)) return;

        UUID partnerId = partners.get(id);
        if (partnerId == null) {
            player.sendMessage(Component.text("Şu an bir eşleşmen yok.", NamedTextColor.RED));
            return;
        }

        pending.put(id, share);
        player.sendMessage(Component.text("Seçimin kaydedildi, partnerini bekliyorsun.", NamedTextColor.GRAY));

        if (pending.containsKey(partnerId)) resolve(id, partnerId);
    }

    private void resolve(UUID a, UUID b) {
        boolean shareA = pending.remove(a);
        boolean shareB = pending.remove(b);

        game.scores().addStage(a, gain(shareA, shareB));
        game.scores().addStage(b, gain(shareB, shareA));

        notifyResult(a, shareA, shareB);
        notifyResult(b, shareB, shareA);
    }

    // Paylaş-paylaş: 2 puan. Çal-paylaş: çalan 5 puan. Çal-çal: 0.
    private static double gain(boolean mine, boolean theirs) {
        if (mine && theirs) return 2;
        if (!mine && theirs) return 5;
        return 0;
    }

    private void notifyResult(UUID id, boolean mine, boolean theirs) {
        Player p = Bukkit.getPlayer(id);
        if (p == null) return;
        String mineText = mine ? "paylaştın" : "çaldın";
        String theirsText = theirs ? "paylaştı" : "çaldı";
        p.sendMessage(Component.text("Sonuç: " + mineText + ", partnerin " + theirsText
                + ". +" + (int) gain(mine, theirs) + " puan", NamedTextColor.AQUA));
    }

    @Override
    public void end() {
        if (reshuffleTask != null) reshuffleTask.cancel();
        reshuffleTask = null;
        game.rooms().clearChoiceFrames();
        partners.clear();
        pending.clear();
    }
}
