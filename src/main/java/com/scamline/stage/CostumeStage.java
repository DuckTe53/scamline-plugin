package com.scamline.stage;

import com.scamline.game.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CostumeStage implements Stage {

    private final GameManager game;
    private UUID clown;
    private final Set<UUID> guessed = new HashSet<>();

    public CostumeStage(GameManager game) {
        this.game = game;
    }

    @Override
    public String name() {
        return "Kostüm Partisi";
    }

    @Override
    public void start() {
        guessed.clear();
        List<Player> list = new ArrayList<>(game.activePlayers());
        if (list.isEmpty()) return;
        Collections.shuffle(list);
        clown = list.get(0).getUniqueId();

        for (Player p : list) {
            boolean isClown = p.getUniqueId().equals(clown);
            game.skins().apply(p, isClown ? game.skins().clownSkin() : game.skins().frogSkin());
            p.showTitle(Title.title(
                    isClown ? Component.text("Palyaço", NamedTextColor.RED)
                            : Component.text("Kurbağa", NamedTextColor.GREEN),
                    Component.text("Rolünü kimseye söyleme", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(5), Duration.ofMillis(800))));
        }
    }

    /** /scamline palyacotahmin <oyuncu> */
    public void guess(Player guesser, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (!game.isParticipant(guesser.getUniqueId())
                || target == null
                || !game.isParticipant(target.getUniqueId())
                || target.equals(guesser)) {
            guesser.sendMessage(Component.text("Geçersiz hedef.", NamedTextColor.RED));
            return;
        }
        if (!guessed.add(guesser.getUniqueId())) {
            guesser.sendMessage(Component.text("Bu etapta zaten tahmin ettin.", NamedTextColor.RED));
            return;
        }
        if (target.getUniqueId().equals(clown)) {
            game.scores().addStage(guesser.getUniqueId(), 1);
            guesser.sendMessage(Component.text("Doğru tahmin! +1 puan", NamedTextColor.GREEN));
        } else {
            guesser.sendMessage(Component.text("Yanlış tahmin.", NamedTextColor.RED));
        }
    }

    @Override
    public void end() {
        for (Player p : game.activePlayers()) {
            game.skins().apply(p, game.skins().defaultSkin());
        }
        clown = null;
        guessed.clear();
    }

    @Override
    public void onPlayerLeave(UUID id) {
        guessed.remove(id);
    }
}
