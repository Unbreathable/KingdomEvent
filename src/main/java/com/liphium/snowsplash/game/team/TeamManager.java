package com.liphium.snowsplash.game.team;

import com.liphium.snowsplash.game.team.impl.ColoredTeam;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class TeamManager {

    private final ArrayList<Team> teams = new ArrayList<>();

    public TeamManager() {

        // Register teams
        teams.add(new ColoredTeam("Red", NamedTextColor.RED, Material.RED_WOOL));
        teams.add(new ColoredTeam("Blue", NamedTextColor.BLUE, Material.BLUE_WOOL));
    }

    public Team getTeam(String name) {
        for (Team team : teams) {
            if (team.getName().equalsIgnoreCase(name)) {
                return team;
            }
        }

        return null;
    }

    public Team getTeamWithLeastPlayers() {
        Team currentTeam = null;
        for (Team team : teams) {
            if(currentTeam == null) {
                currentTeam = team;
            }

            if(currentTeam.getPlayers().size() > team.getPlayers().size()) {
                currentTeam = team;
            }
        }

        return currentTeam;
    }

    public Team getTeam(Player player) {
        for (Team team : teams) {
            if (team.getPlayers().contains(player)) {
                return team;
            }
        }

        return null;
    }

    public void tick() {
        for (Team team : teams) {
            team.tick();
        }
    }

    public ArrayList<Team> getTeams() {
        return teams;
    }
}
