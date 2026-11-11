package com.scamline.stage;

import com.scamline.game.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** İyi oy (evil = false) ve kötü oy (evil = true) etapları. */
public final class VoteStage implements Stage {

    private final GameManager game;
    private final boolean evil;
    private final Map<UUID, UUID> votes = new HashMap<>();

    public VoteStage(GameManager game, boolean evil) {
        this.game = game;
        this.evil = evil;
    }

    @Override
    public String name() {
        return evil ? "Kötü Oy" : "İyi Oy";
    }

    @Override
    public void start() {
        votes.clear();
        Location corridor = game.rooms().corridor();
        for (Player p : game.activePlayers()) {
            p.teleport(corridor);
            p.sendMessage(Component.text("Oy vermek için bir oyuncuya sağ tıkla.", NamedTextColor.YELLOW));
        }
    }

    /** Oyuncuya sağ tıklanınca çağrılır. Oy değiştirilebilir. */
    public void vote(Player voter, Player target) {
        if (!game.isParticipant(voter.getUniqueId())
                || !game.isParticipant(target.getUniqueId())
                || voter.equals(target)) {
            return;
        }
        votes.put(voter.getUniqueId(), target.getUniqueId());
        voter.sendMessage(Component.text(target.getName() + " için oy verdin.", NamedTextColor.GRAY));
    }

    @Override
    public void end() {
        if (!votes.isEmpty()) {
            Map<UUID, Long> counts = votes.values().stream()
                    .collect(Collectors.groupingBy(id -> id, Collectors.counting()));
            long max = Collections.max(counts.values());
            double points = evil ? -1 : 1;
            // Eşitlik varsa en çok oy alan herkes puan alır
            counts.forEach((id, count) -> {
                if (count == max) game.scores().addStage(id, points);
            });
        }
        votes.clear();
    }

    @Override
    public void onPlayerLeave(UUID id) {
        votes.remove(id);
        votes.values().removeIf(id::equals);
    }
}
