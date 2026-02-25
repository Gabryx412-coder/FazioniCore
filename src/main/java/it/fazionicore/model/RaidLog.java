package it.fazionicore.model;

public class RaidLog {

    private final String factionName;
    private final String playerName;
    private final String playerUUID;
    private final String blockType;
    private final String world;
    private final int x, y, z;
    private final String action;
    private final long timestamp;

    public RaidLog(String factionName, String playerName, String playerUUID,
                   String blockType, String world, int x, int y, int z, String action) {
        this.factionName = factionName;
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.blockType = blockType;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.action = action;
        this.timestamp = System.currentTimeMillis();
    }

    public String toLogLine() {
        return String.format("[%s] %s (%s) ha %s %s in %s [%d,%d,%d] nella fazione %s",
                timestamp, playerName, playerUUID, action, blockType, world, x, y, z, factionName);
    }

    public String getFactionName() { return factionName; }
    public String getPlayerName() { return playerName; }
    public String getPlayerUUID() { return playerUUID; }
    public String getBlockType() { return blockType; }
    public String getWorld() { return world; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public String getAction() { return action; }
    public long getTimestamp() { return timestamp; }
}
