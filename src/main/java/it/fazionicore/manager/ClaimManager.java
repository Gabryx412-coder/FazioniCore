package it.fazionicore.manager;

import it.fazionicore.FazioniCore;
import it.fazionicore.model.ClaimData;
import it.fazionicore.model.Faction;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClaimManager {

    private final FazioniCore plugin;
    private final Map<String, ClaimData> claims = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> factionClaims = new ConcurrentHashMap<>();

    public ClaimManager(FazioniCore plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        File file = new File(plugin.getDataFolder(), "data/claims/claims.yml");
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        var section = yaml.getConfigurationSection("claims");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            String faction = section.getString(key + ".faction");
            String world = section.getString(key + ".world");
            int cx = section.getInt(key + ".chunkX");
            int cz = section.getInt(key + ".chunkZ");
            if (faction == null || world == null) continue;
            ClaimData cd = new ClaimData(world, cx, cz, faction);
            claims.put(cd.getKey(), cd);
            factionClaims.computeIfAbsent(faction.toLowerCase(), k -> ConcurrentHashMap.newKeySet()).add(cd.getKey());
        }
        plugin.getLogger().info("Caricati " + claims.size() + " claim.");
    }

    public void saveAll() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::saveSync);
    }

    private void saveSync() {
        File file = new File(plugin.getDataFolder(), "data/claims/claims.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        int i = 0;
        for (ClaimData cd : claims.values()) {
            String path = "claims." + i;
            yaml.set(path + ".faction", cd.getFactionName());
            yaml.set(path + ".world", cd.getWorld());
            yaml.set(path + ".chunkX", cd.getChunkX());
            yaml.set(path + ".chunkZ", cd.getChunkZ());
            i++;
        }
        try { yaml.save(file); } catch (Exception e) {
            plugin.getLogger().warning("Errore salvataggio claims.");
        }
    }

    public boolean claim(Faction faction, Chunk chunk) {
        String key = ClaimData.key(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        if (claims.containsKey(key)) return false;
        int currentClaims = getClaimCount(faction.getName());
        int limit = plugin.getFactionManager().getClaimLimit(faction);
        if (currentClaims >= limit) return false;
        if (hasEnemyClaimNearby(chunk, faction.getName())) {
            return false;
        }
        ClaimData cd = new ClaimData(chunk.getWorld().getName(), chunk.getX(), chunk.getZ(), faction.getName());
        claims.put(key, cd);
        factionClaims.computeIfAbsent(faction.getName().toLowerCase(), k -> ConcurrentHashMap.newKeySet()).add(key);
        faction.incrementClaimsEver();
        plugin.getFactionManager().recalcLevel(faction);
        saveAll();
        return true;
    }

    public boolean unclaim(Faction faction, Chunk chunk) {
        String key = ClaimData.key(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        ClaimData cd = claims.get(key);
        if (cd == null || !cd.getFactionName().equalsIgnoreCase(faction.getName())) return false;
        claims.remove(key);
        Set<String> fClaims = factionClaims.get(faction.getName().toLowerCase());
        if (fClaims != null) fClaims.remove(key);
        saveAll();
        return true;
    }

    public void unclaimAll(String factionName) {
        Set<String> keys = factionClaims.remove(factionName.toLowerCase());
        if (keys != null) keys.forEach(claims::remove);
        saveAll();
    }

    public ClaimData getClaimAt(Location loc) {
        if (loc.getWorld() == null) return null;
        Chunk chunk = loc.getChunk();
        return claims.get(ClaimData.key(loc.getWorld().getName(), chunk.getX(), chunk.getZ()));
    }

    public ClaimData getClaimAt(Chunk chunk) {
        return claims.get(ClaimData.key(chunk.getWorld().getName(), chunk.getX(), chunk.getZ()));
    }

    public int getClaimCount(String factionName) {
        Set<String> set = factionClaims.get(factionName.toLowerCase());
        return set == null ? 0 : set.size();
    }

    public boolean isClaimed(Location loc) { return getClaimAt(loc) != null; }

    public Set<String> getFactionClaimKeys(String factionName) {
        return factionClaims.getOrDefault(factionName.toLowerCase(), Collections.emptySet());
    }

    private boolean hasEnemyClaimNearby(Chunk chunk, String factionName) {
        String world = chunk.getWorld().getName();
        int cx = chunk.getX();
        int cz = chunk.getZ();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;

                String key = ClaimData.key(world, cx + dx, cz + dz);
                ClaimData adjacent = claims.get(key);

                if (adjacent != null && !adjacent.getFactionName().equalsIgnoreCase(factionName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
