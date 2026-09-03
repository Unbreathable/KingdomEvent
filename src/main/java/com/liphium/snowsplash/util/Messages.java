package com.liphium.snowsplash.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Messages {

    public static void actionBar(Component bar) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(bar);
        }
    }

}
