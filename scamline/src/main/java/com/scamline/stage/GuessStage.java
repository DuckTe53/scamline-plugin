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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Sayı ve renk tahmin etaplarının ortak sınıfı. */
public final class GuessStage implements Stage {

    public enum Kind { NUMBER, COLOR }

    private static final List<String> COLORS = List.of(
            "Kırmızı", "Mavi", "Yeşil", "Sarı", "Mor", "Turuncu", "Beyaz", "Siyah");

    private final GameManager game;
    private final Kind kind;
    private final Map<UUID, String> secret = new HashMap<>();
    private final Set<String> done = new HashSet<>(); // "tahmin eden:hedef"

    public GuessStage(GameManager game, Kind kind) {
        this.game = game;
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public String name() {
        return kind == Kind.NUMBER ? "Sayı Tahmin" : "Renk Tahmin";
    }

    @Override
    public void start() {
        secret.clear();
        done.clear();

        List<String> values = new ArrayList<>();
        if (kind == Kind.NUMBER) {
            for (int i = 1; i <= 8; i++) values.add(String.valueOf(i));
        } else {
            values.addAll(COLORS);
        }
        Collections.shuffle(values);

        List<Player> list = new ArrayList<>(game.activePlayers());
        for (int i = 0; i < list.size(); i++) {
            Player p = list.get(i);
            String value = values.get(i);
            secret.put(p.getUniqueId(), value);
            p.showTitle(Title.title(
                    Component.text(value, NamedTextColor.AQUA),
                    Component.text(kind == Kind.NUMBER ? "Senin sayın, ipucu ver" : "Senin rengin, ipucu ver",
                            NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(6), Duration.ofMillis(800))));
        }
    }

    /** /scamline sayitahmin veya renktahmin komutundan çağrılır. */
    public void guess(Player guesser, String value, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (!game.isParticipant(guesser.getUniqueId())
                || target == null
                || !secret.containsKey(target.getUniqueId())
                || target.equals(guesser)) {
            guesser.sendMessage(Component.text("Geçersiz hedef.", NamedTextColor.RED));
            return;
        }
        String key = guesser.getUniqueId() + ":" + target.getUniqueId();
        if (!done.add(key)) {
            guesser.sendMessage(Component.text("Bu oyuncu için zaten tahmin yaptın.", NamedTextColor.RED));
            return;
        }
        String correct = secret.get(target.getUniqueId());
        if (normalize(value).equals(normalize(correct))) {
            game.scores().addStage(guesser.getUniqueId(), 0.25);
            guesser.sendMessage(Component.text("Doğru tahmin! +0.25 puan", NamedTextColor.GREEN));
        } else {
            guesser.sendMessage(Component.text("Yanlış tahmin.", NamedTextColor.RED));
        }
    }

    // "Sarı" ile "sari" gibi girişlerin eşleşmesi için
    public static String normalize(String s) {
        return s.toLowerCase(Locale.forLanguageTag("tr"))
                .replace('ı', 'i')
                .replace('ç', 'c')
                .replace('ğ', 'g')
                .replace('ö', 'o')
                .replace('ş', 's')
                .replace('ü', 'u')
                .trim();
    }

    @Override
    public void end() {
        secret.clear();
        done.clear();
    }

    @Override
    public void onPlayerLeave(UUID id) {
        secret.remove(id);
    }
}
