package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.utils.LoggingUtils;
import dev.lsdmc.edenCorrections.models.PlayerData;
import dev.lsdmc.edenCorrections.models.ChaseData;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static dev.lsdmc.edenCorrections.managers.MessageManager.*;


public class BossBarManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    
    private final Map<UUID, BossBar> activeBossBars;
    private final Map<UUID, BukkitTask> bossBarTasks;
    private final Map<UUID, String> bossBarTypes;
    
    public BossBarManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.activeBossBars = new HashMap<>();
        this.bossBarTasks = new HashMap<>();
        this.bossBarTypes = new HashMap<>();
    }
    
    public void initialize() {
        LoggingUtils.info(logger, "BossBarManager initialized successfully!");
        
        
        startBatchBossBarUpdates();
    }
    
    
    private void startBatchBossBarUpdates() {
        new BukkitRunnable() {
            @Override
            public void run() {
                
                for (Map.Entry<UUID, BossBar> entry : activeBossBars.entrySet()) {
                    UUID playerId = entry.getKey();
                    BossBar bossBar = entry.getValue();
                    String type = bossBarTypes.get(playerId);
                    
                    if (type != null && bossBar != null) {
                        Player player = plugin.getServer().getPlayer(playerId);
                        if (player != null && player.isOnline()) {
                            
                            updateBossBarByType(player, bossBar, type);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L * 5L, 20L * 5L); 
    }
    
    
    private void updateBossBarByType(Player player, BossBar bossBar, String type) {
        try {
            switch (type) {
                case "wanted":
                    
                    PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
                    if (data != null && data.isWanted()) {
                        updateWantedBossBar(player, data.getWantedLevel(), data.getRemainingWantedTime());
                    }
                    break;
                case "chase":
                    
                    ChaseData chase = plugin.getDataManager().getChaseByTarget(player.getUniqueId());
                    if (chase != null) {
                        Player guard = plugin.getServer().getPlayer(chase.getGuardId());
                        if (guard != null) {
                            double distance = player.getLocation().distance(guard.getLocation());
                            updateChaseBossBar(player, distance, guard);
                        }
                    }
                    break;
                
            }
        } catch (Exception e) {
            logger.warning("Error updating boss bar for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    
    public void showWantedBossBar(Player player, int wantedLevel, long remainingTime) {
        if (!plugin.getConfigManager().areBossBarsEnabled() || 
            !plugin.getConfigManager().isWantedBossBarEnabled()) {
            return;
        }
        
        try {
            
            hideBossBar(player);
            
            
            BossBar bossBar = createBossBar(
                "bossbar.wanted.active",
                getBossBarColor(plugin.getConfigManager().getWantedBossBarColor()),
                getBossBarOverlay(plugin.getConfigManager().getWantedBossBarOverlay()),
                player,
                numberPlaceholder("level", wantedLevel),
                starsPlaceholder("stars", wantedLevel),
                numberPlaceholder("minutes", Math.max(0, remainingTime / 60))
            );
            
            
            showBossBar(player, bossBar, "wanted");
            
            
            startWantedBossBarUpdate(player, bossBar, wantedLevel, remainingTime);
            
        } catch (Exception e) {
            logger.warning("Error showing wanted boss bar for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    
    public void showChaseTargetBossBar(Player target, Player guard, double distance) {
        if (!plugin.getConfigManager().areBossBarsEnabled() || 
            !plugin.getConfigManager().isChaseBossBarEnabled()) {
            return;
        }
        
        try {
            
            hideBossBar(target);
            
            
            BossBar bossBar = createBossBar(
                "bossbar.chase.target",
                getBossBarColor(plugin.getConfigManager().getChaseBossBarColor()),
                getBossBarOverlay(plugin.getConfigManager().getChaseBossBarOverlay()),
                target,
                playerPlaceholder("guard", guard),
                distancePlaceholder("distance", distance)
            );
            
            
            showBossBar(target, bossBar, "chase_target");
            
        } catch (Exception e) {
            logger.warning("Error showing chase target boss bar for " + target.getName() + ": " + e.getMessage());
        }
    }
    
    
    public void showChaseGuardBossBar(Player guard, Player target, double distance) {
        if (!plugin.getConfigManager().areBossBarsEnabled() || 
            !plugin.getConfigManager().isChaseBossBarEnabled()) {
            return;
        }
        
        try {
            
            hideBossBar(guard);
            
            
            BossBar bossBar = createBossBar(
                "bossbar.chase.guard",
                getBossBarColor(plugin.getConfigManager().getChaseBossBarColor()),
                getBossBarOverlay(plugin.getConfigManager().getChaseBossBarOverlay()),
                guard,
                playerPlaceholder("target", target),
                distancePlaceholder("distance", distance)
            );
            
            
            showBossBar(guard, bossBar, "chase_guard");
            
        } catch (Exception e) {
            logger.warning("Error showing chase guard boss bar for " + guard.getName() + ": " + e.getMessage());
        }
    }
    
    
    public void showCombatBossBar(Player player, int duration) {
        if (!plugin.getConfigManager().areBossBarsEnabled() || 
            !plugin.getConfigManager().isCombatBossBarEnabled()) {
            return;
        }
        
        try {
            
            hideBossBar(player);
            
            
            BossBar bossBar = createBossBar(
                "bossbar.combat-timer",
                getBossBarColor(plugin.getConfigManager().getCombatBossBarColor()),
                getBossBarOverlay(plugin.getConfigManager().getCombatBossBarOverlay()),
                player,
                numberPlaceholder("seconds", duration)
            );
            
            
            showBossBar(player, bossBar, "combat");
            
            
            startCountdownBossBar(player, bossBar, duration, "combat");
            
        } catch (Exception e) {
            logger.warning("Error showing combat boss bar for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    
    public void showJailBossBar(Player player, int duration) {
        if (!plugin.getConfigManager().areBossBarsEnabled() || 
            !plugin.getConfigManager().isJailBossBarEnabled()) {
            return;
        }
        
        try {
            
            hideBossBar(player);
            
            
            BossBar bossBar = createBossBar(
                "bossbar.jail-countdown",
                getBossBarColor(plugin.getConfigManager().getJailBossBarColor()),
                getBossBarOverlay(plugin.getConfigManager().getJailBossBarOverlay()),
                player,
                numberPlaceholder("seconds", duration)
            );
            
            
            showBossBar(player, bossBar, "jail");
            
            
            startCountdownBossBar(player, bossBar, duration, "jail");
            
        } catch (Exception e) {
            logger.warning("Error showing jail boss bar for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    
    public void showDutyBossBar(Player player, int duration, String rank) {
        if (!plugin.getConfigManager().areBossBarsEnabled() || 
            !plugin.getConfigManager().isDutyBossBarEnabled()) {
            return;
        }
        
        try {
            
            hideBossBar(player);
            
            
            BossBar bossBar = createBossBar(
                "bossbar.duty-transition",
                getBossBarColor(plugin.getConfigManager().getDutyBossBarColor()),
                getBossBarOverlay(plugin.getConfigManager().getDutyBossBarOverlay()),
                player,
                numberPlaceholder("seconds", duration),
                stringPlaceholder("rank", rank)
            );
            
            
            showBossBar(player, bossBar, "duty");
            
            
            startCountdownBossBar(player, bossBar, duration, "duty");
            
        } catch (Exception e) {
            logger.warning("Error showing duty boss bar for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    
    public void showContrabandBossBar(Player player, int duration, String description) {
        if (!plugin.getConfigManager().areBossBarsEnabled() || 
            !plugin.getConfigManager().isContrabandBossBarEnabled()) {
            return;
        }
        
        try {
            
            hideBossBar(player);
            
            
            BossBar bossBar = createBossBar(
                "bossbar.contraband-countdown",
                getBossBarColor(plugin.getConfigManager().getContrabandBossBarColor()),
                getBossBarOverlay(plugin.getConfigManager().getContrabandBossBarOverlay()),
                player,
                numberPlaceholder("seconds", duration),
                stringPlaceholder("type", description)
            );
            
            
            showBossBar(player, bossBar, "contraband");
            
            
            startCountdownBossBar(player, bossBar, duration, "contraband");
            
        } catch (Exception e) {
            logger.warning("Error showing contraband boss bar for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    
    public void showGraceBossBar(Player player, int duration) {
        if (!plugin.getConfigManager().areBossBarsEnabled() || 
            !plugin.getConfigManager().isGraceBossBarEnabled()) {
            return;
        }
        
        try {
            
            hideBossBar(player);
            
            
            BossBar bossBar = createBossBar(
                "bossbar.grace-period",
                getBossBarColor(plugin.getConfigManager().getGraceBossBarColor()),
                getBossBarOverlay(plugin.getConfigManager().getGraceBossBarOverlay()),
                player,
                numberPlaceholder("seconds", duration)
            );
            
            
            showBossBar(player, bossBar, "grace");
            
            
            startCountdownBossBar(player, bossBar, duration, "grace");
            
        } catch (Exception e) {
            logger.warning("Error showing grace boss bar for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    
    public void showPenaltyBossBar(Player player, int stage, long overrunMinutes) {
        if (!plugin.getConfigManager().areBossBarsEnabled() || 
            !plugin.getConfigManager().isPenaltyBossBarEnabled()) {
            return;
        }
        
        try {
            
            BossBar bossBar = createBossBar(
                "bossbar.penalty",
                getBossBarColor(plugin.getConfigManager().getPenaltyBossBarColor()),
                getBossBarOverlay(plugin.getConfigManager().getPenaltyBossBarOverlay()),
                player,
                numberPlaceholder("stage", stage),
                numberPlaceholder("minutes", overrunMinutes)
            );
            
            
            showBossBar(player, bossBar, "penalty");
            
            
            int duration = plugin.getConfigManager().getPenaltyBossBarDuration();
            if (duration > 0) {
                startCountdownBossBar(player, bossBar, duration, "penalty");
            }
            
        } catch (Exception e) {
            logger.warning("Error showing penalty boss bar for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    
    public void updateChaseBossBar(Player player, double distance, Player otherPlayer) {
        if (!activeBossBars.containsKey(player.getUniqueId())) {
            return;
        }
        
        String type = bossBarTypes.get(player.getUniqueId());
        if (type == null || (!type.equals("chase_target") && !type.equals("chase_guard"))) {
            return;
        }
        
        try {
            BossBar bossBar = activeBossBars.get(player.getUniqueId());
            
            if (type.equals("chase_target")) {
                
                bossBar.name(plugin.getMessageManager().getMessage(player, "bossbar.chase.target",
                    playerPlaceholder("guard", otherPlayer),
                    distancePlaceholder("distance", distance)));
            } else {
                
                bossBar.name(plugin.getMessageManager().getMessage(player, "bossbar.chase.guard",
                    playerPlaceholder("target", otherPlayer),
                    distancePlaceholder("distance", distance)));
            }
            
            
            float progress = Math.max(0.1f, Math.min(1.0f, 1.0f - (float)(distance / 100.0)));
            bossBar.progress(progress);
            
        } catch (Exception e) {
            logger.warning("Error updating chase boss bar for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    
    public void updateWantedBossBar(Player player, int wantedLevel, long remainingTime) {
        if (!activeBossBars.containsKey(player.getUniqueId())) {
            return;
        }
        
        String type = bossBarTypes.get(player.getUniqueId());
        if (type == null || !type.equals("wanted")) {
            return;
        }
        
        try {
            BossBar bossBar = activeBossBars.get(player.getUniqueId());
            
            
            bossBar.name(plugin.getMessageManager().getMessage(player, "bossbar.wanted.active",
                numberPlaceholder("level", wantedLevel),
                starsPlaceholder("stars", wantedLevel),
                numberPlaceholder("minutes", Math.max(0, remainingTime / 60))));
            
            
            long totalTime = plugin.getConfigManager().getWantedDuration();
            float progress = Math.max(0.0f, Math.min(1.0f, (float) remainingTime / totalTime));
            bossBar.progress(progress);
            
        } catch (Exception e) {
            logger.warning("Error updating wanted boss bar for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    
    public void updateJailCountdown(Player player, int remaining, int total) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        if (!activeBossBars.containsKey(player.getUniqueId())) {
            return;
        }
        
        String type = bossBarTypes.get(player.getUniqueId());
        if (type == null || !type.equals("jail")) {
            return;
        }
        
        try {
            BossBar bossBar = activeBossBars.get(player.getUniqueId());
            if (bossBar == null) {
                return;
            }
            
            
            bossBar.name(plugin.getMessageManager().getMessage(player, "bossbar.jail-countdown",
                numberPlaceholder("seconds", remaining)));
            
            
            float progress = Math.max(0.0f, Math.min(1.0f, (float) remaining / total));
            bossBar.progress(progress);
            
        } catch (Exception e) {
            logger.warning("Error updating jail countdown boss bar for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    
    public void updateDutyTransition(Player player, int remaining, int total) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        if (!activeBossBars.containsKey(player.getUniqueId())) {
            return;
        }
        
        String type = bossBarTypes.get(player.getUniqueId());
        if (type == null || !type.equals("duty")) {
            return;
        }
        
        try {
            BossBar bossBar = activeBossBars.get(player.getUniqueId());
            if (bossBar == null) {
                return;
            }
            
            
            bossBar.name(plugin.getMessageManager().getMessage(player, "bossbar.duty-transition",
                numberPlaceholder("seconds", remaining)));
            
            
            float progress = Math.max(0.0f, Math.min(1.0f, (float) remaining / total));
            bossBar.progress(progress);
            
        } catch (Exception e) {
            logger.warning("Error updating duty transition boss bar for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    
    public void updateContrabandCountdown(Player player, int remaining, int total, String description) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        if (!activeBossBars.containsKey(player.getUniqueId())) {
            return;
        }
        
        String type = bossBarTypes.get(player.getUniqueId());
        if (type == null || !type.equals("contraband")) {
            return;
        }
        
        try {
            BossBar bossBar = activeBossBars.get(player.getUniqueId());
            if (bossBar == null) {
                return;
            }
            
            
            bossBar.name(plugin.getMessageManager().getMessage(player, "bossbar.contraband-countdown",
                numberPlaceholder("seconds", remaining),
                stringPlaceholder("type", description)));
            
            
            float progress = Math.max(0.0f, Math.min(1.0f, (float) remaining / total));
            bossBar.progress(progress);
            
        } catch (Exception e) {
            logger.warning("Error updating contraband countdown boss bar for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    
    public void updateGracePeriodCountdown(Player player, int remaining, int total) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        if (!activeBossBars.containsKey(player.getUniqueId())) {
            return;
        }
        
        String type = bossBarTypes.get(player.getUniqueId());
        if (type == null || !type.equals("grace")) {
            return;
        }
        
        try {
            BossBar bossBar = activeBossBars.get(player.getUniqueId());
            if (bossBar == null) {
                return;
            }
            
            
            bossBar.name(plugin.getMessageManager().getMessage(player, "bossbar.grace-period",
                numberPlaceholder("seconds", remaining)));
            
            
            float progress = Math.max(0.0f, Math.min(1.0f, (float) remaining / total));
            bossBar.progress(progress);
            
        } catch (Exception e) {
            logger.warning("Error updating grace period countdown boss bar for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    
    public void hideBossBar(Player player) {
        UUID playerId = player.getUniqueId();
        
        
        BukkitTask task = bossBarTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        
        
        BossBar bossBar = activeBossBars.remove(playerId);
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
        
        
        bossBarTypes.remove(playerId);
    }
    
    
    public void hideBossBarByType(Player player, String type) {
        String currentType = bossBarTypes.get(player.getUniqueId());
        if (currentType != null && currentType.equals(type)) {
            hideBossBar(player);
        }
    }
    
    
    public boolean hasBossBar(Player player) {
        return activeBossBars.containsKey(player.getUniqueId());
    }
    
    
    public String getBossBarType(Player player) {
        return bossBarTypes.get(player.getUniqueId());
    }
    
    
    
    private BossBar createBossBar(String messageKey, BossBar.Color color, BossBar.Overlay overlay, 
                                 Player player, TagResolver... placeholders) {
        return BossBar.bossBar(
            plugin.getMessageManager().getMessage(player, messageKey, placeholders),
            1.0f,
            color,
            overlay
        );
    }
    
    private void showBossBar(Player player, BossBar bossBar, String type) {
        UUID playerId = player.getUniqueId();
        
        
        activeBossBars.put(playerId, bossBar);
        bossBarTypes.put(playerId, type);
        
        
        player.showBossBar(bossBar);
        
        if (plugin.getConfigManager().isDebugMode()) {
            logger.info("DEBUG: Showed " + type + " boss bar to " + player.getName());
        }
    }
    
    private void startCountdownBossBar(Player player, BossBar bossBar, int duration, String type) {
        UUID playerId = player.getUniqueId();
        
        
        BukkitTask existingTask = bossBarTasks.remove(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        
        BukkitTask task = new BukkitRunnable() {
            private int remaining = duration;
            
            @Override
            public void run() {
                if (!player.isOnline()) {
                    hideBossBar(player);
                    return;
                }
                
                if (remaining <= 0) {
                    hideBossBar(player);
                    return;
                }
                
                try {
                    
                    float progress = Math.max(0.0f, Math.min(1.0f, (float) remaining / duration));
                    bossBar.progress(progress);
                    
                    remaining--;
                    
                } catch (Exception e) {
                    logger.warning("Error updating countdown boss bar for " + player.getName() + ": " + e.getMessage());
                    hideBossBar(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        
        bossBarTasks.put(playerId, task);
    }
    
    private void startWantedBossBarUpdate(Player player, BossBar bossBar, int wantedLevel, long remainingTime) {
        UUID playerId = player.getUniqueId();
        
        
        BukkitTask existingTask = bossBarTasks.remove(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        
        BukkitTask task = new BukkitRunnable() {
            private long remaining = remainingTime;
            
            @Override
            public void run() {
                if (!player.isOnline()) {
                    hideBossBar(player);
                    return;
                }
                
                if (remaining <= 0) {
                    hideBossBar(player);
                    return;
                }
                
                try {
                    
                    updateWantedBossBar(player, wantedLevel, remaining);
                    
                    remaining--;
                    
                } catch (Exception e) {
                    logger.warning("Error updating wanted boss bar for " + player.getName() + ": " + e.getMessage());
                    hideBossBar(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        
        bossBarTasks.put(playerId, task);
    }
    
    private BossBar.Color getBossBarColor(String colorName) {
        try {
            return BossBar.Color.valueOf(colorName.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid boss bar color: " + colorName + ", using RED");
            return BossBar.Color.RED;
        }
    }
    
    private BossBar.Overlay getBossBarOverlay(String overlayName) {
        try {
            return BossBar.Overlay.valueOf(overlayName.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid boss bar overlay: " + overlayName + ", using PROGRESS");
            return BossBar.Overlay.PROGRESS;
        }
    }
    
    
    public void cleanup() {
        LoggingUtils.info(logger, "BossBarManager cleaning up...");
        
        try {
            
            for (Map.Entry<UUID, BossBar> entry : activeBossBars.entrySet()) {
                try {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null && player.isOnline()) {
                        player.hideBossBar(entry.getValue());
                    }
                } catch (Exception e) {
                    LoggingUtils.warn(logger, "Error hiding boss bar for player " + entry.getKey() + ": " + e.getMessage());
                }
            }
            
            
            for (BukkitTask task : bossBarTasks.values()) {
                try {
                    if (task != null && !task.isCancelled()) {
                        task.cancel();
                    }
                } catch (Exception e) {
                    LoggingUtils.warn(logger, "Error cancelling boss bar task: " + e.getMessage());
                }
            }
            
            
            activeBossBars.clear();
            bossBarTasks.clear();
            bossBarTypes.clear();
            
            LoggingUtils.info(logger, "BossBarManager cleanup complete");
        } catch (Exception e) {
            LoggingUtils.error(logger, "Error during BossBarManager cleanup: " + e.getMessage());
        }
    }
    
    
    public void cleanupPlayer(Player player) {
        hideBossBar(player);
    }
}