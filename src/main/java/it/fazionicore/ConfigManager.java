package it.fazionicore;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final FazioniCore plugin;
    private FileConfiguration cfg;

    public ConfigManager(FazioniCore plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.cfg = plugin.getConfig();
    }

    public boolean isDatabaseEnabled() { return cfg.getBoolean("database.enabled", false); }
    public String getDbHost() { return cfg.getString("database.host", "localhost"); }
    public int getDbPort() { return cfg.getInt("database.port", 3306); }
    public String getDbName() { return cfg.getString("database.name", "fazionicore"); }
    public String getDbUser() { return cfg.getString("database.username", "root"); }
    public String getDbPass() { return cfg.getString("database.password", "password"); }
    public int getDbPoolSize() { return cfg.getInt("database.pool-size", 10); }

    public double getFactionCreationCost() { return cfg.getDouble("faction.creation-cost", 500.0); }
    public int getMaxNameLength() { return cfg.getInt("faction.max-name-length", 16); }
    public int getMinNameLength() { return cfg.getInt("faction.min-name-length", 3); }
    public int getMaxTagLength() { return cfg.getInt("faction.max-tag-length", 5); }
    public int getMinTagLength() { return cfg.getInt("faction.min-tag-length", 2); }
    public int getMaxDescLength() { return cfg.getInt("faction.max-description-length", 100); }
    public boolean getDefaultFriendlyFire() { return cfg.getBoolean("faction.friendly-fire-default", false); }
    public boolean getDefaultOpen() { return cfg.getBoolean("faction.open-default", false); }

    public double getClaimCost() { return cfg.getDouble("claim.cost", 100.0); }
    public List<String> getClaimEnabledWorlds() { return cfg.getStringList("claim.enabled-worlds"); }
    public boolean isWildernessAllowed() { return cfg.getBoolean("claim.allow-wilderness-build", true); }

    public boolean isBufferZoneEnabled() { 
        return cfg.getBoolean("claim.buffer-zone.enabled", false); 
    }

    public int getBufferZoneRadius() { 
        return cfg.getInt("claim.buffer-zone.radius", 1); 
    }

    public boolean isClaimToolEnabled() { 
        return cfg.getBoolean("claim.tool.enabled", true); 
    }

    public boolean isClaimVisualizationEnabled() { 
        return cfg.getBoolean("claim.visualization.enabled", true); 
    }

    public int getMaxMapRadius() { 
        return cfg.getInt("claim.map.max-radius", 10); 
    }

    public Map<Integer, Integer> getClaimLimits() {
        Map<Integer, Integer> map = new HashMap<>();
        var section = cfg.getConfigurationSection("claim-limits");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try { map.put(Integer.parseInt(key), section.getInt(key)); } catch (NumberFormatException ignored) {}
            }
        }
        if (map.isEmpty()) {
            map.put(1, 10); map.put(2, 20); map.put(3, 35); map.put(4, 55); map.put(5, 80);
        }
        return map;
    }

    public Map<Integer, Integer> getLevelThresholds() {
        Map<Integer, Integer> map = new HashMap<>();
        var section = cfg.getConfigurationSection("levels.thresholds");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try { map.put(Integer.parseInt(key), section.getInt(key)); } catch (NumberFormatException ignored) {}
            }
        }
        if (map.isEmpty()) {
            map.put(2,15); map.put(3,35); map.put(4,65); map.put(5,100);
        }
        return map;
    }

    public double getMemberMultiplier() { return cfg.getDouble("levels.member-multiplier", 2.0); }

    public int getMaxMembersForLevel(int level) {
        return cfg.getInt("levels.bonuses.max-members." + level, 10 + level * 10);
    }

    public double getBankBonusPercent(int level) {
        return cfg.getDouble("levels.bonuses.bank-bonus-percent." + level, 0.0);
    }

    public boolean isTntDamageEnabled() { return cfg.getBoolean("war.tnt-damage-in-claims", true); }
    public boolean isCreeperDamageEnabled() { return cfg.getBoolean("war.creeper-damage-in-claims", false); }
    public boolean isExplosionDamageEnabled() { return cfg.getBoolean("war.explosion-damage-in-claims", false); }
    public long getRaidCooldownMs() { return cfg.getLong("war.raid-cooldown-hours", 6) * 3600_000L; }
    public double getWarPointsToMoney() { return cfg.getDouble("war.war-points-to-money", 10.0); }
    public int getPointsPerBlock() { return cfg.getInt("war.points-per-block-broken", 1); }
    public int getPointsPerKill() { return cfg.getInt("war.points-per-kill", 5); }
    public long getWarCooldownMs() { return cfg.getLong("war.cooldown-between-wars-hours", 24) * 3600_000L; }

    public boolean isAntiInsideEnabled() { return cfg.getBoolean("anti-inside.enabled", true); }
    public long getAntiInsideMs() { return cfg.getLong("anti-inside.protection-time-minutes", 30) * 60_000L; }

    public boolean isShieldEnabled() { return cfg.getBoolean("shield.enabled", true); }
    public String getDefaultShieldStart() { return cfg.getString("shield.default-start", "22:00"); }
    public String getDefaultShieldEnd() { return cfg.getString("shield.default-end", "10:00"); }

    public boolean isWarPointsRewardEnabled() { return cfg.getBoolean("economy.war-points-reward-enabled", true); }
    public double getBankTaxPercent() { return cfg.getDouble("economy.faction-bank-tax-percent", 0.0); }

    public String getPrefix() { return cfg.getString("prefix", "&8[&6Fazioni&8] &r"); }
}
