package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import dev.lsdmc.edenCorrections.models.ChaseData;
import dev.lsdmc.edenCorrections.utils.LoggingUtils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.UUID;
import java.util.logging.Logger;
import java.util.Collection;
import org.bukkit.Location;
import org.bukkit.potion.PotionEffectType;

import static dev.lsdmc.edenCorrections.managers.MessageManager.*;

public class ChaseManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    
    private final Map<UUID, Long> combatTimers;
    private final Map<UUID, BukkitTask> combatTasks;
    
    public ChaseManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.combatTimers = new HashMap<>();
        this.combatTasks = new HashMap<>();
    }
    
    public void initialize() {
        LoggingUtils.info(logger, "ChaseManager initialized successfully!");
        
        
        cleanupInvalidChases();
        
        
        startChaseMonitoring();
    }
    
    public void shutdown() {
        LoggingUtils.info(logger, "ChaseManager shutting down...");
        
        try {
            
            for (ChaseData chase : plugin.getDataManager().getAllActiveChases()) {
                try {
                    endChase(chase.getChaseId(), plugin.getMessageManager().getPlainTextMessage("chase.end-reasons.plugin-shutdown"));
                } catch (Exception e) {
                    LoggingUtils.warn(logger, "Error ending chase " + chase.getChaseId() + " during shutdown: " + e.getMessage());
                }
            }
            
            
            cleanupAllCombatTimers();
            
            LoggingUtils.info(logger, "ChaseManager shutdown complete");
        } catch (Exception e) {
            LoggingUtils.error(logger, "Error during ChaseManager shutdown: " + e.getMessage());
        }
    }
    
    private void startChaseMonitoring() {
        new BukkitRunnable() {
            @Override
            public void run() {
                
                monitorActiveChases();
            }
        }.runTaskTimer(plugin, 20L * 10L, 20L * 10L); 
    }
    
    private void monitorActiveChases() {
        try {
            Collection<ChaseData> activeChases = plugin.getDataManager().getAllActiveChases();
            if (activeChases == null || activeChases.isEmpty()) {
                return;
            }
            
            
            List<ChaseData> chasesToMonitor = new ArrayList<>(activeChases);
            
            
            Map<UUID, Location> playerLocations = new HashMap<>();
            for (ChaseData chase : chasesToMonitor) {
                if (chase != null && chase.getGuardId() != null && chase.getTargetId() != null) {
                    Player guard = plugin.getServer().getPlayer(chase.getGuardId());
                    Player target = plugin.getServer().getPlayer(chase.getTargetId());
                    
                    if (guard != null) playerLocations.put(chase.getGuardId(), guard.getLocation());
                    if (target != null) playerLocations.put(chase.getTargetId(), target.getLocation());
                }
            }
            
            
            for (ChaseData chase : chasesToMonitor) {
                try {
                    
                    if (chase == null) {
                        logger.warning("Found null chase data in active chases - skipping");
                        continue;
                    }
                    
                    UUID chaseId = chase.getChaseId();
                    UUID guardId = chase.getGuardId();
                    UUID targetId = chase.getTargetId();
                    
                    
                    if (chaseId == null || guardId == null || targetId == null) {
                        logger.warning("Found chase with null UUIDs: chaseId=" + chaseId + 
                                     ", guardId=" + guardId + ", targetId=" + targetId + " - ending chase");
                        if (chaseId != null) {
                            endChase(chaseId, "Invalid chase data (null UUIDs)");
                        } else {
                            
                            plugin.getDataManager().removeChaseData(chase.getChaseId());
                        }
                        continue;
                    }
                    
                    
                    Location guardLoc = playerLocations.get(guardId);
                    Location targetLoc = playerLocations.get(targetId);
            
                    
                    if (guardLoc == null || targetLoc == null) {
                        String offlinePlayer = guardLoc == null ? "guard" : "target";
                        endChase(chaseId, "Player offline (" + offlinePlayer + ")");
                        continue;
                    }
                    
                    
                    try {
                        
                        
                        if (!guardLoc.getWorld().equals(targetLoc.getWorld())) {
                            endChase(chaseId, "Players in different worlds");
                            continue;
                        }
                        
                        double distance;
                        try {
                            distance = guardLoc.distance(targetLoc);
                        } catch (IllegalArgumentException e) {
                            logger.warning("Distance calculation failed for chase " + chaseId + ": " + e.getMessage());
                            endChase(chaseId, "Distance calculation error");
                continue;
            }
            
                        
                        int maxDistance = plugin.getConfigManager().getMaxChaseDistance();
                        if (distance > maxDistance) {
                            endChase(chaseId, "Target too far (" + Math.round(distance) + " > " + maxDistance + ")");
                continue;
            }
            
            
            Player guardPlayer = plugin.getServer().getPlayer(guardId);
            Player targetPlayer = plugin.getServer().getPlayer(targetId);
            
            if (guardPlayer == null || targetPlayer == null) {
                endChase(chaseId, "Player offline");
                continue;
            }
            
            
            if (isPlayerInRestrictedArea(targetPlayer)) {
                endChase(chaseId, "Target entered restricted area");
                continue;
            }
            
            
            try {
                plugin.getBossBarManager().updateChaseBossBar(guardPlayer, distance, targetPlayer);
                plugin.getBossBarManager().updateChaseBossBar(targetPlayer, distance, guardPlayer);
            } catch (Exception e) {
                logger.warning("Boss bar update failed for chase " + chaseId + ": " + e.getMessage());
                
            }
            
            
                        int warningDistance = plugin.getConfigManager().getChaseWarningDistance();
                        if (distance > warningDistance) {
                            try {
                plugin.getMessageManager().sendMessage(guardPlayer, "chase.warnings.distance",
                                    MessageManager.numberPlaceholder("distance", Math.round(distance)));
                            } catch (Exception e) {
                                logger.warning("Warning message failed for chase " + chaseId + ": " + e.getMessage());
                            }
                        }
                        
                    } catch (Exception e) {
                        logger.severe("Location processing failed for chase " + chaseId + ": " + e.getMessage());
                        endChase(chaseId, "Location processing error");
                        continue;
                    }
                    
                } catch (Exception e) {
                    logger.severe("Chase monitoring failed for chase data: " + e.getMessage());
                    e.printStackTrace();
                    
                    
                    if (chase != null && chase.getChaseId() != null) {
                        try {
                            endChase(chase.getChaseId(), "Monitor error - emergency cleanup");
                        } catch (Exception cleanupError) {
                            logger.severe("Emergency cleanup failed: " + cleanupError.getMessage());
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.severe("Critical error in chase monitoring: " + e.getMessage());
            e.printStackTrace();
        }
    }

    
    
    public void handleCombatEvent(Player player) {
        if (!plugin.getConfigManager().shouldPreventCaptureInCombat()) {
            return; 
        }
        
        UUID playerId = player.getUniqueId();
        int duration = plugin.getConfigManager().getCombatTimerDuration();
        
        
        combatTimers.put(playerId, System.currentTimeMillis() + (duration * 1000L));
        
        
        BukkitTask existingTask = combatTasks.remove(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        
        plugin.getBossBarManager().showCombatBossBar(player, duration);
        
        
        plugin.getMessageManager().sendActionBar(player, "actionbar.combat-active");
        
        
        plugin.getMessageManager().sendMessage(player, "combat.timer-started");
        
        
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                endCombatTimer(player);
            }
        }.runTaskLater(plugin, duration * 20L);
        
        combatTasks.put(playerId, task);
        
        if (plugin.getConfigManager().isDebugMode()) {
            logger.info("DEBUG: Combat timer started for " + player.getName() + " (" + duration + "s)");
        }
    }
    
    public void endCombatTimer(Player player) {
        UUID playerId = player.getUniqueId();
        
        
        combatTimers.remove(playerId);
        
        
        BukkitTask task = combatTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        
        
        plugin.getBossBarManager().hideBossBarByType(player, "combat");
        
        
        plugin.getMessageManager().sendMessage(player, "combat.timer-ended");
        
        if (plugin.getConfigManager().isDebugMode()) {
            logger.info("DEBUG: Combat timer ended for " + player.getName());
        }
    }
    
    public boolean isInCombat(Player player) {
        Long combatEnd = combatTimers.get(player.getUniqueId());
        if (combatEnd == null) return false;
        
        if (System.currentTimeMillis() >= combatEnd) {
            
            endCombatTimer(player);
            return false;
        }
        
        return true;
    }
    
    public long getRemainingCombatTime(Player player) {
        Long combatEnd = combatTimers.get(player.getUniqueId());
        if (combatEnd == null) return 0;
        
        long remaining = combatEnd - System.currentTimeMillis();
        return Math.max(0, remaining / 1000L);
        }

    
    
    public boolean startChase(Player guard, Player target) {
        
        if (guard == null || target == null) {
            logger.warning("Cannot start chase: null player provided (guard=" + guard + ", target=" + target + ")");
            return false;
        }
        
        
        if (!canStartChaseWithMessages(guard, target)) {
            return false;
        }
        
        if (!plugin.getSecurityManager().canPlayerBeChased(target)) {
            plugin.getMessageManager().sendMessage(guard, "security.guard-immunity.chase-protected");
            plugin.getSecurityManager().logSecurityViolation("start chase", guard, target);
            return false;
        }
        
        UUID guardId = guard.getUniqueId();
        UUID targetId = target.getUniqueId();
        
        if (guardId == null || targetId == null) {
            logger.warning("Cannot start chase: null UUID (guard=" + guardId + ", target=" + targetId + ")");
            return false;
        }
        
        try {
        
        try {
            if (plugin.getWorldGuardUtils().isPlayerInSafeZone(target)) {
                plugin.getMessageManager().sendMessage(guard, "chase.restrictions.in-safe-zone");
                return false;
            }
        } catch (Exception e) {
            logger.warning("Safe zone check failed for chase start: " + e.getMessage());
            plugin.getMessageManager().sendMessage(guard, "chase.errors.region-check-failed");
            return false;
        }
        
        
        try {
            Location guardLoc = guard.getLocation();
            Location targetLoc = target.getLocation();
                
                if (guardLoc == null || targetLoc == null) {
                    logger.warning("Cannot start chase: null location (guard=" + guardLoc + ", target=" + targetLoc + ")");
                    plugin.getMessageManager().sendMessage(guard, "chase.errors.invalid-location");
                    return false;
                }
                
                if (!guardLoc.getWorld().equals(targetLoc.getWorld())) {
                    plugin.getMessageManager().sendMessage(guard, "chase.restrictions.different-worlds");
                    return false;
                }
                
                
                double initialDistance = guardLoc.distance(targetLoc);
                if (initialDistance > plugin.getConfigManager().getMaxChaseDistance()) {
                    
                    plugin.getMessageManager().sendMessage(guard, "chase.restrictions.too-far-to-start");
                    return false;
                }
                
            } catch (Exception e) {
                logger.warning("Location validation failed for chase start: " + e.getMessage());
                plugin.getMessageManager().sendMessage(guard, "chase.errors.location-validation-failed");
            return false;
        }
        
        
        UUID chaseId = UUID.randomUUID();
            long duration = plugin.getConfigManager().getChaseDuration() * 1000L;
            ChaseData chase = new ChaseData(chaseId, guardId, targetId, duration);
            
            
            PlayerData guardData = null;
            PlayerData targetData = null;
            boolean dataUpdated = false;
            
            try {
                guardData = plugin.getDataManager().getOrCreatePlayerData(guardId, guard.getName());
                targetData = plugin.getDataManager().getOrCreatePlayerData(targetId, target.getName());
                
                if (guardData == null || targetData == null) {
                    logger.warning("Failed to create/load player data for chase");
                    plugin.getMessageManager().sendMessage(guard, "chase.errors.data-load-failed");
                    return false;
                }
                
                
                boolean originalTargetChased = targetData.isBeingChased();
                UUID originalChaser = targetData.getChaserGuard();
                long originalChaseStart = targetData.getChaseStartTime();
                
                
        targetData.setBeingChased(true);
                targetData.setChaserGuard(guardId);
        targetData.setChaseStartTime(System.currentTimeMillis());
        
                
        plugin.getDataManager().savePlayerData(guardData);
        plugin.getDataManager().savePlayerData(targetData);
        
                
                plugin.getDataManager().addChaseData(chase);
                dataUpdated = true;
                
                
                try {
                    double distance = guard.getLocation().distance(target.getLocation());
                    plugin.getBossBarManager().showChaseGuardBossBar(guard, target, distance);
                    plugin.getBossBarManager().showChaseTargetBossBar(target, guard, distance);
                } catch (Exception e) {
                    logger.warning("Boss bar creation failed for chase " + chaseId + ": " + e.getMessage());
                    
                }
        
                
                try {
        plugin.getMessageManager().sendMessage(guard, "chase.start.success", 
            playerPlaceholder("target", target));
        plugin.getMessageManager().sendMessage(target, "chase.start.target-notification", 
            playerPlaceholder("guard", guard));
        
        
        plugin.getMessageManager().sendGuardAlert("chase.start.guard-alert",
            playerPlaceholder("guard", guard),
            playerPlaceholder("target", target));
                } catch (Exception e) {
                    logger.warning("Message sending failed for chase " + chaseId + ": " + e.getMessage());
                    
                }
        
                logger.info("Chase started successfully: " + guard.getName() + " -> " + target.getName() + " (ID: " + chaseId + ")");
        return true;
                
            } catch (Exception e) {
                logger.severe("Failed to create chase data: " + e.getMessage());
                e.printStackTrace();
                
                
                if (dataUpdated) {
                    try {
                        logger.info("Attempting rollback for failed chase " + chaseId);
                        
                        
                        plugin.getDataManager().removeChaseData(chaseId);
                        
                        
                        if (targetData != null) {
                            targetData.setBeingChased(false);
                            targetData.setChaserGuard(null);
                            targetData.setChaseStartTime(0);
                            plugin.getDataManager().savePlayerData(targetData);
                        }
                        
                        
                        plugin.getBossBarManager().hideBossBarByType(guard, "chase_guard");
                        plugin.getBossBarManager().hideBossBarByType(target, "chase_target");
                        
                        logger.info("Rollback completed for failed chase " + chaseId);
                    } catch (Exception rollbackError) {
                        logger.severe("Rollback failed for chase " + chaseId + ": " + rollbackError.getMessage());
                    }
                }
                
                plugin.getMessageManager().sendMessage(guard, "chase.errors.creation-failed");
                return false;
            }
            
        } catch (Exception e) {
            logger.severe("Critical error in startChase: " + e.getMessage());
            e.printStackTrace();
            
            try {
                plugin.getMessageManager().sendMessage(guard, "chase.errors.critical-error");
            } catch (Exception msgError) {
                logger.severe("Failed to send error message: " + msgError.getMessage());
            }
            
            return false;
        }
    }
    
    public boolean endChase(UUID chaseId, String reason) {
        
        if (chaseId == null) {
            logger.warning("Cannot end chase: null chaseId provided");
            return false;
        }
        
        if (reason == null || reason.trim().isEmpty()) {
            reason = "Unknown reason";
        }
        
        try {
            ChaseData chase = plugin.getDataManager().getChaseData(chaseId);
            if (chase == null) {
                logger.fine("Chase " + chaseId + " not found - may have already been ended");
                return false;
            }
            
            UUID guardId = chase.getGuardId();
            UUID targetId = chase.getTargetId();
            Player guard = null;
            Player target = null;
            
            
            try {
                if (guardId != null) {
                    guard = plugin.getServer().getPlayer(guardId);
                }
                if (targetId != null) {
                    target = plugin.getServer().getPlayer(targetId);
                }
            } catch (Exception e) {
                logger.warning("Error retrieving players for chase " + chaseId + ": " + e.getMessage());
            }
            
            
            boolean guardDataCleared = false;
            boolean targetDataCleared = false;
            
            if (guard != null && guardId != null) {
                try {
                    PlayerData guardData = plugin.getDataManager().getPlayerData(guardId);
                    if (guardData != null) {
                        
                        plugin.getDataManager().savePlayerData(guardData);
                        guardDataCleared = true;
                    }
                } catch (Exception e) {
                    logger.warning("Error clearing guard data for chase " + chaseId + ": " + e.getMessage());
                }
            }
            
            
            try {
                PlayerData targetData = plugin.getDataManager().getPlayerData(targetId);
                if (targetData != null) {
                    targetData.clearChaseData();
                    plugin.getDataManager().savePlayerData(targetData);
                    targetDataCleared = true;
                }
            } catch (Exception e) {
                logger.warning("Error clearing target data for chase " + chaseId + ": " + e.getMessage());
            }
            
            
            boolean chaseDataRemoved = false;
            try {
                plugin.getDataManager().removeChaseData(chaseId);
                chaseDataRemoved = true;
            } catch (Exception e) {
                logger.severe("Error removing chase data for " + chaseId + ": " + e.getMessage());
            }
            
            
        if (guard != null) {
                try {
            plugin.getBossBarManager().hideBossBarByType(guard, "chase_guard");
                } catch (Exception e) {
                    logger.warning("Error hiding guard boss bar for chase " + chaseId + ": " + e.getMessage());
                }
        }
            
        if (target != null) {
                try {
            plugin.getBossBarManager().hideBossBarByType(target, "chase_target");
                } catch (Exception e) {
                    logger.warning("Error hiding target boss bar for chase " + chaseId + ": " + e.getMessage());
                }
        }
        
            
        if (guard != null) {
                try {
            plugin.getMessageManager().sendMessage(guard, "chase.end.success",
                stringPlaceholder("reason", reason));
                } catch (Exception e) {
                    logger.warning("Error sending end message to guard for chase " + chaseId + ": " + e.getMessage());
                }
        }
        
        if (target != null) {
                try {
            plugin.getMessageManager().sendMessage(target, "chase.end.target-notification",
                stringPlaceholder("reason", reason));
                } catch (Exception e) {
                    logger.warning("Error sending end message to target for chase " + chaseId + ": " + e.getMessage());
                }
            }
            
            
            try {
                if (guard != null) {
                    endCombatTimer(guard);
                    try { guard.removePotionEffect(PotionEffectType.SLOWNESS); } catch (Exception ignored) {}
                }
                if (target != null) {
                    endCombatTimer(target);
                    try { target.removePotionEffect(PotionEffectType.SLOWNESS); } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                logger.warning("Error clearing combat timers for chase " + chaseId + ": " + e.getMessage());
            }
            
            
            String guardName = guard != null ? guard.getName() : (guardId != null ? guardId.toString() : "unknown");
            String targetName = target != null ? target.getName() : (targetId != null ? targetId.toString() : "unknown");
            
            logger.info("Chase ended: " + guardName + " -> " + targetName + " (" + reason + ")" +
                       " [Data removed: " + chaseDataRemoved + ", Guard cleared: " + guardDataCleared + 
                       ", Target cleared: " + targetDataCleared + "]");
            
            return chaseDataRemoved; 
            
        } catch (Exception e) {
            logger.severe("Critical error ending chase " + chaseId + ": " + e.getMessage());
            e.printStackTrace();
            
            
            try {
                plugin.getDataManager().removeChaseData(chaseId);
                logger.info("Emergency cleanup completed for chase " + chaseId);
            } catch (Exception emergencyError) {
                logger.severe("Emergency cleanup failed for chase " + chaseId + ": " + emergencyError.getMessage());
            }
            
            return false;
        }
    }
    
    
    public boolean canStartChase(Player guard, Player target) {
        
        if (!plugin.getDutyManager().isOnDuty(guard)) {
            return false;
        }
        
        
        
        
        if (plugin.getConfigManager().shouldPreventChaseDuringCombat() && isInCombat(target)) {
            return false;
        }
        
        
        if (isPlayerInRestrictedArea(target)) {
            return false;
        }
        
        
        int activeChases = plugin.getDataManager().getAllActiveChases().size();
        if (activeChases >= plugin.getConfigManager().getMaxConcurrentChases()) {
            return false;
        }
        
        
        if (plugin.getDataManager().isGuardChasing(guard.getUniqueId())) {
            return false;
        }
        
        
        if (plugin.getDataManager().isPlayerBeingChased(target.getUniqueId())) {
            return false;
        }
        
        
        if (guard.equals(target)) {
            return false;
        }
        
        
        double distance = guard.getLocation().distance(target.getLocation());
        if (distance > plugin.getConfigManager().getMaxChaseDistance()) {
            return false;
        }
        
        return true;
    }
    private boolean canStartChaseWithMessages(Player guard, Player target) {
        
        if (!plugin.getDutyManager().isOnDuty(guard)) {
            plugin.getMessageManager().sendMessage(guard, "chase.restrictions.not-on-duty");
            return false;
        }
        
        
        
        
        if (plugin.getConfigManager().shouldPreventChaseDuringCombat() && isInCombat(target)) {
            plugin.getMessageManager().sendMessage(guard, "chase.restrictions.target-in-combat");
            return false;
        }
        
        
        if (isPlayerInRestrictedArea(target)) {
            plugin.getMessageManager().sendMessage(guard, "chase.restrictions.area-restricted");
            return false;
        }
        
        
        int activeChases = plugin.getDataManager().getAllActiveChases().size();
        if (activeChases >= plugin.getConfigManager().getMaxConcurrentChases()) {
            plugin.getMessageManager().sendMessage(guard, "chase.restrictions.max-concurrent");
            return false;
        }
        
        
        if (plugin.getDataManager().isGuardChasing(guard.getUniqueId())) {
            plugin.getMessageManager().sendMessage(guard, "chase.restrictions.already-chasing");
            return false;
        }
        
        
        if (plugin.getDataManager().isPlayerBeingChased(target.getUniqueId())) {
            plugin.getMessageManager().sendMessage(guard, "chase.restrictions.target-being-chased");
            return false;
        }
        
        
        if (guard.equals(target)) {
            plugin.getMessageManager().sendMessage(guard, "chase.restrictions.cannot-chase-self");
            return false;
        }
        
        
        double distance = guard.getLocation().distance(target.getLocation());
        if (distance > plugin.getConfigManager().getMaxChaseDistance()) {
            plugin.getMessageManager().sendMessage(guard, "chase.restrictions.too-far");
            return false;
        }
        
        return true;
    }
    
    public boolean captureTarget(Player guard, Player target) {
        ChaseData chase = plugin.getDataManager().getChaseByGuard(guard.getUniqueId());
        if (chase == null) {
            return false;
        }
        
        
        endChase(chase.getChaseId(), plugin.getMessageManager().getPlainTextMessage("chase.end-reasons.target-captured"));
        
        
        return plugin.getJailManager().startJailCountdown(guard, target, "Captured during chase");
    }
    

    
    public boolean isPlayerInChase(Player player) {
        return plugin.getDataManager().isPlayerBeingChased(player.getUniqueId()) || 
               plugin.getDataManager().isGuardChasing(player.getUniqueId());
    }
    
    
    public boolean isPlayerInRestrictedArea(Player player) {
        if (!plugin.getConfigManager().shouldBlockRestrictedAreas()) {
            return false;
        }
        
        String[] restrictedAreas = plugin.getConfigManager().getChaseRestrictedAreas();
        return plugin.getWorldGuardUtils().isPlayerInAnyRegion(player, restrictedAreas);
    }
    
    public ChaseData getChaseByPlayer(Player player) {
        ChaseData chase = plugin.getDataManager().getChaseByGuard(player.getUniqueId());
        if (chase != null) return chase;
        
        return plugin.getDataManager().getChaseByTarget(player.getUniqueId());
    }
    
    
    
    private void cleanupInvalidChases() {
        List<ChaseData> invalidChases = new ArrayList<>();
        
        for (ChaseData chase : plugin.getDataManager().getAllActiveChases()) {
            if (chase.getGuardId() == null || chase.getTargetId() == null) {
                invalidChases.add(chase);
            }
        }
        
        for (ChaseData chase : invalidChases) {
            logger.warning("Cleaning up invalid chase with null UUIDs: " + chase.getChaseId());
            plugin.getDataManager().removeChaseData(chase.getChaseId());
        }
        
        if (!invalidChases.isEmpty()) {
            logger.info("Cleaned up " + invalidChases.size() + " invalid chases with null UUIDs");
        }
    }
    
    private void cleanupAllCombatTimers() {
        try {
            for (Map.Entry<UUID, BukkitTask> entry : combatTasks.entrySet()) {
                try {
                    BukkitTask task = entry.getValue();
                    if (task != null) {
                        task.cancel();
                    }
                    
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null) {
                        plugin.getBossBarManager().hideBossBarByType(player, "combat");
                    }
                } catch (Exception e) {
                    LoggingUtils.warn(logger, "Error cleaning up combat timer for " + entry.getKey() + ": " + e.getMessage());
                }
            }
            
            combatTimers.clear();
            combatTasks.clear();
        } catch (Exception e) {
            LoggingUtils.warn(logger, "Error during combat timer cleanup: " + e.getMessage());
        }
    }
    
    public void cleanupPlayer(Player player) {
        
        endCombatTimer(player);
    }
} 
