package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionType;
import org.bukkit.NamespacedKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;

import static dev.lsdmc.edenCorrections.managers.MessageManager.*;


public class GuardLootManager {

    private final EdenCorrections plugin;
    private final Logger logger;
    private final Random random;

    private FileConfiguration lootConfig;
    private File lootFile;

    
    private final Map<String, LootPool> lootPools;
    private boolean announceDeaths;
    private boolean dropExperience;
    private int baseExperienceAmount;
    private boolean allowInventoryRetrieval;
    private int retrievalCost;
    private long retrievalTimeLimit;
    private boolean debugMode;

    public GuardLootManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.random = new Random();
        this.lootPools = new HashMap<>();
    }

    public void initialize() {
        loadLootConfiguration();
        logger.info("GuardLootManager initialized with " + lootPools.size() + " loot pools");
    }

    private void loadLootConfiguration() {
        
        lootFile = new File(plugin.getDataFolder(), "loot.yml");
        if (!lootFile.exists()) {
            try {
                
                InputStream defaultLoot = plugin.getResource("loot.yml");
                if (defaultLoot != null) {
                    Files.copy(defaultLoot, lootFile.toPath());
                    logger.info("Created default loot.yml configuration");
                } else {
                    
                    lootFile.createNewFile();
                    logger.warning("Created empty loot.yml - please configure guard loot pools");
                }
            } catch (IOException e) {
                logger.severe("Failed to create loot.yml: " + e.getMessage());
                return;
            }
        }

        
        lootConfig = YamlConfiguration.loadConfiguration(lootFile);

        
        ConfigurationSection settings = lootConfig.getConfigurationSection("settings");
        if (settings != null) {
            announceDeaths = settings.getBoolean("announce-guard-deaths", true);
            dropExperience = settings.getBoolean("drop-experience", true);
            baseExperienceAmount = settings.getInt("experience-amount", 50);
            allowInventoryRetrieval = settings.getBoolean("allow-inventory-retrieval", true);
            retrievalCost = settings.getInt("retrieval-cost", 1000);
            retrievalTimeLimit = settings.getLong("retrieval-time-limit", 1800);
            debugMode = settings.getBoolean("debug-mode", false);
        }

        
        ConfigurationSection guardLoot = lootConfig.getConfigurationSection("guard-loot");
        if (guardLoot != null) {
            for (String rank : guardLoot.getKeys(false)) {
                ConfigurationSection rankSection = guardLoot.getConfigurationSection(rank);
                if (rankSection != null && rankSection.getBoolean("enabled", true)) {
                    LootPool pool = loadLootPool(rankSection);
                    if (pool != null) {
                        lootPools.put(rank.toLowerCase(), pool);
                        if (debugMode) {
                            logger.info("Loaded loot pool for rank: " + rank + " with " + pool.items.size() + " items");
                        }
                    }
                }
            }
        }

        logger.info("Loaded guard loot configuration with " + lootPools.size() + " loot pools");
    }

    private LootPool loadLootPool(ConfigurationSection section) {
        try {
            int minItems = section.getInt("min-items", 2);
            int maxItems = section.getInt("max-items", 4);

            List<LootItem> items = new ArrayList<>();
            List<String> itemStrings = section.getStringList("items");

            for (String itemString : itemStrings) {
                LootItem item = parseLootItem(itemString);
                if (item != null) {
                    items.add(item);
                }
            }

            if (items.isEmpty()) {
                logger.warning("Loot pool has no valid items: " + section.getCurrentPath());
                return null;
            }

            return new LootPool(minItems, maxItems, items);
        } catch (Exception e) {
            logger.warning("Failed to load loot pool " + section.getCurrentPath() + ": " + e.getMessage());
            return null;
        }
    }

    
    private LootItem parseLootItem(String itemString) {
        try {
            String[] parts = itemString.split(":" , -1);
            if (parts.length < 3) {
                logger.warning("Invalid loot item format: " + itemString);
                return null;
            }

            Material material;
            try {
                material = Material.valueOf(parts[0].toUpperCase());
            } catch (IllegalArgumentException ex) {
                logger.warning("Invalid material in loot item: " + parts[0]);
                return null;
            }
            int amount;
            int weight;
            try {
                amount = Integer.parseInt(parts[1]);
                weight = Integer.parseInt(parts[2]);
            } catch (NumberFormatException nfe) {
                logger.warning("Invalid amount or weight in loot item: " + itemString);
                return null;
            }

            Map<Enchantment, Integer> enchantments = new HashMap<>();
            String customName = null;
            PotionType potionType = null;

            
            if (parts.length > 3 && parts[3] != null && !parts[3].isEmpty()) {
                String[] tokens = parts[3].split(",");
                for (String token : tokens) {
                    String enchantPart = token.trim();
                    if (enchantPart.isEmpty()) continue;
                    if (enchantPart.contains("=")) {
                        String[] enchantData = enchantPart.split("=", 2);
                        String key = enchantData[0].toLowerCase();
                        String value = enchantData[1];

                        if (key.equals("potion")) {
                            
                            try {
                                String potionName = value.replace("minecraft:", "").toUpperCase();
                                potionType = PotionType.valueOf(potionName);
                            } catch (IllegalArgumentException e) {
                                logger.warning("Invalid potion type in " + itemString + ": " + value);
                            }
                        } else {
                            
                            try {
                                NamespacedKey namespacedKey = null;
                                try {
                                    namespacedKey = NamespacedKey.fromString(key);
                                } catch (Throwable ignore) {
                                    
                                }
                                if (namespacedKey == null) {
                                    namespacedKey = NamespacedKey.minecraft(key);
                                }
                                Enchantment enchant = namespacedKey != null ? Enchantment.getByKey(namespacedKey) : null;
                                if (enchant != null) {
                                    int level = Integer.parseInt(value);
                                    enchantments.put(enchant, level);
                                } else {
                                    logger.warning("Unknown enchantment key in loot item: " + key);
                                }
                            } catch (Exception e) {
                                logger.warning("Invalid enchantment in " + itemString + ": " + enchantPart);
                            }
                        }
                    } else {
                        
                        try {
                            String potionName = enchantPart.replace("minecraft:", "").toUpperCase();
                            potionType = PotionType.valueOf(potionName);
                        } catch (IllegalArgumentException ignored) {
                            
                        }
                    }
                }
            }

            
            if (parts.length > 4 && parts[4] != null) {
                String p4 = parts[4];
                if (p4.startsWith("name=")) {
                    customName = p4.substring(5).replace("\\'", "'");
                } else {
                    
                    try {
                        String potionName = p4.replace("minecraft:", "").toUpperCase();
                        potionType = potionType == null ? PotionType.valueOf(potionName) : potionType;
                    } catch (IllegalArgumentException ignored) {}
                }
            }

            return new LootItem(material, amount, weight, enchantments, customName, potionType);
        } catch (Exception e) {
            logger.warning("Failed to parse loot item: " + itemString + " - " + e.getMessage());
            return null;
        }
    }

    
    public void handleGuardDeath(Player guard) {
        if (!plugin.getDutyManager().isOnDuty(guard)) {
            return; 
        }

        String guardRank = plugin.getDutyManager().getPlayerGuardRank(guard);
        if (guardRank == null) {
            return;
        }

        
        LootPool lootPool = getLootPoolForRank(guardRank);
        if (lootPool == null) {
            logger.warning("No loot pool configured for guard rank: " + guardRank);
            return;
        }

        
        List<ItemStack> lootItems = generateLoot(lootPool);
        Location deathLocation = guard.getLocation();

        for (ItemStack item : lootItems) {
            deathLocation.getWorld().dropItemNaturally(deathLocation, item);
        }

        
        if (dropExperience) {
            int expAmount = calculateExperienceAmount(guardRank);
            ExperienceOrb orb = deathLocation.getWorld().spawn(deathLocation, ExperienceOrb.class);
            orb.setExperience(expAmount);
        }

        
        if (announceDeaths) {
            plugin.getMessageManager().sendGuardAlert("guard.death.announcement",
                    playerPlaceholder("player", guard),
                    stringPlaceholder("rank", guardRank),
                    stringPlaceholder("location", deathLocation.getBlockX() + "," + deathLocation.getBlockY() + "," + deathLocation.getBlockZ()));
        }

        if (debugMode) {
            logger.info("Generated " + lootItems.size() + " loot items for guard " + guard.getName() + " (rank: " + guardRank + ")");
        }
    }

    private LootPool getLootPoolForRank(String rank) {
        if (rank == null) {
            return lootPools.get("default");
        }

        
        LootPool pool = lootPools.get(rank.toLowerCase());
        if (pool != null) {
            return pool;
        }

        
        return lootPools.get("default");
    }

    private List<ItemStack> generateLoot(LootPool pool) {
        List<ItemStack> loot = new ArrayList<>();

        
        int itemCount = random.nextInt(pool.maxItems - pool.minItems + 1) + pool.minItems;

        
        int totalWeight = pool.items.stream().mapToInt(item -> item.weight).sum();

        
        for (int i = 0; i < itemCount; i++) {
            LootItem selectedItem = selectRandomItem(pool.items, totalWeight);
            if (selectedItem != null) {
                loot.add(selectedItem.createItemStack());
            }
        }

        return loot;
    }

    private LootItem selectRandomItem(List<LootItem> items, int totalWeight) {
        int randomValue = random.nextInt(totalWeight);
        int currentWeight = 0;

        for (LootItem item : items) {
            currentWeight += item.weight;
            if (randomValue < currentWeight) {
                return item;
            }
        }

        
        return items.isEmpty() ? null : items.get(0);
    }

    private int calculateExperienceAmount(String rank) {
        
        int multiplier = 1;

        switch (rank.toLowerCase()) {
            case "trainee":
                multiplier = 1;
                break;
            case "private":
                multiplier = 2;
                break;
            case "officer":
                multiplier = 3;
                break;
            case "sergeant":
                multiplier = 4;
                break;
            case "captain":
                multiplier = 5;
                break;
            
            default:
                multiplier = 1; 
                break;
        }

        return baseExperienceAmount * multiplier;
    }

    public void reloadConfiguration() {
        lootPools.clear();
        loadLootConfiguration();
        logger.info("Reloaded guard loot configuration");
    }

    
    private static class LootPool {
        final int minItems;
        final int maxItems;
        final List<LootItem> items;

        LootPool(int minItems, int maxItems, List<LootItem> items) {
            this.minItems = minItems;
            this.maxItems = maxItems;
            this.items = items;
        }
    }

    private static class LootItem {
        final Material material;
        final int amount;
        final int weight;
        final Map<Enchantment, Integer> enchantments;
        final String customName;
        final PotionType potionType;

        LootItem(Material material, int amount, int weight, Map<Enchantment, Integer> enchantments, String customName, PotionType potionType) {
            this.material = material;
            this.amount = amount;
            this.weight = weight;
            this.enchantments = enchantments != null ? enchantments : new HashMap<>();
            this.customName = customName;
            this.potionType = potionType;
        }

        
        public ItemStack createItemStack() {
            ItemStack item = new ItemStack(material, amount);
            ItemMeta meta = item.getItemMeta();

            
            if (potionType != null && meta instanceof PotionMeta) {
                PotionMeta potionMeta = (PotionMeta) meta;
                potionMeta.setBasePotionType(potionType);
            }

            
            if (!enchantments.isEmpty()) {
                for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                    Enchantment enchant = entry.getKey();
                    int level = entry.getValue();
                    try {
                        if (enchant != null) {
                            if (meta != null && level <= enchant.getMaxLevel()) {
                                
                                meta.addEnchant(enchant, level, true);
                            } else {
                                
                                item.addUnsafeEnchantment(enchant, level);
                            }
                        }
                    } catch (Exception e) {
                        
                        try {
                            item.addUnsafeEnchantment(enchant, level);
                        } catch (Exception ignore) {
                            
                        }
                    }
                }
            }

            
            if (customName != null && !customName.isEmpty() && meta != null) {
                meta.displayName(Component.text(customName).decoration(TextDecoration.ITALIC, false));
            }

            
            if (meta != null) {
                item.setItemMeta(meta);
            }

            return item;
        }
    }
}