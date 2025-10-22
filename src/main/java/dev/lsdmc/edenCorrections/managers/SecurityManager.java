package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.logging.Logger;

import static dev.lsdmc.edenCorrections.managers.MessageManager.*;


public class SecurityManager implements Listener {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    public SecurityManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    public void initialize() {
        logger.info("SecurityManager initialized successfully!");
        
        
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    
    public boolean isPlayerProtected(Player player) {
        if (!plugin.getConfigManager().isGuardImmunityEnabled()) {
            return false;
        }
        
        return plugin.getDutyManager().isOnDuty(player);
    }
    
    
    public boolean canPlayerBeWanted(Player player) {
        if (!plugin.getConfigManager().isGuardWantedProtected()) {
            return true;
        }
        
        return !isPlayerProtected(player);
    }
    
    
    public boolean canPlayerBeChased(Player player) {
        if (!plugin.getConfigManager().isGuardChaseProtected()) {
            return true;
        }
        
        return !isPlayerProtected(player);
    }
    
    
    public boolean canPlayerBeContrabandTargeted(Player player) {
        if (!plugin.getConfigManager().isGuardContrabandProtected()) {
            return true;
        }
        
        return !isPlayerProtected(player);
    }
    
    
    public boolean canPlayerBeJailed(Player player) {
        if (!plugin.getConfigManager().isGuardJailProtected()) {
            return true;
        }
        
        return !isPlayerProtected(player);
    }
    
    
    public boolean canPlayerBeAttacked(Player player) {
        if (!plugin.getConfigManager().isGuardCombatProtected()) {
            return true;
        }
        
        return !isPlayerProtected(player);
    }
    
    
    public boolean canGuardAttackGuard(Player attacker, Player victim) {
        if (!plugin.getConfigManager().isGuardToGuardProtectionEnabled()) {
            return true;
        }
        
        
        boolean attackerOnDuty = plugin.getDutyManager().isOnDuty(attacker);
        boolean victimOnDuty = plugin.getDutyManager().isOnDuty(victim);
        
        
        if (attackerOnDuty && victimOnDuty) {
            return false;
        }
        
        return true;
    }
    
    
    public boolean canPlayerBeTeleported(Player player) {
        if (!plugin.getConfigManager().isGuardTeleportProtected()) {
            return true;
        }
        
        return !isPlayerProtected(player);
    }
    
    
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        
        
        try {
            if (plugin.getJailManager() != null && plugin.getJailManager().hasJailTeleportBypass(player)) {
                return;
            }
        } catch (Throwable ignored) {}

        
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN || 
            event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND) {
            
            if (!canPlayerBeTeleported(player)) {
                event.setCancelled(true);
                
                
                plugin.getMessageManager().sendMessage(player, "security.guard-immunity.teleport-blocked");
                
                if (plugin.getConfigManager().isDebugMode()) {
                    logger.info("DEBUG: Teleport blocked for protected guard " + player.getName());
                }
            }
        }
    }
    
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();
        
        
        if (command.startsWith("/wanted") || command.startsWith("/chase") || 
            command.startsWith("/jail") || command.startsWith("/contraband")) {
            
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Command executed by " + player.getName() + ": " + command);
            }
        }
    }
    
    
    public String getPlayerSecurityStatus(Player player) {
        if (!isPlayerProtected(player)) {
            return "UNPROTECTED";
        }
        
        StringBuilder status = new StringBuilder("PROTECTED (");
        boolean first = true;
        
        if (plugin.getConfigManager().isGuardWantedProtected()) {
            if (!first) status.append(", ");
            status.append("WANTED");
            first = false;
        }
        
        if (plugin.getConfigManager().isGuardChaseProtected()) {
            if (!first) status.append(", ");
            status.append("CHASE");
            first = false;
        }
        
        if (plugin.getConfigManager().isGuardContrabandProtected()) {
            if (!first) status.append(", ");
            status.append("CONTRABAND");
            first = false;
        }
        
        if (plugin.getConfigManager().isGuardJailProtected()) {
            if (!first) status.append(", ");
            status.append("JAIL");
            first = false;
        }
        
        if (plugin.getConfigManager().isGuardCombatProtected()) {
            if (!first) status.append(", ");
            status.append("COMBAT");
            first = false;
        }
        
        if (plugin.getConfigManager().isGuardTeleportProtected()) {
            if (!first) status.append(", ");
            status.append("TELEPORT");
        }
        
        status.append(")");
        return status.toString();
    }
    
    
    public void logSecurityViolation(String action, Player perpetrator, Player target) {
        try {
            String targetName = target != null ? target.getName() : "unknown";
            String perpName = perpetrator != null ? perpetrator.getName() : "unknown";
            logger.warning("SECURITY VIOLATION: " + perpName + " attempted " + action + " on protected guard " + targetName);
        } catch (Exception e) {
            logger.warning("SECURITY VIOLATION (logging error): attempted " + action);
        }
        
        
        plugin.getServer().getOnlinePlayers().stream()
            .filter(player -> player.hasPermission("edencorrections.admin"))
            .forEach(admin -> {
                try {
                    plugin.getMessageManager().sendMessage(admin, "security.violation-alert",
                        stringPlaceholder("action", action),
                        perpetrator != null ? playerPlaceholder("perpetrator", perpetrator) : stringPlaceholder("perpetrator", "unknown"),
                        target != null ? playerPlaceholder("target", target) : stringPlaceholder("target", "unknown"));
                } catch (Exception ignore) {}
            });
    }
    
    
    public void cleanup() {
        
    }
} 