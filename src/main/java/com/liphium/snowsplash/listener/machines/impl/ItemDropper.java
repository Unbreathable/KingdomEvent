package com.liphium.snowsplash.listener.machines.impl;

import com.liphium.snowsplash.listener.machines.Machine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.util.function.Supplier;

public class ItemDropper extends Machine {

    private final UUID uniqueId;
    private final String name;
    private final NamedTextColor color;
    private final ItemStack toDrop;
    private final Supplier<Integer>  dropRate;

    public ItemDropper(Location location, String name, NamedTextColor color, ItemStack toDrop, Supplier<Integer> ticks) {
        super(location, true);

        this.name = name;
        this.color = color;
        this.toDrop = toDrop;
        this.dropRate = ticks;
        count = ticks.get();

        ArmorStand stand = location.getWorld().spawn(location.clone().add(0, -1, 0), ArmorStand.class);

        stand.setCustomNameVisible(true);
        stand.customName(Component.text(name, color).appendSpace().append(Component.text("production happens here.", NamedTextColor.GRAY)));
        stand.setGravity(false);
        stand.setInvisible(true);
        stand.setInvulnerable(true);
        stand.setRemoveWhenFarAway(false);

        uniqueId = stand.getUniqueId();
    }

    int count = 10, tickCount = 0;

    @Override
    public void tick() {
        if (broken) {
            return;
        }
        if (tickCount++ >= 20) {
            tickCount = 0;

            final var stand = (ArmorStand) location.getWorld().getEntity(uniqueId);
            if (stand == null) {
                return;
            }

            count--;
            stand.customName(Component.text(name, color).appendSpace().append(Component.text("in", NamedTextColor.GRAY)).appendSpace().append(Component.text(count, color, TextDecoration.BOLD)).appendSpace().append(Component.text("..", NamedTextColor.GRAY)));

            if (count == 0) {
                count = dropRate.get();

                Item item = location.getWorld().dropItem(location.clone().add(0.5, 1.5, 0.5), toDrop);
                item.setVelocity(new Vector());
            }
        }
    }

    @Override
    public void destroy() {
        final var stand = (ArmorStand) location.getWorld().getEntity(uniqueId);
        if (stand == null) {
            return;
        }
        stand.remove();
    }
}
