package com.liphium.kingdom.game;

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;

public abstract class GameState {

    private final String name;
    public int count;
    public boolean paused = false;

    public GameState(String name, int count) {
        this.name = name;
        this.count = count;
    }

    public abstract void start();

    public String getName() {
        return name;
    }

    public void onInteract(PlayerInteractEvent event) {
    }

    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
    }

    public void onEntityExplode(EntityExplodeEvent event) {
    }

    public void onMove(PlayerMoveEvent event) {
    }

    public void onDeath(PlayerDeathEvent event) {
    }

    public void onDamage(EntityDamageEvent event) {
    }

    public void onDamageByEntity(EntityDamageByEntityEvent event) {
    }

    public void onKnockbackByEntity(EntityKnockbackByEntityEvent event) {
    }

    public void onPlace(BlockPlaceEvent event) {
    }

    public void onDrop(PlayerDropItemEvent event) {
    }

    public void onBreak(BlockBreakEvent event) {
    }

    public void onSpawn(EntitySpawnEvent event) {
    }

    public void onFirework(FireworkExplodeEvent event) {
    }

    public void onRespawn(PlayerRespawnEvent event) {
    }

    public void onProjectileHit(ProjectileHitEvent event) {
    }

    public void onProjectileLaunch(ProjectileLaunchEvent event) {
    }

    public void join(Player player) {
    }

    public void quit(Player player) {
    }

}
