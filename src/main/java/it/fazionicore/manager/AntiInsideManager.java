package it.fazionicore.manager;

import it.fazionicore.FazioniCore;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AntiInsideManager {

    private final FazioniCore plugin;
    private final Map<UUID, Long> joinTimes = new ConcurrentHashMap<>();

    public AntiInsideManager(FazioniCore plugin) {
        this.plugin = plugin;
    }

    public void recordJoin(UUID uuid) {
        if (!plugin.getConfigManager().isAntiInsideEnabled()) return;
        joinTimes.put(uuid, System.currentTimeMillis() + plugin.getConfigManager().getAntiInsideMs());
    }

    public boolean isProtected(UUID uuid) {
        if (!plugin.getConfigManager().isAntiInsideEnabled()) return false;
        Long expiry = joinTimes.get(uuid);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) { joinTimes.remove(uuid); return false; }
        return true;
    }

    public long getRemainingMs(UUID uuid) {
        Long expiry = joinTimes.get(uuid);
        if (expiry == null) return 0;
        return Math.max(0, expiry - System.currentTimeMillis());
    }

    public void clear(UUID uuid) { joinTimes.remove(uuid); }
}
