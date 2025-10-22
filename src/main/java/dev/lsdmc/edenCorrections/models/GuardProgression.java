package dev.lsdmc.edenCorrections.models;

import java.util.Objects;
import java.util.UUID;


public class GuardProgression {
    private final UUID playerId;

    private long xp;              
    private int level;            
    private int tier;             
    private long totalXp;         
    private long lastLevelupAt;   

    private long dailyXp;         
    private long weeklyXp;
    private int streakDays;       

    public GuardProgression(UUID playerId) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.level = 1;
        this.tier = 0;
        this.xp = 0L;
        this.totalXp = 0L;
        this.lastLevelupAt = 0L;
        this.dailyXp = 0L;
        this.weeklyXp = 0L;
        this.streakDays = 0;
    }

    public UUID getPlayerId() { return playerId; }
    public long getXp() { return xp; }
    public int getLevel() { return level; }
    public int getTier() { return tier; }
    public long getTotalXp() { return totalXp; }
    public long getLastLevelupAt() { return lastLevelupAt; }
    public long getDailyXp() { return dailyXp; }
    public long getWeeklyXp() { return weeklyXp; }
    public int getStreakDays() { return streakDays; }

    public void setXp(long xp) { this.xp = Math.max(0L, xp); }
    public void setLevel(int level) { this.level = Math.max(1, level); }
    public void setTier(int tier) { this.tier = Math.max(0, tier); }
    public void setTotalXp(long totalXp) { this.totalXp = Math.max(0L, totalXp); }
    public void setLastLevelupAt(long ts) { this.lastLevelupAt = ts; }
    public void setDailyXp(long v) { this.dailyXp = Math.max(0L, v); }
    public void setWeeklyXp(long v) { this.weeklyXp = Math.max(0L, v); }
    public void setStreakDays(int v) { this.streakDays = Math.max(0, v); }

    public void addXp(long amount) {
        if (amount <= 0) return;
        this.xp += amount;
        this.totalXp += amount;
        this.dailyXp += amount;
        this.weeklyXp += amount;
    }

    public double getProgressToNext(long neededForNext) {
        if (neededForNext <= 0) return 1.0;
        return Math.max(0.0, Math.min(1.0, (double) xp / (double) neededForNext));
    }
}


