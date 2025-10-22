package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Logger;

import static dev.lsdmc.edenCorrections.managers.MessageManager.*;

public class DutyBankingManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    public DutyBankingManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    public void initialize() {
        if (!plugin.getConfigManager().isDutyBankingEnabled()) {
            logger.info("DutyBankingManager initialized (disabled by config)");
            return;
        }
        
        logger.info("DutyBankingManager initialized successfully!");
        
        
        if (plugin.getConfigManager().isAutoConvert()) {
            startAutoConversionMonitoring();
        }
    }
    
    private void startAutoConversionMonitoring() {
        new BukkitRunnable() {
            @Override
            public void run() {
                checkAutoConversions();
            }
        }.runTaskTimer(plugin, 20L * 60L, 20L * 60L); 
    }
    
    private void checkAutoConversions() {
        int threshold = plugin.getConfigManager().getAutoConvertThreshold();
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (plugin.getDutyManager().isSubjectToGuardRestrictions(player)) {
                PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
                if (data != null) {
                    long totalDutyTime = data.getTotalDutyTime() / 1000L; 
                    
                    if (totalDutyTime >= threshold) {
                        
                        performAutoConversion(player, data);
                    }
                }
            }
        }
    }
    
    private void performAutoConversion(Player player, PlayerData data) {
        long totalDutyTime = data.getTotalDutyTime() / 1000L; 
        int conversionRate = plugin.getConfigManager().getConversionRate();
        int minimumConversion = plugin.getConfigManager().getMinimumConversion();
        
        if (totalDutyTime < minimumConversion) {
            return; 
        }
        
        
        int tokensToGive = (int) (totalDutyTime / conversionRate);
        if (tokensToGive <= 0) return;
        
        
        long timeUsed = tokensToGive * conversionRate;
        long remainingTime = (totalDutyTime - timeUsed) * 1000L; 
        
        
        if (giveTokens(player, tokensToGive)) {
            
            data.setTotalDutyTime(remainingTime);
            plugin.getDataManager().savePlayerData(data);
            
            
            plugin.getMessageManager().sendMessage(player, "banking.auto-conversion",
                timePlaceholder("time", timeUsed),
                numberPlaceholder("tokens", tokensToGive));
            
            logger.info("Auto-converted " + timeUsed + " seconds of duty time to " + 
                       tokensToGive + " tokens for " + player.getName());
        }
    }

    
    
    public void handleDutyEnd(Player player, long dutyTimeSeconds) {
        if (!plugin.getConfigManager().isDutyBankingEnabled()) {
            return;
        }
        
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
        
        
        
        
        if (plugin.getConfigManager().isAutoConvert()) {
            long totalDutyTime = data.getTotalDutyTime() / 1000L;
            int threshold = plugin.getConfigManager().getAutoConvertThreshold();
            
            if (totalDutyTime >= threshold) {
                performAutoConversion(player, data);
            }
        } else {
            
            int availableTokens = getAvailableTokens(player);
            long totalDutyTime = data.getTotalDutyTime() / 1000L;
            
            
            if (availableTokens >= 5) {
                
                plugin.getMessageManager().sendActionBar(player, "actionbar.banking-available", 
                    MessageManager.timePlaceholder("time", totalDutyTime),
                    MessageManager.numberPlaceholder("tokens", availableTokens));
            }
        }
        
        if (plugin.getConfigManager().isDebugMode()) {
            logger.info("DEBUG: Duty session ended for " + player.getName() + " (Session: " + dutyTimeSeconds + "s, Total: " + (data.getTotalDutyTime() / 1000L) + "s)");
        }
    }

    
    
    public boolean convertDutyTime(Player player) {
        if (!plugin.getConfigManager().isDutyBankingEnabled()) {
            plugin.getMessageManager().sendMessage(player, "banking.disabled");
            return false;
        }
        
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
        long totalDutyTime = data.getTotalDutyTime() / 1000L; 
        
        int conversionRate = plugin.getConfigManager().getConversionRate();
        int minimumConversion = plugin.getConfigManager().getMinimumConversion();
        
        
        if (totalDutyTime < minimumConversion) {
            plugin.getMessageManager().sendMessage(player, "banking.insufficient-time",
                timePlaceholder("minimum", minimumConversion));
            return false;
        }
        
        
        int tokensToGive = (int) (totalDutyTime / conversionRate);
        if (tokensToGive <= 0) {
            plugin.getMessageManager().sendMessage(player, "banking.insufficient-time",
                timePlaceholder("minimum", minimumConversion));
            return false;
        }
        
        
        long timeUsed = tokensToGive * conversionRate;
        long remainingTime = (totalDutyTime - timeUsed) * 1000L; 
        
        
        if (giveTokens(player, tokensToGive)) {
            
            data.setTotalDutyTime(remainingTime);
            plugin.getDataManager().savePlayerData(data);
            
            
            plugin.getBossBarManager().showGraceBossBar(player, 5);
            
            
            plugin.getMessageManager().sendMessage(player, "banking.conversion-success",
                timePlaceholder("time", timeUsed),
                numberPlaceholder("tokens", tokensToGive));
            
            logger.info("Converted " + timeUsed + " seconds of duty time to " + 
                       tokensToGive + " tokens for " + player.getName());
            return true;
        }
        
        return false;
    }
    
    private boolean giveTokens(Player player, int amount) {
        try {
            String command = plugin.getConfigManager().getCurrencyCommand()
                .replace("{player}", player.getName())
                .replace("{amount}", String.valueOf(amount));
            
            
            boolean result = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            
            if (!result) {
                logger.warning("Failed to execute currency command: " + command);
                plugin.getMessageManager().sendMessage(player, "universal.failed");
                return false;
            }
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Executed currency command: " + command);
            }
            
            return true;
        } catch (Exception e) {
            logger.severe("Error executing currency command for " + player.getName() + ": " + e.getMessage());
            plugin.getMessageManager().sendMessage(player, "universal.failed");
            return false;
        }
    }

    
    
    public boolean showBankingStatus(Player player) {
        if (!plugin.getConfigManager().isDutyBankingEnabled()) {
            plugin.getMessageManager().sendMessage(player, "banking.disabled");
            return false;
        }
        
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
        long totalDutyTime = data.getTotalDutyTime() / 1000L; 
        int availableTokens = getAvailableTokens(player);
        
        plugin.getMessageManager().sendMessage(player, "banking.status",
            timePlaceholder("time", totalDutyTime),
            numberPlaceholder("tokens", availableTokens));
        
        return true;
    }
    
    public int getAvailableTokens(Player player) {
        if (!plugin.getConfigManager().isDutyBankingEnabled()) {
            return 0;
        }
        
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return 0;
        
        long totalDutyTime = data.getTotalDutyTime() / 1000L; 
        int conversionRate = plugin.getConfigManager().getConversionRate();
        int minimumConversion = plugin.getConfigManager().getMinimumConversion();
        
        if (totalDutyTime < minimumConversion) {
            return 0;
        }
        
        return (int) (totalDutyTime / conversionRate);
    }
    
    public long getTotalDutyTime(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return 0;
        
        return data.getTotalDutyTime() / 1000L; 
    }
    
    public boolean hasMinimumDutyTime(Player player) {
        long totalTime = getTotalDutyTime(player);
        int minimum = plugin.getConfigManager().getMinimumConversion();
        return totalTime >= minimum;
    }

    
    
    public void addDutyTime(Player player, long timeInSeconds) {
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
        data.addDutyTime(timeInSeconds * 1000L);
        plugin.getDataManager().savePlayerData(data);
        
        logger.info("Admin added " + timeInSeconds + " seconds of duty time to " + player.getName());
    }
    
    public void removeDutyTime(Player player, long timeInSeconds) {
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
        long currentTime = data.getTotalDutyTime();
        long newTime = Math.max(0, currentTime - (timeInSeconds * 1000L));
        
        data.setTotalDutyTime(newTime);
        plugin.getDataManager().savePlayerData(data);
        
        logger.info("Admin removed " + timeInSeconds + " seconds of duty time from " + player.getName());
    }
    
    public void resetDutyTime(Player player) {
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
        data.setTotalDutyTime(0);
        plugin.getDataManager().savePlayerData(data);
        
        logger.info("Admin reset duty time for " + player.getName());
    }

    
    
    public String getFormattedConversionRate() {
        int rate = plugin.getConfigManager().getConversionRate();
        return rate + " seconds = 1 token";
    }
    
    public String getFormattedMinimumTime() {
        int minimum = plugin.getConfigManager().getMinimumConversion();
        return formatTime(minimum);
    }
    
    public String getFormattedAutoConvertThreshold() {
        int threshold = plugin.getConfigManager().getAutoConvertThreshold();
        return formatTime(threshold);
    }
    
    private String formatTime(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        
        if (hours > 0) {
            return hours + "h " + minutes + "m " + secs + "s";
        } else if (minutes > 0) {
            return minutes + "m " + secs + "s";
        } else {
            return secs + "s";
        }
    }

    
    
    public void cleanup() {
        
        logger.info("DutyBankingManager cleaned up successfully");
    }
} 