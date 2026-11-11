package com.scamline.listeners;

import com.scamline.game.GameManager;
import com.scamline.stage.ArrowRouletteStage;
import com.scamline.stage.ShareStealStage;
import com.scamline.stage.VoteStage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class GameListener implements Listener {

    private final GameManager game;

    public GameListener(GameManager game) {
        this.game = game;
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!game.isRunning()) return;

        Entity clicked = event.getRightClicked();

        if (clicked instanceof ItemFrame frame) {
            if (!(game.current() instanceof ShareStealStage stage)) return;
            if (frame.getScoreboardTags().contains("scamline_share")) {
                event.setCancelled(true);
                stage.choose(event.getPlayer(), true);
            } else if (frame.getScoreboardTags().contains("scamline_steal")) {
                event.setCancelled(true);
                stage.choose(event.getPlayer(), false);
            }
            return;
        }

        if (clicked instanceof Player target && game.current() instanceof VoteStage vote) {
            event.setCancelled(true);
            vote.vote(event.getPlayer(), target);
        }
    }

    @EventHandler
    public void onDropBow(PlayerDropItemEvent event) {
        if (!game.isRunning()) return;
        if (!(game.current() instanceof ArrowRouletteStage roulette)) return;
        if (event.getItemDrop().getItemStack().getType() != Material.BOW) return;

        event.setCancelled(true);
        roulette.onSelfShot(event.getPlayer());
    }

    @EventHandler
    public void onArrowDamage(EntityDamageByEntityEvent event) {
        if (!game.isRunning()) return;
        if (!(game.current() instanceof ArrowRouletteStage roulette)) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;

        // Hasarı biz hesaplıyoruz, Minecraft'ın varsayılan hasarını iptal et
        event.setCancelled(true);
        roulette.onArrowHit(shooter, victim);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!game.isRunning()) return;
        if (!(game.current() instanceof ArrowRouletteStage roulette)) return;

        roulette.onDeath(event.getEntity().getUniqueId());
        event.getDrops().removeIf(item ->
                item.getType() == Material.BOW || item.getType() == Material.ARROW);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!game.isRunning()) return;
        if (!(game.current() instanceof ArrowRouletteStage)) return;

        Player player = event.getPlayer();
        if (!game.isParticipant(player.getUniqueId())) return;

        event.setRespawnLocation(game.rooms().corridor());
        Bukkit.getScheduler().runTask(game.plugin(), () -> player.setGameMode(GameMode.SPECTATOR));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        game.onPlayerLeave(event.getPlayer().getUniqueId());
    }
}
