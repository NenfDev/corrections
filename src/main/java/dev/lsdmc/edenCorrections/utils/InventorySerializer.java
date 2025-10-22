package dev.lsdmc.edenCorrections.utils;

import com.google.gson.*;
import com.google.gson.JsonParser;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.NamespacedKey;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;


public class InventorySerializer {

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Logger logger = Logger.getLogger(InventorySerializer.class.getName());

    
    public static String serializePlayerInventory(Player player) {
        try {
            PlayerInventory inventory = player.getInventory();
            JsonObject inventoryJson = new JsonObject();

            
            inventoryJson.addProperty("format", "bukkit-base64-v1");
            inventoryJson.addProperty("data", encodeInventory(inventory));

            
            JsonObject metadata = new JsonObject();
            metadata.addProperty("heldItemSlot", inventory.getHeldItemSlot());
            metadata.addProperty("timestamp", System.currentTimeMillis());
            metadata.addProperty("playerName", player.getName());
            metadata.addProperty("playerUuid", player.getUniqueId().toString());
            inventoryJson.add("metadata", metadata);

            return gson.toJson(inventoryJson);

        } catch (Exception e) {
            logger.severe("Failed to serialise inventory for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    
    public static boolean deserializePlayerInventory(Player player, String inventoryJson) {
        try {
            if (inventoryJson == null || inventoryJson.trim().isEmpty()) {
                logger.warning("Cannot deserialise null or empty inventory data for " + player.getName());
                return false;
            }

            JsonObject inventoryObj = JsonParser.parseString(inventoryJson).getAsJsonObject();
            PlayerInventory inventory = player.getInventory();

            
            inventory.clear();

            
            if (inventoryObj.has("format") && "bukkit-base64-v1".equalsIgnoreCase(inventoryObj.get("format").getAsString()) && inventoryObj.has("data")) {
                ItemStack[] contents = decodeInventory(inventoryObj.get("data").getAsString());
                if (contents != null && contents.length > 0) {
                    
                    int mainLen = Math.min(36, contents.length);
                    for (int i = 0; i < mainLen; i++) {
                        inventory.setItem(i, contents[i]);
                    }
                    
                    if (contents.length >= 40) {
                        ItemStack[] armour = new ItemStack[] { contents[36], contents[37], contents[38], contents[39] };
                        inventory.setArmorContents(armour);
                    }
                    
                    if (contents.length >= 41) {
                        inventory.setItemInOffHand(contents[40]);
                    }
                }
                
                if (inventoryObj.has("metadata")) {
                    JsonObject metadata = inventoryObj.getAsJsonObject("metadata");
                    if (metadata.has("heldItemSlot")) {
                        inventory.setHeldItemSlot(metadata.get("heldItemSlot").getAsInt());
                    }
                } else if (inventoryObj.has("heldItemSlot")) {
                    inventory.setHeldItemSlot(inventoryObj.get("heldItemSlot").getAsInt());
                }
            } else {
                
                
                if (inventoryObj.has("main")) {
                    JsonArray mainInventory = inventoryObj.getAsJsonArray("main");
                    for (int i = 0; i < mainInventory.size() && i < 36; i++) {
                        ItemStack item = deserializeItemStack(mainInventory.get(i));
                        if (item != null) {
                            inventory.setItem(i, item);
                        }
                    }
                }

                
                if (inventoryObj.has("armor")) {
                    JsonArray armourInventory = inventoryObj.getAsJsonArray("armor");
                    ItemStack[] armour = new ItemStack[4];
                    for (int i = 0; i < armourInventory.size() && i < 4; i++) {
                        armour[i] = deserializeItemStack(armourInventory.get(i));
                    }
                    inventory.setArmorContents(armour);
                }

                
                if (inventoryObj.has("offhand")) {
                    ItemStack offhand = deserializeItemStack(inventoryObj.get("offhand"));
                    if (offhand != null) {
                        inventory.setItemInOffHand(offhand);
                    }
                }

                
                if (inventoryObj.has("metadata")) {
                    JsonObject metadata = inventoryObj.getAsJsonObject("metadata");
                    if (metadata.has("heldItemSlot")) {
                        inventory.setHeldItemSlot(metadata.get("heldItemSlot").getAsInt());
                    }
                }
            }

            
            player.updateInventory();

            return true;

        } catch (Exception e) {
            logger.severe("Failed to deserialise inventory for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    
    private static String encodeInventory(PlayerInventory inventory) {
        try {
            ItemStack[] contents = new ItemStack[41];
            for (int i = 0; i < 36; i++) {
                contents[i] = inventory.getItem(i);
            }
            
            ItemStack[] armour = inventory.getArmorContents();
            for (int i = 0; i < 4; i++) {
                contents[36 + i] = (armour != null && i < armour.length) ? armour[i] : null;
            }
            
            contents[40] = inventory.getItemInOffHand();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BukkitObjectOutputStream out = new BukkitObjectOutputStream(baos);
            out.writeInt(contents.length);
            for (ItemStack stack : contents) {
                out.writeObject(stack);
            }
            out.close();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            logger.severe("Failed to encode inventory: " + e.getMessage());
            return "";
        }
    }

    
    private static ItemStack[] decodeInventory(String base64) {
        try {
            byte[] data = Base64.getDecoder().decode(base64);
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            BukkitObjectInputStream in = new BukkitObjectInputStream(bais);
            int len = in.readInt();
            ItemStack[] contents = new ItemStack[len];
            for (int i = 0; i < len; i++) {
                contents[i] = (ItemStack) in.readObject();
            }
            in.close();
            return contents;
        } catch (Exception e) {
            logger.severe("Failed to decode inventory: " + e.getMessage());
            return null;
        }
    }

    
    private static JsonElement serializeItemStackToJson(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return JsonNull.INSTANCE;
        }

        try {
            JsonObject itemJson = new JsonObject();

            
            itemJson.addProperty("type", item.getType().name());
            itemJson.addProperty("amount", item.getAmount());
            itemJson.addProperty("durability", item.getDurability());

            
            if (item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                JsonObject metaJson = new JsonObject();

                
                if (meta.hasDisplayName()) {
                    metaJson.addProperty("displayName", meta.getDisplayName());
                }

                
                if (meta.hasLore()) {
                    JsonArray loreArray = new JsonArray();
                    for (String loreLine : meta.getLore()) {
                        loreArray.add(loreLine);
                    }
                    metaJson.add("lore", loreArray);
                }

                
                JsonObject enchantmentsJson = new JsonObject();
                
                Map<Enchantment, Integer> combinedEnchants = new HashMap<>();
                
                if (item.getEnchantments() != null && !item.getEnchantments().isEmpty()) {
                    combinedEnchants.putAll(item.getEnchantments());
                }
                
                if (meta.hasEnchants()) {
                    combinedEnchants.putAll(meta.getEnchants());
                }
                for (Map.Entry<Enchantment, Integer> enchantEntry : combinedEnchants.entrySet()) {
                    try {
                        enchantmentsJson.addProperty(enchantEntry.getKey().getKey().toString(), enchantEntry.getValue());
                    } catch (Exception ignore) {
                        
                    }
                }
                if (enchantmentsJson.size() > 0) {
                    metaJson.add("enchantments", enchantmentsJson);
                }

                
                if (meta.hasCustomModelData()) {
                    metaJson.addProperty("customModelData", meta.getCustomModelData());
                }

                
                if (meta.isUnbreakable()) {
                    metaJson.addProperty("unbreakable", true);
                }

                
                if (meta.hasAttributeModifiers()) {
                    
                    
                    metaJson.addProperty("hasAttributeModifiers", true);
                }

                
                if (!meta.getPersistentDataContainer().isEmpty()) {
                    metaJson.addProperty("hasPersistentData", true);
                }

                itemJson.add("meta", metaJson);
            }

            return itemJson;

        } catch (Exception e) {
            logger.warning("Failed to serialise ItemStack: " + e.getMessage());
            return JsonNull.INSTANCE;
        }
    }

    
    private static ItemStack deserializeItemStack(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }

        try {
            JsonObject itemJson = element.getAsJsonObject();

            
            Material type = Material.valueOf(itemJson.get("type").getAsString());
            int amount = itemJson.get("amount").getAsInt();
            short durability = itemJson.has("durability") ? itemJson.get("durability").getAsShort() : 0;

            ItemStack item = new ItemStack(type, amount);
            item.setDurability(durability);

            
            if (itemJson.has("meta")) {
                JsonObject metaJson = itemJson.getAsJsonObject("meta");
                ItemMeta meta = item.getItemMeta();
                
                Map<Enchantment, Integer> unsafeEnchants = new HashMap<>();

                if (meta != null) {
                    
                    if (metaJson.has("displayName")) {
                        meta.setDisplayName(metaJson.get("displayName").getAsString());
                    }

                    
                    if (metaJson.has("lore")) {
                        JsonArray loreArray = metaJson.getAsJsonArray("lore");
                        List<String> lore = new ArrayList<>();
                        for (JsonElement loreElement : loreArray) {
                            lore.add(loreElement.getAsString());
                        }
                        meta.setLore(lore);
                    }

                    
                    if (metaJson.has("enchantments")) {
                        JsonObject enchantmentsJson = metaJson.getAsJsonObject("enchantments");
                        for (Map.Entry<String, JsonElement> entry : enchantmentsJson.entrySet()) {
                            try {
                                String keyString = entry.getKey();
                                NamespacedKey namespacedKey = null;
                                try {
                                    
                                    namespacedKey = NamespacedKey.fromString(keyString);
                                } catch (Throwable ignore) {
                                    
                                }
                                if (namespacedKey == null && keyString != null) {
                                    
                                    namespacedKey = NamespacedKey.minecraft(keyString);
                                }
                                Enchantment enchantment = namespacedKey != null ? Enchantment.getByKey(namespacedKey) : null;
                                if (enchantment != null) {
                                    int level = entry.getValue().getAsInt();
                                    
                                    if (level > enchantment.getMaxLevel()) {
                                        unsafeEnchants.put(enchantment, level);
                                    } else {
                                        
                                        try {
                                            meta.addEnchant(enchantment, level, true);
                                        } catch (Exception applyEx) {
                                            
                                            unsafeEnchants.put(enchantment, level);
                                        }
                                    }
                                }
                            } catch (Exception ex) {
                                logger.warning("Failed to deserialise enchantment: " + entry.getKey());
                            }
                        }
                    }

                    
                    if (metaJson.has("customModelData")) {
                        meta.setCustomModelData(metaJson.get("customModelData").getAsInt());
                    }

                    
                    if (metaJson.has("unbreakable") && metaJson.get("unbreakable").getAsBoolean()) {
                        meta.setUnbreakable(true);
                    }

                    
                    item.setItemMeta(meta);

                    
                    if (!unsafeEnchants.isEmpty()) {
                        for (Map.Entry<Enchantment, Integer> unsafeEntry : unsafeEnchants.entrySet()) {
                            try {
                                item.addUnsafeEnchantment(unsafeEntry.getKey(), unsafeEntry.getValue());
                            } catch (Exception ignore) {
                                
                            }
                        }
                    }
                }
            }

            return item;

        } catch (Exception e) {
            logger.warning("Failed to deserialise ItemStack: " + e.getMessage());
            return null;
        }
    }

    
    public static String createInventorySnapshot(Player player) {
        try {
            JsonObject snapshot = new JsonObject();
            PlayerInventory inventory = player.getInventory();

            
            Map<Material, Integer> itemCounts = new HashMap<>();

            
            for (int i = 0; i < 36; i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack != null && stack.getType() != Material.AIR) {
                    itemCounts.merge(stack.getType(), stack.getAmount(), Integer::sum);
                }
            }

            
            for (ItemStack armour : inventory.getArmorContents()) {
                if (armour != null && armour.getType() != Material.AIR) {
                    itemCounts.merge(armour.getType(), armour.getAmount(), Integer::sum);
                }
            }

            
            ItemStack offhand = inventory.getItemInOffHand();
            if (offhand != null && offhand.getType() != Material.AIR) {
                itemCounts.merge(offhand.getType(), offhand.getAmount(), Integer::sum);
            }

            
            JsonObject itemCountsJson = new JsonObject();
            for (Map.Entry<Material, Integer> entry : itemCounts.entrySet()) {
                itemCountsJson.addProperty(entry.getKey().name(), entry.getValue());
            }
            snapshot.add("itemCounts", itemCountsJson);

            
            snapshot.addProperty("timestamp", System.currentTimeMillis());
            snapshot.addProperty("totalItems", itemCounts.values().stream().mapToInt(Integer::intValue).sum());

            return gson.toJson(snapshot);

        } catch (Exception e) {
            logger.warning("Failed to create inventory snapshot for " + player.getName() + ": " + e.getMessage());
            return "{}";
        }
    }

    
    public static boolean hasGuardKitItems(Player player, List<Material> guardKitItems) {
        PlayerInventory inventory = player.getInventory();

        
        for (int i = 0; i < 36; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && guardKitItems.contains(item.getType())) {
                return true;
            }
        }

        
        for (ItemStack armour : inventory.getArmorContents()) {
            if (armour != null && guardKitItems.contains(armour.getType())) {
                return true;
            }
        }

        
        ItemStack offhand = inventory.getItemInOffHand();
        return offhand != null && guardKitItems.contains(offhand.getType());
    }

    
    public static int removeGuardKitItems(Player player, List<Material> guardKitItems) {
        PlayerInventory inventory = player.getInventory();
        int removedCount = 0;

        
        for (int i = 0; i < 36; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && guardKitItems.contains(item.getType())) {
                removedCount += item.getAmount();
                inventory.setItem(i, null);
            }
        }

        
        ItemStack[] armour = inventory.getArmorContents();
        for (int i = 0; i < armour.length; i++) {
            if (armour[i] != null && guardKitItems.contains(armour[i].getType())) {
                removedCount += armour[i].getAmount();
                armour[i] = null;
            }
        }
        inventory.setArmorContents(armour);

        
        ItemStack offhand = inventory.getItemInOffHand();
        if (offhand != null && guardKitItems.contains(offhand.getType())) {
            removedCount += offhand.getAmount();
            inventory.setItemInOffHand(null);
        }

        player.updateInventory();
        return removedCount;
    }

    
    public static List<Material> getCommonGuardKitItems() {
        List<Material> guardItems = new ArrayList<>();

        
        guardItems.add(Material.WOODEN_SWORD);
        guardItems.add(Material.STONE_SWORD);
        guardItems.add(Material.IRON_SWORD);
        guardItems.add(Material.DIAMOND_SWORD);
        guardItems.add(Material.NETHERITE_SWORD);
        guardItems.add(Material.BOW);
        guardItems.add(Material.CROSSBOW);
        guardItems.add(Material.TRIDENT);

        
        guardItems.add(Material.LEATHER_HELMET);
        guardItems.add(Material.LEATHER_CHESTPLATE);
        guardItems.add(Material.LEATHER_LEGGINGS);
        guardItems.add(Material.LEATHER_BOOTS);
        guardItems.add(Material.CHAINMAIL_HELMET);
        guardItems.add(Material.CHAINMAIL_CHESTPLATE);
        guardItems.add(Material.CHAINMAIL_LEGGINGS);
        guardItems.add(Material.CHAINMAIL_BOOTS);

        return guardItems;
    }
    
    
    
    
    public static String serializeItemStackToBase64(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BukkitObjectOutputStream out = new BukkitObjectOutputStream(baos);
            out.writeObject(item);
            out.close();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            logger.severe("Failed to serialize ItemStack: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    
    public static ItemStack deserializeItemStackFromBase64(String base64) {
        if (base64 == null || base64.trim().isEmpty()) {
            return null;
        }
        
        try {
            byte[] data = Base64.getDecoder().decode(base64);
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            BukkitObjectInputStream in = new BukkitObjectInputStream(bais);
            ItemStack item = (ItemStack) in.readObject();
            in.close();
            return item;
        } catch (Exception e) {
            logger.severe("Failed to deserialize ItemStack: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    
    public static String serializeItemStacks(List<ItemStack> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        
        try {
            JsonArray itemsArray = new JsonArray();
            for (ItemStack item : items) {
                if (item != null && item.getType() != Material.AIR) {
                    String serializedItem = serializeItemStackToBase64(item);
                    if (serializedItem != null) {
                        JsonObject itemWrapper = new JsonObject();
                        itemWrapper.addProperty("data", serializedItem);
                        itemWrapper.addProperty("type", item.getType().name()); 
                        itemWrapper.addProperty("amount", item.getAmount()); 
                        itemsArray.add(itemWrapper);
                    }
                }
            }
            return gson.toJson(itemsArray);
        } catch (Exception e) {
            logger.severe("Failed to serialize ItemStack list: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    
    public static List<ItemStack> deserializeItemStacks(String json) {
        List<ItemStack> items = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) {
            return items;
        }
        
        try {
            JsonArray itemsArray = JsonParser.parseString(json).getAsJsonArray();
            for (JsonElement element : itemsArray) {
                JsonObject itemWrapper = element.getAsJsonObject();
                if (itemWrapper.has("data")) {
                    String serializedItem = itemWrapper.get("data").getAsString();
                    ItemStack item = deserializeItemStackFromBase64(serializedItem);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
        } catch (Exception e) {
            logger.severe("Failed to deserialize ItemStack list: " + e.getMessage());
            e.printStackTrace();
        }
        
        return items;
    }
}