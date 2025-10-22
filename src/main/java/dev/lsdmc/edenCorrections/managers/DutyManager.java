package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import dev.lsdmc.edenCorrections.utils.InventorySerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.logging.Logger;

import static dev.lsdmc.edenCorrections.managers.MessageManager.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.lsdmc.edenCorrections.utils.LoggingUtils;

public class DutyManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    
    private LuckPerms luckPerms;
    
    
    private final Map<UUID, BukkitTask> dutyTransitions;
    private final Map<UUID, Location> transitionLocations;
    
    
    private final Map<UUID, String> inventoryCache; 
    private final Map<UUID, String> onDutyInventoryCache; 
    private final List<Material> guardKitItems;
    
    
    private final Map<UUID, List<String>> playerGuardKitItems; 

    
    private final Map<UUID, Integer> lastTimeBasedBonusHourAwarded = new HashMap<>();
    private final Map<UUID, Long> lastBonusNotificationAt = new HashMap<>();
    private final java.util.Set<UUID> timeBonusNotifiedOnce = new java.util.HashSet<>();
    
    public DutyManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.dutyTransitions = new HashMap<>();
        this.transitionLocations = new HashMap<>();
        this.inventoryCache = new HashMap<>();
        this.onDutyInventoryCache = new HashMap<>();
        this.guardKitItems = InventorySerializer.getCommonGuardKitItems();
        this.playerGuardKitItems = new HashMap<>();
    }
    
    public void initialize() {
        logger.info("DutyManager initializing...");
        
        
        initializeLuckPerms();
        
        
        startDutyMonitoring();
        
        logger.info("DutyManager initialized successfully!");
    }
    
    private void initializeLuckPerms() {
        RegisteredServiceProvider<LuckPerms> lpProvider = 
            Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (lpProvider != null) {
            luckPerms = lpProvider.getProvider();
            logger.info("LuckPerms integration enabled - rank detection available");
        } else {
            logger.warning("LuckPerms not found - using permission-based rank detection");
        }
    }
    

    
    private void startDutyMonitoring() {
        new BukkitRunnable() {
            @Override
            public void run() {
                
                checkDutyStatus();
            }
        }.runTaskTimer(plugin, 20L * 30L, 20L * 30L); 
    }
    
    private void checkDutyStatus() {
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (isSubjectToGuardRestrictions(player)) {
                PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
                if (data != null) {
                    
                    if (data.isOnDuty() || hasValidGuardRank(player)) {
                        checkOffDutyTimeEarning(player, data);
                        checkOffDutyTimeConsumption(player, data);
                    }
                }
            }
        }
    }
    
    
    private void checkOffDutyTimeEarning(Player player, PlayerData data) {
        if (!data.isOnDuty()) return;
        
        long dutyTime = System.currentTimeMillis() - data.getDutyStartTime();
        int dutyMinutes = (int) (dutyTime / (1000L * 60L));
        int dutyHours = dutyMinutes / 60; 
        
        
        if (!data.hasEarnedBaseTime() && dutyMinutes >= plugin.getConfigManager().getBaseDutyRequirement()) {
            awardBaseOffDutyTime(player, data);
        }
        
        
        checkPerformanceBonuses(player, data);
        
        
        if (dutyHours > 0) {
            Integer lastHour = lastTimeBasedBonusHourAwarded.get(player.getUniqueId());
            if (lastHour == null || dutyHours > lastHour) {
                awardTimeBasedBonus(player, data);
                lastTimeBasedBonusHourAwarded.put(player.getUniqueId(), dutyHours);
            }
        }
    }
    
    private void checkOffDutyTimeConsumption(Player player, PlayerData data) {
        if (data.isOnDuty()) {
            
            if (data.isPenaltyTrackingActive()) {
                clearOffDutyPenalties(player, data);
            }
            data.resetConsumedOffDutyTime(); 
            return;
        }
        
        
        data.addConsumedOffDutyTime(1000L); 
        
        long earnedTime = data.getEarnedOffDutyTime();
        long consumedTime = data.getConsumedOffDutyTime();
        
        if (consumedTime > earnedTime) {
            
            
            
            
            long maxReasonableOverrun = 30 * 24 * 60 * 60 * 1000L; 
            long totalOverrun = consumedTime - earnedTime;
            if (totalOverrun > maxReasonableOverrun) {
                logger.warning("Player " + player.getName() + " has overrun their off-duty time by " + 
                             (totalOverrun / (24 * 60 * 60 * 1000L)) + " days - giving fresh start");
                data.resetConsumedOffDutyTime(); 
                data.setEarnedOffDutyTime(earnedTime); 
                data.clearPenaltyTracking();
                data.setHasBeenNotifiedOfExpiredTime(false);
                plugin.getDataManager().savePlayerData(data);
                return;
            }
            
            if (!data.hasBeenNotifiedOfExpiredTime()) {
                
                plugin.getMessageManager().sendMessage(player, "duty.restrictions.off-duty-time-expired");
                plugin.getMessageManager().sendMessage(player, "duty.restrictions.must-return-to-duty");
                data.setHasBeenNotifiedOfExpiredTime(true);
                
                
                if (!data.isPenaltyTrackingActive()) {
                    data.initializePenaltyTracking();
                    
                    data.setPenaltyStartTime(System.currentTimeMillis());
                    plugin.getDataManager().savePlayerData(data);
                    logger.info(player.getName() + " has used up their earned off-duty time - penalty tracking initiated");
                }
            }
            
            
            if (plugin.getConfigManager().isPenaltyEscalationEnabled()) {
                
                
                long penaltyStartTime = data.getPenaltyStartTime();
                long currentTime = System.currentTimeMillis();
                long overrunTime = currentTime - penaltyStartTime;
                
                
                
                if (penaltyStartTime == 0 || overrunTime < 0) {
                    
                    penaltyStartTime = data.getOffDutyTime() + earnedTime;
                    overrunTime = currentTime - penaltyStartTime;
                    data.setPenaltyStartTime(penaltyStartTime);
                    
                    if (plugin.getConfigManager().isDebugMode()) {
                        logger.info("Recalculated penalty start time for " + player.getName() + 
                                   " to " + penaltyStartTime);
                    }
                }
                
                
                long maxWeeklyOverrun = 7 * 24 * 60 * 60 * 1000L; 
                if (overrunTime > maxWeeklyOverrun) {
                    logger.warning("Capping overrun time for " + player.getName() + " from " + 
                                 (overrunTime / (60 * 1000L)) + " minutes to " + 
                                 (maxWeeklyOverrun / (60 * 1000L)) + " minutes (7 days)");
                    overrunTime = maxWeeklyOverrun;
                }
                
                
                if (overrunTime > 24 * 60 * 60 * 1000L) { 
                    logger.warning("Extreme overrun time detected for " + player.getName() + 
                                 ": " + (overrunTime / (60 * 1000L)) + " minutes (" + 
                                 (overrunTime / (24 * 60 * 60 * 1000L)) + " days)");
                    logger.warning("  penaltyStartTime: " + penaltyStartTime);
                    logger.warning("  currentTime: " + currentTime);
                    logger.warning("  consumedTime: " + (consumedTime / (60 * 1000L)) + " minutes");
                    logger.warning("  earnedTime: " + (earnedTime / (60 * 1000L)) + " minutes");
                }
                
                applyEscalatingPenalties(player, data, overrunTime);
            }
        }
    }
    
    
    private void clearOffDutyPenalties(Player player, PlayerData data) {
        
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        
        
        if (data.hasActivePenaltyBossBar()) {
            plugin.getBossBarManager().hideBossBarByType(player, "penalty");
            data.setHasActivePenaltyBossBar(false);
        }
        
        
        data.clearPenaltyTracking();
        data.setHasBeenNotifiedOfExpiredTime(false);
        
        
        plugin.getDataManager().savePlayerData(data);
        
        
        plugin.getMessageManager().sendSuccess(player, "duty.penalties.cleared");
        
        logger.info("Cleared off-duty penalties for " + player.getName());
    }
    
    
    private void applyEscalatingPenalties(Player player, PlayerData data, long overrunTime) {
        long overrunMinutes = overrunTime / (60 * 1000L);
        
        
        int gracePeriod = plugin.getConfigManager().getPenaltyGracePeriod();
        int stage1Time = plugin.getConfigManager().getPenaltyStage1Time();
        int stage2Time = plugin.getConfigManager().getPenaltyStage2Time();
        int recurringInterval = plugin.getConfigManager().getPenaltyRecurringInterval();
        
        
        if (overrunMinutes < gracePeriod) {
            return;
        }
        
        long effectiveOverrunMinutes = overrunMinutes - gracePeriod;
        
        
        int currentStage = data.getCurrentPenaltyStage();
        int nextStage = currentStage + 1;
        
        
        long timeSinceLastPenalty = System.currentTimeMillis() - data.getLastPenaltyTime();
        long minimumIntervalMs = Math.max(recurringInterval * 60 * 1000L, 60000L); 
        
        
        boolean shouldApplyNextStage = false;
        
        if (currentStage == 0) {
            
            shouldApplyNextStage = effectiveOverrunMinutes >= stage1Time;
        } else if (currentStage == 1) {
            
            shouldApplyNextStage = (effectiveOverrunMinutes >= stage2Time) && 
                                   (timeSinceLastPenalty >= minimumIntervalMs);
        } else if (currentStage >= 2) {
            
            long timeForNextRecurring = stage2Time + ((currentStage - 1) * recurringInterval);
            shouldApplyNextStage = (effectiveOverrunMinutes >= timeForNextRecurring) && 
                                   (timeSinceLastPenalty >= minimumIntervalMs);
        }
        
        
        if (plugin.getConfigManager().isDebugMode() && shouldApplyNextStage) {
            logger.info("Penalty stage change for " + player.getName() + ":");
            logger.info("  Effective overrun: " + effectiveOverrunMinutes + " minutes");
            logger.info("  Advancing from stage " + currentStage + " to stage " + nextStage);
        }
        
        if (shouldApplyNextStage) {
            
            data.setCurrentPenaltyStage(nextStage);
            
            
            if (nextStage == 1) {
                applyStage1Penalty(player, data);
            } else if (nextStage == 2) {
                applyStage2Penalty(player, data);
            } else if (nextStage >= 3) {
                applyRecurringPenalty(player, data);
            }
            
            data.setLastPenaltyTime(System.currentTimeMillis());
            plugin.getDataManager().savePlayerData(data);
            
            
            updatePenaltyBossBar(player, data, overrunMinutes);
            data.setHasActivePenaltyBossBar(true);
            
            logger.info("Applied penalty stage " + nextStage + " to " + player.getName() + 
                       " (effective overrun: " + effectiveOverrunMinutes + " minutes)");
        }
    }
    
    
    private void applyStage1Penalty(Player player, PlayerData data) {
        
        int slownessLevel = plugin.getConfigManager().getPenaltyStage1SlownessLevel();
        PotionEffect slowness = new PotionEffect(
            PotionEffectType.SLOWNESS, 
            Integer.MAX_VALUE, 
            slownessLevel - 1 
        );
        player.addPotionEffect(slowness);
        data.setLastSlownessApplication(System.currentTimeMillis());
        
        
        if (plugin.getConfigManager().isPenaltyStage1WarningEnabled()) {
            plugin.getMessageManager().sendWarning(player, "duty.penalties.stage1-applied", 
                MessageManager.numberPlaceholder("slowness_level", slownessLevel)
            );
        }
        
        logger.info("Applied Stage 1 off-duty penalty to " + player.getName() + 
                   " (Slowness " + slownessLevel + ")");
    }
    
    
    private void applyStage2Penalty(Player player, PlayerData data) {
        
        int slownessLevel = plugin.getConfigManager().getPenaltyStage2SlownessLevel();
        PotionEffect slowness = new PotionEffect(
            PotionEffectType.SLOWNESS, 
            Integer.MAX_VALUE,
            slownessLevel - 1
        );
        player.addPotionEffect(slowness);
        data.setLastSlownessApplication(System.currentTimeMillis());
        
        
        if (plugin.getConfigManager().isPenaltyStage2WarningEnabled()) {
            plugin.getMessageManager().sendWarning(player, "duty.penalties.stage2-applied",
                MessageManager.numberPlaceholder("slowness_level", slownessLevel)
            );
        }
        
        logger.info("Applied Stage 2 off-duty penalty to " + player.getName() + 
                   " (Slowness " + slownessLevel + ")");
    }
    
    
    private void applyRecurringPenalty(Player player, PlayerData data) {
        
        int slownessLevel = plugin.getConfigManager().getPenaltyRecurringSlownessLevel();
        PotionEffect slowness = new PotionEffect(
            PotionEffectType.SLOWNESS, 
            Integer.MAX_VALUE,
            slownessLevel - 1
        );
        player.addPotionEffect(slowness);
        data.setLastSlownessApplication(System.currentTimeMillis());
        
        
        int economyPenalty = plugin.getConfigManager().getPenaltyRecurringEconomyPenalty();
        applyEconomyPenalty(player, economyPenalty, "recurring off-duty violation");
        
        
        if (plugin.getConfigManager().isPenaltyRecurringWarningEnabled()) {
            plugin.getMessageManager().sendWarning(player, "duty.penalties.recurring-applied",
                MessageManager.stringPlaceholder("time", "$" + economyPenalty),
                MessageManager.numberPlaceholder("stage", data.getCurrentPenaltyStage())
            );
        }
        
        logger.info("Applied recurring off-duty penalty to " + player.getName() + 
                   " (Stage " + data.getCurrentPenaltyStage() + ", $" + economyPenalty + " deducted)");
    }
    
    
    private void applyEconomyPenalty(Player player, int amount, String reason) {
        
        plugin.getVaultEconomyManager().takeMoney(player, amount, reason)
            .thenAccept(success -> {
                if (success) {
                    logger.info("Successfully deducted $" + amount + " from " + 
                               player.getName() + " (Reason: " + reason + ")");
                } else {
                    logger.warning("Failed to deduct money from " + player.getName() + 
                                 " - Vault economy operation failed or insufficient funds");
                }
            })
            .exceptionally(throwable -> {
                logger.severe("Error executing economy penalty for " + player.getName() + ": " + throwable.getMessage());
                return null;
            });
    }
    
    
    private void updatePenaltyBossBar(Player player, PlayerData data, long overrunMinutes) {
        if (!plugin.getConfigManager().isPenaltyBossBarEnabled()) {
            return;
        }
        
        
        plugin.getBossBarManager().showPenaltyBossBar(player, data.getCurrentPenaltyStage(), overrunMinutes);
        data.setHasActivePenaltyBossBar(true);
    }

    

    
    public void adminClearPenalties(Player player) {
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
        clearOffDutyPenalties(player, data);
    }

    
    public boolean adminSetPenaltyStage(Player player, int stage) {
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());

        if (stage <= 0) {
            adminClearPenalties(player);
            return true;
        }

        
        if (!data.isPenaltyTrackingActive()) {
            data.initializePenaltyTracking();
            data.setPenaltyStartTime(System.currentTimeMillis());
        }

        
        data.setCurrentPenaltyStage(stage - 1); 
        data.setLastPenaltyTime(0); 

        
        int grace = plugin.getConfigManager().getPenaltyGracePeriod();
        int stage1 = plugin.getConfigManager().getPenaltyStage1Time();
        int stage2 = plugin.getConfigManager().getPenaltyStage2Time();
        int recur = plugin.getConfigManager().getPenaltyRecurringInterval();

        long overrunMinutes;
        if (stage == 1) {
            overrunMinutes = grace + stage1;
        } else if (stage == 2) {
            overrunMinutes = grace + stage2;
        } else {
            overrunMinutes = grace + stage2 + (long) (stage - 2) * recur;
        }

        applyEscalatingPenalties(player, data, overrunMinutes * 60_000L);
        updatePenaltyBossBar(player, data, overrunMinutes);
        plugin.getDataManager().savePlayerData(data);
        return true;
    }

    
    public boolean adminSimulateOverrun(Player player, int minutes) {
        if (minutes < 0) minutes = 0;
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
        if (!data.isPenaltyTrackingActive()) {
            data.initializePenaltyTracking();
        }
        data.setPenaltyStartTime(System.currentTimeMillis() - minutes * 60_000L);
        data.setLastPenaltyTime(0);
        applyEscalatingPenalties(player, data, minutes * 60_000L);
        updatePenaltyBossBar(player, data, minutes);
        plugin.getDataManager().savePlayerData(data);
        return true;
    }

    
    public int adminGetPenaltyStage(Player player) {
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
        return data.getCurrentPenaltyStage();
    }

    
    public long adminGetOverrunSeconds(Player player) {
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
        if (!data.isPenaltyTrackingActive() || data.getPenaltyStartTime() <= 0) return 0L;
        return Math.max(0L, (System.currentTimeMillis() - data.getPenaltyStartTime()) / 1000L);
    }

    
    
    public boolean toggleDuty(Player player) {
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
        
        if (data.isOnDuty()) {
            return goOffDuty(player, data);
        } else {
            return initiateGuardDuty(player); 
        }
    }
    
    public boolean initiateGuardDuty(Player player) {
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
        
        if (plugin.getConfigManager().isDebugMode()) {
            LoggingUtils.debug(logger, true, player.getName() + " attempting to go on duty");
        }
        if (data.isOnDuty()) {
            plugin.getMessageManager().sendMessage(player, "duty.activation.already-on");
            return false;
        }
        
        


        
        String guardRank = getPlayerGuardRank(player);
        if (guardRank == null) {
            plugin.getMessageManager().sendMessage(player, "duty.restrictions.no-rank");
            
            if (plugin.getConfigManager().isDebugMode()) {
                LoggingUtils.debug(logger, true, player.getName() + " denied duty - no valid guard rank");
                
                debugPlayerGroups(player);
            }
            return false;
        }
        
        if (plugin.getConfigManager().isDebugMode()) {
            LoggingUtils.debug(logger, true, player.getName() + " has valid guard rank: " + guardRank);
        }
        
        
        if (!canGoOnDuty(data, player)) {
            long remainingTime = getRemainingOffDutyTime(data);
            plugin.getMessageManager().sendMessage(player, "duty.restrictions.insufficient-time",
                timePlaceholder("time", remainingTime));
            return false;
        }
        
        
        if (!isInDutyRegion(player)) {
            plugin.getMessageManager().sendMessage(player, "duty.restrictions.wrong-region");
            return false;
        }
        
        
        
        
        if (plugin.getWantedManager().isWanted(player)) {
            plugin.getMessageManager().sendMessage(player, "duty.restrictions.wanted-active");
            return false;
        }
        
        
        data.setGuardRank(guardRank);
        plugin.getDataManager().savePlayerData(data);
        
        plugin.getMessageManager().sendMessage(player, "duty.activation.rank-detected", 
            stringPlaceholder("rank", guardRank));
        
        
        return startDutyTransition(player, guardRank);
    }

    
    
    private void debugPlayerGroups(Player player) {
        if (luckPerms != null) {
            try {
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    logger.info("DEBUG: " + player.getName() + " has LuckPerms groups:");
                    user.getInheritedGroups(user.getQueryOptions()).forEach(group -> {
                        logger.info("DEBUG: - " + group.getName());
                    });
                    
                    Map<String, String> rankMappings = plugin.getConfigManager().getRankMappings();
                    logger.info("DEBUG: Current rank mappings in config:");
                    for (Map.Entry<String, String> entry : rankMappings.entrySet()) {
                        logger.info("DEBUG: - " + entry.getKey() + " -> " + entry.getValue());
                    }
                } else {
                    logger.info("DEBUG: " + player.getName() + " has no LuckPerms user data");
                }
            } catch (Exception e) {
                logger.warning("DEBUG: Error checking " + player.getName() + " groups: " + e.getMessage());
            }
        } else {
            logger.info("DEBUG: LuckPerms not available for " + player.getName());
        }
    }
    
    private boolean startDutyTransition(Player player, String guardRank) {
        UUID playerId = player.getUniqueId();
        
        
        cancelDutyTransition(player, null);
        
        
        transitionLocations.put(playerId, player.getLocation().clone());
        
        int immobilizationTime = plugin.getConfigManager().getImmobilizationTime();
        
        if (plugin.getConfigManager().isDebugMode()) {
            LoggingUtils.debug(logger, true, "Starting duty transition for " + player.getName() + " (" + immobilizationTime + "s)");
        }
        
        
        plugin.getBossBarManager().showDutyBossBar(player, immobilizationTime, guardRank);
        
        
        BukkitTask task = new BukkitRunnable() {
            private int remaining = immobilizationTime;
            private final long startedAt = System.currentTimeMillis();
            
            @Override
            public void run() {
                try {
                    
                    if (!player.isOnline()) {
                        dutyTransitions.remove(playerId);
                        transitionLocations.remove(playerId);
                        this.cancel();
                        return;
                    }
                    
                    long maxMillis = Math.max(immobilizationTime * 2000L, 15000L);
                    if (System.currentTimeMillis() - startedAt > maxMillis) {
                        cancelDutyTransition(player, "duty.restrictions.transition-timeout");
                        this.cancel();
                        return;
                    }
                    
                    if (remaining <= 0) {
                        
                        completeDutyActivation(player, guardRank);
                        dutyTransitions.remove(playerId);
                        transitionLocations.remove(playerId);
                        this.cancel();
                        return;
                    }
                    
                    
                    Location storedLocation = transitionLocations.get(playerId);
                    if (storedLocation != null && player.getLocation().distanceSquared(storedLocation) > 0.25) {
                        
                        cancelDutyTransition(player, "duty.restrictions.movement-cancelled");
                        this.cancel();
                        return;
                    }
                    
                    
                    if (!isInDutyRegion(player)) {
                        cancelDutyTransition(player, "duty.restrictions.left-region");
                        this.cancel();
                        return;
                    }
                    
                    
                    plugin.getBossBarManager().updateDutyTransition(player, remaining, immobilizationTime);
                    
                    remaining--;
                } catch (Exception e) {
                    logger.severe("Error in duty transition countdown for " + player.getName() + ": " + e.getMessage());
                    cancelDutyTransition(player, "System error");
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
        
        dutyTransitions.put(playerId, task);
        return true;
    }
    
    public void cancelDutyTransition(Player player, String reason) {
        UUID playerId = player.getUniqueId();
        
        
        BukkitTask task = dutyTransitions.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        
        
        transitionLocations.remove(playerId);
        
        
        plugin.getBossBarManager().hideBossBarByType(player, "duty");
        
        
        if (reason != null) {
            plugin.getMessageManager().sendMessage(player, reason);
        }
        
        if (plugin.getConfigManager().isDebugMode()) {
            logger.info("DEBUG: Duty transition cancelled for " + player.getName() + " - " + reason);
        }
    }
    
    private void completeDutyActivation(Player player, String guardRank) {
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
        
        
        if (!isInDutyRegion(player)) {
            plugin.getMessageManager().sendMessage(player, "duty.restrictions.left-region");
            return;
        }
        
        
        if (!storePlayerInventory(player)) {
            plugin.getMessageManager().sendMessage(player, "universal.failed");
            return;
        }
        
        
        boolean restoredOnDutyInventory = restoreOnDutyInventory(player);
        if (!restoredOnDutyInventory) {
            
            player.getInventory().clear();
        }
        
        
        long now = System.currentTimeMillis();
        long previousOffDutyStart = data.getOffDutyTime();
        if (previousOffDutyStart > 0) {
            long consumedMillis = Math.max(0L, now - previousOffDutyStart);
            if (consumedMillis > 0) {
                data.consumeOffDutyTime(consumedMillis);
            }
        }
        
        
        data.setOnDuty(true);
        data.setDutyStartTime(now);
        data.setGuardRank(guardRank);
        data.setOffDutyTime(0);
        
        
        data.resetSessionStats();
        
        
        plugin.getDataManager().savePlayerData(data);
        
        
        plugin.getBossBarManager().hideBossBarByType(player, "duty");
        
        
        if (!restoredOnDutyInventory) {
            giveGuardKit(player, guardRank);
        }
        
        
        applyPendingTransferredItems(player, true);
        
        
        setGuardTag(player, data);
        
        
        plugin.getMessageManager().sendMessage(player, "duty.activation.success",
            stringPlaceholder("rank", guardRank));
        
        
        plugin.getMessageManager().sendGuardDutyNotification(player.getName(), guardRank);
        
        LoggingUtils.info(logger, player.getName() + " went on duty as " + guardRank);
    }
    
    private void giveGuardKit(Player player, String guardRank) {
        String kitName = plugin.getConfigManager().getKitForRank(guardRank);
        
        if (kitName == null || kitName.trim().isEmpty()) {
            logger.warning("No kit configured for rank: " + guardRank);
            return;
        }
        
        
        final ItemStack[] inventoryBefore = plugin.getConfigManager().isTransferDynamicDetectionEnabled() 
            ? player.getInventory().getContents().clone() : null;
        
        try {
            
            plugin.getCMIIntegration().giveKit(player, kitName)
                .thenAccept(success -> {
                    if (success) {
                        plugin.getMessageManager().sendMessage(player, "duty.activation.kit-given", 
                            MessageManager.stringPlaceholder("kit", kitName));
                        
                        LoggingUtils.debug(logger, plugin.getConfigManager().isDebugMode(), "Successfully gave kit " + kitName + " to " + player.getName() + " via CMI integration");
                        
                        
                        if (plugin.getConfigManager().isTransferDynamicDetectionEnabled() && inventoryBefore != null) {
                            int delayTicks = plugin.getConfigManager().getKitCaptureDelayTicks();
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                captureGuardKitItemsOptimized(player, inventoryBefore);
                            }, delayTicks);
                        }
                    } else {
                        LoggingUtils.warn(logger, "CMI integration failed for kit " + kitName + " - falling back to console command");
                        
                        
                        try {
            String command = "cmi kit " + kitName + " " + player.getName();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            
                            plugin.getMessageManager().sendMessage(player, "duty.activation.kit-given-fallback", 
                                MessageManager.stringPlaceholder("kit", kitName));
            
                            LoggingUtils.debug(logger, plugin.getConfigManager().isDebugMode(), "Gave kit " + kitName + " to " + player.getName() + " via fallback console command");
                            
                            
                            if (plugin.getConfigManager().isTransferDynamicDetectionEnabled() && inventoryBefore != null) {
                                int delayTicks = plugin.getConfigManager().getKitCaptureDelayTicks() * 2; 
                                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                    captureGuardKitItemsOptimized(player, inventoryBefore);
                                }, delayTicks);
                            }
                        } catch (Exception fallbackError) {
                            LoggingUtils.error(logger, "Both CMI integration and fallback failed for kit " + kitName + ": " + fallbackError.getMessage());
                            plugin.getMessageManager().sendMessage(player, "duty.activation.kit-failed", 
                                MessageManager.stringPlaceholder("kit", kitName));
                        }
                    }
                })
                .exceptionally(throwable -> {
                    LoggingUtils.error(logger, "CMI kit integration error for " + kitName + ": " + throwable.getMessage());
                    plugin.getMessageManager().sendMessage(player, "duty.activation.kit-failed", 
                        MessageManager.stringPlaceholder("kit", kitName));
                    return null;
                });
                
        } catch (Exception e) {
            LoggingUtils.warn(logger, "Failed to initiate kit giving for " + kitName + " to " + player.getName() + ": " + e.getMessage());
            plugin.getMessageManager().sendMessage(player, "duty.activation.kit-failed", 
                MessageManager.stringPlaceholder("kit", kitName));
        }
    }
    
    public boolean goOffDuty(Player player, PlayerData data) {
        if (!data.isOnDuty()) {
            plugin.getMessageManager().sendMessage(player, "duty.deactivation.already-off");
            return false;
        }
        
        
        if (data.isBeingChased() || plugin.getDataManager().isGuardChasing(player.getUniqueId())) {
            plugin.getMessageManager().sendMessage(player, "duty.restrictions.cannot-during-chase");
            return false;
        }
        
        
        
        
        if (!data.hasAvailableOffDutyTime()) {
            plugin.getMessageManager().sendMessage(player, "duty.restrictions.no-earned-off-duty-time");
            return false;
        }
        
        
        if (!isInValidOffDutyRegion(player)) {
            plugin.getMessageManager().sendMessage(player, "duty.restrictions.wrong-region");
            return false;
        }
        
        
        long dutyTime = System.currentTimeMillis() - data.getDutyStartTime();
        data.addDutyTime(dutyTime);
        
        
        storeOnDutyInventory(player);
        
        
        data.setOnDuty(false);
        data.setOffDutyTime(System.currentTimeMillis());
        
        
        data.resetConsumedOffDutyTime();
        
        
        data.setHasBeenNotifiedOfExpiredTime(false);
        
        
        boolean inventoryRestored = restorePlayerInventory(player);
        
        
        applyPendingTransferredItems(player, false);
        
        
        plugin.getDataManager().savePlayerData(data);
        
		
		long availableSeconds = data.getAvailableOffDutyTimeInMillis() / 1000L;
		plugin.getMessageManager().sendMessage(player, "duty.deactivation.success-with-time",
			timePlaceholder("time", availableSeconds));
        
        
        if (inventoryRestored) {
            LoggingUtils.debug(logger, plugin.getConfigManager().isDebugMode(), "Successfully restored original inventory for " + player.getName() + " when going off duty");
        } else {
            
            LoggingUtils.debug(logger, plugin.getConfigManager().isDebugMode(), "No stored inventory found for " + player.getName() + " when going off duty");
        }
        
        
        removeGuardTag(player);
        
        
        plugin.getCMIIntegration().cleanupPlayerAttachments(player);
        
        
        plugin.getMessageManager().sendActionBar(player, "actionbar.duty-deactivated");
        
        
        if (plugin.getConfigManager().isDutyBankingEnabled()) {
            plugin.getDutyBankingManager().handleDutyEnd(player, dutyTime / 1000L);
        }
        
		logger.info(player.getName() + " went off duty after " + (dutyTime / 1000) + " seconds (has " + availableSeconds + "s off-duty time)");
        
        return true;
    }
    
    
    private void awardBaseOffDutyTime(Player player, PlayerData data) {
        int baseTimeMinutes = plugin.getConfigManager().getBaseOffDutyEarned();
        long baseTimeMillis = baseTimeMinutes * 60L * 1000L;
        
        data.addEarnedOffDutyTime(baseTimeMillis);
        data.setHasEarnedBaseTime(true);
        
        plugin.getDataManager().savePlayerData(data);
        
        
        updateGuardTag(player, data);
        
        plugin.getMessageManager().sendMessage(player, "duty.earning.base-time-earned",
            timePlaceholder("time", baseTimeMinutes * 60L));
        
        logger.info(player.getName() + " earned " + baseTimeMinutes + " minutes of off-duty time (base)");
    }
    
    
    private void checkPerformanceBonuses(Player player, PlayerData data) {
        
        int searchesPerBonus = plugin.getConfigManager().getSearchesPerBonus();
        if (data.getSessionSearches() >= searchesPerBonus) {
            int bonusTime = plugin.getConfigManager().getSearchBonusTime();
            awardPerformanceBonus(player, data, bonusTime * 60L * 1000L, "searches");
            data.setSessionSearches(data.getSessionSearches() - searchesPerBonus);
        }
        
        
        int killsPerBonus = plugin.getConfigManager().getKillsPerBonus();
        if (data.getSessionKills() >= killsPerBonus) {
            int bonusTime = plugin.getConfigManager().getKillBonusTime();
            awardPerformanceBonus(player, data, bonusTime * 60L * 1000L, "kills");
            data.setSessionKills(data.getSessionKills() - killsPerBonus);
        }
    }
    
    
    private void awardTimeBasedBonus(Player player, PlayerData data) {
        int bonusMinutes = plugin.getConfigManager().getDutyTimeBonusRate();
        long bonusMillis = bonusMinutes * 60L * 1000L;
        
        awardPerformanceBonus(player, data, bonusMillis, "continuous duty");
        
        
        updateGuardTag(player, data);
    }
    
    
    private void awardPerformanceBonus(Player player, PlayerData data, long bonusMillis, String reason) {
        data.addEarnedOffDutyTime(bonusMillis);
        plugin.getDataManager().savePlayerData(data);
        
        
        updateGuardTag(player, data);
        
        
        boolean disableContinuous = plugin.getConfigManager().isDutySystemContinuousMessagesDisabled();
        boolean disablePerformanceSpam = plugin.getConfigManager().isDutySystemPerformanceSpamDisabled();
        boolean showBonusesOnce = plugin.getConfigManager().isDutySystemShowBonusesOnce();
        int notifyCooldown = Math.max(0, plugin.getConfigManager().getDutySystemNotificationCooldown());

        long now = System.currentTimeMillis();
        Long lastNotify = lastBonusNotificationAt.get(player.getUniqueId());
        boolean cooldownOk = lastNotify == null || (now - lastNotify) >= (notifyCooldown * 1000L);

        boolean shouldNotify = true;
        if ("continuous duty".equalsIgnoreCase(reason)) {
            if (disableContinuous) shouldNotify = false;
            if (showBonusesOnce && timeBonusNotifiedOnce.contains(player.getUniqueId())) shouldNotify = false;
        }
        if (disablePerformanceSpam) shouldNotify = false;
        if (!cooldownOk) shouldNotify = false;

        if (shouldNotify) {
            long bonusSeconds = bonusMillis / 1000L;
            plugin.getMessageManager().sendMessage(player, "duty.earning.performance-bonus",
                timePlaceholder("time", bonusSeconds),
                stringPlaceholder("reason", reason));
            lastBonusNotificationAt.put(player.getUniqueId(), now);
            if ("continuous duty".equalsIgnoreCase(reason) && showBonusesOnce) {
                timeBonusNotifiedOnce.add(player.getUniqueId());
            }
        }
        
        logger.info(player.getName() + " earned " + (bonusMillis / 60000L) + " minutes of off-duty time (" + reason + ")");
    }
    
    
    private void requireReturnToDuty(Player player, PlayerData data) {
        plugin.getMessageManager().sendMessage(player, "duty.restrictions.off-duty-time-expired");
        plugin.getMessageManager().sendMessage(player, "duty.restrictions.must-return-to-duty");
        
        
        long gracePeriod = 5 * 60 * 1000L; 
        data.addEarnedOffDutyTime(gracePeriod);
        plugin.getDataManager().savePlayerData(data);
        
        logger.info(player.getName() + " has used up their earned off-duty time");
    }

    
    
    
    private boolean storePlayerInventory(Player player) {
        if (player == null || !player.isOnline()) {
            logger.warning("Cannot store inventory for null or offline player");
            return false;
        }
        
        try {
            
            String inventoryData = InventorySerializer.serializePlayerInventory(player);
            
            if (inventoryData == null) {
                logger.warning("Failed to serialize inventory for " + player.getName());
                return false;
            }
            
            
            inventoryCache.put(player.getUniqueId(), inventoryData);
            
            
            plugin.getDataManager().savePlayerInventory(player.getUniqueId(), inventoryData);
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Stored off-duty inventory for " + player.getName());
            }
            
            return true;
        } catch (Exception e) {
            logger.severe("Failed to store inventory for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    
    private boolean storeOnDutyInventory(Player player) {
        if (player == null || !player.isOnline()) {
            logger.warning("Cannot store on-duty inventory for null or offline player");
            return false;
        }
        
        try {
            
            String inventoryData = InventorySerializer.serializePlayerInventory(player);
            
            if (inventoryData == null) {
                logger.warning("Failed to serialize on-duty inventory for " + player.getName());
                return false;
            }
            
            
            onDutyInventoryCache.put(player.getUniqueId(), inventoryData);
            
            
            plugin.getDataManager().saveOnDutyInventory(player.getUniqueId(), inventoryData);
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Stored on-duty inventory for " + player.getName());
            }
            
            return true;
        } catch (Exception e) {
            logger.severe("Failed to store on-duty inventory for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    
    private boolean restorePlayerInventory(Player player) {
        if (player == null || !player.isOnline()) {
            logger.warning("Cannot restore inventory for null or offline player");
            return false;
        }
        
        try {
            UUID playerId = player.getUniqueId();
            String inventoryData = null;
            
            
            if (inventoryCache.containsKey(playerId)) {
                inventoryData = inventoryCache.get(playerId);
            } else {
                
                inventoryData = plugin.getDataManager().loadPlayerInventory(playerId);
            }
            
            if (inventoryData == null) {
                logger.warning("No cached inventory found for " + player.getName());
                return false;
            }

            
            try {
                com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(inventoryData).getAsJsonObject();
                if (!obj.has("format") || !obj.has("data")) {
                    logger.warning("Invalid inventory data format for " + player.getName());
                    return false;
                }
            } catch (Exception ex) {
                logger.warning("Corrupted inventory data for " + player.getName() + ": " + ex.getMessage());
                return false;
            }
            
            
            int removedItems = InventorySerializer.removeGuardKitItems(player, guardKitItems);
            
            
            boolean success = InventorySerializer.deserializePlayerInventory(player, inventoryData);
            
            if (success) {
                
                inventoryCache.remove(playerId);
                plugin.getDataManager().deletePlayerInventory(playerId);
                
                if (plugin.getConfigManager().isDebugMode()) {
                    logger.info("DEBUG: Restored inventory for " + player.getName() + " (removed " + removedItems + " guard items)");
                }
            }
            
            return success;
        } catch (Exception e) {
            logger.severe("Failed to restore inventory for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    
    private boolean restoreOnDutyInventory(Player player) {
        if (player == null || !player.isOnline()) {
            logger.warning("Cannot restore on-duty inventory for null or offline player");
            return false;
        }
        
        try {
            UUID playerId = player.getUniqueId();
            String inventoryData = null;
            
            
            if (onDutyInventoryCache.containsKey(playerId)) {
                inventoryData = onDutyInventoryCache.get(playerId);
            }
            
            if (inventoryData == null) {
                
                inventoryData = plugin.getDataManager().loadOnDutyInventory(playerId);
                if (inventoryData == null) {
                    if (plugin.getConfigManager().isDebugMode()) {
                        logger.info("DEBUG: No cached or stored on-duty inventory for " + player.getName() + " - giving fresh kit");
                    }
                    return false;
                }
                
                onDutyInventoryCache.put(playerId, inventoryData);
            }
            
            
            player.getInventory().clear();
            boolean success = InventorySerializer.deserializePlayerInventory(player, inventoryData);
            
            if (success) {
                LoggingUtils.debug(logger, plugin.getConfigManager().isDebugMode(), "Restored on-duty inventory for " + player.getName());
            }
            
            return success;
        } catch (Exception e) {
            logger.severe("Failed to restore on-duty inventory for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    
    private boolean isInValidOffDutyRegion(Player player) {
        
        if (isInDutyRegion(player)) {
            return true;
        }
        
        
        String[] dutyRequiredZones = plugin.getConfigManager().getDutyRequiredZones();
        return plugin.getWorldGuardUtils().isPlayerInAnyRegion(player, dutyRequiredZones);
    }
    
    
    public void cleanupOldStoredInventories() {
        try {
            
            long cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L); 
            
            
            List<UUID> storedInventoryPlayers = plugin.getDataManager().getPlayersWithStoredInventory();
            
            int cleanedCount = 0;
            for (UUID playerId : storedInventoryPlayers) {
                PlayerData data = plugin.getDataManager().getPlayerData(playerId);
                
                
                if (data != null && !data.isOnDuty()) {
                    String inventoryData = plugin.getDataManager().loadPlayerInventory(playerId);
                    if (inventoryData != null) {
                        
                        try {
                            JsonObject inventoryObj = JsonParser.parseString(inventoryData).getAsJsonObject();
                            if (inventoryObj.has("metadata")) {
                                JsonObject metadata = inventoryObj.getAsJsonObject("metadata");
                                if (metadata.has("timestamp")) {
                                    long timestamp = metadata.get("timestamp").getAsLong();
                                    if (timestamp < cutoffTime) {
                                        plugin.getDataManager().deletePlayerInventory(playerId);
                                        cleanedCount++;
                                        
                                        if (plugin.getConfigManager().isDebugMode()) {
                                            logger.info("DEBUG: Cleaned up old stored inventory for " + playerId);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            
                            plugin.getDataManager().deletePlayerInventory(playerId);
                            cleanedCount++;
                        }
                    }
                }
            }
            
            if (cleanedCount > 0) {
                logger.info("Cleaned up " + cleanedCount + " old stored inventories");
            }
            
        } catch (Exception e) {
            logger.severe("Failed to cleanup old stored inventories: " + e.getMessage());
        }
    }
    
    
    public boolean hasStoredInventoryForRestoration(Player player) {
        if (player == null) {
            return false;
        }
        
        UUID playerId = player.getUniqueId();
        
        
        if (inventoryCache.containsKey(playerId)) {
            return true;
        }
        
        
        return plugin.getDataManager().hasStoredInventory(playerId);
    }
    
    
    public boolean restorePlayerInventoryPublic(Player player) {
        return restorePlayerInventory(player);
    }
    
    
    public boolean storeOnDutyInventoryPublic(Player player) {
        return storeOnDutyInventory(player);
    }
    
    
    public boolean restoreOnDutyInventoryPublic(Player player) {
        return restoreOnDutyInventory(player);
    }
    
    
    private final Map<UUID, List<String>> pendingOffDutyItems = new HashMap<>();
    private final Map<UUID, List<String>> pendingOnDutyItems = new HashMap<>();
    
    
    public boolean addItemToOffDutyInventory(Player player, ItemStack item) {
        try {
            UUID playerId = player.getUniqueId();
            
            
            String serializedItem = InventorySerializer.serializeItemStackToBase64(item);
            if (serializedItem == null) {
                logger.warning("Failed to serialize item for transfer: " + item.getType().name());
                return false;
            }
            
            
            pendingOffDutyItems.computeIfAbsent(playerId, k -> new ArrayList<>()).add(serializedItem);
            
            logger.info("Transfer system: " + player.getName() + " queued " + 
                       item.getAmount() + "x " + item.getType().name() + " for off-duty inventory (preserving enchantments)");
            
            return true;
        } catch (Exception e) {
            logger.severe("Failed to add item to off-duty inventory for " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    
    public boolean addItemToOnDutyInventory(Player player, ItemStack item) {
        try {
            UUID playerId = player.getUniqueId();
            
            
            String serializedItem = InventorySerializer.serializeItemStackToBase64(item);
            if (serializedItem == null) {
                logger.warning("Failed to serialize item for transfer: " + item.getType().name());
                return false;
            }
            
            
            pendingOnDutyItems.computeIfAbsent(playerId, k -> new ArrayList<>()).add(serializedItem);
            
            logger.info("Transfer system: " + player.getName() + " queued " + 
                       item.getAmount() + "x " + item.getType().name() + " for on-duty inventory (preserving enchantments)");
            
            return true;
        } catch (Exception e) {
            logger.severe("Failed to add item to on-duty inventory for " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    
    private void applyPendingTransferredItems(Player player, boolean toOnDuty) {
        UUID playerId = player.getUniqueId();
        List<String> pendingItems = toOnDuty ? pendingOnDutyItems.get(playerId) : pendingOffDutyItems.get(playerId);
        
        if (pendingItems != null && !pendingItems.isEmpty()) {
            int appliedCount = 0;
            for (String serializedItem : pendingItems) {
                ItemStack item = InventorySerializer.deserializeItemStackFromBase64(serializedItem);
                if (item != null) {
                    player.getInventory().addItem(item);
                    appliedCount++;
                }
            }
            
            
            if (toOnDuty) {
                pendingOnDutyItems.remove(playerId);
            } else {
                pendingOffDutyItems.remove(playerId);
            }
            
            if (appliedCount > 0) {
                logger.info("Applied " + appliedCount + " transferred items to " + player.getName() + 
                          "'s " + (toOnDuty ? "on-duty" : "off-duty") + " inventory");
            }
        }
    }
    
    
    public void giveGuardKitPublic(Player player, String guardRank) {
        giveGuardKit(player, guardRank);
    }
    
    
    public boolean hasGuardKitItems(Player player) {
        return InventorySerializer.hasGuardKitItems(player, guardKitItems);
    }
    
    
    public int removeGuardKitItems(Player player) {
        return InventorySerializer.removeGuardKitItems(player, guardKitItems);
    }
    
    
    public List<Material> getGuardKitItems() {
        return guardKitItems;
    }
    
    
    private void captureGuardKitItemsOptimized(Player player, ItemStack[] inventoryBefore) {
        if (!plugin.getConfigManager().isTransferDynamicDetectionEnabled()) {
            return;
        }
        
        try {
            UUID playerId = player.getUniqueId();
            List<String> kitItems = new ArrayList<>();
            ItemStack[] inventoryAfter = player.getInventory().getContents();
            
            
            if (plugin.getConfigManager().isTransferSkipIdenticalInventoriesEnabled() && 
                inventoryBefore.length == inventoryAfter.length) {
                boolean identical = true;
                for (int i = 0; i < inventoryAfter.length; i++) {
                    if (!areItemStacksEquivalent(inventoryBefore[i], inventoryAfter[i])) {
                        identical = false;
                        break;
                    }
                }
                if (identical) {
                    if (plugin.getConfigManager().isDebugMode()) {
                        logger.info("DEBUG: No inventory changes detected for " + player.getName() + " - skipping kit capture");
                    }
                    return;
                }
            }
            
            
            for (int i = 0; i < inventoryAfter.length; i++) {
                ItemStack afterItem = inventoryAfter[i];
                ItemStack beforeItem = (i < inventoryBefore.length) ? inventoryBefore[i] : null;
                
                
                if (afterItem != null && afterItem.getType() != Material.AIR) {
                    boolean isNewKitItem = false;
                    
                    if (beforeItem == null || beforeItem.getType() == Material.AIR) {
                        
                        isNewKitItem = true;
                    } else if (afterItem.getType() == beforeItem.getType()) {
                        
                        if (afterItem.getAmount() > beforeItem.getAmount()) {
                            
                            ItemStack addedItem = afterItem.clone();
                            addedItem.setAmount(afterItem.getAmount() - beforeItem.getAmount());
                            
                            
                            String serializedItem = InventorySerializer.serializeItemStackToBase64(addedItem);
                            if (serializedItem != null) {
                                kitItems.add(serializedItem);
                            }
                            continue; 
                        }
                    } else {
                        
                        isNewKitItem = true;
                    }
                    
                    if (isNewKitItem) {
                        
                        String serializedItem = InventorySerializer.serializeItemStackToBase64(afterItem);
                        if (serializedItem != null) {
                            kitItems.add(serializedItem);
                        }
                    }
                }
            }
            
            
            if (!kitItems.isEmpty()) {
                playerGuardKitItems.put(playerId, kitItems);
                
                
                if (plugin.getConfigManager().isTransferKitLoggingEnabled()) {
                    if (plugin.getConfigManager().isTransferBatchLoggingEnabled()) {
                        
                        logger.info("DEBUG: Captured " + kitItems.size() + " guard kit items for " + player.getName() + 
                                   " (including ExecutableItems and custom items)");
                        
                        
                        if (plugin.getConfigManager().isDebugMode() && 
                            plugin.getConfigManager().isTransferOnlyDebugIndividualItemsEnabled()) {
                            for (int i = 0; i < kitItems.size(); i++) {
                                try {
                                    ItemStack item = InventorySerializer.deserializeItemStackFromBase64(kitItems.get(i));
                                    if (item != null) {
                                        String displayName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() 
                                            ? item.getItemMeta().getDisplayName() : item.getType().name();
                                        logger.info("DEBUG:   Kit Item " + (i + 1) + ": " + displayName + " x" + item.getAmount());
                                    }
                                } catch (Exception e) {
                                    logger.warning("DEBUG: Could not deserialize kit item for logging: " + e.getMessage());
                                }
                            }
                        }
                    } else {
                        
                        logger.info("DEBUG: Captured " + kitItems.size() + " guard kit items for " + player.getName() + 
                                   " (including ExecutableItems and custom items)");
                        
                        for (int i = 0; i < kitItems.size(); i++) {
                            try {
                                ItemStack item = InventorySerializer.deserializeItemStackFromBase64(kitItems.get(i));
                                if (item != null) {
                                    String displayName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() 
                                        ? item.getItemMeta().getDisplayName() : item.getType().name();
                                    logger.info("DEBUG:   Kit Item " + (i + 1) + ": " + displayName + " x" + item.getAmount());
                                }
                            } catch (Exception e) {
                                logger.warning("DEBUG: Could not deserialize kit item for logging: " + e.getMessage());
                            }
                        }
                    }
                }
            } else {
                if (plugin.getConfigManager().isDebugMode()) {
                    logger.info("DEBUG: No new guard kit items detected for " + player.getName() + " (inventory unchanged)");
                }
            }
            
        } catch (Exception e) {
            logger.severe("Error capturing guard kit items for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    private void captureGuardKitItems(Player player, ItemStack[] inventoryBefore) {
        if (!plugin.getConfigManager().isTransferDynamicDetectionEnabled()) {
            return;
        }
        
        try {
            UUID playerId = player.getUniqueId();
            List<String> kitItems = new ArrayList<>();
            ItemStack[] inventoryAfter = player.getInventory().getContents();
            
            
            for (int i = 0; i < inventoryAfter.length; i++) {
                ItemStack afterItem = inventoryAfter[i];
                ItemStack beforeItem = (i < inventoryBefore.length) ? inventoryBefore[i] : null;
                
                
                if (afterItem != null && afterItem.getType() != Material.AIR) {
                    boolean isNewKitItem = false;
                    
                    if (beforeItem == null || beforeItem.getType() == Material.AIR) {
                        
                        isNewKitItem = true;
                    } else if (afterItem.getType() == beforeItem.getType()) {
                        
                        if (afterItem.getAmount() > beforeItem.getAmount()) {
                            
                            ItemStack addedItem = afterItem.clone();
                            addedItem.setAmount(afterItem.getAmount() - beforeItem.getAmount());
                            
                            
                            String serializedItem = InventorySerializer.serializeItemStackToBase64(addedItem);
                            if (serializedItem != null) {
                                kitItems.add(serializedItem);
                            }
                            continue; 
                        }
                    } else {
                        
                        isNewKitItem = true;
                    }
                    
                    if (isNewKitItem) {
                        
                        String serializedItem = InventorySerializer.serializeItemStackToBase64(afterItem);
                        if (serializedItem != null) {
                            kitItems.add(serializedItem);
                        }
                    }
                }
            }
            
            
            if (!kitItems.isEmpty()) {
                playerGuardKitItems.put(playerId, kitItems);
                
                if (plugin.getConfigManager().isTransferKitLoggingEnabled()) {
                    logger.info("DEBUG: Captured " + kitItems.size() + " guard kit items for " + player.getName() + 
                               " (including ExecutableItems and custom items)");
                    
                    
                    for (int i = 0; i < kitItems.size(); i++) {
                        try {
                            ItemStack item = InventorySerializer.deserializeItemStackFromBase64(kitItems.get(i));
                            if (item != null) {
                                String displayName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() 
                                    ? item.getItemMeta().getDisplayName() : item.getType().name();
                                logger.info("DEBUG:   Kit Item " + (i + 1) + ": " + displayName + " x" + item.getAmount());
                            }
                        } catch (Exception e) {
                            logger.warning("DEBUG: Could not deserialize kit item for logging: " + e.getMessage());
                        }
                    }
                }
            } else {
                if (plugin.getConfigManager().isDebugMode()) {
                    logger.info("DEBUG: No new guard kit items detected for " + player.getName() + " (inventory unchanged)");
                }
            }
            
        } catch (Exception e) {
            logger.severe("Error capturing guard kit items for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    public boolean isPlayerGuardKitItem(Player player, ItemStack item) {
        if (!plugin.getConfigManager().isTransferDynamicDetectionEnabled()) {
            
            return guardKitItems.contains(item.getType());
        }
        
        UUID playerId = player.getUniqueId();
        List<String> kitItems = playerGuardKitItems.get(playerId);
        
        if (kitItems == null || kitItems.isEmpty()) {
            
            return guardKitItems.contains(item.getType());
        }
        
        if (!plugin.getConfigManager().isTransferFullMetadataCheckEnabled()) {
            
            for (String serializedKitItem : kitItems) {
                try {
                    ItemStack kitItem = InventorySerializer.deserializeItemStackFromBase64(serializedKitItem);
                    if (kitItem != null && kitItem.getType() == item.getType()) {
                        return true;
                    }
                } catch (Exception e) {
                    
                }
            }
            return false;
        }
        
        
        String itemSerialized = InventorySerializer.serializeItemStackToBase64(item);
        if (itemSerialized == null) {
            return false;
        }
        
        for (String serializedKitItem : kitItems) {
            try {
                ItemStack kitItem = InventorySerializer.deserializeItemStackFromBase64(serializedKitItem);
                if (kitItem != null && areItemStacksEquivalent(item, kitItem)) {
                    return true;
                }
            } catch (Exception e) {
                
            }
        }
        
        return false;
    }
    
    
    private boolean areItemStacksEquivalent(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) {
            return item1 == item2;
        }
        
        if (item1.getType() != item2.getType()) {
            return false;
        }
        
        
        return java.util.Objects.equals(item1.getItemMeta(), item2.getItemMeta());
    }

    
    
    public String getPlayerGuardRank(Player player) {
        
        if (plugin.getConfigManager().isLuckPermsRequired() && luckPerms == null) {
            plugin.getUnauthorizedAccessSpamManager().logUnauthorizedAccess(player, 
                "LuckPerms is required for guard rank detection but not available", true);
            return null;
        }
        
        
        if (luckPerms != null) {
            String rank = detectRankFromLuckPerms(player);
            if (rank != null) {
                return rank;
            }
            
            
            plugin.getUnauthorizedAccessSpamManager().logUnauthorizedAccess(player, 
                "Player " + player.getName() + " has no mapped guard rank in LuckPerms", false);
        }
        
        
        if (luckPerms == null && !plugin.getConfigManager().isLuckPermsRequired()) {
            logger.warning("SECURITY WARNING: Using fallback permission-based rank detection for " + player.getName());
            return detectRankFromPermissions(player);
        }
        
        
        return null;
    }
    
    private String detectRankFromLuckPerms(Player player) {
        try {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null) {
                return null;
            }
            
                    Map<String, String> rankMappings = plugin.getConfigManager().getRankMappings();
        
        
        
        String[] rankOrder = {"captain", "sergeant", "officer", "private", "trainee"};
        
        for (String rankKey : rankOrder) {
                String groupName = rankMappings.get(rankKey);
                if (groupName != null) {
                    
                    boolean hasGroup = user.getInheritedGroups(user.getQueryOptions()).stream()
                            .anyMatch(group -> group.getName().equalsIgnoreCase(groupName));
                    
                    if (hasGroup) {
                        return rankKey;
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.warning("Error detecting rank from LuckPerms for " + player.getName() + ": " + e.getMessage());
            return null;
        }
    }
    
    private String detectRankFromPermissions(Player player) {
        
        
        if (player.hasPermission("edencorrections.guard.captain")) return "captain";
        if (player.hasPermission("edencorrections.guard.sergeant")) return "sergeant";
        if (player.hasPermission("edencorrections.guard.officer")) return "officer";
        if (player.hasPermission("edencorrections.guard.private")) return "private";
        if (player.hasPermission("edencorrections.guard.trainee")) return "trainee";
        if (player.hasPermission("edencorrections.guard")) return "trainee"; 
        
        return null;
    }
    
    private boolean isInDutyRegion(Player player) {
        return plugin.getWorldGuardUtils().isPlayerInDutyRegion(player);
    }
    
    public boolean isInRegion(Player player, String regionName) {
        return plugin.getWorldGuardUtils().isPlayerInRegion(player, regionName);
    }
    
    public boolean isInAnyRegion(Player player, String[] regionNames) {
        return plugin.getWorldGuardUtils().isPlayerInAnyRegion(player, regionNames);
    }

    
    
    public boolean canGoOnDuty(PlayerData data) {
        if (data.isOnDuty()) return false;
        
        
        if (data.getOffDutyTime() == 0) {
            return true;
        }
        
        long offDutyTime = System.currentTimeMillis() - data.getOffDutyTime();
        long requiredOffDutyTime = getRequiredOffDutyTime();
        
        return offDutyTime >= requiredOffDutyTime;
    }
    
    
    public boolean canGoOnDuty(PlayerData data, Player player) {
        if (data.isOnDuty()) return false;
        
        
        if (data.getOffDutyTime() == 0) {
            return true;
        }
        
        
        if (player.hasPermission("edencorrections.admin.bypass.cooldown")) {
            return true;
        }
        
        long offDutyTime = System.currentTimeMillis() - data.getOffDutyTime();
        long requiredOffDutyTime = getRequiredOffDutyTime();
        
        return offDutyTime >= requiredOffDutyTime;
    }
    
    public long getRemainingOffDutyTime(PlayerData data) {
        if (data.getOffDutyTime() == 0) return 0;
        
        long offDutyTime = System.currentTimeMillis() - data.getOffDutyTime();
        long requiredOffDutyTime = getRequiredOffDutyTime();
        
        return Math.max(0, requiredOffDutyTime - offDutyTime) / 1000L; 
    }
    
    
    @Deprecated
    public boolean hasGuardPermission(Player player) {
        
        logger.warning("DEPRECATED: hasGuardPermission() called for " + player.getName() + " - use hasValidGuardRank() instead");
        return hasValidGuardRank(player);
    }
    
    
    public boolean hasValidGuardRank(Player player) {
        
        return getPlayerGuardRank(player) != null;
    }
    
    
    public boolean hasGuardAccessOrBypass(Player player) {
        
        if (plugin.getConfigManager().isAdminBypassEnabled() && 
            player.hasPermission("edencorrections.admin.bypass.guard-rank")) {
            plugin.getUnauthorizedAccessSpamManager().logUnauthorizedAccess(player, 
                "Admin bypass used by " + player.getName() + " for guard access", false);
            return true;
        }
        
        
        if (plugin.getConfigManager().isStrictRankEnforcementEnabled()) {
            boolean hasValidRank = hasValidGuardRank(player);
            if (!hasValidRank) {
                plugin.getUnauthorizedAccessSpamManager().logUnauthorizedAccess(player, "guard access attempt");
            }
            return hasValidRank;
        } else {
            
            logger.warning("SECURITY WARNING: Strict rank enforcement is disabled - using legacy permission checks");
            return player.hasPermission("edencorrections.guard");
        }
    }
    
    
    public boolean isSubjectToGuardRestrictions(Player player) {
        
        return hasValidGuardRank(player);
    }
    
    public boolean isOnDuty(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        return data != null && data.isOnDuty();
    }
    
    
    public boolean forceOffDuty(Player player, PlayerData data) {
        if (!data.isOnDuty()) {
            
            restorePlayerInventoryPublic(player);
            return true;
        }
        
        
        long dutyTime = System.currentTimeMillis() - data.getDutyStartTime();
        data.addDutyTime(dutyTime);
        
        
        storeOnDutyInventory(player);
        
        
        data.setOnDuty(false);
        data.setOffDutyTime(System.currentTimeMillis());
        
        
        data.resetConsumedOffDutyTime();
        
        
        data.setHasBeenNotifiedOfExpiredTime(false);
        
        
        data.clearPenaltyTracking();
        
        
        restorePlayerInventory(player);
        
        
        applyPendingTransferredItems(player, false);
        
        
        plugin.getDataManager().savePlayerData(data);
        
        
        plugin.getBossBarManager().hideBossBar(player);
        plugin.getMessageManager().clearActionBar(player);
        
        
        plugin.getCMIIntegration().cleanupPlayerAttachments(player);
        
        
        plugin.getMessageManager().sendMessage(player, "duty.deactivation.success");
        
        if (plugin.getConfigManager().isDebugMode()) {
            LoggingUtils.debug(logger, true, "Force off duty completed for " + player.getName() + 
                " (duty time: " + (dutyTime / 60000L) + " minutes)");
        }
        
        return true;
    }

    public boolean isInDutyTransition(Player player) {
        return dutyTransitions.containsKey(player.getUniqueId());
    }
    
    public long getRequiredOffDutyTime() {
        return plugin.getConfigManager().getBaseDutyRequirement() * 60L * 1000L; 
    }
    
    public long getMaxOffDutyTime() {
        return getRequiredOffDutyTime() * 3; 
    }
    
    public long getGracePenalty() {
        return 300000L; 
    }
    
    
    
    
    public void awardSearchPerformance(Player guard) {
        if (!isOnDuty(guard)) return;
        
        PlayerData data = plugin.getDataManager().getPlayerData(guard.getUniqueId());
        if (data != null) {
            data.incrementSessionSearches();
            plugin.getDataManager().savePlayerData(data);
            
            
            updateGuardTag(guard, data);
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: " + guard.getName() + " performed search (total: " + data.getSessionSearches() + ")");
            }
        }
    }
    
    
    public void awardSuccessfulSearchPerformance(Player guard) {
        if (!isOnDuty(guard)) return;
        
        PlayerData data = plugin.getDataManager().getPlayerData(guard.getUniqueId());
        if (data != null) {
            data.incrementSessionSuccessfulSearches();
            
            
            int bonusMinutes = plugin.getConfigManager().getSuccessfulSearchBonus();
            long bonusMillis = bonusMinutes * 60L * 1000L;
            awardPerformanceBonus(guard, data, bonusMillis, "successful search");
            
            
            updateGuardTag(guard, data);
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: " + guard.getName() + " successful search (total: " + data.getSessionSuccessfulSearches() + ")");
            }
        }
    }
    
    
    public void awardArrestPerformance(Player guard) {
        if (!isOnDuty(guard)) return;
        
        PlayerData data = plugin.getDataManager().getPlayerData(guard.getUniqueId());
        if (data != null) {
            data.incrementSessionArrests();
            
            
            int bonusMinutes = plugin.getConfigManager().getSuccessfulArrestBonus();
            long bonusMillis = bonusMinutes * 60L * 1000L;
            awardPerformanceBonus(guard, data, bonusMillis, "successful arrest");
            
            
            updateGuardTag(guard, data);
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: " + guard.getName() + " successful arrest (total: " + data.getSessionArrests() + ")");
            }
        }
    }
    
    
    public void awardKillPerformance(Player guard, Player victim) {
        if (!isOnDuty(guard)) return;
        
        PlayerData data = plugin.getDataManager().getPlayerData(guard.getUniqueId());
        if (data != null) {
            data.incrementSessionKills();
            plugin.getDataManager().savePlayerData(data);
            
            
            updateGuardTag(guard, data);
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: " + guard.getName() + " killed " + victim.getName() + " (total: " + data.getSessionKills() + ")");
            }
        }
    }
    
    
    public void awardDetectionPerformance(Player guard) {
        if (!isOnDuty(guard)) return;
        
        PlayerData data = plugin.getDataManager().getPlayerData(guard.getUniqueId());
        if (data != null) {
            data.incrementSessionDetections();
            
            
            int bonusMinutes = plugin.getConfigManager().getSuccessfulDetectionBonus();
            long bonusMillis = bonusMinutes * 60L * 1000L;
            awardPerformanceBonus(guard, data, bonusMillis, "successful detection");
            
            
            updateGuardTag(guard, data);
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: " + guard.getName() + " successful detection (total: " + data.getSessionDetections() + ")");
            }
        }
    }
    
    
    
    public void cleanup() {
        
        for (Map.Entry<UUID, BukkitTask> entry : dutyTransitions.entrySet()) {
            BukkitTask task = entry.getValue();
            if (task != null) {
                task.cancel();
            }
            
            
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                plugin.getBossBarManager().hideBossBarByType(player, "duty");
            }
        }
        
        
        for (Map.Entry<UUID, String> entry : inventoryCache.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                restorePlayerInventory(player);
            }
        }
        
        
        
        dutyTransitions.clear();
        transitionLocations.clear();
        inventoryCache.clear();
        onDutyInventoryCache.clear();
        pendingOffDutyItems.clear();
        pendingOnDutyItems.clear();
    }
    
    public void cleanupPlayer(Player player) {
        
        cancelDutyTransition(player, null);
        
        
        if (isOnDuty(player)) {
            
            removeGuardTag(player);
            
            if (plugin.getConfigManager().isGuardDefaultOnDuty()) {
                
                storeOnDutyInventory(player);
                
                if (plugin.getConfigManager().isDebugMode()) {
                    logger.info("DEBUG: Guard " + player.getName() + " logging off while on duty - stored on-duty inventory");
                }
            }
            
            
            
            
            UUID playerId = player.getUniqueId();
            if (plugin.getConfigManager().isDebugMode()) {
                boolean hasOffDutyCache = inventoryCache.containsKey(playerId);
                logger.info("DEBUG: Guard " + player.getName() + " logging off while on duty - preserving off-duty inventory cache (exists: " + hasOffDutyCache + ")");
            }
            
            
            pendingOffDutyItems.remove(playerId);
            pendingOnDutyItems.remove(playerId);
            
            
            playerGuardKitItems.remove(playerId);
        } else {
            
            UUID playerId = player.getUniqueId();
            if (inventoryCache.containsKey(playerId)) {
                
                inventoryCache.remove(playerId);
                plugin.getDataManager().deletePlayerInventory(playerId);
                
                restorePlayerInventory(player);
            }
        }
    }
    
    
    
    
    private void setGuardTag(Player player, PlayerData data) { }
    
    private void removeGuardTag(Player player) { }
    
    private void updateGuardTag(Player player, PlayerData data) { }
    
    private void cleanupAllTags() { }
} 