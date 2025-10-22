package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;


public class BuybackNpcManager implements Listener {

    private final EdenCorrections plugin;
    private final Logger logger;

    private boolean enabled;
    private int intervalSeconds;
    private String npcName;
    private EntityType npcType;
    private final List<Location> locations = new ArrayList<>();
    private final Random random = new Random();

    private BukkitTask task;
    private UUID currentNpcId;
    private Location currentTarget;
    
    
    private boolean citizensAvailable = false;
    private boolean preferCitizens = true;
    private boolean citizensProtected = true;
    private net.citizensnpcs.api.npc.NPCRegistry citizensRegistry;
    private net.citizensnpcs.api.npc.NPC citizensNpc;

    private final NamespacedKey pdcKey;
    private final ConcurrentHashMap<UUID, Long> interactCooldown = new ConcurrentHashMap<>();
    
    
    private File locationsFile;
    private FileConfiguration locationsConfig;

    public BuybackNpcManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.pdcKey = new NamespacedKey(plugin, "buyback_npc");
    }

    public void initialize() {
        loadConfiguration();
        if (!enabled) {
            return;
        }

        PluginManager pm = plugin.getServer().getPluginManager();
        pm.registerEvents(this, plugin);

        
        try {
            if (preferCitizens && plugin.getServer().getPluginManager().getPlugin("Citizens") != null) {
                citizensAvailable = true;
                
                citizensRegistry = net.citizensnpcs.api.CitizensAPI.getNPCRegistry();
                
                pm.registerEvents(new CitizensClickListener(this), plugin);
            }
        } catch (Throwable t) {
            citizensAvailable = false; 
        }

        
        if (intervalSeconds <= 0) {
            intervalSeconds = 300; 
        }

        
        spawnOrMoveNpc();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::spawnOrMoveNpc, intervalSeconds * 20L, intervalSeconds * 20L);

        logger.info("Buyback NPC manager initialized:");
        logger.info("  - Locations: " + locations.size());
        logger.info("  - Spawn interval: " + intervalSeconds + "s");
        logger.info("  - NPC type: " + npcType);
        logger.info("  - Citizens integration: " + (citizensAvailable ? "enabled" : "disabled"));
        
        if (plugin.getConfigManager().isDebugMode()) {
            logger.info("DEBUG: Buyback NPC locations:");
            for (int i = 0; i < locations.size(); i++) {
                Location loc = locations.get(i);
                logger.info("  " + (i + 1) + ": " + loc.getWorld().getName() + " " + 
                    String.format("%.1f,%.1f,%.1f", loc.getX(), loc.getY(), loc.getZ()));
            }
        }
    }

    public void reload() {
        cleanup();
        initialize();
    }

    private void loadConfiguration() {
        try {
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("integration.buyback-npc");
            if (section == null) {
                enabled = false;
                return;
            }
            enabled = section.getBoolean("enabled", false);
            preferCitizens = section.getBoolean("prefer-citizens", true);
            intervalSeconds = section.getInt("spawn-interval-seconds", 300);
            npcName = section.getString("npc-name", "§dBuyback Broker");
            String typeName = section.getString("npc-type", "WANDERING_TRADER");
            try {
                npcType = EntityType.valueOf(typeName.toUpperCase());
            } catch (Exception ignore) {
                npcType = EntityType.WANDERING_TRADER;
            }
            ConfigurationSection citizensSec = section.getConfigurationSection("citizens");
            if (citizensSec != null) {
                citizensProtected = citizensSec.getBoolean("protected", true);
            } else {
                citizensProtected = true;
            }
            
            loadLocationsFromConfig(section);
            loadLocationsFromFile();

            if (locations.isEmpty()) {
                enabled = false;
                logger.warning("Buyback NPC disabled - no valid locations configured.");
                logger.warning("Add locations using: /corrections buybacknpc add");
                logger.warning("Or configure them in config.yml under integration.buyback-npc.locations");
            }
        } catch (Exception e) {
            enabled = false;
            logger.warning("Failed to load buyback NPC configuration: " + e.getMessage());
        }
    }

    private void ensureLocationsFile() throws IOException {
        if (!plugin.getDataFolder().exists()) {
            
            plugin.getDataFolder().mkdirs();
        }
        if (locationsFile == null) {
            locationsFile = new File(plugin.getDataFolder(), "buyback_locations.yml");
        }
        if (!locationsFile.exists()) {
            
            locationsFile.createNewFile();
        }
        locationsConfig = YamlConfiguration.loadConfiguration(locationsFile);
    }

    private void loadLocationsFromConfig(ConfigurationSection section) {
        locations.clear();
        List<String> configLocations = section.getStringList("locations");
        if (configLocations != null && !configLocations.isEmpty()) {
            for (String s : configLocations) {
                Location loc = parseLocationString(s);
                if (loc != null) {
                    locations.add(loc);
                    if (plugin.getConfigManager().isDebugMode()) {
                        logger.info("DEBUG: Loaded buyback location from config: " + s);
                    }
                }
            }
        }
    }

    private void loadLocationsFromFile() {
        try {
            ensureLocationsFile();
            List<String> locStrings = locationsConfig.getStringList("locations");
            for (String s : locStrings) {
                Location loc = parseLocationString(s);
                if (loc != null) {
                    locations.add(loc);
                    if (plugin.getConfigManager().isDebugMode()) {
                        logger.info("DEBUG: Loaded buyback location from file: " + s);
                    }
                }
            }
        } catch (IOException e) {
            logger.warning("Failed to initialize buyback locations file: " + e.getMessage());
        }
    }

    private void saveLocationsToFile() {
        try {
            ensureLocationsFile();
            List<String> locStrings = new ArrayList<>();
            for (Location l : locations) {
                String s = String.format("%s:%.1f,%.1f,%.1f,%.1f,%.1f",
                    l.getWorld() != null ? l.getWorld().getName() : "world",
                    l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
                locStrings.add(s);
            }
            locationsConfig.set("locations", locStrings);
            locationsConfig.save(locationsFile);
        } catch (Exception e) {
            logger.warning("Failed to save buyback locations: " + e.getMessage());
        }
    }

    private Location parseLocationString(String s) {
        try {
            
            
            String[] parts = s.split(":");
            String worldName;
            String coords;
            if (parts.length == 2) {
                worldName = parts[0];
                coords = parts[1];
            } else if (parts.length > 2) {
                worldName = parts[0];
                coords = s.substring(worldName.length() + 1);
            } else {
                return null;
            }
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                return null;
            }
            String[] nums = coords.split(",");
            double x = Double.parseDouble(nums[0]);
            double y = Double.parseDouble(nums[1]);
            double z = Double.parseDouble(nums[2]);
            float yaw = nums.length >= 5 ? Float.parseFloat(nums[3]) : 0f;
            float pitch = nums.length >= 5 ? Float.parseFloat(nums[4]) : 0f;
            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception ignore) {
            return null;
        }
    }

    private void spawnOrMoveNpc() {
        if (!enabled || locations.isEmpty()) return;

        Location target = pickRandomLocation();
        if (target == null) return;
        this.currentTarget = target;

        
        Chunk chunk = target.getChunk();
        if (!chunk.isLoaded()) {
            chunk.load();
        }

        if (citizensAvailable) {
            
            if (citizensNpc == null) {
                try {
                    
                    net.citizensnpcs.api.npc.NPC created = citizensRegistry.createNPC(npcType, npcName);
                    created.setProtected(citizensProtected);
                    citizensNpc = created;
                } catch (Throwable ignore) {
                    citizensAvailable = false; 
                }
            }
            if (citizensNpc != null) {
                if (!citizensNpc.isSpawned()) {
                    citizensNpc.spawn(target);
                } else {
                    citizensNpc.teleport(target, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
                }
                
                if (citizensNpc.getEntity() != null) {
                    currentNpcId = citizensNpc.getEntity().getUniqueId();
                }
                return; 
            }
        }

        
        Entity npc = getCurrentNpcEntity();
        if (npc == null || !npc.isValid() || npc.isDead() || !npc.getWorld().equals(target.getWorld())) {
            if (npc != null && npc.isValid()) {
                npc.remove();
            }
            npc = target.getWorld().spawnEntity(target, npcType);
            currentNpcId = npc.getUniqueId();
            tagAsBuybackNpc(npc);
        } else {
            npc.teleport(target);
        }

        try {
            npc.customName(net.kyori.adventure.text.Component.text(npcName));
            npc.setCustomNameVisible(true);
        } catch (Throwable ignore) {
            try {
                npc.setCustomName(npcName);
                npc.setCustomNameVisible(true);
            } catch (Throwable ignored) {}
        }

        if (npc instanceof Villager villager) {
            try {
                villager.setAI(false);
                villager.setInvulnerable(true);
                villager.setGravity(false);
                villager.setSilent(true);
            } catch (Throwable ignore) {}
        }
    }

    private void tagAsBuybackNpc(Entity entity) {
        try {
            PersistentDataContainer pdc = entity.getPersistentDataContainer();
            pdc.set(pdcKey, PersistentDataType.INTEGER, 1);
        } catch (Exception ignore) {}
    }

    private boolean isBuybackNpc(Entity entity) {
        if (entity == null) return false;
        try {
            
            if (entity.hasMetadata("NPC") && !entity.getUniqueId().equals(currentNpcId)) {
                return false;
            }
            PersistentDataContainer pdc = entity.getPersistentDataContainer();
            Integer val = pdc.get(pdcKey, PersistentDataType.INTEGER);
            if (val != null && val == 1) return true;
        } catch (Exception ignore) {}
        return currentNpcId != null && currentNpcId.equals(entity.getUniqueId());
    }

    private Location pickRandomLocation() {
        if (locations.isEmpty()) return null;
        return locations.get(random.nextInt(locations.size()));
    }

    private Entity getCurrentNpcEntity() {
        if (currentNpcId == null) return null;
        for (World world : Bukkit.getWorlds()) {
            Entity e = world.getEntities().stream().filter(en -> en.getUniqueId().equals(currentNpcId)).findFirst().orElse(null);
            if (e != null) return e;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (!isBuybackNpc(entity)) return;
        handleBuybackClick(event.getPlayer());
    }

    public void cleanup() {
        if (task != null) {
            try { task.cancel(); } catch (Exception ignore) {}
            task = null;
        }
        if (citizensNpc != null) {
            try {
                if (citizensNpc.isSpawned()) citizensNpc.despawn();
                citizensRegistry.deregister(citizensNpc);
            } catch (Throwable ignore) {}
            citizensNpc = null;
        } else {
            Entity npc = getCurrentNpcEntity();
            if (npc != null && npc.isValid()) {
                try { npc.remove(); } catch (Exception ignore) {}
            }
        }
        currentNpcId = null;
        currentTarget = null;
        interactCooldown.clear();
    }

    public List<String> getConfiguredLocationSummaries() {
        if (locations.isEmpty()) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (int i = 0; i < locations.size(); i++) {
            Location l = locations.get(i);
            String base = l.getWorld().getName() + ":" + String.format("%.1f,%.1f,%.1f", l.getX(), l.getY(), l.getZ());
            if (isCurrentTarget(l)) {
                out.add(base + " (current)");
            } else {
                out.add(base);
            }
        }
        return out;
    }

    private boolean isCurrentTarget(Location loc) {
        if (currentTarget == null || loc == null) return false;
        if (currentTarget.getWorld() == null || loc.getWorld() == null) return false;
        if (!currentTarget.getWorld().equals(loc.getWorld())) return false;
        return currentTarget.getBlockX() == loc.getBlockX()
            && currentTarget.getBlockY() == loc.getBlockY()
            && currentTarget.getBlockZ() == loc.getBlockZ();
    }

    public Location getCurrentTarget() {
        return currentTarget;
    }

    
    public void replaceStoredLocations(List<String> rawLocations) {
        locations.clear();
        if (rawLocations != null) {
            for (String s : rawLocations) {
                Location loc = parseLocationString(s);
                if (loc != null) {
                    locations.add(loc);
                }
            }
        }
        saveLocationsToFile();
    }

    
    public boolean addLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        locations.add(location);
        saveLocationsToFile();
        
        
        if (!enabled && locations.size() == 1) {
            enabled = true;
            spawnOrMoveNpc();
            if (task == null) {
                task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::spawnOrMoveNpc, intervalSeconds * 20L, intervalSeconds * 20L);
            }
            logger.info("Buyback NPC re-enabled with first location added");
        }
        
        return true;
    }

    
    public boolean removeLocation(Location location) {
        if (location == null) return false;
        
        boolean removed = locations.removeIf(loc -> 
            loc.getWorld().equals(location.getWorld()) &&
            loc.getBlockX() == location.getBlockX() &&
            loc.getBlockY() == location.getBlockY() &&
            loc.getBlockZ() == location.getBlockZ()
        );
        
        if (removed) {
            saveLocationsToFile();
            
            
            if (locations.isEmpty()) {
                enabled = false;
                cleanup();
                logger.warning("Buyback NPC disabled - no locations remaining");
            }
        }
        
        return removed;
    }

    
    public int getLocationCount() {
        return locations.size();
    }

    
    public boolean isEnabled() {
        return enabled && !locations.isEmpty();
    }

    
    private void handleBuybackClick(Player player) {
        long now = System.currentTimeMillis();
        Long last = interactCooldown.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 1000L) {
            return; 
        }
        interactCooldown.put(player.getUniqueId(), now);

        if (!player.hasPermission("edencorrections.guard.retrieve")) {
            plugin.getMessageManager().sendMessage(player, "universal.no-permission");
            return;
        }
        if (!plugin.getContrabandManager().hasStoredContraband(player.getUniqueId())) {
            plugin.getMessageManager().sendMessage(player, "retrieve.no-inventory");
            return;
        }
        boolean ok = plugin.getContrabandManager().buybackStoredContraband(player);
        if (ok) {
            plugin.getMessageManager().sendMessage(player, "retrieve.success");
        } else {
            plugin.getMessageManager().sendMessage(player, "universal.failed");
        }
    }

    
    private static class CitizensClickListener implements Listener {
        private final BuybackNpcManager manager;
        CitizensClickListener(BuybackNpcManager manager) { this.manager = manager; }

        @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.NORMAL)
        public void onNpcRightClick(net.citizensnpcs.api.event.NPCRightClickEvent event) {
            try {
                if (manager.citizensNpc != null && event.getNPC() != null && event.getNPC().getId() == manager.citizensNpc.getId()) {
                    manager.handleBuybackClick(event.getClicker());
                }
            } catch (Throwable ignore) {
                
            }
        }
    }
}


