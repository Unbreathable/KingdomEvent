package com.liphium.kingdom.listener.machines;

import com.liphium.kingdom.Kingdom;
import com.liphium.kingdom.game.team.Team;
import com.liphium.kingdom.listener.machines.impl.*;
import com.liphium.kingdom.util.LocationAPI;
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
        registered.add("ItemShop");
        registered.add("CoinDropper");

        for (String s : registered) {
            for (int i = 1; i <= 1000; i++) {
                if (LocationAPI.exists(s + i)) {
                    final var machine = newMachineByLocation(s, LocationAPI.getLocation(s + i));
                    if(machine == null) {
                        break;
                    }

                    machines.add(machine);
                } else break;
            }
        }

        // The coin dropper also supports locations named after the teams (e.g. CoinDropperRed for the Red team)
        for (Team team : Kingdom.getInstance().getGameManager().getTeamManager().getTeams()) {
            if (LocationAPI.exists("CoinDropper" + team.getName())) {
                machines.add(newMachineByLocation("CoinDropper" + team.getName(), LocationAPI.getLocation("CoinDropper" + team.getName())));
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
        if (name.startsWith("CoinDropper")) {
            // CoinDropper1 belongs to the first team, CoinDropper2 to the second, etc.
            Team team = Kingdom.getInstance().getGameManager().getTeamManager().getTeam(name.replace("CoinDropper", ""));
            if(team == null) return null;

            return new ItemDropper(location, "Coin dropper", NamedTextColor.GOLD,
                    () -> new ItemStack(Material.GOLD_NUGGET, Kingdom.getInstance().getGameManager().getTeamManager().getTeamWithLeastPlayers().getPlayers().size()),
                    () -> 10 - (team.getCoinDropperLevel() - 1) * 2);
        }

        return switch (name) {
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
