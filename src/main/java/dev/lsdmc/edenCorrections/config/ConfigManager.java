package dev.lsdmc.edenCorrections.config;

import dev.lsdmc.edenCorrections.EdenCorrections;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Material;

import java.io.File;
import java.util.logging.Logger;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConfigManager {
    
    private static final int CONFIG_VERSION = 2;
    private static final String CONFIG_VERSION_PATH = "config-version";

    private final EdenCorrections plugin;
    private final Logger logger;
    private FileConfiguration config;
    
    
    private boolean configValid = true;
    private final List<String> validationErrors = new ArrayList<>();
    
    
    private final AtomicBoolean isReloading = new AtomicBoolean(false);
    private final Map<String, Object> configCache = new ConcurrentHashMap<>();
    private long lastReloadTime = 0;
    
    
    private final List<ConfigChangeListener> changeListeners = new ArrayList<>();
    
    public interface ConfigChangeListener {
        void onConfigReloaded();
        void onConfigValueChanged(String path, Object oldValue, Object newValue);
    }
    
    public ConfigManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        
        
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
            logger.info("Default configuration created");
        }
        
        
        loadConfig();
    }
    
    private void loadConfig() {
        try {
            isReloading.set(true);
            
            
            Map<String, Object> oldValues = new HashMap<>(configCache);
            
            
            if (!validateYamlSyntax()) {
                logger.severe("YAML syntax error detected in config.yml - attempting to restore from backup");
                if (!attemptBackupRestore()) {
                    logger.severe("Failed to restore from backup - plugin will use default configuration");
                    
                    createFreshConfigFromDefaults();
                }
                return;
            }
            
            plugin.reloadConfig();
            config = plugin.getConfig();
            
            
            migrateIfNeeded();

            
            
            
            
            validateConfigurationStructure();
            
            
            validateConfiguration();
            
            
            cacheConfigValues();
            
            
            detectConfigChanges(oldValues);
            
            if (configValid) {
                logger.info("Configuration loaded and validated successfully!");
                lastReloadTime = System.currentTimeMillis();
            } else {
                logger.warning("Configuration loaded with " + validationErrors.size() + " validation errors:");
                for (String error : validationErrors) {
                    logger.warning("  - " + error);
                }
            }
            
            
            notifyConfigReloaded();
            
        } catch (Exception e) {
            logger.severe("Failed to load configuration: " + e.getMessage());
            e.printStackTrace();
            
            
            if (!attemptBackupRestore()) {
                logger.severe("Failed to restore from backup - plugin will use default configuration");
                createFreshConfigFromDefaults();
            }
        } finally {
            isReloading.set(false);
        }
    }
    
    
    private boolean validateYamlSyntax() {
        try {
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                return true; 
            }
            
            
            YamlConfiguration.loadConfiguration(configFile);
            return true;
        } catch (Exception e) {
            logger.severe("YAML syntax error in config.yml: " + e.getMessage());
            
            
            if (e.getMessage().contains("line")) {
                logger.severe("Please check the line mentioned in the error above for:");
                logger.severe("- Missing quotes around values with special characters");
                logger.severe("- Incorrect indentation (use spaces, not tabs)");
                logger.severe("- Unclosed quotes or brackets");
                logger.severe("- Invalid YAML syntax");
            }
            
            return false;
        }
    }
    
    
    private boolean attemptBackupRestore() {
        try {
            File backupFile = new File(plugin.getDataFolder(), "config.yml.backup");
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            
            if (backupFile.exists()) {
                
                try {
                    YamlConfiguration.loadConfiguration(backupFile);
                } catch (Exception e) {
                    logger.severe("Backup file also has syntax errors: " + e.getMessage());
                    return false;
                }
                
                
                YamlConfiguration backupConfig = YamlConfiguration.loadConfiguration(backupFile);
                backupConfig.save(configFile);
                
                
                plugin.reloadConfig();
                config = plugin.getConfig();
                
                logger.info("Configuration restored from backup successfully");
                return true;
            } else {
                logger.warning("No backup configuration found");
                return false;
            }
        } catch (Exception e) {
            logger.severe("Failed to restore from backup: " + e.getMessage());
            return false;
        }
    }
    
    
    private void createFreshConfigFromDefaults() {
        try {
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            
            
            if (configFile.exists()) {
                configFile.delete();
                logger.info("Removed corrupted config.yml");
            }
            
            
            
            
            
            plugin.reloadConfig();
            config = plugin.getConfig();
            
            logger.info("Fresh configuration created from defaults");
            
        } catch (Exception e) {
            logger.severe("Failed to create fresh configuration: " + e.getMessage());
        }
    }
    
    
    private void createAutomaticBackup() {
        try {
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            File backupFile = new File(plugin.getDataFolder(), "config.yml.backup");
            
            if (configFile.exists()) {
                
                String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                File timestampedBackup = new File(plugin.getDataFolder(), "config.yml.backup." + timestamp);
                
                
                java.nio.file.Files.copy(configFile.toPath(), timestampedBackup.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                
                
                java.nio.file.Files.copy(configFile.toPath(), backupFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                
                logger.info("Automatic backup created: config.yml.backup." + timestamp);
                
                
                cleanupOldBackups();
            }
        } catch (Exception e) {
            logger.warning("Failed to create automatic backup: " + e.getMessage());
        }
    }
    
    
    private void cleanupOldBackups() {
        try {
            File dataFolder = plugin.getDataFolder();
            File[] backupFiles = dataFolder.listFiles((dir, name) -> name.startsWith("config.yml.backup.") && name.matches("config\\.yml\\.backup\\.[0-9]{8}_[0-9]{6}"));
            
            if (backupFiles != null && backupFiles.length > 5) {
                
                java.util.Arrays.sort(backupFiles, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
                
                
                int filesToDelete = backupFiles.length - 5;
                for (int i = 0; i < filesToDelete; i++) {
                    if (backupFiles[i].delete()) {
                        logger.info("Cleaned up old backup: " + backupFiles[i].getName());
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to cleanup old backups: " + e.getMessage());
        }
    }
    
    private void cacheConfigValues() {
        configCache.clear();
        cacheSection("", config);
    }
    
    private void cacheSection(String prefix, ConfigurationSection section) {
        for (String key : section.getKeys(true)) {
            String fullPath = prefix.isEmpty() ? key : prefix + "." + key;
            if (!section.isConfigurationSection(key)) {
                configCache.put(fullPath, section.get(key));
            }
        }
    }
    
    private void detectConfigChanges(Map<String, Object> oldValues) {
        for (Map.Entry<String, Object> entry : configCache.entrySet()) {
            String path = entry.getKey();
            Object newValue = entry.getValue();
            Object oldValue = oldValues.get(path);
            
            if (!java.util.Objects.equals(oldValue, newValue)) {
                notifyConfigValueChanged(path, oldValue, newValue);
            }
        }
    }
    
    private void notifyConfigReloaded() {
        for (ConfigChangeListener listener : changeListeners) {
            try {
                listener.onConfigReloaded();
            } catch (Exception e) {
                logger.warning("Error notifying config change listener: " + e.getMessage());
            }
        }
    }
    
    private void notifyConfigValueChanged(String path, Object oldValue, Object newValue) {
        for (ConfigChangeListener listener : changeListeners) {
            try {
                listener.onConfigValueChanged(path, oldValue, newValue);
            } catch (Exception e) {
                logger.warning("Error notifying config value change listener: " + e.getMessage());
            }
        }
    }
    
    public void addConfigChangeListener(ConfigChangeListener listener) {
        changeListeners.add(listener);
    }
    
    public void removeConfigChangeListener(ConfigChangeListener listener) {
        changeListeners.remove(listener);
    }
    
    public boolean isReloading() {
        return isReloading.get();
    }
    
    public long getLastReloadTime() {
        return lastReloadTime;
    }
    
    private void setCoreDefaultsIfMissing() {
        
        setDefaultIfMissing("core.debug", false);
        setDefaultIfMissing("core.language", "en");
        
        
        setDefaultIfMissing("times.wanted-duration", 1800);
        setDefaultIfMissing("times.contraband-compliance", 10);
        
        
        setDefaultIfMissing("guard-system.duty-region", "guard");
        setDefaultIfMissing("guard-system.immobilization-time", 5);
        
        
        setDefaultIfMissing("guard-system.restrictions.block-mining", true);
        setDefaultIfMissing("guard-system.restrictions.block-crafting", true);
        setDefaultIfMissing("guard-system.restrictions.block-storage", true);
        setDefaultIfMissing("guard-system.restrictions.block-item-dropping", true);
        
        
        setDefaultIfMissing("guard-system.off-duty-earning.base-duty-requirement", 15);
        setDefaultIfMissing("guard-system.off-duty-earning.base-off-duty-earned", 30);
        setDefaultIfMissing("guard-system.off-duty-earning.performance-bonuses.searches-per-bonus", 10);
        setDefaultIfMissing("guard-system.off-duty-earning.performance-bonuses.search-bonus-time", 5);
        setDefaultIfMissing("guard-system.off-duty-earning.performance-bonuses.successful-search-bonus", 10);
        setDefaultIfMissing("guard-system.off-duty-earning.performance-bonuses.successful-arrest-bonus", 8);
        setDefaultIfMissing("guard-system.off-duty-earning.performance-bonuses.kills-per-bonus", 5);
        setDefaultIfMissing("guard-system.off-duty-earning.performance-bonuses.kill-bonus-time", 15);
        setDefaultIfMissing("guard-system.off-duty-earning.performance-bonuses.successful-detection-bonus", 10);
        setDefaultIfMissing("guard-system.off-duty-earning.performance-bonuses.duty-time-bonus-rate", 2);
        
        
        setDefaultIfMissing("guard-system.penalty-escalation.enabled", true);
        setDefaultIfMissing("guard-system.penalty-escalation.grace-period", 5);
        setDefaultIfMissing("guard-system.penalty-escalation.stages.stage-1.time-minutes", 5);
        setDefaultIfMissing("guard-system.penalty-escalation.stages.stage-1.slowness-level", 1);
        setDefaultIfMissing("guard-system.penalty-escalation.stages.stage-1.economy-penalty", 1000);
        setDefaultIfMissing("guard-system.penalty-escalation.stages.stage-1.warning-message", true);
        setDefaultIfMissing("guard-system.penalty-escalation.stages.stage-2.time-minutes", 10);
        setDefaultIfMissing("guard-system.penalty-escalation.stages.stage-2.slowness-level", 2);
        setDefaultIfMissing("guard-system.penalty-escalation.stages.stage-2.economy-penalty", 1000);
        setDefaultIfMissing("guard-system.penalty-escalation.stages.stage-2.warning-message", true);
        setDefaultIfMissing("guard-system.penalty-escalation.stages.recurring.interval-minutes", 5);
        setDefaultIfMissing("guard-system.penalty-escalation.stages.recurring.slowness-level", 2);
        setDefaultIfMissing("guard-system.penalty-escalation.stages.recurring.economy-penalty", 1000);
        setDefaultIfMissing("guard-system.penalty-escalation.stages.recurring.warning-message", true);
        
        
        setDefaultIfMissing("penalty-escalation.bypass.earned-time-bonus", 60);
        setDefaultIfMissing("penalty-escalation.bypass.clear-penalty-tracking", true);
        setDefaultIfMissing("penalty-escalation.bypass.remove-potion-effects", true);
        
        
        setDefaultIfMissing("jail-system.base-time", 300);
        setDefaultIfMissing("jail-system.level-multiplier", 60);
        setDefaultIfMissing("jail-system.max-wanted-level", 5);
        setDefaultIfMissing("jail-system.countdown", 10);
        setDefaultIfMissing("jail-system.countdown-radius", 5.0);
        setDefaultIfMissing("jail-system.chase-integration.enabled", true);
        setDefaultIfMissing("jail-system.chase-integration.flee-threshold", 2.5);
        
        
        setDefaultIfMissing("chase-system.max-distance", 100);
        setDefaultIfMissing("chase-system.warning-distance", 20);
        setDefaultIfMissing("chase-system.max-concurrent", 3);
        setDefaultIfMissing("chase-system.restrictions.prevent-chase-during-combat", true);
        setDefaultIfMissing("chase-system.restrictions.block-restricted-areas", true);
        setDefaultIfMissing("chase-system.restrictions.restricted-areas", "");
        setDefaultIfMissing("chase-system.restrictions.auto-end-in-restricted-area", true);
        setDefaultIfMissing("chase-system.restrictions.prevent-teleport", true);
        setDefaultIfMissing("chase-system.restrictions.allowed-plugin-teleport-distance", 1.0);
        setDefaultIfMissing("chase-system.restrictions.allow-admin-teleports", true);
        
        
        setDefaultIfMissing("contraband-system.enabled", true);
        setDefaultIfMissing("contraband-system.drug-detection", true);
        setDefaultIfMissing("contraband-system.max-request-distance", 5);
        setDefaultIfMissing("contraband-system.compliance-grace-period", 3);
        
        
        setDefaultIfMissing("wanted-system.prevent-teleport", true);
        setDefaultIfMissing("wanted-system.allowed-plugin-teleport-distance", 1.0);
        
        
        setDefaultIfMissing("combat-system.timer-duration", 5);
        setDefaultIfMissing("combat-system.prevent-capture", true);
        setDefaultIfMissing("combat-system.prevent-teleport", true);
        
        
        setDefaultIfMissing("banking-system.enabled", true);
        setDefaultIfMissing("banking-system.conversion-rate", 100);
        setDefaultIfMissing("banking-system.minimum-conversion", 300);
        setDefaultIfMissing("banking-system.auto-convert", false);
        setDefaultIfMissing("banking-system.auto-convert-threshold", 3600);
        setDefaultIfMissing("banking-system.currency-command", "et give {player} {amount}");
        
        
        setDefaultIfMissing("regions.no-chase-zones", "safezon");
        setDefaultIfMissing("regions.duty-required-zones", "guard_lockers,guard_lockers2,guardplotstairs");
        
        
        setDefaultIfMissing("security.guard-immunity.enabled", true);
        setDefaultIfMissing("security.guard-immunity.wanted-protection", true);
        setDefaultIfMissing("security.guard-immunity.chase-protection", true);
        setDefaultIfMissing("security.guard-immunity.contraband-protection", true);
        setDefaultIfMissing("security.guard-immunity.jail-protection", true);
        setDefaultIfMissing("security.guard-immunity.combat-protection", false);
        setDefaultIfMissing("security.guard-immunity.teleport-protection", true);
        
        
        setDefaultIfMissing("user-interface.boss-bars.enabled", true);
        setDefaultIfMissing("user-interface.boss-bars.wanted.enabled", true);
        setDefaultIfMissing("user-interface.boss-bars.wanted.color", "RED");
        setDefaultIfMissing("user-interface.boss-bars.wanted.overlay", "PROGRESS");
        setDefaultIfMissing("user-interface.boss-bars.chase.enabled", true);
        setDefaultIfMissing("user-interface.boss-bars.chase.color", "BLUE");
        setDefaultIfMissing("user-interface.boss-bars.chase.overlay", "PROGRESS");
        setDefaultIfMissing("user-interface.boss-bars.combat.enabled", true);
        setDefaultIfMissing("user-interface.boss-bars.combat.color", "RED");
        setDefaultIfMissing("user-interface.boss-bars.combat.overlay", "PROGRESS");
        setDefaultIfMissing("user-interface.boss-bars.jail.enabled", true);
        setDefaultIfMissing("user-interface.boss-bars.jail.color", "PURPLE");
        setDefaultIfMissing("user-interface.boss-bars.jail.overlay", "PROGRESS");
        setDefaultIfMissing("user-interface.boss-bars.duty.enabled", true);
        setDefaultIfMissing("user-interface.boss-bars.duty.color", "GREEN");
        setDefaultIfMissing("user-interface.boss-bars.duty.overlay", "PROGRESS");
        setDefaultIfMissing("user-interface.boss-bars.contraband.enabled", true);
        setDefaultIfMissing("user-interface.boss-bars.contraband.color", "YELLOW");
        setDefaultIfMissing("user-interface.boss-bars.contraband.overlay", "PROGRESS");
        setDefaultIfMissing("user-interface.boss-bars.grace-period.enabled", true);
        setDefaultIfMissing("user-interface.boss-bars.grace-period.color", "PINK");
        setDefaultIfMissing("user-interface.boss-bars.grace-period.overlay", "PROGRESS");
        setDefaultIfMissing("user-interface.boss-bars.penalty.enabled", true);
        setDefaultIfMissing("user-interface.boss-bars.penalty.color", "RED");
        setDefaultIfMissing("user-interface.boss-bars.penalty.overlay", "PROGRESS");
        setDefaultIfMissing("user-interface.boss-bars.penalty.duration", 30);
        
        
        setDefaultIfMissing("guard-tags.enabled", true);
        setDefaultIfMissing("guard-tags.prefix", "🛡️");
        setDefaultIfMissing("guard-tags.priority", 100);
        setDefaultIfMissing("guard-tags.hover.enabled", true);
        setDefaultIfMissing("guard-tags.hover.title", "§6§l◆ CORRECTIONAL OFFICER ◆");
        setDefaultIfMissing("guard-tags.hover.rank-format", "§7Rank: §b{rank}");
        setDefaultIfMissing("guard-tags.hover.status-format", "§7Status: §aON DUTY");
        setDefaultIfMissing("guard-tags.hover.session-header", "§7═══ Session Stats ═══");
        setDefaultIfMissing("guard-tags.hover.session-arrests", "§7Arrests: §e{arrests}");
        setDefaultIfMissing("guard-tags.hover.session-searches", "§7Searches: §e{searches}");
        setDefaultIfMissing("guard-tags.hover.session-detections", "§7Detections: §e{detections}");
        setDefaultIfMissing("guard-tags.hover.total-header", "§7═══ Total Stats ═══");
        setDefaultIfMissing("guard-tags.hover.total-arrests", "§7Total Arrests: §c{total_arrests}");
        setDefaultIfMissing("guard-tags.hover.duty-time", "§7Duty Time: §d{hours}h {minutes}m");
        setDefaultIfMissing("guard-tags.hover.off-duty-time", "§7Off-Duty Available: §a{minutes}m");
        setDefaultIfMissing("guard-tags.hover.no-off-duty-time", "§7Off-Duty Available: §cNone");
        
        
        setDefaultIfMissing("wanted-tags.enabled", true);
        setDefaultIfMissing("wanted-tags.format", "<red><bold>WANTED {stars}</bold></red>");
        setDefaultIfMissing("wanted-tags.priority", 150);
        
        
        setDefaultIfMissing("database.type", "sqlite");
        setDefaultIfMissing("database.sqlite.file", "edencorrections.db");
        setDefaultIfMissing("database.sqlite.maintenance.enabled", true);
        setDefaultIfMissing("database.sqlite.maintenance.enable-vacuum", true);
        setDefaultIfMissing("database.sqlite.maintenance.vacuum-timeout", 10000);
        setDefaultIfMissing("database.sqlite.maintenance.maintenance-interval", 60);
        setDefaultIfMissing("database.mysql.host", "localhost");
        setDefaultIfMissing("database.mysql.port", 3306);
        setDefaultIfMissing("database.mysql.database", "edencorrections");
        setDefaultIfMissing("database.mysql.username", "username");
        setDefaultIfMissing("database.mysql.password", "password");
        
        
        setDefaultIfMissing("performance.spam-control.duty-system.disable-continuous-messages", true);
        setDefaultIfMissing("performance.spam-control.duty-system.show-status-changes-only", true);
        setDefaultIfMissing("performance.spam-control.duty-system.notification-cooldown", 30);
        setDefaultIfMissing("performance.spam-control.duty-system.disable-performance-spam", true);
        setDefaultIfMissing("performance.spam-control.duty-system.show-bonuses-once", true);
        setDefaultIfMissing("performance.spam-control.chase-system.disable-distance-warnings", false);
        setDefaultIfMissing("performance.spam-control.chase-system.distance-warning-cooldown", 10);
        setDefaultIfMissing("performance.spam-control.chase-system.disable-status-spam", true);
        setDefaultIfMissing("performance.spam-control.combat-system.disable-timer-spam", true);
        setDefaultIfMissing("performance.spam-control.combat-system.show-start-end-only", true);
        setDefaultIfMissing("performance.spam-control.general.disable-debug-spam", true);
        setDefaultIfMissing("performance.spam-control.general.message-cooldown", 5);
        setDefaultIfMissing("performance.spam-control.general.disable-error-spam", true);
        setDefaultIfMissing("performance.caching.enable-message-cache", true);
        setDefaultIfMissing("performance.caching.message-cache-size", 1000);
        setDefaultIfMissing("performance.caching.enable-database-cache", true);
        setDefaultIfMissing("performance.caching.database-cache-size", 500);
        setDefaultIfMissing("performance.caching.cache-cleanup-interval", 300);
        setDefaultIfMissing("performance.caching.database-cleanup-interval", 600);
        
        
        
        
        
        plugin.saveConfig();
    }
    
    
    private void setMessageDefaultsIfMissing() {
        
        
        
    }
    
    
    private void validateConfigurationStructure() {
        
        String[] requiredSections = {
            "messages", "core", "guard-system", "chase-system", 
            "jail-system", "contraband-system", "wanted-system"
        };
        
        for (String section : requiredSections) {
            if (!config.contains(section)) {
                logger.warning("Missing required configuration section: " + section);
                validationErrors.add("Missing section: " + section);
            }
        }
        
        
        if (config.contains("messages")) {
            String[] requiredMessageSections = {
                "messages.universal", "messages.system", "messages.duty", 
                "messages.chase", "messages.jail", "messages.wanted"
            };
            
            for (String section : requiredMessageSections) {
                if (!config.contains(section)) {
                    logger.warning("Missing required message section: " + section);
                    validationErrors.add("Missing message section: " + section);
                }
            }
        }
    }

    
    private void setDefaultIfMissing(String path, Object defaultValue) {
        if (!config.contains(path)) {
            config.set(path, defaultValue);
        }
    }

    
    private boolean migrateIfNeeded() {
        try {
            int currentVersion = config.getInt(CONFIG_VERSION_PATH, 0);
            if (currentVersion >= CONFIG_VERSION) {
                return false;
            }

            
            

            java.util.concurrent.atomic.AtomicBoolean changed = new java.util.concurrent.atomic.AtomicBoolean(false);

            
            java.util.function.BiConsumer<String, String> moveIfPresent = (oldPath, newPath) -> {
                if (config.contains(oldPath)) {
                    Object value = config.get(oldPath);
                    
                    if (!config.contains(newPath)) {
                        config.set(newPath, value);
                    }
                    config.set(oldPath, null);
                    changed.set(true);
                }
            };

            java.util.function.Consumer<String> removeIfPresent = (oldPath) -> {
                if (config.contains(oldPath)) {
                    config.set(oldPath, null);
                    changed.set(true);
                }
            };

            
            moveIfPresent.accept("debug", "core.debug");
            moveIfPresent.accept("language", "core.language");

            
            if (!config.contains("times.wanted-duration") && config.contains("times.chase-duration")) {
                config.set("times.wanted-duration", config.get("times.chase-duration"));
                changed.set(true);
            }
            removeIfPresent.accept("times.chase-duration");
            
            if (config.contains("times.jail-countdown") && !config.contains("jail-system.countdown")) {
                config.set("jail-system.countdown", config.get("times.jail-countdown"));
                changed.set(true);
            }
            removeIfPresent.accept("times.jail-countdown");

            
            moveIfPresent.accept("jail.base-time", "jail-system.base-time");
            moveIfPresent.accept("jail.level-multiplier", "jail-system.level-multiplier");
            moveIfPresent.accept("jail.max-wanted-level", "jail-system.max-wanted-level");

            
            moveIfPresent.accept("chase.max-distance", "chase-system.max-distance");
            moveIfPresent.accept("chase.warning-distance", "chase-system.warning-distance");
            moveIfPresent.accept("chase.max-concurrent", "chase-system.max-concurrent");
            moveIfPresent.accept("chase.restrictions.prevent-chase-during-combat", "chase-system.restrictions.prevent-chase-during-combat");
            moveIfPresent.accept("chase.restrictions.block-restricted-areas", "chase-system.restrictions.block-restricted-areas");
            moveIfPresent.accept("chase.restrictions.restricted-areas", "chase-system.restrictions.restricted-areas");
            moveIfPresent.accept("chase.restrictions.auto-end-in-restricted-area", "chase-system.restrictions.auto-end-in-restricted-area");

            
            
            moveIfPresent.accept("wanted-system.prevent-teleport", "wanted.prevent-teleport");
            moveIfPresent.accept("wanted-system.allowed-plugin-teleport-distance", "wanted.allowed-plugin-teleport-distance");

            
            if (!config.contains("wanted.prevent-teleport")) {
                config.set("wanted.prevent-teleport", true);
                changed.set(true);
            }
            if (!config.contains("wanted.allowed-plugin-teleport-distance")) {
                config.set("wanted.allowed-plugin-teleport-distance", 1.0);
                changed.set(true);
            }

            
            moveIfPresent.accept("combat-timer.duration", "combat-system.timer-duration");
            moveIfPresent.accept("combat-timer.prevent-capture", "combat-system.prevent-capture");
            moveIfPresent.accept("combat-timer.prevent-teleport", "combat-system.prevent-teleport");

            
            moveIfPresent.accept("duty-banking.enabled", "banking-system.enabled");
            moveIfPresent.accept("duty-banking.conversion-rate", "banking-system.conversion-rate");
            moveIfPresent.accept("duty-banking.minimum-conversion", "banking-system.minimum-conversion");
            moveIfPresent.accept("duty-banking.auto-convert", "banking-system.auto-convert");
            moveIfPresent.accept("duty-banking.auto-convert-threshold", "banking-system.auto-convert-threshold");
            moveIfPresent.accept("duty-banking.currency-command", "banking-system.currency-command");

            
            moveIfPresent.accept("bossbars.enabled", "user-interface.boss-bars.enabled");
            moveIfPresent.accept("bossbars.wanted.enabled", "user-interface.boss-bars.wanted.enabled");
            moveIfPresent.accept("bossbars.wanted.color", "user-interface.boss-bars.wanted.color");
            moveIfPresent.accept("bossbars.wanted.overlay", "user-interface.boss-bars.wanted.overlay");
            moveIfPresent.accept("bossbars.chase.enabled", "user-interface.boss-bars.chase.enabled");
            moveIfPresent.accept("bossbars.chase.color", "user-interface.boss-bars.chase.color");
            moveIfPresent.accept("bossbars.chase.overlay", "user-interface.boss-bars.chase.overlay");
            moveIfPresent.accept("bossbars.combat.enabled", "user-interface.boss-bars.combat.enabled");
            moveIfPresent.accept("bossbars.combat.color", "user-interface.boss-bars.combat.color");
            moveIfPresent.accept("bossbars.combat.overlay", "user-interface.boss-bars.combat.overlay");
            moveIfPresent.accept("bossbars.jail.enabled", "user-interface.boss-bars.jail.enabled");
            moveIfPresent.accept("bossbars.jail.color", "user-interface.boss-bars.jail.color");
            moveIfPresent.accept("bossbars.jail.overlay", "user-interface.boss-bars.jail.overlay");
            moveIfPresent.accept("bossbars.duty.enabled", "user-interface.boss-bars.duty.enabled");
            moveIfPresent.accept("bossbars.duty.color", "user-interface.boss-bars.duty.color");
            moveIfPresent.accept("bossbars.duty.overlay", "user-interface.boss-bars.duty.overlay");
            moveIfPresent.accept("bossbars.contraband.enabled", "user-interface.boss-bars.contraband.enabled");
            moveIfPresent.accept("bossbars.contraband.color", "user-interface.boss-bars.contraband.color");
            moveIfPresent.accept("bossbars.contraband.overlay", "user-interface.boss-bars.contraband.overlay");
            moveIfPresent.accept("bossbars.grace.enabled", "user-interface.boss-bars.grace-period.enabled");
            moveIfPresent.accept("bossbars.grace.color", "user-interface.boss-bars.grace-period.color");
            moveIfPresent.accept("bossbars.grace.overlay", "user-interface.boss-bars.grace-period.overlay");

            
            if (config.contains("guard-immunity") && !config.contains("security.guard-immunity")) {
                config.set("security.guard-immunity", config.getConfigurationSection("guard-immunity").getValues(true));
                config.set("guard-immunity", null);
                changed.set(true);
            }

            
            if (config.contains("integrations.unlimited-nametags")) {
                config.set("integrations.unlimited-nametags", null);
                changed.set(true);
            }

            if (changed.get()) {
                config.set(CONFIG_VERSION_PATH, CONFIG_VERSION);
                
                
                
                
                
                logger.info("Configuration migration would have updated to version " + CONFIG_VERSION + " (disabled to preserve config).");
                return true;
            } else {
                
                if (currentVersion < CONFIG_VERSION) {
                    config.set(CONFIG_VERSION_PATH, CONFIG_VERSION);
                    
                    
                    return true;
                }
            }
        } catch (Exception e) {
            logger.severe("Config migration failed: " + e.getMessage());
        }
        return false;
    }
    
    private void validateConfiguration() {
        configValid = true;
        validationErrors.clear();
        
        
        validateCoreSettings();
        
        
        validateTimeSettings();
        
        
        validateJailSettings();
        
        
        validateChaseSettings();
        
        
        validateGuardSystem();
        
        
        validateContrabandSystem();
        
        
        validateCombatTimer();
        
        
        validateDutyBanking();
        
        
        validateRegionSettings();
        
        
        validatePerformanceSettings();
        
        
        validateIntegrationSettings();
        
        
        validateSecuritySettings();
        
        
        validateBossBarSettings();
    }
    
    private void validateCoreSettings() {
        
        String language = config.getString("core.language", "en");
        if (!Arrays.asList("en", "es", "fr", "de").contains(language)) {
            addValidationError("Invalid language: " + language + " (defaulting to 'en')");
        }
    }
    
    private void validateTimeSettings() {
        validatePositiveInt("times.wanted-duration", "Wanted duration");
        validatePositiveInt("times.contraband-compliance", "Contraband compliance time");
    }
    
    private void validateJailSettings() {
        validatePositiveInt("jail-system.base-time", "Base jail time");
        validatePositiveInt("jail-system.level-multiplier", "Jail level multiplier");
        validateRange("jail-system.max-wanted-level", "Max wanted level", 1, 10);
    }
    
    private void validateChaseSettings() {
        validatePositiveInt("chase-system.max-distance", "Max chase distance");
        validatePositiveInt("chase-system.warning-distance", "Chase warning distance");
        validatePositiveInt("chase-system.max-concurrent", "Max concurrent chases");
        
        
        int maxDistance = config.getInt("chase-system.max-distance", 100);
        int warningDistance = config.getInt("chase-system.warning-distance", 20);
        if (warningDistance >= maxDistance) {
            addValidationError("Chase warning distance (" + warningDistance + ") should be less than max distance (" + maxDistance + ")");
        }
    }
    
    private void validateGuardSystem() {
        
        validateNonNegativeInt("guard-system.immobilization-time", "Immobilization time");
        
        
        ConfigurationSection rankMappings = config.getConfigurationSection("guard-system.rank-mappings");
        if (rankMappings == null || rankMappings.getKeys(false).isEmpty()) {
            addValidationError("No guard rank mappings configured");
        }
        
        
        ConfigurationSection kitMappings = config.getConfigurationSection("guard-system.kit-mappings");
        if (kitMappings == null || kitMappings.getKeys(false).isEmpty()) {
            addValidationError("No guard kit mappings configured");
        }
    }
    
    private void validateContrabandSystem() {
        if (!config.getBoolean("contraband-system.enabled", true)) {
            return; 
        }
        
        validatePositiveInt("contraband-system.max-request-distance", "Max contraband request distance");
        validateNonNegativeInt("contraband-system.compliance-grace-period", "Contraband grace period");
        
        
        ConfigurationSection contrabandTypes = config.getConfigurationSection("contraband-system.types");
        if (contrabandTypes != null) {
            for (String type : contrabandTypes.getKeys(false)) {
                validateContrabandType(type);
            }
        }
    }
    
    private void validateContrabandType(String type) {
        String itemsPath = "contraband-system.types." + type + ".items"; 
        String descriptionPath = "contraband-system.types." + type + ".description";
        
        if (!config.contains(itemsPath)) {
            addValidationError("Missing contraband type items configuration: " + itemsPath);
            return;
        }
        
        
        java.util.List<String> itemList = new java.util.ArrayList<>();
        if (config.isList(itemsPath)) {
            itemList.addAll(config.getStringList(itemsPath));
        } else {
            String items = config.getString(itemsPath, "");
            if (items != null && !items.trim().isEmpty()) {
                for (String token : items.split(",")) {
                    itemList.add(token.trim());
                }
            }
        }
        if (itemList.isEmpty()) {
            addValidationError("Empty contraband items for type: " + type);
        }
        
        for (String materialNameRaw : itemList) {
            String materialName = String.valueOf(materialNameRaw).trim().toUpperCase();
            if (materialName.isEmpty()) continue;
            try {
                Material.valueOf(materialName);
            } catch (IllegalArgumentException e) {
                addValidationError("Invalid material in contraband type " + type + ": " + materialName);
            }
        }
        
        String description = config.getString(descriptionPath, "");
        if (description.trim().isEmpty()) {
            addValidationError("Contraband type '" + type + "' has no description");
        }
    }
    
    private void validateCombatTimer() {
        validateNonNegativeInt("combat-system.timer-duration", "Combat timer duration");
    }
    
    private void validateDutyBanking() {
        if (!config.getBoolean("banking-system.enabled", true)) {
            return; 
        }
        
        validatePositiveInt("banking-system.conversion-rate", "Duty banking conversion rate");
        validateNonNegativeInt("banking-system.minimum-conversion", "Minimum conversion time");
        validatePositiveInt("banking-system.auto-convert-threshold", "Auto-convert threshold");
        
        
        String currencyCommand = config.getString("banking-system.currency-command", "");
        if (currencyCommand.trim().isEmpty()) {
            addValidationError("Duty banking currency command is empty");
        } else {
            if (!currencyCommand.contains("{player}")) {
                addValidationError("Currency command missing {player} placeholder");
            }
            if (!currencyCommand.contains("{amount}")) {
                addValidationError("Currency command missing {amount} placeholder");
            }
        }
    }
    
    private void validateRegionSettings() {
        
        String[] noChaseZones = getNoChaseZones();
        String[] dutyRequiredZones = getDutyRequiredZones();
        
        if (noChaseZones.length == 0) {
            addValidationError("No no-chase zones configured");
        }
        if (dutyRequiredZones.length == 0) {
            addValidationError("No duty-required zones configured");
        }
    }
    
    private void validatePerformanceSettings() {
        
        validateNonNegativeInt("performance.spam-control.duty-system.notification-cooldown", "Duty system notification cooldown");
        validateNonNegativeInt("performance.spam-control.chase-system.distance-warning-cooldown", "Chase system distance warning cooldown");
        validateNonNegativeInt("performance.spam-control.general.message-cooldown", "General message cooldown");
        
        
        validatePositiveInt("performance.caching.message-cache-size", "Message cache size");
        validatePositiveInt("performance.caching.database-cache-size", "Database cache size");
        validatePositiveInt("performance.caching.cache-cleanup-interval", "Cache cleanup interval");
        validatePositiveInt("performance.caching.database-cleanup-interval", "Database cleanup interval");
    }
    
    private void validateIntegrationSettings() {
        
        if (config.getBoolean("integrations.placeholderapi.enabled", true)) {
            
        }
        
        
        if (config.getBoolean("integrations.luckperms.strict-mode", true)) {
            
        }
        
        
        if (config.getBoolean("integration.chatcontrol.enabled", true)) {
            String channelName = config.getString("integration.chatcontrol.guard-channel.name", "edencorrections-guards");
            if (channelName.isEmpty() || channelName.contains(" ")) {
                addValidationError("ChatControl guard channel name cannot be empty or contain spaces");
            }
        }
    }
    
    private void validateSecuritySettings() {
        
        if (config.getBoolean("security.guard-immunity.enabled", true)) {
            
        }
    }
    
    private void validateBossBarSettings() {
        
        String[] validColors = {"BLUE", "GREEN", "PINK", "PURPLE", "RED", "WHITE", "YELLOW"};
        String[] validOverlays = {"PROGRESS", "NOTCHED_6", "NOTCHED_10", "NOTCHED_12", "NOTCHED_20"};
        
        validateBossBarSetting("user-interface.boss-bars.wanted.color", validColors);
        validateBossBarSetting("user-interface.boss-bars.chase.color", validColors);
        validateBossBarSetting("user-interface.boss-bars.combat.color", validColors);
        validateBossBarSetting("user-interface.boss-bars.jail.color", validColors);
        validateBossBarSetting("user-interface.boss-bars.duty.color", validColors);
        validateBossBarSetting("user-interface.boss-bars.contraband.color", validColors);
        validateBossBarSetting("user-interface.boss-bars.grace-period.color", validColors);
        validateBossBarSetting("user-interface.boss-bars.penalty.color", validColors);
        
        validateBossBarSetting("user-interface.boss-bars.wanted.overlay", validOverlays);
        validateBossBarSetting("user-interface.boss-bars.chase.overlay", validOverlays);
        validateBossBarSetting("user-interface.boss-bars.combat.overlay", validOverlays);
        validateBossBarSetting("user-interface.boss-bars.jail.overlay", validOverlays);
        validateBossBarSetting("user-interface.boss-bars.duty.overlay", validOverlays);
        validateBossBarSetting("user-interface.boss-bars.contraband.overlay", validOverlays);
        validateBossBarSetting("user-interface.boss-bars.grace-period.overlay", validOverlays);
        validateBossBarSetting("user-interface.boss-bars.penalty.overlay", validOverlays);
    }
    
    private void validateBossBarSetting(String path, String[] validValues) {
        String value = config.getString(path, "");
        if (!value.isEmpty() && !Arrays.asList(validValues).contains(value.toUpperCase())) {
            addValidationError("Invalid boss bar setting '" + path + "': " + value + " (valid: " + String.join(", ", validValues) + ")");
        }
    }
    
    private void validatePositiveInt(String path, String name) {
        int value = config.getInt(path, 1);
        if (value <= 0) {
            addValidationError(name + " must be positive (current: " + value + ")");
        }
    }
    
    private void validateNonNegativeInt(String path, String name) {
        int value = config.getInt(path, 0);
        if (value < 0) {
            addValidationError(name + " must be non-negative (current: " + value + ")");
        }
    }
    
    private void validateRange(String path, String name, int min, int max) {
        int value = config.getInt(path, min);
        if (value < min || value > max) {
            addValidationError(name + " must be between " + min + " and " + max + " (current: " + value + ")");
        }
    }
    
    private void addValidationError(String error) {
        configValid = false;
        validationErrors.add(error);
    }
    
    public void reload() {
        if (isReloading.compareAndSet(false, true)) {
            try {
                            
            
                
                
                configCache.clear();
                
                
                loadConfig();
                
                
                cacheConfigValues();
                
                
                lastReloadTime = System.currentTimeMillis();
                
                
                notifyConfigReloaded();
                
                logger.info("Configuration reloaded successfully");
                
            } catch (Exception e) {
                logger.severe("Failed to reload configuration: " + e.getMessage());
                e.printStackTrace();
            } finally {
                isReloading.set(false);
            }
        } else {
            logger.warning("Configuration reload already in progress");
        }
    }
    
    
    
    public boolean isConfigValid() {
        return configValid;
    }
    
    public List<String> getValidationErrors() {
        return new ArrayList<>(validationErrors);
    }
    
    
    
    public boolean isDebugMode() {
        return config.getBoolean("core.debug", false);
    }
    
    public String getLanguage() {
        return config.getString("core.language", "en");
    }
    

    
    public int getChaseDuration() {
        return config.getInt("times.wanted-duration", 1800); 
    }
    
    public int getWantedDuration() {
        return config.getInt("times.wanted-duration", 1800);
    }

    
    public int getWantedDurationForLevel(int level) {
        int fallback = getWantedDuration();
        if (level <= 0) return 0;
        String path = "wanted-levels.level-" + level + ".duration";
        if (config.isConfigurationSection("wanted-levels") && config.contains(path)) {
            return Math.max(1, config.getInt(path, fallback));
        }
        
        path = "jail-system.wanted-levels.level-" + level + ".duration";
        if (config.contains(path)) {
            return Math.max(1, config.getInt(path, fallback));
        }
        return fallback;
    }
    
    public int getJailCountdown() {
        return config.getInt("jail-system.countdown", 10);
    }
    
    public double getJailCountdownRadius() {
        return config.getDouble("jail-system.countdown-radius", 5.0);
    }
    
    public boolean isJailChaseIntegrationEnabled() {
        return config.getBoolean("jail-system.chase-integration.enabled", true);
    }
    
    public double getJailFleeThreshold() {
        return config.getDouble("jail-system.chase-integration.flee-threshold", 2.5);
    }
    
    public int getContrabandCompliance() {
        return config.getInt("times.contraband-compliance", 10);
    }
    
    public int getBaseJailTime() {
        return config.getInt("jail-system.base-time", 300);
    }
    
    public int getJailLevelMultiplier() {
        return config.getInt("jail-system.level-multiplier", 60);
    }
    
    public int getMaxWantedLevel() {
        return config.getInt("jail-system.max-wanted-level", 5);
    }
    
    
    public boolean isWantedTeleportEnabled() {
        
        if (config.contains("wanted.prevent-teleport")) {
            return config.getBoolean("wanted.prevent-teleport", true);
        }
        if (config.contains("wanted-system.prevent-teleport")) {
            return config.getBoolean("wanted-system.prevent-teleport", true);
        }
        
        return config.getBoolean("chase-system.restrictions.prevent-teleport", true);
    }
    
    
    public double getAllowedPluginTeleportDistance() {
        if (config.contains("wanted.allowed-plugin-teleport-distance")) {
            return config.getDouble("wanted.allowed-plugin-teleport-distance", 1.0);
        }
        if (config.contains("wanted-system.allowed-plugin-teleport-distance")) {
            return config.getDouble("wanted-system.allowed-plugin-teleport-distance", 1.0);
        }
        
        return config.getDouble("chase-system.restrictions.allowed-plugin-teleport-distance", 1.0);
    }
    
    public int getMaxChaseDistance() {
        return config.getInt("chase-system.max-distance", 100);
    }
    
    public int getChaseWarningDistance() {
        return config.getInt("chase-system.warning-distance", 20);
    }
    
    public int getMaxConcurrentChases() {
        return config.getInt("chase-system.max-concurrent", 3);
    }
    
    
    
    public boolean shouldPreventChaseDuringCombat() {
        return config.getBoolean("chase-system.restrictions.prevent-chase-during-combat", true);
    }
    
    public boolean shouldBlockRestrictedAreas() {
        return config.getBoolean("chase-system.restrictions.block-restricted-areas", true);
    }
    
    public String[] getChaseRestrictedAreas() {
        String path = "chase-system.restrictions.restricted-areas";
        if (config.isList(path)) {
            java.util.List<String> list = config.getStringList(path);
            return list.stream().filter(s -> s != null && !s.trim().isEmpty()).toArray(String[]::new);
        }
        String areas = config.getString(path, "");
        if (areas == null || areas.trim().isEmpty()) {
            return new String[0];
        }
        String[] split = areas.split(",");
        java.util.List<String> normalized = new java.util.ArrayList<>();
        for (String s : split) {
            if (s != null && !s.trim().isEmpty()) {
                normalized.add(s.trim());
            }
        }
        return normalized.toArray(new String[0]);
    }

    
    public boolean addChaseRestrictedArea(String areaName) {
        String path = "chase-system.restrictions.restricted-areas";
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();
        if (config.isList(path)) {
            set.addAll(config.getStringList(path));
        } else {
            String existing = config.getString(path, "");
            if (existing != null && !existing.trim().isEmpty()) {
                for (String s : existing.split(",")) {
                    if (s != null && !s.trim().isEmpty()) set.add(s.trim());
                }
            }
        }
        String norm = areaName.trim();
        boolean added = set.add(norm);
        if (added) {
            config.set(path, new java.util.ArrayList<>(set));
            plugin.saveConfig();
        }
        return added;
    }

    
    public boolean removeChaseRestrictedArea(String areaName) {
        String path = "chase-system.restrictions.restricted-areas";
        java.util.List<String> list;
        if (config.isList(path)) {
            list = new java.util.ArrayList<>(config.getStringList(path));
        } else {
            list = new java.util.ArrayList<>();
            String existing = config.getString(path, "");
            if (existing != null && !existing.trim().isEmpty()) {
                for (String s : existing.split(",")) {
                    if (s != null && !s.trim().isEmpty()) list.add(s.trim());
                }
            }
        }
        boolean removed = false;
        for (java.util.Iterator<String> it = list.iterator(); it.hasNext();) {
            String s = it.next();
            if (s != null && s.equalsIgnoreCase(areaName.trim())) {
                it.remove();
                removed = true;
            }
        }
        if (removed) {
            config.set(path, list);
            plugin.saveConfig();
        }
        return removed;
    }
    
    public boolean shouldAutoEndInRestrictedArea() {
        return config.getBoolean("chase-system.restrictions.auto-end-in-restricted-area", true);
    }

    
    public boolean isChaseTeleportEnabled() {
        return config.getBoolean("chase-system.restrictions.prevent-teleport", true);
    }

    
    public double getChaseAllowedPluginTeleportDistance() {
        return config.getDouble("chase-system.restrictions.allowed-plugin-teleport-distance", 1.0);
    }

    
    public boolean areAdminTeleportsAllowedDuringChase() {
        return config.getBoolean("chase-system.restrictions.allow-admin-teleports", true);
    }
    
    public boolean isContrabandEnabled() {
        return config.getBoolean("contraband-system.enabled", true);
    }
    
    public boolean isDrugDetectionEnabled() {
        return config.getBoolean("contraband-system.drug-detection", true);
    }

    
    public boolean isContrabandSystemEnabled() {
        return config.getBoolean("contraband-system.enabled", true);
    }

    
    public boolean isContrabandConfiscationEnabled() {
        return config.getBoolean("contraband-system.confiscation.enabled", true);
    }
    public String getContrabandConfiscationMode() {
        return config.getString("contraband-system.confiscation.mode", "specific"); 
    }
    public int getContrabandStorageDurationSeconds() {
        return config.getInt("contraband-system.confiscation.storage-duration", 3600);
    }
    public String getContrabandPricingMode() {
        return config.getString("contraband-system.confiscation.pricing.mode", "set"); 
    }
    public int getContrabandSetPrice() {
        return config.getInt("contraband-system.confiscation.pricing.set-price", 1000);
    }
    public int getContrabandRngMinPrice() {
        return config.getInt("contraband-system.confiscation.pricing.rng.min", 500);
    }
    public int getContrabandRngMaxPrice() {
        return config.getInt("contraband-system.confiscation.pricing.rng.max", 2500);
    }
    
    public boolean isGuardMiningBlocked() {
        return config.getBoolean("guard-system.restrictions.block-mining", true);
    }
    
    public boolean isGuardCraftingBlocked() {
        return config.getBoolean("guard-system.restrictions.block-crafting", true);
    }
    
    public boolean isGuardStorageBlocked() {
        return config.getBoolean("guard-system.restrictions.block-storage", true);
    }
    
    public boolean isGuardItemDroppingBlocked() {
        return config.getBoolean("guard-system.restrictions.block-item-dropping", true);
    }
    
    public String getDatabaseType() {
        return config.getString("database.type", "sqlite");
    }
    
    public String getSQLiteFile() {
        String configured = config.getString("database.sqlite.file", "corrections.db");
        try {
            java.io.File dataDir = dev.lsdmc.edenCorrections.EdenCorrections.getInstance().getDataFolder();
            java.io.File newFile = new java.io.File(dataDir, configured);
            java.io.File oldFile = new java.io.File(dataDir, "edencorrections.db");
            if (!newFile.exists() && oldFile.exists()) {
                return oldFile.getName();
            }
        } catch (Throwable ignored) {}
        return configured;
    }
    
    
    public String getMySQLHost() {
        return config.getString("database.mysql.host", "localhost");
    }
    
    public int getMySQLPort() {
        return config.getInt("database.mysql.port", 3306);
    }
    
    public String getMySQLDatabase() {
        return config.getString("database.mysql.database", "corrections");
    }
    
    public String getMySQLUsername() {
        return config.getString("database.mysql.username", "username");
    }
    
    public String getMySQLPassword() {
        return config.getString("database.mysql.password", "password");
    }
    
    
    public boolean isDatabaseMaintenanceEnabled() {
        return config.getBoolean("database.sqlite.maintenance.enabled", true);
    }
    
    public boolean isDatabaseVacuumEnabled() {
        return config.getBoolean("database.sqlite.maintenance.enable-vacuum", true);
    }
    
    public int getDatabaseVacuumTimeout() {
        return config.getInt("database.sqlite.maintenance.vacuum-timeout", 10000);
    }
    
    public int getDatabaseMaintenanceInterval() {
        return config.getInt("database.sqlite.maintenance.maintenance-interval", 60);
    }
    
    
    
    
    public String getDutyRegion() {
        return config.getString("guard-system.duty-region", "guard");
    }
    
    
    public org.bukkit.Location getGuardSpawnLocation() {
        String world = config.getString("guard-system.spawn.world", null);
        if (world == null || world.trim().isEmpty()) {
            return null;
        }
        
        org.bukkit.World bukkitWorld = org.bukkit.Bukkit.getWorld(world);
        if (bukkitWorld == null) {
            return null;
        }
        
        double x = config.getDouble("guard-system.spawn.x", 0.0);
        double y = config.getDouble("guard-system.spawn.y", 64.0);
        double z = config.getDouble("guard-system.spawn.z", 0.0);
        float yaw = (float) config.getDouble("guard-system.spawn.yaw", 0.0);
        float pitch = (float) config.getDouble("guard-system.spawn.pitch", 0.0);
        
        return new org.bukkit.Location(bukkitWorld, x, y, z, yaw, pitch);
    }

    
    public void setGuardSpawnLocation(org.bukkit.Location location) {
        if (location == null) {
            config.set("guard-system.spawn", null);
        } else {
            config.set("guard-system.spawn.world", location.getWorld().getName());
            config.set("guard-system.spawn.x", location.getX());
            config.set("guard-system.spawn.y", location.getY());
            config.set("guard-system.spawn.z", location.getZ());
            config.set("guard-system.spawn.yaw", location.getYaw());
            config.set("guard-system.spawn.pitch", location.getPitch());
        }
        
        try {
            config.save(new java.io.File(plugin.getDataFolder(), "config.yml"));
        } catch (Exception e) {
            logger.warning("Failed to save guard spawn location: " + e.getMessage());
        }
    }

    
    public boolean hasGuardSpawnLocation() {
        return getGuardSpawnLocation() != null;
    }
    
    
    public boolean isGuardDefaultOnDuty() {
        return config.getBoolean("guard-system.default-on-duty", true);
    }

    public List<String> getTransferBlacklistedItems() {
        return config.getStringList("guard-system.transfer.blacklisted-items");
    }

    public boolean isTransferDynamicDetectionEnabled() {
        return config.getBoolean("guard-system.transfer.dynamic-detection.enabled", true);
    }

    public boolean isTransferKitLoggingEnabled() {
        return config.getBoolean("guard-system.transfer.dynamic-detection.log-kit-items", true);
    }

    public boolean isTransferFullMetadataCheckEnabled() {
        return config.getBoolean("guard-system.transfer.dynamic-detection.check-full-metadata", true);
    }
    
    
    public int getKitCaptureDelayTicks() {
        return config.getInt("guard-system.transfer.dynamic-detection.kit-capture-delay-ticks", 1);
    }
    
    public boolean isTransferBatchLoggingEnabled() {
        return config.getBoolean("guard-system.transfer.dynamic-detection.batch-logging", true);
    }
    
    public boolean isTransferSkipIdenticalInventoriesEnabled() {
        return config.getBoolean("guard-system.transfer.dynamic-detection.skip-identical-inventories", true);
    }
    
    public boolean isTransferOnlyDebugIndividualItemsEnabled() {
        return config.getBoolean("guard-system.transfer.dynamic-detection.only-debug-individual-items", true);
    }
    
    public int getImmobilizationTime() {
        return config.getInt("guard-system.immobilization-time", 5);
    }
    
    public Map<String, String> getRankMappings() {
        Map<String, String> mappings = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("guard-system.rank-mappings");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                mappings.put(key, section.getString(key));
            }
        }
        return mappings;
    }
    
    public Map<String, String> getKitMappings() {
        Map<String, String> mappings = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("guard-system.kit-mappings");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                mappings.put(key, section.getString(key));
            }
        }
        return mappings;
    }
    
    public String getKitForRank(String rank) {
        return config.getString("guard-system.kit-mappings." + rank, "guard_kit");
    }
    
    
    public int getBaseDutyRequirement() {
        return config.getInt("guard-system.off-duty-earning.base-duty-requirement", 15);
    }
    
    public int getBaseOffDutyEarned() {
        return config.getInt("guard-system.off-duty-earning.base-off-duty-earned", 30);
    }
    
    public int getSearchesPerBonus() {
        return config.getInt("guard-system.off-duty-earning.performance-bonuses.searches-per-bonus", 10);
    }
    
    public int getSearchBonusTime() {
        return config.getInt("guard-system.off-duty-earning.performance-bonuses.search-bonus-time", 5);
    }
    
    public int getSuccessfulSearchBonus() {
        return config.getInt("guard-system.off-duty-earning.performance-bonuses.successful-search-bonus", 10);
    }
    
    public int getSuccessfulArrestBonus() {
        return config.getInt("guard-system.off-duty-earning.performance-bonuses.successful-arrest-bonus", 8);
    }
    
    public int getKillsPerBonus() {
        return config.getInt("guard-system.off-duty-earning.performance-bonuses.kills-per-bonus", 5);
    }
    
    public int getKillBonusTime() {
        return config.getInt("guard-system.off-duty-earning.performance-bonuses.kill-bonus-time", 15);
    }
    
    public int getSuccessfulDetectionBonus() {
        return config.getInt("guard-system.off-duty-earning.performance-bonuses.successful-detection-bonus", 10);
    }
    
    public int getDutyTimeBonusRate() {
        return config.getInt("guard-system.off-duty-earning.performance-bonuses.duty-time-bonus-rate", 2);
    }
    
    
    public ConfigurationSection getContrabandTypes() {
        return config.getConfigurationSection("contraband-system.types");
    }
    
    public int getMaxRequestDistance() {
        return config.getInt("contraband-system.max-request-distance", 5);
    }
    
    public int getGracePeriod() {
        return config.getInt("contraband-system.compliance-grace-period", 3);
    }
    
    public String getContrabandItems(String type) {
        return config.getString("contraband-system.types." + type + ".items", "");
    }
    
    public String getContrabandDescription(String type) {
        return config.getString("contraband-system.types." + type + ".description", type);
    }
    
    
    public int getCombatTimerDuration() {
        return config.getInt("combat-system.timer-duration", 5);
    }

    
    public int getPotionTestCooldownSeconds() {
        return config.getInt("contraband-system.potion-test.cooldown-seconds", 30);
    }
    
    public boolean shouldPreventCaptureInCombat() {
        return config.getBoolean("combat-system.prevent-capture", true);
    }
    
    public boolean shouldPreventTeleportInCombat() {
        return config.getBoolean("combat-system.prevent-teleport", true);
    }
    
    
    public boolean isDutyBankingEnabled() {
        return config.getBoolean("banking-system.enabled", true);
    }
    
    public int getConversionRate() {
        return config.getInt("banking-system.conversion-rate", 100);
    }
    
    public int getMinimumConversion() {
        return config.getInt("banking-system.minimum-conversion", 300);
    }
    
    public boolean isAutoConvert() {
        return config.getBoolean("banking-system.auto-convert", false);
    }
    
    public int getAutoConvertThreshold() {
        return config.getInt("banking-system.auto-convert-threshold", 3600);
    }
    
    public String getCurrencyCommand() {
        return config.getString("banking-system.currency-command", "et give {player} {amount}");
    }
    
    
    public String[] getNoChaseZones() {
        String zones = config.getString("regions.no-chase-zones", "safezon");
        return zones.split(",");
    }
    
    public String[] getDutyRequiredZones() {
        String zones = config.getString("regions.duty-required-zones", "guard_lockers,guard_lockers2,guardplotstairs");
        return zones.split(",");
    }

    public String[] getDutyRestrictedZones() {
        String listPath = "regions.duty-restricted-zones";
        if (config.isList(listPath)) {
            java.util.List<String> list = config.getStringList(listPath);
            return list.stream().filter(s -> s != null && !s.trim().isEmpty()).toArray(String[]::new);
        }
        String zones = config.getString(listPath, "");
        if (zones == null || zones.trim().isEmpty()) return new String[]{};
        return zones.split(",");
    }

    public String[] getDutyDisallowedZonesForRank(String rank) {
        if (rank == null || rank.trim().isEmpty()) return new String[]{};
        String path = "regions.duty-disallowed-zones-by-rank." + rank.toLowerCase();
        if (config.isList(path)) {
            java.util.List<String> list = config.getStringList(path);
            return list.stream().filter(s -> s != null && !s.trim().isEmpty()).toArray(String[]::new);
        }
        String zones = config.getString(path, "");
        if (zones == null || zones.trim().isEmpty()) return new String[]{};
        return zones.split(",");
    }

    
    public String[] getBreakZones() {
        String listPath = "regions.break-zones";
        if (config.isList(listPath)) {
            java.util.List<String> list = config.getStringList(listPath);
            return list.stream().filter(s -> s != null && !s.trim().isEmpty()).toArray(String[]::new);
        }
        String zones = config.getString(listPath, null);
        if (zones == null || zones.trim().isEmpty()) {
            
            String legacy = config.getString("regions.wanted-exempt-zones", "");
            if (legacy == null || legacy.trim().isEmpty()) return new String[]{};
            return legacy.split(",");
        }
        return zones.split(",");
    }

    
    @Deprecated
    public String[] getWantedExemptZones() {
        return getBreakZones();
    }
    
    
    public boolean isDutySystemContinuousMessagesDisabled() {
        return config.getBoolean("performance.spam-control.duty-system.disable-continuous-messages", true);
    }
    
    public boolean isDutySystemStatusChangesOnly() {
        return config.getBoolean("performance.spam-control.duty-system.show-status-changes-only", true);
    }
    
    public int getDutySystemNotificationCooldown() {
        return config.getInt("performance.spam-control.duty-system.notification-cooldown", 30);
    }
    
    public boolean isDutySystemPerformanceSpamDisabled() {
        return config.getBoolean("performance.spam-control.duty-system.disable-performance-spam", true);
    }
    
    public boolean isDutySystemShowBonusesOnce() {
        return config.getBoolean("performance.spam-control.duty-system.show-bonuses-once", true);
    }
    
    public boolean isChaseSystemDistanceWarningsDisabled() {
        return config.getBoolean("performance.spam-control.chase-system.disable-distance-warnings", false);
    }
    
    public int getChaseSystemDistanceWarningCooldown() {
        return config.getInt("performance.spam-control.chase-system.distance-warning-cooldown", 10);
    }
    
    public boolean isChaseSystemStatusSpamDisabled() {
        return config.getBoolean("performance.spam-control.chase-system.disable-status-spam", true);
    }
    
    public boolean isCombatSystemTimerSpamDisabled() {
        return config.getBoolean("performance.spam-control.combat-system.disable-timer-spam", true);
    }
    
    public boolean isCombatSystemShowStartEndOnly() {
        return config.getBoolean("performance.spam-control.combat-system.show-start-end-only", true);
    }
    
    public boolean isGeneralDebugSpamDisabled() {
        return config.getBoolean("performance.spam-control.general.disable-debug-spam", true);
    }
    
    public int getGeneralMessageCooldown() {
        return config.getInt("performance.spam-control.general.message-cooldown", 5);
    }
    
    public boolean isGeneralErrorSpamDisabled() {
        return config.getBoolean("performance.spam-control.general.disable-error-spam", true);
    }
    
    public boolean isMessageCacheEnabled() {
        return config.getBoolean("performance.caching.enable-message-cache", true);
    }
    
    public int getMessageCacheSize() {
        return config.getInt("performance.caching.message-cache-size", 1000);
    }
    
    public boolean isDatabaseCacheEnabled() {
        return config.getBoolean("performance.caching.enable-database-cache", true);
    }
    
    public int getDatabaseCacheSize() {
        return config.getInt("performance.caching.database-cache-size", 500);
    }
    
    public int getCacheCleanupInterval() {
        return config.getInt("performance.caching.cache-cleanup-interval", 300);
    }
    
    public int getDatabaseCleanupInterval() {
        return config.getInt("performance.caching.database-cleanup-interval", 600);
    }
    
    
    public boolean isPlaceholderAPIEnabled() {
        return config.getBoolean("integrations.placeholderapi.enabled", true);
    }
    
    public boolean isLuckPermsStrictMode() {
        return config.getBoolean("integrations.luckperms.strict-mode", true);
    }
    
    public boolean isWorldGuardRequired() {
        return config.getBoolean("integrations.worldguard.required", false);
    }
    
    public boolean isCMIKitsEnabled() {
        return config.getBoolean("integrations.cmi.kits-enabled", true);
    }
    
    
    
    public boolean isGuardImmunityEnabled() {
        return config.getBoolean("security.guard-immunity.enabled", true);
    }
    
    public boolean isGuardWantedProtected() {
        return config.getBoolean("security.guard-immunity.wanted-protection", true);
    }
    
    public boolean isGuardChaseProtected() {
        return config.getBoolean("security.guard-immunity.chase-protection", true);
    }
    
    public boolean isGuardContrabandProtected() {
        return config.getBoolean("security.guard-immunity.contraband-protection", true);
    }
    
    public boolean isGuardJailProtected() {
        return config.getBoolean("security.guard-immunity.jail-protection", true);
    }
    
    public boolean isGuardCombatProtected() {
        return config.getBoolean("security.guard-immunity.combat-protection", false);
    }
    
    public boolean isGuardTeleportProtected() {
        return config.getBoolean("security.guard-immunity.teleport-protection", true);
    }
    
    public boolean isGuardToGuardProtectionEnabled() {
        return config.getBoolean("security.guard-immunity.guard-to-guard-protection", true);
    }
    
    
    
    public boolean areBossBarsEnabled() {
        return config.getBoolean("user-interface.boss-bars.enabled", true);
    }
    
    public boolean isWantedBossBarEnabled() {
        return config.getBoolean("user-interface.boss-bars.wanted.enabled", true);
    }
    
    public String getWantedBossBarColor() {
        return config.getString("user-interface.boss-bars.wanted.color", "RED");
    }
    
    public String getWantedBossBarOverlay() {
        return config.getString("user-interface.boss-bars.wanted.overlay", "PROGRESS");
    }
    
    public boolean isChaseBossBarEnabled() {
        return config.getBoolean("user-interface.boss-bars.chase.enabled", true);
    }
    
    public String getChaseBossBarColor() {
        return config.getString("user-interface.boss-bars.chase.color", "BLUE");
    }
    
    public String getChaseBossBarOverlay() {
        return config.getString("user-interface.boss-bars.chase.overlay", "PROGRESS");
    }
    
    public boolean isCombatBossBarEnabled() {
        return config.getBoolean("user-interface.boss-bars.combat.enabled", true);
    }
    
    public String getCombatBossBarColor() {
        return config.getString("user-interface.boss-bars.combat.color", "RED");
    }
    
    public String getCombatBossBarOverlay() {
        return config.getString("user-interface.boss-bars.combat.overlay", "PROGRESS");
    }
    
    public boolean isJailBossBarEnabled() {
        return config.getBoolean("user-interface.boss-bars.jail.enabled", true);
    }
    
    public String getJailBossBarColor() {
        return config.getString("user-interface.boss-bars.jail.color", "PURPLE");
    }
    
    public String getJailBossBarOverlay() {
        return config.getString("user-interface.boss-bars.jail.overlay", "PROGRESS");
    }
    
    public boolean isDutyBossBarEnabled() {
        return config.getBoolean("user-interface.boss-bars.duty.enabled", true);
    }
    
    public String getDutyBossBarColor() {
        return config.getString("user-interface.boss-bars.duty.color", "GREEN");
    }
    
    public String getDutyBossBarOverlay() {
        return config.getString("user-interface.boss-bars.duty.overlay", "PROGRESS");
    }
    
    public boolean isContrabandBossBarEnabled() {
        return config.getBoolean("user-interface.boss-bars.contraband.enabled", true);
    }
    
    public String getContrabandBossBarColor() {
        return config.getString("user-interface.boss-bars.contraband.color", "YELLOW");
    }
    
    public String getContrabandBossBarOverlay() {
        return config.getString("user-interface.boss-bars.contraband.overlay", "PROGRESS");
    }
    
    public boolean isGraceBossBarEnabled() {
        return config.getBoolean("user-interface.boss-bars.grace-period.enabled", true);
    }
    
    public String getGraceBossBarColor() {
        return config.getString("user-interface.boss-bars.grace-period.color", "PINK");
    }
    
    public String getGraceBossBarOverlay() {
        return config.getString("user-interface.boss-bars.grace-period.overlay", "PROGRESS");
    }
    
    public boolean isPenaltyBossBarEnabled() {
        return config.getBoolean("user-interface.boss-bars.penalty.enabled", true);
    }
    
    public String getPenaltyBossBarColor() {
        return config.getString("user-interface.boss-bars.penalty.color", "RED");
    }
    
    public String getPenaltyBossBarOverlay() {
        return config.getString("user-interface.boss-bars.penalty.overlay", "PROGRESS");
    }
    
    public int getPenaltyBossBarDuration() {
        return config.getInt("user-interface.boss-bars.penalty.duration", 30);
    }

    
    
    public boolean isGuardTagsEnabled() {
        return config.getBoolean("guard-tags.enabled", true);
    }
    
    public String getGuardTagPrefix() {
        return config.getString("guard-tags.prefix", "🛡️");
    }
    
    public int getGuardTagPriority() {
        return config.getInt("guard-tags.priority", 100);
    }
    
    public boolean isGuardTagHoverEnabled() {
        return config.getBoolean("guard-tags.hover.enabled", true);
    }
    
    public String getGuardTagHoverTitle() {
        return config.getString("guard-tags.hover.title", "§6§l◆ CORRECTIONAL OFFICER ◆");
    }
    
    public String getGuardTagHoverRankFormat() {
        return config.getString("guard-tags.hover.rank-format", "§7Rank: §b{rank}");
    }
    
    public String getGuardTagHoverStatusFormat() {
        return config.getString("guard-tags.hover.status-format", "§7Status: §aON DUTY");
    }
    
    public String getGuardTagHoverSessionHeader() {
        return config.getString("guard-tags.hover.session-header", "§7═══ Session Stats ═══");
    }
    
    public String getGuardTagHoverSessionArrests() {
        return config.getString("guard-tags.hover.session-arrests", "§7Arrests: §e{arrests}");
    }
    
    public String getGuardTagHoverSessionSearches() {
        return config.getString("guard-tags.hover.session-searches", "§7Searches: §e{searches}");
    }
    
    public String getGuardTagHoverSessionDetections() {
        return config.getString("guard-tags.hover.session-detections", "§7Detections: §e{detections}");
    }
    
    public String getGuardTagHoverTotalHeader() {
        return config.getString("guard-tags.hover.total-header", "§7═══ Total Stats ═══");
    }
    
    public String getGuardTagHoverTotalArrests() {
        return config.getString("guard-tags.hover.total-arrests", "§7Total Arrests: §c{total_arrests}");
    }
    
    public String getGuardTagHoverDutyTime() {
        return config.getString("guard-tags.hover.duty-time", "§7Duty Time: §d{hours}h {minutes}m");
    }
    
    public String getGuardTagHoverOffDutyTime() {
        return config.getString("guard-tags.hover.off-duty-time", "§7Off-Duty Available: §a{minutes}m");
    }
    
    public String getGuardTagHoverNoOffDutyTime() {
        return config.getString("guard-tags.hover.no-off-duty-time", "§7Off-Duty Available: §cNone");
    }
    
    
    
    public boolean isWantedTagsEnabled() {
        return config.getBoolean("wanted-tags.enabled", true);
    }
    
    public String getWantedTagFormat() {
        return config.getString("wanted-tags.format", "<red><bold>WANTED {stars}</bold></red>");
    }
    
    public int getWantedTagPriority() {
        return config.getInt("wanted-tags.priority", 150);
    }
    
    
    
    
    public boolean isArrestMinigameEnabled() {
        return config.getBoolean("jail-system.minigame.enabled", true);
    }
    
    public int getArrestMinigameBarLength() {
        int len = config.getInt("jail-system.minigame.bar-length", 9);
        return Math.max(3, Math.min(30, len));
    }
    
    public int getArrestMinigameRefreshTicks() {
        int t = config.getInt("jail-system.minigame.refresh-ticks", 2);
        return Math.max(1, t);
    }
    
    public int getArrestMinigameSuccessStart() {
        String windowSize = config.getString("jail-system.minigame.success-window-size", "auto");
        if (!"auto".equalsIgnoreCase(windowSize)) {
            try {
                int size = Integer.parseInt(windowSize);
                int length = getArrestMinigameBarLength();
                int center = length / 2;
                return Math.max(0, center - size / 2);
            } catch (NumberFormatException ignored) {}
        }
        return config.getInt("jail-system.minigame.success-start", 3);
    }
    
    public int getArrestMinigameSuccessEnd() {
        String windowSize = config.getString("jail-system.minigame.success-window-size", "auto");
        if (!"auto".equalsIgnoreCase(windowSize)) {
            try {
                int size = Integer.parseInt(windowSize);
                int length = getArrestMinigameBarLength();
                int center = length / 2;
                return Math.min(length - 1, center + size / 2);
            } catch (NumberFormatException ignored) {}
        }
        return config.getInt("jail-system.minigame.success-end", 5);
    }
    
    
    public int getArrestMinigameMissThreshold() {
        int threshold = config.getInt("jail-system.minigame.miss-threshold", 3);
        return Math.max(1, threshold);
    }

    public String getArrestMinigamePointerChar() {
        return config.getString("jail-system.minigame.pointer-char", "■");
    }
    
    public String getArrestMinigameEmptyChar() {
        return config.getString("jail-system.minigame.empty-char", "□");
    }
    
    
    
    public boolean isArrestMinigameSpeedVariationEnabled() {
        return config.getBoolean("jail-system.minigame.speed-variation.enabled", false);
    }
    
    public int getArrestMinigameMinSpeed() {
        return Math.max(1, config.getInt("jail-system.minigame.speed-variation.min-speed", 1));
    }
    
    public int getArrestMinigameMaxSpeed() {
        return Math.max(getArrestMinigameMinSpeed(), config.getInt("jail-system.minigame.speed-variation.max-speed", 4));
    }
    
    public int getArrestMinigameSpeedChangeInterval() {
        return Math.max(1, config.getInt("jail-system.minigame.speed-variation.change-interval", 3));
    }
    
    public boolean isArrestMinigameDirectionChangeEnabled() {
        return config.getBoolean("jail-system.minigame.direction-change.enabled", true);
    }
    
    public boolean isArrestMinigameRandomDirectionEnabled() {
        return config.getBoolean("jail-system.minigame.direction-change.random-direction", false);
    }
    
    
    
    public String getArrestMinigameTitleColor() {
        String v = config.getString("messages.jail.minigame.colors.title", null);
        if (v != null && !v.isEmpty()) return v;
        return config.getString("jail-system.minigame.colors.title", "<gradient:#9D4EDD:#06FFA5>");
    }
    
    public String getArrestMinigameFrameColor() {
        String v = config.getString("messages.jail.minigame.colors.frame", null);
        if (v != null && !v.isEmpty()) return v;
        return config.getString("jail-system.minigame.colors.frame", "<color:#ADB5BD>");
    }
    
    public String getArrestMinigameSuccessWindowColor() {
        String v = config.getString("messages.jail.minigame.colors.success-window", null);
        if (v != null && !v.isEmpty()) return v;
        return config.getString("jail-system.minigame.colors.success-window", "<color:#51CF66>");
    }
    
    public String getArrestMinigamePointerSuccessColor() {
        String v = config.getString("messages.jail.minigame.colors.pointer-success", null);
        if (v != null && !v.isEmpty()) return v;
        return config.getString("jail-system.minigame.colors.pointer-success", "<color:#51CF66>");
    }
    
    public String getArrestMinigamePointerDangerColor() {
        String v = config.getString("messages.jail.minigame.colors.pointer-danger", null);
        if (v != null && !v.isEmpty()) return v;
        return config.getString("jail-system.minigame.colors.pointer-danger", "<color:#FF6B6B>");
    }
    
    public String getArrestMinigameHintColor() {
        String v = config.getString("messages.jail.minigame.colors.hint-text", null);
        if (v != null && !v.isEmpty()) return v;
        return config.getString("jail-system.minigame.colors.hint-text", "<color:#FFE066>");
    }
    
    
    
    public String getArrestMinigameTitle() {
        String v = config.getString("messages.jail.minigame.title", null);
        if (v != null && !v.isEmpty()) return v;
        return config.getString("jail-system.minigame.messages.title", "⛓ Arrest Minigame");
    }
    
    public String getArrestMinigameHint() {
        String v = config.getString("messages.jail.minigame.hint", null);
        if (v != null && !v.isEmpty()) return v;
        return config.getString("jail-system.minigame.messages.hint", "(Shift in green)");
    }
    
    public String getArrestMinigameBracketLeft() {
        String v = config.getString("messages.jail.minigame.bracket-left", null);
        if (v != null && !v.isEmpty()) return v;
        return config.getString("jail-system.minigame.messages.bracket-left", "[");
    }
    
    public String getArrestMinigameBracketRight() {
        String v = config.getString("messages.jail.minigame.bracket-right", null);
        if (v != null && !v.isEmpty()) return v;
        return config.getString("jail-system.minigame.messages.bracket-right", "]");
    }
    
    
    
    public boolean isArrestMinigameMissFeedbackEnabled() {
        return config.getBoolean("jail-system.minigame.miss-feedback.enabled", true);
    }
    
    public int getArrestMinigameMissFeedbackDuration() {
        return Math.max(10, config.getInt("jail-system.minigame.miss-feedback.duration-ticks", 40));
    }
    
    public String getArrestMinigameMissMessage() {
        return config.getString("jail-system.minigame.miss-feedback.message", 
            "<gradient:#FFA94D:#FFB570>⚠️ Miss</gradient> <color:#ADB5BD>Try again!</color>");
    }
    
    
    
    public boolean isArrestMinigameStartChaseOnSuccess() {
        return config.getBoolean("jail-system.minigame.on-success.start-chase", true);
    }
    
    public boolean isArrestMinigameClearWantedOnSuccess() {
        return config.getBoolean("jail-system.minigame.on-success.clear-wanted", false);
    }
    
    public int getArrestMinigameBonusWantedLevel() {
        return Math.max(0, config.getInt("jail-system.minigame.on-success.bonus-wanted", 1));
    }
    
    
    
    public boolean isPenaltyEscalationEnabled() {
        return config.getBoolean("guard-system.penalty-escalation.enabled", true);
    }
    
    public int getPenaltyGracePeriod() {
        return config.getInt("guard-system.penalty-escalation.grace-period", 5);
    }
    
    public int getPenaltyStage1Time() {
        return config.getInt("guard-system.penalty-escalation.stages.stage-1.time-minutes", 5);
    }
    
    public int getPenaltyStage1SlownessLevel() {
        return config.getInt("guard-system.penalty-escalation.stages.stage-1.slowness-level", 1);
    }
    
    public int getPenaltyStage1EconomyPenalty() {
        return config.getInt("guard-system.penalty-escalation.stages.stage-1.economy-penalty", 1000);
    }
    
    public boolean isPenaltyStage1WarningEnabled() {
        return config.getBoolean("guard-system.penalty-escalation.stages.stage-1.warning-message", true);
    }
    
    public int getPenaltyStage2Time() {
        return config.getInt("guard-system.penalty-escalation.stages.stage-2.time-minutes", 10);
    }
    
    public int getPenaltyStage2SlownessLevel() {
        return config.getInt("guard-system.penalty-escalation.stages.stage-2.slowness-level", 2);
    }
    
    public int getPenaltyStage2EconomyPenalty() {
        return config.getInt("guard-system.penalty-escalation.stages.stage-2.economy-penalty", 1000);
    }
    
    public boolean isPenaltyStage2WarningEnabled() {
        return config.getBoolean("guard-system.penalty-escalation.stages.stage-2.warning-message", true);
    }
    
    public int getPenaltyRecurringInterval() {
        return config.getInt("guard-system.penalty-escalation.stages.recurring.interval-minutes", 5);
    }
    
    public int getPenaltyRecurringSlownessLevel() {
        return config.getInt("guard-system.penalty-escalation.stages.recurring.slowness-level", 2);
    }
    
    public int getPenaltyRecurringEconomyPenalty() {
        return config.getInt("guard-system.penalty-escalation.stages.recurring.economy-penalty", 1000);
    }
    
    public boolean isPenaltyRecurringWarningEnabled() {
        return (Boolean) getConfigValue("penalty-escalation.recurring.warning-enabled", true);
    }

    public int getPenaltyBypassEarnedTimeBonus() {
        return (Integer) getConfigValue("penalty-escalation.bypass.earned-time-bonus", 60);
    }
    
    public boolean isPenaltyBypassClearTracking() {
        return (Boolean) getConfigValue("penalty-escalation.bypass.clear-penalty-tracking", true);
    }
    
    public boolean isPenaltyBypassRemovePotionEffects() {
        return (Boolean) getConfigValue("penalty-escalation.bypass.remove-potion-effects", true);
    }

    
    
    public void setDebugMode(boolean debug) {
        config.set("core.debug", debug);
        plugin.saveConfig();
    }
    
    public void setConfigValue(String path, Object value) {
        config.set(path, value);
        plugin.saveConfig();
    }
    
    public Object getConfigValue(String path, Object defaultValue) {
        return config.get(path, defaultValue);
    }
    
    public boolean hasConfigValue(String path) {
        return config.contains(path);
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    
    
    public void backupConfig() {
        try {
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            File backupFile = new File(plugin.getDataFolder(), "config.yml.backup");
            
            if (configFile.exists()) {
                YamlConfiguration backupConfig = YamlConfiguration.loadConfiguration(configFile);
                backupConfig.save(backupFile);
                logger.info("Configuration backed up to config.yml.backup");
            }
        } catch (Exception e) {
            logger.severe("Failed to backup configuration: " + e.getMessage());
        }
    }
    
    public void restoreConfig() {
        try {
            File backupFile = new File(plugin.getDataFolder(), "config.yml.backup");
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            
            if (backupFile.exists()) {
                YamlConfiguration backupConfig = YamlConfiguration.loadConfiguration(backupFile);
                backupConfig.save(configFile);
                loadConfig();
                logger.info("Configuration restored from backup");
            } else {
                logger.warning("No backup configuration found");
            }
        } catch (Exception e) {
            logger.severe("Failed to restore configuration: " + e.getMessage());
        }
    }
    
    
    
    
    public boolean isStrictRankEnforcementEnabled() {
        return config.getBoolean("security.strict-rank-enforcement", true);
    }
    
    
    public boolean isAdminBypassEnabled() {
        return config.getBoolean("security.admin-bypass.enabled", false);
    }
    
    
    public boolean isLuckPermsRequired() {
        return config.getBoolean("security.guard-validation.require-luckperms", true);
    }
    
    
    public boolean shouldLogUnauthorizedAttempts() {
        return config.getBoolean("security.guard-validation.log-unauthorized-attempts", true);
    }
    
    
    public boolean shouldWarnUnauthorizedPlayers() {
        return config.getBoolean("security.guard-validation.warn-unauthorized-players", true);
    }
    
    
    public boolean isUnauthorizedAccessAntiSpamEnabled() {
        return config.getBoolean("security.guard-validation.anti-spam.enabled", true);
    }
    
    
    public int getUnauthorizedAccessWarningCooldown() {
        return config.getInt("security.guard-validation.anti-spam.warning-cooldown", 300);
    }
    
    
    public int getUnauthorizedAccessLogCooldown() {
        return config.getInt("security.guard-validation.anti-spam.log-cooldown", 60);
    }
    
    
    public int getMaxUnauthorizedAccessWarnings() {
        return config.getInt("security.guard-validation.anti-spam.max-warnings", 3);
    }
    
    
    public int getMaxUnauthorizedAccessLogs() {
        return config.getInt("security.guard-validation.anti-spam.max-logs", 5);
    }
    
    
    public boolean isDutyNotificationsEnabled() {
        return config.getBoolean("security.duty-notifications.enabled", true);
    }
    
    
    public String getDutyNotificationMethod() {
        return config.getString("security.duty-notifications.method", "actionbar");
    }
    
    
    public int getDutyNotificationDuration() {
        return config.getInt("security.duty-notifications.actionbar-duration", 5);
    }
    
    
    
    
    public boolean isChatControlIntegrationEnabled() {
        return config.getBoolean("integration.chatcontrol.enabled", true);
    }
    
    
    public boolean preferChatControlOverLuckPerms() {
        return config.getBoolean("integration.chatcontrol.prefer-over-luckperms", true);
    }
    
    
    public boolean isChatControlGuardChannelEnabled() {
        return config.getBoolean("integration.chatcontrol.guard-channel.enabled", true);
    }
    
    
    public String getChatControlGuardChannelName() {
        return config.getString("integration.chatcontrol.guard-channel.name", "edencorrections-guards");
    }
    
    
    
    
    public boolean isChatControlGuardTagsEnabled() {
        return config.getBoolean("integration.chatcontrol.guard-tags.enabled", true);
    }
    
    
    public String getChatControlGuardTagPrefix() {
        return config.getString("integration.chatcontrol.guard-tags.prefix", "🛡️");
    }
    
    
    public boolean isChatControlGuardTagHoverEnabled() {
        return config.getBoolean("integration.chatcontrol.guard-tags.hover.enabled", true);
    }
    
    
    public String getChatControlGuardTagHoverTitle() {
        return config.getString("integration.chatcontrol.guard-tags.hover.title", "§6§l◆ CORRECTIONAL OFFICER ◆");
    }
    
    
    public String getChatControlGuardTagHoverRankFormat() {
        return config.getString("integration.chatcontrol.guard-tags.hover.rank-format", "§7Rank: §b{rank}");
    }
    
    
    public String getChatControlGuardTagHoverStatusFormat() {
        return config.getString("integration.chatcontrol.guard-tags.hover.status-format", "§7Status: §aON DUTY");
    }
    
    
    public String getChatControlGuardTagHoverSessionHeader() {
        return config.getString("integration.chatcontrol.guard-tags.hover.session-header", "§7═══ Session Stats ═══");
    }
    
    
    public String getChatControlGuardTagHoverSessionArrests() {
        return config.getString("integration.chatcontrol.guard-tags.hover.session-arrests", "§7Arrests: §e{arrests}");
    }
    
    
    public String getChatControlGuardTagHoverSessionSearches() {
        return config.getString("integration.chatcontrol.guard-tags.hover.session-searches", "§7Searches: §e{searches}");
    }
    
    
    public String getChatControlGuardTagHoverSessionDetections() {
        return config.getString("integration.chatcontrol.guard-tags.hover.session-detections", "§7Detections: §e{detections}");
    }
    
    
    public String getChatControlGuardTagHoverTotalHeader() {
        return config.getString("integration.chatcontrol.guard-tags.hover.total-header", "§7═══ Total Stats ═══");
    }
    
    
    public String getChatControlGuardTagHoverTotalArrests() {
        return config.getString("integration.chatcontrol.guard-tags.hover.total-arrests", "§7Total Arrests: §c{total_arrests}");
    }
    
    
    public String getChatControlGuardTagHoverDutyTime() {
        return config.getString("integration.chatcontrol.guard-tags.hover.duty-time", "§7Duty Time: §d{hours}h {minutes}m");
    }
    
    
    public String getChatControlGuardTagHoverOffDutyTime() {
        return config.getString("integration.chatcontrol.guard-tags.hover.off-duty-time", "§7Off-Duty Available: §a{minutes}m");
    }
    
    
    public String getChatControlGuardTagHoverNoOffDutyTime() {
        return config.getString("integration.chatcontrol.guard-tags.hover.no-off-duty-time", "§7Off-Duty Available: §cNone");
    }
    
    
    public boolean isChatControlWantedTagsEnabled() {
        return config.getBoolean("integration.chatcontrol.wanted-tags.enabled", true);
    }
    
    
    public String getChatControlWantedTagFormat() {
        return config.getString("integration.chatcontrol.wanted-tags.format", "&4[WANTED {level}★]");
    }

    
    
    
    public int getRankLimit(String rank) {
        String path = "progression.rank-limits." + rank.toLowerCase();
        return config.getInt(path, -1); 
    }
    
    
    public boolean isRankPromotionEnabled(String rank) {
        return getRankLimit(rank) != 0;
    }
    
    
    public boolean isRankUnlimited(String rank) {
        return getRankLimit(rank) == -1;
    }
    
    
    public Map<String, Integer> getAllRankLimits() {
        Map<String, Integer> limits = new HashMap<>();
        
        
        Map<String, String> rankMappings = getRankMappings();
        for (String rank : rankMappings.keySet()) {
            limits.put(rank, getRankLimit(rank));
        }
        
        return limits;
    }
}
