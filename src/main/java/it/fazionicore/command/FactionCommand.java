package it.fazionicore.command;

import it.fazionicore.FazioniCore;
import it.fazionicore.model.Faction;
import it.fazionicore.model.FactionRole;
import it.fazionicore.model.WarData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class FactionCommand implements CommandExecutor, TabCompleter {

    private final FazioniCore plugin;

    public FactionCommand(FazioniCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo i giocatori possono usare questo comando.");
            return true;
        }
        if (args.length == 0) { sendHelp(player); return true; }

        switch (args[0].toLowerCase()) {
            case "create", "crea" -> cmdCreate(player, args);
            case "disband", "sciogli" -> cmdDisband(player);
            case "invite", "invita" -> cmdInvite(player, args);
            case "join", "entra" -> cmdJoin(player, args);
            case "leave", "lascia" -> cmdLeave(player);
            case "kick", "espelli" -> cmdKick(player, args);
            case "promote", "promuovi" -> cmdPromote(player, args);
            case "demote", "demoti" -> cmdDemote(player, args);
            case "info" -> cmdInfo(player, args);
            case "list", "lista" -> cmdList(player);
            case "claim" -> cmdClaim(player);
            case "unclaim" -> cmdUnclaim(player);
            case "desc" -> cmdDesc(player, args);
            case "tag" -> cmdTag(player, args);
            case "ff", "friendlyfire" -> cmdFriendlyFire(player);
            case "open", "aperta" -> cmdOpen(player);
            case "bank", "banca" -> cmdBank(player, args);
            case "war", "guerra" -> cmdWar(player, args);
            case "endwar", "fineguerra" -> cmdEndWar(player, args);
            case "shield" -> cmdShield(player, args);
            case "map" -> {
                handleMap(player, args);
            }
            case "reload" -> cmdReload(player);
            default -> sendHelp(player);
        }
        return true;
    }

    private void cmdCreate(Player player, String[] args) {
        if (args.length < 3) { plugin.getMessages().sendUsage(player, "/f create <nome> <tag>"); return; }
        if (plugin.getFactionManager().isInFaction(player.getUniqueId())) {
            plugin.getMessages().send(player, "&cSei già in una fazione."); return;
        }
        String name = args[1];
        String tag = args[2];
        int minN = plugin.getConfigManager().getMinNameLength(), maxN = plugin.getConfigManager().getMaxNameLength();
        int minT = plugin.getConfigManager().getMinTagLength(), maxT = plugin.getConfigManager().getMaxTagLength();
        if (name.length() < minN || name.length() > maxN) {
            plugin.getMessages().send(player, "&cIl nome deve essere tra " + minN + " e " + maxN + " caratteri."); return;
        }
        if (tag.length() < minT || tag.length() > maxT) {
            plugin.getMessages().send(player, "&cIl tag deve essere tra " + minT + " e " + maxT + " caratteri."); return;
        }
        if (plugin.getFactionManager().factionExists(name)) {
            plugin.getMessages().send(player, "&cEsiste già una fazione con questo nome."); return;
        }
        if (!name.matches("[a-zA-Z0-9_]+") || !tag.matches("[a-zA-Z0-9_]+")) {
            plugin.getMessages().send(player, "&cUsa solo lettere, numeri e underscore."); return;
        }
        double cost = plugin.getConfigManager().getFactionCreationCost();
        if (!plugin.getEconomyManager().withdraw(player, cost)) {
            plugin.getMessages().sendNotEnoughMoney(player, cost); return;
        }
        Faction f = plugin.getFactionManager().createFaction(name, tag, player.getUniqueId());
        plugin.getMessages().send(player, "&aFazione &e" + f.getName() + " &acreata con successo! Costo: &e" + plugin.getEconomyManager().format(cost));
    }

    private void cmdDisband(Player player) {
        Faction f = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        if (f == null) { plugin.getMessages().sendNoFaction(player); return; }
        if (!f.getLeader().equals(player.getUniqueId())) { plugin.getMessages().sendNoPermission(player); return; }
        plugin.getMessages().broadcastFaction(f, "&cLa fazione &e" + f.getName() + " &cè stata sciolta!");
        plugin.getFactionManager().disbandFaction(f.getName());
    }

    private void cmdInvite(Player player, String[] args) {
        if (args.length < 2) { plugin.getMessages().sendUsage(player, "/f invite <giocatore>"); return; }
        Faction f = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        if (f == null) { plugin.getMessages().sendNoFaction(player); return; }
        FactionRole role = f.getRole(player.getUniqueId());
        if (!role.isAtLeast(FactionRole.OFFICER)) { plugin.getMessages().sendNoPermission(player); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { plugin.getMessages().sendPlayerNotFound(player); return; }
        if (plugin.getFactionManager().isInFaction(target.getUniqueId())) {
            plugin.getMessages().send(player, "&cQuesto giocatore è già in una fazione."); return;
        }
        if (f.getMembers().size() >= plugin.getFactionManager().getMaxMembers(f)) {
            plugin.getMessages().send(player, "&cLa fazione ha raggiunto il limite massimo di membri (livello " + f.getLevel() + ")."); return;
        }
        f.addInvite(target.getUniqueId());
        plugin.getFactionManager().saveFaction(f);
        plugin.getMessages().send(player, "&aInvito inviato a &e" + target.getName());
        plugin.getMessages().send(target, "&aHai ricevuto un invito dalla fazione &e" + f.getName() + "&a. Usa &e/f join " + f.getName() + " &aper accettare.");
    }

    private void cmdJoin(Player player, String[] args) {
        if (args.length < 2) { plugin.getMessages().sendUsage(player, "/f join <fazione>"); return; }
        if (plugin.getFactionManager().isInFaction(player.getUniqueId())) {
            plugin.getMessages().send(player, "&cSei già in una fazione."); return;
        }
        Faction f = plugin.getFactionManager().getFaction(args[1]);
        if (f == null) { plugin.getMessages().sendFactionNotFound(player); return; }
        if (f.getMembers().size() >= plugin.getFactionManager().getMaxMembers(f)) {
            plugin.getMessages().send(player, "&cLa fazione è piena."); return;
        }
        if (!f.isOpen() && !f.hasInvite(player.getUniqueId())) {
            plugin.getMessages().send(player, "&cNon sei stato invitato in questa fazione."); return;
        }
        f.removeInvite(player.getUniqueId());
        plugin.getFactionManager().joinFaction(player.getUniqueId(), f, FactionRole.RECRUIT);
        plugin.getAntiInsideManager().recordJoin(player.getUniqueId());
        plugin.getMessages().broadcastFaction(f, "&e" + player.getName() + " &aè entrato nella fazione!");
    }

    private void cmdLeave(Player player) {
        Faction f = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        if (f == null) { plugin.getMessages().sendNoFaction(player); return; }
        if (f.getLeader().equals(player.getUniqueId())) {
            plugin.getMessages().send(player, "&cSei il leader, usa &e/f disband &cper sciogliere o &e/f promote &cper trasferire la leadership."); return;
        }
        plugin.getFactionManager().leaveFaction(player.getUniqueId(), f);
        plugin.getAntiInsideManager().clear(player.getUniqueId());
        plugin.getMessages().send(player, "&aHai lasciato la fazione &e" + f.getName());
        plugin.getMessages().broadcastFaction(f, "&e" + player.getName() + " &cha lasciato la fazione.");
    }

    private void cmdKick(Player player, String[] args) {
        if (args.length < 2) { plugin.getMessages().sendUsage(player, "/f kick <giocatore>"); return; }
        Faction f = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        if (f == null) { plugin.getMessages().sendNoFaction(player); return; }
        FactionRole myRole = f.getRole(player.getUniqueId());
        if (!myRole.isAtLeast(FactionRole.OFFICER)) { plugin.getMessages().sendNoPermission(player); return; }
        Player target = Bukkit.getPlayer(args[1]);
        UUID targetUUID = target != null ? target.getUniqueId() : getOfflineUUID(args[1]);
        if (targetUUID == null || !f.hasMember(targetUUID)) {
            plugin.getMessages().send(player, "&cGiocatore non trovato nella fazione."); return;
        }
        FactionRole targetRole = f.getRole(targetUUID);
        if (targetRole.isAtLeast(myRole)) {
            plugin.getMessages().send(player, "&cNon puoi espellere qualcuno con un rango uguale o superiore."); return;
        }
        plugin.getFactionManager().leaveFaction(targetUUID, f);
        plugin.getMessages().send(player, "&aGiocatore espulso dalla fazione.");
        plugin.getMessages().broadcastFaction(f, "&e" + args[1] + " &cè stato espulso dalla fazione.");
        if (target != null) plugin.getMessages().send(target, "&cSei stato espulso dalla fazione &e" + f.getName());
    }

    private void cmdPromote(Player player, String[] args) {
        if (args.length < 2) { plugin.getMessages().sendUsage(player, "/f promote <giocatore>"); return; }
        Faction f = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        if (f == null) { plugin.getMessages().sendNoFaction(player); return; }
        if (!f.getLeader().equals(player.getUniqueId())) { plugin.getMessages().sendNoPermission(player); return; }
        Player target = Bukkit.getPlayer(args[1]);
        UUID targetUUID = target != null ? target.getUniqueId() : getOfflineUUID(args[1]);
        if (targetUUID == null || !f.hasMember(targetUUID)) {
            plugin.getMessages().send(player, "&cGiocatore non trovato nella fazione."); return;
        }
        FactionRole current = f.getRole(targetUUID);
        FactionRole[] roles = FactionRole.values();
        int next = current.ordinal() + 1;
        if (next >= FactionRole.LEADER.ordinal()) {
            // Transfer leadership
            f.setRole(player.getUniqueId(), FactionRole.OFFICER);
            f.setLeader(targetUUID);
            plugin.getFactionManager().saveFaction(f);
            plugin.getMessages().broadcastFaction(f, "&e" + args[1] + " &aè il nuovo leader della fazione!");
            return;
        }
        FactionRole newRole = roles[next];
        f.setRole(targetUUID, newRole);
        plugin.getFactionManager().saveFaction(f);
        plugin.getMessages().broadcastFaction(f, "&e" + args[1] + " &apromosso a &e" + newRole.getDisplay());
    }

    private void cmdDemote(Player player, String[] args) {
        if (args.length < 2) { plugin.getMessages().sendUsage(player, "/f demote <giocatore>"); return; }
        Faction f = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        if (f == null) { plugin.getMessages().sendNoFaction(player); return; }
        if (!f.getLeader().equals(player.getUniqueId())) { plugin.getMessages().sendNoPermission(player); return; }
        Player target = Bukkit.getPlayer(args[1]);
        UUID targetUUID = target != null ? target.getUniqueId() : getOfflineUUID(args[1]);
        if (targetUUID == null || !f.hasMember(targetUUID) || targetUUID.equals(player.getUniqueId())) {
            plugin.getMessages().send(player, "&cGiocatore non trovato o non valido."); return;
        }
        FactionRole current = f.getRole(targetUUID);
        if (current.ordinal() == 0) { plugin.getMessages().send(player, "&cIl giocatore ha già il rango minimo."); return; }
        FactionRole newRole = FactionRole.values()[current.ordinal() - 1];
        f.setRole(targetUUID, newRole);
        plugin.getFactionManager().saveFaction(f);
        plugin.getMessages().broadcastFaction(f, "&e" + args[1] + " &cretrocesso a &e" + newRole.getDisplay());
    }

    private void cmdInfo(Player player, String[] args) {
        Faction f;
        if (args.length >= 2) {
            f = plugin.getFactionManager().getFaction(args[1]);
            if (f == null) { plugin.getMessages().sendFactionNotFound(player); return; }
        } else {
            f = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
            if (f == null) { plugin.getMessages().sendNoFaction(player); return; }
        }
        int claims = plugin.getClaimManager().getClaimCount(f.getName());
        int maxClaims = plugin.getFactionManager().getClaimLimit(f);
        int maxMembers = plugin.getFactionManager().getMaxMembers(f);
        WarData war = plugin.getWarManager().getActiveWar(f.getName());
        plugin.getMessages().sendRaw(player, "&6&l=== " + f.getName() + " [" + f.getTag() + "] ===");
        plugin.getMessages().sendRaw(player, "&7Descrizione: &f" + (f.getDescription().isEmpty() ? "Nessuna" : f.getDescription()));
        plugin.getMessages().sendRaw(player, "&7Livello: &e" + f.getLevel() + " &7| Punti guerra: &e" + f.getWarPoints());
        plugin.getMessages().sendRaw(player, "&7Membri: &e" + f.getMembers().size() + "/" + maxMembers);
        plugin.getMessages().sendRaw(player, "&7Claim: &e" + claims + "/" + maxClaims);
        plugin.getMessages().sendRaw(player, "&7Banca: &e" + plugin.getEconomyManager().format(f.getBank()));
        plugin.getMessages().sendRaw(player, "&7Friendly Fire: " + (f.isFriendlyFire() ? "&aON" : "&cOFF") +
                " &7| Aperta: " + (f.isOpen() ? "&aSì" : "&cNo"));
        plugin.getMessages().sendRaw(player, "&7Shield: " + (f.isShieldEnabled() ? "&a" + f.getShieldStart() + " - " + f.getShieldEnd() : "&cDisabilitato"));
        if (war != null) plugin.getMessages().sendRaw(player, "&cGuerra attiva contro: &e" + war.getOpponent(f.getName()) +
                " &c(Punti: &e" + war.getPoints(f.getName()) + "&c)");
    }

    private void cmdList(Player player) {
        Collection<Faction> all = plugin.getFactionManager().getAllFactions();
        plugin.getMessages().sendRaw(player, "&6&lFazioni online (" + all.size() + "):");
        for (Faction f : all) {
            long online = f.getMembers().keySet().stream()
                    .filter(u -> Bukkit.getPlayer(u) != null).count();
            plugin.getMessages().sendRaw(player, "&7- &e" + f.getName() + " &7[" + f.getTag() + "] &7| Lv." +
                    f.getLevel() + " | &a" + online + "&7/" + f.getMembers().size() + " online");
        }
    }

    private void cmdClaim(Player player) {
        Faction f = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        if (f == null) { plugin.getMessages().sendNoFaction(player); return; }
        if (!f.getRole(player.getUniqueId()).isAtLeast(FactionRole.OFFICER)) { plugin.getMessages().sendNoPermission(player); return; }
        if (!plugin.getConfigManager().getClaimEnabledWorlds().contains(player.getWorld().getName())) {
            plugin.getMessages().send(player, "&cI claim non sono abilitati in questo mondo."); return;
        }
        if (plugin.getClaimManager().getClaimAt(player.getLocation()) != null) {
            plugin.getMessages().send(player, "&cQuesto chunk è già stato claimato."); return;
        }
        double cost = plugin.getConfigManager().getClaimCost();
        if (!plugin.getEconomyManager().withdraw(player, cost)) { plugin.getMessages().sendNotEnoughMoney(player, cost); return; }
        if (!plugin.getClaimManager().claim(f, player.getChunk())) {
            plugin.getEconomyManager().deposit(player, cost);
            plugin.getMessages().send(player, "&cHai raggiunto il limite massimo di claim per questo livello."); return;
        }
        plugin.getFactionManager().saveFaction(f);
        int current = plugin.getClaimManager().getClaimCount(f.getName());
        int max = plugin.getFactionManager().getClaimLimit(f);
        plugin.getMessages().send(player, "&aChunk claimato! &7(" + current + "/" + max + ") Costo: &e" + plugin.getEconomyManager().format(cost));
    }

    private void cmdUnclaim(Player player) {
        Faction f = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        if (f == null) { plugin.getMessages().sendNoFaction(player); return; }
        if (!f.getRole(player.getUniqueId()).isAtLeast(FactionRole.OFFICER)) { plugin.getMessages().sendNoPermission(player); return; }
        if (!plugin.getClaimManager().unclaim(f, player.getChunk())) {
            plugin.getMessages().send(player, "&cQuesto chunk non è di proprietà della tua fazione."); return;
        }
        plugin.getMessages().send(player, "&aChunk rimosso dalla fazione.");
    }

    private void cmdDesc(Player player, String[] args) {
        if (args.length < 2) { plugin.getMessages().sendUsage(player, "/f desc <testo>"); return; }
        Faction f = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        if (f == null) { plugin.getMessages().sendNoFaction(player); return; }
        if (!f.getRole(player.getUniqueId()).isAtLeast(FactionRole.OFFICER)) { plugin.getMessages().sendNoPermission(player); return; }
        String desc = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (desc.length() > plugin.getConfigManager().getMaxDescLength()) {
            plugin.getMessages().send(player, "&cDescrizione troppo lunga (max " + plugin.getConfigManager().getMaxDescLength() + " caratteri)."); return;
        }
        f.setDescription(desc);
        plugin.getFactionManager().saveFaction(f);
        plugin.getMessages().send(player, "&aDescrizione aggiornata.");
    }

    private void cmdTag(Player player, String[] args) {
        if (args.length < 2) { plugin.getMessages().sendUsage(player, "/f tag <tag>"); return; }
        Faction f = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        if (f == null) { plugin.getMessages().sendNoFaction(player); return; }
        if (!f.getLeader().equals(player.getUniqueId())) { plugin.getMessages().sendNoPermission(player); return; }
        String tag = args[1];
        if (tag.length() < plugin.getConfigManager().getMinTagLength() || tag.length() > plugin.getConfigManager().getMaxTagLength()) {
            plugin.getMessages().send(player, "&cTag non valido (lunghezza: " + plugin.getConfigManager().getMinTagLength() + "-" + plugin.getConfigManager().getMaxTagLength() + ")."); return;
        }
        f.setTag(tag);
        plugin.getFactionManager().saveFaction(f);
        plugin.getMessages().send(player, "&aTag aggiornato a: &e[" + tag + "]");
    }

    private void cmdFriendlyFire(Player player) {
        Faction f = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        if (f == null) { plugin.getMessages().sendNoFaction(player); return; }
        if (!f.getLeader().equals(player.getUniqueId())) { plugin.getMessages().sendNoPermission(player); return; }
        f.setFriendlyFire(!f.isFriendlyFire());
        plugin.getFactionManager().saveFaction(f);
        plugin.getMessages().broadcastFaction(f, "&7Friendly fire: " + (f.isFriendlyFire() ? "&aON" : "&cOFF"));
    }

    private void cmdOpen(Player player) {
        Faction f = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        if (f == null) { plugin.getMessages().sendNoFaction(player); return; }
        if (!f.getLeader().equals(player.getUniqueId())) { plugin.getMessages().sendNoPermission(player); return; }
        f.setOpen(!f.isOpen());
        plugin.getFactionManager().saveFaction(f);
        plugin.getMessages().broadcastFaction(f, "&7Fazione " + (f.isOpen() ? "&aaperta (chiunque può entrare)" : "&csu invito"));
    }

    private void cmdBank(Player player, String[] args) {
        if (args.length < 2) {
            Faction f = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
            if (f == null) { plugin.getMessages().sendNoFaction(player); return; }
            plugin.getMessages().send(player, "&7Banca fazione: &e" + plugin.getEconomyManager().format(f.getBank()));
            plugin.getMessages().sendRaw(player, "&7/f bank deposit <importo> | /f bank withdraw <importo>");
            return;
        }
        Faction f = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        if (f == null) { plugin.getMessages().sendNoFaction(player); return; }
        switch (args[1].toLowerCase()) {
            case "deposit", "deposita" -> {
                if (args.length < 3) { plugin.getMessages().sendUsage(player, "/f bank deposit <importo>"); return; }
                double amount;
                try { amount = Double.parseDouble(args[2]); } catch (NumberFormatException e) { plugin.getMessages().send(player, "&cImporto non valido."); return; }
                if (amount <= 0) { plugin.getMessages().send(player, "&cL'importo deve essere positivo."); return; }
                if (!plugin.getEconomyManager().withdraw(player, amount)) { plugin.getMessages().sendNotEnoughMoney(player, amount); return; }
                f.depositBank(amount);
                plugin.getFactionManager().saveFaction(f);
                plugin.getMessages().send(player, "&aDepositati &e" + plugin.getEconomyManager().format(amount) + " &anella banca.");
            }
            case "withdraw", "preleva" -> {
                if (args.length < 3) { plugin.getMessages().sendUsage(player, "/f bank withdraw <importo>"); return; }
                if (!f.getRole(player.getUniqueId()).isAtLeast(FactionRole.OFFICER)) { plugin.getMessages().sendNoPermission(player); return; }
                double amount;
                try { amount = Double.parseDouble(args[2]); } catch (NumberFormatException e) { plugin.getMessages().send(player, "&cImporto non valido."); return; }
                if (!f.withdrawBank(amount)) { plugin.getMessages().send(player, "&cFondi insufficienti nella banca."); return; }
                double tax = amount * plugin.getConfigManager().getBankTaxPercent() / 100.0;
                double net = amount - tax;
                plugin.getEconomyManager().deposit(player, net);
                plugin.getFactionManager().saveFaction(f);
                plugin.getMessages().send(player, "&aPrelevati &e" + plugin.getEconomyManager().format(net) + (tax > 0 ? " &7(tassa: " + plugin.getEconomyManager().format(tax) + ")" : ""));
            }
            default -> plugin.getMessages().sendUsage(player, "/f bank [deposit|withdraw] <importo>");
        }
    }

    private void cmdWar(Player player, String[] args) {
        if (args.length < 2) { plugin.getMessages().sendUsage(player, "/f war <fazione>"); return; }
        Faction attacker = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        if (attacker == null) { plugin.getMessages().sendNoFaction(player); return; }
        if (!attacker.getLeader().equals(player.getUniqueId())) { plugin.getMessages().sendNoPermission(player); return; }
        Faction defender = plugin.getFactionManager().getFaction(args[1]);
        if (defender == null) { plugin.getMessages().sendFactionNotFound(player); return; }
        if (attacker.getName().equalsIgnoreCase(defender.getName())) { plugin.getMessages().send(player, "&cNon puoi dichiarare guerra a te stesso."); return; }
        if (plugin.getWarManager().getActiveWar(attacker.getName()) != null) {
            plugin.getMessages().send(player, "&cSei già in guerra."); return;
        }
        if (plugin.getWarManager().isOnWarCooldown(attacker.getName(), defender.getName())) {
            plugin.getMessages().send(player, "&cSei in cooldown per dichiarare guerra a questa fazione."); return;
        }
        WarData war = plugin.getWarManager().declareWar(attacker, defender);
        plugin.getMessages().broadcastFaction(attacker, "&c&lGUERRA dichiarata contro &e" + defender.getName() + "&c!");
        plugin.getMessages().broadcastFaction(defender, "&c&l" + attacker.getName() + " &c&lha dichiarato guerra alla vostra fazione!");
        for (Player p : Bukkit.getOnlinePlayers()) {
            plugin.getMessages().send(p, "&c" + attacker.getName() + " &cha dichiarato guerra a &c" + defender.getName() + "!");
        }
    }

    private void cmdEndWar(Player player, String[] args) {
        Faction f = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        if (f == null) { plugin.getMessages().sendNoFaction(player); return; }
        if (!f.getLeader().equals(player.getUniqueId())) { plugin.getMessages().sendNoPermission(player); return; }
        WarData war = plugin.getWarManager().getActiveWar(f.getName());
        if (war == null) { plugin.getMessages().send(player, "&cNon sei in guerra."); return; }
        // Determine winner by points
        String winner = war.getPoints(f.getName()) >= war.getPoints(war.getOpponent(f.getName()))
                ? f.getName() : war.getOpponent(f.getName());
        plugin.getWarManager().endWar(war, winner);
        for (Player p : Bukkit.getOnlinePlayers()) {
            plugin.getMessages().send(p, "&7Guerra tra &e" + war.getFaction1() + " &7e &e" + war.getFaction2() + " &7terminata. Vincitore: &a" + winner);
        }
    }

    private void cmdShield(Player player, String[] args) {
        Faction f = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        if (f == null) { plugin.getMessages().sendNoFaction(player); return; }
        if (!f.getLeader().equals(player.getUniqueId())) { plugin.getMessages().sendNoPermission(player); return; }
        if (args.length == 1) {
            f.setShieldEnabled(!f.isShieldEnabled());
            plugin.getFactionManager().saveFaction(f);
            plugin.getMessages().send(player, "&7Shield: " + (f.isShieldEnabled() ? "&aAbilitato (" + f.getShieldStart() + " - " + f.getShieldEnd() + ")" : "&cDisabilitato"));
            return;
        }
        if (args.length < 3) { plugin.getMessages().sendUsage(player, "/f shield [start|end] <HH:mm>"); return; }
        String time = args[2];
        if (!plugin.getShieldManager().isValidTime(time)) { plugin.getMessages().send(player, "&cFormato orario non valido (HH:mm)."); return; }
        if (args[1].equalsIgnoreCase("start")) { f.setShieldStart(time); plugin.getMessages().send(player, "&aShield start impostato a &e" + time); }
        else if (args[1].equalsIgnoreCase("end")) { f.setShieldEnd(time); plugin.getMessages().send(player, "&aShield end impostato a &e" + time); }
        else { plugin.getMessages().sendUsage(player, "/f shield [start|end] <HH:mm>"); return; }
        plugin.getFactionManager().saveFaction(f);
    }

    private void cmdMap(Player player) {
        it.fazionicore.model.ClaimData center = plugin.getClaimManager().getClaimAt(player.getLocation());
        String centerFaction = center != null ? center.getFactionName() : "Wilderness";
        plugin.getMessages().sendRaw(player, "&6&l=== Mappa chunk ===");
        int cx = player.getChunk().getX();
        int cz = player.getChunk().getZ();
        for (int dz = -3; dz <= 3; dz++) {
            StringBuilder row = new StringBuilder();
            for (int dx = -5; dx <= 5; dx++) {
                it.fazionicore.model.ClaimData cd = plugin.getClaimManager().getClaimAt(
                        new org.bukkit.Location(player.getWorld(), (cx + dx) * 16.0, 64, (cz + dz) * 16.0));
                if (dx == 0 && dz == 0) row.append("&6+");
                else if (cd == null) row.append("&7-");
                else if (cd.getFactionName().equalsIgnoreCase(centerFaction)) row.append("&a#");
                else row.append("&c#");
            }
            plugin.getMessages().sendRaw(player, row.toString());
        }
        plugin.getMessages().sendRaw(player, "&7Sei in: &e" + centerFaction);
    }

    private void handleMap(Player player, String[] args) {
        int radius = 5; // Default

        if (args.length > 1) {
            try {
                radius = Integer.parseInt(args[1]);
                if (radius < 1 || radius > 10) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() + org.bukkit.ChatColor.RED +
                            "Il raggio deve essere tra 1 e 10!");
                    return;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getConfigManager().getPrefix() + org.bukkit.ChatColor.RED +
                        "Raggio non valido!");
                return;
            }
        }

        List<String> map = plugin.getChunkMapGenerator().generateMap(player, radius);
        for (String line : map) {
            player.sendMessage(line);
        }
    }

    private void cmdReload(Player player) {
        if (!player.hasPermission("fazionicore.admin")) { plugin.getMessages().sendNoPermission(player); return; }
        plugin.getConfigManager().reload();
        plugin.getMessages().send(player, "&aConfigurazione ricaricata.");
    }

    private void sendHelp(Player player) {
        plugin.getMessages().sendRaw(player, "&6&l=== FazioniCore - Comandi ===");
        plugin.getMessages().sendRaw(player, "&e/f create <nome> <tag> &7- Crea fazione");
        plugin.getMessages().sendRaw(player, "&e/f disband &7- Sciogli fazione");
        plugin.getMessages().sendRaw(player, "&e/f invite <giocatore> &7- Invita giocatore");
        plugin.getMessages().sendRaw(player, "&e/f join <fazione> &7- Entra in fazione");
        plugin.getMessages().sendRaw(player, "&e/f leave &7- Lascia fazione");
        plugin.getMessages().sendRaw(player, "&e/f kick <giocatore> &7- Espelli membro");
        plugin.getMessages().sendRaw(player, "&e/f promote/demote <giocatore> &7- Cambia rango");
        plugin.getMessages().sendRaw(player, "&e/f info [fazione] &7- Info fazione");
        plugin.getMessages().sendRaw(player, "&e/f list &7- Lista fazioni");
        plugin.getMessages().sendRaw(player, "&e/f claim/unclaim &7- Gestisci territori");
        plugin.getMessages().sendRaw(player, "&e/f map &7- Mappa chunk");
        plugin.getMessages().sendRaw(player, "&e/f desc <testo> &7- Imposta descrizione");
        plugin.getMessages().sendRaw(player, "&e/f tag <tag> &7- Cambia tag");
        plugin.getMessages().sendRaw(player, "&e/f ff &7- Toggle friendly fire");
        plugin.getMessages().sendRaw(player, "&e/f open &7- Toggle fazione aperta");
        plugin.getMessages().sendRaw(player, "&e/f bank [deposit|withdraw] &7- Banca fazione");
        plugin.getMessages().sendRaw(player, "&e/f war <fazione> &7- Dichiara guerra");
        plugin.getMessages().sendRaw(player, "&e/f endwar &7- Termina guerra");
        plugin.getMessages().sendRaw(player, "&e/f shield [start|end] <HH:mm> &7- Gestisci shield");
        plugin.getMessages().sendRaw(player, org.bukkit.ChatColor.YELLOW + "/f map [raggio]" + org.bukkit.ChatColor.GRAY + " - Mostra mappa ASCII dei territori");
    }

    private UUID getOfflineUUID(String name) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(name);
        return op.hasPlayedBefore() ? op.getUniqueId() : null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create","disband","invite","join","leave","kick","promote","demote",
                    "info","list","claim","unclaim","desc","tag","ff","open","bank","war","endwar","shield","map","reload")
                    .stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2) {
            if (Arrays.asList("invite","kick","promote","demote").contains(args[0].toLowerCase())) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).toList();
            }
            if (Arrays.asList("info","join","war").contains(args[0].toLowerCase())) {
                return plugin.getFactionManager().getAllFactions().stream().map(Faction::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).toList();
            }
            if (args[0].equalsIgnoreCase("bank")) return Arrays.asList("deposit","withdraw");
            if (args[0].equalsIgnoreCase("shield")) return Arrays.asList("start","end");
        }
        return Collections.emptyList();
    }
}
