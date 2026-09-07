package com.liphium.kingdom.game.team.impl;

import com.liphium.core.util.ItemStackBuilder;
import com.liphium.kingdom.game.horse.HorseManager;
import com.liphium.kingdom.game.team.Team;
import com.liphium.kingdom.util.LocationAPI;
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
        player.getInventory().setHelmet(teamHelmet(this.getColor()));

        // Default equipment: copper tools + whistle
        player.getInventory().addItem(new ItemStackBuilder(Material.COPPER_SWORD).makeUnbreakable().buildStack());
        player.getInventory().addItem(new ItemStackBuilder(Material.COPPER_PICKAXE).makeUnbreakable().buildStack());
        player.getInventory().addItem(new ItemStackBuilder(Material.COPPER_AXE).makeUnbreakable().buildStack());
        player.getInventory().addItem(HorseManager.whistle());
    }

    /**
     * Returns an unbreakable leather helmet colored like the team (same color as the other armor).
     */
    public static ItemStack teamHelmet(NamedTextColor color) {
        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta helmetMeta = (LeatherArmorMeta) helmet.getItemMeta();
        if (helmetMeta != null) {
            helmetMeta.setUnbreakable(true);
            helmetMeta.setColor(Color.fromRGB(color.value()));
            helmet.setItemMeta(helmetMeta);
        }
        return helmet;
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
            player.sendMessage(Component.text("Steal the ", NamedTextColor.GRAY)
                    .append(Component.text("enemy flag ", NamedTextColor.AQUA))
                    .append(Component.text("and carry it to your ", NamedTextColor.GRAY))
                    .append(Component.text("own flag spot", NamedTextColor.AQUA))
                    .append(Component.text("!", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("After 15 minutes the team with the most flags wins!", NamedTextColor.GRAY));
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
