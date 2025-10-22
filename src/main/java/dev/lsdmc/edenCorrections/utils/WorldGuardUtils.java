package dev.lsdmc.edenCorrections.utils;

import dev.lsdmc.edenCorrections.EdenCorrections;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.World;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.scheduler.BukkitRunnable;


public class WorldGuardUtils {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    
    private WorldGuardPlugin worldGuardPlugin;
    private RegionContainer regionContainer;
    private boolean worldGuardEnabled;
    
    
    private final Map<UUID, CachedRegionData> regionCache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = 5000L; 
    
    
    private static class CachedRegionData {
        private final Map<String, Boolean> regionResults;
        private final long timestamp;
        private final Set<String> allRegions;
        
        CachedRegionData(Map<String, Boolean> regionResults, Set<String> allRegions) {
            this.regionResults = new HashMap<>(regionResults);
            this.allRegions = new HashSet<>(allRegions);
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION_MS;
        }
        
        boolean isInRegion(String regionName) {
            return regionResults.getOrDefault(regionName, false);
        }
        
        Set<String> getAllRegions() {
            return allRegions;
        }
    }
    
    public WorldGuardUtils(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.worldGuardEnabled = false;
        
        initializeWorldGuard();
    }
    
    
    private void initializeWorldGuard() {
        try {
            
            if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") == null) {
                logger.info("WorldGuard not found - region restrictions will be disabled");
                return;
            }
            
            
            worldGuardPlugin = WorldGuardPlugin.inst();
            if (worldGuardPlugin == null) {
                logger.warning("Failed to get WorldGuard plugin instance");
                return;
            }
            
            
            regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
            if (regionContainer == null) {
                logger.warning("Failed to get WorldGuard region container");
                return;
            }
            
            worldGuardEnabled = true;
            logger.info("WorldGuard integration initialized successfully");
            
            
            startCacheCleanupTask();
            
            
            validateConfiguredRegions();
            
        } catch (Exception e) {
            logger.severe("Failed to initialize WorldGuard integration: " + e.getMessage());
            e.printStackTrace();
            worldGuardEnabled = false;
        }
    }
    
    
    private void validateConfiguredRegions() {
        if (!worldGuardEnabled) return;
        
        try {
            
            String dutyRegion = plugin.getConfigManager().getDutyRegion();
            if (!regionExists(dutyRegion)) {
                logger.warning("Configured duty region '" + dutyRegion + "' does not exist in any world");
            }
            
            
            String[] safeZones = plugin.getConfigManager().getNoChaseZones();
            for (String zone : safeZones) {
                if (!regionExists(zone.trim())) {
                    logger.warning("Configured safe zone '" + zone + "' does not exist in any world");
                }
            }
            
            
            String[] noChaseZones = plugin.getConfigManager().getNoChaseZones();
            for (String zone : noChaseZones) {
                if (!regionExists(zone.trim())) {
                    logger.warning("Configured no-chase zone '" + zone + "' does not exist in any world");
                }
            }
            
            
            String[] dutyRequiredZones = plugin.getConfigManager().getDutyRequiredZones();
            for (String zone : dutyRequiredZones) {
                if (!regionExists(zone.trim())) {
                    logger.warning("Configured duty-required zone '" + zone + "' does not exist in any world");
                }
            }
            
        } catch (Exception e) {
            logger.warning("Error validating configured regions: " + e.getMessage());
        }
    }
    
    
    private void startCacheCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                
                regionCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
            }
        }.runTaskTimer(plugin, 20L * 30L, 20L * 30L); 
    }
    
    
    
    
    public boolean isPlayerInRegion(Player player, String regionName) {
        if (!worldGuardEnabled || player == null || regionName == null) {
            return false;
        }
        
        UUID playerId = player.getUniqueId();
        
        
        CachedRegionData cached = regionCache.get(playerId);
        if (cached != null && !cached.isExpired()) {
            return cached.isInRegion(regionName);
        }
        
        
        try {
            RegionQuery query = regionContainer.createQuery();
            ApplicableRegionSet regions = query.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));
            
            Map<String, Boolean> regionResults = new HashMap<>();
            Set<String> allRegions = new HashSet<>();
            
            boolean inTargetRegion = false;
            for (ProtectedRegion region : regions) {
                String regionId = region.getId();
                allRegions.add(regionId);
                boolean isTarget = regionId.equalsIgnoreCase(regionName.trim());
                regionResults.put(regionId, true);
                if (isTarget) {
                    inTargetRegion = true;
                }
            }
            
            
            regionCache.put(playerId, new CachedRegionData(regionResults, allRegions));
            
            return inTargetRegion;
        } catch (Exception e) {
            logger.warning("Error checking if player " + player.getName() + " is in region " + regionName + ": " + e.getMessage());
            return false;
        }
    }
    
    
    public boolean isPlayerInAnyRegion(Player player, String[] regionNames) {
        if (!worldGuardEnabled || player == null || regionNames == null) {
            return false;
        }
        
        UUID playerId = player.getUniqueId();
        
        
        CachedRegionData cached = regionCache.get(playerId);
        if (cached != null && !cached.isExpired()) {
            for (String regionName : regionNames) {
                if (cached.isInRegion(regionName)) {
                    return true;
                }
            }
            return false;
        }
        
        
        for (String regionName : regionNames) {
            if (isPlayerInRegion(player, regionName)) {
                return true;
            }
        }
        
        return false;
    }
    
    
    public boolean isLocationInRegion(Location location, String regionName) {
        if (!worldGuardEnabled || location == null || regionName == null) {
            return false;
        }
        
        try {
            RegionQuery query = regionContainer.createQuery();
            ApplicableRegionSet regions = query.getApplicableRegions(BukkitAdapter.adapt(location));
            
            for (ProtectedRegion region : regions) {
                if (region.getId().equalsIgnoreCase(regionName.trim())) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            logger.warning("Error checking if location is in region " + regionName + ": " + e.getMessage());
            return false;
        }
    }
    
    
    public Set<String> getRegionsAtPlayer(Player player) {
        Set<String> regionNames = new HashSet<>();
        
        if (!worldGuardEnabled || player == null) {
            return regionNames;
        }
        
        try {
            RegionQuery query = regionContainer.createQuery();
            ApplicableRegionSet regions = query.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));
            
            for (ProtectedRegion region : regions) {
                regionNames.add(region.getId());
            }
            
        } catch (Exception e) {
            logger.warning("Error getting regions at player " + player.getName() + ": " + e.getMessage());
        }
        
        return regionNames;
    }
    
    
    public Set<String> getRegionsAtLocation(Location location) {
        Set<String> regionNames = new HashSet<>();
        
        if (!worldGuardEnabled || location == null) {
            return regionNames;
        }
        
        try {
            RegionQuery query = regionContainer.createQuery();
            ApplicableRegionSet regions = query.getApplicableRegions(BukkitAdapter.adapt(location));
            
            for (ProtectedRegion region : regions) {
                regionNames.add(region.getId());
            }
            
        } catch (Exception e) {
            logger.warning("Error getting regions at location: " + e.getMessage());
        }
        
        return regionNames;
    }
    
    
    
    
    public boolean isPlayerInDutyRegion(Player player) {
        if (!worldGuardEnabled) {
            return true; 
        }
        
        String dutyRegion = plugin.getConfigManager().getDutyRegion();
        return isPlayerInRegion(player, dutyRegion);
    }
    
    
    public boolean isPlayerInSafeZone(Player player) {
        if (!worldGuardEnabled) {
            return false; 
        }
        
        String[] safeZones = plugin.getConfigManager().getNoChaseZones();
        return isPlayerInAnyRegion(player, safeZones);
    }
    
    
    public boolean isPlayerInNoChaseZone(Player player) {
        if (!worldGuardEnabled) {
            return false; 
        }
        
        String[] noChaseZones = plugin.getConfigManager().getNoChaseZones();
        return isPlayerInAnyRegion(player, noChaseZones);
    }
    
    
    public boolean isPlayerInDutyRequiredZone(Player player) {
        if (!worldGuardEnabled) {
            return false; 
        }
        
        String[] dutyRequiredZones = plugin.getConfigManager().getDutyRequiredZones();
        return isPlayerInAnyRegion(player, dutyRequiredZones);
    }
    
    
    public boolean isPlayerInDutyRestrictedZone(Player player) {
        if (!worldGuardEnabled) {
            return false; 
        }
        
        String[] dutyRestricted = plugin.getConfigManager().getDutyRestrictedZones();
        return isPlayerInAnyRegion(player, dutyRestricted);
    }
    
    
    public boolean isPlayerInBreakZone(Player player) {
        if (!worldGuardEnabled) {
            return false; 
        }
        String[] breakZones = plugin.getConfigManager().getBreakZones();
        return isPlayerInAnyRegion(player, breakZones);
    }
    
    
    public boolean isLocationInBreakZone(Location location) {
        if (!worldGuardEnabled) {
            return false;
        }
        String[] breakZones = plugin.getConfigManager().getBreakZones();
        for (String zone : breakZones) {
            if (isLocationInRegion(location, zone)) {
                return true;
            }
        }
        return false;
    }
    
    
    public boolean isLocationInSafeZone(Location location) {
        if (!worldGuardEnabled) {
            return false;
        }
        
        String[] safeZones = plugin.getConfigManager().getNoChaseZones();
        for (String zone : safeZones) {
            if (isLocationInRegion(location, zone)) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean isLocationInAnyRegion(Location location, String[] regions) {
        if (!worldGuardEnabled || location == null || regions == null) return false;
        for (String region : regions) {
            if (isLocationInRegion(location, region)) return true;
        }
        return false;
    }
    
    
    
    
    public boolean regionExists(String regionName) {
        if (!worldGuardEnabled || regionName == null) {
            return false;
        }
        
        try {
            for (org.bukkit.World world : plugin.getServer().getWorlds()) {
                World adaptedWorld = BukkitAdapter.adapt(world);
                RegionManager regionManager = regionContainer.get(adaptedWorld);
                
                if (regionManager != null && regionManager.getRegion(regionName) != null) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            logger.warning("Error checking if region " + regionName + " exists: " + e.getMessage());
            return false;
        }
    }
    
    
    public Set<String> getAllRegionsInWorld(org.bukkit.World world) {
        Set<String> regionNames = new HashSet<>();
        
        if (!worldGuardEnabled || world == null) {
            return regionNames;
        }
        
        try {
            World adaptedWorld = BukkitAdapter.adapt(world);
            RegionManager regionManager = regionContainer.get(adaptedWorld);
            
            if (regionManager != null) {
                for (String regionName : regionManager.getRegions().keySet()) {
                    regionNames.add(regionName);
                }
            }
            
        } catch (Exception e) {
            logger.warning("Error getting regions in world " + world.getName() + ": " + e.getMessage());
        }
        
        return regionNames;
    }
    
    
    public Set<String> getAllRegions() {
        Set<String> allRegions = new HashSet<>();
        
        if (!worldGuardEnabled) {
            return allRegions;
        }
        
        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            allRegions.addAll(getAllRegionsInWorld(world));
        }
        
        return allRegions;
    }
    
    
    
    
    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }
    
    
    public WorldGuardPlugin getWorldGuardPlugin() {
        return worldGuardPlugin;
    }
    
    
    public RegionContainer getRegionContainer() {
        return regionContainer;
    }
    
    
    public void reinitialize() {
        logger.info("Reinitializing WorldGuard integration...");
        initializeWorldGuard();
    }
    
    
    public void generateDiagnosticReport() {
        logger.info("=== WorldGuard Integration Diagnostic Report ===");
        logger.info("WorldGuard Enabled: " + worldGuardEnabled);
        
        if (worldGuardEnabled) {
            logger.info("WorldGuard Plugin: " + (worldGuardPlugin != null ? "Available" : "Not Available"));
            logger.info("Region Container: " + (regionContainer != null ? "Available" : "Not Available"));
            
            
            logger.info("Configured Duty Region: " + plugin.getConfigManager().getDutyRegion());
            logger.info("Configured No-Chase Zones: " + Arrays.toString(plugin.getConfigManager().getNoChaseZones()));
            logger.info("Configured Duty-Required Zones: " + Arrays.toString(plugin.getConfigManager().getDutyRequiredZones()));
            
            
            Set<String> allRegions = getAllRegions();
            logger.info("Total Regions Found: " + allRegions.size());
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("All Regions: " + allRegions);
            }
        }
        
        logger.info("=== End WorldGuard Diagnostic Report ===");
    }
    
    
    
    
    public boolean canPlayerBeChased(Player player) {
        if (!worldGuardEnabled) {
            return true; 
        }
        
        
        return !isPlayerInSafeZone(player) && !isPlayerInNoChaseZone(player);
    }
    
    
    public boolean shouldForcePlayerOnDuty(Player player) {
        if (!worldGuardEnabled) {
            return false; 
        }
        
        
        return isPlayerInDutyRequiredZone(player);
    }
    
    
    public String getPlayerRegionContext(Player player) {
        if (!worldGuardEnabled) {
            return "WorldGuard not available";
        }
        
        Set<String> regions = getRegionsAtPlayer(player);
        if (regions.isEmpty()) {
            return "No regions (wilderness)";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("Regions: ").append(String.join(", ", regions));
        
        
        if (isPlayerInSafeZone(player)) {
            context.append(" [SAFE ZONE]");
        }
        if (isPlayerInNoChaseZone(player)) {
            context.append(" [NO CHASE]");
        }
        if (isPlayerInDutyRequiredZone(player)) {
            context.append(" [DUTY REQUIRED]");
        }
        if (isPlayerInDutyRegion(player)) {
            context.append(" [DUTY REGION]");
        }
        
        return context.toString();
    }
}
