package com.liphium.kingdom.game.state;

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent;
import com.liphium.core.util.ItemStackBuilder;
import com.liphium.kingdom.Kingdom;
import com.liphium.kingdom.game.GameState;
import com.liphium.kingdom.game.team.Team;
import com.liphium.kingdom.screens.ItemShopScreen;
import com.liphium.kingdom.util.CastleRegion;
import com.liphium.kingdom.util.LocationAPI;
import com.liphium.kingdom.util.Messages;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.*;

public class IngameState extends GameState {

    private final ArrayList<DroppableTrap> traps = new ArrayList<>();

    private Runnable runnable;

    private static final int GAME_SECONDS = 15 * 60;

    // Respawn
    private static final int RESPAWN_SECONDS = 10;

    // Economy
    private static final int KILL_REWARD = 5;
    private static final Material CURRENCY = Material.GOLD_NUGGET;

    // Explosives
    public static final Key TNT_BOW_COOLDOWN_KEY = Key.key("kingdom", "tnt_bow_cooldown");
    private static final int TNT_BOW_COOLDOWN = 60;

    // Block regeneration
    private static final int WOOD_COBBLE_REGEN_SECONDS = 5;
    private static final Map<Material, Integer> ORE_REGEN_SECONDS = Map.ofEntries(
            Map.entry(Material.COAL_ORE, 10), Map.entry(Material.DEEPSLATE_COAL_ORE, 10),
            Map.entry(Material.COPPER_ORE, 10), Map.entry(Material.DEEPSLATE_COPPER_ORE, 10),
            Map.entry(Material.IRON_ORE, 10), Map.entry(Material.DEEPSLATE_IRON_ORE, 10),
            Map.entry(Material.GOLD_ORE, 10), Map.entry(Material.DEEPSLATE_GOLD_ORE, 10),
            Map.entry(Material.DIAMOND_ORE, 20), Map.entry(Material.DEEPSLATE_DIAMOND_ORE, 20),
            Map.entry(Material.EMERALD_ORE, 10), Map.entry(Material.DEEPSLATE_EMERALD_ORE, 10)
    );

    // Ore purchases (5 per team per type)
    private static final int MAX_ORES_PER_TEAM = 5;

    public IngameState() {
        super("In game", GAME_SECONDS);
    }

    private final HashMap<Location, Boolean> placedBlocks = new HashMap<>();
    private final HashMap<Location, Integer> toDeleteAfter = new HashMap<>();

    // Block regeneration: location -> (material, seconds left)
    private final HashMap<Location, PendingRegen> regenerating = new HashMap<>();
    // Purchased ores per team
    private final HashMap<Team, HashMap<Material, Integer>> boughtOres = new HashMap<>();

    private static class PendingRegen {
        final Material material;
        int seconds;

        PendingRegen(Material material, int seconds) {
            this.material = material;
            this.seconds = seconds;
        }
    }

    @Override
    public void start() {

        final var world = Bukkit.getWorld(Kingdom.GAME_WORLD);
        assert(world != null);
        world.setTime(0);
        world.setThundering(false);
        world.setStorm(false);
        world.setGameRule(GameRules.NATURAL_HEALTH_REGENERATION, false);
        world.setGameRule(GameRules.ADVANCE_TIME, false);
        world.setGameRule(GameRules.ADVANCE_WEATHER, false);
        world.setGameRule(GameRules.LOCATOR_BAR, false);
        world.setGameRule(GameRules.KEEP_INVENTORY, true);
        world.setDifficulty(Difficulty.EASY);

        ArrayList<Player> playersWithOutTeam = new ArrayList<>(Bukkit.getOnlinePlayers());
        playersWithOutTeam.removeIf(x -> Kingdom.getInstance().getGameManager().getTeamManager().getTeam(x) != null);

        for (Player player : playersWithOutTeam) {
            Team team = Kingdom.getInstance().getGameManager().getTeamManager().getTeamWithLeastPlayers();
            team.addPlayer(player);
        }

        // World cleanup
        for (Entity entity : Objects.requireNonNull(Bukkit.getWorld(Kingdom.GAME_WORLD)).getEntities()) {
            if (entity.getType() != EntityType.ARMOR_STAND && entity.getType() != EntityType.PLAYER) entity.remove();
        }

        // Set up the flags and remove old horses
        Kingdom.getInstance().getFlagManager().start();
        Kingdom.getInstance().getHorseManager().stop();

        // Initialize all the teams
        for (Team team : Kingdom.getInstance().getGameManager().getTeamManager().getTeams()) {
            team.sendStartMessage();

            for (Player player : team.getPlayers()) {
                player.getInventory().clear();
                player.setHealth(20);
                player.setFoodLevel(20);
                team.giveKit(player, true);
            }
        }

        // Start the game loop
        Kingdom.getInstance().getTaskManager().inject(runnable = new Runnable() {
            int tickCount = 0;

            @Override
            public void run() {
                Kingdom.getInstance().getGameManager().getTeamManager().tick();
                Kingdom.getInstance().getMachineManager().tick();
                Kingdom.getInstance().getFlagManager().tick();

                // Process regenerating blocks and the dropped flags once a second
                if (tickCount++ >= 20) {
                    tickCount = 0;

                    processRegeneration();
                    Kingdom.getInstance().getFlagManager().tickPerSecond();

                    // The timer can be paused with /timer pause
                    if (!paused) count--;

                    // Action bar with the remaining time and the flag scores
                    var teams = Kingdom.getInstance().getGameManager().getTeamManager().getTeams();
                    var bar = Component.text(formatTime(count), NamedTextColor.AQUA, TextDecoration.BOLD).append(Component.text("  |  ", NamedTextColor.DARK_GRAY));
                    // Scores as "1 : 0 flags": numbers colored by team, the word stays gray & not bold
                    for (int index = 0; index < teams.size(); index++) {
                        Team team = teams.get(index);
                        if (index > 0) {
                            bar = bar.append(Component.text(" : ", NamedTextColor.DARK_GRAY));
                        }
                        bar = bar.append(Component.text(String.valueOf(Kingdom.getInstance().getFlagManager().getScore(team)), team.getColor())
                                .decoration(TextDecoration.BOLD, false));
                    }
                    bar = bar.append(Component.text(" flags", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false));
                    Messages.actionBar(bar);

                    // End the game when the time is over
                    if (count <= 0) {
                        handleTimeOver();
                    }
                }
            }
        });
    }

    /**
     * Returns "flag" for exactly one and "flags" for everything else.
     */
    public static String flagWord(int count) {
        return count == 1 ? "flag" : "flags";
    }

    private String formatTime(int seconds) {
        if(paused) {
            return "PAUSED";
        }
        if (seconds < 0) seconds = 0;
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    private void handleTimeOver() {
        Kingdom.getInstance().getTaskManager().uninject(runnable);
        Kingdom.getInstance().getFlagManager().stop();
        Kingdom.getInstance().getHorseManager().stop();

        var teams = Kingdom.getInstance().getGameManager().getTeamManager().getTeams();
        Team winner = null;
        int highest = -1;
        boolean tie = false;
        for (Team team : teams) {
            int score = Kingdom.getInstance().getFlagManager().getScore(team);
            if (score > highest) {
                highest = score;
                winner = team;
                tie = false;
            } else if (score == highest) {
                tie = true;
            }
        }

        if (winner == null || tie) {
            Bukkit.broadcast(Kingdom.PREFIX.append(Component.text("It's a tie! ", NamedTextColor.GRAY)
                    .append(Component.text("Both teams captured " + highest + " " + flagWord(highest) + ".", NamedTextColor.GRAY))));
        } else {
            winner.handleWin();
        }

        Kingdom.getInstance().getGameManager().setCurrentState(new EndState());
    }

    public void handleWin(Team team) {
        team.handleWin();
        Kingdom.getInstance().getTaskManager().uninject(runnable);
        Kingdom.getInstance().getFlagManager().stop();
        Kingdom.getInstance().getHorseManager().stop();
        Kingdom.getInstance().getGameManager().setCurrentState(new EndState());
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (event.getItem() != null && (event.getItem().getType() == Material.WIND_CHARGE)) {
            return;
        }

        // Block using the TNT bow while it's on cooldown (other bows stay usable)
        if (event.getItem() != null && event.getItem().getType() == Material.BOW
                && (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)
                && isTntBow(event.getItem())
                && event.getPlayer().getCooldown(TNT_BOW_COOLDOWN_KEY) > 0) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Kingdom.PREFIX.append(Component.text("Your TNT bow is still on cooldown!", NamedTextColor.RED)));
            return;
        }

        Kingdom.getInstance().getMachineManager().onInteract(event);

        if (event.getItem() != null) {
            Team team = Kingdom.getInstance().getGameManager().getTeamManager().getTeam(event.getPlayer());
            ItemStack usedItem = event.getItem();

            var hit = false;
            DroppableTrap trapToPlace = null;

            // Throwable fireballs: launch a fireball in the direction the player is looking
            if (usedItem.getType() == Material.FIRE_CHARGE
                    && (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)) {
                event.setCancelled(true);
                reduceMainHandItem(event.getPlayer(), Material.FIRE_CHARGE);

                Fireball fireball = event.getPlayer().launchProjectile(Fireball.class);
                fireball.setVelocity(event.getPlayer().getEyeLocation().getDirection().multiply(1.5));
                fireball.setYield(2f);
                return;
            }

            if (event.getClickedBlock() != null) {
                switch (usedItem.getType()) {
                    case Material.GRAY_DYE -> {
                        reduceMainHandItem(event.getPlayer(), Material.GRAY_DYE);
                        trapToPlace = new SlowTrap(event.getClickedBlock().getLocation().clone().add(0.5, 1, 0.5), team);
                    }
                    case Material.LIME_DYE -> {
                        reduceMainHandItem(event.getPlayer(), Material.LIME_DYE);
                        trapToPlace = new PoisonTrap(event.getClickedBlock().getLocation().clone().add(0.5, 1, 0.5), team);
                    }
                    case Material.GUNPOWDER -> {
                        reduceMainHandItem(event.getPlayer(), Material.GUNPOWDER);
                        trapToPlace = new ExplosionTrap(event.getClickedBlock().getLocation().clone().add(0.5, 1, 0.5), team);
                    }
                    case Material.WHITE_DYE -> {
                        reduceMainHandItem(event.getPlayer(), Material.WHITE_DYE);
                        trapToPlace = new WebTrap(event.getClickedBlock().getLocation().clone().add(0.5, 1, 0.5), team);
                    }
                    default -> {
                    }
                }
            }

            // Place the trap
            if (trapToPlace != null) {
                traps.add(trapToPlace);
                trapToPlace.drop();
                event.getPlayer().sendMessage(Kingdom.PREFIX
                        .append(Component.text("Trap placed!", NamedTextColor.GRAY)));
            }
        }
    }

    void reduceMainHandItem(Player player, Material material) {
        if (player.getInventory().getItemInMainHand().getType() == material) {
            int amount = player.getInventory().getItemInMainHand().getAmount();
            if (amount == 1) {
                player.getInventory().setItemInMainHand(null);
            } else player.getInventory().getItemInMainHand().setAmount(amount - 1);
        } else if (player.getInventory().getItemInOffHand().getType() == material) {
            int amount = player.getInventory().getItemInOffHand().getAmount();
            if (amount == 1) {
                player.getInventory().setItemInOffHand(null);
            } else player.getInventory().getItemInOffHand().setAmount(amount - 1);
        }
    }

    @Override
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Kingdom.getInstance().getMachineManager().onInteractAtEntity(event);

        if (event.getRightClicked().getType().equals(EntityType.ARMOR_STAND)) {
            event.setCancelled(true);
        }
    }

    @Override
    public void join(Player player) {
        player.kick(Component.text("You can only join after the game has finished.", NamedTextColor.RED));
    }

    @Override
    public void onMove(PlayerMoveEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        Team team = Kingdom.getInstance().getGameManager().getTeamManager().getTeam(event.getPlayer());

        // Check if they wandered into a trap
        ArrayList<DroppableTrap> toRemove = new ArrayList<>();
        for (DroppableTrap trap : traps) {
            if (trap.location.distance(event.getPlayer().getLocation()) <= 4 && !team.getName().equals(trap.team.getName())) {

                // Make sure the trap is actually visible
                final var toRaytrace = Arrays.asList(
                        event.getPlayer().getLocation(), // Feet
                        event.getPlayer().getLocation().clone().add(0, 1, 0), // Middle
                        event.getPlayer().getLocation().clone().add(0, 2, 0) // Eyes
                );

                var found = false;
                for (final var toTrace : toRaytrace) {
                    final var direction = toTrace.clone().subtract(trap.location).toVector().normalize();
                    final var distance = trap.location.distance(toTrace);
                    final var result = trap.location.getWorld().rayTraceBlocks(trap.location, direction, distance, FluidCollisionMode.NEVER, true);

                    if (result == null) {
                        found = true;
                        break;
                    }
                }

                if (found) {
                    toRemove.add(trap);
                    trap.doEffect(List.of(event.getPlayer()));
                    for (var loc : trap.blocksToDelete()) {
                        toDeleteAfter.put(loc, 120);
                    }
                }
                break;
            }
        }
        for (DroppableTrap rem : toRemove) {
            rem.item.remove();
            traps.remove(rem);
        }
    }

    @Override
    public void onDamage(EntityDamageEvent event) {
        event.setCancelled(event.getEntity().getType() == EntityType.ITEM);
    }

    @Override
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity().getType() == EntityType.ARMOR_STAND || event.getEntity().getType() == EntityType.ITEM) {
            event.setCancelled(true);
        }

        // No friendly fire between players
        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player attacker) {
            Team victimTeam = Kingdom.getInstance().getGameManager().getTeamManager().getTeam(victim);
            Team attackerTeam = Kingdom.getInstance().getGameManager().getTeamManager().getTeam(attacker);

            if (victimTeam != null && victimTeam.equals(attackerTeam)) {
                event.setCancelled(true);
            }
        }
    }

    @Override
    public void onKnockbackByEntity(EntityKnockbackByEntityEvent event) {
        if (event.getEntity().getType() == EntityType.ITEM) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() == null || !(event.getEntity().getShooter() instanceof Player player)) return;

        Team shooterTeam = Kingdom.getInstance().getGameManager().getTeamManager().getTeam(player);
        if (shooterTeam == null) {
            event.setCancelled(true);
            return;
        }

        event.getEntity().setMetadata("team", new FixedMetadataValue(Kingdom.getInstance(), shooterTeam.getName()));

        // Handle the TNT bow
        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (weapon.getType() == Material.BOW && isTntBow(weapon)) {
            player.setCooldown(TNT_BOW_COOLDOWN_KEY, TNT_BOW_COOLDOWN * 20);
            event.getEntity().setMetadata("tntbow", new FixedMetadataValue(Kingdom.getInstance(), true));
        }
    }

    private boolean isTntBow(ItemStack item) {
        final var meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(Kingdom.TNT_BOW_KEY, PersistentDataType.BYTE);
    }

    @Override
    public void onEntityExplode(EntityExplodeEvent event) {
        // Explosions only destroy placed blocks, every other block is only protected in castles
        event.blockList().removeIf(block -> {

            // If the block was placed, it can safely be removed
            if (placedBlocks.containsKey(block.getLocation())) {
                placedBlocks.remove(block.getLocation());
                return false;
            }

            // Check if it's in castle and if so, cancel the explosion for that block
            Team castleTeam = CastleRegion.teamAt(block.getLocation());
            return castleTeam != null;
        });
    }

    @Override
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getLocation().getY() >= 250) {
            event.setCancelled(true);
            return;
        }

        for (DroppableTrap trap : traps) {
            if (trap.location.distance(event.getBlock().getLocation()) <= 2) {
                event.getPlayer().sendMessage(Component.text("You can't place a block near a trap!", NamedTextColor.RED));
                event.setCancelled(true);
                return;
            }
        }

        // Instantly light TNT
        if (event.getBlock().getType() == Material.TNT) {
            event.getBlock().setType(Material.AIR);
            final var world = event.getBlock().getWorld();
            world.spawnEntity(event.getBlock().getLocation().clone().add(0.5, 0, 0.5), EntityType.TNT);
            return;
        }

        placedBlocks.put(event.getBlock().getLocation(), true);
    }

    final List<Material> grassTypes = Arrays.asList(Material.TALL_GRASS, Material.SHORT_GRASS, Material.CORNFLOWER, Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.ALLIUM, Material.AZURE_BLUET, Material.RED_TULIP, Material.ORANGE_TULIP, Material.WHITE_TULIP, Material.PINK_TULIP, Material.OXEYE_DAISY, Material.SUNFLOWER, Material.LILAC, Material.ROSE_BUSH, Material.PEONY, Material.LILY_OF_THE_VALLEY, Material.WITHER_ROSE, Material.COBWEB, Material.FERN, Material.SWEET_BERRY_BUSH, Material.SNOW, Material.DEAD_TUBE_CORAL, Material.DEAD_FIRE_CORAL);

    @Override
    public void onBreak(BlockBreakEvent event) {
        if (Kingdom.getInstance().getMachineManager().breakLocation(event.getBlock().getLocation())) {
            event.setDropItems(false);
            return;
        }

        // Let grass blocks be removed permanently (for PvP)
        if (grassTypes.contains(event.getBlock().getType())) {
            event.setDropItems(false);
            event.setCancelled(false);
            return;
        }

        // Placed blocks can always be broken again
        if (placedBlocks.get(event.getBlock().getLocation()) != null) {
            placedBlocks.remove(event.getBlock().getLocation());
            return;
        }

        Material type = event.getBlock().getType();
        Team playerTeam = Kingdom.getInstance().getGameManager().getTeamManager().getTeam(event.getPlayer());
        Team castleTeam = CastleRegion.teamAt(event.getBlock().getLocation());

        // Inside castles: only the own team can break blocks, wood & cobble regenerate
        if (castleTeam != null) {
            if (!castleTeam.equals(playerTeam)) {
                event.setCancelled(true);
                return;
            }

            // Ores regenerate everywhere (also inside castles)
            if (ORE_REGEN_SECONDS.containsKey(type)) {
                handleRegenBreak(event, type, ORE_REGEN_SECONDS.get(type));
                return;
            }

            if (isWoodOrCobble(type)) {
                handleRegenBreak(event, type, WOOD_COBBLE_REGEN_SECONDS);
                return;
            }

            event.setCancelled(true);
            return;
        }

        // Ores regenerate everywhere outside castles
        if (ORE_REGEN_SECONDS.containsKey(type)) {
            handleRegenBreak(event, type, ORE_REGEN_SECONDS.get(type));
            return;
        }

        // The rest of the map is fully destructible
        event.setCancelled(false);
    }

    /**
     * Drops direct materials (or the block itself) and turns the block into bedrock for a while.
     */
    private void handleRegenBreak(BlockBreakEvent event, Material type, int regenSeconds) {
        // Cancel the break: the server would otherwise remove the block (set it to air) after this
        // handler returns, overwriting the bedrock we set here.
        event.setCancelled(true);
        event.setDropItems(false);

        // Give the drop straight to the player's inventory (ores drop their direct material,
        // everything else the block itself); overflow drops on the ground
        Material drop = oreDrop(type);
        var leftover = event.getPlayer().getInventory().addItem(new ItemStack(drop != null ? drop : type));
        for (ItemStack item : leftover.values()) {
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation().clone().add(0.5, 0.5, 0.5), item);
        }

        regenerating.put(event.getBlock().getLocation(), new PendingRegen(type, regenSeconds));
        event.getBlock().setType(Material.BEDROCK);
    }

    private Material oreDrop(Material ore) {
        return switch (ore) {
            case COAL_ORE, DEEPSLATE_COAL_ORE -> Material.COAL;
            case COPPER_ORE, DEEPSLATE_COPPER_ORE -> Material.COPPER_INGOT;
            case IRON_ORE, DEEPSLATE_IRON_ORE -> Material.IRON_INGOT;
            case GOLD_ORE, DEEPSLATE_GOLD_ORE -> Material.GOLD_INGOT;
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> Material.DIAMOND;
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> Material.EMERALD;
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> Material.REDSTONE;
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> Material.LAPIS_LAZULI;
            default -> null;
        };
    }

    private boolean isWoodOrCobble(Material material) {
        return material == Material.COBBLESTONE
                || material.name().endsWith("_LOG")
                || material.name().endsWith("_WOOD")
                || material.name().endsWith("_PLANKS");
    }

    private void processRegeneration() {
        final var newMap = new HashMap<Location, PendingRegen>();
        for (var entry : regenerating.entrySet()) {
            if (--entry.getValue().seconds <= 0) {
                // Only regenerate if the block is still bedrock
                if (entry.getKey().getBlock().getType() == Material.BEDROCK) {
                    entry.getKey().getBlock().setType(entry.getValue().material);
                }
            } else {
                newMap.put(entry.getKey(), entry.getValue());
            }
        }
        regenerating.clear();
        regenerating.putAll(newMap);
    }

    /**
     * Lets a team buy an ore in the shop which is then placed at their base.
     * Returns the message for the player.
     */
    public boolean buyOre(Player player, Team team, Material ore) {
        int cost = ore == Material.IRON_ORE ? 10 : 30;

        int coins = ItemShopScreen.countMaterial(player, CURRENCY);
        if (coins < cost) {
            player.sendMessage(Kingdom.PREFIX.append(Component.text("You don't have enough coins!", NamedTextColor.RED)));
            return false;
        }

        int bought = boughtOres.computeIfAbsent(team, t -> new HashMap<>()).getOrDefault(ore, 0);
        if (bought >= MAX_ORES_PER_TEAM) {
            player.sendMessage(Kingdom.PREFIX.append(Component.text("Your team already bought the maximum of " + MAX_ORES_PER_TEAM + " of this ore!", NamedTextColor.RED)));
            return false;
        }

        String shortName = ore == Material.IRON_ORE ? "Iron" : "Diamond";
        Location spot = null;
        for (int i = 1; i <= MAX_ORES_PER_TEAM; i++) {
            Location location = LocationAPI.safe(team.getName() + "-" + shortName + "Ore" + i);
            if (location != null && location.getBlock().getType() == Material.AIR) {
                spot = location;
                break;
            }
        }

        if (spot == null) {
            player.sendMessage(Kingdom.PREFIX.append(Component.text("No free ore spot found at your base!", NamedTextColor.RED)));
            return false;
        }

        ItemShopScreen.removeAmountFromInventory(player, CURRENCY, cost);
        boughtOres.get(team).put(ore, bought + 1);
        spot.getBlock().setType(ore);

        player.sendMessage(Kingdom.PREFIX.append(Component.text("An ", NamedTextColor.GRAY)
                .append(Component.text(shortName + " ore", NamedTextColor.AQUA))
                .append(Component.text(" has been added to your base!", NamedTextColor.GRAY))));
        return true;
    }

    @Override
    public void onDeath(PlayerDeathEvent event) {
        final var player = event.getPlayer();

        event.deathMessage(null);
        event.setKeepInventory(true);
        event.setKeepLevel(true);

        // Drop the flag if the player was carrying one
        Kingdom.getInstance().getFlagManager().dropFlag(player);

        // Give the killer a coin reward
        if (player.getKiller() != null) {
            player.getKiller().getInventory().addItem(new ItemStack(CURRENCY, KILL_REWARD));

            Bukkit.broadcast(Kingdom.PREFIX.append(Component.text(player.getName(), NamedTextColor.AQUA)
                    .append(Component.text(" was killed by ", NamedTextColor.GRAY))
                    .append(Component.text(player.getKiller().getName(), NamedTextColor.AQUA, TextDecoration.BOLD))
                    .append(Component.text("!", NamedTextColor.GRAY))));
        } else {
            Bukkit.broadcast(Kingdom.PREFIX
                    .append(Component.text(player.getName(), NamedTextColor.AQUA, TextDecoration.BOLD))
                    .append(Component.text(" died!", NamedTextColor.GRAY)));
        }

        // Respawn into spectator mode, the respawn event handles the rest
        Kingdom.getInstance().getTaskManager().inject(new Runnable() {
            int tickCount = 0;

            @Override
            public void run() {
                if (tickCount++ >= 1) {
                    if (player.isDead()) {
                        player.spigot().respawn();
                    }
                    Kingdom.getInstance().getTaskManager().uninject(this);
                }
            }
        });
    }

    @Override
    public void onProjectileHit(ProjectileHitEvent event) {

        // Handle the TNT bow
        if (event.getEntity() instanceof Arrow arrow && arrow.hasMetadata("tntbow")) {
            arrow.remove();

            Location location = event.getHitBlock() == null ? event.getHitEntity().getLocation() : event.getHitBlock().getLocation().add(event.getHitBlockFace() == null ? new Vector(0, 0.5, 0) : event.getHitBlockFace().getDirection());
            var tnt = (TNTPrimed) location.getWorld().spawnEntity(location.add(0.5, 0, 0.5), EntityType.TNT);
            tnt.setFuseTicks(0);
        }
    }

    @Override
    public void onRespawn(PlayerRespawnEvent event) {
        final var player = event.getPlayer();
        final var team = Kingdom.getInstance().getGameManager().getTeamManager().getTeam(player);

        // Spectator respawn at the team's respawn location
        Location respawnLocation = LocationAPI.safe(team.getName() + "-Respawn");
        if (respawnLocation == null) {
            respawnLocation = LocationAPI.getLocation(team.getName());
        }
        event.setRespawnLocation(Objects.requireNonNull(respawnLocation));

        // Spectator mode with a countdown, then back into the game
        Kingdom.getInstance().getTaskManager().inject(new Runnable() {
            int seconds = RESPAWN_SECONDS * 20; // Convert to ticks

            @Override
            public void run() {
                if (!player.isOnline()) {
                    Kingdom.getInstance().getTaskManager().uninject(this);
                    return;
                }
                if (seconds == RESPAWN_SECONDS * 20) {
                    player.setGameMode(GameMode.SPECTATOR);
                }

                final double secondsRounded = Math.round((seconds / 20.0) * 10.0) / 10.0;
                player.showTitle(Title.title(Component.empty(), Component.text("Respawning in " + secondsRounded + "s", NamedTextColor.GRAY), Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)));

                if (seconds-- <= 0) {
                    Kingdom.getInstance().getTaskManager().uninject(this);
                    player.setGameMode(GameMode.SURVIVAL);
                    player.getInventory().clear();
                    player.setHealth(20);
                    player.setFoodLevel(20);
                    team.giveKit(player, true);
                    player.clearTitle();
                }
            }
        });
    }

    @Override
    public void quit(Player player) {
        // Drop the flag when the carrier disconnects
        Kingdom.getInstance().getFlagManager().dropFlag(player);

        Team team = Kingdom.getInstance().getGameManager().getTeamManager().getTeam(player);
        team.getPlayers().remove(player);

        // Make sure the team loses if there are no players left
        if (team.getPlayers().isEmpty()) {
            handleWin(Kingdom.getInstance().getGameManager().getTeamManager().getTeams().stream()
                    .filter(team1 -> !team1.equals(team)).findFirst().get());
        }
    }

    public abstract static class DroppableTrap {

        public final Location location;
        public final Team team;
        public final Material material;
        public Item item;
        public long start;

        public DroppableTrap(Location location, Team team, Material material) {
            this.location = location;
            this.team = team;
            this.material = material;
        }

        public void drop() {
            item = (Item) location.getWorld().spawnEntity(location.clone().add(0, 0.5, 0), EntityType.ITEM);
            item.setItemStack(new ItemStackBuilder(material).buildStack());
            item.setVelocity(new Vector(0, 0, 0));
            item.setPickupDelay(1000000000);
            item.setCanPlayerPickup(false);
            item.setCanMobPickup(false);
            item.setUnlimitedLifetime(true);
            start = System.currentTimeMillis();
        }

        public abstract void doEffect(List<LivingEntity> entities);

        public List<Location> blocksToDelete() {
            return List.of();
        }
    }

    public static class SlowTrap extends DroppableTrap {

        SlowTrap(Location location, Team team) {
            super(location, team, Material.GRAY_DYE);
        }

        @Override
        public void doEffect(List<LivingEntity> players) {
            for (var player : players) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 300, 4));
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 300, 0));
            }
        }
    }

    public static class PoisonTrap extends DroppableTrap {

        PoisonTrap(Location location, Team team) {
            super(location, team, Material.LIME_DYE);
        }

        @Override
        public void doEffect(List<LivingEntity> players) {
            for (var player : players) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 300, 0));
            }
        }
    }

    public static class ExplosionTrap extends DroppableTrap {

        ExplosionTrap(Location location, Team team) {
            super(location, team, Material.GUNPOWDER);
        }

        @Override
        public void doEffect(List<LivingEntity> players) {
            location.getWorld().spawnEntity(location.clone().add(-0.5, 1, -0.5), EntityType.TNT);

            for (var player : players) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 300, 0));
            }
        }
    }

    public static class WebTrap extends DroppableTrap {

        WebTrap(Location location, Team team) {
            super(location, team, Material.WHITE_DYE);
        }

        @Override
        public void doEffect(List<LivingEntity> players) {

            // Place 5 blocks of webs around the location
            final var main = location.clone().getBlock();
            main.setType(Material.COBWEB);
            main.getRelative(BlockFace.EAST).setType(Material.COBWEB);
            main.getRelative(BlockFace.WEST).setType(Material.COBWEB);
            main.getRelative(BlockFace.NORTH).setType(Material.COBWEB);
            main.getRelative(BlockFace.SOUTH).setType(Material.COBWEB);

            for (var player : players) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 300, 0));
            }
        }

        @Override
        public List<Location> blocksToDelete() {
            final var main = location.clone().getBlock();

            return List.of(
                    main.getLocation(),
                    main.getRelative(BlockFace.EAST).getLocation(),
                    main.getRelative(BlockFace.WEST).getLocation(),
                    main.getRelative(BlockFace.NORTH).getLocation(),
                    main.getRelative(BlockFace.SOUTH).getLocation()
            );
        }
    }
}
