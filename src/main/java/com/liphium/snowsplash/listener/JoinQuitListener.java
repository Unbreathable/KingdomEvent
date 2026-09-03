package com.liphium.snowsplash.listener;

import com.liphium.snowsplash.Snowsplash;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinQuitListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.joinMessage(null);
        Snowsplash.getInstance().getGameManager().join(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.quitMessage(null);
        Snowsplash.getInstance().getGameManager().quit(event.getPlayer());
    }

}
