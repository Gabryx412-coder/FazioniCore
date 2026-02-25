package it.fazionicore;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {

    private final FazioniCore plugin;
    private Economy economy;

    public EconomyManager(FazioniCore plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public boolean has(OfflinePlayer player, double amount) {
        return economy.has(player, amount);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!has(player, amount)) return false;
        economy.withdrawPlayer(player, amount);
        return true;
    }

    public void deposit(OfflinePlayer player, double amount) {
        economy.depositPlayer(player, amount);
    }

    public double getBalance(OfflinePlayer player) {
        return economy.getBalance(player);
    }

    public String format(double amount) {
        return economy.format(amount);
    }

    public Economy getEconomy() { return economy; }
}
