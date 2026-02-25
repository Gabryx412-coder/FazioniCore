package it.fazionicore.model;

import java.util.Objects;

public class ClaimData {

    private final String world;
    private final int chunkX;
    private final int chunkZ;
    private String factionName;

    public ClaimData(String world, int chunkX, int chunkZ, String factionName) {
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.factionName = factionName;
    }

    public static String key(String world, int cx, int cz) {
        return world + ";" + cx + ";" + cz;
    }

    public String getKey() { return key(world, chunkX, chunkZ); }

    public String getWorld() { return world; }
    public int getChunkX() { return chunkX; }
    public int getChunkZ() { return chunkZ; }
    public String getFactionName() { return factionName; }
    public void setFactionName(String factionName) { this.factionName = factionName; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClaimData)) return false;
        ClaimData c = (ClaimData) o;
        return chunkX == c.chunkX && chunkZ == c.chunkZ && Objects.equals(world, c.world);
    }

    @Override
    public int hashCode() { return Objects.hash(world, chunkX, chunkZ); }
}
