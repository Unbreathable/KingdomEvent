package com.liphium.kingdom.game.team;

import com.liphium.kingdom.Kingdom;
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
    private int coinDropperLevel = 1;

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

    public Material getBanner() {
        return switch (name) {
            case "Red" -> Material.RED_BANNER;
            case "Blue" -> Material.BLUE_BANNER;
            default -> Material.WHITE_BANNER;
        };
    }

    public int getCoinDropperLevel() {
        return coinDropperLevel;
    }

    public void setCoinDropperLevel(int coinDropperLevel) {
        this.coinDropperLevel = coinDropperLevel;
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
        return (Kingdom.getInstance().getGameManager().getMaxTeamSize() > getPlayers().size());
    }

    public void giveKit(Player player, boolean teleport) {
    }

    public void tick() {
    }

    public void sendStartMessage() {
    }

    public void handleWin() {
    }
}
