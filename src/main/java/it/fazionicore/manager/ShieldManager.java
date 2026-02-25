package it.fazionicore.manager;

import it.fazionicore.FazioniCore;
import it.fazionicore.model.Faction;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ShieldManager {

    private final FazioniCore plugin;
    private BukkitTask task;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm");

    public ShieldManager(FazioniCore plugin) {
        this.plugin = plugin;
    }

    public void startTask() {
        if (!plugin.getConfigManager().isShieldEnabled()) return;
        task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {}, 0L, 20L * 60);
    }

    public boolean isShielded(Faction faction) {
        if (!plugin.getConfigManager().isShieldEnabled()) return false;
        if (!faction.isShieldEnabled()) return false;
        try {
            LocalTime now = LocalTime.now();
            LocalTime start = LocalTime.parse(faction.getShieldStart(), FMT);
            LocalTime end = LocalTime.parse(faction.getShieldEnd(), FMT);
            return isInShieldWindow(now, start, end);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private boolean isInShieldWindow(LocalTime now, LocalTime start, LocalTime end) {
        if (start.isBefore(end)) {
            return !now.isBefore(start) && !now.isAfter(end);
        } else {
            return !now.isBefore(start) || !now.isAfter(end);
        }
    }

    public boolean isValidTime(String time) {
        try { LocalTime.parse(time, FMT); return true; } catch (DateTimeParseException e) { return false; }
    }

    public void stop() {
        if (task != null) task.cancel();
    }
}
