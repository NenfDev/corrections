package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;
import java.util.logging.Logger;


public class LockerManager {

    private final EdenCorrections plugin;
    private final Logger logger;
    private final NamespacedKey lockerKey;
    private final NamespacedKey ownerKey;

    public LockerManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.lockerKey = new NamespacedKey(plugin, "guard_locker");
        this.ownerKey = new NamespacedKey(plugin, "guard_locker_owner");
    }

    public boolean tagBlockAsLocker(Block block, UUID owner) {
        try {
            if (block == null) return false;
            BlockState state = block.getState();
            if (!(state instanceof Container)) return false;
            Container container = (Container) state;
            PersistentDataContainer pdc = container.getPersistentDataContainer();
            pdc.set(lockerKey, PersistentDataType.BYTE, (byte) 1);
            if (owner != null) {
                pdc.set(ownerKey, PersistentDataType.STRING, owner.toString());
            } else {
                pdc.remove(ownerKey);
            }
            return container.update(true, false);
        } catch (Exception e) {
            logger.warning("Failed to tag block as locker: " + e.getMessage());
            return false;
        }
    }

    public boolean untagBlock(Block block) {
        try {
            if (block == null) return false;
            BlockState state = block.getState();
            if (!(state instanceof Container)) return false;
            Container container = (Container) state;
            PersistentDataContainer pdc = container.getPersistentDataContainer();
            if (!pdc.has(lockerKey, PersistentDataType.BYTE)) return false;
            pdc.remove(lockerKey);
            pdc.remove(ownerKey);
            return container.update(true, false);
        } catch (Exception e) {
            logger.warning("Failed to untag locker: " + e.getMessage());
            return false;
        }
    }

    public boolean isLocker(Block block) {
        try {
            if (block == null) return false;
            BlockState state = block.getState();
            if (!(state instanceof Container)) return false;
            Container container = (Container) state;
            PersistentDataContainer pdc = container.getPersistentDataContainer();
            Byte flag = pdc.get(lockerKey, PersistentDataType.BYTE);
            return flag != null && flag == (byte) 1;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAllowedLocker(Block block, Player player) {
        try {
            if (block == null || player == null) return false;
            BlockState state = block.getState();
            if (!(state instanceof Container)) return false;
            Container container = (Container) state;
            PersistentDataContainer pdc = container.getPersistentDataContainer();
            Byte flag = pdc.get(lockerKey, PersistentDataType.BYTE);
            if (flag == null || flag != (byte) 1) return false;

            boolean requireOwner = plugin.getConfig().getBoolean("guard-system.storage.whitelist.require-ownership", true);
            if (!requireOwner) return true;

            String ownerStr = pdc.get(ownerKey, PersistentDataType.STRING);
            if (ownerStr == null || ownerStr.trim().isEmpty()) return false;
            try {
                UUID ownerId = UUID.fromString(ownerStr);
                return ownerId.equals(player.getUniqueId());
            } catch (IllegalArgumentException ex) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    
    public UUID getLockerOwner(Block block) {
        try {
            if (block == null) return null;
            BlockState state = block.getState();
            if (!(state instanceof Container)) return null;
            Container container = (Container) state;
            PersistentDataContainer pdc = container.getPersistentDataContainer();
            Byte flag = pdc.get(lockerKey, PersistentDataType.BYTE);
            if (flag == null || flag != (byte) 1) return null;

            String ownerStr = pdc.get(ownerKey, PersistentDataType.STRING);
            if (ownerStr == null || ownerStr.trim().isEmpty()) return null;
            
            try {
                return UUID.fromString(ownerStr);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    
    public String getLockerOwnerName(Block block) {
        UUID ownerId = getLockerOwner(block);
        if (ownerId == null) return null;
        
        
        Player onlinePlayer = plugin.getServer().getPlayer(ownerId);
        if (onlinePlayer != null) {
            return onlinePlayer.getName();
        }
        
        
        try {
            return plugin.getDataManager().getPlayerData(ownerId).getPlayerName();
        } catch (Exception e) {
            return "Unknown Player";
        }
    }
}


