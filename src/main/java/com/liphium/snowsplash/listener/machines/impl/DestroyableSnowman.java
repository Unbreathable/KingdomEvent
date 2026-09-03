package com.liphium.snowsplash.listener.machines.impl;

import com.liphium.snowsplash.Snowsplash;
import com.liphium.snowsplash.game.team.Team;
import com.liphium.snowsplash.listener.machines.Machine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Snowman;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class DestroyableSnowman extends Machine {

	private final Team own;
    public final Snowman man;
    private int shootCooldown = 0;
    private static final int SHOOT_INTERVAL = 24;
    private static final double TARGET_RANGE = 20.0;

    public DestroyableSnowman(Location location, Team own, double health) {
        super(location, false);
        this.own = own;

        man = location.getWorld().spawn(location.clone(), Snowman.class);
        man.setAI(false);
        man.setGravity(false);

        // Set max health
        man.registerAttribute(Attribute.MAX_HEALTH);
        Objects.requireNonNull(man.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(health);
        man.setHealth(health);

        man.setCustomNameVisible(true);
        man.customName(getColoredHealthBar());
    }

    @Override
    public void tick() {
        man.setCustomNameVisible(true);
        man.customName(getColoredHealthBar());

        // Find and update target every tick
        final var currentTarget = findBestTarget();
        
        // Make the snowman look at the target
        if (currentTarget != null && currentTarget.isOnline() && !currentTarget.isDead()) {
            Location targetLoc = currentTarget.getLocation();

            Vector direction = targetLoc.toVector().subtract(location.toVector()).normalize();
            Location lookAt = location.clone().setDirection(direction);
            
            man.teleport(lookAt);
        }
        
        // Shoot every 12 ticks if we have a valid target
        shootCooldown++;
        if (shootCooldown >= SHOOT_INTERVAL) {
            shootCooldown = 0;
            if (currentTarget != null && currentTarget.isOnline() && !currentTarget.isDead()) {
                shootSnowball(currentTarget);
            }
        }
    }
    
    /**
     * Finds the best target based on health priority, team checking, range, and line of sight
     */
    private Player findBestTarget() {
        List<Player> validTargets = new ArrayList<>();
        
        // Get all online players
        for (Player player : man.getWorld().getPlayers()) {
            // Skip if player is dead or not online
            if (!player.isOnline() || player.isDead()) {
                continue;
            }
            
            // Check if player is in the snowman's team
            if (isPlayerInTeam(player)) {
                continue;
            }
            
            // Check if player is within range
            if (player.getLocation().distance(man.getLocation()) > TARGET_RANGE) {
                continue;
            }
            
            // Check line of sight with raycast
            if (!hasLineOfSight(player)) {
                continue;
            }
            
            validTargets.add(player);
        }
        
        // Sort by health (lowest first)
        if (!validTargets.isEmpty()) {
            validTargets.sort(Comparator.comparingDouble(Player::getHealth));
            return validTargets.get(0);
        }
        
        return null;
    }
    
    /**
     * Checks if the player is in the snowman's team
     */
    private boolean isPlayerInTeam(Player player) {
        if (own == null) {
            return false;
        }
        return own.getPlayers().contains(player);
    }
    
    /**
     * Checks if the snowman has line of sight to the player using raycast
     */
    private boolean hasLineOfSight(Player player) {
        Location snowmanEye = man.getLocation().clone().add(0, 1.5, 0);
        Location playerEye = player.getEyeLocation();
        
        Vector direction = playerEye.toVector().subtract(snowmanEye.toVector());
        double distance = direction.length();
        
        // Perform raycast
        RayTraceResult result = man.getWorld().rayTraceBlocks(
            snowmanEye,
            direction.normalize(),
            distance,
            org.bukkit.FluidCollisionMode.NEVER,
            true
        );
        
        // If no block was hit, we have line of sight
        return result == null || result.getHitBlock() == null;
    }
    
    /**
     * Shoots a snowball at the target
     */
    private void shootSnowball(Player target) {
        Location shootFrom = man.getLocation().clone().add(0, 1.5, 0);
        Vector direction = target.getEyeLocation().toVector()
            .subtract(shootFrom.toVector())
            .normalize();
        
        Snowball snowball = man.launchProjectile(Snowball.class, direction.multiply(1.5));

        snowball.setMetadata("team", new FixedMetadataValue(Snowsplash.getInstance(), own.getName()));

        snowball.setShooter(man);
    }

    public Component colorWithHealth(String text, NamedTextColor color, NamedTextColor background) {
        final double maxHealth = Objects.requireNonNull(man.getAttribute(Attribute.MAX_HEALTH)).getBaseValue();
        final double progress = man.getHealth() / maxHealth;

        // Color the string so it's green after a certain index
        final var splitPoint = (int) (text.length() * progress);
        return Component.text(text.substring(0, splitPoint), color)
                .append(Component.text(text.substring(splitPoint), background));
    }

    private Component getColoredHealthBar() {
        return colorWithHealth(getHealthBar(), NamedTextColor.GREEN, NamedTextColor.GRAY);
    }

    private String getHealthBar() {
        return String.format("■■■■■ %.0f/%.0f ■■■■■", man.getHealth(), Objects.requireNonNull(man.getAttribute(Attribute.MAX_HEALTH)).getBaseValue());
    }

    @Override
    public void destroy() {
        man.remove();
    }
}
