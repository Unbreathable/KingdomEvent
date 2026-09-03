package com.liphium.snowsplash.listener;

import io.papermc.paper.event.entity.EntityKnockbackByEntityEvent;
import com.liphium.snowsplash.Snowsplash;
import com.liphium.snowsplash.game.state.LobbyState;
import org.bukkit.block.data.type.Snow;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;

public class GameListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Snowsplash.getInstance().getGameManager().getCurrentState().onInteract(event);
    }

    @EventHandler
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Snowsplash.getInstance().getGameManager().getCurrentState().onInteractAtEntity(event);
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        Snowsplash.getInstance().getGameManager().getCurrentState().onEntityExplode(event);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Snowsplash.getInstance().getGameManager().getCurrentState().onBreak(event);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Snowsplash.getInstance().getGameManager().getCurrentState().onPlace(event);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Snowsplash.getInstance().getGameManager().getCurrentState().onMove(event);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        Snowsplash.getInstance().getGameManager().getCurrentState().onDamage(event);
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        Snowsplash.getInstance().getGameManager().getCurrentState().onDamageByEntity(event);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Snowsplash.getInstance().getGameManager().getCurrentState().onDeath(event);
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        Snowsplash.getInstance().getGameManager().getCurrentState().onDrop(event);
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event.getEntityType().equals(EntityType.ITEM) || event.getEntityType().equals(EntityType.FIREWORK_ROCKET) || event.getEntityType().equals(EntityType.ARMOR_STAND) || event.getEntityType().equals(EntityType.LINGERING_POTION) || event.getEntityType().equals(EntityType.SPLASH_POTION) || event.getEntityType().equals(EntityType.AREA_EFFECT_CLOUD) || event.getEntityType().equals(EntityType.WIND_CHARGE) || event.getEntityType().equals(EntityType.BREEZE_WIND_CHARGE) || event.getEntityType().equals(EntityType.TNT) || event.getEntityType().equals(EntityType.ARROW) || event.getEntityType().equals(EntityType.SNOW_GOLEM) || event.getEntityType().equals(EntityType.SNOWBALL)) {
            Snowsplash.getInstance().getGameManager().getCurrentState().onSpawn(event);
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onRocket(FireworkExplodeEvent event) {
        Snowsplash.getInstance().getGameManager().getCurrentState().onFirework(event);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Snowsplash.getInstance().getGameManager().getCurrentState().onRespawn(event);
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent event) {
        if (Snowsplash.getInstance().getGameManager().getCurrentState() instanceof LobbyState) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Snowsplash.getInstance().getGameManager().getCurrentState().onProjectileHit(event);
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Snowsplash.getInstance().getGameManager().getCurrentState().onProjectileLaunch(event);
    }

    @EventHandler
    public void onKnockbackByEntity(EntityKnockbackByEntityEvent event) {
        Snowsplash.getInstance().getGameManager().getCurrentState().onKnockbackByEntity(event);
    }
}
