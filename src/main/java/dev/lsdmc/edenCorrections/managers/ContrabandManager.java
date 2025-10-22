package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static dev.lsdmc.edenCorrections.managers.MessageManager.*;

public class ContrabandManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    
    private final Map<UUID, ContrabandRequest> activeRequests;
    
    private final Map<UUID, java.util.List<StoredContraband>> confiscatedStorage = new HashMap<>();
    
    private final java.util.List<StoredContraband> globalContrabandPool = new java.util.ArrayList<>();
    
    private final Map<UUID, Long> lastPotionTestByTarget = new HashMap<>();
    
    public ContrabandManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.activeRequests = new HashMap<>();
    }
    
    public void initialize() {
        logger.info("ContrabandManager initialized successfully!");
    }
    
    
    
    public boolean requestContraband(Player guard, Player target, String contrabandType) {
        
        if (!plugin.getDutyManager().isOnDuty(guard)) {
            plugin.getMessageManager().sendMessage(guard, "contraband.request.not-on-duty");
            return false;
        }
        
        
        if (!plugin.getConfigManager().isContrabandEnabled()) {
            plugin.getMessageManager().sendMessage(guard, "contraband.disabled");
            return false;
        }
        
        
        if (!plugin.getSecurityManager().canPlayerBeContrabandTargeted(target)) {
            plugin.getMessageManager().sendMessage(guard, "security.guard-immunity.contraband-protected",
                playerPlaceholder("player", target));
            plugin.getSecurityManager().logSecurityViolation("contraband request", guard, target);
            return false;
        }
        
        
        double maxDistance = plugin.getConfigManager().getMaxRequestDistance();
        if (maxDistance > 0) {
            double distance = guard.getLocation().distance(target.getLocation());
            if (distance > maxDistance) {
                plugin.getMessageManager().sendMessage(guard, "contraband.request.too-far");
                return false;
            }
        }
        
        
        if (activeRequests.containsKey(target.getUniqueId())) {
            plugin.getMessageManager().sendMessage(guard, "contraband.request.already-active");
            return false;
        }
        
        
        String itemsConfig = plugin.getConfigManager().getContrabandItems(contrabandType);
        String description = plugin.getConfigManager().getContrabandDescription(contrabandType);
        
        if (itemsConfig == null || itemsConfig.isEmpty()) {
            plugin.getMessageManager().sendMessage(guard, "contraband.request.type-disabled",
                stringPlaceholder("type", contrabandType));
            return false;
        }
        
        
        List<Material> targetItems = parseContrabandItems(itemsConfig);
        if (targetItems.isEmpty()) {
            plugin.getMessageManager().sendMessage(guard, "contraband.request.no-items",
                stringPlaceholder("type", contrabandType));
            return false;
        }
        
        
        plugin.getDutyManager().awardSearchPerformance(guard);
        
        
        return startContrabandRequest(guard, target, contrabandType, description, targetItems);
    }
    
    private boolean startContrabandRequest(Player guard, Player target, String type, String description, List<Material> targetItems) {
        int timeout = plugin.getConfigManager().getContrabandCompliance();
        
        ContrabandRequest request = new ContrabandRequest(
            guard.getUniqueId(),
            target.getUniqueId(),
            type,
            description,
            targetItems,
            System.currentTimeMillis(),
            timeout
        );
        
        activeRequests.put(target.getUniqueId(), request);
        
        
        plugin.getMessageManager().sendMessage(guard, "contraband.request.success",
            playerPlaceholder("player", target),
            stringPlaceholder("type", type),
            stringPlaceholder("description", description));
        
        plugin.getMessageManager().sendMessage(target, "contraband.request.target-notification",
            stringPlaceholder("type", type),
            stringPlaceholder("description", description),
            numberPlaceholder("seconds", timeout));
        
        
        plugin.getBossBarManager().showContrabandBossBar(target, timeout, description);
        
        
        plugin.getMessageManager().sendActionBar(target, "actionbar.contraband-request",
            stringPlaceholder("type", type),
            stringPlaceholder("description", description),
            numberPlaceholder("seconds", timeout));
        
        
        BukkitTask timeoutTask = new BukkitRunnable() {
            private int remaining = timeout;
            
            @Override
            public void run() {
                try {
                    
                    if (!target.isOnline()) {
                        activeRequests.remove(target.getUniqueId());
                        return;
                    }
                    
                    
                    if (!activeRequests.containsKey(target.getUniqueId())) {
                        return;
                    }
                    
                    if (remaining <= 0) {
                        
                        handleContrabandTimeout(request);
                        return;
                    }
                    
                    
                    plugin.getBossBarManager().updateContrabandCountdown(target, remaining, timeout, description);
                    
                    remaining--;
                } catch (Exception e) {
                    logger.severe("Error in contraband countdown for " + target.getName() + ": " + e.getMessage());
                    
                    activeRequests.remove(target.getUniqueId());
                    if (request.getTimeoutTask() != null) {
                        request.getTimeoutTask().cancel();
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); 
        
        request.setTimeoutTask(timeoutTask);
        
        logger.info("Contraband request started: " + guard.getName() + " -> " + target.getName() + " (" + type + ")");
        return true;
    }
    
    private List<Material> parseContrabandItems(String itemsConfig) {
        List<Material> materials = new ArrayList<>();
        String[] items = itemsConfig.split(",");
        
        for (String item : items) {
            try {
                Material material = Material.valueOf(item.trim().toUpperCase());
                materials.add(material);
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid material in contraband config: " + item);
            }
        }
        
        return materials;
    }
    
    
    
    public void handleItemDrop(org.bukkit.event.player.PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        
        
        if (event.isCancelled()) {
            return;
        }
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        ContrabandRequest request = activeRequests.get(player.getUniqueId());
        if (request == null) return;
        
        Material droppedMaterial = droppedItem.getType();
        
        
        if (request.getTargetItems().contains(droppedMaterial)) {
            
            request.addDroppedItem(droppedMaterial);
            
            boolean storeEnabled = plugin.getConfigManager().isContrabandConfiscationEnabled();
            if (storeEnabled) {
                try {
                    
                    
                    org.bukkit.entity.Item itemEntity = event.getItemDrop();
                    try { itemEntity.setPickupDelay(Integer.MAX_VALUE); } catch (Throwable ignore) {}
                    
                    long expiryAt = System.currentTimeMillis() + 
                        plugin.getConfigManager().getContrabandStorageDurationSeconds() * 1000L;
                    
                    
                    ItemStack itemToStore = droppedItem.clone();
                    
                    
                    StoredContraband sc = new StoredContraband(itemToStore, expiryAt);
                    confiscatedStorage.computeIfAbsent(player.getUniqueId(), k -> new java.util.ArrayList<>()).add(sc);
                    globalContrabandPool.add(sc);
                    
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try { itemEntity.remove(); } catch (Exception ignore) {}
                    });
                    
                    
                    plugin.getMessageManager().sendMessage(player, "contraband.detection.item-dropped",
                        stringPlaceholder("item", droppedMaterial.name()));
                } catch (Exception e) {
                    logger.warning("Failed to auto-confiscate compliant drop for " + player.getName() + ": " + e.getMessage());
                }
            } else {
                
                plugin.getMessageManager().sendMessage(player, "contraband.detection.item-dropped",
                    stringPlaceholder("item", droppedMaterial.name()));
            }
            
            
            if (!hasAnyRequestedItemsIncludingArmor(player, request.getTargetItems())) {
                handleContrabandCompliance(request, true);
            }
        } else {
            
            plugin.getMessageManager().sendMessage(player, "contraband.detection.wrong-item",
                stringPlaceholder("item", droppedMaterial.name()));
        }
    }

    
    private boolean hasAnyRequestedItemsIncludingArmor(Player player, java.util.List<Material> requested) {
        if (requested == null || requested.isEmpty()) return false;
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        
        ItemStack[] contents = inv.getContents();
        if (contents != null) {
            for (ItemStack stack : contents) {
                if (stack != null && requested.contains(stack.getType())) {
                    return true;
                }
            }
        }
        
        ItemStack[] armor = inv.getArmorContents();
        if (armor != null) {
            for (ItemStack stack : armor) {
                if (stack != null && requested.contains(stack.getType())) {
                    return true;
                }
            }
        }
        
        ItemStack off = inv.getItemInOffHand();
        if (off != null && requested.contains(off.getType())) {
            return true;
        }
        
        ItemStack main = inv.getItemInMainHand();
        if (main != null && requested.contains(main.getType())) {
            return true;
        }
        return false;
    }
    
    private void handleContrabandCompliance(ContrabandRequest request, boolean compliant) {
        Player target = Bukkit.getPlayer(request.getTargetId());
        Player guard = Bukkit.getPlayer(request.getGuardId());
        
        
        activeRequests.remove(request.getTargetId());
        
        
        if (request.getTimeoutTask() != null) {
            request.getTimeoutTask().cancel();
        }
        
        if (target != null) {
            
            plugin.getBossBarManager().hideBossBarByType(target, "contraband");
            
            if (compliant) {
                
                plugin.getMessageManager().sendMessage(target, "contraband.detection.compliance-success");
                plugin.getMessageManager().sendActionBar(target, "actionbar.contraband-compliance");
                
                
                logger.info("Contraband compliance successful: " + target.getName() + " dropped " + request.getDescription());
            } else {
                
                plugin.getMessageManager().sendMessage(target, "contraband.detection.compliance-failed");
                
                
                increaseWantedLevel(target, "Contraband possession: " + request.getDescription());
                
                
                if (guard != null) {
                    startChaseAfterContrabandViolation(guard, target, request);
                }
            }
        }
        
        if (guard != null) {
            if (compliant) {
                plugin.getMessageManager().sendMessage(guard, "contraband.detection.request-completed");
                
                plugin.getDutyManager().awardSuccessfulSearchPerformance(guard);
            } else {
                plugin.getMessageManager().sendMessage(guard, "contraband.detection.timeout");
                
                plugin.getDutyManager().awardSuccessfulSearchPerformance(guard);
            }
        }
        
        logger.info("Contraband request completed: " + (compliant ? "SUCCESS" : "FAILED") + 
                   " - " + request.getType());
    }
    
    private void handleContrabandTimeout(ContrabandRequest request) {
        Player target = Bukkit.getPlayer(request.getTargetId());
        Player guard = Bukkit.getPlayer(request.getGuardId());
        
        if (target != null) {
            
            boolean hasContraband = hasAnyRequestedItemsIncludingArmor(target, request.getTargetItems());
            
            if (hasContraband) {
                
                handleContrabandCompliance(request, false);
            } else {
                
                handleContrabandCompliance(request, true);
            }
        } else {
            
            activeRequests.remove(request.getTargetId());
        }
    }
    
    private void startChaseAfterContrabandViolation(Player guard, Player target, ContrabandRequest request) {
        
        if (!plugin.getChaseManager().canStartChase(guard, target)) {
            
            logger.info("Chase could not start after contraband violation for " + target.getName());
            return;
        }
        
        
        boolean chaseStarted = plugin.getChaseManager().startChase(guard, target);
        
        if (chaseStarted) {
            plugin.getMessageManager().sendMessage(guard, "contraband.chase.started",
                playerPlaceholder("target", target),
                stringPlaceholder("reason", "Contraband possession: " + request.getDescription()));
            
            plugin.getMessageManager().sendMessage(target, "contraband.chase.target-notification",
                playerPlaceholder("guard", guard),
                stringPlaceholder("reason", "Contraband possession: " + request.getDescription()));
            
            logger.info("Chase started after contraband violation: " + guard.getName() + " -> " + target.getName());
        }
    }
    
    private void increaseWantedLevel(Player player, String reason) {
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
        int currentLevel = data.getWantedLevel();
        int newLevel = Math.min(currentLevel + 1, plugin.getConfigManager().getMaxWantedLevel());
        
        
        plugin.getWantedManager().setWantedLevel(player, newLevel, reason);
        
        
        if (newLevel >= 3) {
            applyWantedGlowEffect(player, true);
        }
    }
    
    private void applyWantedGlowEffect(Player player, boolean shouldGlow) {
        if (shouldGlow) {
            
            player.setGlowing(true);
            
            
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                if (plugin.getDutyManager().isOnDuty(onlinePlayer) && 
                    plugin.getDutyManager().isSubjectToGuardRestrictions(onlinePlayer)) {
                    plugin.getMessageManager().sendMessage(onlinePlayer, "wanted.glow.notification",
                        playerPlaceholder("player", player),
                        numberPlaceholder("level", plugin.getWantedManager().getWantedLevel(player)));
                }
            }
        } else {
            player.setGlowing(false);
        }
    }
    
    
    
    public void performDrugTest(Player guard, Player target) {
        if (!plugin.getDutyManager().isOnDuty(guard)) {
            plugin.getMessageManager().sendMessage(guard, "contraband.request.not-on-duty");
            return;
        }
        
        if (!plugin.getConfigManager().isDrugDetectionEnabled()) {
            plugin.getMessageManager().sendMessage(guard, "contraband.disabled");
            return;
        }
        
        
        if (!plugin.getSecurityManager().canPlayerBeContrabandTargeted(target)) {
            plugin.getMessageManager().sendMessage(guard, "security.guard-immunity.contraband-protected",
                playerPlaceholder("player", target));
            plugin.getSecurityManager().logSecurityViolation("drug test", guard, target);
            return;
        }
        
        
        double maxDistance = plugin.getConfigManager().getMaxRequestDistance();
        if (maxDistance > 0) {
            double distance = guard.getLocation().distance(target.getLocation());
            if (distance > maxDistance) {
                plugin.getMessageManager().sendMessage(guard, "contraband.request.too-far");
                return;
            }
        }
        
        
        String[] drugItems = plugin.getConfigManager().getContrabandItems("drugs").split(",");
        boolean foundDrugs = false;
        String foundDrug = "";
        
        for (String drugItem : drugItems) {
            try {
                Material drugMaterial = Material.valueOf(drugItem.trim().toUpperCase());
                if (target.getInventory().contains(drugMaterial)) {
                    foundDrugs = true;
                    foundDrug = drugMaterial.name();
                    break;
                }
            } catch (IllegalArgumentException e) {
                
            }
        }
        
        
        plugin.getMessageManager().sendMessage(guard, "contraband.drug-test.kit-used",
            playerPlaceholder("player", target));
        
        if (foundDrugs) {
            plugin.getMessageManager().sendMessage(guard, "contraband.drug-test.positive",
                stringPlaceholder("drug", foundDrug));
            plugin.getMessageManager().sendMessage(target, "contraband.drug-test.positive",
                stringPlaceholder("drug", foundDrug));
            
            
            increaseWantedLevel(target, "Positive drug test: " + foundDrug);
            
            
            plugin.getDutyManager().awardDetectionPerformance(guard);
        } else {
            plugin.getMessageManager().sendMessage(guard, "contraband.drug-test.negative");
            plugin.getMessageManager().sendMessage(target, "contraband.drug-test.negative");
        }
        
        logger.info("Drug test performed: " + guard.getName() + " -> " + target.getName() + 
                   " (Result: " + (foundDrugs ? "POSITIVE" : "NEGATIVE") + ")");
    }
    
    public void performPotionTest(Player guard, Player target) {
        if (!plugin.getDutyManager().isOnDuty(guard)) {
            plugin.getMessageManager().sendMessage(guard, "contraband.request.not-on-duty");
            return;
        }
        
        if (!plugin.getConfigManager().isDrugDetectionEnabled()) {
            plugin.getMessageManager().sendMessage(guard, "contraband.disabled");
            return;
        }
        
        
        if (!plugin.getSecurityManager().canPlayerBeContrabandTargeted(target)) {
            plugin.getMessageManager().sendMessage(guard, "security.guard-immunity.contraband-protected",
                playerPlaceholder("player", target));
            plugin.getSecurityManager().logSecurityViolation("potion test", guard, target);
            return;
        }
        
        
        double maxDistance = plugin.getConfigManager().getMaxRequestDistance();
        if (maxDistance > 0) {
            double distance = guard.getLocation().distance(target.getLocation());
            if (distance > maxDistance) {
                plugin.getMessageManager().sendMessage(guard, "contraband.request.too-far");
                return;
            }
        }

        
        int cooldown = plugin.getConfigManager().getPotionTestCooldownSeconds();
        if (cooldown > 0) {
            long now = System.currentTimeMillis();
            Long last = lastPotionTestByTarget.get(target.getUniqueId());
            if (last != null) {
                long remaining = (last + cooldown * 1000L) - now;
                if (remaining > 0) {
                    long remainingSec = (remaining + 999) / 1000L;
                    plugin.getMessageManager().sendMessage(guard, "system.info",
                        stringPlaceholder("message", "Potion test on " + target.getName() + " available in " + remainingSec + "s"));
                    return;
                }
            }
            lastPotionTestByTarget.put(target.getUniqueId(), now);
        }
        
        
        java.util.List<String> suspiciousEffects = plugin.getConfigManager().getConfig()
            .getStringList("contraband-system.drug-test.suspicious-effects");
        boolean foundEffects = false;
        String matchedEffect = "";
        if (suspiciousEffects != null && !suspiciousEffects.isEmpty()) {
            for (org.bukkit.potion.PotionEffect effect : target.getActivePotionEffects()) {
                String typeName = effect.getType().getName();
                for (String cfg : suspiciousEffects) {
                    if (typeName.equalsIgnoreCase(cfg.trim())) {
                        foundEffects = true;
                        matchedEffect = typeName;
                        break;
                    }
                }
                if (foundEffects) break;
            }
        }
        
        
        plugin.getMessageManager().sendMessage(guard, "contraband.potion-test.kit-used",
            playerPlaceholder("player", target));
        
        if (foundEffects) {
            plugin.getMessageManager().sendMessage(guard, "contraband.potion-test.positive",
                stringPlaceholder("potion", matchedEffect));
            plugin.getMessageManager().sendMessage(target, "contraband.potion-test.positive",
                stringPlaceholder("potion", matchedEffect));

            
            increaseWantedLevel(target, "Positive potion effect test: " + matchedEffect);

            
            plugin.getDutyManager().awardDetectionPerformance(guard);
        } else {
            plugin.getMessageManager().sendMessage(guard, "contraband.potion-test.negative");
            plugin.getMessageManager().sendMessage(target, "contraband.potion-test.negative");
        }
        
        logger.info("Potion test performed: " + guard.getName() + " -> " + target.getName() + 
                   " (Result: " + (foundEffects ? "POSITIVE" : "NEGATIVE") + ")");
    }
    
    
    
    
    public void removeContrabandOnCapture(Player target) {
        boolean storeEnabled = plugin.getConfigManager().isContrabandConfiscationEnabled();
        long expiryAt = System.currentTimeMillis() + plugin.getConfigManager().getContrabandStorageDurationSeconds() * 1000L;
        java.util.List<StoredContraband> stored = new java.util.ArrayList<>();
        
        
        String[] contrabandTypes = {"sword", "bow", "armor", "drugs", "potion"};
        for (String type : contrabandTypes) {
            String itemsConfig = plugin.getConfigManager().getContrabandItems(type);
            if (itemsConfig == null || itemsConfig.isEmpty()) continue;
            List<Material> contrabandItems = parseContrabandItems(itemsConfig);
            for (Material material : contrabandItems) {
                
                ItemStack[] contents = target.getInventory().getContents();
                for (int i = 0; i < contents.length; i++) {
                    ItemStack stack = contents[i];
                    if (stack != null && stack.getType() == material) {
                        if (storeEnabled) {
                            
                            StoredContraband sc = new StoredContraband(stack, expiryAt);
                            stored.add(sc);
                            globalContrabandPool.add(sc);
                        }
                        target.getInventory().setItem(i, null);
                    }
                }
            }
        }
        target.updateInventory();
        
        if (storeEnabled && !stored.isEmpty()) {
            confiscatedStorage.computeIfAbsent(target.getUniqueId(), k -> new java.util.ArrayList<>()).addAll(stored);
            logger.info("Stored " + stored.size() + " contraband stacks for " + target.getName());
        }
        
        plugin.getMessageManager().sendMessage(target, "contraband.removal.captured");
        logger.info("Contraband removed from " + target.getName() + " upon capture");
    }
    
    
    
    public boolean hasActiveRequest(Player player) {
        return activeRequests.containsKey(player.getUniqueId());
    }
    
    public ContrabandRequest getActiveRequest(Player player) {
        return activeRequests.get(player.getUniqueId());
    }
    
    public void cancelActiveRequest(Player player) {
        ContrabandRequest request = activeRequests.remove(player.getUniqueId());
        if (request != null) {
            
            if (request.getTimeoutTask() != null) {
                request.getTimeoutTask().cancel();
            }
            
            
            plugin.getBossBarManager().hideBossBarByType(player, "contraband");
            
            logger.info("Contraband request cancelled for " + player.getName());
        }
    }
    
    
    public boolean hasContrabandItems(Player player) {
        String[] contrabandTypes = {"sword", "bow", "armor", "drugs", "potion"};
        
        for (String type : contrabandTypes) {
            String itemsConfig = plugin.getConfigManager().getContrabandItems(type);
            if (itemsConfig != null && !itemsConfig.isEmpty()) {
                List<Material> contrabandItems = parseContrabandItems(itemsConfig);
                
                for (Material material : contrabandItems) {
                    if (player.getInventory().contains(material)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    
    
    public void cleanup() {
        for (ContrabandRequest request : activeRequests.values()) {
            if (request.getTimeoutTask() != null) {
                request.getTimeoutTask().cancel();
            }
            
            Player target = Bukkit.getPlayer(request.getTargetId());
            if (target != null) {
                plugin.getBossBarManager().hideBossBarByType(target, "contraband");
            }
        }
        
        activeRequests.clear();
        
        long now = System.currentTimeMillis();
        for (java.util.Iterator<Map.Entry<UUID, java.util.List<StoredContraband>>> it = confiscatedStorage.entrySet().iterator(); it.hasNext();) {
            Map.Entry<UUID, java.util.List<StoredContraband>> e = it.next();
            e.getValue().removeIf(sc -> sc.expiresAt <= now);
            if (e.getValue().isEmpty()) it.remove();
        }
        globalContrabandPool.removeIf(sc -> sc.expiresAt <= now);
        logger.info("ContrabandManager cleaned up successfully");
    }
    
    public void cleanupPlayer(Player player) {
        cancelActiveRequest(player);
        
        
        if (plugin.getWantedManager().getWantedLevel(player) >= 3) {
            applyWantedGlowEffect(player, false);
        }
    }

    
    public boolean hasStoredContraband(UUID playerId) {
        purgeExpired(playerId);
        java.util.List<StoredContraband> list = confiscatedStorage.get(playerId);
        return list != null && !list.isEmpty();
    }
    public int getBuybackPrice(UUID playerId) {
        String pricingMode = plugin.getConfigManager().getContrabandPricingMode();
        
        if ("set".equalsIgnoreCase(pricingMode)) {
            
            int price = plugin.getConfigManager().getContrabandSetPrice();
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Using set pricing mode - price: " + price);
            }
            return price;
        } else {
            
            int price = new java.util.Random().nextInt(
                plugin.getConfigManager().getContrabandRngMaxPrice() - 
                plugin.getConfigManager().getContrabandRngMinPrice() + 1
            ) + plugin.getConfigManager().getContrabandRngMinPrice();
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Using RNG pricing mode - price: " + price + " (range: " + 
                    plugin.getConfigManager().getContrabandRngMinPrice() + "-" + 
                    plugin.getConfigManager().getContrabandRngMaxPrice() + ")");
            }
            return price;
        }
    }

    
    public int getTotalStoredContrabandCount() {
        int total = 0;
        for (java.util.List<StoredContraband> items : confiscatedStorage.values()) {
            total += items.size();
        }
        return total;
    }

    
    public int getActiveStoredContrabandPlayers() {
        return confiscatedStorage.size();
    }
    
    
    public int getActiveRequestCount() {
        return activeRequests.size();
    }
    public boolean buybackStoredContraband(Player player) {
        if (!hasStoredContraband(player.getUniqueId())) return false;
        int price = getBuybackPrice(player.getUniqueId());
        return plugin.getVaultEconomyManager().takeMoney(player, price, "Contraband buyback").join() &&
               giveStoredItems(player);
    }
    private boolean giveStoredItems(Player player) {
        String mode = plugin.getConfigManager().getContrabandConfiscationMode();
        java.util.List<StoredContraband> list = confiscatedStorage.get(player.getUniqueId());
        if (list == null || list.isEmpty()) return false;
        if ("random".equalsIgnoreCase(mode)) {
            
            purgeExpired(player.getUniqueId());
            if (globalContrabandPool.isEmpty()) return false;
            java.util.Random rng = new java.util.Random();
            int idx = rng.nextInt(globalContrabandPool.size());
            StoredContraband sc = globalContrabandPool.remove(idx);
            
            ItemStack restoredItem = sc.getItemStack();
            player.getInventory().addItem(restoredItem);
        } else {
            
            for (StoredContraband sc : list) {
                ItemStack restoredItem = sc.getItemStack();
                player.getInventory().addItem(restoredItem);
            }
            list.clear();
        }
        player.updateInventory();
        if (list.isEmpty()) {
            confiscatedStorage.remove(player.getUniqueId());
        }
        return true;
    }
    private void purgeExpired(UUID playerId) {
        java.util.List<StoredContraband> list = confiscatedStorage.get(playerId);
        if (list == null) return;
        long now = System.currentTimeMillis();
        list.removeIf(sc -> sc.expiresAt <= now);
        if (list.isEmpty()) confiscatedStorage.remove(playerId);
    }
    
    
    
    public static class ContrabandRequest {
        private final UUID guardId;
        private final UUID targetId;
        private final String type;
        private final String description;
        private final List<Material> targetItems;
        private final long startTime;
        private final int timeout;
        private final List<Material> droppedItems;
        private BukkitTask timeoutTask;
        
        public ContrabandRequest(UUID guardId, UUID targetId, String type, String description,
                               List<Material> targetItems, long startTime, int timeout) {
            this.guardId = guardId;
            this.targetId = targetId;
            this.type = type;
            this.description = description;
            this.targetItems = new ArrayList<>(targetItems);
            this.startTime = startTime;
            this.timeout = timeout;
            this.droppedItems = new ArrayList<>();
        }
        
        
        public UUID getGuardId() { return guardId; }
        public UUID getTargetId() { return targetId; }
        public String getType() { return type; }
        public String getDescription() { return description; }
        public List<Material> getTargetItems() { return targetItems; }
        public long getStartTime() { return startTime; }
        public int getTimeout() { return timeout; }
        public List<Material> getDroppedItems() { return droppedItems; }
        public BukkitTask getTimeoutTask() { return timeoutTask; }
        
        
        public void setTimeoutTask(BukkitTask timeoutTask) { this.timeoutTask = timeoutTask; }
        
        
        public void addDroppedItem(Material material) {
            if (!droppedItems.contains(material)) {
                droppedItems.add(material);
            }
        }
        
        public boolean isCompliant() {
            
            for (Material requiredItem : targetItems) {
                if (!droppedItems.contains(requiredItem)) {
                    return false;
                }
            }
            return true;
        }
        
        public long getElapsedTime() {
            return System.currentTimeMillis() - startTime;
        }
        
        public int getRemainingTime() {
            long elapsed = getElapsedTime() / 1000L;
            return Math.max(0, timeout - (int) elapsed);
        }
        
        @Override
        public String toString() {
            return "ContrabandRequest{" +
                    "guardId=" + guardId +
                    ", targetId=" + targetId +
                    ", type='" + type + '\'' +
                    ", description='" + description + '\'' +
                    '}';
        }
    }

    
    private static class StoredContraband {
        final Material material; 
        final int amount; 
        final long expiresAt;
        final String serializedItemData; 
        
        StoredContraband(ItemStack item, long expiresAt) {
            this.material = item.getType();
            this.amount = item.getAmount();
            this.expiresAt = expiresAt;
            this.serializedItemData = dev.lsdmc.edenCorrections.utils.InventorySerializer.serializeItemStackToBase64(item);
        }
        
        
        @Deprecated
        StoredContraband(Material material, int amount, long expiresAt) {
            this.material = material;
            this.amount = amount;
            this.expiresAt = expiresAt;
            this.serializedItemData = null; 
        }
        
        
        public ItemStack getItemStack() {
            if (serializedItemData != null) {
                
                ItemStack restored = dev.lsdmc.edenCorrections.utils.InventorySerializer.deserializeItemStackFromBase64(serializedItemData);
                if (restored != null) {
                    return restored;
                }
            }
            
            
            return new ItemStack(material, amount);
        }
    }
}