package com.liphium.snowsplash.game.team;

import com.liphium.snowsplash.Snowsplash;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class Team {

    private final String name;
    private final NamedTextColor color;
    private final Material material;
    private final ArrayList<Player> players = new ArrayList<>();

    public Team(String name, NamedTextColor color, Material material) {
        this.name = name;
        this.color = color;
        this.material = material;
    }

    public String getName() {
        return name;
    }

    public NamedTextColor getColor() {
        return color;
    }

    public Material getMaterial() {
        return material;
    }

    public void addPlayer(Player player) {
        players.add(player);
    }

    public ArrayList<Player> getPlayers() {
        return players;
    }

    public ArrayList<Component> playerLore() {
        ArrayList<Component> lore = new ArrayList<>();

        if (players.isEmpty()) {
            lore.add(Component.text("Click to join!", NamedTextColor.GRAY));
        } else {
            for (Player player : players) {
                lore.add(Component.text("- " + player.getName(), NamedTextColor.GRAY));
            }
        }

        return lore;
    }

    public boolean isJoinable() {
        return (Snowsplash.getInstance().getGameManager().getMaxTeamSize() > getPlayers().size());
    }

    public void giveKit(Player player, boolean teleport) {
    }

    public void tick() {
    }

    public void sendStartMessage() {
    }

    public void join(Player player) {
    }

    public void handleWin() {
    }
}
