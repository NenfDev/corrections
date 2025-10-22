package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import dev.lsdmc.edenCorrections.utils.LoggingUtils;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;
import java.util.logging.Logger;

import static dev.lsdmc.edenCorrections.managers.MessageManager.*;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public class WantedManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    
    private BukkitTask monitoringTask;
    
    public WantedManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    public void initialize() {
        LoggingUtils.info(logger, "WantedManager initialized successfully!");
        
        
        startWantedMonitoring();
    }
    
    private void startWantedMonitoring() {
        monitoringTask = new BukkitRunnable() {
            @Override
            public void run() {
                
                plugin.getDataManager().cleanupExpiredWantedLevels();
            }
        }.runTaskTimer(plugin, 20L * 60L, 20L * 60L); 
    }
    
    public boolean setWantedLevel(Player target, int level, String reason) {
        
        if (!plugin.getSecurityManager().canPlayerBeWanted(target)) {
            plugin.getMessageManager().sendMessage(target, "security.guard-immunity.wanted-protected");
            plugin.getSecurityManager().logSecurityViolation("set wanted level", null, target);
            return false;
        }
        
        try {
            if (plugin.getWorldGuardUtils().isPlayerInBreakZone(target)) {
                if (plugin.getConfigManager().isDebugMode()) {
                    logger.info("DEBUG: Skipping wanted set for " + target.getName() + " in break zone");
                }
                return false;
            }
        } catch (Exception ignored) {}
        
        return setWantedLevel(target.getUniqueId(), target.getName(), level, reason);
    }
    
    public boolean setWantedLevel(UUID targetId, String targetName, int level, String reason) {
        if (level < 0 || level > plugin.getConfigManager().getMaxWantedLevel()) {
            return false;
        }
        
        
        Player targetPlayer = plugin.getServer().getPlayer(targetId);
        if (targetPlayer != null && plugin.getDutyManager().isOnDuty(targetPlayer)) {
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Attempted to set wanted level on guard on duty: " + targetName);
            }
            return false;
        }
        
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(targetId, targetName);
        
        if (level == 0) {
            clearWantedLevel(targetId);
            return true;
        }
        
        data.setWantedLevel(level);
        int durationSeconds = plugin.getConfigManager().getWantedDurationForLevel(level);
        data.setWantedExpireTime(System.currentTimeMillis() + durationSeconds * 1000L);
        data.setWantedReason(reason);
        
        plugin.getDataManager().savePlayerData(data);
        
        
        if (targetPlayer != null) {
            plugin.getMessageManager().sendMessage(targetPlayer, "wanted.level.set",
                numberPlaceholder("level", level),
                starsPlaceholder("stars", level));
            plugin.getMessageManager().sendMessage(targetPlayer, "wanted.level.reason",
                stringPlaceholder("reason", reason));
            
            
            plugin.getBossBarManager().showWantedBossBar(targetPlayer, level, durationSeconds);
            
            
        }
        
        logger.info(targetName + "'s wanted level set to " + level + " - Reason: " + reason);
        return true;
    }
    
    public boolean increaseWantedLevel(Player target, int amount, String reason) {
        
        if (!plugin.getSecurityManager().canPlayerBeWanted(target)) {
            plugin.getMessageManager().sendMessage(target, "security.guard-immunity.wanted-protected");
            plugin.getSecurityManager().logSecurityViolation("increase wanted level", null, target);
            return false;
        }
        
        try {
            if (plugin.getWorldGuardUtils().isPlayerInBreakZone(target)) {
                if (plugin.getConfigManager().isDebugMode()) {
                    logger.info("DEBUG: Skipping wanted increase for " + target.getName() + " in break zone");
                }
                return false;
            }
        } catch (Exception ignored) {}
        
        return increaseWantedLevel(target.getUniqueId(), target.getName(), amount, reason);
    }
    
    public boolean increaseWantedLevel(UUID targetId, String targetName, int amount, String reason) {
        
        Player targetPlayer = plugin.getServer().getPlayer(targetId);
        if (targetPlayer != null && plugin.getDutyManager().isOnDuty(targetPlayer)) {
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Attempted to increase wanted level on guard on duty: " + targetName);
            }
            return false;
        }
        
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(targetId, targetName);
        
        int currentLevel = data.getWantedLevel();
        int newLevel = Math.min(currentLevel + amount, plugin.getConfigManager().getMaxWantedLevel());
        
        if (newLevel == currentLevel) {
            return false; 
        }
        
        return setWantedLevel(targetId, targetName, newLevel, reason);
    }
    
    public boolean clearWantedLevel(Player target) {
        return clearWantedLevel(target.getUniqueId());
    }
    
    public boolean clearWantedLevel(UUID targetId) {
        PlayerData data = plugin.getDataManager().getPlayerData(targetId);
        if (data == null || !data.isWanted()) {
            return false;
        }
        
        
        Player targetPlayer = plugin.getServer().getPlayer(targetId);
        if (targetPlayer != null && data.getWantedLevel() >= 3) {
            targetPlayer.setGlowing(false);
        }
        
        data.clearWantedLevel();
        plugin.getDataManager().savePlayerData(data);
        
        if (targetPlayer != null) {
            plugin.getMessageManager().sendMessage(targetPlayer, "wanted.level.cleared");
            plugin.getBossBarManager().hideBossBarByType(targetPlayer, "wanted");
            
            
        }
        
        logger.info(data.getPlayerName() + "'s wanted level has been cleared");
        return true;
    }
    
    public int getWantedLevel(Player player) {
        return getWantedLevel(player.getUniqueId());
    }
    
    public int getWantedLevel(UUID playerId) {
        PlayerData data = plugin.getDataManager().getPlayerData(playerId);
        return data != null ? data.getWantedLevel() : 0;
    }
    
    public boolean isWanted(Player player) {
        return isWanted(player.getUniqueId());
    }
    
    public boolean isWanted(UUID playerId) {
        PlayerData data = plugin.getDataManager().getPlayerData(playerId);
        return data != null && data.isWanted();
    }
    
    public long getRemainingWantedTime(Player player) {
        return getRemainingWantedTime(player.getUniqueId());
    }
    
    public long getRemainingWantedTime(UUID playerId) {
        PlayerData data = plugin.getDataManager().getPlayerData(playerId);
        return data != null ? data.getRemainingWantedTime() : 0;
    }
    
    public String getWantedReason(Player player) {
        return getWantedReason(player.getUniqueId());
    }
    
    public String getWantedReason(UUID playerId) {
        PlayerData data = plugin.getDataManager().getPlayerData(playerId);
        return data != null ? data.getWantedReason() : "";
    }
    
    public void handlePlayerKillGuard(Player player, Player guard) {
        
        if (!isRealPlayer(player) || !isRealPlayer(guard)) {
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Skipping wanted level for NPC kill (guard kill)");
            }
            return;
        }
        
        
        increaseWantedLevel(player, 2, "Killing a guard");
        
        
        notifyGuards("wanted.alerts.guard-killed",
            playerPlaceholder("player", player),
            playerPlaceholder("guard", guard));
    }
    
    public void handlePlayerKillPlayer(Player player, Player victim) {
        
        if (!isRealPlayer(player) || !isRealPlayer(victim)) {
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Skipping wanted level for NPC kill (player kill)");
            }
            return;
        }
        
        
        increaseWantedLevel(player, 1, "Killing another player");
    }
    
    public void handleContrabandViolation(Player player, String contrabandType) {
        
        increaseWantedLevel(player, 1, "Contraband violation: " + contrabandType);
    }
    
    public void handleChaseEscape(Player player) {
        
        increaseWantedLevel(player, 1, "Escaping from chase");
    }
    
    public String getWantedStars(int level) {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < level; i++) {
            stars.append("⭐");
        }
        return stars.toString();
    }
    
    private void notifyGuards(String messageKey, TagResolver... placeholders) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (plugin.getDutyManager().isSubjectToGuardRestrictions(player) && plugin.getDutyManager().isOnDuty(player)) {
                plugin.getMessageManager().sendGuardAlert(messageKey, placeholders);
            }
        }
    }
    
    
    private boolean isRealPlayer(Player player) {
        if (player == null) {
            return false;
        }
        
        
        if (player.hasMetadata("NPC")) {
            return false;
        }
        
        
        try {
            if (plugin.getServer().getPluginManager().getPlugin("Citizens") != null) {
                if (net.citizensnpcs.api.CitizensAPI.getNPCRegistry().isNPC(player)) {
                    return false;
                }
            }
        } catch (Exception e) {
            
        }
        
        
        
        return true;
    }
    
    
    public void cleanup() {
        LoggingUtils.info(logger, "Cleaning up WantedManager resources...");
        
        try {
            
            if (monitoringTask != null && !monitoringTask.isCancelled()) {
                try {
                    monitoringTask.cancel();
                    LoggingUtils.info(logger, "Cancelled wanted level monitoring task");
                } catch (Exception e) {
                    LoggingUtils.warn(logger, "Error cancelling wanted monitoring task: " + e.getMessage());
                }
            }
            
            LoggingUtils.info(logger, "WantedManager cleanup completed");
        } catch (Exception e) {
            LoggingUtils.error(logger, "Error during WantedManager cleanup: " + e.getMessage());
        }
    }
    
    
    
    
    private void setWantedTag(Player player, int level, String reason) { }
    
    
    private void removeWantedTag(Player player) { }
} 