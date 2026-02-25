package it.fazionicore;

import it.fazionicore.command.ClaimCommand;
import it.fazionicore.command.FactionCommand;
import it.fazionicore.listener.PlayerListener;
import it.fazionicore.listener.ProtectionListener;
import it.fazionicore.manager.*;
import org.bukkit.plugin.java.JavaPlugin;

public final class FazioniCore extends JavaPlugin {

    private static FazioniCore instance;
    private ConfigManager configManager;
    private Messages messages;
    private EconomyManager economyManager;
    private DatabaseManager databaseManager;
    private FactionManager factionManager;
    private ClaimManager claimManager;
    private WarManager warManager;
    private ShieldManager shieldManager;
    private AntiInsideManager antiInsideManager;
    private ClaimTool claimTool;
    private ClaimVisualizer claimVisualizer;
    private ChunkMapGenerator chunkMapGenerator;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        configManager = new ConfigManager(this);
        messages = new Messages(this);
        economyManager = new EconomyManager(this);

        if (!economyManager.setup()) {
            getLogger().severe("Vault non trovato o nessuna economia registrata! Disabilitazione plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        factionManager = new FactionManager(this);
        claimManager = new ClaimManager(this);
        warManager = new WarManager(this);
        shieldManager = new ShieldManager(this);
        antiInsideManager = new AntiInsideManager(this);
        this.claimTool = new ClaimTool(this);
        this.claimVisualizer = new ClaimVisualizer(this);
        this.chunkMapGenerator = new ChunkMapGenerator(this);

        factionManager.loadAll();
        claimManager.loadAll();
        warManager.loadAll();

        FactionCommand factionCommand = new FactionCommand(this);
        getCommand("faction").setExecutor(factionCommand);
        getCommand("faction").setTabCompleter(factionCommand);

        getCommand("claim").setExecutor(new ClaimCommand(this));

        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        shieldManager.startTask();

        getLogger().info("FazioniCore v" + getDescription().getVersion() + " abilitato con successo!");
    }

    @Override
    public void onDisable() {
        shieldManager.stop();
        if (factionManager != null) factionManager.saveAll();
        if (claimManager != null) claimManager.saveAll();
        if (warManager != null) warManager.saveAll();
        if (databaseManager != null) databaseManager.close();
        getServer().getScheduler().cancelTasks(this);
        getLogger().info("FazioniCore disabilitato. Dati salvati.");
        if (claimVisualizer != null) {
            getServer().getOnlinePlayers().forEach(claimVisualizer::cleanupPlayer);
        }
    }

    public static FazioniCore getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public Messages getMessages() { return messages; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public FactionManager getFactionManager() { return factionManager; }
    public ClaimManager getClaimManager() { return claimManager; }
    public WarManager getWarManager() { return warManager; }
    public ShieldManager getShieldManager() { return shieldManager; }
    public AntiInsideManager getAntiInsideManager() { return antiInsideManager; }
    public ClaimTool getClaimTool() { return claimTool; }
    public ClaimVisualizer getClaimVisualizer() { return claimVisualizer; }
    public ChunkMapGenerator getChunkMapGenerator() { return chunkMapGenerator; }
}
