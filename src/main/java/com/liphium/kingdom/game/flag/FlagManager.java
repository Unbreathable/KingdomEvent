package com.liphium.kingdom.game.flag;

import com.liphium.kingdom.Kingdom;
import com.liphium.kingdom.game.team.Team;
import com.liphium.kingdom.game.team.impl.ColoredTeam;
import com.liphium.kingdom.util.LocationAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the two team flags: the stands in each base, carrying, dropping and capturing.
 */
public class FlagManager implements Listener {

    public static final int RETURN_SECONDS = 10;
    public static final double PICKUP_RANGE = 0.5;
    public static final double CAPTURE_RANGE = 1.0;

    private final Map<Team, ArmorStand> stands = new HashMap<>();
    private final Map<Team, Player> carriers = new HashMap<>();
    private final Map<Team, DroppedFlag> dropped = new HashMap<>();
    private final Map<Team, Integer> scores = new HashMap<>();

    private static class DroppedFlag {
        final ArmorStand flag, hologram;
        // The location the flag was dropped at (the base for the walk-over pickup check,
        // NOT the armor stand itself which is spawned lower for the banner visual)
        final Location baseLocation;
        int returnTimer = RETURN_SECONDS;

        DroppedFlag(ArmorStand flag, ArmorStand hologram, Location baseLocation) {
            this.flag = flag;
            this.hologram = hologram;
            this.baseLocation = baseLocation;
        }
    }

    /**
     * Spawns the flag stands for all teams and resets all scores.
     */
    public void start() {
        stop();
        scores.clear();

        for (Team team : Kingdom.getInstance().getGameManager().getTeamManager().getTeams()) {
            scores.put(team, 0);
            restoreStand(team);
        }
    }

    /**
     * Spawns a flag stand for a team at the team's flag location.
     */
    private ArmorStand spawnStandForTeam(Location location, Team team) {
        return location.getWorld().spawn(location.clone().add(0, -0.6, 0), ArmorStand.class, s -> {
            s.setVisible(false);
            s.setMarker(true);
            s.setGravity(false);
            s.setInvulnerable(true);
            s.setRemoveWhenFarAway(false);
            s.getEquipment().setHelmet(new ItemStack(team.getBanner()));
        });
    }

    /**
     * Removes all stands, dropped items and resets carried flags.
     */
    public void stop() {
        carriers.clear();
        for (ArmorStand stand : stands.values()) {
            stand.remove();
        }
        stands.clear();
        for (DroppedFlag flag : dropped.values()) {
            flag.flag.remove();
            flag.hologram.remove();
        }
        dropped.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getInventory().getHelmet() != null && isBanner(player.getInventory().getHelmet().getType())) {
                player.getInventory().setHelmet(null);
            }
        }
    }

    private boolean isBanner(Material material) {
        return material.name().endsWith("_BANNER");
    }

    /**
     * Called every tick from the IngameState: pickups and captures.
     */
    public void tick() {
        // Handle dropped flags (walk-over pickup)
        for (Team team : new HashMap<>(dropped).keySet()) {
            DroppedFlag flag = dropped.get(team);

            // Let enemies pick it up when they are close (check from the base location:
            // the armor stand itself is spawned 1.8 blocks lower, so players would never
            // be in range when scanning around it)
            for (Player player : flag.baseLocation.getNearbyPlayers(PICKUP_RANGE, 2, PICKUP_RANGE)) {
                Team playerTeam = Kingdom.getInstance().getGameManager().getTeamManager().getTeam(player);
                if (playerTeam != null && !playerTeam.equals(team) && pickUpDropped(team, player)) {
                    removeDropped(team);
                    break;
                }
            }
        }

        // Handle flag stands (enemies stealing flags from stands)
        // Copy the key set: takeFlag removes the stand from the map while we are iterating
        for (Team team : List.copyOf(stands.keySet())) {
            ArmorStand stand = stands.get(team);
            if (carriers.containsKey(team) || dropped.containsKey(team)) {
                continue;
            }

            for (Player player : stand.getLocation().getNearbyPlayers(PICKUP_RANGE, 2, PICKUP_RANGE)) {
                Team playerTeam = Kingdom.getInstance().getGameManager().getTeamManager().getTeam(player);
                if (playerTeam != null && !playerTeam.equals(team)) {
                    takeFlag(team, player);
                    break;
                }
            }
        }

        // Handle captures (carriers reaching their own flag spot)
        for (Team team : new HashMap<>(carriers).keySet()) {
            Player carrier = carriers.get(team);

            // Safety: drop the flag if the carrier is gone
            if (carrier == null || !carrier.isOnline() || carrier.isDead()) {
                if (carrier == null) {
                    carriers.remove(team);
                    restoreStand(team);
                } else {
                    dropFlag(carrier);
                }
                continue;
            }

            Team carrierTeam = Kingdom.getInstance().getGameManager().getTeamManager().getTeam(carrier);
            if (carrierTeam == null) {
                continue;
            }

            Location ownFlag = LocationAPI.safe(carrierTeam.getName() + "-Flag");
            if (ownFlag == null) {
                continue;
            }

            if (carrier.getLocation().distance(ownFlag) <= CAPTURE_RANGE) {
                capture(carrier, carrierTeam, team);
            }
        }
    }

    /**
     * Called once a second from the IngameState: return timers.
     */
    public void tickPerSecond() {
        for (Team team : new HashMap<>(dropped).keySet()) {
            DroppedFlag flag = dropped.get(team);

            // Count down the return timer
            if (--flag.returnTimer <= 0) {
                removeDropped(team);
                restoreStand(team);
                Bukkit.broadcast(Kingdom.PREFIX.append(Component.text("The ", NamedTextColor.GRAY)
                        .append(Component.text(team.getName() + " flag", team.getColor()))
                        .append(Component.text(" has returned to its base.", NamedTextColor.GRAY))));
            } else {
                flag.hologram.customName(Component.text(team.getName() + " flag", team.getColor())
                        .append(Component.text(" returns in ", NamedTextColor.GRAY))
                        .append(Component.text(flag.returnTimer + "s", team.getColor(), TextDecoration.BOLD)));
            }
        }
    }

    private void removeDropped(Team team) {
        DroppedFlag flag = dropped.remove(team);
        if (flag == null) {
            return;
        }
        flag.flag.remove();
        flag.hologram.remove();
    }

    /**
     * Lets a player take a flag from the stand: hides the stand and puts the banner on their head.
     */
    private boolean takeFlag(Team flagTeam, Player carrier) {
        ArmorStand stand = stands.get(flagTeam);
        if (stand == null || carriers.containsKey(flagTeam) || dropped.containsKey(flagTeam) || isCarrying(carrier) || carrier.isDead() || carrier.getGameMode() != GameMode.SURVIVAL) {
            return false;
        }

        stand.remove();
        stands.remove(flagTeam);
        carriers.put(flagTeam, carrier);

        carrier.getInventory().setHelmet(new ItemStack(flagTeam.getBanner()));

        Bukkit.broadcast(Kingdom.PREFIX.append(Component.text(carrier.getName(), NamedTextColor.AQUA)
                .append(Component.text(" took the ", NamedTextColor.GRAY))
                .append(Component.text(flagTeam.getName() + " flag", flagTeam.getColor()))
                .append(Component.text("!", NamedTextColor.GRAY))));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 0.5f);
        }
        return true;
    }

    /**
     * Lets a player pick up a dropped flag from the ground.
     */
    private boolean pickUpDropped(Team flagTeam, Player carrier) {
        if (carriers.containsKey(flagTeam) || isCarrying(carrier) || carrier.isDead() || carrier.getGameMode() != GameMode.SURVIVAL) {
            return false;
        }

        carriers.put(flagTeam, carrier);
        carrier.getInventory().setHelmet(new ItemStack(flagTeam.getBanner()));

        Bukkit.broadcast(Kingdom.PREFIX.append(Component.text(carrier.getName(), NamedTextColor.AQUA)
                .append(Component.text(" picked up the ", NamedTextColor.GRAY))
                .append(Component.text(flagTeam.getName() + " flag", flagTeam.getColor()))
                .append(Component.text("!", NamedTextColor.GRAY))));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 0.5f);
        }
        return true;
    }

    /**
     * Called when a flag carrier dies (or disconnects): drops the flag at their location.
     */
    public void dropFlag(Player carrier) {
        Team flagTeam = getFlagTeam(carrier);
        if (flagTeam == null) {
            return;
        }

        carriers.remove(flagTeam);
        Location location = carrier.getLocation();

        ArmorStand flag = spawnStandForTeam(location.clone().add(0, -1.2, 0), flagTeam);
        var shift = location.getDirection();
        shift.setY(0);
        shift.normalize().multiply(-0.4);
        ArmorStand hologram = location.getWorld().spawn(location.clone().add(0, 1.8, 0).add(shift), ArmorStand.class, s -> {
            s.setVisible(false);
            s.setMarker(true);
            s.setGravity(false);
            s.setInvulnerable(true);
            s.setRemoveWhenFarAway(false);
            s.setCustomNameVisible(true);
        });

        dropped.put(flagTeam, new DroppedFlag(flag, hologram, location));
    }

    private void restoreStand(Team team) {
        if(stands.containsKey(team)) {
            stands.get(team).remove();
        }

        Location location = LocationAPI.safe(team.getName() + "-Flag");
        if (location == null) {
            Kingdom.getInstance().getLogger().warning("Missing location '" + team.getName() + "-Flag'.");
            return;
        }

        stands.put(team, spawnStandForTeam(location, team));
    }

    private void capture(Player carrier, Team carrierTeam, Team flagTeam) {
        carriers.remove(flagTeam);
        carrier.getInventory().setHelmet(ColoredTeam.teamHelmet(carrierTeam.getColor()));
        scores.put(carrierTeam, scores.getOrDefault(carrierTeam, 0) + 1);

        restoreStand(flagTeam);

        Bukkit.broadcast(Kingdom.PREFIX.append(Component.text(carrier.getName(), NamedTextColor.AQUA)
                .append(Component.text(" captured the ", NamedTextColor.GRAY))
                .append(Component.text(flagTeam.getName() + " flag", flagTeam.getColor()))
                .append(Component.text("!", NamedTextColor.GRAY))));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }
    }

    public boolean isCarrying(Player player) {
        return getFlagTeam(player) != null;
    }

    public Team getFlagTeam(Player player) {
        for (Team team : carriers.keySet()) {
            if (carriers.get(team).equals(player)) {
                return team;
            }
        }
        return null;
    }

    public int getScore(Team team) {
        return scores.getOrDefault(team, 0);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // The helmet slot is always locked (flag or barrier)
        if (event.getSlotType() == InventoryType.SlotType.ARMOR && event.getSlot() == 39) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        // The helmet slot is always locked (flag or barrier)
        if (event.getInventorySlots().contains(39)) {
            event.setCancelled(true);
        }
    }
}
