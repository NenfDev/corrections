package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;


public class UnauthorizedAccessSpamManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    
    private final Map<UUID, Long> lastWarningTime = new HashMap<>();
    private final Map<UUID, Integer> warningCount = new HashMap<>();
    
    
    private final Map<UUID, Long> lastLogTime = new HashMap<>();
    private final Map<UUID, Integer> logCount = new HashMap<>();
    
    public UnauthorizedAccessSpamManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    
    public boolean shouldWarnPlayer(Player player) {
        if (!plugin.getConfigManager().isUnauthorizedAccessAntiSpamEnabled()) {
            return plugin.getConfigManager().shouldWarnUnauthorizedPlayers();
        }
        
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        int cooldownMs = plugin.getConfigManager().getUnauthorizedAccessWarningCooldown() * 1000;
        int maxWarnings = plugin.getConfigManager().getMaxUnauthorizedAccessWarnings();
        
        
        int currentWarnings = warningCount.getOrDefault(playerId, 0);
        if (maxWarnings > 0 && currentWarnings >= maxWarnings) {
            return false;
        }
        
        
        Long lastWarning = lastWarningTime.get(playerId);
        if (lastWarning != null && currentTime - lastWarning < cooldownMs) {
            return false;
        }
        
        
        lastWarningTime.put(playerId, currentTime);
        warningCount.put(playerId, currentWarnings + 1);
        
        return plugin.getConfigManager().shouldWarnUnauthorizedPlayers();
    }
    
    
    public boolean shouldLogToConsole(Player player) {
        if (!plugin.getConfigManager().isUnauthorizedAccessAntiSpamEnabled()) {
            return plugin.getConfigManager().shouldLogUnauthorizedAttempts();
        }
        
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        int cooldownMs = plugin.getConfigManager().getUnauthorizedAccessLogCooldown() * 1000;
        int maxLogs = plugin.getConfigManager().getMaxUnauthorizedAccessLogs();
        
        
        int currentLogs = logCount.getOrDefault(playerId, 0);
        if (maxLogs > 0 && currentLogs >= maxLogs) {
            return false;
        }
        
        
        Long lastLog = lastLogTime.get(playerId);
        if (lastLog != null && currentTime - lastLog < cooldownMs) {
            return false;
        }
        
        
        lastLogTime.put(playerId, currentTime);
        logCount.put(playerId, currentLogs + 1);
        
        return plugin.getConfigManager().shouldLogUnauthorizedAttempts();
    }
    
    
    public void logUnauthorizedAccess(Player player, String action) {
        if (shouldLogToConsole(player)) {
            logger.warning("Unauthorized guard access attempt by " + player.getName() + " - Action: " + action);
        }
    }
    
    
    public void logUnauthorizedAccess(Player player, String message, boolean isWarning) {
        if (shouldLogToConsole(player)) {
            if (isWarning) {
                logger.warning(message);
            } else {
                logger.info(message);
            }
        }
    }
    
    
    public void resetPlayerTracking(Player player) {
        UUID playerId = player.getUniqueId();
        lastWarningTime.remove(playerId);
        warningCount.remove(playerId);
        lastLogTime.remove(playerId);
        logCount.remove(playerId);
    }
    
    
    public Map<String, Object> getPlayerSpamStats(Player player) {
        UUID playerId = player.getUniqueId();
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("warning_count", warningCount.getOrDefault(playerId, 0));
        stats.put("log_count", logCount.getOrDefault(playerId, 0));
        
        Long lastWarning = lastWarningTime.get(playerId);
        Long lastLog = lastLogTime.get(playerId);
        
        if (lastWarning != null) {
            long timeSinceWarning = (System.currentTimeMillis() - lastWarning) / 1000;
            stats.put("seconds_since_last_warning", timeSinceWarning);
        }
        
        if (lastLog != null) {
            long timeSinceLog = (System.currentTimeMillis() - lastLog) / 1000;
            stats.put("seconds_since_last_log", timeSinceLog);
        }
        
        return stats;
    }
    
    
    public void clearAllTracking() {
        lastWarningTime.clear();
        warningCount.clear();
        lastLogTime.clear();
        logCount.clear();
        logger.info("Cleared all unauthorized access spam tracking data");
    }
    
    
    public Map<String, Object> getTotalStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("tracked_players", lastWarningTime.size());
        stats.put("total_warnings", warningCount.values().stream().mapToInt(Integer::intValue).sum());
        stats.put("total_logs", logCount.values().stream().mapToInt(Integer::intValue).sum());
        return stats;
    }
}
