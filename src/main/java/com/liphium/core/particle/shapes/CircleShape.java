package com.liphium.core.particle.shapes;

import com.liphium.core.particle.ParticleBuilder;
import com.liphium.core.particle.ParticleShape;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public record CircleShape(int radius) implements ParticleShape {

    @Override
    public void renderShape(Player player, Location[] locations, ParticleBuilder builder) {

        // Center location
        Location center = locations[0];

        for (int i = 0; i <= 45; i += 1) {
            Location particleLoc = center.clone();
            particleLoc.setX(center.getX() + Math.cos(i * 2) * radius);
            particleLoc.setZ(center.getZ() + Math.sin(i * 2) * radius);
            builder.getRenderer().renderParticle(player, particleLoc);
        }
    }
}
