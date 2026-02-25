package it.fazionicore.listener;

import it.fazionicore.FazioniCore;
import it.fazionicore.model.Faction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final FazioniCore plugin;

    public PlayerListener(FazioniCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(event.getPlayer().getUniqueId());
        if (faction != null) {
            plugin.getMessages().broadcastFaction(faction,
                    "&a" + event.getPlayer().getName() + " &7è entrato online.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(event.getPlayer().getUniqueId());
        if (faction != null) {
            plugin.getMessages().broadcastFaction(faction,
                    "&c" + event.getPlayer().getName() + " &7è andato offline.");
        }
        plugin.getAntiInsideManager().clear(event.getPlayer().getUniqueId());
    }
}
