package it.fazionicore.manager;

import it.fazionicore.FazioniCore;
import it.fazionicore.model.Faction;
import it.fazionicore.model.WarData;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WarManager {

    private final FazioniCore plugin;
    private final List<WarData> wars = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Long> raidCooldowns = new ConcurrentHashMap<>();
    private final Map<String, Long> warCooldowns = new ConcurrentHashMap<>();

    public WarManager(FazioniCore plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        File file = new File(plugin.getDataFolder(), "data/wars/wars.yml");
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        var section = yaml.getConfigurationSection("wars");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            String f1 = section.getString(key + ".faction1");
            String f2 = section.getString(key + ".faction2");
            if (f1 == null || f2 == null) continue;
            WarData wd = new WarData(f1, f2);
            wd.setPoints1(section.getInt(key + ".points1", 0));
            wd.setPoints2(section.getInt(key + ".points2", 0));
            wd.setActive(section.getBoolean(key + ".active", true));
            wars.add(wd);
        }
        var cdSection = yaml.getConfigurationSection("raidCooldowns");
        if (cdSection != null) {
            for (String k : cdSection.getKeys(false)) raidCooldowns.put(k, cdSection.getLong(k));
        }
    }

    public void saveAll() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = new File(plugin.getDataFolder(), "data/wars/wars.yml");
            YamlConfiguration yaml = new YamlConfiguration();
            for (int i = 0; i < wars.size(); i++) {
                WarData wd = wars.get(i);
                String path = "wars." + i;
                yaml.set(path + ".faction1", wd.getFaction1());
                yaml.set(path + ".faction2", wd.getFaction2());
                yaml.set(path + ".points1", wd.getPoints1());
                yaml.set(path + ".points2", wd.getPoints2());
                yaml.set(path + ".active", wd.isActive());
            }
            for (Map.Entry<String, Long> entry : raidCooldowns.entrySet()) {
                yaml.set("raidCooldowns." + entry.getKey(), entry.getValue());
            }
            try { yaml.save(file); } catch (Exception e) {
                plugin.getLogger().warning("Errore salvataggio guerre.");
            }
        });
    }

    public WarData declareWar(Faction attacker, Faction defender) {
        WarData wd = new WarData(attacker.getName(), defender.getName());
        wars.add(wd);
        setWarCooldown(attacker.getName() + "_" + defender.getName());
        saveAll();
        return wd;
    }

    public WarData getActiveWar(String factionName) {
        synchronized (wars) {
            return wars.stream()
                    .filter(w -> w.isActive() && w.involves(factionName))
                    .findFirst().orElse(null);
        }
    }

    public boolean areAtWar(String f1, String f2) {
        WarData wd = getActiveWar(f1);
        return wd != null && wd.involves(f2);
    }

    public void endWar(WarData war, String winnerName) {
        war.end();
        Faction winner = plugin.getFactionManager().getFaction(winnerName);
        Faction loser = plugin.getFactionManager().getFaction(war.getOpponent(winnerName));
        if (winner != null && loser != null && plugin.getConfigManager().isWarPointsRewardEnabled()) {
            int pts = war.getPoints(winnerName);
            double reward = pts * plugin.getConfigManager().getWarPointsToMoney();
            winner.depositBank(reward);
            plugin.getMessages().broadcastFaction(winner,
                    "&aGuerra vinta contro &e" + loser.getName() + "&a! Punti: &e" + pts + " &a-> Banca: &e" + plugin.getEconomyManager().format(reward));
            plugin.getMessages().broadcastFaction(loser,
                    "&cGuerra persa contro &e" + winner.getName() + "&c. Punti avversario: &e" + pts);
            plugin.getFactionManager().saveFaction(winner);
        }
        saveAll();
    }

    public void addRaidCooldown(String factionName) {
        raidCooldowns.put(factionName.toLowerCase(), System.currentTimeMillis() + plugin.getConfigManager().getRaidCooldownMs());
    }

    public boolean isOnRaidCooldown(String factionName) {
        Long time = raidCooldowns.get(factionName.toLowerCase());
        if (time == null) return false;
        if (System.currentTimeMillis() > time) { raidCooldowns.remove(factionName.toLowerCase()); return false; }
        return true;
    }

    public long getRaidCooldownRemaining(String factionName) {
        Long time = raidCooldowns.get(factionName.toLowerCase());
        if (time == null) return 0;
        return Math.max(0, time - System.currentTimeMillis());
    }

    private void setWarCooldown(String key) {
        warCooldowns.put(key.toLowerCase(), System.currentTimeMillis() + plugin.getConfigManager().getWarCooldownMs());
    }

    public boolean isOnWarCooldown(String f1, String f2) {
        String key1 = (f1 + "_" + f2).toLowerCase();
        String key2 = (f2 + "_" + f1).toLowerCase();
        long now = System.currentTimeMillis();
        Long t1 = warCooldowns.get(key1);
        Long t2 = warCooldowns.get(key2);
        return (t1 != null && now < t1) || (t2 != null && now < t2);
    }

    public List<WarData> getActiveWars() {
        synchronized (wars) {
            return wars.stream().filter(WarData::isActive).toList();
        }
    }
}
