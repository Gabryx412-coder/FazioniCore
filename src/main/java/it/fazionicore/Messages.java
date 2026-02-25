package it.fazionicore;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Messages {

    private final FazioniCore plugin;

    public Messages(FazioniCore plugin) {
        this.plugin = plugin;
    }

    private String prefix() {
        return plugin.getConfigManager().getPrefix();
    }

    private Component parse(String msg) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(msg);
    }

    public void send(CommandSender sender, String message) {
        sender.sendMessage(parse(prefix() + message));
    }

    public void sendRaw(CommandSender sender, String message) {
        sender.sendMessage(parse(message));
    }

    public void sendNoFaction(CommandSender sender) {
        send(sender, "&cNon sei in nessuna fazione.");
    }

    public void sendNoPermission(CommandSender sender) {
        send(sender, "&cNon hai i permessi per eseguire questa azione.");
    }

    public void sendNotEnoughMoney(CommandSender sender, double amount) {
        send(sender, "&cNon hai abbastanza soldi. Necessari: &e" + String.format("%.2f", amount));
    }

    public void sendFactionNotFound(CommandSender sender) {
        send(sender, "&cFazione non trovata.");
    }

    public void sendPlayerNotFound(CommandSender sender) {
        send(sender, "&cGiocatore non trovato.");
    }

    public void sendUsage(CommandSender sender, String usage) {
        send(sender, "&cUso corretto: &e" + usage);
    }

    public void broadcastFaction(it.fazionicore.model.Faction faction, String message) {
        String parsed = prefix() + message;
        for (java.util.UUID uuid : faction.getMembers().keySet()) {
            org.bukkit.entity.Player p = plugin.getServer().getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(parse(parsed));
            }
        }
    }

    public static String color(String msg) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(msg).toString();
    }
}
