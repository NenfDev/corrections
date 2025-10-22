package dev.lsdmc.edenCorrections.events;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import dev.lsdmc.edenCorrections.managers.MessageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.Bukkit;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static dev.lsdmc.edenCorrections.managers.MessageManager.*;
import org.bukkit.Location;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryAction;
import java.util.UUID;

public class GuardEventHandler implements Listener {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    
    private final List<String> blockedChaseCommands = Arrays.asList(
        "/spawn", "/home", "/tpa", "/tpaccept", "/warp", "/back", "/rtp"
    );
    
    public GuardEventHandler(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    

    private boolean isInBreakZone(Player player) {
        try {
            return plugin.getWorldGuardUtils().isPlayerInBreakZone(player);
        } catch (Exception ignored) {
            return false;
        }
    }
    
    
    private boolean isRealPlayer(Player player) {
        if (player == null) {
            return false;
        }
        
        
        if (player.hasMetadata("NPC")) {
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Detected NPC (Citizens): " + player.getName() + " - ignoring for wanted system");
            }
            return false;
        }
        
        
        try {
            if (plugin.getServer().getPluginManager().getPlugin("Citizens") != null) {
                if (net.citizensnpcs.api.CitizensAPI.getNPCRegistry().isNPC(player)) {
                    if (plugin.getConfigManager().isDebugMode()) {
                        logger.info("DEBUG: Detected NPC (Citizens API): " + player.getName() + " - ignoring for wanted system");
                    }
                    return false;
                }
            }
        } catch (Exception e) {
            
        }
        
        
        
        
        return true;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
        
        
        if (data.hasExpiredWanted()) {
            data.clearWantedLevel();
            plugin.getDataManager().savePlayerData(data);
        }
        
        
        handleInventoryOnJoin(player, data);
        
        
        
        if (plugin.getConfigManager().isDebugMode()) {
            logger.info("Player " + player.getName() + " joined - Data loaded/created");
        }
    }
    
    
    private void handleInventoryOnJoin(Player player, PlayerData data) {
        UUID playerId = player.getUniqueId();
        
        
        boolean hasStoredInventory = plugin.getDataManager().hasStoredInventory(playerId);
        
        if (data.isOnDuty()) {
            
            if (plugin.getConfigManager().isGuardDefaultOnDuty()) {
                
                
                boolean restoredOnDutyInventory = plugin.getDutyManager().restoreOnDutyInventoryPublic(player);
                
                if (!restoredOnDutyInventory) {
                    
                    player.getInventory().clear();
                    String guardRank = data.getGuardRank();
                    if (guardRank != null) {
                        plugin.getDutyManager().giveGuardKitPublic(player, guardRank);
                        if (plugin.getConfigManager().isDebugMode()) {
                            logger.info("DEBUG: Gave fresh guard kit to " + player.getName() + " on login (no stored on-duty inventory)");
                        }
                    }
                } else {
                    if (plugin.getConfigManager().isDebugMode()) {
                        logger.info("DEBUG: Restored on-duty inventory for " + player.getName() + " (including earned items)");
                    }
                }
                
                
                if (hasStoredInventory) {
                    plugin.getDataManager().deletePlayerInventory(playerId);
                    if (plugin.getConfigManager().isDebugMode()) {
                        logger.info("DEBUG: Cleared off-duty inventory for " + player.getName() + " (staying on duty)");
                    }
                }
                
                
                data.setDutyStartTime(System.currentTimeMillis());
                plugin.getDataManager().savePlayerData(data);
                
                
                
                logger.info("Guard " + player.getName() + " logged in while on duty - restored inventory and guard status");
            } else {
                
                if (hasStoredInventory) {
                    logger.info("Player " + player.getName() + " was on duty before restart with stored inventory - properly ending duty");
                    
                    
                    long sessionDutyTime = 0;
                    if (data.getDutyStartTime() > 0) {
                        sessionDutyTime = System.currentTimeMillis() - data.getDutyStartTime();
                        data.addDutyTime(sessionDutyTime);
                    }
                    
                    
                    boolean offDutySuccess = plugin.getDutyManager().goOffDuty(player, data);
                    
                    if (offDutySuccess) {
                        plugin.getMessageManager().sendMessage(player, "duty.deactivation.success");
                        plugin.getMessageManager().sendMessage(player, "system.info", 
                            MessageManager.stringPlaceholder("message", "Your duty was ended due to server restart"));
                            
                        if (sessionDutyTime > 0) {
                            String timeStr = String.format("%.1f", sessionDutyTime / 60000.0);
                            plugin.getMessageManager().sendMessage(player, "system.info",
                                MessageManager.stringPlaceholder("message", "Duty time from last session: " + timeStr + " minutes"));
                        }
                    } else {
                        
                        logger.warning("Normal off-duty process failed for " + player.getName() + " - forcing cleanup");
                        plugin.getDutyManager().restorePlayerInventoryPublic(player);
                        data.setOnDuty(false);
                        data.setDutyStartTime(0);
                        
                        plugin.getMessageManager().sendMessage(player, "system.info", 
                            MessageManager.stringPlaceholder("message", "Your inventory was restored after server restart"));
                    }
                } else {
                    
                    if (!plugin.getDutyManager().hasGuardKitItems(player)) {
                        String guardRank = data.getGuardRank();
                        if (guardRank != null) {
                            plugin.getDutyManager().giveGuardKitPublic(player, guardRank);
                            if (plugin.getConfigManager().isDebugMode()) {
                                logger.info("DEBUG: Restored guard kit for " + player.getName() + " on join (was on duty)");
                            }
                        }
                    }
                }
            }
            
        } else {
            
            if (hasStoredInventory) {
                plugin.getDutyManager().restorePlayerInventoryPublic(player);
                if (plugin.getConfigManager().isDebugMode()) {
                    logger.info("DEBUG: Restored original inventory for " + player.getName() + " on join (was off duty)");
                }
            }
            
            
            if (plugin.getDutyManager().isSubjectToGuardRestrictions(player)) {
                if (!data.hasAvailableOffDutyTime()) {
                    
                    if (plugin.getDutyManager().initiateGuardDuty(player)) {
                        plugin.getMessageManager().sendMessage(player, "duty.auto-activated");
                        logger.info("Auto-activated duty for guard " + player.getName() + " (no available off-duty time)");
                    }
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        
        if (plugin.getDutyManager().isSubjectToGuardRestrictions(player) && plugin.getDutyManager().isOnDuty(player)) {
            PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
            if (data != null) {
                if (plugin.getConfigManager().isGuardDefaultOnDuty()) {
                    
                    long currentTime = System.currentTimeMillis();
                    if (data.getDutyStartTime() > 0) {
                        long sessionDutyTime = currentTime - data.getDutyStartTime();
                        data.addDutyTime(sessionDutyTime);
                        
                        if (plugin.getConfigManager().isDebugMode()) {
                            logger.info("DEBUG: Guard " + player.getName() + " logged off while on duty - tracked " + 
                                       (sessionDutyTime / 60000L) + " minutes of duty time");
                        }
                    }
                    
                    
                    data.setDutyStartTime(currentTime);
                    
                    
                    
                    if (!plugin.getDutyManager().storeOnDutyInventoryPublic(player)) {
                        logger.warning("Failed to store on-duty inventory for " + player.getName() + " during logout");
                    }
                    
                    logger.info("Guard " + player.getName() + " logged off while on duty - maintaining duty status and inventory");
                } else {
                    
                    data.setOnDuty(false);
                    data.setOffDutyTime(System.currentTimeMillis());
                    
                    
                    data.setHasBeenNotifiedOfExpiredTime(false);
                    
                    
                    plugin.getDutyManager().restorePlayerInventoryPublic(player);
                    
                    logger.info("Automatically set guard " + player.getName() + " to off duty on logout (old behavior)");
                }
                
                
                plugin.getDataManager().savePlayerData(data);
            }
        }
        
        
        if (plugin.getDataManager().isPlayerBeingChased(player.getUniqueId())) {
            
            plugin.getChaseManager().endChase(
                plugin.getDataManager().getChaseByTarget(player.getUniqueId()).getChaseId(),
                "Target disconnected"
            );
        } else if (plugin.getDataManager().isGuardChasing(player.getUniqueId())) {
            
            plugin.getChaseManager().endChase(
                plugin.getDataManager().getChaseByGuard(player.getUniqueId()).getChaseId(),
                "Guard disconnected"
            );
        }
        
        
        if (plugin.getJailManager().isInJailCountdown(player)) {
            plugin.getJailManager().cancelJailCountdown(player);
        }
        
        
        if (plugin.getJailManager().isInArrestMinigame(player)) {
            plugin.getJailManager().stopArrestMinigamePublic(player);
        }
        
        
        cleanupPlayerSystems(player);
        
        
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data != null) {
            plugin.getDataManager().savePlayerData(data);
        }
        
        if (plugin.getConfigManager().isDebugMode()) {
            logger.info("Player " + player.getName() + " left - Data saved and systems cleaned up");
        }
    }
    
    private void cleanupPlayerSystems(Player player) {
        
        plugin.getDutyManager().cleanupPlayer(player);
        
        
        plugin.getChaseManager().cleanupPlayer(player);
        
        
        plugin.getContrabandManager().cleanupPlayer(player);
        
        
        plugin.getBossBarManager().cleanupPlayer(player);
        
        
        plugin.getMessageManager().cleanupPlayer(player);
        
        
        plugin.getCMIIntegration().cleanupPlayerAttachments(player);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        
        
        if (plugin.getDutyManager().isSubjectToGuardRestrictions(victim) && plugin.getDutyManager().isOnDuty(victim)) {
            
            event.getDrops().clear();
            event.setDroppedExp(0);
            
            
            plugin.getGuardLootManager().handleGuardDeath(victim);
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("Guard " + victim.getName() + " died on duty - using loot system instead of inventory drops");
            }
        }
        
        
        if (killer != null && killer != victim && isRealPlayer(killer) && isRealPlayer(victim)) {
            
            handlePlayerKill(killer, victim);
        }
        
        
        if (plugin.getDataManager().isPlayerBeingChased(victim.getUniqueId())) {
            plugin.getChaseManager().endChase(
                plugin.getDataManager().getChaseByTarget(victim.getUniqueId()).getChaseId(),
                "Target died"
            );
        } else if (plugin.getDataManager().isGuardChasing(victim.getUniqueId())) {
            plugin.getChaseManager().endChase(
                plugin.getDataManager().getChaseByGuard(victim.getUniqueId()).getChaseId(),
                "Guard died"
            );
        }
        
        
        try {
            plugin.getWantedManager().clearWantedLevel(victim);
        } catch (Exception ignored) {}

        
        cleanupPlayerSystems(victim);
        
        
        if (killer != null) {
            plugin.getChaseManager().endCombatTimer(victim);
            plugin.getChaseManager().endCombatTimer(killer);
        }
    }
    
    private void handlePlayerKill(Player killer, Player victim) {
        String killerRank = plugin.getDutyManager().getPlayerGuardRank(killer);
        String victimRank = plugin.getDutyManager().getPlayerGuardRank(victim);
        boolean killerIsGuard = killerRank != null;
        boolean victimIsGuard = victimRank != null && plugin.getDutyManager().isOnDuty(victim);
        
        if (victimIsGuard && !killerIsGuard) {
            
            plugin.getWantedManager().handlePlayerKillGuard(killer, victim);
            logger.info("NON-GUARD KILLED GUARD: " + killer.getName() + " killed guard " + 
                       victim.getName() + " (" + victimRank + ")");
                       
        } else if (victimIsGuard && killerIsGuard) {
            
            logger.warning("GUARD VS GUARD COMBAT: Guard " + killer.getName() + " (" + killerRank + 
                          ") killed guard " + victim.getName() + " (" + victimRank + ")");
            
            
        } else if (!victimIsGuard && killerIsGuard) {
            
            logger.info("Guard " + killer.getName() + " (" + killerRank + ") killed player " + victim.getName());
            
            
            plugin.getDutyManager().awardKillPerformance(killer, victim);
            
            try {
                boolean qualifies = plugin.getWantedManager().isWanted(victim) ||
                        plugin.getDataManager().isPlayerBeingChased(victim.getUniqueId());
                if (qualifies) {
                    dev.lsdmc.edenCorrections.models.PlayerData pdata = plugin.getDataManager().getPlayerData(killer.getUniqueId());
                    if (pdata != null) {
                        pdata.incrementQualifyingKills();
                        plugin.getDataManager().savePlayerData(pdata);
                    }
                }
            } catch (Exception ignored) {}
            
        } else {
            
            plugin.getWantedManager().handlePlayerKillPlayer(killer, victim);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }
        
        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();
        
        
        if (isInBreakZone(victim) || isInBreakZone(attacker)) {
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Combat in break zone - restrictions skipped");
            }
            return;
        }
        
        
        if (!plugin.getSecurityManager().canGuardAttackGuard(attacker, victim)) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(attacker, "security.guard-immunity.guard-to-guard-blocked",
                playerPlaceholder("player", victim));
            plugin.getSecurityManager().logSecurityViolation("guard-to-guard attack", attacker, victim);
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Guard-to-guard attack blocked - " + attacker.getName() + " attacked " + victim.getName());
            }
            return;
        }
        
        
        if (!plugin.getSecurityManager().canPlayerBeAttacked(victim)) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(attacker, "security.guard-immunity.combat-protected",
                playerPlaceholder("player", victim));
            plugin.getSecurityManager().logSecurityViolation("attack", attacker, victim);
            return;
        }
        
        
        boolean victimInChase = plugin.getDataManager().isPlayerBeingChased(victim.getUniqueId()) || 
                               plugin.getDataManager().isGuardChasing(victim.getUniqueId());
        boolean attackerInChase = plugin.getDataManager().isPlayerBeingChased(attacker.getUniqueId()) || 
                                 plugin.getDataManager().isGuardChasing(attacker.getUniqueId());
        
        if (victimInChase) {
            plugin.getChaseManager().handleCombatEvent(victim);
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Combat timer activated for " + victim.getName() + " (involved in chase)");
            }
        }
        
        if (attackerInChase) {
            plugin.getChaseManager().handleCombatEvent(attacker);
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Combat timer activated for " + attacker.getName() + " (involved in chase)");
            }
        }
        
        
        if (plugin.getConfigManager().isDebugMode() && !victimInChase && !attackerInChase) {
            logger.info("DEBUG: PvP between " + attacker.getName() + " and " + victim.getName() + 
                       " - no combat timer (not involved in chases)");
        }
        
        
        if (!event.isCancelled() && plugin.getDutyManager().isSubjectToGuardRestrictions(victim) && plugin.getDutyManager().isOnDuty(victim)) {
            
            String attackerRank = plugin.getDutyManager().getPlayerGuardRank(attacker);
            String victimRank = plugin.getDutyManager().getPlayerGuardRank(victim);
            
            if (attackerRank == null) {
                
                logger.info("Non-guard " + attacker.getName() + " attacked guard " + victim.getName());
                
            } else {
                
                logger.info("Guard " + attacker.getName() + " (" + attackerRank + ") attacked guard " + 
                           victim.getName() + " (" + victimRank + ")");
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        
        if (!plugin.getDataManager().isPlayerBeingChased(player.getUniqueId()) && 
            !plugin.getDutyManager().isOnDuty(player)) {
            return; 
        }

        
        try {
            if (plugin.getJailManager() != null && plugin.getJailManager().hasJailTeleportBypass(player)) {
                return;
            }
        } catch (Throwable ignored) {}

        
        
        if (plugin.getChaseManager().isPlayerInChase(player) && plugin.getConfigManager().isChaseTeleportEnabled()) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "chase.blocking.teleport");
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Blocked teleport during chase for " + player.getName() +
                           " - Cause: " + event.getCause());
            }
            return;
        }

        
        if (isInBreakZone(player) || plugin.getWorldGuardUtils().isLocationInBreakZone(event.getTo())) {
            return;
        }
        
        
        if (plugin.getDutyManager().isSubjectToGuardRestrictions(player) && plugin.getDutyManager().isOnDuty(player)) {
            org.bukkit.Location to = event.getTo();
            if (to != null) {
                String[] restricted = plugin.getConfigManager().getDutyRestrictedZones();
                for (String region : restricted) {
                    if (plugin.getWorldGuardUtils().isLocationInRegion(to, region)) {
                        event.setCancelled(true);
                        plugin.getMessageManager().sendMessage(player, "restrictions.duty-region-entry");
                        return;
                    }
                }

                
                String guardRank = plugin.getDutyManager().getPlayerGuardRank(player);
                String[] disallowed = plugin.getConfigManager().getDutyDisallowedZonesForRank(guardRank);
                if (disallowed != null && disallowed.length > 0) {
                    java.util.Set<String> toRegions = plugin.getWorldGuardUtils().getRegionsAtLocation(to);
                    java.util.Set<String> disallowedLower = new java.util.HashSet<>();
                    for (String d : disallowed) if (d != null) disallowedLower.add(d.trim().toLowerCase());
                    for (String regionName : toRegions) {
                        if (disallowedLower.contains(regionName.toLowerCase())) {
                            if (plugin.getConfigManager().isDebugMode()) {
                                logger.info("DEBUG: Blocking on-duty rank teleport for " + player.getName() + " (" + guardRank + ") into region " + regionName);
                            }
                            event.setCancelled(true);
                            plugin.getMessageManager().sendMessage(player, "restrictions.duty-rank-zone-entry");
                            return;
                        }
                    }
                }
            }
        }

        
        if (plugin.getDutyManager().isSubjectToGuardRestrictions(player) && !plugin.getDutyManager().isOnDuty(player)) {
            org.bukkit.Location to = event.getTo();
            if (to != null) {
                String[] required = plugin.getConfigManager().getDutyRequiredZones();
                for (String region : required) {
                    if (plugin.getWorldGuardUtils().isLocationInRegion(to, region)) {
                        event.setCancelled(true);
                        plugin.getMessageManager().sendMessage(player, "duty.restrictions.must-return-to-duty");
                        return;
                    }
                }
            }
        }

        
        if (!plugin.getSecurityManager().canPlayerBeTeleported(player)) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "security.guard-immunity.teleport-blocked");
            plugin.getSecurityManager().logSecurityViolation("teleport", player, null);
            return;
        }
        
        
        if (plugin.getJailManager().isInArrestMinigame(player)) {
            
            if (event.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN && 
                event.getCause() != PlayerTeleportEvent.TeleportCause.COMMAND) {
                event.setCancelled(true);
                plugin.getMessageManager().sendMessage(player, "jail.restrictions.no-teleport-during-arrest");
                
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("DEBUG: Blocked teleport during arrest minigame for " + player.getName() + " - Cause: " + event.getCause());
                }
                return;
            }
        }
        
        
        
        
        
        if (plugin.getWantedManager().isWanted(player) && plugin.getConfigManager().isWantedTeleportEnabled()) {
            
            if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN || 
                event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND) {
                
                double allowedDistance = plugin.getConfigManager().getAllowedPluginTeleportDistance();
                if (allowedDistance > 0) {
                    double distance = event.getFrom().distance(event.getTo());
                    if (distance <= allowedDistance) {
                        
                        return;
                    }
                }
            }
            
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "wanted.blocking.teleport");
            return;
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();
        
        
        if (isInBreakZone(player)) {
            return;
        }
        
        
        if (plugin.getChaseManager().isPlayerInChase(player)) {
            for (String blockedCommand : blockedChaseCommands) {
                if (command.startsWith(blockedCommand)) {
                    event.setCancelled(true);
                    plugin.getMessageManager().sendMessage(player, "chase.blocking.command");
                    return;
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        
        if (isInBreakZone(player)) {
            return;
        }
        
        
        if (plugin.getDutyManager().isSubjectToGuardRestrictions(player) && plugin.getDutyManager().isOnDuty(player)) {
            if (plugin.getConfigManager().isGuardMiningBlocked()) {
                event.setCancelled(true);
                plugin.getMessageManager().sendMessage(player, "restrictions.mining");
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        
        
        if (isInBreakZone(player)) {
            return;
        }
        
        
        if (plugin.getDutyManager().isSubjectToGuardRestrictions(player) && plugin.getDutyManager().isOnDuty(player)) {
            
            
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        
        
        if (isInBreakZone(player)) {
            return;
        }
        
        
        if (plugin.getDutyManager().isSubjectToGuardRestrictions(player) && plugin.getDutyManager().isOnDuty(player)) {
            if (plugin.getConfigManager().isGuardStorageBlocked()) {
                org.bukkit.inventory.InventoryHolder holder = event.getInventory().getHolder();
                java.util.List<org.bukkit.block.Block> backingBlocks = new java.util.ArrayList<>();

                
                if (holder instanceof org.bukkit.block.BlockState) {
                    backingBlocks.add(((org.bukkit.block.BlockState) holder).getBlock());
                } else if (holder instanceof org.bukkit.block.Container) {
                    backingBlocks.add(((org.bukkit.block.Container) holder).getBlock());
                } else if (holder instanceof org.bukkit.block.DoubleChest) {
                    org.bukkit.block.DoubleChest dc = (org.bukkit.block.DoubleChest) holder;
                    org.bukkit.inventory.InventoryHolder left = dc.getLeftSide();
                    org.bukkit.inventory.InventoryHolder right = dc.getRightSide();
                    if (left instanceof org.bukkit.block.Chest) {
                        backingBlocks.add(((org.bukkit.block.Chest) left).getBlock());
                    }
                    if (right instanceof org.bukkit.block.Chest) {
                        backingBlocks.add(((org.bukkit.block.Chest) right).getBlock());
                    }
                }

                
                if (backingBlocks.isEmpty()) {
                    return;
                }

                boolean allowed = false;
                if (plugin.getLockerManager() != null) {
                    for (org.bukkit.block.Block b : backingBlocks) {
                        if (plugin.getLockerManager().isAllowedLocker(b, player)) {
                            allowed = true;
                            break;
                        }
                    }
                }

                if (!allowed) {
                    event.setCancelled(true);
                    plugin.getMessageManager().sendMessage(player, "restrictions.storage");
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        
        
        if (isInBreakZone(player)) {
            return;
        }
        
        
        if (plugin.getDutyManager().isSubjectToGuardRestrictions(player) && plugin.getDutyManager().isOnDuty(player)) {
            if (plugin.getConfigManager().isGuardCraftingBlocked()) {
                
                if (event.getClickedInventory() != null) {
                    InventoryType clickedType = event.getClickedInventory().getType();
                    if (clickedType == InventoryType.WORKBENCH || 
                        clickedType == InventoryType.ENCHANTING ||
                        clickedType == InventoryType.ANVIL) {
                        
                        if (event.getSlot() == 0 || 
                            (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && 
                             event.getClickedInventory().getType() == InventoryType.WORKBENCH)) {
                            event.setCancelled(true);
                            plugin.getMessageManager().sendMessage(player, "restrictions.crafting");
                        }
                    }
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        
        
        if (isInBreakZone(player)) {
            return;
        }
        
        
        if (plugin.getContrabandManager().hasActiveRequest(player)) {
            plugin.getContrabandManager().handleItemDrop(event);
            return;
        }
        
        
        if (plugin.getDutyManager().isSubjectToGuardRestrictions(player) && plugin.getDutyManager().isOnDuty(player)) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "restrictions.dropping");
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        
        if (!plugin.getDataManager().isPlayerBeingChased(player.getUniqueId()) && 
            !plugin.getDutyManager().isOnDuty(player)) {
            return; 
        }
        
        
        if (isInBreakZone(player)) {
            return;
        }
        
        
        if (plugin.getDutyManager().isSubjectToGuardRestrictions(player) && plugin.getDutyManager().isOnDuty(player)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to != null && !isInBreakZone(player)) {
                
                String[] restricted = plugin.getConfigManager().getDutyRestrictedZones();
                for (String region : restricted) {
                    boolean entering = plugin.getWorldGuardUtils().isLocationInRegion(to, region) &&
                                       !plugin.getWorldGuardUtils().isLocationInRegion(from, region);
                    if (entering) {
                        event.setCancelled(true);
                        plugin.getMessageManager().sendMessage(player, "restrictions.duty-region-entry");
                        return;
                    }
                }

                
                String guardRank = plugin.getDutyManager().getPlayerGuardRank(player);
                String[] disallowed = plugin.getConfigManager().getDutyDisallowedZonesForRank(guardRank);
                if (disallowed != null && disallowed.length > 0) {
                    java.util.Set<String> toRegions = plugin.getWorldGuardUtils().getRegionsAtLocation(to);
                    java.util.Set<String> fromRegions = plugin.getWorldGuardUtils().getRegionsAtLocation(from);
                    java.util.Set<String> fromRegionsLower = new java.util.HashSet<>();
                    for (String r : fromRegions) fromRegionsLower.add(r.toLowerCase());
                    java.util.Set<String> disallowedLower = new java.util.HashSet<>();
                    for (String d : disallowed) if (d != null) disallowedLower.add(d.trim().toLowerCase());
                    for (String regionName : toRegions) {
                        String regionLower = regionName.toLowerCase();
                        boolean isNew = !fromRegionsLower.contains(regionLower);
                        boolean isDisallowed = disallowedLower.contains(regionLower);
                        if (isNew && isDisallowed) {
                            if (plugin.getConfigManager().isDebugMode()) {
                                logger.info("DEBUG: Blocking on-duty rank entry for " + player.getName() + " (" + guardRank + ") into region " + regionName);
                            }
                            event.setCancelled(true);
                            plugin.getMessageManager().sendMessage(player, "restrictions.duty-rank-zone-entry");
                            return;
                        }
                    }
                }
            }
        }

        
        if (plugin.getDutyManager().isSubjectToGuardRestrictions(player) && !plugin.getDutyManager().isOnDuty(player)) {
            Location from2 = event.getFrom();
            Location to2 = event.getTo();
            if (to2 != null && !isInBreakZone(player)) {
                String[] required = plugin.getConfigManager().getDutyRequiredZones();
                for (String region : required) {
                    boolean enteringReq = plugin.getWorldGuardUtils().isLocationInRegion(to2, region) &&
                                           !plugin.getWorldGuardUtils().isLocationInRegion(from2, region);
                    if (enteringReq) {
                        event.setCancelled(true);
                        plugin.getMessageManager().sendMessage(player, "duty.restrictions.must-return-to-duty");
                        return;
                    }
                }
            }
        }

        
        if (plugin.getDutyManager().isInDutyTransition(player)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            
            if (to != null && from.distanceSquared(to) > 0.25) { 
                event.setCancelled(true);
                plugin.getDutyManager().cancelDutyTransition(player, "duty.restrictions.movement-cancelled");
                return;
            }
        }
        
        
        if (plugin.getJailManager().isInArrestMinigame(player)) {
            Location immobilizeLocation = plugin.getJailManager().getMinigameImmobilizeLocation(player);
            if (immobilizeLocation != null) {
                Location from = event.getFrom();
                Location to = event.getTo();
                
                if (to != null && from.distanceSquared(to) > 0.01) { 
                    
                    event.setCancelled(true);
                    player.teleport(immobilizeLocation);
                    
                    if (plugin.getConfigManager().isDebugMode()) {
                        plugin.getLogger().info("DEBUG: Prevented movement during arrest minigame for " + player.getName());
                    }
                    return;
                }
            }
        }
        
        
        if (plugin.getJailManager().isInJailCountdown(player)) {
            
            
        }
        
        
        if (plugin.getDataManager().isPlayerBeingChased(player.getUniqueId()) && 
            plugin.getConfigManager().shouldBlockRestrictedAreas()) {
            
            Location from = event.getFrom();
            Location to = event.getTo();
            
            if (to != null && !from.getWorld().equals(to.getWorld())) {
                
                if (plugin.getChaseManager().isPlayerInRestrictedArea(player)) {
                    event.setCancelled(true);
                    plugin.getMessageManager().sendMessage(player, "chase.blocking.area-entry");
                    return;
                }
            } else if (to != null) {
                
                String[] restrictedAreas = plugin.getConfigManager().getChaseRestrictedAreas();
                for (String area : restrictedAreas) {
                    if (plugin.getWorldGuardUtils().isLocationInRegion(to, area) && 
                        !plugin.getWorldGuardUtils().isLocationInRegion(from, area)) {
                        
                        event.setCancelled(true);
                        plugin.getMessageManager().sendMessage(player, "chase.blocking.area-entry");
                        return;
                    }
                }
            }
        }
        
        
        
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        
        if (isInBreakZone(player)) {
            return;
        }
        
        
        
        
        try {
            if (event.getHand() != null && event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) {
                return;
            }
        } catch (Throwable ignored) {}
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        try {
            Player player = event.getPlayer();
            
            if (!event.isSneaking()) return;
            if (!plugin.getJailManager().isInArrestMinigame(player)) return;
            
            boolean consumed = plugin.getJailManager().handleMinigameInteraction(player);
            if (consumed) {
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("DEBUG: Minigame interaction consumed via shift for " + player.getName());
                }
            }
        } catch (Throwable ignore) {}
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        
        if (plugin.getDutyManager().isSubjectToGuardRestrictions(player) && plugin.getDutyManager().isOnDuty(player)) {
            PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
            if (data != null && data.isOnDuty()) {
                String guardRank = data.getGuardRank();
                
                
                org.bukkit.Location guardSpawn = plugin.getConfigManager().getGuardSpawnLocation();
                if (guardSpawn != null) {
                    event.setRespawnLocation(guardSpawn);
                    
                    if (plugin.getConfigManager().isDebugMode()) {
                        logger.info("DEBUG: Set guard spawn location for " + player.getName() + " at " + 
                            guardSpawn.getWorld().getName() + " " + 
                            (int) guardSpawn.getX() + "," + (int) guardSpawn.getY() + "," + (int) guardSpawn.getZ());
                    }
                }
                
                if (guardRank != null) {
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        
                        if (player.isOnline() && plugin.getDutyManager().isOnDuty(player)) {
                            plugin.getDutyManager().giveGuardKitPublic(player, guardRank);
                            
                            if (plugin.getConfigManager().isDebugMode()) {
                                logger.info("DEBUG: Restored guard kit for " + player.getName() + " on respawn (rank: " + guardRank + ")");
                            }
                            
                            
                            plugin.getMessageManager().sendMessage(player, "duty.respawn.kit-restored",
                                MessageManager.stringPlaceholder("rank", guardRank));
                            
                            
                            if (guardSpawn != null) {
                                plugin.getMessageManager().sendMessage(player, "guard-spawn.respawned");
                            }
                        }
                    });
                }
            }
        }
    }
} 