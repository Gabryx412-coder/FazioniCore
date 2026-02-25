package it.fazionicore.manager;

import it.fazionicore.FazioniCore;
import it.fazionicore.model.Faction;
import it.fazionicore.model.FactionRole;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FactionManager {

    private final FazioniCore plugin;
    private final Map<String, Faction> factions = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerFactionMap = new ConcurrentHashMap<>();

    public FactionManager(FazioniCore plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        File dir = new File(plugin.getDataFolder(), "data/factions");
        if (!dir.exists()) return;
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            Faction faction = loadFaction(f);
            if (faction != null) {
                factions.put(faction.getName().toLowerCase(), faction);
                for (UUID uuid : faction.getMembers().keySet()) {
                    playerFactionMap.put(uuid, faction.getName().toLowerCase());
                }
            }
        }
        plugin.getLogger().info("Caricate " + factions.size() + " fazioni.");
    }

    private Faction loadFaction(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String name = yaml.getString("name");
        String tag = yaml.getString("tag", "?");
        String leaderStr = yaml.getString("leader");
        if (name == null || leaderStr == null) return null;
        UUID leader;
        try { leader = UUID.fromString(leaderStr); } catch (Exception e) { return null; }
        Faction f = new Faction(name, tag, leader);
        f.setDescription(yaml.getString("description", ""));
        f.setBank(yaml.getDouble("bank", 0.0));
        f.setFriendlyFire(yaml.getBoolean("friendlyFire", false));
        f.setOpen(yaml.getBoolean("open", false));
        f.setWarPoints(yaml.getInt("warPoints", 0));
        f.setLevel(yaml.getInt("level", 1));
        f.setShieldEnabled(yaml.getBoolean("shieldEnabled", false));
        f.setShieldStart(yaml.getString("shieldStart", "22:00"));
        f.setShieldEnd(yaml.getString("shieldEnd", "10:00"));
        f.setTotalClaimsEver(yaml.getInt("totalClaimsEver", 0));
        // load members
        var membersSection = yaml.getConfigurationSection("members");
        if (membersSection != null) {
            for (String uuidStr : membersSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String roleName = membersSection.getString(uuidStr, "MEMBER");
                    FactionRole role;
                    try { role = FactionRole.valueOf(roleName); } catch (Exception e) { role = FactionRole.MEMBER; }
                    f.addMember(uuid, role);
                } catch (Exception ignored) {}
            }
        }
        List<String> invites = yaml.getStringList("invites");
        for (String inv : invites) f.getPendingInvites().add(inv);
        return f;
    }

    public void saveAll() {
        for (Faction f : factions.values()) saveFaction(f);
    }

    public void saveFaction(Faction f) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = new File(plugin.getDataFolder(), "data/factions/" + f.getName().toLowerCase() + ".yml");
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("name", f.getName());
            yaml.set("tag", f.getTag());
            yaml.set("description", f.getDescription());
            yaml.set("leader", f.getLeader().toString());
            yaml.set("bank", f.getBank());
            yaml.set("friendlyFire", f.isFriendlyFire());
            yaml.set("open", f.isOpen());
            yaml.set("warPoints", f.getWarPoints());
            yaml.set("level", f.getLevel());
            yaml.set("shieldEnabled", f.isShieldEnabled());
            yaml.set("shieldStart", f.getShieldStart());
            yaml.set("shieldEnd", f.getShieldEnd());
            yaml.set("totalClaimsEver", f.getTotalClaimsEver());
            for (Map.Entry<UUID, FactionRole> entry : f.getMembers().entrySet()) {
                yaml.set("members." + entry.getKey().toString(), entry.getValue().name());
            }
            yaml.set("invites", new ArrayList<>(f.getPendingInvites()));
            try { yaml.save(file); } catch (Exception e) {
                plugin.getLogger().warning("Errore salvataggio fazione: " + f.getName());
            }
        });
    }

    public Faction createFaction(String name, String tag, UUID leader) {
        Faction f = new Faction(name, tag, leader);
        f.setFriendlyFire(plugin.getConfigManager().getDefaultFriendlyFire());
        f.setOpen(plugin.getConfigManager().getDefaultOpen());
        f.setShieldStart(plugin.getConfigManager().getDefaultShieldStart());
        f.setShieldEnd(plugin.getConfigManager().getDefaultShieldEnd());
        factions.put(name.toLowerCase(), f);
        playerFactionMap.put(leader, name.toLowerCase());
        saveFaction(f);
        return f;
    }

    public void disbandFaction(String name) {
        Faction f = factions.remove(name.toLowerCase());
        if (f == null) return;
        for (UUID uuid : f.getMembers().keySet()) playerFactionMap.remove(uuid);
        plugin.getClaimManager().unclaimAll(name);
        File file = new File(plugin.getDataFolder(), "data/factions/" + name.toLowerCase() + ".yml");
        file.delete();
    }

    public Faction getFaction(String name) {
        return factions.get(name.toLowerCase());
    }

    public Faction getPlayerFaction(UUID uuid) {
        String name = playerFactionMap.get(uuid);
        return name == null ? null : factions.get(name);
    }

    public boolean isInFaction(UUID uuid) { return playerFactionMap.containsKey(uuid); }

    public void joinFaction(UUID uuid, Faction faction, FactionRole role) {
        faction.addMember(uuid, role);
        playerFactionMap.put(uuid, faction.getName().toLowerCase());
        recalcLevel(faction);
        saveFaction(faction);
    }

    public void leaveFaction(UUID uuid, Faction faction) {
        faction.removeMember(uuid);
        playerFactionMap.remove(uuid);
        recalcLevel(faction);
        saveFaction(faction);
    }

    public void recalcLevel(Faction faction) {
        faction.recalculateLevel(
                plugin.getConfigManager().getLevelThresholds(),
                plugin.getConfigManager().getMemberMultiplier()
        );
    }

    public Collection<Faction> getAllFactions() { return factions.values(); }

    public boolean factionExists(String name) { return factions.containsKey(name.toLowerCase()); }

    public int getMaxMembers(Faction f) {
        return plugin.getConfigManager().getMaxMembersForLevel(f.getLevel());
    }
    
    public int getClaimLimit(Faction faction) {
        Map<Integer, Integer> limits = plugin.getConfigManager().getClaimLimits();
        int baseLimit = limits.getOrDefault(faction.getLevel(), 10);

        int memberBonus = faction.getMembers().size() * 2;

        int powerBonus = 0; // TODO: Implementare sistema power
        
        return baseLimit + memberBonus + powerBonus;
    }

    public String getPlayerFactionName(UUID playerId) {
        Faction faction = getPlayerFaction(playerId);
        return faction != null ? faction.getName() : null;
    }
}
