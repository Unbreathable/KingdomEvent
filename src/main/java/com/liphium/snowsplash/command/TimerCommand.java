package com.liphium.snowsplash.command;

import com.liphium.snowsplash.Snowsplash;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TimerCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args) {
        if (cs instanceof Player player && player.hasPermission("timer")) {
            if (args.length == 0) {
                sendHelp(player);
            } else if (args[0].equalsIgnoreCase("pause") || args[0].equalsIgnoreCase("resume")) {
                boolean paused = Snowsplash.getInstance().getGameManager().getCurrentState().paused;
                Snowsplash.getInstance().getGameManager().getCurrentState().paused = !paused;

                if (paused) {
                    player.sendMessage(Snowsplash.PREFIX.append(Component.text("The ", NamedTextColor.GRAY).append(Component.text("timer ", NamedTextColor.AQUA)).append(Component.text("has been ", NamedTextColor.GRAY)).append(Component.text("resumed", NamedTextColor.AQUA)).append(Component.text("!", NamedTextColor.GRAY))));
                } else {
                    player.sendMessage(Snowsplash.PREFIX.append(Component.text("The ", NamedTextColor.GRAY).append(Component.text("timer ", NamedTextColor.AQUA)).append(Component.text("has been ", NamedTextColor.GRAY)).append(Component.text("paused", NamedTextColor.AQUA)).append(Component.text("!", NamedTextColor.GRAY))));
                }
            } else if (args[0].equalsIgnoreCase("skip")) {
                if (Snowsplash.getInstance().getGameManager().getCurrentState().count <= 5) {
                    player.sendMessage(Snowsplash.PREFIX.append(Component.text("The ", NamedTextColor.GRAY).append(Component.text("timer ", NamedTextColor.AQUA)).append(Component.text("has already been ", NamedTextColor.GRAY)).append(Component.text("skipped", NamedTextColor.AQUA)).append(Component.text("!", NamedTextColor.GRAY))));
                    return false;
                }

                player.sendMessage(Snowsplash.PREFIX.append(Component.text("You ", NamedTextColor.GRAY).append(Component.text("skipped ", NamedTextColor.AQUA)).append(Component.text("the ", NamedTextColor.GRAY)).append(Component.text("timer", NamedTextColor.AQUA)).append(Component.text("!", NamedTextColor.GRAY))));

                Snowsplash.getInstance().getGameManager().getCurrentState().count = 10;
            } else {
                sendHelp(player);
            }
        }

        return false;
    }

    public void sendHelp(Player player) {
        player.sendMessage(Component.text(" "));
        player.sendMessage(Component.text("    ", NamedTextColor.RED).append(Component.text("Timer", NamedTextColor.RED)).append(Component.text(": ", NamedTextColor.DARK_GRAY)).append(Component.text(Snowsplash.getInstance().getGameManager().getCurrentState().count, NamedTextColor.RED, TextDecoration.BOLD)));

        Component status = Snowsplash.getInstance().getGameManager().getCurrentState().paused ? Component.text("Paused ", NamedTextColor.AQUA, TextDecoration.BOLD).append(Component.text("(", NamedTextColor.DARK_GRAY)).append(Component.text("/timer resume", NamedTextColor.AQUA)).append(Component.text(")", NamedTextColor.DARK_GRAY)) : Component.text("Running ", NamedTextColor.AQUA, TextDecoration.BOLD).append(Component.text("(", NamedTextColor.DARK_GRAY)).append(Component.text("/timer pause", NamedTextColor.AQUA)).append(Component.text(")", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("    ", NamedTextColor.AQUA).append(Component.text("Status", NamedTextColor.AQUA)).append(Component.text(": ", NamedTextColor.DARK_GRAY)).append(status));
        player.sendMessage(Component.text("/timer skip ", NamedTextColor.AQUA).append(Component.text("-> ", NamedTextColor.DARK_GRAY)).append(Component.text("Adjust the timer to 10 seconds.", NamedTextColor.GRAY)));
        player.sendMessage(Component.text(" "));
    }
}
