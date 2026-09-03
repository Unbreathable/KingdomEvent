package com.liphium.snowsplash.game.team.impl;

import com.liphium.core.util.ItemStackBuilder;
import com.liphium.snowsplash.game.team.Team;
import com.liphium.snowsplash.util.LocationAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.time.Duration;
import java.util.Objects;

public class ColoredTeam extends Team {

    public ColoredTeam(String name, NamedTextColor color, Material material) {
        super(name, color, material);
    }

    @Override
    public void giveKit(Player player, boolean teleport) {
        if (teleport) {
            player.teleport(Objects.requireNonNull(LocationAPI.getLocation(this.getName())));
        }

        // Get color from NamedTextColor RGB values
        Color armorColor = Color.fromRGB(this.getColor().value());

        // Create colored leather boots
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta bootsMeta = (LeatherArmorMeta) boots.getItemMeta();
        if (bootsMeta != null) {
            bootsMeta.setUnbreakable(true);
            bootsMeta.setColor(armorColor);
            boots.setItemMeta(bootsMeta);
        }

        // Create colored leather leggings
        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta leggingsMeta = (LeatherArmorMeta) leggings.getItemMeta();
        if (leggingsMeta != null) {
            leggingsMeta.setUnbreakable(true);
            leggingsMeta.setColor(armorColor);
            leggings.setItemMeta(leggingsMeta);
        }

        // Equip the armor
        player.getInventory().setBoots(boots);
        player.getInventory().setLeggings(leggings);
        if(!player.getInventory().contains(Material.WOODEN_SWORD)) {
            player.getInventory().addItem(new ItemStackBuilder(Material.WOODEN_SWORD).makeUnbreakable().buildStack());
            player.getInventory().addItem(new ItemStackBuilder(Material.WOODEN_AXE).makeUnbreakable().buildStack());
        }
    }

    @Override
    public void sendStartMessage() {
        for (Player player : getPlayers()) {
            player.sendMessage(Component.text(" "));
            player.sendMessage(Component.text("    ", NamedTextColor.GRAY)
                    .append(Component.text("You are in team ", NamedTextColor.GRAY))
                    .append(Component.text(this.getName(), this.getColor(), TextDecoration.BOLD))
                    .append(Component.text("!", NamedTextColor.GRAY)));
            player.sendMessage(Component.text(" "));
            player.sendMessage(Component.text("Destroy the ", NamedTextColor.GRAY)
                    .append(Component.text("snowman ", NamedTextColor.AQUA))
                    .append(Component.text("on the other", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("side to win the game!", NamedTextColor.GRAY));
            player.sendMessage(Component.text(" "));
        }
    }

    @Override
    public void handleWin() {
        Bukkit.broadcast(Component.text(" "));
        Bukkit.broadcast(Component.text("   ", NamedTextColor.GRAY)
                .append(Component.text("Team", this.getColor())).appendSpace()
                .append(Component.text(this.getName(), this.getColor(), TextDecoration.BOLD)).appendSpace()
                .append(Component.text("won the", NamedTextColor.GRAY)).appendSpace()
                .append(Component.text("game", this.getColor()))
                .append(Component.text("!", NamedTextColor.GRAY)));
        Bukkit.broadcast(Component.text(" "));

        for (Player player : getPlayers()) {
            player.showTitle(Title.title(Component.text("Victory Royale", NamedTextColor.GREEN, TextDecoration.BOLD), Component.empty(), Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))));
            player.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_1, 1f, 1f);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!getPlayers().contains(player)) {
                player.showTitle(Title.title(Component.text("Game Lost", NamedTextColor.RED, TextDecoration.BOLD), Component.empty(), Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))));
                player.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_1, 1f, 1f);
            }
        }
    }
}
