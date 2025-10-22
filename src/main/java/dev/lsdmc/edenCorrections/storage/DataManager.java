package dev.lsdmc.edenCorrections.storage;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import dev.lsdmc.edenCorrections.models.ChaseData;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.sql.SQLException;
import org.bukkit.Bukkit;

public class DataManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    
    private DatabaseHandler databaseHandler;
    
    
    private final Map<UUID, PlayerData> playerDataCache;
    private final Map<UUID, ChaseData> activeChases;
    
    
    private final Map<UUID, Long> lastCacheUpdate;
    private static final long CACHE_EXPIRY_TIME = 5 * 60 * 1000L; 
    private static final int MAX_CACHE_SIZE = 1000; 
    
    private final Object playerDataLock = new Object();
    private final Object chaseLock = new Object();
    
    public DataManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.playerDataCache = new ConcurrentHashMap<>();
        this.activeChases = new ConcurrentHashMap<>();
        this.lastCacheUpdate = new ConcurrentHashMap<>();
    }
    
    public void initialize() {
        try {
            
            initializeDatabase();
            
            
            loadExistingData();
            
            
            startCacheCleanup();
            
            logger.info("DataManager initialized successfully with " + 
                       getDatabaseType() + " database!");
            
        } catch (Exception e) {
            logger.severe("Failed to initialize DataManager: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("DataManager initialization failed", e);
        }
    }
    
    private void initializeDatabase() throws SQLException {
        String dbType = plugin.getConfigManager().getDatabaseType().toLowerCase();
        
        if ("sqlite".equalsIgnoreCase(dbType)) {
            String sqliteFile = plugin.getConfigManager().getSQLiteFile();
            databaseHandler = new SQLiteHandler(plugin, sqliteFile);
            logger.info("Using SQLite database: " + sqliteFile);
        } else if ("mysql".equalsIgnoreCase(dbType)) {
            
            String host = plugin.getConfigManager().getMySQLHost();
            int port = plugin.getConfigManager().getMySQLPort();
            String database = plugin.getConfigManager().getMySQLDatabase();
            String username = plugin.getConfigManager().getMySQLUsername();
            String password = plugin.getConfigManager().getMySQLPassword();
            
            
            databaseHandler = new MySQLHandler(plugin, host, port, database, username, password);
            logger.info("Using MySQL database: " + host + ":" + port + "/" + database);
        } else {
            throw new SQLException("Unsupported database type: " + dbType);
        }
        
        
        databaseHandler.initialize();
        logger.info("Database initialized successfully");
        
        
        if (!databaseHandler.testConnection()) {
            throw new SQLException("Database connection test failed");
        }
    }
    
    private void loadExistingData() {
        try {
            
            CompletableFuture<List<PlayerData>> playerDataFuture = databaseHandler.loadAllPlayerData();
            List<PlayerData> playerDataList = playerDataFuture.get(30, TimeUnit.SECONDS);
            
            synchronized (playerDataLock) {
                for (PlayerData playerData : playerDataList) {
                    playerDataCache.put(playerData.getPlayerId(), playerData);
                    lastCacheUpdate.put(playerData.getPlayerId(), System.currentTimeMillis());
                }
            }
            
            
            CompletableFuture<List<ChaseData>> chaseDataFuture = databaseHandler.loadAllActiveChases();
            List<ChaseData> chaseDataList = chaseDataFuture.get(30, TimeUnit.SECONDS);
            
            synchronized (chaseLock) {
                for (ChaseData chaseData : chaseDataList) {
                    activeChases.put(chaseData.getChaseId(), chaseData);
                }
            }
            
            logger.info("Loaded " + playerDataList.size() + " player records and " + 
                       chaseDataList.size() + " active chases from database");
            
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.severe("Failed to load existing data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void startCacheCleanup() {
        
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::cleanupExpiredCache, 
            6000L, 6000L); 
        
        
        int maintenanceIntervalMinutes = plugin.getConfigManager().getDatabaseMaintenanceInterval();
        long maintenanceIntervalTicks = maintenanceIntervalMinutes * 60L * 20L; 
        
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::performDatabaseMaintenance,
            maintenanceIntervalTicks, maintenanceIntervalTicks);
        
        
        if (plugin.getConfigManager().isDatabaseMaintenanceEnabled()) {
            logger.info("Database maintenance scheduled every " + maintenanceIntervalMinutes + " minutes");
        } else {
            logger.info("Database maintenance is disabled in configuration");
        }
    }
    
    private void cleanupExpiredCache() {
        long currentTime = System.currentTimeMillis();
        
        
        synchronized (playerDataLock) {
            lastCacheUpdate.entrySet().removeIf(entry -> {
                if (currentTime - entry.getValue() > CACHE_EXPIRY_TIME) {
                    playerDataCache.remove(entry.getKey());
                    return true;
                }
                return false;
            });
        }
        
        
        synchronized (playerDataLock) {
            if (playerDataCache.size() > MAX_CACHE_SIZE) {
                
                List<Map.Entry<UUID, Long>> sortedEntries = new ArrayList<>(lastCacheUpdate.entrySet());
                sortedEntries.sort(Map.Entry.comparingByValue());
                
                int toRemove = playerDataCache.size() - MAX_CACHE_SIZE;
                for (int i = 0; i < toRemove && i < sortedEntries.size(); i++) {
                    UUID playerId = sortedEntries.get(i).getKey();
                    playerDataCache.remove(playerId);
                    lastCacheUpdate.remove(playerId);
                }
                
                logger.info("Cache size limit reached, removed " + toRemove + " oldest entries");
            }
        }
        
        
        synchronized (chaseLock) {
            activeChases.entrySet().removeIf(entry -> entry.getValue().isExpired());
        }
    }
    
    private void performDatabaseMaintenance() {
        try {
            databaseHandler.performMaintenance();
        } catch (Exception e) {
            logger.warning("Database maintenance failed: " + e.getMessage());
        }
    }
    
    public void shutdown() {
        try {
            
            saveAllCachedData();
            
            
            if (databaseHandler != null) {
                databaseHandler.close();
            }
            
            logger.info("DataManager shutdown successfully!");
            
        } catch (Exception e) {
            logger.severe("Error during DataManager shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void saveAllCachedData() {
        try {
            
            List<PlayerData> playerDataList = new ArrayList<>(playerDataCache.values());
            if (!playerDataList.isEmpty()) {
                databaseHandler.batchSavePlayerData(playerDataList).get(30, TimeUnit.SECONDS);
                logger.info("Saved " + playerDataList.size() + " player records to database");
            }
            
            
            for (ChaseData chaseData : activeChases.values()) {
                databaseHandler.saveChaseData(chaseData);
            }
            
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.severe("Failed to save all cached data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    
    public PlayerData getPlayerData(UUID playerId) {
        
        PlayerData cachedData;
        synchronized (playerDataLock) {
            cachedData = playerDataCache.get(playerId);
            if (cachedData != null) {
                
                Long lastUpdate = lastCacheUpdate.get(playerId);
                if (lastUpdate != null && System.currentTimeMillis() - lastUpdate < CACHE_EXPIRY_TIME) {
                    return cachedData;
                }
            }
        }
        
        
        if (cachedData != null) {
            
            databaseHandler.loadPlayerData(playerId).thenAccept(data -> {
                if (data != null) {
                    synchronized (playerDataLock) {
                        playerDataCache.put(playerId, data);
                        lastCacheUpdate.put(playerId, System.currentTimeMillis());
                    }
                }
            }).exceptionally(throwable -> {
                logger.warning("Failed to load player data for " + playerId + ": " + throwable.getMessage());
                return null;
            });
            return cachedData;
        }
        
        
        PlayerData defaultData = new PlayerData(playerId, "Unknown");
        synchronized (playerDataLock) {
            playerDataCache.put(playerId, defaultData);
            lastCacheUpdate.put(playerId, System.currentTimeMillis());
        }
        
        
        databaseHandler.loadPlayerData(playerId).thenAccept(data -> {
            if (data != null) {
                synchronized (playerDataLock) {
                    playerDataCache.put(playerId, data);
                    lastCacheUpdate.put(playerId, System.currentTimeMillis());
                }
            }
        }).exceptionally(throwable -> {
            logger.warning("Failed to load player data for " + playerId + ": " + throwable.getMessage());
            return null;
        });
        
        return defaultData;
    }
    
    public PlayerData getOrCreatePlayerData(UUID playerId, String playerName) {
        PlayerData data = getPlayerData(playerId);
        if (data == null) {
            data = new PlayerData(playerId, playerName);
            
            savePlayerData(data);
        }
        return data;
    }
    
    public void savePlayerData(PlayerData playerData) {
        
        synchronized (playerDataLock) {
            playerDataCache.put(playerData.getPlayerId(), playerData);
            lastCacheUpdate.put(playerData.getPlayerId(), System.currentTimeMillis());
        }
        
        
        databaseHandler.savePlayerData(playerData).whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.severe("Failed to save player data for " + playerData.getPlayerName() + ": " + throwable.getMessage());
            } else if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Saved player data for " + playerData.getPlayerName());
            }
        });
    }
    
    public PlayerData getPlayerDataByName(String playerName) {
        
        synchronized (playerDataLock) {
            for (PlayerData data : playerDataCache.values()) {
                if (data.getPlayerName().equalsIgnoreCase(playerName)) {
                    return data;
                }
            }
        }
        
        
        databaseHandler.loadPlayerDataByName(playerName).thenAccept(data -> {
            if (data != null) {
                synchronized (playerDataLock) {
                    playerDataCache.put(data.getPlayerId(), data);
                    lastCacheUpdate.put(data.getPlayerId(), System.currentTimeMillis());
                }
            }
        }).exceptionally(throwable -> {
            logger.warning("Failed to load player data for " + playerName + ": " + throwable.getMessage());
            return null;
        });
        
        return null; 
    }
    
    public void deletePlayerData(UUID playerId) {
        
        synchronized (playerDataLock) {
            playerDataCache.remove(playerId);
            lastCacheUpdate.remove(playerId);
        }
        
        
        databaseHandler.deletePlayerData(playerId).whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.severe("Failed to delete player data for " + playerId + ": " + throwable.getMessage());
            }
        });
    }
    
    
    
    public ChaseData getChaseData(UUID chaseId) {
        synchronized (chaseLock) {
            return activeChases.get(chaseId);
        }
    }
    
    public ChaseData getChaseByGuard(UUID guardId) {
        return activeChases.values().stream()
                .filter(chase -> chase.getGuardId() != null && chase.getGuardId().equals(guardId) && chase.isActive())
                .findFirst()
                .orElse(null);
    }
    
    public ChaseData getChaseByTarget(UUID targetId) {
        return activeChases.values().stream()
                .filter(chase -> chase.getTargetId() != null && chase.getTargetId().equals(targetId) && chase.isActive())
                .findFirst()
                .orElse(null);
    }
    
    public void addChaseData(ChaseData chaseData) {
        activeChases.put(chaseData.getChaseId(), chaseData);
        
        
        databaseHandler.saveChaseData(chaseData).whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.severe("Failed to save chase data: " + throwable.getMessage());
            }
        });
    }
    
    public void removeChaseData(UUID chaseId) {
        ChaseData chaseData = activeChases.remove(chaseId);
        
        if (chaseData != null) {
            
            databaseHandler.saveChaseData(chaseData).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.severe("Failed to update chase data: " + throwable.getMessage());
                }
            });
        }
    }
    
    public Collection<ChaseData> getAllActiveChases() {
        synchronized (chaseLock) {
            return new ArrayList<>(activeChases.values());
        }
    }
    
    public void cleanupExpiredChases() {
        List<UUID> expiredChases = new ArrayList<>();
        
        synchronized (chaseLock) {
            for (ChaseData chase : activeChases.values()) {
                if (chase.isExpired()) {
                    expiredChases.add(chase.getChaseId());
                }
            }
        }
        
        for (UUID chaseId : expiredChases) {
            removeChaseData(chaseId);
        }
        
        
        databaseHandler.cleanupExpiredChases().whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.warning("Failed to cleanup expired chases from database: " + throwable.getMessage());
            }
        });
    }
    
    public void cleanupExpiredWantedLevels() {
        List<PlayerData> expiredPlayers = new ArrayList<>();
        
        synchronized (playerDataLock) {
            for (PlayerData playerData : playerDataCache.values()) {
                if (playerData.hasExpiredWanted()) {
                    playerData.clearWantedLevel();
                    expiredPlayers.add(playerData);
                }
            }
        }
        
        
        for (PlayerData playerData : expiredPlayers) {
            savePlayerData(playerData);
        }
    }
    
    
    
    public void savePlayerInventory(UUID playerId, String inventoryData) {
        databaseHandler.savePlayerInventory(playerId, inventoryData).whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.severe("Failed to save player inventory for " + playerId + ": " + throwable.getMessage());
            } else if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Saved inventory cache for " + playerId);
            }
        });
    }
    
    public String loadPlayerInventory(UUID playerId) {
        
        databaseHandler.loadPlayerInventory(playerId).thenAccept(inventoryData -> {
            if (inventoryData != null && plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Loaded inventory cache for " + playerId);
            }
        }).exceptionally(throwable -> {
            logger.warning("Failed to load player inventory for " + playerId + ": " + throwable.getMessage());
            return null;
        });
        
        return null; 
    }

    
    public void saveOnDutyInventory(UUID playerId, String inventoryData) {
        databaseHandler.saveOnDutyInventory(playerId, inventoryData).whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.severe("Failed to save on-duty inventory for " + playerId + ": " + throwable.getMessage());
            }
        });
    }

    public String loadOnDutyInventory(UUID playerId) {
        try {
            return databaseHandler.loadOnDutyInventory(playerId).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.warning("Failed to load on-duty inventory for " + playerId + ": " + e.getMessage());
            return null;
        }
    }

    public void deleteOnDutyInventory(UUID playerId) {
        databaseHandler.deleteOnDutyInventory(playerId).whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.severe("Failed to delete on-duty inventory for " + playerId + ": " + throwable.getMessage());
            }
        });
    }
    
    public void deletePlayerInventory(UUID playerId) {
        databaseHandler.deletePlayerInventory(playerId).whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.severe("Failed to delete player inventory for " + playerId + ": " + throwable.getMessage());
            }
        });
    }
    
    
    public boolean hasStoredInventory(UUID playerId) {
        try {
            String inventoryData = loadPlayerInventory(playerId);
            return inventoryData != null && !inventoryData.trim().isEmpty();
        } catch (Exception e) {
            logger.warning("Failed to check stored inventory for " + playerId + ": " + e.getMessage());
            return false;
        }
    }
    
    
    public List<UUID> getPlayersWithStoredInventory() {
        try {
            return databaseHandler.getPlayersWithStoredInventory().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.warning("Failed to get players with stored inventory: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    
    
    public boolean isPlayerOnDuty(UUID playerId) {
        
        PlayerData cachedData = playerDataCache.get(playerId);
        if (cachedData != null) {
            Long lastUpdate = lastCacheUpdate.get(playerId);
            if (lastUpdate != null && System.currentTimeMillis() - lastUpdate < CACHE_EXPIRY_TIME) {
                return cachedData.isOnDuty();
            }
        }
        
        PlayerData data = getPlayerData(playerId);
        return data != null && data.isOnDuty();
    }
    
    public boolean isPlayerWanted(UUID playerId) {
        
        PlayerData cachedData = playerDataCache.get(playerId);
        if (cachedData != null) {
            Long lastUpdate = lastCacheUpdate.get(playerId);
            if (lastUpdate != null && System.currentTimeMillis() - lastUpdate < CACHE_EXPIRY_TIME) {
                return cachedData.isWanted();
            }
        }
        
        PlayerData data = getPlayerData(playerId);
        return data != null && data.isWanted();
    }
    
    public boolean isPlayerBeingChased(UUID playerId) {
        
        PlayerData cachedData = playerDataCache.get(playerId);
        if (cachedData != null) {
            Long lastUpdate = lastCacheUpdate.get(playerId);
            if (lastUpdate != null && System.currentTimeMillis() - lastUpdate < CACHE_EXPIRY_TIME) {
                return cachedData.isBeingChased();
            }
        }
        
        PlayerData data = getPlayerData(playerId);
        return data != null && data.isBeingChased();
    }
    
    public boolean isGuardChasing(UUID guardId) {
        return getChaseByGuard(guardId) != null;
    }
    
    public int getActiveChaseCount() {
        return (int) activeChases.values().stream()
                .filter(ChaseData::isActive)
                .count();
    }
    
    
    
    public DatabaseHandler.DatabaseStats getDatabaseStats() {
        
        databaseHandler.getStatistics().thenAccept(stats -> {
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Database stats loaded: " + stats);
            }
        }).exceptionally(throwable -> {
            logger.warning("Failed to get database statistics: " + throwable.getMessage());
            return null;
        });
        
        return null; 
    }
    
    public String getDatabaseType() {
        if (databaseHandler instanceof SQLiteHandler) {
            return "SQLite";
        } else if (databaseHandler instanceof MySQLHandler) {
            return "MySQL";
        } else {
            return "Unknown";
        }
    }
    
    public boolean isDatabaseConnected() {
        return databaseHandler != null && databaseHandler.isConnected();
    }
    
    public boolean testDatabaseConnection() {
        return databaseHandler != null && databaseHandler.testConnection();
    }
    
    public void createDatabaseBackup(String backupPath) {
        if (databaseHandler != null) {
            databaseHandler.createBackup(backupPath).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.severe("Failed to create database backup: " + throwable.getMessage());
                } else {
                    logger.info("Database backup created successfully at: " + backupPath);
                }
            });
        }
    }
    
    public Map<String, Object> getDiagnosticInfo() {
        Map<String, Object> diagnostics = new ConcurrentHashMap<>();
        
        diagnostics.put("databaseType", getDatabaseType());
        diagnostics.put("databaseConnected", isDatabaseConnected());
        diagnostics.put("cachedPlayerData", playerDataCache.size());
        diagnostics.put("activeChases", activeChases.size());
        diagnostics.put("cacheHitRate", calculateCacheHitRate());
        
        DatabaseHandler.DatabaseStats stats = getDatabaseStats();
        if (stats != null) {
            diagnostics.put("totalPlayersInDB", stats.getTotalPlayers());
            diagnostics.put("activeChasesInDB", stats.getActiveChases());
            diagnostics.put("cachedInventoriesInDB", stats.getCachedInventories());
            diagnostics.put("databaseSizeBytes", stats.getDatabaseSize());
        }
        
        return diagnostics;
    }
    
    private double calculateCacheHitRate() {
        
        
        int totalCacheEntries = playerDataCache.size();
        int recentEntries = (int) lastCacheUpdate.values().stream()
                .filter(time -> System.currentTimeMillis() - time < CACHE_EXPIRY_TIME)
                .count();
        
        return totalCacheEntries > 0 ? (double) recentEntries / totalCacheEntries : 0.0;
    }
    
    
    
    public void batchSavePlayerData(List<PlayerData> playerDataList) {
        if (playerDataList.isEmpty()) return;
        
        
        for (PlayerData data : playerDataList) {
            playerDataCache.put(data.getPlayerId(), data);
            lastCacheUpdate.put(data.getPlayerId(), System.currentTimeMillis());
        }
        
        
        databaseHandler.batchSavePlayerData(playerDataList).whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.severe("Failed to batch save player data: " + throwable.getMessage());
            } else {
                logger.info("Batch saved " + playerDataList.size() + " player records");
            }
        });
    }
    
    public List<PlayerData> batchLoadPlayerData(List<UUID> playerIds) {
        if (playerIds.isEmpty()) return new ArrayList<>();
        
        
        databaseHandler.batchLoadPlayerData(playerIds).thenAccept(playerDataList -> {
            
            synchronized (playerDataLock) {
                for (PlayerData data : playerDataList) {
                    playerDataCache.put(data.getPlayerId(), data);
                    lastCacheUpdate.put(data.getPlayerId(), System.currentTimeMillis());
                }
            }
        }).exceptionally(throwable -> {
            logger.warning("Failed to batch load player data: " + throwable.getMessage());
            return null;
        });
        
        return new ArrayList<>(); 
    }
    
    
    
    public List<PlayerData> getOnlineGuards() {
        return playerDataCache.values().stream()
                .filter(data -> data.isOnDuty() && plugin.getServer().getPlayer(data.getPlayerId()) != null)
                .collect(java.util.stream.Collectors.toList());
    }
    
    public List<PlayerData> getWantedPlayers() {
        return playerDataCache.values().stream()
                .filter(PlayerData::isWanted)
                .collect(java.util.stream.Collectors.toList());
    }
    
    public List<PlayerData> getPlayersInChase() {
        return playerDataCache.values().stream()
                .filter(PlayerData::isBeingChased)
                .collect(java.util.stream.Collectors.toList());
    }
    
    public int getTotalGuardDutyTime() {
        return playerDataCache.values().stream()
                .mapToInt(data -> (int) (data.getTotalDutyTime() / 1000L))
                .sum();
    }
    
    public int getTotalArrests() {
        return playerDataCache.values().stream()
                .mapToInt(PlayerData::getTotalArrests)
                .sum();
    }
    
    public int getTotalViolations() {
        return playerDataCache.values().stream()
                .mapToInt(PlayerData::getTotalViolations)
                .sum();
    }
}
