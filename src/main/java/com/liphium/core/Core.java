package com.liphium.core;

import com.liphium.core.inventory.Screens;
import com.liphium.core.listener.InventoryListener;
import com.liphium.snowsplash.Snowsplash;
import org.bukkit.event.Listener;

import java.util.Arrays;

public class Core {

    private static Core instance;

    private Screens screens;

    public static void init() {
        instance = new Core();
        instance.screens = new Screens();

        System.out.println(" ");
        System.out.println("Starting ServerCore..");

        System.out.println("Initializing listeners..");
        Listener[] listeners = new Listener[]{new InventoryListener()};
        Arrays.stream(listeners).forEach(listener -> Snowsplash.getInstance().getServer().getPluginManager().registerEvents(listener, Snowsplash.getInstance()));

        System.out.println(" ");
    }

    public Screens getScreens() {
        return screens;
    }

    public static Core getInstance() {
        return instance;
    }
}
