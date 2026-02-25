package it.fazionicore.manager;

import it.fazionicore.FazioniCore;
import it.fazionicore.model.ClaimData;
import it.fazionicore.model.Faction;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ClaimTool implements Listener {

    private final FazioniCore plugin;
    private final Set<UUID> toolUsers = new HashSet<>();

    public ClaimTool(FazioniCore plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public ItemStack createClaimTool() {
        ItemStack tool = new ItemStack(Material.GOLDEN_SHOVEL);
        ItemMeta meta = tool.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Claim Tool");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Click destro per claimare",
                ChatColor.GRAY + "Click sinistro per visualizzare chunk",
                ChatColor.GRAY + "Shift + Click destro per unclaimare"
        ));
        tool.setItemMeta(meta);
        return tool;
    }

    public boolean isClaimTool(ItemStack item) {
        if (item == null || item.getType() != Material.GOLDEN_SHOVEL) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() &&
                meta.getDisplayName().equals(ChatColor.GOLD + "Claim Tool");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isClaimTool(item)) return;

        event.setCancelled(true);

        if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR) {
            // Visualizza chunk
            plugin.getClaimVisualizer().toggleVisualization(player);
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            // Claim o unclaim
            Chunk chunk = player.getLocation().getChunk();
            Faction faction = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());

            if (faction == null) {
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        ChatColor.RED + "Devi essere in una fazione per usare questo tool!");
                return;
            }

            if (player.isSneaking()) {
                // Unclaim
                if (plugin.getClaimManager().unclaim(faction, chunk)) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() +
                            ChatColor.GREEN + "Chunk unclaimate con successo!");
                } else {
                    player.sendMessage(plugin.getConfigManager().getPrefix() +
                            ChatColor.RED + "Non puoi unclaim questo chunk!");
                }
            } else {
                // Claim
                if (plugin.getClaimManager().claim(faction, chunk)) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() +
                            ChatColor.GREEN + "Chunk claimate con successo!");
                } else {
                    player.sendMessage(plugin.getConfigManager().getPrefix() +
                            ChatColor.RED + "Non puoi claimare questo chunk!");
                }
            }
        }
    }
}