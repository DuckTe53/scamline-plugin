package com.scamline;

import com.scamline.commands.ScamLineCommand;
import com.scamline.game.GameManager;
import com.scamline.game.RoomManager;
import com.scamline.game.ScoreManager;
import com.scamline.game.SkinManager;
import com.scamline.listeners.GameListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class ScamLinePlugin extends JavaPlugin {

    private GameManager game;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        RoomManager rooms = new RoomManager(this);
        ScoreManager scores = new ScoreManager();
        SkinManager skins = new SkinManager(this);
        game = new GameManager(this, rooms, scores, skins);

        getServer().getPluginManager().registerEvents(new GameListener(game), this);
        getCommand("scamline").setExecutor(new ScamLineCommand(game));
        getLogger().info("ScamLine aktif.");
    }

    @Override
    public void onDisable() {
        if (game != null) game.stop();
    }
}
