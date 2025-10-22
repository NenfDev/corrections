package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;


import me.clip.placeholderapi.PlaceholderAPI;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;

public class MessageManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    private final MiniMessage miniMessage;
    
    
    private final Map<String, String> messageCache;
    
    
    private final Map<UUID, BossBar> activeBossBars;
    
    
    private final Map<UUID, BukkitTask> bossBarTasks;
    
    
    private final Map<UUID, BukkitTask> actionBarTasks;
    
    
    private boolean placeholderAPIEnabled;
    
    
    private static final Pattern INTERNAL_PLACEHOLDER = Pattern.compile("<([^>]+)>");
    private static final Pattern EXTERNAL_PLACEHOLDER = Pattern.compile("%([^%]+)%");
    private static final Pattern LEGACY_PLACEHOLDER = Pattern.compile("\\{([^}]+)\\}");
    
    
    private static final String PRIMARY_COLOR = "#9D4EDD";      
    private static final String SECONDARY_COLOR = "#06FFA5";    
    private static final String ACCENT_COLOR = "#FFB3C6";       
    private static final String ERROR_COLOR = "#FFA94D";        
    private static final String SUCCESS_COLOR = "#51CF66";      
    private static final String NEUTRAL_COLOR = "#ADB5BD";      
    private static final String WARNING_COLOR = "#FFE066";      
    private static final String INFO_COLOR = "#74C0FC";         
    
    
    private final Map<String, Integer> messageUsageCount;
    private final List<String> missingMessages;
    private final List<String> invalidMessages;
    
    public MessageManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.miniMessage = MiniMessage.miniMessage();
        this.messageCache = new ConcurrentHashMap<>();
        this.activeBossBars = new ConcurrentHashMap<>();
        this.bossBarTasks = new ConcurrentHashMap<>();
        this.actionBarTasks = new ConcurrentHashMap<>();
        this.placeholderAPIEnabled = false;
        this.messageUsageCount = new ConcurrentHashMap<>();
        this.missingMessages = new ArrayList<>();
        this.invalidMessages = new ArrayList<>();
    }
    
    public void initialize() {
        logger.info("MessageManager initializing...");
        
        
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            if (plugin.getConfigManager().isPlaceholderAPIEnabled()) {
                placeholderAPIEnabled = true;
                logger.info("PlaceholderAPI integration enabled!");
            } else {
                logger.info("PlaceholderAPI found but disabled in config");
            }
        } else {
            logger.info("PlaceholderAPI not found - external placeholder support disabled");
        }
        
        
        loadMessages();
        validateMessages();
        
        
        testLegacyPlaceholderConversion();
        
        logger.info("MessageManager initialized successfully with MiniMessage support!");
    }
    
    public void reload() {
        logger.info("Reloading MessageManager...");
        
        
        messageCache.clear();
        messageUsageCount.clear();
        missingMessages.clear();
        invalidMessages.clear();
        
        
        loadMessages();
        validateMessages();
        
        
        logMissingMessagesSummary();
        
        logger.info("MessageManager reloaded successfully!");
    }
    
    public void forceReload() {
        logger.info("Force reloading MessageManager...");
        
        
        messageCache.clear();
        messageUsageCount.clear();
        missingMessages.clear();
        invalidMessages.clear();
        
        
        if (plugin.getConfigManager() != null) {
            plugin.getConfigManager().reload();
        }
        
        
        loadMessages();
        validateMessages();
        
        
        logMissingMessagesSummary();
        
        
        generateDiagnosticReport();
        
        logger.info("MessageManager force reloaded successfully!");
    }
    
    private void loadMessages() {
        try {
            logger.info("MessageManager: Starting to load messages...");
            
            
            if (plugin.getConfigManager() == null) {
                logger.severe("MessageManager: ConfigManager is null!");
                return;
            }
            
            if (plugin.getConfigManager().getConfig() == null) {
                logger.severe("MessageManager: ConfigManager config is null!");
                return;
            }
            
            logger.info("MessageManager: Config available, proceeding with loading...");
            logger.info("MessageManager: Available config sections: " + plugin.getConfigManager().getConfig().getKeys(false));
            
            
            String prefix = plugin.getConfigManager().getConfig().getString("prefix");
            if (prefix != null) {
                messageCache.put("prefix", prefix);
                logger.info("MessageManager: Loaded prefix: " + prefix);
            } else {
                logger.warning("MessageManager: No prefix found in config!");
            }
            
            
            ConfigurationSection messages = plugin.getConfigManager().getConfig().getConfigurationSection("messages");
            if (messages != null) {
                logger.info("MessageManager: Found messages section, loading messages...");
                logger.info("MessageManager: Messages section keys: " + messages.getKeys(false));
                loadMessageSection(messages, "");
                logger.info("MessageManager: Loaded " + messageCache.size() + " messages from configuration");
                
                
                logger.info("MessageManager: Sample loaded messages:");
                int count = 0;
                for (String key : messageCache.keySet()) {
                    if (count < 10) {
                        logger.info("  " + key + ": " + messageCache.get(key));
                        count++;
                    } else {
                        break;
                    }
                }
            } else {
                logger.severe("MessageManager: No messages section found in configuration!");
                logger.severe("MessageManager: Available config sections: " + plugin.getConfigManager().getConfig().getKeys(false));
                
                
                logger.info("MessageManager: Debug - All config keys:");
                for (String key : plugin.getConfigManager().getConfig().getKeys(true)) {
                    logger.info("  " + key + " = " + plugin.getConfigManager().getConfig().get(key));
                }
            }
        } catch (Exception e) {
            logger.severe("MessageManager: Error loading messages: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadMessageSection(ConfigurationSection section, String prefix) {
        for (String key : section.getKeys(false)) {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            
            if (section.isConfigurationSection(key)) {
                loadMessageSection(section.getConfigurationSection(key), fullKey);
            } else if (section.isString(key)) {
                String message = section.getString(key);
                if (message != null) {
                    messageCache.put(fullKey, message);
                    
                    
                    if (LEGACY_PLACEHOLDER.matcher(message).find()) {
                        logger.warning("Message '" + fullKey + "' uses legacy {placeholder} format. Consider updating to <placeholder> format.");
                    }
                }
            }
        }
    }
    
    
    
    public void sendMessage(Player player, String messageKey, TagResolver... placeholders) {
        if (player == null) {
            logger.warning("Attempted to send message to null player: " + messageKey);
            return;
        }
        
        Component message = getMessage(player, messageKey, placeholders);
        if (message != null) {
            player.sendMessage(getPrefix().append(message));
            trackMessageUsage(messageKey);
        }
    }
    
    public void sendMessage(CommandSender sender, String messageKey, TagResolver... placeholders) {
        if (sender == null) {
            logger.warning("Attempted to send message to null sender: " + messageKey);
            return;
        }
        
        Player player = sender instanceof Player ? (Player) sender : null;
        Component message = getMessage(player, messageKey, placeholders);
        if (message != null) {
            if (sender instanceof ConsoleCommandSender) {
                
                String plainMessage = PlainTextComponentSerializer.plainText().serialize(getPrefix().append(message));
                sender.sendMessage(plainMessage);
            } else {
                sender.sendMessage(getPrefix().append(message));
            }
            trackMessageUsage(messageKey);
        }
    }
    
    public void sendMessage(Audience audience, String messageKey, TagResolver... placeholders) {
        if (audience == null) {
            logger.warning("Attempted to send message to null audience: " + messageKey);
            return;
        }
        
        Component message = getMessage(null, messageKey, placeholders);
        if (message != null) {
            audience.sendMessage(getPrefix().append(message));
            trackMessageUsage(messageKey);
        }
    }
    
    public void sendRawMessage(Player player, String messageKey, TagResolver... placeholders) {
        if (player == null) {
            logger.warning("Attempted to send raw message to null player: " + messageKey);
            return;
        }
        
        Component message = getMessage(player, messageKey, placeholders);
        if (message != null) {
            player.sendMessage(message);
            trackMessageUsage(messageKey);
        }
    }
    
    public void sendRawString(Player player, String rawMessage) {
        if (player == null) {
            logger.warning("Attempted to send raw string to null player");
            return;
        }
        
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            return;
        }
        
        try {
            Component message = miniMessage.deserialize(rawMessage);
            player.sendMessage(message);
        } catch (Exception e) {
            logger.warning("Error parsing raw message: " + e.getMessage());
            logger.warning("Raw message was: " + rawMessage);
            
            player.sendMessage(rawMessage);
        }
    }
    
    public void sendGuardAlert(String messageKey, TagResolver... placeholders) {
        try {
            Component alertMessage = getMessage(null, "system.guard-alert", 
                Placeholder.component("message", getMessage(null, messageKey, placeholders)));
        
        if (alertMessage != null) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (plugin.getDutyManager().isSubjectToGuardRestrictions(player) && 
                    plugin.getDutyManager().isOnDuty(player)) {
                    player.sendMessage(alertMessage);
                }
            }
                trackMessageUsage(messageKey);
            }
        } catch (Exception e) {
            logger.severe("Error sending guard alert: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    
    public void showBossBar(Player player, String messageKey, BossBar.Color color, BossBar.Overlay overlay, TagResolver... placeholders) {
        if (player == null) {
            logger.warning("Attempted to show boss bar to null player: " + messageKey);
            return;
        }
        
        try {
            Component title = getMessage(player, messageKey, placeholders);
            if (title == null) {
                logger.warning("Failed to get boss bar title for message: " + messageKey);
                return;
            }
        
        
        hideBossBar(player);
        
        BossBar bossBar = BossBar.bossBar(title, 1.0f, color, overlay);
        activeBossBars.put(player.getUniqueId(), bossBar);
        player.showBossBar(bossBar);
            
            trackMessageUsage(messageKey);
        } catch (Exception e) {
            logger.severe("Error showing boss bar to " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void showTimedBossBar(Player player, String messageKey, BossBar.Color color, BossBar.Overlay overlay, int durationSeconds, TagResolver... placeholders) {
        if (player == null) {
            logger.warning("Attempted to show timed boss bar to null player: " + messageKey);
            return;
        }
        
        try {
            showBossBar(player, messageKey, color, overlay, placeholders);
            
            
            BukkitTask existingTask = bossBarTasks.remove(player.getUniqueId());
            if (existingTask != null) {
                existingTask.cancel();
            }
            
            
            BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                hideBossBar(player);
            }, durationSeconds * 20L);
            
            bossBarTasks.put(player.getUniqueId(), task);
        } catch (Exception e) {
            logger.severe("Error showing timed boss bar to " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void showCountdownBossBar(Player player, String messageKey, BossBar.Color color, BossBar.Overlay overlay, int durationSeconds, TagResolver... staticPlaceholders) {
        if (player == null) {
            logger.warning("Attempted to show countdown boss bar to null player: " + messageKey);
            return;
        }
        
        try {
            
            Component initialTitle = getMessage(player, messageKey, combineTagResolvers(staticPlaceholders, timePlaceholder("time", durationSeconds)));
            if (initialTitle == null) {
                logger.warning("Failed to get countdown boss bar title for message: " + messageKey);
                return;
            }
            
            
            hideBossBar(player);
            
            
            BossBar bossBar = BossBar.bossBar(initialTitle, 1.0f, color, overlay);
            activeBossBars.put(player.getUniqueId(), bossBar);
            player.showBossBar(bossBar);
            
            
            BukkitTask existingTask = bossBarTasks.remove(player.getUniqueId());
            if (existingTask != null) {
                existingTask.cancel();
            }
            
            
            BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
                private int remaining = durationSeconds;
                
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
                        
                        TagResolver[] combinedPlaceholders = combineTagResolvers(staticPlaceholders, timePlaceholder("time", remaining));
                        
                        
                        Component title = getMessage(player, messageKey, combinedPlaceholders);
                        if (title != null) {
                            bossBar.name(title);
                        }
                        
                        
                        float progress = Math.max(0.0f, Math.min(1.0f, (float) remaining / durationSeconds));
                        bossBar.progress(progress);
                        
                        remaining--;
                        
                    } catch (Exception e) {
                        logger.warning("Error updating countdown boss bar for " + player.getName() + ": " + e.getMessage());
                        hideBossBar(player);
                    }
                }
            }, 0L, 20L);
            
            bossBarTasks.put(player.getUniqueId(), task);
            trackMessageUsage(messageKey);
        } catch (Exception e) {
            logger.severe("Error showing countdown boss bar to " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void updateBossBar(Player player, String messageKey, TagResolver... placeholders) {
        if (player == null) {
            logger.warning("Attempted to update boss bar for null player: " + messageKey);
            return;
        }
        
        try {
        BossBar bossBar = activeBossBars.get(player.getUniqueId());
        if (bossBar != null) {
                Component title = getMessage(player, messageKey, placeholders);
            if (title != null) {
                bossBar.name(title);
                    trackMessageUsage(messageKey);
                }
            }
        } catch (Exception e) {
            logger.warning("Error updating boss bar for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    public void updateBossBarProgress(Player player, float progress) {
        if (player == null) {
            logger.warning("Attempted to update boss bar progress for null player");
            return;
        }
        
        try {
        BossBar bossBar = activeBossBars.get(player.getUniqueId());
        if (bossBar != null) {
                float clampedProgress = Math.max(0.0f, Math.min(1.0f, progress));
                bossBar.progress(clampedProgress);
            }
        } catch (Exception e) {
            logger.warning("Error updating boss bar progress for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    public void hideBossBar(Player player) {
        if (player == null) {
            logger.warning("Attempted to hide boss bar for null player");
            return;
        }
        
        try {
            UUID playerId = player.getUniqueId();
            
            
            BossBar bossBar = activeBossBars.remove(playerId);
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
            
            
            BukkitTask task = bossBarTasks.remove(playerId);
            if (task != null) {
                task.cancel();
            }
            
            
        } catch (Exception e) {
            logger.warning("Error hiding boss bar for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    
    private TagResolver[] combineTagResolvers(TagResolver[] staticPlaceholders, TagResolver... additionalPlaceholders) {
        TagResolver[] combined = new TagResolver[staticPlaceholders.length + additionalPlaceholders.length];
        System.arraycopy(staticPlaceholders, 0, combined, 0, staticPlaceholders.length);
        System.arraycopy(additionalPlaceholders, 0, combined, staticPlaceholders.length, additionalPlaceholders.length);
        return combined;
    }

    
    
    public void sendActionBar(Player player, String messageKey, TagResolver... placeholders) {
        if (player == null) {
            logger.warning("Attempted to send action bar to null player: " + messageKey);
            return;
        }
        
        try {
            Component message = getMessage(player, messageKey, placeholders);
            if (message != null) {
                player.sendActionBar(message);
                trackMessageUsage(messageKey);
            }
        } catch (Exception e) {
            logger.warning("Error sending action bar to " + player.getName() + ": " + e.getMessage());
        }
    }
    
    public void sendTimedActionBar(Player player, String messageKey, int durationSeconds, TagResolver... placeholders) {
        if (player == null) {
            logger.warning("Attempted to send timed action bar to null player: " + messageKey);
            return;
        }
        
        try {
            Component message = getMessage(player, messageKey, placeholders);
            if (message == null) return;
            
            
            BukkitTask existingTask = actionBarTasks.remove(player.getUniqueId());
            if (existingTask != null) {
                existingTask.cancel();
            }
            
            
            player.sendActionBar(message);
            trackMessageUsage(messageKey);
            
            
            BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
                private int remaining = durationSeconds;
                
                @Override
                public void run() {
                    if (remaining <= 0 || !player.isOnline()) {
                        actionBarTasks.remove(player.getUniqueId());
                        
                        if (player.isOnline()) {
                            player.sendActionBar(Component.empty());
                        }
                        return;
                    }
                    
                    player.sendActionBar(message);
                    remaining--;
                }
            }, 0L, 20L);
            
            actionBarTasks.put(player.getUniqueId(), task);
        } catch (Exception e) {
            logger.warning("Error sending timed action bar to " + player.getName() + ": " + e.getMessage());
        }
    }
    
    public void sendActionBarToGuards(String messageKey, TagResolver... placeholders) {
        try {
            Component message = getMessage(null, messageKey, placeholders);
            if (message == null) return;
            
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (plugin.getDutyManager().isSubjectToGuardRestrictions(player) && 
                    plugin.getDutyManager().isOnDuty(player)) {
                    player.sendActionBar(message);
                }
            }
            trackMessageUsage(messageKey);
        } catch (Exception e) {
            logger.warning("Error sending action bar to guards: " + e.getMessage());
        }
    }
    
    public void sendActionBarToAll(String messageKey, TagResolver... placeholders) {
        try {
            Component message = getMessage(null, messageKey, placeholders);
            if (message == null) return;
            
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                player.sendActionBar(message);
            }
            trackMessageUsage(messageKey);
        } catch (Exception e) {
            logger.warning("Error sending action bar to all players: " + e.getMessage());
        }
    }
    
    public void sendActionBarToAllDuration(String messageKey, int durationSeconds, TagResolver... placeholders) {
        try {
            Component message = getMessage(null, messageKey, placeholders);
            if (message == null) return;
            
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                sendTimedActionBar(player, messageKey, durationSeconds, placeholders);
            }
        } catch (Exception e) {
            logger.warning("Error sending timed action bar to all players: " + e.getMessage());
        }
    }
    
    public void sendMessageToAll(String messageKey, TagResolver... placeholders) {
        try {
            Component message = getMessage(null, messageKey, placeholders);
            if (message == null) return;
            
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                player.sendMessage(getPrefix().append(message));
            }
            trackMessageUsage(messageKey);
        } catch (Exception e) {
            logger.warning("Error sending message to all players: " + e.getMessage());
        }
    }
    
    
    public void sendGuardDutyNotification(String guardName, String guardRank) {
        if (!plugin.getConfigManager().isDutyNotificationsEnabled()) {
            return;
        }
        
        TagResolver[] placeholders = {
            stringPlaceholder("player", guardName),
            stringPlaceholder("rank", guardRank)
        };
        
        String method = plugin.getConfigManager().getDutyNotificationMethod().toLowerCase();
        
        try {
            switch (method) {
                case "chat":
                    sendMessageToAll("system.guard-on-duty", placeholders);
                    break;
                case "actionbar":
                    int duration = plugin.getConfigManager().getDutyNotificationDuration();
                    sendActionBarToAllDuration("system.guard-on-duty", duration, placeholders);
                    break;
                case "both":
                    sendMessageToAll("system.guard-on-duty", placeholders);
                    int bothDuration = plugin.getConfigManager().getDutyNotificationDuration();
                    sendActionBarToAllDuration("system.guard-on-duty", bothDuration, placeholders);
                    break;
                default:
                    logger.warning("Invalid duty notification method: " + method + ". Using actionbar instead.");
                    sendActionBarToAllDuration("system.guard-on-duty", 5, placeholders);
                    break;
            }
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("Sent guard duty notification: " + guardName + " (" + guardRank + ") via " + method);
            }
        } catch (Exception e) {
            logger.warning("Error sending guard duty notification: " + e.getMessage());
        }
    }
    
    public void clearActionBar(Player player) {
        if (player == null) {
            logger.warning("Attempted to clear action bar for null player");
            return;
        }
        
        try {
            BukkitTask task = actionBarTasks.remove(player.getUniqueId());
            if (task != null) {
                task.cancel();
            }
            player.sendActionBar(Component.empty());
        } catch (Exception e) {
            logger.warning("Error clearing action bar for " + player.getName() + ": " + e.getMessage());
        }
    }

    
    
    public void sendTitle(Player player, String titleKey, String subtitleKey, int fadeIn, int stay, int fadeOut, TagResolver... placeholders) {
        if (player == null) {
            logger.warning("Attempted to send title to null player");
            return;
        }
        
        try {
            Component title = titleKey != null ? getMessage(player, titleKey, placeholders) : Component.empty();
            Component subtitle = subtitleKey != null ? getMessage(player, subtitleKey, placeholders) : Component.empty();
            
            Title titleObj = Title.title(
                title,
                subtitle,
                Title.Times.times(
                    Duration.ofMillis(fadeIn * 50L),
                    Duration.ofMillis(stay * 50L),
                    Duration.ofMillis(fadeOut * 50L)
                )
            );
            
            player.showTitle(titleObj);
            
            if (titleKey != null) trackMessageUsage(titleKey);
            if (subtitleKey != null) trackMessageUsage(subtitleKey);
        } catch (Exception e) {
            logger.warning("Error sending title to " + player.getName() + ": " + e.getMessage());
        }
    }

    
    
    public Component getMessage(Player player, String messageKey, TagResolver... placeholders) {
        String rawMessage = getRawMessage(messageKey);
        if (rawMessage == null) {
            
            if (!missingMessages.contains(messageKey)) {
                missingMessages.add(messageKey);
                logger.warning("MessageManager: Message not found: " + messageKey);
            }
            return miniMessage.deserialize("<color:" + ERROR_COLOR + ">Message not found: " + messageKey + "</color>");
        }
        
        
        rawMessage = convertLegacyPlaceholders(rawMessage);
        
        
        if (placeholderAPIEnabled && player != null) {
            rawMessage = parsePlaceholderAPI(player, rawMessage);
        }
        
        try {
            
            Component result = miniMessage.deserialize(rawMessage, TagResolver.resolver(placeholders));
            return result;
        } catch (Exception e) {
            logger.warning("Error parsing message '" + messageKey + "': " + e.getMessage());
            logger.warning("Raw message was: " + rawMessage);
            invalidMessages.add(messageKey + ": " + e.getMessage());
            return miniMessage.deserialize("<color:" + ERROR_COLOR + ">Error parsing message: " + messageKey + "</color>");
        }
    }
    
    
    private String convertLegacyPlaceholders(String message) {
        if (message == null) return null;
        
        
        return LEGACY_PLACEHOLDER.matcher(message).replaceAll("<$1>");
    }
    
    
    public void testLegacyPlaceholderConversion() {
        String testMessage = "Test message with {count} and {player} placeholders";
        String converted = convertLegacyPlaceholders(testMessage);
        logger.info("TEST: Original: " + testMessage);
        logger.info("TEST: Converted: " + converted);
        logger.info("TEST: Pattern matches: " + LEGACY_PLACEHOLDER.matcher(testMessage).find());
    }
    
    private String parsePlaceholderAPI(Player player, String message) {
        try {
            
            return PlaceholderAPI.setPlaceholders(player, message);
        } catch (Exception e) {
            logger.warning("Error parsing PlaceholderAPI placeholders: " + e.getMessage());
            return message;
        }
    }
    
    public String getRawMessage(String messageKey) {
        return messageCache.get(messageKey);
    }
    
    public Component getPrefix() {
        String prefixMessage = getRawMessage("prefix");
        if (prefixMessage != null) {
            try {
            return miniMessage.deserialize(prefixMessage);
            } catch (Exception e) {
                logger.warning("Error parsing prefix: " + e.getMessage());
            }
        }
        return miniMessage.deserialize("<gradient:" + PRIMARY_COLOR + ":" + SECONDARY_COLOR + ">[₠]</gradient> ");
    }
    
    
    
    public String getExternalMessage(String messageKey, Map<String, String> placeholders) {
        String rawMessage = getRawMessage(messageKey);
        if (rawMessage == null) return "Message not found: " + messageKey;
        
        
        String processed = rawMessage;
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                processed = processed.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        
        
        return stripMiniMessageTags(processed);
    }
    
    private String stripMiniMessageTags(String message) {
        
        return message.replaceAll("<[^>]*>", "");
    }
    
    public String getPlainTextMessage(String messageKey, TagResolver... placeholders) {
        Component component = getMessage(null, messageKey, placeholders);
        if (component != null) {
            return PlainTextComponentSerializer.plainText().serialize(component);
        }
        return "Message not found: " + messageKey;
    }
    
    public String getLegacyMessage(String messageKey, TagResolver... placeholders) {
        Component component = getMessage(null, messageKey, placeholders);
        if (component != null) {
            return LegacyComponentSerializer.legacySection().serialize(component);
        }
        return "Message not found: " + messageKey;
    }

    
    
    public void sendSuccess(Player player, String messageKey, TagResolver... placeholders) {
        sendMessage(player, messageKey, placeholders);
    }
    
    public void sendError(Player player, String messageKey, TagResolver... placeholders) {
        sendMessage(player, messageKey, placeholders);
    }
    
    public void sendWarning(Player player, String messageKey, TagResolver... placeholders) {
        sendMessage(player, messageKey, placeholders);
    }
    
    public void sendInfo(Player player, String messageKey, TagResolver... placeholders) {
        sendMessage(player, messageKey, placeholders);
    }

    
    
    public static TagResolver playerPlaceholder(String key, Player player) {
        return Placeholder.unparsed(key, player.getName());
    }
    
    public static TagResolver stringPlaceholder(String key, String value) {
        return Placeholder.unparsed(key, value != null ? value : "");
    }
    
    public static TagResolver numberPlaceholder(String key, Number value) {
        return Placeholder.unparsed(key, String.valueOf(value));
    }
    
    public static TagResolver timePlaceholder(String key, long timeInSeconds) {
        long minutes = timeInSeconds / 60;
        long seconds = timeInSeconds % 60;
        String timeString = minutes > 0 ? minutes + "m " + seconds + "s" : seconds + "s";
        return Placeholder.unparsed(key, timeString);
    }
    
    public static TagResolver distancePlaceholder(String key, double distance) {
        return Placeholder.unparsed(key, String.valueOf((int) distance));
    }
    
    public static TagResolver starsPlaceholder(String key, int level) {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < level; i++) {
            stars.append("⭐");
        }
        return Placeholder.unparsed(key, stars.toString());
    }
    
    public static TagResolver booleanPlaceholder(String key, boolean value) {
        return Placeholder.unparsed(key, value ? "true" : "false");
    }
    
    public static TagResolver percentagePlaceholder(String key, double percentage) {
        return Placeholder.unparsed(key, String.format("%.1f%%", percentage * 100));
    }
    
    public static TagResolver componentPlaceholder(String key, Component component) {
        return Placeholder.component(key, component);
    }

    
    
    public boolean hasMessage(String messageKey) {
        return messageCache.containsKey(messageKey);
    }
    
    public void validateMessages() {
        logger.info("Validating message configuration...");
        
        String[] requiredMessages = {
            "universal.no-permission",
            "universal.player-not-found",
            "universal.player-only",
            "duty.activation.success",
            "duty.deactivation.success",
            "chase.start.success",
            "chase.end.success",
            "system.startup",
            "system.shutdown",
            "prefix"
        };
        
        List<String> missingRequired = new ArrayList<>();
        for (String messageKey : requiredMessages) {
            if (!hasMessage(messageKey)) {
                missingRequired.add(messageKey);
            }
        }
        
        if (missingRequired.isEmpty()) {
            logger.info("All required messages are present!");
        } else {
            logger.warning("Found " + missingRequired.size() + " missing required messages:");
            for (String missing : missingRequired) {
                logger.warning("  - " + missing);
            }
        }
        
        
        validateMessageFormats();
    }
    
    private void validateMessageFormats() {
        int invalidCount = 0;
        int legacyPlaceholderMessages = 0;
        for (Map.Entry<String, String> entry : messageCache.entrySet()) {
            String key = entry.getKey();
            String message = entry.getValue();

            
            if (LEGACY_PLACEHOLDER.matcher(message).find()) {
                legacyPlaceholderMessages++;
            }

            try {
                
                miniMessage.deserialize(message);
            } catch (Exception e) {
                logger.warning("Invalid message format '" + key + "': " + e.getMessage());
                invalidCount++;
            }
        }

        if (legacyPlaceholderMessages > 0) {
            logger.info("Detected legacy-style placeholders in " + legacyPlaceholderMessages + " messages. Runtime conversion {placeholder} -> <placeholder> is ENABLED.");
        } else {
            logger.info("No legacy-style placeholders detected in messages.");
        }

        if (invalidCount > 0) {
            logger.warning("Found " + invalidCount + " messages with invalid format");
        }
    }
    
    private void trackMessageUsage(String messageKey) {
        if (plugin.getConfigManager().isDebugMode()) {
            messageUsageCount.put(messageKey, messageUsageCount.getOrDefault(messageKey, 0) + 1);
        }
    }
    
    public Map<String, Integer> getMessageUsageStats() {
        return new HashMap<>(messageUsageCount);
    }
    
    public List<String> getMissingMessages() {
        return new ArrayList<>(missingMessages);
    }
    
    public void logMissingMessagesSummary() {
        if (!missingMessages.isEmpty()) {
            logger.warning("=== MISSING MESSAGES SUMMARY ===");
            logger.warning("Total missing messages: " + missingMessages.size());
            logger.warning("Missing message keys:");
            for (String missing : missingMessages) {
                logger.warning("  - " + missing);
            }
            logger.warning("=== END MISSING MESSAGES SUMMARY ===");
        }
    }
    
    public List<String> getInvalidMessages() {
        return new ArrayList<>(invalidMessages);
    }
    
    public Map<String, String> getAllMessages() {
        return new HashMap<>(messageCache);
    }
    
    public void testMessage(String messageKey) {
        logger.info("=== Testing Message: " + messageKey + " ===");
        String rawMessage = getRawMessage(messageKey);
        if (rawMessage != null) {
            logger.info("Raw message found: " + rawMessage);
            try {
                Component parsed = miniMessage.deserialize(rawMessage);
                logger.info("Message parsed successfully");
            } catch (Exception e) {
                logger.warning("Failed to parse message: " + e.getMessage());
            }
        } else {
            logger.warning("Message not found in cache");
            logger.info("Available message count: " + messageCache.size());
        }
        logger.info("=== End Test ===");
    }
    
    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled;
    }

    
    
    public void cleanup() {
        logger.info("Cleaning up MessageManager resources...");
        
        try {
        
        for (Map.Entry<UUID, BossBar> entry : activeBossBars.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    try {
                player.hideBossBar(entry.getValue());
                    } catch (Exception e) {
                        logger.warning("Error hiding boss bar for " + player.getName() + " during cleanup: " + e.getMessage());
                    }
            }
        }
        activeBossBars.clear();
            
            
            for (Map.Entry<UUID, BukkitTask> entry : bossBarTasks.entrySet()) {
                BukkitTask task = entry.getValue();
                if (task != null) {
                    try {
                        task.cancel();
                    } catch (Exception e) {
                        logger.warning("Error cancelling boss bar task during cleanup: " + e.getMessage());
                    }
                }
            }
            bossBarTasks.clear();
            
            
            for (Map.Entry<UUID, BukkitTask> entry : actionBarTasks.entrySet()) {
                BukkitTask task = entry.getValue();
                if (task != null) {
                    try {
                        task.cancel();
                    } catch (Exception e) {
                        logger.warning("Error cancelling action bar task during cleanup: " + e.getMessage());
                    }
                }
                
                Player player = plugin.getServer().getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    try {
                        player.sendActionBar(Component.empty());
                    } catch (Exception e) {
                        logger.warning("Error clearing action bar for " + player.getName() + " during cleanup: " + e.getMessage());
                    }
                }
            }
            actionBarTasks.clear();
            
            
        messageCache.clear();
            messageUsageCount.clear();
            missingMessages.clear();
            invalidMessages.clear();
            
            logger.info("MessageManager cleanup completed successfully");
        } catch (Exception e) {
            logger.severe("Error during MessageManager cleanup: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    
    public void cleanupPlayer(Player player) {
        if (player == null) return;
        
        try {
            UUID playerId = player.getUniqueId();
            
            
            BossBar bossBar = activeBossBars.remove(playerId);
            if (bossBar != null) {
                try {
                    player.hideBossBar(bossBar);
                } catch (Exception e) {
                    logger.warning("Error hiding boss bar for " + player.getName() + " during player cleanup: " + e.getMessage());
                }
            }
            
            
            BukkitTask bossTask = bossBarTasks.remove(playerId);
            if (bossTask != null) {
                try {
                    bossTask.cancel();
                } catch (Exception e) {
                    logger.warning("Error cancelling boss bar task for " + player.getName() + " during player cleanup: " + e.getMessage());
                }
            }
            
            
            BukkitTask actionTask = actionBarTasks.remove(playerId);
            if (actionTask != null) {
                try {
                    actionTask.cancel();
                } catch (Exception e) {
                    logger.warning("Error cancelling action bar task for " + player.getName() + " during player cleanup: " + e.getMessage());
                }
            }
            
            try {
                player.sendActionBar(Component.empty());
            } catch (Exception e) {
                logger.warning("Error clearing action bar for " + player.getName() + " during player cleanup: " + e.getMessage());
            }
            
            
        } catch (Exception e) {
            logger.warning("Error during player cleanup for " + player.getName() + ": " + e.getMessage());
        }
    }

    
    
    public void generateDiagnosticReport() {
        logger.info("=== MessageManager Diagnostic Report ===");
        logger.info("Total messages loaded: " + messageCache.size());
        logger.info("PlaceholderAPI enabled: " + placeholderAPIEnabled);
        logger.info("Active boss bars: " + activeBossBars.size());
        logger.info("Active boss bar tasks: " + bossBarTasks.size());
        logger.info("Active action bar tasks: " + actionBarTasks.size());
        logger.info("Missing messages: " + missingMessages.size());
        logger.info("Invalid messages: " + invalidMessages.size());
        
        
        logger.info("ConfigManager available: " + (plugin.getConfigManager() != null));
        if (plugin.getConfigManager() != null) {
            logger.info("ConfigManager config available: " + (plugin.getConfigManager().getConfig() != null));
            if (plugin.getConfigManager().getConfig() != null) {
                logger.info("ConfigManager config keys: " + plugin.getConfigManager().getConfig().getKeys(false));
                ConfigurationSection messages = plugin.getConfigManager().getConfig().getConfigurationSection("messages");
                logger.info("Messages section exists: " + (messages != null));
                if (messages != null) {
                    logger.info("Messages section keys: " + messages.getKeys(false));
                }
            }
        }
        
        
        logger.info("Direct plugin config available: " + (plugin.getConfig() != null));
        if (plugin.getConfig() != null) {
            logger.info("Direct plugin config keys: " + plugin.getConfig().getKeys(false));
            ConfigurationSection messages = plugin.getConfig().getConfigurationSection("messages");
            logger.info("Direct messages section exists: " + (messages != null));
            if (messages != null) {
                logger.info("Direct messages section keys: " + messages.getKeys(false));
            }
        }
        
        
        logger.info("Loaded message keys:");
        for (String key : messageCache.keySet()) {
            logger.info("  " + key);
        }
        
        if (plugin.getConfigManager().isDebugMode()) {
            logger.info("Most used messages:");
            messageUsageCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> logger.info("  " + entry.getKey() + ": " + entry.getValue()));
        }
        
        logger.info("=== End Diagnostic Report ===");
    }
} 