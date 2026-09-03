package com.liphium.snowsplash.listener.machines;

import com.liphium.snowsplash.listener.machines.impl.*;
import com.liphium.snowsplash.util.LocationAPI;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class MachineManager {

    private final ArrayList<Machine> machines = new ArrayList<>();

    public MachineManager() {

        ArrayList<String> registered = new ArrayList<>();

        // Add all machines (all the ones that can be spawned by location)
        registered.add("IceDropper");
        registered.add("ItemShop");

        for (String s : registered) {
            for (int i = 1; i <= 1000; i++) {
                if (LocationAPI.exists(s + i)) {
                    machines.add(newMachineByLocation(s, LocationAPI.getLocation(s + i)));
                } else break;
            }
        }

    }

    public <T> ArrayList<T> getMachines(Class<T> clazz) {
        final var toReturn = new ArrayList<T>();
        for (Machine machine : machines) {
            if (machine.getClass().getSimpleName().equals(clazz.getSimpleName())) {
                toReturn.add((T) machine);
            }
        }

        return toReturn;
    }

    public Machine getMachine(Location location) {
        for (Machine machine : machines) {
            if (machine.getLocation().distance(location) <= 0.1) {
                return machine;
            }
        }

        return null;
    }

    public void onInteract(PlayerInteractEvent event) {
        for (Machine machine : machines) {
            machine.onInteract(event);
        }
    }

    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        for (Machine machine : machines) {
            machine.onInteractAtEntity(event);
        }
    }

    public ArrayList<Machine> getMachines() {
        return machines;
    }

    public Machine newMachineByLocation(String name, Location location) {
        return switch (name) {
            case "IceDropper" -> new ItemDropper(location, "Ice", NamedTextColor.AQUA, new ItemStack(Material.BLUE_ICE), () -> Math.max(14 - Bukkit.getOnlinePlayers().size(), 4));
            case "ItemShop" -> new ItemShop(location);
            default -> null;
        };
    }

    public boolean breakLocation(Location location) {
        Machine toRemove = null;
        for (Machine machine : machines) {
            if (machine.isBreakable() && machine.getLocation().getBlock().getLocation().equals(location)) {
                machine.destroy();
                toRemove = machine;
            }
        }

        if (toRemove != null) {
            machines.remove(toRemove);
            return true;
        }

        return false;
    }

    public void addMachine(Machine machine) {
        machines.add(machine);
    }

    public void tick() {
        for (Machine machine : machines) {
            machine.tick();
        }
    }

}
