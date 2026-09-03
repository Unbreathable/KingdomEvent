package com.liphium.kingdom.game.state;

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent;
import com.liphium.core.util.ItemStackBuilder;
import com.liphium.kingdom.Kingdom;
import com.liphium.kingdom.game.GameState;
import com.liphium.kingdom.game.team.Team;
import com.liphium.kingdom.listener.machines.impl.DestroyableSnowman;
import com.liphium.kingdom.util.LocationAPI;
import com.liphium.kingdom.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class IngameState extends GameState {

    private final ArrayList<DroppableTrap> traps = new ArrayList<>();

    private Runnable runnable;

    private static final double SNOWMAN_HEALTH = 300;
    private static final int ICE_ON_DEATH = 2; // The amount of ice a player gets when they kill someone

    private static final int SNOWBALL_TICKS = 1;
    private static final int SNOWBALL_COOLDOWN = 6; // Cooldown that is set when players use a snowball
    private static final double SNOWBALL_DAMAGE = 2.5;

    private static final int ARROW_COOLDOWN = 70;

    public IngameState() {
        super("In game", 30);
    }

    private final HashMap<String, DestroyableSnowman> snowmen = new HashMap<>();
    private final HashMap<Location, Boolean> placedBlocks = new HashMap<>();
    private final HashMap<Player, String> currentArrowEffect = new HashMap<>();
    private HashMap<Location, Integer> toDeleteAfter = new HashMap<>();

    @Override
    public void start() {

        final var world = Bukkit.getWorld(Kingdom.GAME_WORLD);
        assert(world != null);
        world.setGameRule(GameRules.ADVANCE_TIME, true);
        world.setGameRule(GameRules.ADVANCE_WEATHER, true);
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
            team.join(player);
        }

        // World cleanup
        for (Entity entity : Objects.requireNonNull(Bukkit.getWorld(Kingdom.GAME_WORLD)).getEntities()) {
            if (entity.getType() != EntityType.ARMOR_STAND && entity.getType() != EntityType.PLAYER) entity.remove();
        }

        // Place all the snowmen
        for(Team team : Kingdom.getInstance().getGameManager().getTeamManager().getTeams()) {
            final var location = LocationAPI.getLocation(team.getName() + "-Snowman");
            final var man = new DestroyableSnowman(location, team, SNOWMAN_HEALTH);

            Kingdom.getInstance().getMachineManager().addMachine(man);
            snowmen.put(team.getName(), man);
        }

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
            int snowballCount = 0;

            @Override
            public void run() {
                Kingdom.getInstance().getGameManager().getTeamManager().tick();
                Kingdom.getInstance().getMachineManager().tick();

                if (tickCount++ >= 20) {
                    tickCount = 0;

                    // Create an action bar with a health bar for the snowmen of all teams
                    var base = Component.text("");
                    var index = 0;
                    for(Team team : Kingdom.getInstance().getGameManager().getTeamManager().getTeams()) {
                        base = base.append(snowmen.get(team.getName()).colorWithHealth("■■■■■■■■", team.getColor(), NamedTextColor.GRAY));
                        if(index != Kingdom.getInstance().getGameManager().getTeamManager().getTeams().size() - 1) {
                            base = base.appendSpace().append(Component.text("|", NamedTextColor.DARK_GRAY)).appendSpace();
                        }
                        index++;
                    }

                    // Let a team win the game when the snowman is down
                    for(Team team : Kingdom.getInstance().getGameManager().getTeamManager().getTeams()) {
                        final var man = snowmen.get(team.getName());

                        if(man.man.isDead()) {
                            final var other = Kingdom.getInstance().getGameManager().getTeamManager().getTeams().stream()
                                    .filter(t -> !t.getName().equals(team.getName())).findFirst().get();
                            handleWin(other);
                        }
                    }

                    Messages.actionBar(base);
                }

                // Give players snowballs every few ticks
                if(snowballCount++ >= SNOWBALL_TICKS) {
                    snowballCount = 0;

                    for(Team team : Kingdom.getInstance().getGameManager().getTeamManager().getTeams()) {
                        for(Player player : team.getPlayers()) {
                            player.getInventory().setItemInOffHand(new ItemStackBuilder(Material.SNOWBALL).withAmount(16).buildStack());
                        }
                    }
                }

                // Delete all blocks that should be deleted
                final var newMap = new HashMap<Location, Integer>();
                for(var entry : toDeleteAfter.entrySet()) {
                    final var newValue = entry.getValue() - 1;
                    if(newValue <= 0) {
                        entry.getKey().getBlock().setType(Material.AIR);
                    } else {
                        newMap.put(entry.getKey(), newValue);
                    }
                }
                toDeleteAfter = newMap;
            }
        });
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (event.getItem() != null && (event.getItem().getType() == Material.WIND_CHARGE || event.getItem().getType() == Material.SNOWBALL)) {
            return;
        }

        Kingdom.getInstance().getMachineManager().onInteract(event);

        if (event.getItem() != null) {
            Team team = Kingdom.getInstance().getGameManager().getTeamManager().getTeam(event.getPlayer());
            ItemStack usedItem = event.getItem();

            var hit = false;
            DroppableTrap trapToPlace = null;
            switch(usedItem.getType()) {
                case Material.GRAY_DYE -> {
                    if(event.getClickedBlock() == null && currentArrowEffect.containsKey(event.getPlayer())) {
                        event.getPlayer().sendMessage(Kingdom.PREFIX.append(Component.text("You already have an arrow effect equipped.", NamedTextColor.RED)));
                        return;
                    }

                    hit = true;
                    reduceMainHandItem(event.getPlayer(), Material.GRAY_DYE);
                    if(event.getClickedBlock() == null) {
                        currentArrowEffect.put(event.getPlayer(), "slowness");
                    } else {
                        trapToPlace = new SlowTrap(event.getClickedBlock().getLocation().clone().add(0.5, 1, 0.5), team);
                    }
                }
                case Material.LIME_DYE -> {
                    if(event.getClickedBlock() == null && currentArrowEffect.containsKey(event.getPlayer())) {
                        event.getPlayer().sendMessage(Kingdom.PREFIX.append(Component.text("You already have an arrow effect equipped.", NamedTextColor.RED)));
                        return;
                    }

                    hit = true;
                    reduceMainHandItem(event.getPlayer(), Material.LIME_DYE);
                    if(event.getClickedBlock() == null) {
                        currentArrowEffect.put(event.getPlayer(), "poison");
                    } else {
                        trapToPlace = new PoisonTrap(event.getClickedBlock().getLocation().clone().add(0.5, 1, 0.5), team);
                    }
                }
                case Material.GUNPOWDER -> {
                    if(event.getClickedBlock() == null && currentArrowEffect.containsKey(event.getPlayer())) {
                        event.getPlayer().sendMessage(Kingdom.PREFIX.append(Component.text("You already have an arrow effect equipped.", NamedTextColor.RED)));
                        return;
                    }

                    hit = true;
                    reduceMainHandItem(event.getPlayer(), Material.GUNPOWDER);
                    if(event.getClickedBlock() == null) {
                        currentArrowEffect.put(event.getPlayer(), "explosion");
                    } else {
                        trapToPlace = new ExplosionTrap(event.getClickedBlock().getLocation().clone().add(0.5, 1, 0.5), team);
                    }
                }
                case Material.WHITE_DYE -> {
                    if(event.getClickedBlock() == null && currentArrowEffect.containsKey(event.getPlayer())) {
                        event.getPlayer().sendMessage(Kingdom.PREFIX.append(Component.text("You already have an arrow effect equipped.", NamedTextColor.RED)));
                        return;
                    }

                    hit = true;
                    reduceMainHandItem(event.getPlayer(), Material.WHITE_DYE);
                    if(event.getClickedBlock() == null) {
                        currentArrowEffect.put(event.getPlayer(), "web");
                    } else {
                        trapToPlace = new WebTrap(event.getClickedBlock().getLocation().clone().add(0.5, 1, 0.5), team);
                    }
                }
            }

            if(trapToPlace != null) {
                traps.add(trapToPlace);
                trapToPlace.drop();
            }

            // Send a message
            if(hit) {
                if(trapToPlace != null) {
                    event.getPlayer().sendMessage(Kingdom.PREFIX
                            .append(Component.text("Trap placed!", NamedTextColor.GRAY)));
                } else {
                    event.getPlayer().sendMessage(Kingdom.PREFIX
                            .append(Component.text("Arrow effect attached to next arrow!", NamedTextColor.GRAY)));
                }
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
                for(final var toTrace : toRaytrace) {
                    final var direction = toTrace.clone().subtract(trap.location).toVector().normalize();
                    final var distance = trap.location.distance(toTrace);
                    final var result = trap.location.getWorld().rayTraceBlocks(trap.location, direction, distance, FluidCollisionMode.NEVER, true);

                    if (result == null) {
                        found = true;
                        break;
                    }
                }

                if(found) {
                    toRemove.add(trap);
                    trap.doEffect(List.of(event.getPlayer()));
                    for(var loc : trap.blocksToDelete()) {
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
        if (event.getEntity().getType() == EntityType.ITEM) {
            event.setCancelled(true);
        } else {
            event.setCancelled(false);
        }
    }

    @Override
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity().getType() == EntityType.ARMOR_STAND || event.getEntity().getType() == EntityType.ITEM) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onKnockbackByEntity(EntityKnockbackByEntityEvent event) {
        if(event.getEntity().getType() == EntityType.ITEM) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if(event.getEntity().getShooter() == null || !(event.getEntity().getShooter() instanceof Player player)) return;

        event.getEntity().setMetadata("team", new FixedMetadataValue(Kingdom.getInstance(), Kingdom.getInstance().getGameManager().getTeamManager().getTeam(player).getName()));

        // Handle the snowball and arrow cooldowns
        if(event.getEntity().getType() == EntityType.SNOWBALL) {
            player.setCooldown(Material.SNOWBALL, SNOWBALL_COOLDOWN);
        } else if(event.getEntity().getType() == EntityType.ARROW) {
            player.setCooldown(Material.CROSSBOW, ARROW_COOLDOWN);
            player.setCooldown(Material.BOW, ARROW_COOLDOWN);

            if(currentArrowEffect.containsKey(player)) {
                final var effect = currentArrowEffect.get(player);
                event.getEntity().setMetadata("effect", new FixedMetadataValue(Kingdom.getInstance(), effect));
                currentArrowEffect.remove(player);
            }
        }
    }

    @Override
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> {
            if(placedBlocks.containsKey(block.getLocation())) {
                placedBlocks.remove(block.getLocation());
                return false;
            }

            return true;
        });
    }

    @Override
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType().equals(Material.REDSTONE_TORCH)) {
            event.setCancelled(true);
            return;
        }

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
        if(event.getBlock().getType() == Material.TNT) {
            event.getBlock().setType(Material.AIR);
            final var world = event.getBlock().getWorld();
            world.spawnEntity(event.getBlock().getLocation(), EntityType.TNT);
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

        // Only let placed blocks be broken again
        if (placedBlocks.get(event.getBlock().getLocation()) != null) {
            placedBlocks.remove(event.getBlock().getLocation());
            return;
        }

        // Let grass blocks be removed permanently (for PvP)
        if (grassTypes.contains(event.getBlock().getType())) {
            event.setDropItems(false);
            event.setCancelled(false);
            return;
        }

        event.setCancelled(true);
    }

    @Override
    public void onDeath(PlayerDeathEvent event) {
        final var player = event.getPlayer();

        event.deathMessage(null);
        event.setKeepInventory(true);
        event.setKeepLevel(true);

        if (player.getKiller() != null) {

            /*
            // Give blue ice to the other person
            final int blueIce = ItemShopScreen.countMaterial(event.getPlayer(), Material.BLUE_ICE);
            player.getInventory().remove(Material.BLUE_ICE);
            player.getKiller().give(new ItemStack(Material.BLUE_ICE, blueIce));
             */

            Bukkit.broadcast(Kingdom.PREFIX.append(Component.text(player.getName(), NamedTextColor.AQUA)
                    .append(Component.text(" was killed by ", NamedTextColor.GRAY))
                    .append(Component.text(player.getKiller().getName(), NamedTextColor.AQUA, TextDecoration.BOLD))
                    .append(Component.text("!", NamedTextColor.GRAY))));
        } else {
            Bukkit.broadcast(Kingdom.PREFIX
                    .append(Component.text(player.getName(), NamedTextColor.AQUA, TextDecoration.BOLD))
                    .append(Component.text(" died!", NamedTextColor.GRAY)));
        }

        Kingdom.getInstance().getTaskManager().inject(new Runnable() {
            int tickCount = 0;

            @Override
            public void run() {
                if (tickCount++ >= 1) {
                    if (player.isDead()) {
                        player.spigot().respawn();
                        player.setHealth(20);
                    }
                    Kingdom.getInstance().getTaskManager().uninject(this);
                }
            }
        });
    }

    @Override
    public void onProjectileHit(ProjectileHitEvent event) {

        // Handle effect arrows
        if(event.getEntity() instanceof Arrow arrow && arrow.hasMetadata("effect")) {
            final var effect = arrow.getMetadata("effect").getFirst().asString();
            final var location = event.getHitBlock() == null ? event.getHitEntity().getLocation() : event.getHitBlock().getLocation();
            final var team = Kingdom.getInstance().getGameManager().getTeamManager().getTeam(arrow.getMetadata("team").getFirst().asString());
            arrow.removeMetadata("effect", Kingdom.getInstance());

            // Choose the correct trap for the arrow effect
            DroppableTrap trap = null;
            switch(effect) {
                case "web" -> {
                    trap = new WebTrap(location, team);
                }
                case "slowness" -> {
                    trap = new SlowTrap(location, team);
                }
                case "explosion" -> {
                    trap = new ExplosionTrap(location, team);
                }
                case "poison" -> {
                    trap = new PoisonTrap(location, team);
                }
            }
            assert(trap != null);

            // Do the arrow effect
            final var players = new ArrayList<LivingEntity>();
            for(Entity entity : location.getNearbyEntities(3, 3, 3)) {
                if(entity instanceof LivingEntity living) {
                    if(entity instanceof Player player && trap.team.getPlayers().contains(player)) {
                        continue;
                    }

                    players.add(living);
                }
            }

            trap.doEffect(players);
            for(var loc : trap.blocksToDelete()) {
                toDeleteAfter.put(loc, 120);
            }
            return;
        }

        // Snowball handling
        if(!(event.getEntity().getType() == EntityType.SNOWBALL)) return;
        if(event.getHitEntity() instanceof LivingEntity target) {
            if(event.getEntity().hasMetadata("team") && target instanceof Player player) {
                final var team = Kingdom.getInstance().getGameManager().getTeamManager().getTeam(event.getEntity().getMetadata("team").getFirst().asString());

                if(Kingdom.getInstance().getGameManager().getTeamManager().getTeam(player).getName().equals(team.getName())) {
                    return;
                }
            }

            // Make sure the player gets kill credit
            if(event.getEntity().getShooter() instanceof Player shooter) {
                target.damage(SNOWBALL_DAMAGE, shooter);
            } else {
                target.damage(SNOWBALL_DAMAGE);
            }

            // Apply knockback similar to vanilla
            if(target instanceof Player && !(event.getEntity().getShooter() instanceof Player)) {
                Vector knockback = event.getEntity().getVelocity().normalize().multiply(0.5);
                knockback.setY(0.4);
                target.setVelocity(target.getVelocity().add(knockback));
            }
        }
    }

    @Override
    public void onRespawn(PlayerRespawnEvent event) {
        final var team = Kingdom.getInstance().getGameManager().getTeamManager().getTeam(event.getPlayer());
        event.setRespawnLocation(Objects.requireNonNull(LocationAPI.getLocation(team.getName())));
    }

    public void handleWin(Team team) {
        team.handleWin();
        Kingdom.getInstance().getTaskManager().uninject(runnable);
        Kingdom.getInstance().getGameManager().setCurrentState(new EndState());
    }

    @Override
    public void quit(Player player) {
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
            for(var player : players) {
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
            for(var player : players) {
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

            for(var player : players) {
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

            for(var player : players) {
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
