package dev.lsdmc.edenCorrections.storage;

import dev.lsdmc.edenCorrections.models.PlayerData;
import dev.lsdmc.edenCorrections.models.ChaseData;

import java.util.UUID;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.sql.SQLException;

public interface DatabaseHandler {
    
    
    
    
    void initialize() throws SQLException;
    
    
    void close();
    
    
    boolean isConnected();
    
    
    boolean testConnection();
    
    
    
    
    CompletableFuture<Void> savePlayerData(PlayerData playerData);
    
    
    CompletableFuture<PlayerData> loadPlayerData(UUID playerId);
    
    
    CompletableFuture<PlayerData> loadPlayerDataByName(String playerName);
    
    
    CompletableFuture<List<PlayerData>> loadAllPlayerData();
    
    
    CompletableFuture<Void> deletePlayerData(UUID playerId);
    
    
    
    
    CompletableFuture<Void> saveChaseData(ChaseData chaseData);
    
    
    CompletableFuture<ChaseData> loadChaseData(UUID chaseId);
    
    
    CompletableFuture<List<ChaseData>> loadAllActiveChases();
    
    
    CompletableFuture<Void> deleteChaseData(UUID chaseId);
    
    
    CompletableFuture<Void> cleanupExpiredChases();
    
    
    
    
    CompletableFuture<Void> savePlayerInventory(UUID playerId, String inventoryData);
    
    
    CompletableFuture<String> loadPlayerInventory(UUID playerId);
    
    
    CompletableFuture<Void> deletePlayerInventory(UUID playerId);
    
    
    CompletableFuture<List<UUID>> getPlayersWithStoredInventory();

    
    
    CompletableFuture<Void> saveOnDutyInventory(UUID playerId, String inventoryData);
    
    CompletableFuture<String> loadOnDutyInventory(UUID playerId);
    
    CompletableFuture<Void> deleteOnDutyInventory(UUID playerId);
    
    
    
    
    CompletableFuture<Void> performMaintenance();
    
    
    CompletableFuture<DatabaseStats> getStatistics();
    
    
    CompletableFuture<Void> createBackup(String backupPath);
    
    
    
    
    CompletableFuture<Void> batchSavePlayerData(List<PlayerData> playerDataList);
    
    
    CompletableFuture<List<PlayerData>> batchLoadPlayerData(List<UUID> playerIds);
    
    
    
    class DatabaseStats {
        private final int totalPlayers;
        private final int activeChases;
        private final int cachedInventories;
        private final long databaseSize;
        private final String databaseType;
        private final long lastMaintenance;
        
        public DatabaseStats(int totalPlayers, int activeChases, int cachedInventories, 
                           long databaseSize, String databaseType, long lastMaintenance) {
            this.totalPlayers = totalPlayers;
            this.activeChases = activeChases;
            this.cachedInventories = cachedInventories;
            this.databaseSize = databaseSize;
            this.databaseType = databaseType;
            this.lastMaintenance = lastMaintenance;
        }
        
        public int getTotalPlayers() { return totalPlayers; }
        public int getActiveChases() { return activeChases; }
        public int getCachedInventories() { return cachedInventories; }
        public long getDatabaseSize() { return databaseSize; }
        public String getDatabaseType() { return databaseType; }
        public long getLastMaintenance() { return lastMaintenance; }
    }
} 