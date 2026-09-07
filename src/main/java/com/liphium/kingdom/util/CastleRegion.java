package com.liphium.kingdom.util;

import com.liphium.kingdom.Kingdom;
import com.liphium.kingdom.game.team.Team;
import org.bukkit.Location;

/**
 * An axis aligned region defined by two corners. Ignoring Y coordinates.
 */
public class CastleRegion {

    private final int minX, minY, minZ, maxX, maxY, maxZ;

    public CastleRegion(Location corner1, Location corner2) {
        this.minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        this.minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
        this.minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        this.maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        this.maxY = Math.max(corner1.getBlockY(), corner2.getBlockY());
        this.maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());
    }

    public boolean contains(Location location) {
        final int x = location.getBlockX();
        final int z = location.getBlockZ();
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    /**
     * Creates the castle region for a team from the "X-Castle1" and "X-Castle2" locations.
     * Returns null if the locations have not been set.
     */
    public static CastleRegion forTeam(Team team) {
        if (!LocationAPI.exists(team.getName() + "-Castle1") || !LocationAPI.exists(team.getName() + "-Castle2")) {
            return null;
        }

        return new CastleRegion(LocationAPI.getLocation(team.getName() + "-Castle1"), LocationAPI.getLocation(team.getName() + "-Castle2"));
    }

    /**
     * Returns the team whose castle contains the location, or null if it's in no castle.
     */
    public static Team teamAt(Location location) {
        for (Team team : Kingdom.getInstance().getGameManager().getTeamManager().getTeams()) {
            CastleRegion region = CastleRegion.forTeam(team);
            if (region != null && region.contains(location)) {
                return team;
            }
        }

        return null;
    }
}
