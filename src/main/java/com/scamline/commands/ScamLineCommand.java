package com.scamline.commands;

import com.scamline.game.GameManager;
import com.scamline.stage.CostumeStage;
import com.scamline.stage.GuessStage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class ScamLineCommand implements CommandExecutor {

    private final GameManager game;

    public ScamLineCommand(GameManager game) {
        this.game = game;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            send(sender, "Kullanım: /scamline <baslat|etapbitir|yenietap|durdur|palyacotahmin|sayitahmin|renktahmin>");
            return true;
        }

        Player player = sender instanceof Player p ? p : null;

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "baslat" -> game.start(sender);
            case "etapbitir" -> game.endStage();
            case "yenietap" -> game.nextStage();
            case "durdur" -> game.stop();

            case "palyacotahmin" -> {
                if (player == null || args.length < 2) {
                    send(sender, "Kullanım: /scamline palyacotahmin <oyuncu>");
                } else if (game.current() instanceof CostumeStage costume) {
                    costume.guess(player, args[1]);
                } else {
                    send(sender, "Şu an kostüm etabı değil.");
                }
            }

            case "sayitahmin" -> {
                if (player == null || args.length < 3) {
                    send(sender, "Kullanım: /scamline sayitahmin <sayı> <oyuncu>");
                } else if (game.current() instanceof GuessStage guess && guess.kind() == GuessStage.Kind.NUMBER) {
                    guess.guess(player, args[1], args[2]);
                } else {
                    send(sender, "Şu an sayı tahmin etabı değil.");
                }
            }

            case "renktahmin" -> {
                if (player == null || args.length < 3) {
                    send(sender, "Kullanım: /scamline renktahmin <renk> <oyuncu>");
                } else if (game.current() instanceof GuessStage guess && guess.kind() == GuessStage.Kind.COLOR) {
                    guess.guess(player, args[1], args[2]);
                } else {
                    send(sender, "Şu an renk tahmin etabı değil.");
                }
            }

            default -> send(sender, "Bilinmeyen alt komut.");
        }
        return true;
    }

    private static void send(CommandSender sender, String text) {
        sender.sendMessage(Component.text(text, NamedTextColor.RED));
    }
}
