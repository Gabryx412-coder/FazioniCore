package it.fazionicore.listener;

import it.fazionicore.FazioniCore;
import it.fazionicore.model.ClaimData;
import it.fazionicore.model.Faction;
import it.fazionicore.model.RaidLog;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

public class ProtectionListener implements Listener {

    private final FazioniCore plugin;

    public ProtectionListener(FazioniCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("fazionicore.admin")) return;

        ClaimData claim = plugin.getClaimManager().getClaimAt(event.getBlock().getLocation());
        if (claim == null) {
            if (!plugin.getConfigManager().isWildernessAllowed()) {
                // Check if world is enabled
            }
            return;
        }

        UUID uuid = player.getUniqueId();
        Faction claimFaction = plugin.getFactionManager().getFaction(claim.getFactionName());
        Faction playerFaction = plugin.getFactionManager().getPlayerFaction(uuid);

        // Same faction members can break
        if (playerFaction != null && playerFaction.getName().equalsIgnoreCase(claim.getFactionName())) {
            // Anti-inside check: new member in own faction territory
            if (plugin.getAntiInsideManager().isProtected(uuid)) {
                plugin.getMessages().send(player, "&cDevi aspettare &e" +
                        (plugin.getAntiInsideManager().getRemainingMs(uuid) / 60000) +
                        "&c minuti prima di poter interagire nel territorio.");
                event.setCancelled(true);
            }
            return;
        }

        // Enemy or wilderness - check if at war
        if (claimFaction != null) {
            boolean atWar = playerFaction != null &&
                    plugin.getWarManager().areAtWar(playerFaction.getName(), claimFaction.getName());

            // Check shield
            if (plugin.getShieldManager().isShielded(claimFaction)) {
                plugin.getMessages().send(player, "&cQuesto territorio è protetto dallo shield.");
                event.setCancelled(true);
                return;
            }

            if (!atWar) {
                plugin.getMessages().send(player, "&cNon puoi costruire in territorio nemico.");
                event.setCancelled(true);
                return;
            }

            // War: log the block break
            RaidLog log = new RaidLog(
                    claimFaction.getName(), player.getName(), uuid.toString(),
                    event.getBlock().getType().name(),
                    event.getBlock().getWorld().getName(),
                    event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ(),
                    "ROTTO"
            );
            plugin.getDatabaseManager().saveRaidLog(log);

            // Add war points
            if (playerFaction != null) {
                it.fazionicore.model.WarData wd = plugin.getWarManager().getActiveWar(playerFaction.getName());
                if (wd != null) {
                    wd.addPoints(playerFaction.getName(), plugin.getConfigManager().getPointsPerBlock());
                }
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("fazionicore.admin")) return;

        ClaimData claim = plugin.getClaimManager().getClaimAt(event.getBlock().getLocation());
        if (claim == null) return;

        UUID uuid = player.getUniqueId();
        Faction playerFaction = plugin.getFactionManager().getPlayerFaction(uuid);

        if (playerFaction != null && playerFaction.getName().equalsIgnoreCase(claim.getFactionName())) {
            if (plugin.getAntiInsideManager().isProtected(uuid)) {
                plugin.getMessages().send(player, "&cAnti-inside attivo, aspetta ancora &e" +
                        (plugin.getAntiInsideManager().getRemainingMs(uuid) / 60000) + "&c minuti.");
                event.setCancelled(true);
            }
            return;
        }

        Faction claimFaction = plugin.getFactionManager().getFaction(claim.getFactionName());
        if (claimFaction != null) {
            if (plugin.getShieldManager().isShielded(claimFaction)) {
                plugin.getMessages().send(player, "&cQuesto territorio è protetto dallo shield.");
                event.setCancelled(true);
                return;
            }
            boolean atWar = playerFaction != null &&
                    plugin.getWarManager().areAtWar(playerFaction.getName(), claimFaction.getName());
            if (!atWar) {
                plugin.getMessages().send(player, "&cNon puoi costruire in territorio nemico.");
                event.setCancelled(true);
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        EntityType type = event.getEntityType();
        boolean isTnt = type == EntityType.TNT;
        boolean isCreeper = type == EntityType.CREEPER;

        event.blockList().removeIf(block -> {
            ClaimData claim = plugin.getClaimManager().getClaimAt(block.getLocation());
            if (claim == null) return false;
            if (isTnt && plugin.getConfigManager().isTntDamageEnabled()) return false;
            if (isCreeper && plugin.getConfigManager().isCreeperDamageEnabled()) return false;
            if (!isTnt && !isCreeper && plugin.getConfigManager().isExplosionDamageEnabled()) return false;
            return true; // Remove from explosion list = protect
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = null;
        if (event.getDamager() instanceof Player p) attacker = p;
        if (attacker == null) return;

        Faction af = plugin.getFactionManager().getPlayerFaction(attacker.getUniqueId());
        Faction df = plugin.getFactionManager().getPlayerFaction(victim.getUniqueId());

        if (af != null && df != null && af.getName().equalsIgnoreCase(df.getName())) {
            if (!af.isFriendlyFire()) {
                event.setCancelled(true);
                plugin.getMessages().send(attacker, "&cIl friendly fire è disabilitato nella tua fazione.");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (!(victim.getKiller() instanceof Player killer)) return;

        Faction kf = plugin.getFactionManager().getPlayerFaction(killer.getUniqueId());
        Faction vf = plugin.getFactionManager().getPlayerFaction(victim.getUniqueId());

        if (kf == null || vf == null) return;
        if (!plugin.getWarManager().areAtWar(kf.getName(), vf.getName())) return;

        it.fazionicore.model.WarData wd = plugin.getWarManager().getActiveWar(kf.getName());
        if (wd != null) {
            wd.addPoints(kf.getName(), plugin.getConfigManager().getPointsPerKill());
            plugin.getMessages().send(killer, "&a+&e" + plugin.getConfigManager().getPointsPerKill() +
                    " &apunti guerra! (&eKill&a)");
        }
    }
}
