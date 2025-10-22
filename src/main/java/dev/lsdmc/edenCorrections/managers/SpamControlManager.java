package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;


public class SpamControlManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    
    private final Map<UUID, Map<String, Long>> commandCooldowns;
    private final Map<UUID, Integer> commandSpamCount;
    
    
    private final Map<UUID, Long> lastMessageTime;
    private final Map<UUID, Integer> messageSpamCount;
    
    
    private int commandCooldownTime = 1000; 
    private int messageCooldownTime = 500;  
    private int maxSpamCount = 5;
    private int spamTimeout = 10000; 
    
    public SpamControlManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.commandCooldowns = new ConcurrentHashMap<>();
        this.commandSpamCount = new ConcurrentHashMap<>();
        this.lastMessageTime = new ConcurrentHashMap<>();
        this.messageSpamCount = new ConcurrentHashMap<>();
    }
    
    public void initialize() {
        
        loadConfiguration();
        
        
        startCleanupTask();
        
        logger.info("SpamControlManager initialized");
    }
    
    private void loadConfiguration() {
        
        commandCooldownTime = (Integer) plugin.getConfigManager().getConfigValue("spam-control.command-cooldown", 1000);
        messageCooldownTime = (Integer) plugin.getConfigManager().getConfigValue("spam-control.message-cooldown", 500);
        maxSpamCount = (Integer) plugin.getConfigManager().getConfigValue("spam-control.max-spam-count", 5);
        spamTimeout = (Integer) plugin.getConfigManager().getConfigValue("spam-control.spam-timeout", 10000);
    }
    
    private void startCleanupTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            
            
            commandCooldowns.entrySet().removeIf(entry -> {
                entry.getValue().entrySet().removeIf(cmdEntry -> 
                    currentTime - cmdEntry.getValue() > commandCooldownTime);
                return entry.getValue().isEmpty();
            });
            
            
            commandSpamCount.entrySet().removeIf(entry -> 
                currentTime - entry.getValue() > spamTimeout);
            
            messageSpamCount.entrySet().removeIf(entry -> 
                currentTime - entry.getValue() > spamTimeout);
            
        }, 20L * 60L, 20L * 60L); 
    }
    
    
    public boolean canExecuteCommand(Player player, String command) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        
        Map<String, Long> playerCooldowns = commandCooldowns.computeIfAbsent(playerId, k -> new HashMap<>());
        
        
        Long lastExecution = playerCooldowns.get(command);
        if (lastExecution != null && currentTime - lastExecution < commandCooldownTime) {
            
            int spamCount = commandSpamCount.getOrDefault(playerId, 0) + 1;
            commandSpamCount.put(playerId, spamCount);
            
            if (spamCount >= maxSpamCount) {
                plugin.getMessageManager().sendMessage(player, "spam-control.command-spam-warning");
                return false;
            }
            
            
            
        }
        
        
        commandSpamCount.remove(playerId);
        
        
        playerCooldowns.put(command, currentTime);
        return true;
    }
    
    
    public boolean canSendMessage(Player player) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        
        Long lastMessage = lastMessageTime.get(playerId);
        if (lastMessage != null && currentTime - lastMessage < messageCooldownTime) {
            
            int spamCount = messageSpamCount.getOrDefault(playerId, 0) + 1;
            messageSpamCount.put(playerId, spamCount);
            
            if (spamCount >= maxSpamCount) {
                plugin.getMessageManager().sendMessage(player, "spam-control.message-spam-warning");
                return false;
            }
            
            
            
        }
        
        
        messageSpamCount.remove(playerId);
        
        
        lastMessageTime.put(playerId, currentTime);
        return true;
    }
    
    
    public void forceAllowCommand(Player player, String command) {
        UUID playerId = player.getUniqueId();
        Map<String, Long> playerCooldowns = commandCooldowns.get(playerId);
        if (playerCooldowns != null) {
            playerCooldowns.remove(command);
        }
        commandSpamCount.remove(playerId);
    }
    
    
    public void forceAllowMessage(Player player) {
        UUID playerId = player.getUniqueId();
        lastMessageTime.remove(playerId);
        messageSpamCount.remove(playerId);
    }
    
    
    public Map<String, Object> getPlayerSpamStats(Player player) {
        UUID playerId = player.getUniqueId();
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("commandSpamCount", commandSpamCount.getOrDefault(playerId, 0));
        stats.put("messageSpamCount", messageSpamCount.getOrDefault(playerId, 0));
        stats.put("activeCooldowns", commandCooldowns.getOrDefault(playerId, new HashMap<>()).size());
        
        return stats;
    }
    
    
    public void resetPlayerSpamData(Player player) {
        UUID playerId = player.getUniqueId();
        commandCooldowns.remove(playerId);
        commandSpamCount.remove(playerId);
        lastMessageTime.remove(playerId);
        messageSpamCount.remove(playerId);
    }
    
    
    public void cleanup() {
        commandCooldowns.clear();
        commandSpamCount.clear();
        lastMessageTime.clear();
        messageSpamCount.clear();
    }
    
    
    public void cleanupPlayer(Player player) {
        resetPlayerSpamData(player);
    }
} 