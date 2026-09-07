package com.liphium.kingdom.util;

import com.liphium.kingdom.Kingdom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.HashMap;

public class LocationAPI {

    private static final HashMap<String, Location> locations = new HashMap<>();

    public static void setLocation(String locationName, Location loc) {
        Kingdom.getInstance().getConfig().set(locationName, loc);
        Kingdom.getInstance().saveConfig();
        locations.put(locationName, loc);
    }

    public static boolean exists(String locationName) {
        return Kingdom.getInstance().getConfig().isSet(locationName);
    }

    /**
     * Same as getLocation, but returns null silently when the location doesn't exist.
     */
    public static Location safe(String locationName) {
        if (!exists(locationName)) {
            return null;
        }

        Location location = Kingdom.getInstance().getConfig().getLocation(locationName);
        if (location != null) location.setWorld(Bukkit.getWorld(Kingdom.GAME_WORLD));
        return location;
    }

    /**
     * Get a location by name in the elfhunt world
     *
     * @param locationName Name of the location in the config file
     * @return The location in the specified world
     */
    public static Location getLocation(String locationName) {

        // Send a warning when the location doesn't exist yet
        if (!exists(locationName)) {
            Bukkit.broadcast(Component.text("Location " + locationName + " not found!").color(NamedTextColor.RED));
            return null;
        }

        // Check if the location has already been cached
        if (locations.containsKey(locationName)) {
            return locations.get(locationName);
        }

        // Make the location
        Location location = Kingdom.getInstance().getConfig().getLocation(locationName);

        if (location != null) location.setWorld(Bukkit.getWorld(Kingdom.GAME_WORLD));

        // Cache the location for use in the future
        locations.put(locationName, location);
        return location;
    }

}
