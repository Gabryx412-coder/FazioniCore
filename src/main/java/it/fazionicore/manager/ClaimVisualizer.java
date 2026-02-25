package it.fazionicore.manager;

import it.fazionicore.FazioniCore;
import it.fazionicore.model.ClaimData;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClaimVisualizer {

    private final FazioniCore plugin;
    private final Map<UUID, BukkitTask> activeVisualizations = new HashMap<>();

    public ClaimVisualizer(FazioniCore plugin) {
        this.plugin = plugin;
    }

    public void toggleVisualization(Player player) {
        UUID uuid = player.getUniqueId();

        if (activeVisualizations.containsKey(uuid)) {
            stopVisualization(player);
        } else {
            startVisualization(player);
        }
    }

    public void startVisualization(Player player) {
        UUID uuid = player.getUniqueId();

        if (activeVisualizations.containsKey(uuid)) {
            stopVisualization(player);
        }

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                stopVisualization(player);
                return;
            }

            visualizeChunk(player, player.getLocation().getChunk());
        }, 0L, 20L); // Ogni secondo

        activeVisualizations.put(uuid, task);
        player.sendMessage(plugin.getConfigManager().getPrefix() +
                "§aVisualizzazione chunk attivata!");
    }

    public void stopVisualization(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitTask task = activeVisualizations.remove(uuid);

        if (task != null) {
            task.cancel();
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§cVisualizzazione chunk disattivata!");
        }
    }

    private void visualizeChunk(Player player, Chunk chunk) {
        ClaimData claim = plugin.getClaimManager().getClaimAt(chunk);

        int minX = chunk.getX() * 16;
        int minZ = chunk.getZ() * 16;
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        int y = player.getLocation().getBlockY() + 1;

        for (int x = minX; x <= maxX; x += 5) {
            spawnParticle(player, new Location(player.getWorld(), x, y, minZ), claim);
            spawnParticle(player, new Location(player.getWorld(), x, y, maxZ), claim);
        }

        for (int z = minZ; z <= maxZ; z += 5) {
            spawnParticle(player, new Location(player.getWorld(), minX, y, z), claim);
            spawnParticle(player, new Location(player.getWorld(), maxX, y, z), claim);
        }
    }

    private void spawnParticle(Player player, Location loc, ClaimData claim) {
        Particle particle;
        if (claim == null) {
            particle = Particle.HAPPY_VILLAGER;
        } else {
            String playerFaction = plugin.getFactionManager().getPlayerFactionName(player.getUniqueId());
            if (claim.getFactionName().equalsIgnoreCase(playerFaction)) {
                particle = Particle.HEART;
            } else {
                particle = Particle.FLAME;
            }
        }

        player.spawnParticle(particle, loc, 1, 0, 0, 0, 0);
    }

    public void cleanupPlayer(Player player) {
        stopVisualization(player);
    }
}