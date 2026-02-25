package it.fazionicore.manager;

import it.fazionicore.FazioniCore;
import it.fazionicore.model.ClaimData;
import it.fazionicore.model.Faction;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ChunkMapGenerator {

    private final FazioniCore plugin;

    public ChunkMapGenerator(FazioniCore plugin) {
        this.plugin = plugin;
    }

    public List<String> generateMap(Player player, int radius) {
        List<String> map = new ArrayList<>();
        Chunk centerChunk = player.getLocation().getChunk();
        int centerX = centerChunk.getX();
        int centerZ = centerChunk.getZ();

        String playerFactionName = plugin.getFactionManager().getPlayerFactionName(player.getUniqueId());

        // Header
        map.add(ChatColor.GOLD + "========== Mappa Fazioni ==========");
        map.add(ChatColor.GRAY + "Posizione: " + centerX + ", " + centerZ);
        map.add("");

        // Legenda
        map.add(ChatColor.GREEN + "§ = Tuo territorio");
        map.add(ChatColor.RED + "■ = Territorio nemico");
        map.add(ChatColor.YELLOW + "□ = Territorio alleato");
        map.add(ChatColor.GRAY + "- = Wilderness");
        map.add(ChatColor.BLUE + "★ = Tua posizione");
        map.add("");

        // Mappa
        for (int z = centerZ - radius; z <= centerZ + radius; z++) {
            StringBuilder line = new StringBuilder();

            for (int x = centerX - radius; x <= centerX + radius; x++) {
                if (x == centerX && z == centerZ) {
                    line.append(ChatColor.BLUE + "★");
                } else {
                    ClaimData claim = plugin.getClaimManager().getClaimAt(
                            new org.bukkit.Location(player.getWorld(), x * 16, 64, z * 16).getChunk()
                    );

                    if (claim == null) {
                        line.append(ChatColor.GRAY + "-");
                    } else {
                        if (claim.getFactionName().equalsIgnoreCase(playerFactionName)) {
                            line.append(ChatColor.GREEN + "§");
                        } else {
                            // TODO: Implementare sistema alleanze per distinguere alleati da nemici
                            line.append(ChatColor.RED + "■");
                        }
                    }
                }
            }

            map.add(line.toString());
        }

        map.add("");
        map.add(ChatColor.GOLD + "===================================");

        return map;
    }
}