package dev.lsdmc.edenCorrections.integrations;

import dev.lsdmc.edenCorrections.EdenCorrections;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;


public class CMIIntegration {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    private final boolean cmiAvailable;
    
    public CMIIntegration(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.cmiAvailable = checkCMIAvailability();
        
        if (cmiAvailable) {
            logger.info("CMI integration initialized successfully");
        } else {
            logger.warning("CMI not available - integration features disabled");
        }
    }
    
    
    private boolean checkCMIAvailability() {
        Plugin cmi = Bukkit.getPluginManager().getPlugin("CMI");
        return cmi != null && cmi.isEnabled();
    }
    
    
    private final Map<UUID, PermissionAttachment> cachedAttachments = new ConcurrentHashMap<>();
    
    
    private CompletableFuture<Boolean> executePlayerCommand(Player executor, String command) {
        return CompletableFuture.supplyAsync(() -> {
            if (!cmiAvailable) {
                logger.warning("Cannot execute CMI command - CMI not available");
                return false;
            }
            
            if (executor == null || !executor.isOnline()) {
                logger.warning("Cannot execute CMI command - executor not available");
                return false;
            }
            
            try {
                
                getOrCreatePermissionAttachment(executor);
                
                
                return Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                    try {
                        return Bukkit.dispatchCommand(executor, command);
                    } catch (Exception e) {
                        logger.warning("CMI command execution failed: " + e.getMessage());
                        return false;
                    }
                }).get();
                
            } catch (Exception e) {
                logger.severe("Error executing CMI command '" + command + "': " + e.getMessage());
                return false;
            }
            
            
        });
    }
    
    
    private PermissionAttachment getOrCreatePermissionAttachment(Player executor) {
        UUID playerId = executor.getUniqueId();
        PermissionAttachment attachment = cachedAttachments.get(playerId);
        
        if (attachment == null) {
            attachment = executor.addAttachment(plugin);
            
            attachment.setPermission("cmi.command.jail", true);
            attachment.setPermission("cmi.command.kit", true);
            attachment.setPermission("cmi.kit.*", true);
            attachment.setPermission("cmi.jail.*", true);
            cachedAttachments.put(playerId, attachment);
        }
        
        return attachment;
    }
    
    
    public void cleanupPlayerAttachments(Player player) {
        UUID playerId = player.getUniqueId();
        PermissionAttachment attachment = cachedAttachments.remove(playerId);
        if (attachment != null) {
            try {
                player.removeAttachment(attachment);
            } catch (Exception e) {
                logger.warning("Failed to remove cached permission attachment for " + player.getName() + ": " + e.getMessage());
            }
        }
    }
    
    
    public CompletableFuture<Boolean> jailPlayer(Player executor, Player target, int jailTime, String reason) {
        if (!cmiAvailable) {
            logger.warning("Cannot jail player - CMI not available");
            return CompletableFuture.completedFuture(false);
        }
        
        if (target == null) {
            logger.warning("Cannot jail player - target is null");
            return CompletableFuture.completedFuture(false);
        }
        
        
        int jailMinutes = Math.max(1, jailTime / 60);
        
        
        String jailCommand = String.format("cmi jail %s %dm %s", 
            target.getName(), 
            jailMinutes, 
            reason != null ? reason : "Arrested by corrections officer"
        );
        
        logger.info("Attempting to jail " + target.getName() + " for " + jailMinutes + " minutes");
        
        return executePlayerCommand(executor, jailCommand).thenApply(success -> {
            if (success) {
                logger.info("Successfully jailed " + target.getName() + " for " + jailMinutes + " minutes");
            } else {
                logger.warning("Failed to jail " + target.getName());
            }
            return success;
        });
    }
    
    
    public CompletableFuture<Boolean> jailOfflinePlayer(Player executor, String targetName, int jailTime, String reason) {
        if (!cmiAvailable) {
            logger.warning("Cannot jail offline player - CMI not available");
            return CompletableFuture.completedFuture(false);
        }
        
        if (targetName == null || targetName.trim().isEmpty()) {
            logger.warning("Cannot jail player - invalid target name");
            return CompletableFuture.completedFuture(false);
        }
        
        
        int jailMinutes = Math.max(1, jailTime / 60);
        
        
        String jailCommand = String.format("cmi jail %s %dm %s", 
            targetName, 
            jailMinutes, 
            reason != null ? reason : "Arrested by corrections officer"
        );
        
        logger.info("Attempting to jail offline player " + targetName + " for " + jailMinutes + " minutes");
        
        return executePlayerCommand(executor, jailCommand).thenApply(success -> {
            if (success) {
                logger.info("Successfully jailed offline player " + targetName + " for " + jailMinutes + " minutes");
            } else {
                logger.warning("Failed to jail offline player " + targetName);
            }
            return success;
        });
    }
    
    
    public CompletableFuture<Boolean> giveKit(Player executor, Player target, String kitName) {
        if (!cmiAvailable) {
            logger.warning("Cannot give kit - CMI not available");
            return CompletableFuture.completedFuture(false);
        }
        
        if (target == null || kitName == null || kitName.trim().isEmpty()) {
            logger.warning("Cannot give kit - invalid parameters");
            return CompletableFuture.completedFuture(false);
        }
        
        
        String kitCommand = String.format("cmi kit %s %s", kitName, target.getName());
        
        logger.info("Attempting to give kit " + kitName + " to " + target.getName());
        
        return executePlayerCommand(executor, kitCommand).thenApply(success -> {
            if (success) {
                logger.info("Successfully gave kit " + kitName + " to " + target.getName());
            } else {
                logger.warning("Failed to give kit " + kitName + " to " + target.getName());
            }
            return success;
        });
    }
    
    
    public CompletableFuture<Boolean> giveKit(Player target, String kitName) {
        return giveKit(target, target, kitName);
    }
    
    
    public boolean isAvailable() {
        return cmiAvailable;
    }
    
    
    public boolean testIntegration() {
        try {
            Plugin cmi = Bukkit.getPluginManager().getPlugin("CMI");
            if (cmi == null) {
                return false;
            }
            
            
            return cmi.isEnabled() && cmi.getDescription() != null;
            
        } catch (Exception e) {
            logger.warning("CMI integration test failed: " + e.getMessage());
            return false;
        }
    }
    
    
    public String getDiagnosticInfo() {
        StringBuilder info = new StringBuilder();
        info.append("CMI Integration Status:\n");
        info.append("  Available: ").append(cmiAvailable).append("\n");
        info.append("  Cached Attachments: ").append(cachedAttachments.size()).append("\n");
        
        if (cmiAvailable) {
            Plugin cmi = Bukkit.getPluginManager().getPlugin("CMI");
            info.append("  Version: ").append(cmi.getDescription().getVersion()).append("\n");
            info.append("  Enabled: ").append(cmi.isEnabled()).append("\n");
        } else {
            info.append("  CMI plugin not found or disabled\n");
        }
        
        return info.toString();
    }
    
    
    public void cleanup() {
        for (Map.Entry<UUID, PermissionAttachment> entry : cachedAttachments.entrySet()) {
            try {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    player.removeAttachment(entry.getValue());
                }
            } catch (Exception e) {
                logger.warning("Failed to cleanup permission attachment for player " + entry.getKey() + ": " + e.getMessage());
            }
        }
        cachedAttachments.clear();
    }
}