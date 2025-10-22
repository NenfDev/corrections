package dev.lsdmc.edenCorrections.integrations;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import dev.lsdmc.edenCorrections.models.ChaseData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class EdenCorrectionsExpansion extends PlaceholderExpansion {

    private final EdenCorrections plugin;

    public EdenCorrectionsExpansion(EdenCorrections plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "corrections";
    }

    @Override
    public String getAuthor() {
        return "LSDMC";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; 
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String params) {
        if (offlinePlayer == null) {
            return null;
        }

        Player player = offlinePlayer.getPlayer();
        if (player == null) {
            return null;
        }

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data == null) {
            return null;
        }

        
        String[] parts = params.split("_");
        String category = parts[0].toLowerCase();

        switch (category) {
            case "wanted":
                return handleWantedPlaceholders(data, parts);
            case "duty":
                return handleDutyPlaceholders(data, player, parts);
            case "chase":
                return handleChasePlaceholders(data, player, parts);
            case "jail":
                return handleJailPlaceholders(data, parts);
            case "contraband":
                return handleContrabandPlaceholders(data, player, parts);
            case "banking":
                return handleBankingPlaceholders(data, player, parts);
            case "player":
                return handlePlayerPlaceholders(data, player, parts);
            case "guards":
                return handleGuardsPlaceholders(parts);
            case "active":
                return handleActivePlaceholders(parts);
            case "progression":
                return handleProgressionPlaceholders(data, player, parts);
            case "session":
                return handleSessionPlaceholders(data, parts);
            case "penalty":
                return handlePenaltyPlaceholders(data, parts);
            case "stats":
                return handleStatsPlaceholders(data, parts);
            case "offduty":
                return handleOffDutyPlaceholders(data, parts);
            default:
                return null;
        }
    }

    private String handleWantedPlaceholders(PlayerData data, String[] parts) {
        if (parts.length < 2) return null;

        switch (parts[1].toLowerCase()) {
            case "level":
                return String.valueOf(data.getWantedLevel());
            case "stars":
                return plugin.getWantedManager().getWantedStars(data.getWantedLevel());
            case "time":
                long remaining = data.getRemainingWantedTime();
                return remaining > 0 ? String.valueOf(remaining / 1000) : "0";
            case "reason":
                return data.getWantedReason();
            case "active":
                return String.valueOf(data.isWanted());
            default:
                return null;
        }
    }

    private String handleDutyPlaceholders(PlayerData data, Player player, String[] parts) {
        if (parts.length < 2) return null;

        switch (parts[1].toLowerCase()) {
            case "status":
                return data.isOnDuty() ? "On Duty" : "Off Duty";
            case "active":
                return String.valueOf(data.isOnDuty());
            case "rank":
                return data.getGuardRank() != null ? data.getGuardRank() : "None";
            case "time":
                if (data.isOnDuty()) {
                    long dutyTime = (System.currentTimeMillis() - data.getDutyStartTime()) / 1000;
                    return String.valueOf(dutyTime);
                }
                return "0";
            case "total":
                return String.valueOf(data.getTotalDutyTime() / 1000);
            default:
                return null;
        }
    }

    private String handleChasePlaceholders(PlayerData data, Player player, String[] parts) {
        if (parts.length < 2) return null;

        switch (parts[1].toLowerCase()) {
            case "active":
                return String.valueOf(data.isBeingChased());
            case "target":
                if (data.isBeingChased()) {
                    Player chaser = plugin.getServer().getPlayer(data.getChaserGuard());
                    return chaser != null ? chaser.getName() : "Unknown";
                }
                return "None";
            case "guard":
                ChaseData chase = plugin.getDataManager().getChaseByGuard(player.getUniqueId());
                if (chase != null) {
                    Player target = plugin.getServer().getPlayer(chase.getTargetId());
                    return target != null ? target.getName() : "Unknown";
                }
                return "None";
            case "time":
                if (data.isBeingChased()) {
                    long chaseTime = (System.currentTimeMillis() - data.getChaseStartTime()) / 1000;
                    return String.valueOf(chaseTime);
                }
                return "0";
            case "combat":
                return String.valueOf(plugin.getChaseManager().isInCombat(player));
            default:
                return null;
        }
    }

    private String handleJailPlaceholders(PlayerData data, String[] parts) {
        if (parts.length < 2) return null;

        switch (parts[1].toLowerCase()) {
            case "countdown":
                return String.valueOf(plugin.getJailManager().isInJailCountdown(plugin.getServer().getPlayer(data.getPlayerId())));
            default:
                return null;
        }
    }

    private String handleContrabandPlaceholders(PlayerData data, Player player, String[] parts) {
        if (parts.length < 2) return null;

        switch (parts[1].toLowerCase()) {
            case "request":
                return String.valueOf(plugin.getContrabandManager().hasActiveRequest(player));
            default:
                return null;
        }
    }

    private String handleBankingPlaceholders(PlayerData data, Player player, String[] parts) {
        if (parts.length < 2) return null;

        switch (parts[1].toLowerCase()) {
            case "tokens":
                return String.valueOf(plugin.getDutyBankingManager().getAvailableTokens(player));
            case "time":
                return String.valueOf(plugin.getDutyBankingManager().getTotalDutyTime(player));
            case "enabled":
                return String.valueOf(plugin.getConfigManager().isDutyBankingEnabled());
            default:
                return null;
        }
    }

    private String handlePlayerPlaceholders(PlayerData data, Player player, String[] parts) {
        if (parts.length < 2) return null;

        switch (parts[1].toLowerCase()) {
            case "arrests":
                return String.valueOf(data.getTotalArrests());
            case "violations":
                return String.valueOf(data.getTotalViolations());
            case "power":
                return String.valueOf(data.getTotalDutyTime()); 
            case "name":
                return player.getName();
            default:
                return null;
        }
    }
    
    private String handleGuardsPlaceholders(String[] parts) {
        if (parts.length < 2) return null;

        switch (parts[1].toLowerCase()) {
            case "online":
                return String.valueOf(getOnlineGuardsCount());
            default:
                return null;
        }
    }
    
    private String handleActivePlaceholders(String[] parts) {
        if (parts.length < 2) return null;

        switch (parts[1].toLowerCase()) {
            case "chases":
                return String.valueOf(getActiveChasesCount());
            default:
                return null;
        }
    }
    
    private int getOnlineGuardsCount() {
        int count = 0;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (plugin.getDutyManager().hasGuardAccessOrBypass(player)) {
                PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
                if (data != null && data.isOnDuty()) {
                    count++;
                }
            }
        }
        return count;
    }
    
    private int getActiveChasesCount() {
        int count = 0;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
            if (data != null && data.isBeingChased()) {
                count++;
            }
        }
        return count;
    }
    
    
    private String handleProgressionPlaceholders(PlayerData data, Player player, String[] parts) {
        if (parts.length < 2) return null;

        switch (parts[1].toLowerCase()) {
            case "current_rank":
                return data.getGuardRank() != null ? data.getGuardRank() : "None";
            case "next_rank":
                String currentRank = data.getGuardRank();
                if (currentRank == null) return "None";
                return getNextRank(currentRank);
            case "can_promote":
                return String.valueOf(canPromote(player, data));
            case "arrests_progress":
                return getArrestsProgress(player, data);
            case "duty_progress":
                return getDutyProgress(player, data);
            case "money_progress":
                return getMoneyProgress(player, data);
            case "tokens_progress":
                return getTokensProgress(player, data);
            case "arrests_required":
                return getArrestsRequired(player, data);
            case "duty_required":
                return getDutyRequired(player, data);
            case "money_required":
                return getMoneyRequired(player, data);
            case "tokens_required":
                return getTokensRequired(player, data);
            case "arrests_percentage":
                return getArrestsPercentage(player, data);
            case "duty_percentage":
                return getDutyPercentage(player, data);
            case "money_percentage":
                return getMoneyPercentage(player, data);
            case "tokens_percentage":
                return getTokensPercentage(player, data);
            case "max_rank":
                return isMaxRank(data.getGuardRank()) ? "true" : "false";
            default:
                return null;
        }
    }
    
    
    private String handleSessionPlaceholders(PlayerData data, String[] parts) {
        if (parts.length < 2) return null;

        switch (parts[1].toLowerCase()) {
            case "searches":
                return String.valueOf(data.getSessionSearches());
            case "successful_searches":
                return String.valueOf(data.getSessionSuccessfulSearches());
            case "arrests":
                return String.valueOf(data.getSessionArrests());
            case "kills":
                return String.valueOf(data.getSessionKills());
            case "detections":
                return String.valueOf(data.getSessionDetections());
            case "search_success_rate":
                if (data.getSessionSearches() == 0) return "0";
                double rate = (double) data.getSessionSuccessfulSearches() / data.getSessionSearches() * 100.0;
                return String.format("%.1f", rate);
            default:
                return null;
        }
    }
    
    
    private String handlePenaltyPlaceholders(PlayerData data, String[] parts) {
        if (parts.length < 2) return null;

        switch (parts[1].toLowerCase()) {
            case "stage":
                return String.valueOf(data.getCurrentPenaltyStage());
            case "active":
                return String.valueOf(data.isPenaltyTrackingActive());
            case "time_since_start":
                return String.valueOf(data.getTimeSincePenaltyStart() / 1000);
            case "time_since_last":
                return String.valueOf(data.getTimeSinceLastPenalty() / 1000);
            case "has_bossbar":
                return String.valueOf(data.hasActivePenaltyBossBar());
            case "penalty_duration":
                return String.valueOf(data.getTimeSincePenaltyStart() / 1000);
            default:
                return null;
        }
    }
    
    
    private String handleStatsPlaceholders(PlayerData data, String[] parts) {
        if (parts.length < 2) return null;

        switch (parts[1].toLowerCase()) {
            case "total_arrests":
                return String.valueOf(data.getTotalArrests());
            case "total_violations":
                return String.valueOf(data.getTotalViolations());
            case "total_duty_time":
                return String.valueOf(data.getTotalDutyTime() / 1000);
            case "total_duty_hours":
                return String.valueOf(data.getTotalDutyTime() / (1000 * 3600));
            case "total_duty_minutes":
                return String.valueOf(data.getTotalDutyTime() / (1000 * 60));
            case "kills_total":
                return String.valueOf(data.getTotalQualifyingKills());
            case "total_kills":
                
                return String.valueOf(data.getTotalQualifyingKills());
            case "qualifying_kills":
                
                return String.valueOf(data.getTotalQualifyingKills());
            case "arrest_rate":
                if (data.getTotalDutyTime() == 0) return "0";
                double rate = (double) data.getTotalArrests() / (data.getTotalDutyTime() / 3600000.0); 
                return String.format("%.2f", rate);
            case "violation_rate":
                if (data.getTotalDutyTime() == 0) return "0";
                double vRate = (double) data.getTotalViolations() / (data.getTotalDutyTime() / 3600000.0); 
                return String.format("%.2f", vRate);
            case "efficiency_score":
                if (data.getTotalDutyTime() == 0) return "0";
                double efficiency = (double) data.getTotalArrests() / Math.max(1, data.getTotalViolations());
                return String.format("%.1f", efficiency);
            default:
                return null;
        }
    }
    
    
    private String handleOffDutyPlaceholders(PlayerData data, String[] parts) {
        if (parts.length < 2) return null;

        switch (parts[1].toLowerCase()) {
            case "earned_time":
                return String.valueOf(data.getEarnedOffDutyTime() / 1000);
            case "earned_minutes":
                return String.valueOf(data.getEarnedOffDutyTime() / (1000 * 60));
            case "earned_hours":
                return String.valueOf(data.getEarnedOffDutyTime() / (1000 * 3600));
            case "consumed_time":
                return String.valueOf(data.getConsumedOffDutyTime() / 1000);
            case "consumed_minutes":
                return String.valueOf(data.getConsumedOffDutyTime() / (1000 * 60));
            case "available_time":
                return String.valueOf(data.getAvailableOffDutyTimeInSeconds());
            case "available_minutes":
                return String.valueOf(data.getAvailableOffDutyTimeInMinutes());
            case "has_earned_base":
                return String.valueOf(data.hasEarnedBaseTime());
            case "has_available_time":
                return String.valueOf(data.hasAvailableOffDutyTime());
            case "time_remaining":
                return String.valueOf(data.getAvailableOffDutyTimeInSeconds());
            default:
                return null;
        }
    }
    
    
    private String getNextRank(String currentRank) {
        if (currentRank == null) return null;
        
        switch (currentRank.toLowerCase()) {
            case "trainee": return "private";
            case "private": return "officer";
            case "officer": return "sergeant";
            case "sergeant": return "captain";
            case "captain": return null; 
            default: return null;
        }
    }
    
    private boolean isMaxRank(String rank) {
        return rank != null && rank.equalsIgnoreCase("captain"); 
    }
    
    private boolean canPromote(Player player, PlayerData data) {
        String currentRank = data.getGuardRank();
        if (currentRank == null || isMaxRank(currentRank)) return false;
        
        String nextRank = getNextRank(currentRank);
        if (nextRank == null) return false;
        
        
        String configPath = "progression.rankup.requirements." + nextRank.toLowerCase();
        int reqArrests = plugin.getConfigManager().getConfig().getInt(configPath + ".arrests", 10);
        long reqDuty = plugin.getConfigManager().getConfig().getLong(configPath + ".duty_time_seconds", 3600L);
        double reqMoney = plugin.getConfigManager().getConfig().getDouble(configPath + ".vault_money", 0.0);
        int reqTokens = plugin.getConfigManager().getConfig().getInt(configPath + ".et_tokens", 0);
        
        
        boolean canPromote = data.getTotalArrests() >= reqArrests && (data.getTotalDutyTime() / 1000) >= reqDuty;
        
        
        String mode = plugin.getConfigManager().getConfig().getString("progression.rankup.economy-mode", "vault");
        if ((mode.equals("vault") || mode.equals("both")) && reqMoney > 0.0) {
            Double currentMoney = plugin.getVaultEconomyManager().getBalance(player).join();
            canPromote = canPromote && currentMoney >= reqMoney;
        }
        if ((mode.equals("et") || mode.equals("both")) && reqTokens > 0) {
            int currentTokens = getPlayerTokens(player);
            canPromote = canPromote && currentTokens >= reqTokens;
        }
        
        return canPromote;
    }
    
    private String getArrestsProgress(Player player, PlayerData data) {
        String nextRank = getNextRank(data.getGuardRank());
        if (nextRank == null) return "0";
        
        String configPath = "progression.rankup.requirements." + nextRank.toLowerCase();
        int reqArrests = plugin.getConfigManager().getConfig().getInt(configPath + ".arrests", 10);
        return String.valueOf(Math.min(data.getTotalArrests(), reqArrests));
    }
    
    private String getDutyProgress(Player player, PlayerData data) {
        String nextRank = getNextRank(data.getGuardRank());
        if (nextRank == null) return "0";
        
        String configPath = "progression.rankup.requirements." + nextRank.toLowerCase();
        long reqDuty = plugin.getConfigManager().getConfig().getLong(configPath + ".duty_time_seconds", 3600L);
        long currentDuty = data.getTotalDutyTime() / 1000;
        return String.valueOf(Math.min(currentDuty, reqDuty));
    }
    
    private String getMoneyProgress(Player player, PlayerData data) {
        String nextRank = getNextRank(data.getGuardRank());
        if (nextRank == null) return "0";
        
        String configPath = "progression.rankup.requirements." + nextRank.toLowerCase();
        double reqMoney = plugin.getConfigManager().getConfig().getDouble(configPath + ".vault_money", 0.0);
        if (reqMoney <= 0) return "0";
        
        Double currentMoney = plugin.getVaultEconomyManager().getBalance(player).join();
        return String.format("%.2f", Math.min(currentMoney, reqMoney));
    }
    
    private String getTokensProgress(Player player, PlayerData data) {
        String nextRank = getNextRank(data.getGuardRank());
        if (nextRank == null) return "0";
        
        String configPath = "progression.rankup.requirements." + nextRank.toLowerCase();
        int reqTokens = plugin.getConfigManager().getConfig().getInt(configPath + ".et_tokens", 0);
        if (reqTokens <= 0) return "0";
        
        int currentTokens = getPlayerTokens(player);
        return String.valueOf(Math.min(currentTokens, reqTokens));
    }
    
    private String getArrestsRequired(Player player, PlayerData data) {
        String nextRank = getNextRank(data.getGuardRank());
        if (nextRank == null) return "0";
        
        String configPath = "progression.rankup.requirements." + nextRank.toLowerCase();
        int reqArrests = plugin.getConfigManager().getConfig().getInt(configPath + ".arrests", 10);
        return String.valueOf(reqArrests);
    }
    
    private String getDutyRequired(Player player, PlayerData data) {
        String nextRank = getNextRank(data.getGuardRank());
        if (nextRank == null) return "0";
        
        String configPath = "progression.rankup.requirements." + nextRank.toLowerCase();
        long reqDuty = plugin.getConfigManager().getConfig().getLong(configPath + ".duty_time_seconds", 3600L);
        return String.valueOf(reqDuty);
    }
    
    private String getMoneyRequired(Player player, PlayerData data) {
        String nextRank = getNextRank(data.getGuardRank());
        if (nextRank == null) return "0";
        
        String configPath = "progression.rankup.requirements." + nextRank.toLowerCase();
        double reqMoney = plugin.getConfigManager().getConfig().getDouble(configPath + ".vault_money", 0.0);
        return String.format("%.2f", reqMoney);
    }
    
    private String getTokensRequired(Player player, PlayerData data) {
        String nextRank = getNextRank(data.getGuardRank());
        if (nextRank == null) return "0";
        
        String configPath = "progression.rankup.requirements." + nextRank.toLowerCase();
        int reqTokens = plugin.getConfigManager().getConfig().getInt(configPath + ".et_tokens", 0);
        return String.valueOf(reqTokens);
    }
    
    private String getArrestsPercentage(Player player, PlayerData data) {
        String nextRank = getNextRank(data.getGuardRank());
        if (nextRank == null) return "0";
        
        String configPath = "progression.rankup.requirements." + nextRank.toLowerCase();
        int reqArrests = plugin.getConfigManager().getConfig().getInt(configPath + ".arrests", 10);
        if (reqArrests == 0) return "100";
        
        double percentage = Math.min(100.0, (double) data.getTotalArrests() / reqArrests * 100.0);
        return String.format("%.1f", percentage);
    }
    
    private String getDutyPercentage(Player player, PlayerData data) {
        String nextRank = getNextRank(data.getGuardRank());
        if (nextRank == null) return "0";
        
        String configPath = "progression.rankup.requirements." + nextRank.toLowerCase();
        long reqDuty = plugin.getConfigManager().getConfig().getLong(configPath + ".duty_time_seconds", 3600L);
        if (reqDuty == 0) return "100";
        
        long currentDuty = data.getTotalDutyTime() / 1000;
        double percentage = Math.min(100.0, (double) currentDuty / reqDuty * 100.0);
        return String.format("%.1f", percentage);
    }
    
    private String getMoneyPercentage(Player player, PlayerData data) {
        String nextRank = getNextRank(data.getGuardRank());
        if (nextRank == null) return "0";
        
        String configPath = "progression.rankup.requirements." + nextRank.toLowerCase();
        double reqMoney = plugin.getConfigManager().getConfig().getDouble(configPath + ".vault_money", 0.0);
        if (reqMoney <= 0) return "100";
        
        Double currentMoney = plugin.getVaultEconomyManager().getBalance(player).join();
        double percentage = Math.min(100.0, currentMoney / reqMoney * 100.0);
        return String.format("%.1f", percentage);
    }
    
    private String getTokensPercentage(Player player, PlayerData data) {
        String nextRank = getNextRank(data.getGuardRank());
        if (nextRank == null) return "0";
        
        String configPath = "progression.rankup.requirements." + nextRank.toLowerCase();
        int reqTokens = plugin.getConfigManager().getConfig().getInt(configPath + ".et_tokens", 0);
        if (reqTokens <= 0) return "100";
        
        int currentTokens = getPlayerTokens(player);
        double percentage = Math.min(100.0, (double) currentTokens / reqTokens * 100.0);
        return String.format("%.1f", percentage);
    }
    
    private int getPlayerTokens(Player player) {
        try {
            String placeholder = plugin.getConfigManager().getConfig().getString("progression.rankup.et.placeholder", "%coinsengine_balance_ecotokens%");
            
            String result = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, placeholder);
            return Integer.parseInt(result.trim());
        } catch (Exception e) {
            return 0;
        }
    }
} 