package com.liphium.kingdom.game.horse;

import com.liphium.core.util.ItemStackBuilder;
import com.liphium.kingdom.Kingdom;
import com.liphium.kingdom.game.flag.FlagManager;
import com.liphium.kingdom.game.state.IngameState;
import com.liphium.kingdom.game.team.Team;
import com.liphium.kingdom.util.CastleRegion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the whistle item, horse summoning, despawning and the sword <-> spear swapping.
 */
public class HorseManager implements Listener {

    public static final int DEATH_COOLDOWN = 30;
    public static final int CASTLE_COOLDOWN = 30;
    public static final double NORMAL_SPEED = 0.3375;
    public static final double CARRIER_SPEED = 0.2;
    public static final double JUMP_STRENGTH = 1.0;
    public static final double MAX_HEALTH = 30.0;

    public static final String OWNER_METADATA = "kingdom-horse-owner";

    private final Map<Player, Horse> horses = new HashMap<>();

    public static ItemStack whistle() {
        return new ItemStackBuilder(Material.GOAT_HORN)
                .withName(Component.text("Whistle", NamedTextColor.AQUA))
                .withLore(Component.text("Right click to summon your horse.", NamedTextColor.GRAY))
                .buildStack();
    }

    /**
     * Called when a player right clicks with the whistle.
     */
    public void onWhistle(Player player) {
        if (!(Kingdom.getInstance().getGameManager().getCurrentState() instanceof IngameState)) {
            return;
        }

        // No horses inside castles
        if (CastleRegion.teamAt(player.getLocation()) != null) {
            player.sendMessage(Kingdom.PREFIX.append(Component.text("You can't summon your horse inside a castle.", NamedTextColor.RED)));
            return;
        }

        // Fix cooldown on goat horn
        if(player.getCooldown(Material.GOAT_HORN) > 0) {
            player.sendMessage(Kingdom.PREFIX.append(Component.text("You can't summon your horse yet.", NamedTextColor.RED)));
            return;
        }
        player.setCooldown(Material.GOAT_HORN, 0);

        // Only one horse per player
        removeHorse(player);

        double speed = Kingdom.getInstance().getFlagManager().isCarrying(player) ? CARRIER_SPEED : NORMAL_SPEED;
        Horse horse = player.getWorld().spawn(player.getLocation(), Horse.class, h -> {
            h.setTamed(true);
            h.setOwner(player);
            h.getInventory().setSaddle(new ItemStack(Material.SADDLE));
            h.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(speed);
            h.getAttribute(Attribute.MAX_HEALTH).setBaseValue(MAX_HEALTH);
            h.setHealth(MAX_HEALTH);
            h.setJumpStrength(JUMP_STRENGTH);
            h.setRemoveWhenFarAway(false);
            h.setMetadata(OWNER_METADATA, new FixedMetadataValue(Kingdom.getInstance(), player.getUniqueId().toString()));
        });
        horse.addPassenger(player);

        horses.put(player, horse);

        player.sendMessage(Kingdom.PREFIX.append(Component.text("Your horse has arrived!", NamedTextColor.GRAY)));
    }

    /**
     * Removes the horse of a player if it exists.
     */
    public void removeHorse(Player player) {
        Horse horse = horses.remove(player);
        if (horse != null && horse.isValid()) {
            horse.remove();
        }
    }

    /**
     * Removes all horses and cooldowns.
     */
    public void stop() {
        for (Player player : new HashMap<>(horses).keySet()) {
            removeHorse(player);
        }
        horses.clear();
    }

    @EventHandler
    public void onWhistleInteract(PlayerInteractEvent event) {
        if (event.getItem() == null || event.getItem().getType() != Material.GOAT_HORN) {
            return;
        }

        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        event.setCancelled(true);

        // No whistling while riding a horse
        if (event.getPlayer().isInsideVehicle()) {
            return;
        }

        onWhistle(event.getPlayer());
    }

    @EventHandler
    public void onMount(EntityMountEvent event) {
        if (!(event.getMount() instanceof Horse horse) || !(event.getEntity() instanceof Player player)) {
            return;
        }

        // Only the owner can ride the horse
        if (!horse.hasMetadata(OWNER_METADATA)
                || !horse.getMetadata(OWNER_METADATA).getFirst().asString().equals(player.getUniqueId().toString())) {
            event.setCancelled(true);
            return;
        }

        // A flag carrier's horse is slower
        if (Kingdom.getInstance().getFlagManager().isCarrying(player)) {
            horse.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(CARRIER_SPEED);
        }

        // Swap all swords in the hotbar for spears
        swapHotbar(player, true);
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (!(event.getDismounted() instanceof Horse horse) || !(event.getEntity() instanceof Player player)) {
            return;
        }

        // Swap all spears in the hotbar back for swords
        swapHotbar(player, false);

        // Swap all spears in the hotbar back for swords
        swapHotbar(player, false);

        // If the horse is dying, the death handler applies the death cooldown
        if (horse.isDead()) {
            return;
        }

        // The horse disappears when the player gets off
        if (horses.get(player) == horse) {
            horses.remove(player);
            if (horse.isValid()) {
                horse.remove();
            }

            // Apply the castle cooldown when the player dismounted inside a castle
            if (CastleRegion.teamAt(player.getLocation()) != null) {
                player.setCooldown(Material.GOAT_HORN, CASTLE_COOLDOWN * 20);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        // No horses in castles: dismount and despawn when riding into one
        if (event.getPlayer().isInsideVehicle()
                && event.getPlayer().getVehicle() instanceof Horse
                && CastleRegion.teamAt(event.getTo()) != null) {
            event.getPlayer().leaveVehicle();
            event.getPlayer().sendMessage(Kingdom.PREFIX.append(Component.text("Your horse has been sent away.", NamedTextColor.GRAY)));
        }
    }

    @EventHandler
    public void onHorseDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Horse horse) || !horse.hasMetadata(OWNER_METADATA)) {
            return;
        }

        // Find the owner through the metadata (works even if the dismount event already fired)
        UUID owner = UUID.fromString(horse.getMetadata(OWNER_METADATA).getFirst().asString());
        Player ownerPlayer = Bukkit.getPlayer(owner);
        if (ownerPlayer == null) {
            return;
        }

        // Make sure nothing is dropped
        event.setDroppedExp(0);
        event.getDrops().clear();

        // Apply the death cooldown to the owner
        horses.values().removeIf(h -> h == horse);
        ownerPlayer.setCooldown(Material.GOAT_HORN, DEATH_COOLDOWN * 20);
        ownerPlayer.sendMessage(Kingdom.PREFIX.append(Component.text("Your horse has died! ", NamedTextColor.RED)
                .append(Component.text(DEATH_COOLDOWN + "s", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text(" until you can whistle again.", NamedTextColor.RED))));
    }

    @EventHandler
    public void onHorseDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Horse horse) || !horse.hasMetadata(OWNER_METADATA)) {
            return;
        }

        // Prevent team members from hurting each other's horses
        if (event.getDamager() instanceof Player player) {
            UUID owner = UUID.fromString(horse.getMetadata(OWNER_METADATA).getFirst().asString());
            Player ownerPlayer = Bukkit.getPlayer(owner);
            if (ownerPlayer != null) {
                Team ownerTeam = Kingdom.getInstance().getGameManager().getTeamManager().getTeam(ownerPlayer);
                Team playerTeam = Kingdom.getInstance().getGameManager().getTeamManager().getTeam(player);
                if (ownerTeam != null && ownerTeam.equals(playerTeam)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onWhistleDrop(PlayerDropItemEvent event) {
        if (event.getItemDrop().getItemStack().isSimilar(whistle())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Kingdom.PREFIX.append(Component.text("You can't drop your whistle.", NamedTextColor.RED)));
        }
    }

    private void swapHotbar(Player player, boolean toSpears) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            String name = item.getType().name();
            if (toSpears && !name.endsWith("_SWORD")) {
                continue;
            }
            if (!toSpears && !name.endsWith("_SPEAR")) {
                continue;
            }

            String target = toSpears ? name.replace("_SWORD", "_SPEAR") : name.replace("_SPEAR", "_SWORD");
            Material material = Material.matchMaterial(target);
            if (material == null) {
                continue;
            }

            ItemStack swapped = item.clone();
            swapped.setType(material);
            player.getInventory().setItem(slot, swapped);
        }
    }
}
