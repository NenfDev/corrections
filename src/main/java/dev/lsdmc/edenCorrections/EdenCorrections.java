package dev.lsdmc.edenCorrections;

import dev.lsdmc.edenCorrections.managers.SpamControlManager;
import dev.lsdmc.edenCorrections.managers.UnauthorizedAccessSpamManager;
import dev.lsdmc.edenCorrections.config.ConfigManager;
import dev.lsdmc.edenCorrections.managers.DutyManager;
import dev.lsdmc.edenCorrections.managers.WantedManager;
import dev.lsdmc.edenCorrections.managers.ChaseManager;
import dev.lsdmc.edenCorrections.managers.JailManager;
import dev.lsdmc.edenCorrections.managers.MessageManager;
import dev.lsdmc.edenCorrections.managers.ContrabandManager;
import dev.lsdmc.edenCorrections.managers.DutyBankingManager;
import dev.lsdmc.edenCorrections.managers.SecurityManager;
import dev.lsdmc.edenCorrections.managers.BossBarManager;
import dev.lsdmc.edenCorrections.managers.GuardLootManager;
import dev.lsdmc.edenCorrections.managers.ProgressionManager;
import dev.lsdmc.edenCorrections.managers.LockerManager;
import dev.lsdmc.edenCorrections.managers.BuybackNpcManager;
import dev.lsdmc.edenCorrections.storage.DataManager;
import dev.lsdmc.edenCorrections.events.GuardEventHandler;
import dev.lsdmc.edenCorrections.commands.CommandHandler;
import dev.lsdmc.edenCorrections.integrations.EdenCorrectionsExpansion;
import dev.lsdmc.edenCorrections.utils.WorldGuardUtils;
import dev.lsdmc.edenCorrections.utils.LoggingUtils;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;

import java.util.logging.Logger;
import java.util.Map;

public class EdenCorrections extends JavaPlugin {
    
    private static EdenCorrections instance;
    private Logger logger;
    
    
    private ConfigManager configManager;
    private DataManager dataManager;
    private MessageManager messageManager;
    private WorldGuardUtils worldGuardUtils;
    
    
    private DutyManager dutyManager;
    private WantedManager wantedManager;
    private ChaseManager chaseManager;
    private JailManager jailManager;
    private ContrabandManager contrabandManager;
    private DutyBankingManager dutyBankingManager;
    
    
    private SecurityManager securityManager;
    private BossBarManager bossBarManager;
    private GuardLootManager guardLootManager;
    private ProgressionManager progressionManager;
    private LockerManager lockerManager;
    
    
    private SpamControlManager spamControlManager;
    private UnauthorizedAccessSpamManager unauthorizedAccessSpamManager;
    
    
    private GuardEventHandler eventHandler;
    
    
    private CommandHandler commandHandler;
    
    
    private EdenCorrectionsExpansion placeholderExpansion;
    
    
    private dev.lsdmc.edenCorrections.integrations.CMIIntegration cmiIntegration;
    
    
    
    
    private dev.lsdmc.edenCorrections.integrations.VaultEconomyManager vaultEconomyManager;

    
    private BuybackNpcManager buybackNpcManager;
    
    
    private dev.lsdmc.edenCorrections.utils.PerformanceMonitor performanceMonitor;
    
    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        
        try {
            LoggingUtils.info(logger, "Starting Corrections v" + getPluginMeta().getVersion());
            
            
            initializeCore();
            
            
            initializeManagers();
            
            
            registerEventsAndCommands();
            
            
            registerPlaceholderAPI();
            
            
            performStartupValidation();
            
            
            logSystemStats();
            
            
            messageManager.sendMessage(getServer().getConsoleSender(), "system.startup");
            
            LoggingUtils.info(logger, "Corrections enabled successfully!");
            
        } catch (Exception e) {
            LoggingUtils.error(logger, "Failed to enable Corrections: " + e.getMessage());
            e.printStackTrace();
            
            
            try {
                cleanupAllManagers();
            } catch (Exception cleanupException) {
                LoggingUtils.error(logger, "Error during emergency cleanup: " + cleanupException.getMessage());
            }
            
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        LoggingUtils.info(logger, "Disabling Corrections...");
        
        try {
            
            cleanupAllManagers();
            
            LoggingUtils.info(logger, "Corrections disabled successfully!");
            
        } catch (Exception e) {
            LoggingUtils.error(logger, "Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void cleanupAllManagers() {
        
        if (commandHandler != null) {
            LoggingUtils.info(logger, "Cleaning up command handler...");
        }
        
        if (eventHandler != null) {
            LoggingUtils.info(logger, "Cleaning up event handler...");
        }
        
        if (spamControlManager != null) {
            try {
                spamControlManager.cleanup();
            } catch (Exception e) {
                LoggingUtils.warn(logger, "Error cleaning up SpamControlManager: " + e.getMessage());
            }
        }
        
        if (guardLootManager != null) {
            LoggingUtils.info(logger, "Cleaning up GuardLootManager...");
            
        }
        
        
        
        if (dutyBankingManager != null) {
            try {
                dutyBankingManager.cleanup();
            } catch (Exception e) {
                LoggingUtils.warn(logger, "Error cleaning up DutyBankingManager: " + e.getMessage());
            }
        }
        
        if (bossBarManager != null) {
            try {
                bossBarManager.cleanup();
            } catch (Exception e) {
                LoggingUtils.warn(logger, "Error cleaning up BossBarManager: " + e.getMessage());
            }
        }
        
        if (securityManager != null) {
            try {
                securityManager.cleanup();
            } catch (Exception e) {
                LoggingUtils.warn(logger, "Error cleaning up SecurityManager: " + e.getMessage());
            }
        }
        
        if (contrabandManager != null) {
            try {
                contrabandManager.cleanup();
            } catch (Exception e) {
                LoggingUtils.warn(logger, "Error cleaning up ContrabandManager: " + e.getMessage());
            }
        }
        
        if (jailManager != null) {
            LoggingUtils.info(logger, "Cleaning up JailManager...");
            
        }
        
        if (chaseManager != null) {
            try {
                chaseManager.shutdown();
            } catch (Exception e) {
                LoggingUtils.warn(logger, "Error cleaning up ChaseManager: " + e.getMessage());
            }
        }
        
        if (wantedManager != null) {
            try {
                wantedManager.cleanup();
            } catch (Exception e) {
                LoggingUtils.warn(logger, "Error cleaning up WantedManager: " + e.getMessage());
            }
        }
        
        if (dutyManager != null) {
            try {
                dutyManager.cleanup();
            } catch (Exception e) {
                LoggingUtils.warn(logger, "Error cleaning up DutyManager: " + e.getMessage());
            }
        }
        
        if (messageManager != null) {
            try {
                messageManager.cleanup();
            } catch (Exception e) {
                LoggingUtils.warn(logger, "Error cleaning up MessageManager: " + e.getMessage());
            }
        }
        
        if (dataManager != null) {
            try {
                dataManager.shutdown();
            } catch (Exception e) {
                LoggingUtils.warn(logger, "Error cleaning up DataManager: " + e.getMessage());
            }
        }
        
        if (worldGuardUtils != null) {
            LoggingUtils.info(logger, "Cleaning up WorldGuardUtils...");
            
        }
        
        
        if (cmiIntegration != null) {
            try {
                cmiIntegration.cleanup();
                LoggingUtils.info(logger, "Cleaned up CMI integration");
            } catch (Exception e) {
                LoggingUtils.warn(logger, "Error cleaning up CMI integration: " + e.getMessage());
            }
        }

        if (buybackNpcManager != null) {
            try {
                buybackNpcManager.cleanup();
                LoggingUtils.info(logger, "Cleaned up Buyback NPC manager");
            } catch (Exception e) {
                LoggingUtils.warn(logger, "Error cleaning up Buyback NPC manager: " + e.getMessage());
            }
        }
    }
    
    private void initializeCore() {
        
        configManager = new ConfigManager(this);
        
        
        if (!configManager.isConfigValid()) {
            LoggingUtils.warn(logger, "Configuration validation failed - some features may not work correctly");
            LoggingUtils.warn(logger, "Validation errors found:");
            for (String error : configManager.getValidationErrors()) {
                LoggingUtils.warn(logger, "  - " + error);
            }
        }
        
        
        messageManager = new MessageManager(this);
        messageManager.initialize();
        
        
        dataManager = new DataManager(this);
        dataManager.initialize();
        
        
        worldGuardUtils = new WorldGuardUtils(this);

        // Backward-compatible data folder migration: EdenCorrections -> Corrections
        try {
            java.io.File oldDir = new java.io.File(getDataFolder().getParentFile(), "EdenCorrections");
            java.io.File newDir = new java.io.File(getDataFolder().getParentFile(), "Corrections");
            if (oldDir.exists() && (!newDir.exists() || (newDir.isDirectory() && newDir.list() != null && newDir.list().length == 0))) {
                if (!newDir.exists() && !newDir.mkdirs()) {
                    LoggingUtils.warn(logger, "Could not create Corrections data folder; continuing to use existing");
                }
                copyDirectory(oldDir, newDir);
                LoggingUtils.info(logger, "Detected existing EdenCorrections data; copied to Corrections folder");
            }
        } catch (Exception ex) {
            LoggingUtils.warn(logger, "Data folder migration warning: " + ex.getMessage());
        }
    }

    private static void copyDirectory(java.io.File source, java.io.File target) throws java.io.IOException {
        java.nio.file.Path src = source.toPath();
        java.nio.file.Path dst = target.toPath();
        java.nio.file.Files.walk(src).forEach(path -> {
            try {
                java.nio.file.Path rel = src.relativize(path);
                java.nio.file.Path dest = dst.resolve(rel);
                if (java.nio.file.Files.isDirectory(path)) {
                    if (!java.nio.file.Files.exists(dest)) {
                        java.nio.file.Files.createDirectories(dest);
                    }
                } else {
                    if (!java.nio.file.Files.exists(dest)) {
                        java.nio.file.Files.copy(path, dest);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    private void initializeManagers() {
        
        dutyManager = new DutyManager(this);
        wantedManager = new WantedManager(this);
        chaseManager = new ChaseManager(this);
        jailManager = new JailManager(this);
        contrabandManager = new ContrabandManager(this);
        dutyBankingManager = new DutyBankingManager(this);
        progressionManager = new ProgressionManager(this);
        lockerManager = new LockerManager(this);
        
        
        securityManager = new SecurityManager(this);
        bossBarManager = new BossBarManager(this);
        guardLootManager = new GuardLootManager(this);
        spamControlManager = new SpamControlManager(this);
        unauthorizedAccessSpamManager = new UnauthorizedAccessSpamManager(this);
        
        
        performanceMonitor = new dev.lsdmc.edenCorrections.utils.PerformanceMonitor(this);
        
        
        dutyManager.initialize();
        wantedManager.initialize();
        chaseManager.initialize();
        jailManager.initialize();
        contrabandManager.initialize();
        dutyBankingManager.initialize();
        progressionManager.initialize();
        securityManager.initialize();
        bossBarManager.initialize();
        guardLootManager.initialize();
        spamControlManager.initialize();
        
        
        
        performanceMonitor.initialize();
        
        
        cmiIntegration = new dev.lsdmc.edenCorrections.integrations.CMIIntegration(this);
        
        
        
        vaultEconomyManager = new dev.lsdmc.edenCorrections.integrations.VaultEconomyManager(this);
        
        
        cmiIntegration.testIntegration();
        vaultEconomyManager.initialize();

        
        buybackNpcManager = new BuybackNpcManager(this);
        buybackNpcManager.initialize();
    }
    
    private void registerEventsAndCommands() {
        
        eventHandler = new GuardEventHandler(this);
        getServer().getPluginManager().registerEvents(eventHandler, this);
        
        
        commandHandler = new CommandHandler(this);
        commandHandler.registerCommands();

        // Notify about identifier migration (one-time informative log)
        try {
            LoggingUtils.info(logger, "Both 'corrections.*' and 'edencorrections.*' permissions are supported. Please migrate to 'corrections.*'.");
        } catch (Exception ignored) {}
    }
    
    private void registerPlaceholderAPI() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                placeholderExpansion = new EdenCorrectionsExpansion(this);
                placeholderExpansion.register();
                try {
                    new dev.lsdmc.edenCorrections.integrations.EdenCorrectionsLegacyExpansion(this).register();
                } catch (Throwable ignored) { }
                LoggingUtils.info(logger, "PlaceholderAPI expansions registered (corrections + legacy edencorrections)");
            } catch (Exception e) {
                LoggingUtils.warn(logger, "Failed to register PlaceholderAPI expansion: " + e.getMessage());
            }
        } else {
            LoggingUtils.info(logger, "PlaceholderAPI not found - placeholder integration disabled");
        }
    }
    
    public void reload() {
        try {
            LoggingUtils.info(logger, "Reloading Corrections configuration...");
            
            configManager.reload();
            messageManager.reload();
            
            
            
            
            
            
            if (buybackNpcManager != null) {
                buybackNpcManager.reload();
            }

            
            if (!configManager.isConfigValid()) {
                LoggingUtils.warn(logger, "Configuration validation failed after reload - some features may not work correctly");
                LoggingUtils.warn(logger, "Validation errors found:");
                for (String error : configManager.getValidationErrors()) {
                    LoggingUtils.warn(logger, "  - " + error);
                }
            }
            
            LoggingUtils.info(logger, "Configuration and messages reloaded successfully!");
            
        } catch (Exception e) {
            LoggingUtils.error(logger, "Failed to reload configuration: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Configuration reload failed", e);
        }
    }
    
    public boolean validateConfiguration() {
        
        return configManager.isConfigValid();
    }
    
    private void performStartupValidation() {
        try {
            LoggingUtils.info(logger, "Performing startup validation...");
            
            boolean hasErrors = false;
            
            
            if (configManager == null) {
                LoggingUtils.error(logger, "ConfigManager failed to initialize!");
                hasErrors = true;
            }
            
            if (dataManager == null) {
                LoggingUtils.error(logger, "DataManager failed to initialize!");
                hasErrors = true;
            }
            
            if (messageManager == null) {
                LoggingUtils.error(logger, "MessageManager failed to initialize!");
                hasErrors = true;
            }
            
            
            try {
                messageManager.validateMessages();
            } catch (Exception e) {
                LoggingUtils.warn(logger, "Message validation issues detected: " + e.getMessage());
            }
            
            
            validatePluginIntegrations();

            
            try {
                if (bossBarManager != null) {
                    
                    for (Player p : getServer().getOnlinePlayers()) {
                        bossBarManager.hideBossBar(p);
                    }
                }
                if (dutyManager != null) {
                    
                    for (Player p : getServer().getOnlinePlayers()) {
                        if (dutyManager.isInDutyTransition(p)) {
                            dutyManager.cancelDutyTransition(p, null);
                        }
                    }
                }
                if (jailManager != null) {
                    
                    for (Player p : getServer().getOnlinePlayers()) {
                        if (jailManager.isInJailCountdown(p)) {
                            jailManager.cancelJailCountdown(p);
                        }
                    }
                }
            } catch (Exception ignored) { }
            
            if (hasErrors) {
                throw new RuntimeException("Critical startup validation failures detected");
            }
            
            LoggingUtils.info(logger, "Startup validation completed successfully");
            
        } catch (Exception e) {
            LoggingUtils.error(logger, "Startup validation failed: " + e.getMessage());
            throw new RuntimeException("Startup validation failed", e);
        }
    }
    
    private void validatePluginIntegrations() {
        
        if (getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                if (dutyManager != null) {
                    
                    LoggingUtils.info(logger, "LuckPerms integration verified");
                }
            } catch (Exception e) {
                LoggingUtils.warn(logger, "LuckPerms integration test failed: " + e.getMessage());
            }
        }
        
        
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            try {
                if (worldGuardUtils != null) {
                    LoggingUtils.info(logger, "WorldGuard integration verified - " + (worldGuardUtils.isWorldGuardEnabled() ? "Enabled" : "Disabled"));
                }
            } catch (Exception e) {
                LoggingUtils.warn(logger, "WorldGuard integration test failed: " + e.getMessage());
            }
        }
        
        
        if (getServer().getPluginManager().getPlugin("CoinsEngine") != null) {
            if (configManager.isDutyBankingEnabled()) {
                String currencyCommand = configManager.getCurrencyCommand();
                if (currencyCommand != null && !currencyCommand.trim().isEmpty()) {
                    LoggingUtils.info(logger, "CoinsEngine integration configured for duty banking");
                } else {
                    LoggingUtils.warn(logger, "CoinsEngine available but currency command not configured");
                }
            }
        } else if (configManager.isDutyBankingEnabled()) {
            LoggingUtils.warn(logger, "Duty banking enabled but CoinsEngine not found");
        }
        
        
        if (getServer().getPluginManager().getPlugin("CMI") != null) {
            LoggingUtils.info(logger, "CMI integration available for kit distribution");
        } else {
            LoggingUtils.warn(logger, "CMI not found - guard kits will not be given");
        }
        
        
        
        
        if (vaultEconomyManager != null && !vaultEconomyManager.isAvailable()) {
            LoggingUtils.warn(logger, "Vault Economy integration failed - penalty economy features will be disabled");
            LoggingUtils.info(logger, "Install Vault and an economy plugin (like EssentialsX) to enable penalty deductions");
        } else if (vaultEconomyManager != null && vaultEconomyManager.isAvailable()) {
            LoggingUtils.info(logger, "Vault Economy integration successful - penalty system ready");
        }
    }
    
    public void logSystemStats() {
        try {
        LoggingUtils.info(logger, "=== Corrections System Statistics ===");
            LoggingUtils.info(logger, "Version: " + getPluginMeta().getVersion());
            LoggingUtils.info(logger, "Debug Mode: " + configManager.isDebugMode());
            LoggingUtils.info(logger, "Online Players: " + getServer().getOnlinePlayers().size() + "/" + getServer().getMaxPlayers());
            
            if (dataManager != null) {
                LoggingUtils.info(logger, "Active Chases: " + dataManager.getAllActiveChases().size());
            }
            
            
            LoggingUtils.info(logger, "LuckPerms Integration: " + (getServer().getPluginManager().getPlugin("LuckPerms") != null ? "Available" : "Not Found"));
            LoggingUtils.info(logger, "WorldGuard Integration: " + (getServer().getPluginManager().getPlugin("WorldGuard") != null ? "Available" : "Not Found"));
            LoggingUtils.info(logger, "CoinsEngine Integration: " + (getServer().getPluginManager().getPlugin("CoinsEngine") != null ? "Available" : "Not Found"));
            LoggingUtils.info(logger, "CMI Integration: " + (getServer().getPluginManager().getPlugin("CMI") != null ? "Available" : "Not Found"));
            LoggingUtils.info(logger, "UNT Integration: " + (getServer().getPluginManager().getPlugin("UnlimitedNameTags") != null ? "Available" : "Not Found"));
            
            LoggingUtils.info(logger, "=== End System Statistics ===");
            
        } catch (Exception e) {
            LoggingUtils.warn(logger, "Error logging system statistics: " + e.getMessage());
        }
    }
    
    public void generateDiagnosticReport() {
        LoggingUtils.info(logger, "=== Corrections Comprehensive Diagnostic Report ===");
        
        try {
            
            LoggingUtils.info(logger, "Plugin Version: " + getPluginMeta().getVersion());
            LoggingUtils.info(logger, "Server Version: " + getServer().getVersion());
            LoggingUtils.info(logger, "Bukkit Version: " + getServer().getBukkitVersion());
            
            
            LoggingUtils.info(logger, "Configuration Valid: " + configManager.isConfigValid());
            if (!configManager.isConfigValid()) {
                LoggingUtils.info(logger, "Configuration Errors:");
                for (String error : configManager.getValidationErrors()) {
                    LoggingUtils.info(logger, "  - " + error);
                }
            }
            
            
            LoggingUtils.info(logger, "DutyManager: " + (dutyManager != null ? "Initialized" : "Not Initialized"));
            LoggingUtils.info(logger, "WantedManager: " + (wantedManager != null ? "Initialized" : "Not Initialized"));
            LoggingUtils.info(logger, "ChaseManager: " + (chaseManager != null ? "Initialized" : "Not Initialized"));
            LoggingUtils.info(logger, "JailManager: " + (jailManager != null ? "Initialized" : "Not Initialized"));
            LoggingUtils.info(logger, "ContrabandManager: " + (contrabandManager != null ? "Initialized" : "Not Initialized"));
            LoggingUtils.info(logger, "DutyBankingManager: " + (dutyBankingManager != null ? "Initialized" : "Not Initialized"));
            
            
            if (messageManager != null) {
                LoggingUtils.info(logger, "MessageManager PlaceholderAPI: " + messageManager.isPlaceholderAPIEnabled());
                LoggingUtils.info(logger, "Missing Messages: " + messageManager.getMissingMessages().size());
                LoggingUtils.info(logger, "Invalid Messages: " + messageManager.getInvalidMessages().size());
                
                
                messageManager.logMissingMessagesSummary();
                
                if (configManager.isDebugMode()) {
                    LoggingUtils.info(logger, "Message Usage Statistics:");
                    messageManager.getMessageUsageStats().entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .limit(10)
                        .forEach(entry -> LoggingUtils.info(logger, "  " + entry.getKey() + ": " + entry.getValue()));
                }
            }
            
            
            LoggingUtils.info(logger, "=== Integration Status ===");
            LoggingUtils.info(logger, "LuckPerms: " + (getServer().getPluginManager().getPlugin("LuckPerms") != null ? "Available" : "Not Found"));
            LoggingUtils.info(logger, "WorldGuard: " + (getServer().getPluginManager().getPlugin("WorldGuard") != null ? "Available" : "Not Found"));
            LoggingUtils.info(logger, "PlaceholderAPI: " + (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null ? "Available" : "Not Found"));
            LoggingUtils.info(logger, "CoinsEngine: " + (getServer().getPluginManager().getPlugin("CoinsEngine") != null ? "Available" : "Not Found"));
            LoggingUtils.info(logger, "CMI: " + (getServer().getPluginManager().getPlugin("CMI") != null ? "Available" : "Not Found"));
            LoggingUtils.info(logger, "UNT: " + (getServer().getPluginManager().getPlugin("UnlimitedNameTags") != null ? "Available" : "Not Found"));
            
            
            if (worldGuardUtils != null) {
                worldGuardUtils.generateDiagnosticReport();
            }
            
            
            if (dataManager != null) {
                LoggingUtils.info(logger, "=== Runtime Statistics ===");
                LoggingUtils.info(logger, "Active Chases: " + dataManager.getAllActiveChases().size());
                LoggingUtils.info(logger, "Online Players: " + getServer().getOnlinePlayers().size());
                
                int guardsOnDuty = 0;
                int wantedPlayers = 0;
                for (Player player : getServer().getOnlinePlayers()) {
                    if (dutyManager != null && dutyManager.isOnDuty(player)) {
                        guardsOnDuty++;
                    }
                    if (wantedManager != null && wantedManager.isWanted(player)) {
                        wantedPlayers++;
                    }
                }
                LoggingUtils.info(logger, "Guards on Duty: " + guardsOnDuty);
                LoggingUtils.info(logger, "Wanted Players: " + wantedPlayers);
            }
            
            
            LoggingUtils.info(logger, "=== Configuration Summary ===");
            LoggingUtils.info(logger, "Debug Mode: " + configManager.isDebugMode());
            LoggingUtils.info(logger, "Contraband Enabled: " + configManager.isContrabandEnabled());
            LoggingUtils.info(logger, "Duty Banking Enabled: " + configManager.isDutyBankingEnabled());
            LoggingUtils.info(logger, "Penalty Escalation Enabled: " + configManager.isPenaltyEscalationEnabled());
            LoggingUtils.info(logger, "Jail-Chase Integration Enabled: " + configManager.isJailChaseIntegrationEnabled());
            LoggingUtils.info(logger, "Guard Tags Enabled: " + configManager.isGuardTagsEnabled());
            LoggingUtils.info(logger, "Wanted Indicators Enabled: " + configManager.isWantedTagsEnabled());
            LoggingUtils.info(logger, "Max Wanted Level: " + configManager.getMaxWantedLevel());
            LoggingUtils.info(logger, "Max Chase Distance: " + configManager.getMaxChaseDistance());
            LoggingUtils.info(logger, "Max Concurrent Chases: " + configManager.getMaxConcurrentChases());
            
            LoggingUtils.info(logger, "=== End Diagnostic Report ===");
            
        } catch (Exception e) {
            LoggingUtils.error(logger, "Error generating diagnostic report: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    public static EdenCorrections getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public DataManager getDataManager() {
        return dataManager;
    }
    
    public MessageManager getMessageManager() {
        return messageManager;
    }
    
    public DutyManager getDutyManager() {
        return dutyManager;
    }
    
    public WantedManager getWantedManager() {
        return wantedManager;
    }
    
    public ChaseManager getChaseManager() {
        return chaseManager;
    }
    
    public JailManager getJailManager() {
        return jailManager;
    }
    
    public ContrabandManager getContrabandManager() {
        return contrabandManager;
    }
    
    public DutyBankingManager getDutyBankingManager() {
        return dutyBankingManager;
    }
    
    public GuardEventHandler getEventHandler() {
        return eventHandler;
    }
    
    public CommandHandler getCommandHandler() {
        return commandHandler;
    }
    
    public WorldGuardUtils getWorldGuardUtils() {
        return worldGuardUtils;
    }
    
    public SecurityManager getSecurityManager() {
        return securityManager;
    }
    
    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }
    
    
    
    public GuardLootManager getGuardLootManager() {
        return guardLootManager;
    }
    
    public ProgressionManager getProgressionManager() {
        return progressionManager;
    }
    public LockerManager getLockerManager() {
        return lockerManager;
    }
    
    public SpamControlManager getSpamControlManager() {
        return spamControlManager;
    }
    
    public UnauthorizedAccessSpamManager getUnauthorizedAccessSpamManager() {
        return unauthorizedAccessSpamManager;
    }
    
    public dev.lsdmc.edenCorrections.integrations.CMIIntegration getCMIIntegration() {
        return cmiIntegration;
    }
    
    
    
    public dev.lsdmc.edenCorrections.integrations.VaultEconomyManager getVaultEconomyManager() {
        return vaultEconomyManager;
    }
    
    public BuybackNpcManager getBuybackNpcManager() {
        return buybackNpcManager;
    }
    
    public dev.lsdmc.edenCorrections.utils.PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }
} 