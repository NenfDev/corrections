package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import dev.lsdmc.edenCorrections.utils.LoggingUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static dev.lsdmc.edenCorrections.managers.MessageManager.*;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
 

public class JailManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    
    private final Map<UUID, BukkitTask> activeCountdowns;
    
    
    private final Map<UUID, JailCountdownData> countdownData;
    
    
    private final Map<UUID, BukkitTask> minigameTasks = new HashMap<>();
    private final Map<UUID, MinigameState> minigameStates = new HashMap<>();
    private final MiniMessage mm = MiniMessage.miniMessage();
    
    
    private final java.util.concurrent.ConcurrentHashMap<java.util.UUID, Long> jailTeleportBypassUntil = new java.util.concurrent.ConcurrentHashMap<>();

    public JailManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.activeCountdowns = new HashMap<>();
        this.countdownData = new HashMap<>();
    }
    
    public void initialize() {
        LoggingUtils.info(logger, "JailManager initialized successfully!");
        
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            try {
                
                for (UUID targetId : new java.util.ArrayList<>(minigameStates.keySet())) {
                    MinigameState st = minigameStates.get(targetId);
                    if (st == null) continue;
                    if (System.currentTimeMillis() >= st.endTimeMs) {
                        stopArrestMinigame(targetId, true);
                    }
                }
                
                for (UUID targetId : new java.util.ArrayList<>(activeCountdowns.keySet())) {
                    org.bukkit.entity.Player p = plugin.getServer().getPlayer(targetId);
                    if (p == null || !p.isOnline()) {
                        cancelCountdown(targetId, "Player offline");
                    }
                }
            } catch (Exception ignored) { }
        }, 200L, 200L); 
    }
    
    public boolean startJailCountdown(Player guard, Player target, String reason) {
        if (activeCountdowns.containsKey(target.getUniqueId())) {
            plugin.getMessageManager().sendMessage(guard, "jail.restrictions.already-active");
            return false;
        }
        
        
        if (!plugin.getSecurityManager().canPlayerBeJailed(target)) {
            plugin.getMessageManager().sendMessage(guard, "security.guard-immunity.jail-protected",
                playerPlaceholder("player", target));
            plugin.getSecurityManager().logSecurityViolation("jail countdown", guard, target);
            return false;
        }
        
        
        if (!plugin.getDutyManager().isOnDuty(guard)) {
            plugin.getMessageManager().sendMessage(guard, "jail.restrictions.not-on-duty");
            return false;
        }
        
        
        double distance = guard.getLocation().distance(target.getLocation());
        double maxDistance = plugin.getConfigManager().getJailCountdownRadius();
        if (distance > maxDistance) {
            plugin.getMessageManager().sendMessage(guard, "jail.restrictions.too-far", 
                numberPlaceholder("distance", (int) distance),
                numberPlaceholder("max_distance", (int) maxDistance));
            return false;
        }
        
        
        int countdownTime = plugin.getConfigManager().getJailCountdown();
        final double fleeThreshold = plugin.getConfigManager().getJailFleeThreshold();
        final boolean chaseEnabled = plugin.getConfigManager().isJailChaseIntegrationEnabled();
        
        
        boolean bypassMinigame = false;
        try {
            dev.lsdmc.edenCorrections.models.ChaseData existing = plugin.getDataManager().getChaseByGuard(guard.getUniqueId());
            bypassMinigame = existing != null && existing.getTargetId() != null && existing.getTargetId().equals(target.getUniqueId());
        } catch (Exception ignored) {}
        
        
        JailCountdownData data = new JailCountdownData(
            guard.getUniqueId(), 
            target.getUniqueId(), 
            reason, 
            target.getLocation(),
            guard.getLocation(),
            bypassMinigame
        );
        countdownData.put(target.getUniqueId(), data);
        
        
        plugin.getMessageManager().sendMessage(guard, "jail.countdown.started",
            stringPlaceholder("target", target.getName()),
            numberPlaceholder("time", countdownTime));
        plugin.getMessageManager().sendMessage(target, "jail.countdown.target-notification",
            numberPlaceholder("seconds", countdownTime));
        
        
        plugin.getBossBarManager().showJailBossBar(target, countdownTime);
        
        
        if (plugin.getConfigManager().isArrestMinigameEnabled() && !bypassMinigame) {
            startArrestMinigame(guard, target, countdownTime);
        }
        
        
        BukkitTask countdownTask = new BukkitRunnable() {
            int remaining = countdownTime;
            
            @Override
            public void run() {
                try {
                    
                    if (!guard.isOnline() || !target.isOnline()) {
                        cancelCountdown(target.getUniqueId(), "Player disconnected");
                        return;
                    }
                    
                    
                    double guardDistance = guard.getLocation().distance(target.getLocation());
                    if (guardDistance > maxDistance) {
                        cancelCountdown(target.getUniqueId(), "Guard moved too far away");
                        plugin.getMessageManager().sendMessage(guard, "jail.countdown.cancelled",
                            stringPlaceholder("reason", "Guard moved too far away"));
                        return;
                    }
                    
                    
                    if (chaseEnabled && !data.isSkipFleeDetection()) {
                        Location targetCurrent = target.getLocation();
                        Location guardCurrent = guard.getLocation();
                        
                        
                        double targetMovement = data.getInitialLocation().distance(targetCurrent);
                        double guardMovement = data.getInitialGuardLocation().distance(guardCurrent);
                        
                        
                        
                        double relativeMovement = targetMovement - guardMovement;
                        
                        
                        double maxRelativeMovement = fleeThreshold + 1.5; 
                        
                        
                        
                        double guardTargetDistance = guardCurrent.distance(targetCurrent);
                        boolean tooFarFromGuard = guardTargetDistance > (maxDistance * 1.2); 
                        
                        if (relativeMovement > maxRelativeMovement || tooFarFromGuard) {
                            
                            cancelCountdown(target.getUniqueId(), "Target fled");
                            startChaseAfterFlee(guard, target, reason, data);
                            return;
                        }
                    }
                    
                    remaining--;
                    
                    
                    plugin.getBossBarManager().updateJailCountdown(target, remaining, countdownTime);
                    
                    if (remaining <= 0) {
                        
                        completeJail(guard, target, reason);
                        cancel();
                        activeCountdowns.remove(target.getUniqueId());
                        countdownData.remove(target.getUniqueId());
                    } else if (remaining <= 3) {
                        
                        plugin.getMessageManager().sendMessage(guard, "jail.countdown.progress",
                            numberPlaceholder("seconds", remaining));
                        plugin.getMessageManager().sendMessage(target, "jail.countdown.progress",
                            numberPlaceholder("seconds", remaining));
                    }
                } catch (Exception e) {
                    logger.severe("Error in jail countdown for " + target.getName() + ": " + e.getMessage());
                    cancelCountdown(target.getUniqueId(), "System error");
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); 
        
        activeCountdowns.put(target.getUniqueId(), countdownTask);
        
        logger.info("Jail countdown started: " + guard.getName() + " -> " + target.getName() + " (" + reason + ")");
        return true;
    }
    
    private void cancelCountdown(UUID targetId, String reason) {
        BukkitTask task = activeCountdowns.remove(targetId);
        if (task != null) {
            task.cancel();
        }
        
        
        countdownData.remove(targetId);
        
        
        stopArrestMinigame(targetId, true);
        
        Player target = plugin.getServer().getPlayer(targetId);
        if (target != null) {
            plugin.getMessageManager().sendMessage(target, "jail.countdown.cancelled",
                stringPlaceholder("reason", reason));
            plugin.getBossBarManager().hideBossBarByType(target, "jail");
        }
    }
    
    private void startChaseAfterFlee(Player guard, Player target, String reason, JailCountdownData data) {
        
        plugin.getMessageManager().sendMessage(guard, "jail.countdown.fled-chase-started",
            stringPlaceholder("player", target.getName()));
        plugin.getMessageManager().sendMessage(target, "jail.countdown.fled-chase-notification");
        
        
        plugin.getBossBarManager().hideBossBarByType(target, "jail");
        
        
        stopArrestMinigame(target.getUniqueId(), true);
        
        
        int currentLevel = plugin.getWantedManager().getWantedLevel(target);
        plugin.getWantedManager().increaseWantedLevel(target, currentLevel + 1, "Fleeing arrest: " + reason);
        
        
        boolean chaseStarted = plugin.getChaseManager().startChase(guard, target);
        
        if (!chaseStarted) {
            
            plugin.getMessageManager().sendMessage(guard, "chase.restrictions.unable-to-start");
        }
        
        logger.info("Chase started after jail flee: " + guard.getName() + " -> " + target.getName() + " (" + reason + ")");
    }
    
    private void completeJail(Player guard, Player target, String reason) {
        
        int wantedLevel = plugin.getWantedManager().getWantedLevel(target);
        int jailTime = calculateJailTime(wantedLevel);
        
        
        stopArrestMinigame(target.getUniqueId(), true);
        
        
        grantJailTeleportBypass(target, 5);

        
        executeJailCommand(guard, target, jailTime, reason).thenAccept(jailSuccess -> {
            if (jailSuccess) {
                
                
                
                try { target.removePotionEffect(PotionEffectType.SLOWNESS); } catch (Exception ignored) {}

                
                plugin.getWantedManager().clearWantedLevel(target);
                
                
                plugin.getContrabandManager().removeContrabandOnCapture(target);
                
                
                PlayerData guardData = plugin.getDataManager().getOrCreatePlayerData(guard.getUniqueId(), guard.getName());
                PlayerData targetData = plugin.getDataManager().getOrCreatePlayerData(target.getUniqueId(), target.getName());
                
                guardData.incrementArrests();
                targetData.incrementViolations();
                
                plugin.getDataManager().savePlayerData(guardData);
                plugin.getDataManager().savePlayerData(targetData);
                
                
                plugin.getMessageManager().sendMessage(guard, "jail.arrest.success",
                    stringPlaceholder("player", target.getName()),
                    timePlaceholder("time", jailTime));
                plugin.getMessageManager().sendMessage(target, "jail.arrest.target-notification",
                    timePlaceholder("time", jailTime),
                    stringPlaceholder("reason", reason != null ? reason : "No reason specified"));
                
                
                plugin.getBossBarManager().hideBossBarByType(target, "jail");
                
                
                notifyGuards("jail.arrest.guard-alert",
                    stringPlaceholder("guard", guard.getName()),
                    stringPlaceholder("target", target.getName()),
                    timePlaceholder("time", jailTime));
                
                
                plugin.getDutyManager().awardArrestPerformance(guard);
                
                logger.info(guard.getName() + " successfully arrested " + target.getName() + " for " + jailTime + " seconds");
            } else {
                
                plugin.getMessageManager().sendMessage(guard, "universal.failed");
                plugin.getMessageManager().sendMessage(target, "jail.countdown.cancelled",
                    stringPlaceholder("reason", "Jail system error"));
                
                
                plugin.getBossBarManager().hideBossBarByType(target, "jail");
                
                logger.warning("Failed to jail " + target.getName() + " - CMI command unsuccessful");
            }
        }).exceptionally(throwable -> {
            
            plugin.getMessageManager().sendMessage(guard, "universal.failed");
            plugin.getMessageManager().sendMessage(target, "jail.countdown.cancelled",
                stringPlaceholder("reason", "System error"));
            
            
            plugin.getBossBarManager().hideBossBarByType(target, "jail");
            
            logger.severe("Exception during jail process for " + target.getName() + ": " + throwable.getMessage());
            throwable.printStackTrace();
            return null;
        });
    }
    
    private int calculateJailTime(int wantedLevel) {
        int baseTime = plugin.getConfigManager().getBaseJailTime();
        int levelMultiplier = plugin.getConfigManager().getJailLevelMultiplier();
        
        return baseTime + (wantedLevel * levelMultiplier);
    }
    
    private CompletableFuture<Boolean> executeJailCommand(Player guard, Player target, int jailTime, String reason) {
        
        if (plugin.getCMIIntegration() != null && plugin.getCMIIntegration().isAvailable()) {
            
            String formattedReason = reason != null ? reason : "Arrested by " + guard.getName();
            return plugin.getCMIIntegration().jailPlayer(guard, target, jailTime, formattedReason);
        } else {
            
            logger.warning("CMI integration not available - cannot jail player " + target.getName());
            return CompletableFuture.completedFuture(false);
        }
    }
    
    public CompletableFuture<Boolean> jailPlayer(Player guard, Player target, String reason) {
        
        try {
            if (plugin.getConfigManager().shouldPreventCaptureInCombat() &&
                (plugin.getChaseManager().isInCombat(target) || plugin.getChaseManager().isInCombat(guard))) {
                plugin.getMessageManager().sendMessage(guard, "chase.restrictions.combat-timer-active");
                return java.util.concurrent.CompletableFuture.completedFuture(false);
            }
        } catch (Exception ignored) {}

        if (reason == null || reason.trim().isEmpty()) {
            reason = plugin.getMessageManager().getPlainTextMessage("jail.no-reason");
        }
        
        
        if (!plugin.getSecurityManager().canPlayerBeJailed(target)) {
            plugin.getMessageManager().sendMessage(guard, "security.guard-immunity.jail-protected",
                stringPlaceholder("player", target.getName()));
            plugin.getSecurityManager().logSecurityViolation("jail", guard, target);
            return java.util.concurrent.CompletableFuture.completedFuture(false);
        }
        
        
        int wantedLevel = plugin.getWantedManager().getWantedLevel(target);
        int jailTime = calculateJailTime(wantedLevel);
        
        final String finalReason = reason;
        
        
        removePlayerArmor(target);
        
        
        grantJailTeleportBypass(target, 5);

        
        return executeJailCommand(guard, target, jailTime, reason).thenApply(jailSuccess -> {
            if (jailSuccess) {
                
                
                
                try { target.removePotionEffect(PotionEffectType.SLOWNESS); } catch (Exception ignored) {}

                
                plugin.getWantedManager().clearWantedLevel(target);
                
                
                plugin.getContrabandManager().removeContrabandOnCapture(target);
                
                
                PlayerData guardData = plugin.getDataManager().getOrCreatePlayerData(guard.getUniqueId(), guard.getName());
                PlayerData targetData = plugin.getDataManager().getOrCreatePlayerData(target.getUniqueId(), target.getName());
                
                guardData.incrementArrests();
                targetData.incrementViolations();
                
                plugin.getDataManager().savePlayerData(guardData);
                plugin.getDataManager().savePlayerData(targetData);
                
                
                plugin.getMessageManager().sendMessage(guard, "jail.arrest.success",
                    stringPlaceholder("player", target.getName()),
                    timePlaceholder("time", jailTime));
                plugin.getMessageManager().sendMessage(target, "jail.arrest.target-notification",
                    timePlaceholder("time", jailTime),
                    stringPlaceholder("reason", finalReason));
                
                
                plugin.getDutyManager().awardArrestPerformance(guard);
                
                logger.info(guard.getName() + " successfully jailed " + target.getName() + " for " + jailTime + " seconds - Reason: " + finalReason);
                return true;
            } else {
                
                plugin.getMessageManager().sendMessage(guard, "universal.failed");
                logger.warning("Failed to jail " + target.getName() + " - CMI command unsuccessful");
                return false;
            }
        }).exceptionally(throwable -> {
            
            plugin.getMessageManager().sendMessage(guard, "universal.failed");
            logger.severe("Exception during jail process for " + target.getName() + ": " + throwable.getMessage());
            throwable.printStackTrace();
            return false;
        });
    }
    
    
    private void removePlayerArmor(Player player) {
        if (player == null) return;
        
        
        player.getInventory().setHelmet(null);
        player.getInventory().setChestplate(null);
        player.getInventory().setLeggings(null);
        player.getInventory().setBoots(null);
        
        
        player.getInventory().remove(Material.LEATHER_HELMET);
        player.getInventory().remove(Material.LEATHER_CHESTPLATE);
        player.getInventory().remove(Material.LEATHER_LEGGINGS);
        player.getInventory().remove(Material.LEATHER_BOOTS);
        player.getInventory().remove(Material.CHAINMAIL_HELMET);
        player.getInventory().remove(Material.CHAINMAIL_CHESTPLATE);
        player.getInventory().remove(Material.CHAINMAIL_LEGGINGS);
        player.getInventory().remove(Material.CHAINMAIL_BOOTS);
        player.getInventory().remove(Material.IRON_HELMET);
        player.getInventory().remove(Material.IRON_CHESTPLATE);
        player.getInventory().remove(Material.IRON_LEGGINGS);
        player.getInventory().remove(Material.IRON_BOOTS);
        player.getInventory().remove(Material.GOLDEN_HELMET);
        player.getInventory().remove(Material.GOLDEN_CHESTPLATE);
        player.getInventory().remove(Material.GOLDEN_LEGGINGS);
        player.getInventory().remove(Material.GOLDEN_BOOTS);
        player.getInventory().remove(Material.DIAMOND_HELMET);
        player.getInventory().remove(Material.DIAMOND_CHESTPLATE);
        player.getInventory().remove(Material.DIAMOND_LEGGINGS);
        player.getInventory().remove(Material.DIAMOND_BOOTS);
        player.getInventory().remove(Material.NETHERITE_HELMET);
        player.getInventory().remove(Material.NETHERITE_CHESTPLATE);
        player.getInventory().remove(Material.NETHERITE_LEGGINGS);
        player.getInventory().remove(Material.NETHERITE_BOOTS);
        
        logger.info("Removed all armor from " + player.getName() + " during jailing");
    }
    
    public CompletableFuture<Boolean> jailOfflinePlayer(Player executor, String targetName, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            reason = plugin.getMessageManager().getPlainTextMessage("jail.no-reason");
        }
        
        final String finalReason = reason;
        
        
        int jailTime = plugin.getConfigManager().getBaseJailTime();
        
        
        if (plugin.getCMIIntegration() != null && plugin.getCMIIntegration().isAvailable()) {
            String formattedReason = finalReason + " (Offline arrest by " + executor.getName() + ")";
            return plugin.getCMIIntegration().jailOfflinePlayer(executor, targetName, jailTime, formattedReason)
                .thenApply(success -> {
                    if (success) {
                        logger.info(executor.getName() + " successfully jailed offline player " + targetName + " for " + jailTime + " seconds - Reason: " + finalReason);
                        plugin.getMessageManager().sendMessage(executor, "jail.arrest.offline-success",
                            stringPlaceholder("target", targetName),
                            timePlaceholder("time", jailTime));
                    } else {
                        logger.warning("Failed to jail offline player " + targetName + " - CMI command unsuccessful");
                        plugin.getMessageManager().sendMessage(executor, "universal.failed");
                    }
                    return success;
                });
        } else {
            
            logger.warning("CMI integration not available - cannot jail offline player " + targetName);
            plugin.getMessageManager().sendMessage(executor, "universal.failed");
            return CompletableFuture.completedFuture(false);
        }
    }
    
    public boolean isInJailCountdown(Player player) {
        return activeCountdowns.containsKey(player.getUniqueId());
    }
    
    public void cancelJailCountdown(Player player) {
        cancelCountdown(player.getUniqueId(), plugin.getMessageManager().getPlainTextMessage("jail.manual-cancellation"));
    }
    
    private void notifyGuards(String messageKey, TagResolver... placeholders) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (plugin.getDutyManager().isSubjectToGuardRestrictions(player) && plugin.getDutyManager().isOnDuty(player)) {
                plugin.getMessageManager().sendGuardAlert(messageKey, placeholders);
            }
        }
    }
    
    
    
    
    private static class JailCountdownData {
        private final UUID guardId;
        private final UUID targetId;
        private final String reason;
        private final Location initialLocation;
        private final Location initialGuardLocation;
        private final long startTime;
        private final boolean skipFleeDetection;
        
        public JailCountdownData(UUID guardId, UUID targetId, String reason, Location initialLocation, Location initialGuardLocation, boolean skipFleeDetection) {
            this.guardId = guardId;
            this.targetId = targetId;
            this.reason = reason;
            this.initialLocation = initialLocation.clone();
            this.initialGuardLocation = initialGuardLocation.clone();
            this.startTime = System.currentTimeMillis();
            this.skipFleeDetection = skipFleeDetection;
        }
        
        public UUID getGuardId() { return guardId; }
        public UUID getTargetId() { return targetId; }
        public String getReason() { return reason; }
        public Location getInitialLocation() { return initialLocation; }
        public Location getInitialGuardLocation() { return initialGuardLocation; }
        public long getStartTime() { return startTime; }
        public boolean isSkipFleeDetection() { return skipFleeDetection; }
    }
    
    
    
    private static class MinigameState {
        private int pointerIndex;
        private int direction; 
        private long endTimeMs;
        private UUID guardId;
        private String reason;
        private int currentSpeed; 
        private long lastSpeedChange; 
        private Location immobilizeLocation; 
        private int missCount; 
        
        
        MinigameState(int startIndex, int direction, long endTimeMs, UUID guardId, String reason, int initialSpeed, Location immobilizeLocation) {
            this.pointerIndex = startIndex;
            this.direction = direction;
            this.endTimeMs = endTimeMs;
            this.guardId = guardId;
            this.reason = reason;
            this.currentSpeed = initialSpeed;
            this.lastSpeedChange = System.currentTimeMillis();
            this.immobilizeLocation = immobilizeLocation.clone();
            this.missCount = 0;
        }
        
        public Location getImmobilizeLocation() {
            return immobilizeLocation.clone();
        }
    }
    
    public boolean isInArrestMinigame(Player player) {
        return player != null && minigameStates.containsKey(player.getUniqueId());
    }
    
    public void startArrestMinigamePublic(Player guard, Player target, int durationSeconds) {
        startArrestMinigame(guard, target, durationSeconds);
    }
    
    public void stopArrestMinigamePublic(Player target) {
        if (target != null) {
            stopArrestMinigame(target.getUniqueId(), true);
        }
    }
    
    public int getMinigamePointerIndex(Player player) {
        MinigameState s = player == null ? null : minigameStates.get(player.getUniqueId());
        return s == null ? -1 : s.pointerIndex;
    }
    
    public Location getMinigameImmobilizeLocation(Player player) {
        MinigameState s = player == null ? null : minigameStates.get(player.getUniqueId());
        return s == null ? null : s.getImmobilizeLocation();
    }
    
    private void startArrestMinigame(Player guard, Player target, int durationSeconds) {
        if (guard == null || target == null) return;
        UUID targetId = target.getUniqueId();
        
        
        stopArrestMinigame(targetId, true);
        
        int length = plugin.getConfigManager().getArrestMinigameBarLength();
        int refresh = plugin.getConfigManager().getArrestMinigameRefreshTicks();
        refresh = Math.max(1, refresh);
        
        
        int initialDirection = +1;
        if (plugin.getConfigManager().isArrestMinigameRandomDirectionEnabled()) {
            initialDirection = Math.random() < 0.5 ? +1 : -1;
        }
        
        
        int startIndex = initialDirection > 0 ? 0 : length - 1;
        
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        MinigameState state = new MinigameState(startIndex, initialDirection, endTime, guard.getUniqueId(), "Arrest minigame", refresh, target.getLocation());
        minigameStates.put(targetId, state);
        
        
        
        
        final int successStart = Math.max(0, Math.min(length - 1, plugin.getConfigManager().getArrestMinigameSuccessStart()));
        final int successEnd = Math.max(0, Math.min(length - 1, plugin.getConfigManager().getArrestMinigameSuccessEnd()));
        final int sMin = Math.min(successStart, successEnd);
        final int sMax = Math.max(successStart, successEnd);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!target.isOnline()) {
                    stopArrestMinigame(targetId, false);
                    return;
                }
                if (System.currentTimeMillis() >= state.endTimeMs) {
                    
                    stopArrestMinigame(targetId, true);
                    return;
                }
                
                try {
                    
                    if (plugin.getConfigManager().isArrestMinigameSpeedVariationEnabled()) {
                        long now = System.currentTimeMillis();
                        long intervalMs = plugin.getConfigManager().getArrestMinigameSpeedChangeInterval() * 1000L;
                        if (now - state.lastSpeedChange >= intervalMs) {
                            int minSpeed = plugin.getConfigManager().getArrestMinigameMinSpeed();
                            int maxSpeed = plugin.getConfigManager().getArrestMinigameMaxSpeed();
                            state.currentSpeed = minSpeed + (int)(Math.random() * (maxSpeed - minSpeed + 1));
                            state.lastSpeedChange = now;
                            
                            if (plugin.getConfigManager().isDebugMode()) {
                                logger.info("DEBUG: Minigame speed changed to " + state.currentSpeed + " for " + target.getName());
                            }
                        }
                    }
                    
                    
                    state.pointerIndex += state.direction;
                    
                    
                    if (plugin.getConfigManager().isArrestMinigameDirectionChangeEnabled()) {
                        if (state.pointerIndex >= length - 1) {
                            state.pointerIndex = length - 1;
                            state.direction = -1;
                        } else if (state.pointerIndex <= 0) {
                            state.pointerIndex = 0;
                            state.direction = +1;
                        }
                    } else {
                        
                        if (state.pointerIndex >= length) {
                            state.pointerIndex = 0;
                        } else if (state.pointerIndex < 0) {
                            state.pointerIndex = length - 1;
                        }
                    }
                    
                    
                    String pointer = plugin.getConfigManager().getArrestMinigamePointerChar();
                    String empty = plugin.getConfigManager().getArrestMinigameEmptyChar();
                    if (pointer == null || pointer.isEmpty()) pointer = "■";
                    if (empty == null || empty.isEmpty()) empty = "□";

                    StringBuilder sb = new StringBuilder();
                    
                    
                    sb.append(plugin.getConfigManager().getArrestMinigameTitleColor());
                    sb.append(plugin.getConfigManager().getArrestMinigameTitle());
                    sb.append("<reset>");
                    
                    
                    sb.append(plugin.getConfigManager().getArrestMinigameFrameColor());
                    sb.append(" ");
                    sb.append(plugin.getConfigManager().getArrestMinigameBracketLeft());
                    
                    
                    for (int i = 0; i < length; i++) {
                        boolean inWindow = i >= sMin && i <= sMax;
                        boolean isPointer = i == state.pointerIndex;
                        
                        if (isPointer) {
                            
                            String pointerColor = inWindow ? 
                                plugin.getConfigManager().getArrestMinigamePointerSuccessColor() : 
                                plugin.getConfigManager().getArrestMinigamePointerDangerColor();
                            sb.append(pointerColor).append(pointer).append("</color>");
                        } else {
                            
                            String spaceColor = inWindow ? 
                                plugin.getConfigManager().getArrestMinigameSuccessWindowColor() : 
                                plugin.getConfigManager().getArrestMinigameFrameColor();
                            sb.append(spaceColor).append(empty).append("</color>");
                        }
                    }
                    
                    
                    sb.append(plugin.getConfigManager().getArrestMinigameBracketRight());
                    sb.append("</color>");
                    
                    
                    sb.append(" ");
                    sb.append(plugin.getConfigManager().getArrestMinigameHintColor());
                    sb.append(plugin.getConfigManager().getArrestMinigameHint());
                    sb.append("</color>");

                    Component bar = mm.deserialize(sb.toString());
                    target.sendActionBar(bar);
                } catch (Exception e) {
                    logger.warning("Minigame tick error for " + target.getName() + ": " + e.getMessage());
                }
            }
        }.runTaskTimer(plugin, 0L, Math.max(1L, state.currentSpeed));
        
        minigameTasks.put(targetId, task);
    }
    
    private void stopArrestMinigame(UUID targetId, boolean clearBar) {
        BukkitTask t = minigameTasks.remove(targetId);
        if (t != null) {
            t.cancel();
        }
        minigameStates.remove(targetId);
        
        if (clearBar) {
            Player p = plugin.getServer().getPlayer(targetId);
            if (p != null && p.isOnline()) {
                plugin.getMessageManager().clearActionBar(p);
            }
        }
    }

    public void grantJailTeleportBypass(Player player, int durationSeconds) {
        if (player == null || durationSeconds <= 0) return;
        long until = System.currentTimeMillis() + (durationSeconds * 1000L);
        jailTeleportBypassUntil.put(player.getUniqueId(), until);
    }

    public boolean hasJailTeleportBypass(Player player) {
        if (player == null) return false;
        Long until = jailTeleportBypassUntil.get(player.getUniqueId());
        if (until == null) return false;
        if (System.currentTimeMillis() > until) {
            jailTeleportBypassUntil.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    
    
    
    public boolean handleMinigameInteraction(Player target) {
        if (target == null) return false;
        UUID targetId = target.getUniqueId();
        MinigameState state = minigameStates.get(targetId);
        if (state == null) return false;
        
        int start = plugin.getConfigManager().getArrestMinigameSuccessStart();
        int end = plugin.getConfigManager().getArrestMinigameSuccessEnd();
        if (start > end) {
            int tmp = start; start = end; end = tmp;
        }
        
        if (plugin.getConfigManager().isDebugMode()) {
            logger.info("DEBUG: Minigame shift detected for " + target.getName() + 
                       " - Pointer at " + state.pointerIndex + ", Success window: " + start + "-" + end);
        }
        
        
        int winStart = Math.max(0, start - 1);
        int winEnd = Math.max(start, Math.min(end + 1, plugin.getConfigManager().getArrestMinigameBarLength() - 1));
        
        if (state.pointerIndex >= winStart && state.pointerIndex <= winEnd) {
            
            logger.info("Minigame success for " + target.getName() + " - handling success behavior");
            stopArrestMinigame(targetId, true);
            
            JailCountdownData data = countdownData.get(targetId);
            Player guard = data != null ? plugin.getServer().getPlayer(data.getGuardId()) : null;
            
            
            if (plugin.getConfigManager().isArrestMinigameClearWantedOnSuccess()) {
                plugin.getWantedManager().clearWantedLevel(target);
                logger.info("Cleared wanted level for " + target.getName() + " (minigame success)");
            } else {
                
                int bonus = plugin.getConfigManager().getArrestMinigameBonusWantedLevel();
                if (bonus > 0) {
                    int currentLevel = plugin.getWantedManager().getWantedLevel(target);
                    plugin.getWantedManager().setWantedLevel(target, currentLevel + bonus, "Minigame escape");
                    logger.info("Added " + bonus + " wanted level to " + target.getName() + " for minigame escape");
                }
            }
            
            
            if (plugin.getConfigManager().isArrestMinigameStartChaseOnSuccess() && guard != null && guard.isOnline()) {
                cancelCountdown(targetId, "Minigame success");
                startChaseAfterFlee(guard, target, "Minigame escape", data);
            } else {
                
                cancelCountdown(targetId, "Minigame success");
                if (guard != null) {
                    plugin.getMessageManager().sendMessage(guard, "jail.countdown.cancelled",
                        stringPlaceholder("reason", "Target escaped minigame"));
                }
            }
            return true;
        } else {
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Minigame miss for " + target.getName());
            }
            
            
            if (plugin.getConfigManager().isArrestMinigameMissFeedbackEnabled()) {
                String missMessage = plugin.getConfigManager().getArrestMinigameMissMessage();
                Component missComponent = mm.deserialize(missMessage);
                target.sendActionBar(missComponent);
                
                
                int duration = plugin.getConfigManager().getArrestMinigameMissFeedbackDuration();
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    
                    if (isInArrestMinigame(target)) {
                        
                    }
                }, duration);
            }

            
            try {
                state.missCount = Math.max(0, state.missCount + 1);
                int threshold = plugin.getConfigManager().getArrestMinigameMissThreshold();
                if (state.missCount >= threshold) {
                    
                    try {
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 0, false, true, true));
                    } catch (Exception ignored) {}

                    
                    JailCountdownData data = countdownData.get(targetId);
                    Player guard = data != null ? plugin.getServer().getPlayer(data.getGuardId()) : null;

                    
                    stopArrestMinigame(targetId, true);
                    cancelCountdown(targetId, "Too many misses - starting chase");
                    if (guard != null && guard.isOnline()) {
                        startChaseAfterFlee(guard, target, "Missed arrest minigame", data);
                    }
                    return true;
                }
            } catch (Exception e) {
                logger.warning("Error handling minigame miss threshold for " + target.getName() + ": " + e.getMessage());
            }
            return true;
        }
    }
} 