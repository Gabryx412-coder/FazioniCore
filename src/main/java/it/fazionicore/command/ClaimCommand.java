package it.fazionicore.command;

import it.fazionicore.FazioniCore;
import it.fazionicore.model.ClaimData;
import it.fazionicore.model.Faction;
import it.fazionicore.model.FactionRole;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ClaimCommand implements CommandExecutor, TabCompleter {

    private final FazioniCore plugin;

    public ClaimCommand(FazioniCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Solo i giocatori possono usare questo comando!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "claim":
                handleClaim(player);
                break;
            case "unclaim":
                handleUnclaim(player);
                break;
            case "info":
                handleInfo(player);
                break;
            case "list":
                handleList(player);
                break;
            case "tool":
                handleTool(player);
                break;
            case "show":
                handleShow(player);
                break;
            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void handleClaim(Player player) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        if (faction == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    ChatColor.RED + "Devi essere in una fazione!");
            return;
        }

        FactionRole role = faction.getRole(player.getUniqueId());
        if (!role.isAtLeast(FactionRole.OFFICER)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    ChatColor.RED + "Solo ufficiali e leader possono claimare territori!");
            return;
        }

        Chunk chunk = player.getLocation().getChunk();

        // Controlla se il mondo è abilitato
        if (!plugin.getConfigManager().getClaimEnabledWorlds().contains(chunk.getWorld().getName())) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    ChatColor.RED + "Non puoi claimare in questo mondo!");
            return;
        }

        // Controlla costo
        double cost = plugin.getConfigManager().getClaimCost();
        if (!plugin.getEconomyManager().has(player, cost)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    ChatColor.RED + "Non hai abbastanza soldi! Costo: " +
                    plugin.getEconomyManager().format(cost));
            return;
        }

        if (plugin.getClaimManager().claim(faction, chunk)) {
            plugin.getEconomyManager().withdraw(player, cost);
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    ChatColor.GREEN + "Territorio claimate con successo! Costo: " +
                    plugin.getEconomyManager().format(cost));
        } else {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    ChatColor.RED + "Non puoi claimare questo territorio!");
        }
    }

    private void handleUnclaim(Player player) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        if (faction == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    ChatColor.RED + "Devi essere in una fazione!");
            return;
        }

        FactionRole role = faction.getRole(player.getUniqueId());
        if (!role.isAtLeast(FactionRole.OFFICER)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    ChatColor.RED + "Solo ufficiali e leader possono unclaim territori!");
            return;
        }

        Chunk chunk = player.getLocation().getChunk();

        if (plugin.getClaimManager().unclaim(faction, chunk)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    ChatColor.GREEN + "Territorio unclaimate con successo!");
        } else {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    ChatColor.RED + "Non puoi unclaim questo territorio!");
        }
    }

    private void handleInfo(Player player) {
        ClaimData claim = plugin.getClaimManager().getClaimAt(player.getLocation());

        if (claim == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    ChatColor.GRAY + "Questo territorio è wilderness (non claimate)");
        } else {
            Faction faction = plugin.getFactionManager().getFaction(claim.getFactionName());
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    ChatColor.GOLD + "Territorio di: " + ChatColor.WHITE + claim.getFactionName());
            if (faction != null) {
                player.sendMessage(ChatColor.GRAY + "Tag: " + ChatColor.WHITE + faction.getTag());
                player.sendMessage(ChatColor.GRAY + "Livello: " + ChatColor.WHITE + faction.getLevel());
            }
        }
    }

    private void handleList(Player player) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        if (faction == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    ChatColor.RED + "Devi essere in una fazione!");
            return;
        }

        int claimCount = plugin.getClaimManager().getClaimCount(faction.getName());
        int claimLimit = plugin.getFactionManager().getClaimLimit(faction);

        player.sendMessage(plugin.getConfigManager().getPrefix() +
                ChatColor.GOLD + "Territori della tua fazione:");
        player.sendMessage(ChatColor.GRAY + "Claimate: " + ChatColor.WHITE + claimCount +
                ChatColor.GRAY + "/" + ChatColor.WHITE + claimLimit);
    }

    private void handleTool(Player player) {
        player.getInventory().addItem(plugin.getClaimTool().createClaimTool());
        player.sendMessage(plugin.getConfigManager().getPrefix() +
                ChatColor.GREEN + "Hai ricevuto il Claim Tool!");
    }

    private void handleShow(Player player) {
        plugin.getClaimVisualizer().toggleVisualization(player);
    }

    private void sendHelp(Player player) {
        player.sendMessage(plugin.getConfigManager().getPrefix() +
                ChatColor.GOLD + "Comandi Claim:");
        player.sendMessage(ChatColor.YELLOW + "/claim claim" + ChatColor.GRAY + " - Claima il chunk corrente");
        player.sendMessage(ChatColor.YELLOW + "/claim unclaim" + ChatColor.GRAY + " - Unclaim il chunk corrente");
        player.sendMessage(ChatColor.YELLOW + "/claim info" + ChatColor.GRAY + " - Info sul chunk corrente");
        player.sendMessage(ChatColor.YELLOW + "/claim list" + ChatColor.GRAY + " - Lista territori della fazione");
        player.sendMessage(ChatColor.YELLOW + "/claim tool" + ChatColor.GRAY + " - Ottieni il claim tool");
        player.sendMessage(ChatColor.YELLOW + "/claim show" + ChatColor.GRAY + " - Visualizza chunk con particelle");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("claim", "unclaim", "info", "list", "tool", "show")
                    .stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return null;
    }
}