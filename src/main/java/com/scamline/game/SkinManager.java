package com.scamline.game;

import com.scamline.ScamLinePlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class SkinManager {

    private final ScamLinePlugin plugin;

    public SkinManager(ScamLinePlugin plugin) {
        this.plugin = plugin;
    }

    public void apply(Player player, String skin) {
        if (skin == null || skin.isBlank()) return;
        String template = plugin.getConfig().getString("skin-command", "skin set {skin} {player}");
        String command = template
                .replace("{skin}", skin)
                .replace("{player}", player.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    public String defaultSkin() {
        return plugin.getConfig().getString("skins.default", "ItzSilentEchoz");
    }

    public String clownSkin() {
        return plugin.getConfig().getString("skins.clown", "im_a_lauver0");
    }

    public String frogSkin() {
        return plugin.getConfig().getString("skins.frog", "MaoZedong67");
    }
}
