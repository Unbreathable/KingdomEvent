package com.liphium.snowsplash.listener;

import com.liphium.snowsplash.Snowsplash;
import com.liphium.snowsplash.game.team.Team;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Team team = Snowsplash.getInstance().getGameManager().getTeamManager().getTeam(event.getPlayer());
        event.setCancelled(true);
        if (team == null) {
            Bukkit.broadcast(Component.text(event.getPlayer().getName(), NamedTextColor.GRAY, TextDecoration.ITALIC).append(Component.text(": ", NamedTextColor.DARK_GRAY)).append(event.message().color(NamedTextColor.GRAY)));
        } else {
            Bukkit.broadcast(Component.text(event.getPlayer().getName(), team.getColor()).append(Component.text(": ", NamedTextColor.DARK_GRAY)).append(event.message().color(NamedTextColor.GRAY)));
        }
    }

}
