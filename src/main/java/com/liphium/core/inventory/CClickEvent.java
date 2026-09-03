package com.liphium.core.inventory;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public record CClickEvent(Player player, ItemStack stack) {

}
