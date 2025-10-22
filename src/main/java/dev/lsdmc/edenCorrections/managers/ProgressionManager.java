package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import net.luckperms.api.LuckPerms;

import net.luckperms.api.track.Track;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.logging.Logger;

import static dev.lsdmc.edenCorrections.managers.MessageManager.*;


public class ProgressionManager {
    private final EdenCorrections plugin;
    private final Logger logger;

    public ProgressionManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void initialize() {
        logger.info("Rank-based progression system initialized");
    }

    public void cleanup() {
        logger.info("ProgressionManager shutting down...");
    }

    
    public void showProgressionStatus(Player player) {
        logger.info("DEBUG: Showing progression status for " + player.getName());

        String currentRank = plugin.getDutyManager().getPlayerGuardRank(player);
        logger.info("DEBUG: Current rank for " + player.getName() + ": " + currentRank);
        if (currentRank == null) {
            logger.info("DEBUG: Player " + player.getName() + " has no rank");
            plugin.getMessageManager().sendMessage(player, "progression.no-rank");
            return;
        }

        String nextRank = getNextRank(currentRank);
        if (nextRank == null) {
            plugin.getMessageManager().sendMessage(player, "progression.max-rank",
                stringPlaceholder("rank", currentRank));
            return;
        }

        
        showProgressStatus(player, currentRank, nextRank);
    }

    
    private void showProgressStatus(Player player, String currentRank, String nextRank) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        
        String configPath = "progression.rankup.requirements." + nextRank.toLowerCase();
        int reqArrests = plugin.getConfigManager().getConfig().getInt(configPath + ".arrests", 10);
        int reqKills = plugin.getConfigManager().getConfig().getInt(configPath + ".kills", 0);
        long reqDuty = plugin.getConfigManager().getConfig().getLong(configPath + ".duty_time_seconds", 3600L);
        double reqMoney = plugin.getConfigManager().getConfig().getDouble(configPath + ".vault_money", 0.0);
        int reqTokens = plugin.getConfigManager().getConfig().getInt(configPath + ".et_tokens", 0);

        
        int currentArrests = data.getTotalArrests();
        int currentKills = data.getTotalQualifyingKills();
        long currentDuty = data.getTotalDutyTime() / 1000L;

        
        boolean usesArrests = reqArrests > 0;
        boolean usesKills = reqKills > 0;
        boolean usesDuty = reqDuty > 0;
        boolean usesVault = reqMoney > 0.0; 
        boolean usesTokens = reqTokens > 0; 

        
        boolean canPromote = true;
        if (usesArrests) {
            canPromote = canPromote && currentArrests >= reqArrests;
        }
        if (usesDuty) {
            canPromote = canPromote && currentDuty >= reqDuty;
        }
        if (usesKills) {
            canPromote = canPromote && currentKills >= reqKills;
        }
        
        
        if (usesVault) {
            Double currentMoney = plugin.getVaultEconomyManager().getBalance(player).join();
            canPromote = canPromote && currentMoney >= reqMoney;
        }
        if (usesTokens) {
            int currentTokens = getPlayerTokens(player);
            canPromote = canPromote && currentTokens >= reqTokens;
        }

        
        if (usesArrests && usesDuty && !usesKills) {
            
            plugin.getMessageManager().sendMessage(player, "progression.progress-status",
                stringPlaceholder("current", currentRank),
                stringPlaceholder("next", nextRank),
                numberPlaceholder("arrests", currentArrests),
                numberPlaceholder("required_arrests", reqArrests),
                numberPlaceholder("current_time", currentDuty),
                numberPlaceholder("required_time", reqDuty));
        } else {
            
            plugin.getMessageManager().sendMessage(player, "system.info",
                componentPlaceholder("message", MiniMessage.miniMessage().deserialize("<gradient:#06FFA5:#00E5FF>📈 Rank Progression</gradient> <color:#ADB5BD>Current: " + currentRank + " → Next: " + nextRank + "</color>")));
            if (usesArrests) {
                plugin.getMessageManager().sendMessage(player, "system.info",
                    componentPlaceholder("message", MiniMessage.miniMessage().deserialize("<color:#ADB5BD>Arrests: <color:#FFB3C6>" + currentArrests + "/" + reqArrests + "</color></color>")));
            }
            if (usesKills) {
                plugin.getMessageManager().sendMessage(player, "system.info",
                    componentPlaceholder("message", MiniMessage.miniMessage().deserialize("<color:#ADB5BD>Kills: <color:#FFB3C6>" + currentKills + "/" + reqKills + "</color> <color:#ADB5BD>(wanted/chased only)</color></color>")));
            }
            if (usesDuty) {
                plugin.getMessageManager().sendMessage(player, "system.info",
                    componentPlaceholder("message", MiniMessage.miniMessage().deserialize("<color:#ADB5BD>Duty Time: <color:#FFB3C6>" + currentDuty + "/" + reqDuty + "s</color></color>")));
            }
        }

        
        if (usesVault || usesTokens) {
            if (usesVault) {
                Double currentMoney = plugin.getVaultEconomyManager().getBalance(player).join();
                double percent = reqMoney > 0 ? (currentMoney / reqMoney) * 100.0 : 100.0;
                percent = Math.max(0.0, Math.min(100.0, percent));
                double rounded = Math.round(percent * 10.0) / 10.0;
                plugin.getMessageManager().sendMessage(player, "progression.progress-vault",
                    numberPlaceholder("money", currentMoney),
                    numberPlaceholder("req_money", reqMoney),
                    numberPlaceholder("money_progress", rounded));
            }
            if (usesTokens) {
                int currentTokens = getPlayerTokens(player);
                double percent = reqTokens > 0 ? ((double) currentTokens / (double) reqTokens) * 100.0 : 100.0;
                percent = Math.max(0.0, Math.min(100.0, percent));
                double rounded = Math.round(percent * 10.0) / 10.0;
                plugin.getMessageManager().sendMessage(player, "progression.progress-tokens",
                    numberPlaceholder("tokens", currentTokens),
                    numberPlaceholder("req_tokens", reqTokens),
                    numberPlaceholder("token_progress", rounded));
            }
        }

        
        if (canPromote) {
            plugin.getMessageManager().sendMessage(player, "progression.ready-to-rankup",
                stringPlaceholder("next", nextRank));
        } else {
            plugin.getMessageManager().sendMessage(player, "progression.not-ready-yet");
        }
    }

    
    private void showRankupRequirements(Player player, String currentRank, String nextRank) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        
        String configPath = "progression.rankup.requirements." + nextRank.toLowerCase();
        int reqArrests = plugin.getConfigManager().getConfig().getInt(configPath + ".arrests", 10);
        int reqKills = plugin.getConfigManager().getConfig().getInt(configPath + ".kills", 0);
        long reqDuty = plugin.getConfigManager().getConfig().getLong(configPath + ".duty_time_seconds", 3600L);
        double reqMoney = plugin.getConfigManager().getConfig().getDouble(configPath + ".vault_money", 0.0);
        int reqTokens = plugin.getConfigManager().getConfig().getInt(configPath + ".et_tokens", 0);

        
        int currentArrests = data.getTotalArrests();
        int currentKills = data.getTotalQualifyingKills();
        long currentDuty = data.getTotalDutyTime() / 1000L;

        boolean usesArrests = reqArrests > 0;
        boolean usesKills = reqKills > 0;
        boolean usesDuty = reqDuty > 0;

        if (usesArrests && usesDuty && !usesKills) {
            plugin.getMessageManager().sendMessage(player, "progression.rankup-requirements",
                stringPlaceholder("rank", nextRank),
                numberPlaceholder("arrests", currentArrests),
                numberPlaceholder("required_arrests", reqArrests),
                numberPlaceholder("current_time", currentDuty),
                numberPlaceholder("required_time", reqDuty));
        } else {
            
            plugin.getMessageManager().sendMessage(player, "system.info",
                componentPlaceholder("message", MiniMessage.miniMessage().deserialize("<gradient:#06FFA5:#00E5FF>📋 Rank Requirements</gradient> <color:#ADB5BD>for <color:#FFB3C6>" + nextRank + "</color></color>")));
            if (usesArrests) {
                plugin.getMessageManager().sendMessage(player, "system.info",
                    componentPlaceholder("message", MiniMessage.miniMessage().deserialize("<color:#ADB5BD>Arrests: <color:#FFB3C6>" + currentArrests + "/" + reqArrests + "</color></color>")));
            }
            if (usesKills) {
                plugin.getMessageManager().sendMessage(player, "system.info",
                    componentPlaceholder("message", MiniMessage.miniMessage().deserialize("<color:#ADB5BD>Kills: <color:#FFB3C6>" + currentKills + "/" + reqKills + "</color> <color:#ADB5BD>(wanted/chased only)</color></color>")));
            }
            if (usesDuty) {
                plugin.getMessageManager().sendMessage(player, "system.info",
                    componentPlaceholder("message", MiniMessage.miniMessage().deserialize("<color:#ADB5BD>Duty Time: <color:#FFB3C6>" + currentDuty + "/" + reqDuty + "s</color></color>")));
            }
        }

        if (reqMoney > 0 || reqTokens > 0) {
            if (reqMoney > 0) {
                double have = 0.0;
                try { have = plugin.getVaultEconomyManager().getBalance(player).join(); } catch (Exception ignored) {}
                double shortfall = Math.max(0.0, reqMoney - have);
                plugin.getMessageManager().sendMessage(player, "progression.vault-requirement",
                    numberPlaceholder("have", have),
                    numberPlaceholder("need", reqMoney),
                    numberPlaceholder("shortfall", shortfall));
            }
            if (reqTokens > 0) {
                int have = 0;
                try { have = getPlayerTokens(player); } catch (Exception ignored) {}
                int shortfall = Math.max(0, reqTokens - have);
                plugin.getMessageManager().sendMessage(player, "progression.tokens-requirement",
                    numberPlaceholder("have", have),
                    numberPlaceholder("need", reqTokens),
                    numberPlaceholder("shortfall", shortfall));
            }
        }
    }

    
    public void attemptPromotion(Player player) {
        logger.info("DEBUG: Attempting promotion for " + player.getName());
        String currentRank = plugin.getDutyManager().getPlayerGuardRank(player);
        logger.info("DEBUG: Current rank for " + player.getName() + ": " + currentRank);
        if (currentRank == null) {
            logger.info("DEBUG: Player " + player.getName() + " has no rank");
            plugin.getMessageManager().sendMessage(player, "progression.no-rank");
            return;
        }

        String nextRank = getNextRank(currentRank);
        if (nextRank == null) {
            plugin.getMessageManager().sendMessage(player, "progression.max-rank",
                stringPlaceholder("rank", currentRank));
            return;
        }

        
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        PromotionResult result = checkPromotionRequirements(player, data, nextRank);
        
        if (!result.canPromote) {
            if (!result.failureReason.isEmpty()) {
                plugin.getMessageManager().sendMessage(player, "progression.rank-limit-reached",
                    stringPlaceholder("reason", result.failureReason));
            } else {
                showRankupRequirements(player, currentRank, nextRank);
            }
            return;
        }

        
        if (result.vaultCost > 0) {
            plugin.getVaultEconomyManager().takeMoney(player, result.vaultCost, "Guard rank promotion");
        }
        if (result.etCost > 0) {
            String cmd = plugin.getConfigManager().getConfig().getString("progression.rankup.et.deduct-command", "et take {player} {amount}")
                .replace("{player}", player.getName())
                .replace("{amount}", String.valueOf(result.etCost));
            Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
        }

        
        performPromotion(player, currentRank, nextRank);
    }

    private String getNextRank(String currentRank) {
        
        java.util.List<String> order = plugin.getConfigManager().getConfig().getStringList("progression.rankup.order");
        if (order == null || order.isEmpty()) {
            try {
                org.bukkit.plugin.RegisteredServiceProvider<LuckPerms> reg = org.bukkit.Bukkit.getServicesManager().getRegistration(LuckPerms.class);
                if (reg != null) {
                    LuckPerms luckPerms = reg.getProvider();
                    String trackName = plugin.getConfigManager().getConfig().getString("progression.rankup.track", "guard");
                    net.luckperms.api.track.Track track = luckPerms.getTrackManager().getTrack(trackName);
                    if (track != null) {
                        order = new java.util.ArrayList<>(track.getGroups());
                    }
                }
            } catch (Exception ignored) {}
        }
        if (order == null || order.isEmpty()) {
            order = java.util.Arrays.asList("trainee", "private", "officer", "sergeant", "captain", "warden");
        }
        for (int i = 0; i < order.size() - 1; i++) {
            if (order.get(i).equalsIgnoreCase(currentRank)) {
                return order.get(i + 1);
            }
        }
        return null;
    }

    private PromotionResult checkPromotionRequirements(Player player, PlayerData data, String nextRank) {
        PromotionResult result = new PromotionResult();
        
        
        if (!plugin.getConfigManager().isRankPromotionEnabled(nextRank)) {
            result.canPromote = false;
            result.failureReason = "Promotions to " + nextRank + " rank are disabled";
            return result;
        }
        
        
        if (!plugin.getConfigManager().isRankUnlimited(nextRank)) {
            int currentCount = getCurrentRankCount(nextRank);
            int maxAllowed = plugin.getConfigManager().getRankLimit(nextRank);
            
            if (currentCount >= maxAllowed) {
                result.canPromote = false;
                result.failureReason = "Rank limit reached for " + nextRank + " (" + currentCount + "/" + maxAllowed + ")";
                return result;
            }
        }
        
        
        String configPath = "progression.rankup.requirements." + nextRank.toLowerCase();
        int reqArrests = plugin.getConfigManager().getConfig().getInt(configPath + ".arrests", 10);
        int reqKills = plugin.getConfigManager().getConfig().getInt(configPath + ".kills", 0);
        long reqDuty = plugin.getConfigManager().getConfig().getLong(configPath + ".duty_time_seconds", 3600L);
        double reqMoney = plugin.getConfigManager().getConfig().getDouble(configPath + ".vault_money", 0.0);
        int reqTokens = plugin.getConfigManager().getConfig().getInt(configPath + ".et_tokens", 0);

        
        result.canPromote = true;
        boolean arrestOk = reqArrests <= 0 || data.getTotalArrests() >= reqArrests;
        boolean killsOk = reqKills <= 0 || data.getTotalQualifyingKills() >= reqKills;
        boolean dutyOk = reqDuty <= 0 || (data.getTotalDutyTime() / 1000L) >= reqDuty;
        boolean vaultOk = true;
        boolean tokensOk = true;

        if (reqMoney > 0.0) {
            Boolean hasEnough = plugin.getVaultEconomyManager().hasEnough(player, reqMoney).join();
            vaultOk = hasEnough != null && hasEnough;
            result.vaultCost = reqMoney;
        }

        if (reqTokens > 0) {
            int tokens = getPlayerTokens(player);
            tokensOk = tokens >= reqTokens;
            result.etCost = reqTokens;
        }

        result.canPromote = arrestOk && killsOk && dutyOk && vaultOk && tokensOk;

        
        try {
            logger.info("DEBUG: Promotion check for " + player.getName() + " → " + nextRank);
            logger.info("DEBUG: Requirements → arrests: required=" + reqArrests + ", current=" + data.getTotalArrests() + ", ok=" + arrestOk);
            logger.info("DEBUG: Requirements → kills: required=" + reqKills + ", current=" + data.getTotalQualifyingKills() + ", ok=" + killsOk);
            logger.info("DEBUG: Requirements → duty_time_seconds: required=" + reqDuty + ", current=" + (data.getTotalDutyTime() / 1000L) + ", ok=" + dutyOk);
            if (reqMoney > 0.0) {
                Double bal = plugin.getVaultEconomyManager().getBalance(player).join();
                logger.info("DEBUG: Requirements → vault_money: required=" + reqMoney + ", current=" + (bal == null ? 0.0 : bal) + ", ok=" + vaultOk);
            }
            if (reqTokens > 0) {
                int tokens = getPlayerTokens(player);
                logger.info("DEBUG: Requirements → et_tokens: required=" + reqTokens + ", current=" + tokens + ", ok=" + tokensOk);
            }
            logger.info("DEBUG: Final eligibility: canPromote=" + result.canPromote);
        } catch (Exception ignored) {}

        return result;
    }

    private int getPlayerTokens(Player player) {
        try {
            if (plugin.getMessageManager().isPlaceholderAPIEnabled()) {
                String placeholder = plugin.getConfigManager().getConfig().getString("progression.rankup.et.placeholder", "%coinsengine_balance_ecotokens%");
                String raw = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, placeholder);
                return Integer.parseInt(raw.replaceAll("[^0-9]", ""));
            }
        } catch (Exception ignored) {}
        return 0;
    }



    private void performPromotion(Player player, String currentRank, String nextRank) {
        try {
            RegisteredServiceProvider<LuckPerms> reg = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (reg == null) {
                plugin.getMessageManager().sendMessage(player, "progression.luckperms-error");
                return;
            }

            LuckPerms luckPerms = reg.getProvider();
            String trackName = plugin.getConfigManager().getConfig().getString("progression.rankup.track", "guard");
            Track track = luckPerms.getTrackManager().getTrack(trackName);

            if (track != null) {
                luckPerms.getUserManager().modifyUser(player.getUniqueId(), user -> {
                    track.promote(user, luckPerms.getContextManager().getStaticContext());
                });
                plugin.getMessageManager().sendMessage(player, "progression.promoted",
                    stringPlaceholder("previous", currentRank),
                    stringPlaceholder("next", nextRank));

                
                plugin.getDataManager().getPlayerData(player.getUniqueId());
                if (plugin.getDutyManager().isOnDuty(player)) {
                    
                    
                }
            } else {
                plugin.getMessageManager().sendMessage(player, "progression.track-not-found",
                    stringPlaceholder("track", trackName));
            }
        } catch (Exception e) {
            logger.warning("Failed to promote player " + player.getName() + ": " + e.getMessage());
            plugin.getMessageManager().sendMessage(player, "progression.promotion-error");
        }
    }

    
    private int getCurrentRankCount(String rank) {
        int count = 0;
        
        
        for (org.bukkit.entity.Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            String playerRank = plugin.getDutyManager().getPlayerGuardRank(onlinePlayer);
            if (rank.equalsIgnoreCase(playerRank)) {
                count++;
            }
        }
        
        
        
        
        
        return count;
    }

    private static class PromotionResult {
        boolean canPromote = true;
        double vaultCost = 0.0;
        int etCost = 0;
        String failureReason = "";
    }
}


