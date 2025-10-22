package dev.lsdmc.edenCorrections.commands;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import dev.lsdmc.edenCorrections.models.ChaseData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Collection;
import java.util.UUID;
import org.bukkit.Location;

import static dev.lsdmc.edenCorrections.managers.MessageManager.*;

public class CommandHandler implements CommandExecutor, TabCompleter {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    public CommandHandler(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    private boolean isInBreakZone(org.bukkit.entity.Player player) {
        try {
            return plugin.getWorldGuardUtils().isPlayerInBreakZone(player);
        } catch (Exception ignored) {
            return false;
        }
    }
    
    public void registerCommands() {
        
        if (plugin.getCommand("duty") != null) {
            plugin.getCommand("duty").setExecutor(this);
            plugin.getCommand("duty").setTabCompleter(this);
        }
        if (plugin.getCommand("chase") != null) {
            plugin.getCommand("chase").setExecutor(this);
            plugin.getCommand("chase").setTabCompleter(this);
        }
        if (plugin.getCommand("jail") != null) {
            plugin.getCommand("jail").setExecutor(this);
            plugin.getCommand("jail").setTabCompleter(this);
        }
        if (plugin.getCommand("jailoffline") != null) {
            plugin.getCommand("jailoffline").setExecutor(this);
            plugin.getCommand("jailoffline").setTabCompleter(this);
        }
        if (plugin.getCommand("corrections") != null) {
            plugin.getCommand("corrections").setExecutor(this);
            plugin.getCommand("corrections").setTabCompleter(this);
        }
        
        if (plugin.getCommand("sword") != null) {
            plugin.getCommand("sword").setExecutor(this);
            plugin.getCommand("sword").setTabCompleter(this);
        }
        if (plugin.getCommand("bow") != null) {
            plugin.getCommand("bow").setExecutor(this);
            plugin.getCommand("bow").setTabCompleter(this);
        }
        if (plugin.getCommand("armor") != null) {
            plugin.getCommand("armor").setExecutor(this);
            plugin.getCommand("armor").setTabCompleter(this);
        }
        if (plugin.getCommand("axe") != null) {
            plugin.getCommand("axe").setExecutor(this);
            plugin.getCommand("axe").setTabCompleter(this);
        }
        if (plugin.getCommand("potion") != null) {
            plugin.getCommand("potion").setExecutor(this);
            plugin.getCommand("potion").setTabCompleter(this);
        }
        if (plugin.getCommand("drugtest") != null) {
            plugin.getCommand("drugtest").setExecutor(this);
            plugin.getCommand("drugtest").setTabCompleter(this);
        }
        if (plugin.getCommand("potiontest") != null) {
            plugin.getCommand("potiontest").setExecutor(this);
            plugin.getCommand("potiontest").setTabCompleter(this);
        }
        
        if (plugin.getCommand("dutybank") != null) {
            plugin.getCommand("dutybank").setExecutor(this);
            plugin.getCommand("dutybank").setTabCompleter(this);
        }
        
        if (plugin.getCommand("tips") != null) {
            plugin.getCommand("tips").setExecutor(this);
            plugin.getCommand("tips").setTabCompleter(this);
        }
        
        if (plugin.getCommand("area") != null) {
            plugin.getCommand("area").setExecutor(this);
            plugin.getCommand("area").setTabCompleter(this);
        }
        
        
        if (plugin.getCommand("guardspawn") != null) {
            plugin.getCommand("guardspawn").setExecutor(this);
            plugin.getCommand("guardspawn").setTabCompleter(this);
        }
        
        
        if (plugin.getCommand("transfer") != null) {
            plugin.getCommand("transfer").setExecutor(this);
            plugin.getCommand("transfer").setTabCompleter(this);
        }
        
        
        if (plugin.getCommand("buybacknpc") != null) {
            plugin.getCommand("buybacknpc").setExecutor(this);
            plugin.getCommand("buybacknpc").setTabCompleter(this);
        }
        
        

        
        if (plugin.getCommand("ecpromote") != null) {
            plugin.getCommand("ecpromote").setExecutor(this);
            plugin.getCommand("ecpromote").setTabCompleter(this);
        }
        if (plugin.getCommand("ecprogress") != null) {
            plugin.getCommand("ecprogress").setExecutor(this);
            plugin.getCommand("ecprogress").setTabCompleter(this);
        }
        if (plugin.getCommand("ecrankup") != null) {
            plugin.getCommand("ecrankup").setExecutor(this);
            plugin.getCommand("ecrankup").setTabCompleter(this);
        }
        if (plugin.getCommand("ecdemote") != null) {
            plugin.getCommand("ecdemote").setExecutor(this);
            plugin.getCommand("ecdemote").setTabCompleter(this);
        }
        
        
        plugin.getCommand("loot").setExecutor(this);
        plugin.getCommand("loot").setTabCompleter(this);
        
        plugin.getCommand("retrieve").setExecutor(this);
        plugin.getCommand("retrieve").setTabCompleter(this);
        if (plugin.getCommand("buyback") != null) {
            plugin.getCommand("buyback").setExecutor(this);
            plugin.getCommand("buyback").setTabCompleter(this);
        }
        if (plugin.getCommand("contrabandinfo") != null) {
            plugin.getCommand("contrabandinfo").setExecutor(this);
            plugin.getCommand("contrabandinfo").setTabCompleter(this);
        }
        
        
        if (plugin.getCommand("arrestminigame") != null) {
            plugin.getCommand("arrestminigame").setExecutor(this);
            plugin.getCommand("arrestminigame").setTabCompleter(this);
        }

        
        if (plugin.getCommand("locker") != null) {
            plugin.getCommand("locker").setExecutor(this);
            plugin.getCommand("locker").setTabCompleter(this);
        }
        
        logger.info("Commands registered successfully with tab completion!");
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String commandName = command.getName().toLowerCase();
        
        switch (commandName) {
            case "duty":
                return handleDutyCommand(sender, args);
            case "chase":
                return handleChaseCommand(sender, args);
            case "jail":
                return handleJailCommand(sender, args);
            case "jailoffline":
                return handleJailOfflineCommand(sender, args);
            case "corrections":
                return handleCorrectionsCommand(sender, args);
            
            case "sword":
                return handleContrabandCommand(sender, "sword", args);
            case "bow":
                return handleContrabandCommand(sender, "bow", args);
            case "armor":
                return handleContrabandCommand(sender, "armor", args);
            case "axe":
                return handleContrabandCommand(sender, "axe", args);
            case "potion":
                return handleContrabandCommand(sender, "potion", args);
            case "drugtest":
                return handleDrugTestCommand(sender, args);
            case "potiontest":
                return handlePotionTestCommand(sender, args);
            case "dutybank":
                return handleDutyBankCommand(sender, args);
            case "tips":
                return handleTipsCommand(sender, args);
            case "area":
                return handleAreaCommand(sender, args);
            case "guardspawn":
                return handleGuardSpawnCommand(sender, args);
            case "transfer":
                return handleTransferCommand(sender, args);
            case "buybacknpc":
                return handleBuybackNpcCommand(sender, args);
            case "promote":
                
                if (sender instanceof Player) {
                    plugin.getMessageManager().sendMessage((Player) sender, "progression.deprecated-promote");
                } else {
                    sender.sendMessage(plugin.getMessageManager().getPlainTextMessage("progression.deprecated-promote"));
                }
                return true;
            case "rankup":
                if (sender instanceof Player) {
                    plugin.getMessageManager().sendMessage((Player) sender, "progression.deprecated-rankup");
                } else {
                    sender.sendMessage(plugin.getMessageManager().getPlainTextMessage("progression.deprecated-rankup"));
                }
                return true;
            case "progress":
                if (sender instanceof Player) {
                    plugin.getMessageManager().sendMessage((Player) sender, "progression.deprecated-progress");
                } else {
                    sender.sendMessage(plugin.getMessageManager().getPlainTextMessage("progression.deprecated-progress"));
                }
                return true;
            case "ecpromote":
            case "ecrankup":
                return handlePromotionCommand(sender, args);
            case "ecprogress":
                return handleProgressCommand(sender, args);
            case "ecdemote":
                return handleDemoteStandalone(sender, args);
            case "loot":
                return handleLootCommand(sender, args);
            case "retrieve":
                return handleRetrieveCommand(sender, args);
            case "buyback":
                return handleBuybackCommand(sender, args);
            case "contrabandinfo":
                return handleContrabandInfoCommand(sender, args);
            case "arrestminigame":
                return handleArrestMinigameTest(sender, args);
            case "locker":
                return handleLockerCommand(sender, args);
            default:
                return false;
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        String commandName = command.getName().toLowerCase();
        
        switch (commandName) {
            case "duty":
                return new ArrayList<>(); 
            case "chase":
                return handleChaseTabComplete(sender, args);
            case "jail":
                return handleJailTabComplete(sender, args);
            case "jailoffline":
                return handleJailOfflineTabComplete(sender, args);
            case "corrections":
                return handleCorrectionsTabComplete(sender, args);
            case "sword":
            case "bow":
            case "armor":
            case "axe":
            case "potion":
            case "drugtest":
            case "potiontest":
                return handleContrabandTabComplete(sender, args);
            case "dutybank":
                return handleDutyBankTabComplete(sender, args);
            case "tips":
                return handleTipsTabComplete(sender, args);
            case "area":
                return handleAreaTabComplete(sender, args);
            case "guardspawn":
                return handleGuardSpawnTabComplete(sender, args);
            case "transfer":
                return handleTransferTabComplete(sender, args);
            case "promote":
            case "progress":
            case "rankup":
            case "retrieve":
                return new ArrayList<>(); 
            case "buyback":
                return new ArrayList<>();
            case "loot":
                return handleLootTabComplete(sender, args);
            case "arrestminigame":
                return handleArrestMinigameTabComplete(sender, args);
            case "buybacknpc":
                return handleBuybackNpcTabComplete(sender, args);
            case "locker":
                return handleLockerTabComplete(sender, args);
            case "ecdemote":
                return handleDemoteTabComplete(sender, args);
            case "ecpromote":
            case "ecprogress":
            case "ecrankup":
                return new ArrayList<>();
            default:
                return new ArrayList<>();
        }
    }
    
    private boolean handleDutyCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        
        
        if (!plugin.getDutyManager().hasGuardAccessOrBypass(player)) {
            if (plugin.getUnauthorizedAccessSpamManager().shouldWarnPlayer(player)) {
                plugin.getMessageManager().sendMessage(player, "universal.no-permission");
            }
            return true;
        }
        
        
        if (args.length > 0) {
            plugin.getMessageManager().sendMessage(player, "universal.invalid-usage",
                stringPlaceholder("command", "/duty"));
            return true;
        }
        
        
        plugin.getDutyManager().toggleDuty(player);
        return true;
    }

    private boolean handleArrestMinigameTest(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("edencorrections.admin") && !player.isOp()) {
            plugin.getMessageManager().sendMessage(player, "universal.no-permission");
            return true;
        }
        int duration = 10;
        if (args.length >= 1) {
            try {
                duration = Math.max(3, Integer.parseInt(args[0]));
            } catch (NumberFormatException ignored) {}
        }
        
        plugin.getJailManager().startArrestMinigamePublic(player, player, duration);
        plugin.getMessageManager().sendActionBar(player, "system.info", stringPlaceholder("message", "Arrest minigame test started for " + duration + "s"));
        return true;
    }

    private boolean handleLockerCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("edencorrections.admin.locker")) {
            plugin.getMessageManager().sendMessage(player, "universal.no-permission");
            return true;
        }
        if (args.length == 0) {
            plugin.getMessageManager().sendMessage(player, "system.info",
                stringPlaceholder("message", "Usage: /locker <tag [player]|untag|info>"));
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "tag": {
                
                Block target = player.getTargetBlockExact(6);
                if (target == null || !(target.getState() instanceof org.bukkit.block.Container)) {
                    plugin.getMessageManager().sendMessage(player, "system.info",
                        stringPlaceholder("message", "Look at a container (e.g., barrel) within 6 blocks."));
                    return true;
                }
                Player owner = player;
                if (args.length >= 2) {
                    Player specified = org.bukkit.Bukkit.getPlayer(args[1]);
                    if (specified == null) {
                        plugin.getMessageManager().sendMessage(player, "universal.player-not-found",
                            stringPlaceholder("player", args[1]));
                        return true;
                    }
                    owner = specified;
                }
                boolean ok = plugin.getLockerManager().tagBlockAsLocker(target, owner != null ? owner.getUniqueId() : null);
                if (ok) {
                    plugin.getMessageManager().sendMessage(player, "system.info",
                        stringPlaceholder("message", "Tagged as guard locker" + (owner != null ? " (owner: " + owner.getName() + ")" : "")));
                } else {
                    plugin.getMessageManager().sendMessage(player, "universal.failed");
                }
                return true;
            }
            case "untag": {
                Block target = player.getTargetBlockExact(6);
                if (target == null || !(target.getState() instanceof org.bukkit.block.Container)) {
                    plugin.getMessageManager().sendMessage(player, "system.info",
                        stringPlaceholder("message", "Look at a container (e.g., barrel) within 6 blocks."));
                    return true;
                }
                boolean ok = plugin.getLockerManager().untagBlock(target);
                plugin.getMessageManager().sendMessage(player, ok ? "system.info" : "universal.failed",
                    stringPlaceholder("message", ok ? "Removed locker tag" : ""));
                return true;
            }
            case "info": {
                Block target = player.getTargetBlockExact(6);
                if (target == null || !(target.getState() instanceof org.bukkit.block.Container)) {
                    plugin.getMessageManager().sendMessage(player, "system.info",
                        stringPlaceholder("message", "Look at a container (e.g., barrel) within 6 blocks."));
                    return true;
                }
                boolean isLocker = plugin.getLockerManager().isLocker(target);
                if (isLocker) {
                    String ownerName = plugin.getLockerManager().getLockerOwnerName(target);
                    String message = "This container is a guard locker";
                    if (ownerName != null) {
                        message += " (owner: " + ownerName + ")";
                    } else {
                        message += " (no specific owner)";
                    }
                    plugin.getMessageManager().sendMessage(player, "system.info",
                        stringPlaceholder("message", message));
                } else {
                    plugin.getMessageManager().sendMessage(player, "system.info",
                        stringPlaceholder("message", "This container is NOT a guard locker"));
                }
                return true;
            }
            default:
                plugin.getMessageManager().sendMessage(player, "system.info",
                    stringPlaceholder("message", "Usage: /locker <tag [player]|untag|info>"));
                return true;
        }
    }
    
    private boolean handleChaseCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("edencorrections.guard.chase")) {
            plugin.getMessageManager().sendMessage(player, "universal.no-permission");
            return true;
        }
        
        if (args.length == 0) {
            plugin.getMessageManager().sendMessage(player, "universal.invalid-usage",
                stringPlaceholder("command", "/chase <player|capture|end>"));
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "capture":
                return handleChaseCapture(player, args);
            case "end":
                return handleChaseEnd(player, args);
            default:
                return handleChaseStart(player, args);
        }
    }
    
    private boolean handleChaseStart(Player player, String[] args) {
        if (args.length != 1) {
            plugin.getMessageManager().sendMessage(player, "universal.invalid-usage",
                stringPlaceholder("command", "/chase <player>"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            plugin.getMessageManager().sendMessage(player, "universal.player-not-found",
                stringPlaceholder("player", args[0]));
            return true;
        }
        
        
        if (isInBreakZone(player) || isInBreakZone(target)) {
            plugin.getMessageManager().sendMessage(player, "system.info",
                stringPlaceholder("message", "This action is disabled in event zones."));
            return true;
        }
        
        plugin.getChaseManager().startChase(player, target);
        return true;
    }
    
    private boolean handleChaseCapture(Player player, String[] args) {
        if (args.length != 1) {
            plugin.getMessageManager().sendMessage(player, "universal.invalid-usage",
                stringPlaceholder("command", "/chase capture"));
            return true;
        }
        
        ChaseData chase = plugin.getDataManager().getChaseByGuard(player.getUniqueId());
        if (chase == null) {
            plugin.getMessageManager().sendMessage(player, "chase.restrictions.not-on-duty");
            return true;
        }
        
        Player target = Bukkit.getPlayer(chase.getTargetId());
        if (target == null) {
            plugin.getMessageManager().sendMessage(player, "universal.player-not-found",
                stringPlaceholder("player", "target"));
            return true;
        }
        
        
        if (isInBreakZone(player) || isInBreakZone(target)) {
            plugin.getMessageManager().sendMessage(player, "system.info",
                stringPlaceholder("message", "This action is disabled in event zones."));
            return true;
        }
        
        plugin.getChaseManager().captureTarget(player, target);
        return true;
    }
    
    private boolean handleChaseEnd(Player player, String[] args) {
        ChaseData chase = plugin.getDataManager().getChaseByGuard(player.getUniqueId());
        if (chase == null) {
            plugin.getMessageManager().sendMessage(player, "admin.chase.not-in-chase");
            return true;
        }
        
        plugin.getChaseManager().endChase(chase.getChaseId(), plugin.getMessageManager().getPlainTextMessage("chase.end-reasons.manually-ended-guard"));
        return true;
    }
    
    private boolean handleJailCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("edencorrections.guard.jail")) {
            plugin.getMessageManager().sendMessage(player, "universal.no-permission");
            return true;
        }
        
        if (args.length < 1) {
            plugin.getMessageManager().sendMessage(player, "universal.invalid-usage",
                stringPlaceholder("command", "/jail <player> [reason]"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            plugin.getMessageManager().sendMessage(player, "universal.player-not-found",
                stringPlaceholder("player", args[0]));
            return true;
        }
        
        
        if (isInBreakZone(player) || isInBreakZone(target)) {
            plugin.getMessageManager().sendMessage(player, "system.info",
                stringPlaceholder("message", "This action is disabled in event zones."));
            return true;
        }
        
        
        String reason = plugin.getMessageManager().getPlainTextMessage("jail.no-reason");
        if (args.length > 1) {
            reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        }
        
        
        plugin.getJailManager().startJailCountdown(player, target, reason);
        return true;
    }
    
    private boolean handleJailOfflineCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("edencorrections.guard.admin")) {
            plugin.getMessageManager().sendMessage(sender, "universal.no-permission");
            return true;
        }
        
        if (args.length < 1) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/jailoffline <player> [reason]"));
            return true;
        }
        
        String targetName = args[0];
        
        
        String reason = "No reason specified";
        if (args.length > 1) {
            reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        }
        
        
        Player executor;
        if (sender instanceof Player) {
            executor = (Player) sender;
        } else {
            executor = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission("edencorrections.guard.admin") || p.isOp())
                    .findFirst().orElse(null);
            if (executor == null) {
                plugin.getMessageManager().sendMessage(sender, "universal.failed");
                return true;
            }
        }
        plugin.getJailManager().jailOfflinePlayer(executor, targetName, reason)
            .thenAccept(success -> {
                if (!success) {
                    plugin.getMessageManager().sendMessage(executor, "universal.failed");
                }
            });
        return true;
    }
    
    private boolean handleCorrectionsCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            return handleCorrectionsHelp(sender);
        }
        
        String subCommand = args[0].toLowerCase();
        
        
        if (!hasPermissionForSubcommand(sender, subCommand)) {
            plugin.getMessageManager().sendMessage(sender, "universal.no-permission");
            return true;
        }
        
        switch (subCommand) {
            case "wanted":
                return handleWantedCommand(sender, args);
            case "chase":
                return handleChaseAdminCommand(sender, args);
            case "duty":
                return handleDutyAdminCommand(sender, args);
            case "penalty":
                return handlePenaltyAdminCommand(sender, args);
            case "progression":
                return handleProgressionAdminCommand(sender, args);
            case "player":
                return handlePlayerAdminCommand(sender, args);
            case "system":
                return handleSystemCommand(sender, args);
            case "reload":
                return handleReloadCommand(sender, args);
            case "help":
            default:
                return handleCorrectionsHelp(sender);
        }
    }
    
    
    private boolean hasPermissionForSubcommand(CommandSender sender, String subCommand) {
        switch (subCommand.toLowerCase()) {
            
            case "wanted":
            case "chase":
            case "duty":
            case "player":
                return sender.hasPermission("edencorrections.guard") || sender.hasPermission("edencorrections.admin");
            
            
            case "penalty":
            case "progression":
            case "system":
            case "reload":
                return sender.hasPermission("edencorrections.admin");
            
            
            case "help":
            default:
                return sender.hasPermission("edencorrections.guard") || sender.hasPermission("edencorrections.admin");
        }
    }
    
    private boolean handleCorrectionsHelp(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            plugin.getMessageManager().sendRawMessage(player, "help.corrections.header");
            plugin.getMessageManager().sendRawMessage(player, "help.corrections.title");
            plugin.getMessageManager().sendRawMessage(player, "help.corrections.divider");
            plugin.getMessageManager().sendRawMessage(player, "help.corrections.wanted");
            plugin.getMessageManager().sendRawMessage(player, "help.corrections.chase");
            plugin.getMessageManager().sendRawMessage(player, "help.corrections.duty");
            plugin.getMessageManager().sendRawMessage(player, "help.corrections.player");
            plugin.getMessageManager().sendRawMessage(player, "help.corrections.player-modify");
            plugin.getMessageManager().sendRawMessage(player, "help.corrections.system");
            plugin.getMessageManager().sendRawMessage(player, "help.corrections.reload");
            plugin.getMessageManager().sendRawMessage(player, "help.corrections.help");
            plugin.getMessageManager().sendRawMessage(player, "help.corrections.footer");
        } else {
            
            sender.sendMessage(plugin.getMessageManager().getPlainTextMessage("help.corrections.console.header"));
            sender.sendMessage(plugin.getMessageManager().getPlainTextMessage("help.corrections.console.wanted"));
            sender.sendMessage(plugin.getMessageManager().getPlainTextMessage("help.corrections.console.chase"));
            sender.sendMessage(plugin.getMessageManager().getPlainTextMessage("help.corrections.console.duty"));
            sender.sendMessage(plugin.getMessageManager().getPlainTextMessage("help.corrections.console.player"));
            sender.sendMessage(plugin.getMessageManager().getPlainTextMessage("help.corrections.console.player-modify"));
            sender.sendMessage(plugin.getMessageManager().getPlainTextMessage("help.corrections.console.system"));
            sender.sendMessage(plugin.getMessageManager().getPlainTextMessage("help.corrections.console.reload"));
            sender.sendMessage(plugin.getMessageManager().getPlainTextMessage("help.corrections.console.help"));
            sender.sendMessage(plugin.getMessageManager().getPlainTextMessage("help.corrections.console.footer"));
        }
        return true;
    }
    
    private boolean handleWantedCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/corrections wanted <set|clear|check|list> [args...]"));
            return true;
        }
        
        String action = args[1].toLowerCase();
        
        
        if (!hasPermissionForWantedAction(sender, action)) {
            plugin.getMessageManager().sendMessage(sender, "universal.no-permission");
            return true;
        }
        
        switch (action) {
            case "set":
                return handleWantedSet(sender, args);
            case "clear":
                return handleWantedClear(sender, args);
            case "check":
                return handleWantedCheck(sender, args);
            case "list":
                return handleWantedList(sender, args);
            default:
                plugin.getMessageManager().sendMessage(sender, "universal.unknown-subcommand",
                    stringPlaceholder("subcommand", action));
                return true;
        }
    }
    
    
    private boolean hasPermissionForWantedAction(CommandSender sender, String action) {
        switch (action.toLowerCase()) {
            case "check":
            case "list":
                
                return sender.hasPermission("edencorrections.guard") || sender.hasPermission("edencorrections.admin");
            case "set":
            case "clear":
                
                return sender.hasPermission("edencorrections.admin");
            default:
                return sender.hasPermission("edencorrections.admin");
        }
    }
    
    private boolean handleWantedSet(CommandSender sender, String[] args) {
        if (args.length < 4) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/corrections wanted set <player> <level> [reason]"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-not-found",
                stringPlaceholder("player", args[2]));
            return true;
        }
        
        
        if (plugin.getDutyManager().isOnDuty(target)) {
            plugin.getMessageManager().sendMessage(sender, "wanted.restrictions.guard-on-duty");
            return true;
        }
        
        try {
            int level = Integer.parseInt(args[3]);
            String reason = args.length > 4 ? String.join(" ", Arrays.copyOfRange(args, 4, args.length)) : "Admin set";
            
            if (plugin.getWantedManager().setWantedLevel(target, level, reason)) {
                plugin.getMessageManager().sendMessage(sender, "admin.wanted.set-success",
                    stringPlaceholder("player", target.getName()),
                    numberPlaceholder("stars", level));
            } else {
                plugin.getMessageManager().sendMessage(sender, "universal.failed");
            }
        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-number",
                stringPlaceholder("input", args[3]));
        }
        return true;
    }
    
    private boolean handleWantedClear(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/corrections wanted clear <player>"));
            return true;
        }
        
        Player clearTarget = Bukkit.getPlayer(args[2]);
        if (clearTarget == null) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-not-found",
                stringPlaceholder("player", args[2]));
            return true;
        }
        
        if (plugin.getWantedManager().clearWantedLevel(clearTarget)) {
            plugin.getMessageManager().sendMessage(sender, "admin.wanted.clear-success",
                stringPlaceholder("player", clearTarget.getName()));
        } else {
            plugin.getMessageManager().sendMessage(sender, "universal.failed");
        }
        return true;
    }
    
    private boolean handleWantedCheck(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/corrections wanted check <player>"));
            return true;
        }
        
        Player checkTarget = Bukkit.getPlayer(args[2]);
        if (checkTarget == null) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-not-found",
                stringPlaceholder("player", args[2]));
            return true;
        }
        
        int level = plugin.getWantedManager().getWantedLevel(checkTarget);
        if (level > 0) {
            long remainingTime = plugin.getWantedManager().getRemainingWantedTime(checkTarget);
            String reason = plugin.getWantedManager().getWantedReason(checkTarget);
            
            plugin.getMessageManager().sendMessage(sender, "admin.wanted.check-result",
                stringPlaceholder("player", checkTarget.getName()),
                numberPlaceholder("level", level),
                starsPlaceholder("stars", level));
            plugin.getMessageManager().sendMessage(sender, "admin.wanted.check-time",
                timePlaceholder("time", remainingTime / 1000));
            plugin.getMessageManager().sendMessage(sender, "admin.wanted.check-reason",
                stringPlaceholder("reason", reason));
        } else {
            plugin.getMessageManager().sendMessage(sender, "admin.wanted.check-none",
                stringPlaceholder("player", checkTarget.getName()));
        }
        return true;
    }
    
    private boolean handleWantedList(CommandSender sender, String[] args) {
        plugin.getMessageManager().sendMessage(sender, "admin.wanted.list-header");
        
        boolean foundWanted = false;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getWantedManager().isWanted(player)) {
                int playerLevel = plugin.getWantedManager().getWantedLevel(player);
                long remainingTime = plugin.getWantedManager().getRemainingWantedTime(player);
                
                plugin.getMessageManager().sendMessage(sender, "admin.wanted.list-entry",
                    stringPlaceholder("player", player.getName()),
                    numberPlaceholder("level", playerLevel),
                    starsPlaceholder("stars", playerLevel));
                foundWanted = true;
            }
        }
        
        if (!foundWanted) {
            plugin.getMessageManager().sendMessage(sender, "admin.wanted.list-none");
        }
        return true;
    }
    
    private boolean handleChaseAdminCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/corrections chase <list|end|endall> [args...]"));
            return true;
        }
        
        String action = args[1].toLowerCase();
        
        
        if (!hasPermissionForChaseAction(sender, action)) {
            plugin.getMessageManager().sendMessage(sender, "universal.no-permission");
            return true;
        }
        
        switch (action) {
            case "list":
                return handleChaseList(sender, args);
            case "end":
                return handleChaseEndAdmin(sender, args);
            case "endall":
                return handleChaseEndAll(sender, args);
            default:
                plugin.getMessageManager().sendMessage(sender, "universal.unknown-subcommand",
                    stringPlaceholder("subcommand", action));
                return true;
        }
    }
    
    
    private boolean hasPermissionForChaseAction(CommandSender sender, String action) {
        switch (action.toLowerCase()) {
            case "list":
                
                return sender.hasPermission("edencorrections.guard") || sender.hasPermission("edencorrections.admin");
            case "end":
            case "endall":
                
                return sender.hasPermission("edencorrections.admin");
            default:
                return sender.hasPermission("edencorrections.admin");
        }
    }
    
    private boolean handleChaseList(CommandSender sender, String[] args) {
        plugin.getMessageManager().sendMessage(sender, "admin.chase.list-header");
        
        boolean foundChases = false;
        for (ChaseData chase : plugin.getDataManager().getAllActiveChases()) {
            if (chase.isActive()) {
                Player guard = Bukkit.getPlayer(chase.getGuardId());
                Player target = Bukkit.getPlayer(chase.getTargetId());
                String guardName = guard != null ? guard.getName() : "Unknown";
                String targetName = target != null ? target.getName() : "Unknown";
                
                plugin.getMessageManager().sendMessage(sender, "admin.chase.list-entry",
                    stringPlaceholder("guard", guardName),
                    stringPlaceholder("target", targetName),
                    timePlaceholder("time", chase.getRemainingTime() / 1000));
                foundChases = true;
            }
        }
        
        if (!foundChases) {
            plugin.getMessageManager().sendMessage(sender, "admin.chase.list-none");
        }
        return true;
    }
    
    private boolean handleChaseEndAdmin(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/corrections chase end <guard>"));
            return true;
        }
        
        String guardName = args[2];
        Player guard = Bukkit.getPlayer(guardName);
        
        if (guard == null) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-not-found",
                stringPlaceholder("player", guardName));
            return true;
        }
        
        ChaseData chase = plugin.getDataManager().getChaseByGuard(guard.getUniqueId());
        if (chase == null) {
            plugin.getMessageManager().sendMessage(sender, "admin.chase.not-in-chase");
            return true;
        }
        
        plugin.getChaseManager().endChase(chase.getChaseId(), plugin.getMessageManager().getPlainTextMessage("chase.end-reasons.ended-by-admin"));
        plugin.getMessageManager().sendMessage(sender, "admin.chase.end-success",
            playerPlaceholder("player", guard));
        return true;
    }
    
    private boolean handleChaseEndAll(CommandSender sender, String[] args) {
        Collection<ChaseData> activeChases = plugin.getDataManager().getAllActiveChases();
        
        if (activeChases.isEmpty()) {
            plugin.getMessageManager().sendMessage(sender, "admin.chase.list-none");
            return true;
        }
        
        int count = 0;
        for (ChaseData chaseData : activeChases) {
            plugin.getChaseManager().endChase(chaseData.getChaseId(), plugin.getMessageManager().getPlainTextMessage("chase.end-reasons.ended-by-admin"));
            count++;
        }
        
        plugin.getMessageManager().sendMessage(sender, "admin.chase.end-all-success",
            numberPlaceholder("count", count));
        return true;
    }
    
    private boolean handleDutyAdminCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/corrections duty <list|time|force> [args...]"));
            return true;
        }
        
        String action = args[1].toLowerCase();
        
        
        if (!hasPermissionForDutyAction(sender, action, args)) {
            plugin.getMessageManager().sendMessage(sender, "universal.no-permission");
            return true;
        }
        
        switch (action) {
            case "list":
                return handleDutyList(sender, args);
            case "time":
                return handleDutyTime(sender, args);
            case "force":
                return handleDutyForce(sender, args);
            default:
                plugin.getMessageManager().sendMessage(sender, "universal.unknown-subcommand",
                    stringPlaceholder("subcommand", action));
                return true;
        }
    }
    
    
    private boolean hasPermissionForDutyAction(CommandSender sender, String action, String[] args) {
        switch (action.toLowerCase()) {
            case "list":
                
                return sender.hasPermission("edencorrections.guard") || sender.hasPermission("edencorrections.admin");
            case "time":
                
                if (args.length >= 4 && args[2].equalsIgnoreCase("check")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        String targetName = args[3];
                        
                        if (player.getName().equalsIgnoreCase(targetName)) {
                            return sender.hasPermission("edencorrections.guard") || sender.hasPermission("edencorrections.admin");
                        }
                    }
                }
                
                return sender.hasPermission("edencorrections.admin");
            case "force":
                
                return sender.hasPermission("edencorrections.admin");
            default:
                return sender.hasPermission("edencorrections.admin");
        }
    }
    
    private boolean handleDutyList(CommandSender sender, String[] args) {
        plugin.getMessageManager().sendMessage(sender, "admin.duty.list-header");
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getDutyManager().isSubjectToGuardRestrictions(player)) {
                PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
                if (data != null) {
                    if (data.isOnDuty()) {
                        plugin.getMessageManager().sendMessage(sender, "admin.duty.list-on-duty",
                            stringPlaceholder("player", player.getName()));
                    } else {
                        plugin.getMessageManager().sendMessage(sender, "admin.duty.list-off-duty",
                            stringPlaceholder("player", player.getName()));
                    }
                }
            }
        }
        return true;
    }
    
    private boolean handlePlayerAdminCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/corrections player <info|stats|reset|modify> <player> [scope]"));
            return true;
        }

        String sub = args[1].toLowerCase();
        
        
        if (!hasPermissionForPlayerAction(sender, sub)) {
            plugin.getMessageManager().sendMessage(sender, "universal.no-permission");
            return true;
        }
        
        switch (sub) {
            case "info":
                if (args.length < 3) {
                    plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                        stringPlaceholder("command", "/corrections player info <player>"));
                    return true;
                }
                return handlePlayerInfo(sender, args[2]);

            case "stats":
                if (args.length < 3) {
                    plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                        stringPlaceholder("command", "/corrections player stats <player>"));
                    return true;
                }
                return handlePlayerStats(sender, args[2]);

            case "reset":
                if (args.length < 3) {
                    plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                        stringPlaceholder("command", "/corrections player reset <player> [penalties|session|all]"));
                    return true;
                }
                String targetName = args[2];
                String scope = args.length >= 4 ? args[3].toLowerCase() : "penalties";
                return handlePlayerReset(sender, targetName, scope);

            case "modify":
                if (args.length < 5) {
                    plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                        stringPlaceholder("command", "/corrections player modify <player> <stat> <value>"));
                    return true;
                }
                return handlePlayerStatModification(sender, args[2], args[3], args[4]);

            default:
                plugin.getMessageManager().sendMessage(sender, "universal.unknown-subcommand",
                    stringPlaceholder("subcommand", sub));
                return true;
        }
    }
    
    
    private boolean hasPermissionForPlayerAction(CommandSender sender, String action) {
        switch (action.toLowerCase()) {
            case "info":
            case "stats":
                
                return sender.hasPermission("edencorrections.guard") || sender.hasPermission("edencorrections.admin");
            case "reset":
            case "modify":
                
                return sender.hasPermission("edencorrections.admin");
            default:
                return sender.hasPermission("edencorrections.admin");
        }
    }

    private boolean handleProgressionAdminCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/corrections progression <info|limits|force|toggle> ..."));
            return true;
        }
        String action = args[1].toLowerCase();
        switch (action) {
            case "info":
                if (args.length < 3) {
                    plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                        stringPlaceholder("command", "/corrections progression info <player>"));
                    return true;
                }
                return handleProgressionInfo(sender, args[2]);
            case "limits":
                return handleProgressionLimits(sender);
            case "force":
                if (args.length < 4) {
                    plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                        stringPlaceholder("command", "/corrections progression force <promote|demote> <player>"));
                    return true;
                }
                return handleProgressionForce(sender, args[2], args[3]);
            case "toggle":
                if (args.length < 4 || !args[2].equalsIgnoreCase("auto")) {
                    plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                        stringPlaceholder("command", "/corrections progression toggle auto <on|off>"));
                    return true;
                }
                return handleProgressionToggleAuto(sender, args[3]);
            default:
                plugin.getMessageManager().sendMessage(sender, "universal.unknown-subcommand",
                    stringPlaceholder("subcommand", action));
                return true;
        }
    }

    private boolean handleProgressionInfo(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-not-found",
                stringPlaceholder("player", playerName));
            return true;
        }
        
        plugin.getProgressionManager().showProgressionStatus(target);
        
        plugin.getMessageManager().sendMessage(sender, "progression.admin.info-sent");
        return true;
    }

    private boolean handleProgressionLimits(CommandSender sender) {
        plugin.getMessageManager().sendMessage(sender, "progression.admin.limits-header");
        
        Map<String, Integer> rankLimits = plugin.getConfigManager().getAllRankLimits();
        
        for (Map.Entry<String, Integer> entry : rankLimits.entrySet()) {
            String rank = entry.getKey();
            int limit = entry.getValue();
            
            
            int current = 0;
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                String playerRank = plugin.getDutyManager().getPlayerGuardRank(player);
                if (rank.equalsIgnoreCase(playerRank)) {
                    current++;
                }
            }
            
            String limitText;
            if (limit == -1) {
                limitText = "Unlimited";
            } else if (limit == 0) {
                limitText = "Disabled";
            } else {
                limitText = current + "/" + limit;
            }
            
            plugin.getMessageManager().sendMessage(sender, "progression.admin.limits-entry",
                stringPlaceholder("rank", rank),
                stringPlaceholder("current", String.valueOf(current)),
                stringPlaceholder("limit", limitText));
        }
        
        return true;
    }

    private boolean handleProgressionForce(CommandSender sender, String mode, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-not-found",
                stringPlaceholder("player", playerName));
            return true;
        }
        try {
            org.bukkit.plugin.RegisteredServiceProvider<net.luckperms.api.LuckPerms> reg = Bukkit.getServicesManager().getRegistration(net.luckperms.api.LuckPerms.class);
            if (reg == null) {
                plugin.getMessageManager().sendMessage(sender, "progression.luckperms-error");
                return true;
            }
            net.luckperms.api.LuckPerms luckPerms = reg.getProvider();
            String trackName = plugin.getConfigManager().getConfig().getString("progression.rankup.track", "guard");
            net.luckperms.api.track.Track track = luckPerms.getTrackManager().getTrack(trackName);
            if (track == null) {
                plugin.getMessageManager().sendMessage(sender, "progression.track-not-found",
                    stringPlaceholder("track", trackName));
                return true;
            }
            if (mode.equalsIgnoreCase("promote")) {
                luckPerms.getUserManager().modifyUser(target.getUniqueId(), user -> {
                    track.promote(user, luckPerms.getContextManager().getStaticContext());
                });
                plugin.getMessageManager().sendMessage(sender, "progression.admin.force-promoted",
                    stringPlaceholder("player", target.getName()));
            } else if (mode.equalsIgnoreCase("demote")) {
                luckPerms.getUserManager().modifyUser(target.getUniqueId(), user -> {
                    
                    for (String group : track.getGroups()) {
                        user.data().remove(net.luckperms.api.node.types.InheritanceNode.builder(group).build());
                    }
                });
                
                PlayerData data = plugin.getDataManager().getOrCreatePlayerData(target.getUniqueId(), target.getName());
                
                plugin.getDutyManager().forceOffDuty(target, data);
                
                
                String courtyardWarp = plugin.getConfigManager().getConfig().getString("demotion.courtyard-warp", "courtyard");
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "warp " + courtyardWarp + " " + target.getName());
                } catch (Exception e) {
                    
                    plugin.getLogger().warning("Failed to teleport " + target.getName() + " to courtyard after demotion: " + e.getMessage());
                }
                plugin.getMessageManager().sendMessage(sender, "progression.admin.force-demoted",
                    stringPlaceholder("player", target.getName()));
            } else {
                plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                    stringPlaceholder("command", "/corrections progression force <promote|demote> <player>"));
                return true;
            }
        } catch (Exception e) {
            plugin.getMessageManager().sendMessage(sender, "progression.promotion-error");
        }
        return true;
    }

    private boolean handleDemoteStandalone(CommandSender sender, String[] args) {
        if (!sender.hasPermission("edencorrections.admin")) {
            plugin.getMessageManager().sendMessage(sender, "universal.no-permission");
            return true;
        }
        if (args.length < 1) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/ecdemote <player> [toRank|none]"));
            return true;
        }
        String playerName = args[0];
        String toRank = args.length > 1 ? args[1] : "none";
        org.bukkit.OfflinePlayer off = Bukkit.getOfflinePlayer(playerName);
        java.util.UUID uuid = off.getUniqueId();
        try {
            org.bukkit.plugin.RegisteredServiceProvider<net.luckperms.api.LuckPerms> reg = Bukkit.getServicesManager().getRegistration(net.luckperms.api.LuckPerms.class);
            if (reg == null) {
                plugin.getMessageManager().sendMessage(sender, "progression.luckperms-error");
                return true;
            }
            net.luckperms.api.LuckPerms luckPerms = reg.getProvider();
            String trackName = plugin.getConfigManager().getConfig().getString("progression.rankup.track", "guard");
            net.luckperms.api.track.Track track = luckPerms.getTrackManager().getTrack(trackName);
            luckPerms.getUserManager().modifyUser(uuid, user -> {
                if (track != null) {
                    for (String group : track.getGroups()) {
                        user.data().remove(net.luckperms.api.node.types.InheritanceNode.builder(group).build());
                    }
                }
                if (!"none".equalsIgnoreCase(toRank)) {
                    user.data().add(net.luckperms.api.node.types.InheritanceNode.builder(toRank.toLowerCase()).build());
                }
            });
            Player online = Bukkit.getPlayer(uuid);
            if (online != null && online.isOnline()) {
                PlayerData data = plugin.getDataManager().getOrCreatePlayerData(uuid, online.getName());
                
                plugin.getDutyManager().forceOffDuty(online, data);
                
                
                String courtyardWarp = plugin.getConfigManager().getConfig().getString("demotion.courtyard-warp", "courtyard");
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "warp " + courtyardWarp + " " + online.getName());
                } catch (Exception e) {
                    
                    plugin.getLogger().warning("Failed to teleport " + online.getName() + " to courtyard after demotion: " + e.getMessage());
                }
                
                plugin.getMessageManager().sendMessage(online, "system.info",
                    stringPlaceholder("message", "You have been demoted by an administrator."));
            } else {
                PlayerData data = plugin.getDataManager().getOrCreatePlayerData(uuid, off.getName() != null ? off.getName() : playerName);
                data.setOnDuty(false);
                data.setOffDutyTime(System.currentTimeMillis());
                data.resetConsumedOffDutyTime();
                data.clearPenaltyTracking();
                plugin.getDataManager().savePlayerData(data);
            }
            plugin.getMessageManager().sendMessage(sender, "system.info",
                stringPlaceholder("message", "Demotion completed for " + playerName));
        } catch (Exception e) {
            plugin.getMessageManager().sendMessage(sender, "progression.promotion-error");
        }
        return true;
    }

    private boolean handleProgressionToggleAuto(CommandSender sender, String value) {
        boolean enable = value.equalsIgnoreCase("on") || value.equalsIgnoreCase("true");
        plugin.getConfigManager().setConfigValue("progression.auto-promotion", enable);
        plugin.getMessageManager().sendMessage(sender, enable ? "progression.admin.auto-enabled" : "progression.admin.auto-disabled");
        return true;
    }
    private boolean handlePlayerInfo(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-not-found",
                stringPlaceholder("player", playerName));
            return true;
        }

        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
        if (data == null) {
            plugin.getMessageManager().sendMessage(sender, "universal.failed");
            return true;
        }

        long earnedMin = data.getEarnedOffDutyTime() / (1000L * 60L);
        long availableMin = data.getAvailableOffDutyTimeInMinutes();
        long usedMin = Math.max(0, earnedMin - availableMin);

        plugin.getMessageManager().sendMessage(sender, "admin.duty.time-check",
            stringPlaceholder("player", target.getName()),
            timePlaceholder("earned", earnedMin * 60),
            timePlaceholder("used", usedMin * 60),
            timePlaceholder("available", availableMin * 60)
        );

        
        if (data.isPenaltyTrackingActive()) {
            long overrunSeconds = Math.max(0L, (System.currentTimeMillis() - data.getPenaltyStartTime()) / 1000L);
            plugin.getMessageManager().sendMessage(sender, "admin.duty.penalties-check",
                stringPlaceholder("player", target.getName()),
                numberPlaceholder("stage", data.getCurrentPenaltyStage()),
                timePlaceholder("overrun", overrunSeconds)
            );
        } else {
            plugin.getMessageManager().sendMessage(sender, "admin.duty.penalties-none",
                stringPlaceholder("player", target.getName()));
        }

        return true;
    }

    private boolean handlePlayerStats(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-not-found",
                stringPlaceholder("player", playerName));
            return true;
        }

        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
        if (data == null) {
            plugin.getMessageManager().sendMessage(sender, "universal.failed");
            return true;
        }

        
        plugin.getMessageManager().sendMessage(sender, "admin.player.stats.header");
        
        
        plugin.getMessageManager().sendMessage(sender, "admin.player.stats.current-session");
        
        
        if (data.isOnDuty()) {
            long currentDutyTime = (System.currentTimeMillis() - data.getDutyStartTime()) / 1000L;
            plugin.getMessageManager().sendMessage(sender, "admin.player.stats.duty-time",
                timePlaceholder("time", currentDutyTime));
        } else {
            plugin.getMessageManager().sendMessage(sender, "admin.player.stats.duty-time",
                timePlaceholder("time", 0L));
        }
        
        
        long earnedTime = data.getEarnedOffDutyTime() / 1000L;
        plugin.getMessageManager().sendMessage(sender, "admin.player.stats.earned-time",
            timePlaceholder("time", earnedTime));
        
        
        plugin.getMessageManager().sendMessage(sender, "admin.player.stats.searches",
            numberPlaceholder("count", data.getSessionSearches()));
        plugin.getMessageManager().sendMessage(sender, "admin.player.stats.successful-searches",
            numberPlaceholder("count", data.getSessionSuccessfulSearches()));
        plugin.getMessageManager().sendMessage(sender, "admin.player.stats.arrests",
            numberPlaceholder("count", data.getSessionArrests()));
        plugin.getMessageManager().sendMessage(sender, "admin.player.stats.kills",
            numberPlaceholder("count", data.getSessionKills()));
        plugin.getMessageManager().sendMessage(sender, "admin.player.stats.detections",
            numberPlaceholder("count", data.getSessionDetections()));
        
        
        plugin.getMessageManager().sendMessage(sender, "admin.player.stats.total-stats");
        plugin.getMessageManager().sendMessage(sender, "admin.player.stats.total-duty-time",
            timePlaceholder("time", data.getTotalDutyTime() / 1000L));
        plugin.getMessageManager().sendMessage(sender, "admin.player.stats.total-arrests",
            numberPlaceholder("count", data.getTotalArrests()));
        plugin.getMessageManager().sendMessage(sender, "admin.player.stats.total-violations",
            numberPlaceholder("count", data.getTotalViolations()));
        
        
        String currentRank = plugin.getDutyManager().getPlayerGuardRank(target);
        if (currentRank != null) {
            plugin.getMessageManager().sendMessage(sender, "admin.player.stats.progression-header");
            plugin.getMessageManager().sendMessage(sender, "admin.player.stats.current-rank",
                stringPlaceholder("rank", currentRank));
            
            
            String nextRank = plugin.getProgressionManager() != null ?
                invokeNextRank(currentRank) : null;
            if (nextRank != null) {
                boolean canPromote = invokeCheckPromotion(target, data, nextRank);
                String status = canPromote ? "Ready for promotion" : "Requirements not met";
                plugin.getMessageManager().sendMessage(sender, "admin.player.stats.promotion-status",
                    stringPlaceholder("rank", nextRank),
                    stringPlaceholder("status", status));
            } else {
                plugin.getMessageManager().sendMessage(sender, "admin.player.stats.max-rank-achieved");
            }
        }
        
        
        plugin.getMessageManager().sendMessage(sender, "admin.player.stats.header");
        
        return true;
    }
    
    
    private String invokeNextRank(String current) {
        try {
            java.lang.reflect.Method m = plugin.getProgressionManager().getClass().getDeclaredMethod("getNextRank", String.class);
            m.setAccessible(true);
            return (String) m.invoke(plugin.getProgressionManager(), current);
        } catch (Throwable ignored) { return null; }
    }
    private boolean invokeCheckPromotion(Player player, PlayerData data, String nextRank) {
        try {
            java.lang.reflect.Method m = plugin.getProgressionManager().getClass().getDeclaredMethod("checkPromotionRequirements", org.bukkit.entity.Player.class, dev.lsdmc.edenCorrections.models.PlayerData.class, String.class);
            m.setAccessible(true);
            Object res = m.invoke(plugin.getProgressionManager(), player, data, nextRank);
            java.lang.reflect.Field f = res.getClass().getDeclaredField("canPromote");
            f.setAccessible(true);
            return f.getBoolean(res);
        } catch (Throwable ignored) { return false; }
    }

    private boolean handlePlayerReset(CommandSender sender, String playerName, String scope) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-not-found",
                stringPlaceholder("player", playerName));
            return true;
        }

        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(target.getUniqueId(), target.getName());

        boolean didSomething = false;
        switch (scope) {
            case "penalties":
                
                data.clearPenaltyTracking();
                plugin.getBossBarManager().hideBossBarByType(target, "penalty");
                plugin.getDataManager().savePlayerData(data);
                plugin.getMessageManager().sendMessage(sender, "admin.duty.penalties-cleared",
                    stringPlaceholder("player", target.getName()));
                plugin.getMessageManager().sendMessage(target, "admin.duty.penalties-cleared-notify",
                    stringPlaceholder("admin", sender.getName()));
                didSomething = true;
                break;
            case "session":
                data.resetSessionStats();
                plugin.getDataManager().savePlayerData(data);
                didSomething = true;
                break;
            case "all":
                data.resetSessionStats();
                data.clearPenaltyTracking();
                plugin.getBossBarManager().hideBossBarByType(target, "penalty");
                plugin.getDataManager().savePlayerData(data);
                didSomething = true;
                break;
            default:
                plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                    stringPlaceholder("command", "/corrections player reset <player> [penalties|session|all]"));
                return true;
        }

        if (!didSomething) {
            plugin.getMessageManager().sendMessage(sender, "universal.failed");
        }
        return true;
    }
    
    private boolean handleDutyTime(CommandSender sender, String[] args) {
        
        if (args.length < 4) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/corrections duty time <check|add|remove|reset> <player> [minutes]"));
            return true;
        }

        String sub = args[2].toLowerCase();
        String playerName = args[3];
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-not-found",
                stringPlaceholder("player", playerName));
            return true;
        }

        switch (sub) {
            case "check": {
                
                PlayerData pdata = plugin.getDataManager().getPlayerData(target.getUniqueId());
                long earnedSec = pdata != null ? (pdata.getEarnedOffDutyTime() / 1000L) : 0L;
                long availableSec = pdata != null ? pdata.getAvailableOffDutyTimeInSeconds() : 0L;
                long usedSec = Math.max(0L, earnedSec - availableSec);
                plugin.getMessageManager().sendMessage(sender, "admin.duty.time-check",
                    stringPlaceholder("player", target.getName()),
                    timePlaceholder("earned", earnedSec),
                    timePlaceholder("used", usedSec),
                    timePlaceholder("available", availableSec)
                );
                return true;
            }
            case "add":
            case "remove": {
                if (args.length < 5) {
                    plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                        stringPlaceholder("command", "/corrections duty time " + sub + " <player> <minutes>"));
                    return true;
                }
                long minutes;
                try {
                    minutes = Long.parseLong(args[4]);
                } catch (NumberFormatException ex) {
                    plugin.getMessageManager().sendMessage(sender, "universal.invalid-number",
                        stringPlaceholder("input", args[4]));
                    return true;
                }

                if (sub.equals("add")) {
                    plugin.getDutyBankingManager().addDutyTime(target, minutes * 60L);
                    plugin.getMessageManager().sendMessage(sender, "admin.duty.time-modified",
                        stringPlaceholder("player", target.getName()),
                        numberPlaceholder("minutes", minutes));
                    long totalSeconds = plugin.getDutyBankingManager().getTotalDutyTime(target);
                    long totalMinutes = totalSeconds / 60L;
                    plugin.getMessageManager().sendMessage(target, "admin.duty.time-modified-notify",
                        stringPlaceholder("admin", sender.getName()),
                        numberPlaceholder("change", minutes),
                        numberPlaceholder("total", totalMinutes));
                } else {
                    plugin.getDutyBankingManager().removeDutyTime(target, minutes * 60L);
                    plugin.getMessageManager().sendMessage(sender, "admin.duty.time-modified",
                        stringPlaceholder("player", target.getName()),
                        numberPlaceholder("minutes", -minutes));
                    long totalSeconds = plugin.getDutyBankingManager().getTotalDutyTime(target);
                    long totalMinutes = totalSeconds / 60L;
                    plugin.getMessageManager().sendMessage(target, "admin.duty.time-modified-notify",
                        stringPlaceholder("admin", sender.getName()),
                        numberPlaceholder("change", -minutes),
                        numberPlaceholder("total", totalMinutes));
                }
                return true;
            }
            case "reset": {
                plugin.getDutyBankingManager().resetDutyTime(target);
                plugin.getMessageManager().sendMessage(sender, "admin.duty.time-reset",
                    stringPlaceholder("player", target.getName()));
                plugin.getMessageManager().sendMessage(target, "admin.duty.time-reset-notify",
                    stringPlaceholder("admin", sender.getName()));
                return true;
            }
            default:
                plugin.getMessageManager().sendMessage(sender, "universal.unknown-subcommand",
                    stringPlaceholder("subcommand", sub));
                return true;
        }
    }

    private boolean handleDutyForce(CommandSender sender, String[] args) {
        
        if (args.length < 4) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/corrections duty force <on|off> <player>"));
            return true;
        }

        String mode = args[2].toLowerCase();
        String playerName = args[3];
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-not-found",
                stringPlaceholder("player", playerName));
            return true;
        }

        if (mode.equals("off")) {
            PlayerData data = plugin.getDataManager().getOrCreatePlayerData(target.getUniqueId(), target.getName());
            boolean result = plugin.getDutyManager().goOffDuty(target, data);
            if (!result) {
                plugin.getMessageManager().sendMessage(sender, "universal.failed");
            }
            return true;
        } else if (mode.equals("on")) {
            boolean result = plugin.getDutyManager().initiateGuardDuty(target);
            if (!result) {
                plugin.getMessageManager().sendMessage(sender, "universal.failed");
            }
            return true;
        } else {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/corrections duty force <on|off> <player>"));
            return true;
        }
    }
    
    private boolean handleSystemCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/corrections system <stats|debug> [args...]"));
            return true;
        }
        
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "stats":
                return handleSystemStats(sender, args);
            case "debug":
                return handleSystemDebug(sender, args);
            case "spam":
                return handleSystemSpam(sender, args);
            default:
                plugin.getMessageManager().sendMessage(sender, "universal.unknown-subcommand",
                    stringPlaceholder("subcommand", action));
                return true;
        }
    }
    
    private boolean handleSystemStats(CommandSender sender, String[] args) {
        plugin.getMessageManager().sendMessage(sender, "admin.system.stats-header");
        plugin.getMessageManager().sendMessage(sender, "admin.system.stats-online",
            numberPlaceholder("online", Bukkit.getOnlinePlayers().size()),
            numberPlaceholder("max", Bukkit.getMaxPlayers()));
        plugin.getMessageManager().sendMessage(sender, "admin.system.stats-chases",
            numberPlaceholder("count", plugin.getDataManager().getActiveChaseCount()));
        
        String debugStatus = plugin.getConfigManager().isDebugMode() ? "admin.system.debug-status-enabled" : "admin.system.debug-status-disabled";
        plugin.getMessageManager().sendMessage(sender, "admin.system.stats-debug",
            stringPlaceholder("status", plugin.getMessageManager().getRawMessage(debugStatus)));
        return true;
    }
    
    private boolean handleSystemDebug(CommandSender sender, String[] args) {
        if (args.length < 3) {
            String debugStatus = plugin.getConfigManager().isDebugMode() ? "enabled" : "disabled";
            plugin.getMessageManager().sendMessage(sender, "debug.status-" + debugStatus);
            return true;
        }
        
        String debugValue = args[2].toLowerCase();
        if (debugValue.equals("on") || debugValue.equals("true")) {
            plugin.getConfigManager().setDebugMode(true);
            plugin.getMessageManager().sendMessage(sender, "debug.enabled");
        } else if (debugValue.equals("off") || debugValue.equals("false")) {
            plugin.getConfigManager().setDebugMode(false);
            plugin.getMessageManager().sendMessage(sender, "debug.disabled");
        } else if (debugValue.equals("rank")) {
            return handleDebugRank(sender, args);
        } else if (debugValue.equals("messages")) {
            return handleDebugMessages(sender, args);
        } else if (debugValue.equals("forcereload")) {
            return handleDebugForceReload(sender, args);
        } else {
            plugin.getMessageManager().sendMessage(sender, "debug.invalid-value");
        }
        return true;
    }
    
    private boolean handleDebugRank(CommandSender sender, String[] args) {
        if (args.length < 4) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/corrections system debug rank <player>"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[3]);
        if (target == null) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-not-found",
                stringPlaceholder("player", args[3]));
            return true;
        }
        
        sender.sendMessage("§6=== Debug Rank Information for " + target.getName() + " ===");
        
        
        boolean hasBasicPerm = target.hasPermission("edencorrections.guard");
        sender.sendMessage("§7Basic Permission (edencorrections.guard): " + (hasBasicPerm ? "§aYES" : "§cNO"));
        
        
        String detectedRank = plugin.getDutyManager().getPlayerGuardRank(target);
        sender.sendMessage("§7Detected Guard Rank: " + (detectedRank != null ? "§a" + detectedRank : "§cNone"));
        
        
        sender.sendMessage("§7Config Rank Mappings:");
        Map<String, String> rankMappings = plugin.getConfigManager().getRankMappings();
        for (Map.Entry<String, String> entry : rankMappings.entrySet()) {
            sender.sendMessage("§7  " + entry.getKey() + " -> " + entry.getValue());
        }
        
        
        if (plugin.getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                net.luckperms.api.LuckPerms luckPerms = net.luckperms.api.LuckPermsProvider.get();
                net.luckperms.api.model.user.User user = luckPerms.getUserManager().getUser(target.getUniqueId());
                if (user != null) {
                    sender.sendMessage("§7Player's LuckPerms Groups:");
                    user.getInheritedGroups(user.getQueryOptions()).forEach(group -> {
                        sender.sendMessage("§7  - " + group.getName());
                    });
                } else {
                    sender.sendMessage("§cNo LuckPerms user data found!");
                }
            } catch (Exception e) {
                sender.sendMessage("§cError accessing LuckPerms: " + e.getMessage());
            }
        } else {
            sender.sendMessage("§cLuckPerms not available!");
        }
        
        return true;
    }
    
    private boolean handleDebugMessages(CommandSender sender, String[] args) {
        plugin.getMessageManager().sendMessage(sender, "debug.diagnostic-report");
        
        
        plugin.getMessageManager().generateDiagnosticReport();
        
        
        plugin.getMessageManager().logMissingMessagesSummary();
        
        return true;
    }
    
    private boolean handleDebugForceReload(CommandSender sender, String[] args) {
        plugin.getMessageManager().sendMessage(sender, "debug.force-reload");
        
        
        plugin.getMessageManager().forceReload();
        
        return true;
    }
    
    private boolean handleSystemSpam(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/corrections system spam <stats|clear|reset> [player]"));
            return true;
        }
        
        String action = args[2].toLowerCase();
        
        switch (action) {
            case "stats":
                return handleSpamStats(sender, args);
            case "clear":
                return handleSpamClear(sender, args);
            case "reset":
                return handleSpamReset(sender, args);
            default:
                plugin.getMessageManager().sendMessage(sender, "universal.unknown-subcommand",
                    stringPlaceholder("subcommand", action));
                return true;
        }
    }
    
    private boolean handleSpamStats(CommandSender sender, String[] args) {
        if (args.length >= 4) {
            
            Player target = Bukkit.getPlayer(args[3]);
            if (target == null) {
                plugin.getMessageManager().sendMessage(sender, "universal.player-not-found",
                    stringPlaceholder("player", args[3]));
                return true;
            }
            
            Map<String, Object> stats = plugin.getUnauthorizedAccessSpamManager().getPlayerSpamStats(target);
            sender.sendMessage("§6=== Spam Stats for " + target.getName() + " ===");
            sender.sendMessage("§7Warning Count: §e" + stats.get("warning_count"));
            sender.sendMessage("§7Log Count: §e" + stats.get("log_count"));
            
            if (stats.containsKey("seconds_since_last_warning")) {
                sender.sendMessage("§7Seconds Since Last Warning: §e" + stats.get("seconds_since_last_warning"));
            }
            if (stats.containsKey("seconds_since_last_log")) {
                sender.sendMessage("§7Seconds Since Last Log: §e" + stats.get("seconds_since_last_log"));
            }
        } else {
            
            Map<String, Object> stats = plugin.getUnauthorizedAccessSpamManager().getTotalStats();
            sender.sendMessage("§6=== Total Spam Stats ===");
            sender.sendMessage("§7Tracked Players: §e" + stats.get("tracked_players"));
            sender.sendMessage("§7Total Warnings: §e" + stats.get("total_warnings"));
            sender.sendMessage("§7Total Logs: §e" + stats.get("total_logs"));
        }
        
        return true;
    }
    
    private boolean handleSpamClear(CommandSender sender, String[] args) {
        plugin.getUnauthorizedAccessSpamManager().clearAllTracking();
        sender.sendMessage("§a✓ Cleared all unauthorized access spam tracking data");
        return true;
    }
    
    private boolean handleSpamReset(CommandSender sender, String[] args) {
        if (args.length < 4) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/corrections system spam reset <player>"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[3]);
        if (target == null) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-not-found",
                stringPlaceholder("player", args[3]));
            return true;
        }
        
        plugin.getUnauthorizedAccessSpamManager().resetPlayerTracking(target);
        sender.sendMessage("§a✓ Reset spam tracking for " + target.getName());
        return true;
    }
    
    private boolean handleReloadCommand(CommandSender sender, String[] args) {
        try {
            plugin.reload();
            plugin.getMessageManager().sendMessage(sender, "system.reload-success");
        } catch (Exception e) {
            plugin.getMessageManager().sendMessage(sender, "system.reload-failed",
                stringPlaceholder("error", e.getMessage()));
        }
        return true;
    }
    
    
    
    private boolean handleContrabandCommand(CommandSender sender, String contrabandType, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-only");
            return true;
        }
        
        Player guard = (Player) sender;
        
        if (!guard.hasPermission("edencorrections.guard.contraband")) {
            plugin.getMessageManager().sendMessage(guard, "universal.no-permission");
            return true;
        }
        
        if (args.length != 1) {
            plugin.getMessageManager().sendMessage(guard, "universal.invalid-usage",
                stringPlaceholder("command", "/" + contrabandType + " <player>"));
            return true;
        }
        
        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            plugin.getMessageManager().sendMessage(guard, "universal.player-not-found",
                stringPlaceholder("player", targetName));
            return true;
        }
        
        
        if (target.equals(guard)) {
            plugin.getMessageManager().sendMessage(guard, "universal.invalid-usage",
                stringPlaceholder("command", "/" + contrabandType + " <player>"));
            return true;
        }
        
        
        if (isInBreakZone(guard) || isInBreakZone(target)) {
            plugin.getMessageManager().sendMessage(guard, "system.info",
                stringPlaceholder("message", "This action is disabled in event zones."));
            return true;
        }
        
        
        if (plugin.getDutyManager().isOnDuty(target)) {
            plugin.getMessageManager().sendMessage(guard, "contraband.restrictions.target-on-duty");
            return true;
        }
        
        
        plugin.getContrabandManager().requestContraband(guard, target, contrabandType);
        return true;
    }
    
    private boolean handleDrugTestCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-only");
            return true;
        }
        
        Player guard = (Player) sender;
        
        if (!guard.hasPermission("edencorrections.guard.contraband")) {
            plugin.getMessageManager().sendMessage(guard, "universal.no-permission");
            return true;
        }
        
        if (args.length != 1) {
            plugin.getMessageManager().sendMessage(guard, "universal.invalid-usage",
                stringPlaceholder("command", "/drugtest <player>"));
            return true;
        }
        
        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            plugin.getMessageManager().sendMessage(guard, "universal.player-not-found",
                stringPlaceholder("player", targetName));
            return true;
        }
        
        
        if (target.equals(guard)) {
            plugin.getMessageManager().sendMessage(guard, "universal.invalid-usage",
                stringPlaceholder("command", "/drugtest <player>"));
            return true;
        }
        
        
        if (isInBreakZone(guard) || isInBreakZone(target)) {
            plugin.getMessageManager().sendMessage(guard, "system.info",
                stringPlaceholder("message", "This action is disabled in event zones."));
            return true;
        }
        
        
        if (plugin.getDutyManager().isOnDuty(target)) {
            plugin.getMessageManager().sendMessage(guard, "contraband.restrictions.target-on-duty");
            return true;
        }
        
        
        plugin.getContrabandManager().performDrugTest(guard, target);
        return true;
    }
    
    private boolean handlePotionTestCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-only");
            return true;
        }
        
        Player guard = (Player) sender;
        
        if (!guard.hasPermission("edencorrections.guard.contraband")) {
            plugin.getMessageManager().sendMessage(guard, "universal.no-permission");
            return true;
        }
        
        if (args.length != 1) {
            plugin.getMessageManager().sendMessage(guard, "universal.invalid-usage",
                stringPlaceholder("command", "/potiontest <player>"));
            return true;
        }
        
        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            plugin.getMessageManager().sendMessage(guard, "universal.player-not-found",
                stringPlaceholder("player", targetName));
            return true;
        }
        
        
        if (target.equals(guard)) {
            plugin.getMessageManager().sendMessage(guard, "universal.invalid-usage",
                stringPlaceholder("command", "/potiontest <player>"));
            return true;
        }
        
        
        if (isInBreakZone(guard) || isInBreakZone(target)) {
            plugin.getMessageManager().sendMessage(guard, "system.info",
                stringPlaceholder("message", "This action is disabled in event zones."));
            return true;
        }
        
        
        if (plugin.getDutyManager().isOnDuty(target)) {
            plugin.getMessageManager().sendMessage(guard, "contraband.restrictions.target-on-duty");
            return true;
        }
        
        
        plugin.getContrabandManager().performPotionTest(guard, target);
        return true;
    }
    
    private boolean handleDutyBankCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("edencorrections.guard.banking")) {
            plugin.getMessageManager().sendMessage(player, "universal.no-permission");
            return true;
        }
        
        if (args.length != 1) {
            plugin.getMessageManager().sendMessage(player, "universal.invalid-usage",
                stringPlaceholder("command", "/dutybank <convert|status>"));
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "convert":
                plugin.getDutyBankingManager().convertDutyTime(player);
                break;
            case "status":
                plugin.getDutyBankingManager().showBankingStatus(player);
                break;
            default:
                plugin.getMessageManager().sendMessage(player, "universal.unknown-subcommand",
                    stringPlaceholder("subcommand", subCommand));
                break;
        }
        return true;
    }
    
    private boolean handleTipsCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("edencorrections.guard")) {
            plugin.getMessageManager().sendMessage(player, "universal.no-permission");
            return true;
        }
        
        String system = "duty-system"; 
        
        if (args.length == 1) {
            system = args[0].toLowerCase() + "-system";
        }
        
        
        List<String> tipKeys = plugin.getConfigManager().getConfig().getStringList("messages.tips_lists." + system);
        if (tipKeys != null && !tipKeys.isEmpty()) {
            for (String key : tipKeys) {
                
                plugin.getMessageManager().sendRawMessage(player, key);
            }
            return true;
        }

        
        List<String> tips = plugin.getConfigManager().getConfig().getStringList("messages.tips." + system);
        if (tips != null && !tips.isEmpty()) {
            for (String tip : tips) {
                plugin.getMessageManager().sendRawString(player, tip);
            }
            return true;
        }

        plugin.getMessageManager().sendMessage(player, "universal.invalid-usage",
            stringPlaceholder("command", "/tips [duty|contraband|chase|jail|banking]"));
        return true;
    }
    
    
    
    private List<String> handleChaseTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            
            List<String> subcommands = Arrays.asList("capture", "end");
            completions.addAll(subcommands);
            completions.addAll(getOnlinePlayerNames());
        }
        
        return filterCompletions(completions, args);
    }
    
    private List<String> handleJailTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(getOnlinePlayerNames());
        }
        
        return filterCompletions(completions, args);
    }
    
    private List<String> handleJailOfflineTabComplete(CommandSender sender, String[] args) {
        
        return new ArrayList<>();
    }
    
    private List<String> handleCorrectionsTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("wanted", "chase", "duty", "penalty", "progression", "player", "system", "reload", "help"));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "wanted":
                    completions.addAll(Arrays.asList("set", "clear", "check", "list"));
                    break;
                case "chase":
                    completions.addAll(Arrays.asList("list", "end", "endall"));
                    break;
                case "duty":
                    completions.addAll(Arrays.asList("list", "time", "force"));
                    break;
                case "penalty":
                    completions.addAll(Arrays.asList("check", "clear", "set", "simulate", "help"));
                    break;
                case "progression":
                    completions.addAll(Arrays.asList("info", "limits", "force", "toggle"));
                    break;
                case "player":
                    completions.addAll(Arrays.asList("info", "stats", "reset", "modify"));
                    break;
                case "system":
                    completions.addAll(Arrays.asList("stats", "debug", "spam"));
                    break;
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            String action = args[1].toLowerCase();
            
            if (subCommand.equals("wanted") && (action.equals("set") || action.equals("clear") || action.equals("check"))) {
                completions.addAll(getOnlinePlayerNames());
            } else if (subCommand.equals("chase") && action.equals("end")) {
                completions.addAll(getOnlinePlayerNames());
            } else if (subCommand.equals("duty") && action.equals("time")) {
                completions.addAll(Arrays.asList("check", "add", "remove", "reset"));
            } else if (subCommand.equals("duty") && action.equals("force")) {
                completions.addAll(Arrays.asList("on", "off"));
            } else if (subCommand.equals("penalty") && (action.equals("check") || action.equals("clear") || action.equals("set") || action.equals("simulate"))) {
                completions.addAll(getOnlinePlayerNames());
            } else if (subCommand.equals("progression") && action.equals("info")) {
                completions.addAll(getOnlinePlayerNames());
            } else if (subCommand.equals("progression") && action.equals("force")) {
                completions.addAll(Arrays.asList("promote", "demote"));
            } else if (subCommand.equals("progression") && action.equals("toggle")) {
                completions.addAll(Arrays.asList("auto"));
            } else if (subCommand.equals("player") && action.equals("info")) {
                completions.addAll(getOnlinePlayerNames());
            } else if (subCommand.equals("player") && action.equals("reset")) {
                completions.addAll(getOnlinePlayerNames());
            } else if (subCommand.equals("player") && action.equals("modify")) {
                completions.addAll(getOnlinePlayerNames());
            } else if (subCommand.equals("player") && action.equals("stats")) {
                
                completions.addAll(getOnlinePlayerNames());
            } else if (subCommand.equals("system") && action.equals("debug")) {
                completions.addAll(Arrays.asList("on", "off", "rank", "messages", "forcereload"));
            } else if (subCommand.equals("system") && action.equals("spam")) {
                completions.addAll(Arrays.asList("stats", "clear", "reset"));
            }
        } else if (args.length == 4) {
            String subCommand = args[0].toLowerCase();
            String action = args[1].toLowerCase();
            String option = args[2].toLowerCase();
            
            if (subCommand.equals("system") && action.equals("debug") && option.equals("rank")) {
                completions.addAll(getOnlinePlayerNames());
            } else if (subCommand.equals("duty") && action.equals("time")) {
                if (option.equals("check") || option.equals("add") || option.equals("remove") || option.equals("reset")) {
                    completions.addAll(getOnlinePlayerNames());
                }
            } else if (subCommand.equals("duty") && action.equals("force")) {
                
                completions.addAll(getOnlinePlayerNames());
            } else if (subCommand.equals("penalty")) {
                if (option.equals("set") || option.equals("simulate")) {
                    
                    completions.addAll(Arrays.asList("1", "2", "3", "4", "5", "10", "15", "30"));
                }
            } else if (subCommand.equals("progression") && action.equals("force")) {
                completions.addAll(getOnlinePlayerNames());
            } else if (subCommand.equals("progression") && action.equals("toggle") && option.equals("auto")) {
                completions.addAll(Arrays.asList("on", "off"));
            } else if (subCommand.equals("player") && action.equals("reset")) {
                
                completions.addAll(Arrays.asList("penalties", "session", "all"));
            } else if (subCommand.equals("player") && action.equals("modify")) {
                
                completions.addAll(Arrays.asList(
                    "dutystarttime", "offdutytime", "gracedebttime", "earnedoffdutytime", "consumedoffdutytime",
                    "penaltystarttime", "lastpenaltytime", "lastslownessapplication", "wantedexpiretime", "chasestarttime", "totaldutytime",
                    "sessionsearches", "sessionsuccessfulsearches", "sessionarrests", "sessionkills", "sessiondetections",
                    "currentpenaltystage", "wantedlevel", "totalarrests", "totalviolations", "totalkills", "totalqualifyingkills", "qualifyingkills",
                    "isonduty", "hasearnedbasetime", "hasbeennotifiedofexpiredtime", "beingchased", "hasactivepenaltybossbar",
                    "guardrank", "wantedreason", "chaser"
                ));
            } else if (subCommand.equals("system") && action.equals("spam") && (option.equals("stats") || option.equals("reset"))) {
                completions.addAll(getOnlinePlayerNames());
            }
        } else if (args.length == 5) {
            String subCommand = args[0].toLowerCase();
            String action = args[1].toLowerCase();
            
            if (subCommand.equals("player") && action.equals("modify")) {
                String statName = args[3].toLowerCase();
                
                switch (statName) {
                    case "isonduty":
                    case "hasearnedbasetime":
                    case "hasbeennotifiedofexpiredtime":
                    case "beingchased":
                    case "hasactivepenaltybossbar":
                        completions.addAll(Arrays.asList("true", "false"));
                        break;
                    case "wantedlevel":
                    case "currentpenaltystage":
                    case "sessionsearches":
                    case "sessionsuccessfulsearches":
                    case "sessionarrests":
                    case "sessionkills":
                    case "sessiondetections":
                    case "totalarrests":
                    case "totalviolations":
                    case "totalkills":
                    case "totalqualifyingkills":
                    case "qualifyingkills":
                        completions.addAll(Arrays.asList("0", "1", "5", "10", "25", "50", "100"));
                        break;
                    case "guardrank":
                        completions.addAll(Arrays.asList("trainee", "private", "officer", "sergeant", "captain"));
                        break;
                    case "wantedreason":
                        completions.addAll(Arrays.asList("none", "contraband", "assault", "murder", "escape"));
                        break;
                    case "chaser":
                        completions.addAll(getOnlinePlayerNames());
                        break;
                    default:
                        
                        if (statName.contains("time") || statName.contains("start") || statName.contains("expire")) {
                            completions.addAll(Arrays.asList("0", "3600000", "7200000", "86400000")); 
                        }
                        break;
                }
            }
        }
        
        return filterCompletions(completions, args);
    }

    private boolean handlePenaltyAdminCommand(CommandSender sender, String[] args) {
        
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/corrections penalty <check|clear|set|simulate|help> <player> [value]"));
            return true;
        }

        String action = args[1].toLowerCase();
        if (action.equals("help")) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                plugin.getMessageManager().sendRawMessage(p, "help.penalty.header");
                plugin.getMessageManager().sendRawMessage(p, "help.penalty.usage");
                plugin.getMessageManager().sendRawMessage(p, "help.penalty.footer");
            } else {
                sender.sendMessage("Penalty admin: /corrections penalty <check|clear|set|simulate> <player> [value]");
            }
            return true;
        }

        if (args.length < 3) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/corrections penalty <check|clear|set|simulate> <player> [value]"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-not-found",
                stringPlaceholder("player", args[2]));
            return true;
        }

        switch (action) {
            case "check": {
                PlayerData data = plugin.getDataManager().getOrCreatePlayerData(target.getUniqueId(), target.getName());
                if (data.isPenaltyTrackingActive()) {
                    long overrunSeconds = Math.max(0L, (System.currentTimeMillis() - data.getPenaltyStartTime()) / 1000L);
                    plugin.getMessageManager().sendMessage(sender, "admin.duty.penalties-check",
                        stringPlaceholder("player", target.getName()),
                        numberPlaceholder("stage", data.getCurrentPenaltyStage()),
                        timePlaceholder("overrun", overrunSeconds)
                    );
                } else {
                    plugin.getMessageManager().sendMessage(sender, "admin.duty.penalties-none",
                        stringPlaceholder("player", target.getName()));
                }
                return true;
            }
            case "clear": {
                plugin.getDutyManager().adminClearPenalties(target);
                plugin.getMessageManager().sendMessage(sender, "admin.duty.penalties-cleared",
                    stringPlaceholder("player", target.getName()));
                plugin.getMessageManager().sendMessage(target, "admin.duty.penalties-cleared-notify",
                    stringPlaceholder("admin", sender.getName()));
                return true;
            }
            case "set": {
                if (args.length < 4) {
                    plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                        stringPlaceholder("command", "/corrections penalty set <player> <stage>"));
                    return true;
                }
                int stage;
                try {
                    stage = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    plugin.getMessageManager().sendMessage(sender, "universal.invalid-number",
                        stringPlaceholder("input", args[3]));
                    return true;
                }
                if (stage <= 0) {
                    plugin.getDutyManager().adminClearPenalties(target);
                    plugin.getMessageManager().sendMessage(sender, "admin.duty.penalties-cleared",
                        stringPlaceholder("player", target.getName()));
                    plugin.getMessageManager().sendMessage(target, "admin.duty.penalties-cleared-notify",
                        stringPlaceholder("admin", sender.getName()));
                    return true;
                }
                boolean applied = plugin.getDutyManager().adminSetPenaltyStage(target, stage);
                if (!applied) {
                    plugin.getMessageManager().sendMessage(sender, "universal.failed");
                    return true;
                }
                
                PlayerData data = plugin.getDataManager().getOrCreatePlayerData(target.getUniqueId(), target.getName());
                long overrunSeconds = Math.max(0L, (System.currentTimeMillis() - data.getPenaltyStartTime()) / 1000L);
                plugin.getMessageManager().sendMessage(sender, "admin.duty.penalties-check",
                    stringPlaceholder("player", target.getName()),
                    numberPlaceholder("stage", data.getCurrentPenaltyStage()),
                    timePlaceholder("overrun", overrunSeconds)
                );
                plugin.getMessageManager().sendMessage(target, "duty.penalties.stage-set-notify",
                    numberPlaceholder("stage", data.getCurrentPenaltyStage()),
                    stringPlaceholder("admin", sender.getName()));
                return true;
            }
            case "simulate": {
                if (args.length < 4) {
                    plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                        stringPlaceholder("command", "/corrections penalty simulate <player> <minutes>"));
                    return true;
                }
                int minutes;
                try {
                    minutes = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    plugin.getMessageManager().sendMessage(sender, "universal.invalid-number",
                        stringPlaceholder("input", args[3]));
                    return true;
                }
                boolean applied = plugin.getDutyManager().adminSimulateOverrun(target, minutes);
                if (!applied) {
                    plugin.getMessageManager().sendMessage(sender, "universal.failed");
                    return true;
                }
                PlayerData data = plugin.getDataManager().getOrCreatePlayerData(target.getUniqueId(), target.getName());
                long overrunSeconds = Math.max(0L, (System.currentTimeMillis() - data.getPenaltyStartTime()) / 1000L);
                plugin.getMessageManager().sendMessage(sender, "admin.duty.penalties-check",
                    stringPlaceholder("player", target.getName()),
                    numberPlaceholder("stage", data.getCurrentPenaltyStage()),
                    timePlaceholder("overrun", overrunSeconds)
                );
                plugin.getMessageManager().sendMessage(target, "duty.penalties.simulated-notify",
                    numberPlaceholder("minutes", minutes),
                    stringPlaceholder("admin", sender.getName()));
                return true;
            }
            default:
                plugin.getMessageManager().sendMessage(sender, "universal.unknown-subcommand",
                    stringPlaceholder("subcommand", action));
                return true;
        }
    }

    
    private List<String> handleContrabandTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(getOnlinePlayerNames());
        }
        
        return filterCompletions(completions, args);
    }
    
    private List<String> handleDutyBankTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("convert", "status"));
        }
        
        return filterCompletions(completions, args);
    }
    
    private List<String> handleTipsTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("duty", "contraband", "chase", "jail", "banking"));
        }
        
        return filterCompletions(completions, args);
    }

    private List<String> handleArrestMinigameTabComplete(CommandSender sender, String[] args) {
        
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(Arrays.asList("5", "10", "15", "20", "30", "60"));
        }
        return filterCompletions(completions, args);
    }
    
    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .collect(Collectors.toList());
    }
    
        private List<String> filterCompletions(List<String> completions, String[] args) {
        if (args.length == 0) return completions;
        
        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(completion -> completion.toLowerCase().startsWith(lastArg))
                .collect(Collectors.toList());
    }
    
    
    
    private boolean handleAreaCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("edencorrections.admin.area")) {
            plugin.getMessageManager().sendMessage(sender, "universal.no-permission");
            return true;
        }
        
        if (args.length == 0) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/area <list|add|remove|check> [area_name]"));
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "list":
                return handleAreaList(sender);
            case "add":
                return handleAreaAdd(sender, args);
            case "remove":
                return handleAreaRemove(sender, args);
            case "check":
                return handleAreaCheck(sender, args);
            default:
                plugin.getMessageManager().sendMessage(sender, "universal.unknown-subcommand",
                    stringPlaceholder("subcommand", subCommand));
                return true;
        }
    }
    
    private boolean handleAreaList(CommandSender sender) {
        String[] restrictedAreas = plugin.getConfigManager().getChaseRestrictedAreas();
        
        plugin.getMessageManager().sendMessage(sender, "system.admin.area.list-header");
        for (String area : restrictedAreas) {
            boolean exists = plugin.getWorldGuardUtils().regionExists(area);
            String status = exists ? "✅" : "❌";
            plugin.getMessageManager().sendMessage(sender, "system.admin.area.list-entry",
                stringPlaceholder("area", area),
                stringPlaceholder("status", status));
        }
        return true;
    }
    
    private boolean handleAreaAdd(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/area add <area_name>"));
            return true;
        }
        
        String areaName = args[1];
        
        
        if (!plugin.getWorldGuardUtils().regionExists(areaName)) {
            plugin.getMessageManager().sendMessage(sender, "system.admin.area.not-found",
                stringPlaceholder("area", areaName));
            return true;
        }
        
        boolean added = plugin.getConfigManager().addChaseRestrictedArea(areaName);
        if (added) {
            plugin.getMessageManager().sendMessage(sender, "system.admin.area.add-success",
                stringPlaceholder("area", areaName));
        } else {
            
            plugin.getMessageManager().sendMessage(sender, "system.admin.area.list-entry",
                stringPlaceholder("area", areaName),
                stringPlaceholder("description", "already present"));
        }
        return true;
    }
    
    private boolean handleAreaRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/area remove <area_name>"));
            return true;
        }
        
        String areaName = args[1];
        String[] currentAreas = plugin.getConfigManager().getChaseRestrictedAreas();
        
        boolean found = false;
        for (String area : currentAreas) {
            if (area.equalsIgnoreCase(areaName)) {
                found = true;
                break;
            }
        }
        
        if (!found) {
            plugin.getMessageManager().sendMessage(sender, "system.admin.area.not-in-list",
                stringPlaceholder("area", areaName));
            return true;
        }
        
        boolean removed = plugin.getConfigManager().removeChaseRestrictedArea(areaName);
        if (removed) {
            plugin.getMessageManager().sendMessage(sender, "system.admin.area.remove-success",
                stringPlaceholder("area", areaName));
        } else {
            plugin.getMessageManager().sendMessage(sender, "system.admin.area.not-in-list",
                stringPlaceholder("area", areaName));
        }
        return true;
    }
    
    private boolean handleAreaCheck(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length < 2) {
            
            Set<String> regionsAtPlayer = plugin.getWorldGuardUtils().getRegionsAtPlayer(player);
            plugin.getMessageManager().sendMessage(player, "system.admin.area.check-current",
                stringPlaceholder("regions", String.join(", ", regionsAtPlayer)));
            return true;
        }
        
        String areaName = args[1];
        boolean inRegion = plugin.getWorldGuardUtils().isPlayerInRegion(player, areaName);
        plugin.getMessageManager().sendMessage(player, "system.admin.area.check-result",
            stringPlaceholder("area", areaName),
            stringPlaceholder("result", inRegion ? "inside" : "outside"));
        return true;
    }
    
    private List<String> handleAreaTabComplete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("edencorrections.admin.area")) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            List<String> completions = Arrays.asList("list", "add", "remove", "check");
            return filterCompletions(completions, args);
        }
        
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "add":
                case "remove":
                    
                    return Arrays.asList(plugin.getConfigManager().getChaseRestrictedAreas());
                case "check":
                    
                    Set<String> allRegions = plugin.getWorldGuardUtils().getAllRegions();
                    return filterCompletions(new ArrayList<>(allRegions), args);
                default:
                    return new ArrayList<>();
            }
        }
        
        return new ArrayList<>();
    }
    
    private boolean handlePromotionCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        
        logger.info("DEBUG: /ecrankup command executed by " + player.getName());
        
        if (!player.hasPermission("edencorrections.progression.use")) {
            logger.info("DEBUG: Player " + player.getName() + " lacks progression permission");
            plugin.getMessageManager().sendMessage(player, "universal.no-permission");
            return true;
        }
        
        
        if (!plugin.getConfigManager().getConfig().getBoolean("progression.enabled", false)) {
            logger.info("DEBUG: Progression system is disabled");
            plugin.getMessageManager().sendMessage(player, "progression.disabled");
            return true;
        }
        
        
        if (!plugin.getDutyManager().hasGuardAccessOrBypass(player)) {
            logger.info("DEBUG: Player " + player.getName() + " is not a guard");
            if (plugin.getUnauthorizedAccessSpamManager().shouldWarnPlayer(player)) {
                plugin.getMessageManager().sendMessage(player, "progression.not-guard");
            }
            return true;
        }
        
        logger.info("DEBUG: Player " + player.getName() + " passed initial checks; validating promotion requirements...");

        
        plugin.getProgressionManager().attemptPromotion(player);
        return true;
    }
    
    private boolean handleProgressCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("edencorrections.progression.use")) {
            plugin.getMessageManager().sendMessage(player, "universal.no-permission");
            return true;
        }
        
        
        if (!plugin.getConfigManager().getConfig().getBoolean("progression.enabled", false)) {
            plugin.getMessageManager().sendMessage(player, "progression.disabled");
            return true;
        }
        
        
        if (!plugin.getDutyManager().hasGuardAccessOrBypass(player)) {
            if (plugin.getUnauthorizedAccessSpamManager().shouldWarnPlayer(player)) {
                plugin.getMessageManager().sendMessage(player, "progression.not-guard");
            }
            return true;
        }
        
        
        plugin.getProgressionManager().showProgressionStatus(player);
        return true;
    }
    
    private boolean handleLootCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("edencorrections.admin.loot")) {
            plugin.getMessageManager().sendMessage(sender, "universal.no-permission");
            return true;
        }
        
        if (args.length == 0) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/loot <test|reload|info> [rank]"));
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                plugin.getGuardLootManager().reloadConfiguration();
                plugin.getMessageManager().sendMessage(sender, "loot.reloaded");
                return true;
                
            case "test":
                if (!(sender instanceof Player)) {
                    plugin.getMessageManager().sendMessage(sender, "universal.player-only");
                    return true;
                }
                
                Player player = (Player) sender;
                String rank = args.length > 1 ? args[1] : plugin.getDutyManager().getPlayerGuardRank(player);
                
                if (rank == null) {
                    plugin.getMessageManager().sendMessage(player, "loot.no-rank");
                    return true;
                }
                
                
                plugin.getGuardLootManager().handleGuardDeath(player);
                plugin.getMessageManager().sendMessage(player, "loot.test-complete",
                    stringPlaceholder("rank", rank));
                return true;
                
            case "info":
                plugin.getMessageManager().sendMessage(sender, "loot.info");
                return true;
                
            default:
                plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                    stringPlaceholder("command", "/loot <test|reload|info> [rank]"));
                return true;
        }
    }
    
    private boolean handleRetrieveCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("edencorrections.guard.retrieve")) {
            plugin.getMessageManager().sendMessage(player, "universal.no-permission");
            return true;
        }
        
        
        if (!plugin.getDutyManager().hasGuardAccessOrBypass(player)) {
            if (plugin.getUnauthorizedAccessSpamManager().shouldWarnPlayer(player)) {
                plugin.getMessageManager().sendMessage(player, "retrieve.not-guard");
            }
            return true;
        }
        
        
        boolean restored = plugin.getDutyManager().restorePlayerInventoryPublic(player);
        
        if (restored) {
            plugin.getMessageManager().sendMessage(player, "retrieve.success");
        } else {
            plugin.getMessageManager().sendMessage(player, "retrieve.no-inventory");
        }
        
        return true;
    }

    private boolean handleBuybackCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-only");
            return true;
        }
        Player player = (Player) sender;
        
        plugin.getMessageManager().sendMessage(player, "retrieve.find-reaper");
        return true;
    }
    
    private boolean handleContrabandInfoCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("edencorrections.admin.contrabandinfo")) {
            plugin.getMessageManager().sendMessage(sender, "universal.no-permission");
            return true;
        }
        
        
        boolean enabled = plugin.getConfigManager().isContrabandEnabled();
        
        if (sender instanceof Player) {
            Player player = (Player) sender;
            
            
            plugin.getMessageManager().sendRawString(player, "<gradient:#FFA500:#FFD700><bold>=== CONTRABAND SYSTEM INFORMATION ===</bold></gradient>");
            
            
            String status = enabled ? "<color:#00FF00>ENABLED</color>" : "<color:#FF0000>DISABLED</color>";
            plugin.getMessageManager().sendRawString(player, "<color:#ADB5BD>System Status: " + status + "</color>");
            
            
            plugin.getMessageManager().sendRawString(player, "<color:#ADB5BD><bold>Configuration:</bold></color>");
            
            
            double maxDistance = plugin.getConfigManager().getMaxRequestDistance();
            String distanceInfo = maxDistance > 0 ? maxDistance + " blocks" : "No limit";
            plugin.getMessageManager().sendRawString(player, "<color:#ADB5BD>  Max Request Distance: <color:#FFB3C6>" + distanceInfo + "</color></color>");
            
            
            int complianceTimeout = plugin.getConfigManager().getContrabandCompliance();
            plugin.getMessageManager().sendRawString(player, "<color:#ADB5BD>  Compliance Timeout: <color:#FFB3C6>" + complianceTimeout + " seconds</color></color>");
            
            
            String confiscationMode = plugin.getConfigManager().getContrabandConfiscationMode();
            plugin.getMessageManager().sendRawString(player, "<color:#ADB5BD>  Confiscation Mode: <color:#FFB3C6>" + confiscationMode + "</color></color>");
            
            
            int storageDuration = plugin.getConfigManager().getContrabandStorageDurationSeconds();
            plugin.getMessageManager().sendRawString(player, "<color:#ADB5BD>  Storage Duration: <color:#FFB3C6>" + storageDuration + " seconds</color></color>");
            
            
            String pricingMode = plugin.getConfigManager().getContrabandPricingMode();
            plugin.getMessageManager().sendRawString(player, "<color:#ADB5BD>  Pricing Mode: <color:#FFB3C6>" + pricingMode + "</color></color>");
            
            if ("set".equalsIgnoreCase(pricingMode)) {
                int setPrice = plugin.getConfigManager().getContrabandSetPrice();
                plugin.getMessageManager().sendRawString(player, "<color:#ADB5BD>    Set Price: <color:#FFB3C6>" + setPrice + "</color></color>");
            } else if ("rng".equalsIgnoreCase(pricingMode)) {
                int minPrice = plugin.getConfigManager().getContrabandRngMinPrice();
                int maxPrice = plugin.getConfigManager().getContrabandRngMaxPrice();
                plugin.getMessageManager().sendRawString(player, "<color:#ADB5BD>    RNG Range: <color:#FFB3C6>" + minPrice + " - " + maxPrice + "</color></color>");
            }
            
            
            plugin.getMessageManager().sendRawString(player, "<color:#ADB5BD><bold>Contraband Types:</bold></color>");
            var contrabandTypes = plugin.getConfigManager().getContrabandTypes();
            if (contrabandTypes != null) {
                for (String type : contrabandTypes.getKeys(false)) {
                    String items = plugin.getConfigManager().getContrabandItems(type);
                    String description = plugin.getConfigManager().getContrabandDescription(type);
                    
                    if (items != null && !items.isEmpty()) {
                        plugin.getMessageManager().sendRawString(player, "<color:#ADB5BD>  <color:#FFB3C6>" + type + "</color><color:#ADB5BD>: " + description + "</color>");
                        
                        int itemCount = items.split(",").length;
                        plugin.getMessageManager().sendRawString(player, "<color:#ADB5BD>    Items: <color:#FFB3C6>" + itemCount + " configured</color></color>");
                    }
                }
            }
            
            
            plugin.getMessageManager().sendRawString(player, "<color:#ADB5BD><bold>Statistics:</bold></color>");
            
            
            int activeRequests = plugin.getContrabandManager().getActiveRequestCount();
            plugin.getMessageManager().sendRawString(player, "<color:#ADB5BD>  Active Requests: <color:#FFB3C6>" + activeRequests + "</color></color>");
            
            
            int storedContraband = plugin.getContrabandManager().getTotalStoredContrabandCount();
            plugin.getMessageManager().sendRawString(player, "<color:#ADB5BD>  Stored Contraband: <color:#FFB3C6>" + storedContraband + " items</color></color>");
            
            
            plugin.getMessageManager().sendRawString(player, "<gradient:#FFA500:#FFD700><bold>=====================================</bold></gradient>");
            
        } else {
            
            sender.sendMessage("=== CONTRABAND SYSTEM INFORMATION ===");
            
            
            String status = enabled ? "ENABLED" : "DISABLED";
            sender.sendMessage("System Status: " + status);
            
            
            sender.sendMessage("Configuration:");
            
            
            double maxDistance = plugin.getConfigManager().getMaxRequestDistance();
            String distanceInfo = maxDistance > 0 ? maxDistance + " blocks" : "No limit";
            sender.sendMessage("  Max Request Distance: " + distanceInfo);
            
            
            int complianceTimeout = plugin.getConfigManager().getContrabandCompliance();
            sender.sendMessage("  Compliance Timeout: " + complianceTimeout + " seconds");
            
            
            String confiscationMode = plugin.getConfigManager().getContrabandConfiscationMode();
            sender.sendMessage("  Confiscation Mode: " + confiscationMode);
            
            
            int storageDuration = plugin.getConfigManager().getContrabandStorageDurationSeconds();
            sender.sendMessage("  Storage Duration: " + storageDuration + " seconds");
            
            
            String pricingMode = plugin.getConfigManager().getContrabandPricingMode();
            sender.sendMessage("  Pricing Mode: " + pricingMode);
            
            if ("set".equalsIgnoreCase(pricingMode)) {
                int setPrice = plugin.getConfigManager().getContrabandSetPrice();
                sender.sendMessage("    Set Price: " + setPrice);
            } else if ("rng".equalsIgnoreCase(pricingMode)) {
                int minPrice = plugin.getConfigManager().getContrabandRngMinPrice();
                int maxPrice = plugin.getConfigManager().getContrabandRngMaxPrice();
                sender.sendMessage("    RNG Range: " + minPrice + " - " + maxPrice);
            }
            
            
            sender.sendMessage("Contraband Types:");
            var contrabandTypes = plugin.getConfigManager().getContrabandTypes();
            if (contrabandTypes != null) {
                for (String type : contrabandTypes.getKeys(false)) {
                    String items = plugin.getConfigManager().getContrabandItems(type);
                    String description = plugin.getConfigManager().getContrabandDescription(type);
                    
                    if (items != null && !items.isEmpty()) {
                        sender.sendMessage("  " + type + ": " + description);
                        
                        int itemCount = items.split(",").length;
                        sender.sendMessage("    Items: " + itemCount + " configured");
                    }
                }
            }
            
            
            sender.sendMessage("Statistics:");
            
            
            int activeRequests = plugin.getContrabandManager().getActiveRequestCount();
            sender.sendMessage("  Active Requests: " + activeRequests);
            
            
            int storedContraband = plugin.getContrabandManager().getTotalStoredContrabandCount();
            sender.sendMessage("  Stored Contraband: " + storedContraband + " items");
            
            sender.sendMessage("=====================================");
        }
        
        return true;
    }
    
    private List<String> handleLootTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("test", "reload", "info");
        } else if (args.length == 2 && "test".equals(args[0])) {
            return Arrays.asList("trainee", "private", "officer", "sergeant", "captain");
        }
        return new ArrayList<>();
    }
    
    
    private boolean handlePlayerStatModification(CommandSender sender, String playerName, String statName, String value) {
        if (!sender.hasPermission("edencorrections.admin")) {
            plugin.getMessageManager().sendMessage(sender, "universal.no-permission");
            return true;
        }
        
        
        PlayerData playerData = plugin.getDataManager().getPlayerDataByName(playerName);
        if (playerData == null) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-not-found",
                stringPlaceholder("player", playerName));
            return true;
        }
        
        Player targetPlayer = Bukkit.getPlayer(playerName);
        boolean isOnline = targetPlayer != null;
        
        
        StatModificationResult result = parseAndValidateStatModification(statName, value);
        if (!result.isValid()) {
            plugin.getMessageManager().sendMessage(sender, "admin.stats.invalid-stat",
                stringPlaceholder("stat", statName),
                stringPlaceholder("error", result.getErrorMessage()));
            return true;
        }
        
        
        boolean success = applyStatModification(playerData, result, targetPlayer);
        if (!success) {
            plugin.getMessageManager().sendMessage(sender, "admin.stats.modification-failed",
                stringPlaceholder("player", playerName),
                stringPlaceholder("stat", statName));
            return true;
        }
        
        
        plugin.getDataManager().savePlayerData(playerData);

        
        if (result.getStatName().equals("guardrank")) {
            try {
                org.bukkit.plugin.RegisteredServiceProvider<net.luckperms.api.LuckPerms> reg = org.bukkit.Bukkit.getServicesManager().getRegistration(net.luckperms.api.LuckPerms.class);
                if (reg != null) {
                    net.luckperms.api.LuckPerms luckPerms = reg.getProvider();
                    String trackName = plugin.getConfigManager().getConfig().getString("progression.rankup.track", "guard");
                    net.luckperms.api.track.Track track = luckPerms.getTrackManager().getTrack(trackName);
                    String targetGroup = playerData.getGuardRank();
                    java.util.UUID targetUuid = playerData.getPlayerId();
                    luckPerms.getUserManager().modifyUser(targetUuid, user -> {
                        if (track != null) {
                            for (String group : track.getGroups()) {
                                user.data().remove(net.luckperms.api.node.types.InheritanceNode.builder(group).build());
                            }
                        }
                        if (targetGroup != null && !targetGroup.trim().isEmpty() && !"none".equalsIgnoreCase(targetGroup)) {
                            user.data().add(net.luckperms.api.node.types.InheritanceNode.builder(targetGroup).build());
                        }
                    });
                }
            } catch (Exception e) {
                logger.warning("Failed to synchronize LuckPerms guard rank (offline-safe): " + e.getMessage());
            }
        }
        
        
        if (isOnline) {
            updateOnlinePlayerSystems(targetPlayer, playerData, result);
        }
        
        
        logger.info(String.format("ADMIN STAT MODIFICATION: %s modified %s's %s from %s to %s", 
            sender.getName(), playerName, statName, result.getOldValue(), result.getNewValue()));
        
        
        plugin.getMessageManager().sendMessage(sender, "admin.stats.modification-success",
            stringPlaceholder("player", playerName),
            stringPlaceholder("stat", statName),
            stringPlaceholder("oldvalue", String.valueOf(result.getOldValue())),
            stringPlaceholder("newvalue", String.valueOf(result.getNewValue())));
        
        return true;
    }
    
    
    private StatModificationResult parseAndValidateStatModification(String statName, String value) {
        StatModificationResult result = new StatModificationResult();
        result.setStatName(statName.toLowerCase());
        
        try {
            switch (result.getStatName()) {
                
                case "dutystarttime":
                case "offdutytime":
                case "gracedebttime":
                case "earnedoffdutytime":
                case "consumedoffdutytime":
                case "penaltystarttime":
                case "lastpenaltytime":
                case "lastslownessapplication":
                case "wantedexpiretime":
                case "chasestarttime":
                case "totaldutytime":
                    {
                        long longVal = Long.parseLong(value);
                        if (longVal < 0L) {
                            result.setValid(false);
                            result.setErrorMessage("Value must be non-negative");
                            return result;
                        }
                        result.setValue(longVal);
                    }
                    result.setType(StatType.LONG);
                    break;
                    
                
                case "sessionsearches":
                case "sessionsuccessfulsearches":
                case "sessionarrests":
                case "sessionkills":
                case "sessiondetections":
                case "totalkills":
                case "totalqualifyingkills":
                case "qualifyingkills":
                case "currentpenaltystage":
                case "wantedlevel":
                case "totalarrests":
                case "totalviolations":
                    {
                        int intVal = Integer.parseInt(value);
                        if (intVal < 0) {
                            result.setValid(false);
                            result.setErrorMessage("Value must be non-negative");
                            return result;
                        }
                        if (result.getStatName().equals("wantedlevel")) {
                            int maxWanted = plugin.getConfigManager().getMaxWantedLevel();
                            if (intVal > maxWanted) {
                                result.setValid(false);
                                result.setErrorMessage("wantedlevel must be between 0 and " + maxWanted);
                                return result;
                            }
                        }
                        result.setValue(intVal);
                    }
                    result.setType(StatType.INTEGER);
                    break;
                    
                
                case "isonduty":
                case "hasearnedbasetime":
                case "hasbeennotifiedofexpiredtime":
                case "beingchased":
                case "hasactivepenaltybossbar":
                    {
                        String boolStr = value.toLowerCase();
                        if (!boolStr.equals("true") && !boolStr.equals("false")) {
                            result.setValid(false);
                            result.setErrorMessage("Value must be true or false");
                            return result;
                        }
                        result.setValue(Boolean.parseBoolean(boolStr));
                    }
                    result.setType(StatType.BOOLEAN);
                    break;
                    
                
                case "wantedreason":
                    {
                        String s = value.trim();
                        if (s.length() > 256) {
                            result.setValid(false);
                            result.setErrorMessage("Value too long (max 256 chars)");
                            return result;
                        }
                        result.setValue(s);
                    }
                    result.setType(StatType.STRING);
                    break;
                case "guardrank":
                    {
                        String rankInput = value.toLowerCase().trim();
                        java.util.Map<String, String> rankMappings = plugin.getConfigManager().getRankMappings();
                        boolean matchesKey = rankMappings.containsKey(rankInput);
                        boolean matchesValue = rankMappings.values().stream().anyMatch(v -> v != null && v.equalsIgnoreCase(rankInput));
                        if (!matchesKey && !matchesValue) {
                            result.setValid(false);
                            result.setErrorMessage("Unknown guardrank. Allowed: " + String.join(", ", rankMappings.keySet()));
                            return result;
                        }
                        result.setValue(rankInput);
                        result.setType(StatType.STRING);
                    }
                    break;
                    
                
                case "chaser":
                    try {
                        result.setValue(UUID.fromString(value));
                        result.setType(StatType.UUID);
                    } catch (IllegalArgumentException e) {
                        result.setValid(false);
                        result.setErrorMessage("Invalid UUID format");
                        return result;
                    }
                    break;
                    
                default:
                    result.setValid(false);
                    result.setErrorMessage("Unknown stat: " + statName);
                    return result;
            }
            
            result.setValid(true);
            return result;
            
        } catch (NumberFormatException e) {
            result.setValid(false);
            result.setErrorMessage("Invalid number format for " + statName);
            return result;
        }
    }
    
    
    private boolean applyStatModification(PlayerData playerData, StatModificationResult result, Player targetPlayer) {
        try {
            switch (result.getStatName()) {
                
                case "dutystarttime":
                    result.setOldValue(playerData.getDutyStartTime());
                    playerData.setDutyStartTime((Long) result.getValue());
                    break;
                    
                case "offdutytime":
                    result.setOldValue(playerData.getOffDutyTime());
                    playerData.setOffDutyTime((Long) result.getValue());
                    break;
                    
                case "gracedebttime":
                    result.setOldValue(playerData.getGraceDebtTime());
                    playerData.setGraceDebtTime((Long) result.getValue());
                    break;
                    
                case "earnedoffdutytime":
                    result.setOldValue(playerData.getEarnedOffDutyTime());
                    playerData.setEarnedOffDutyTime((Long) result.getValue());
                    break;
                    
                case "consumedoffdutytime":
                    result.setOldValue(playerData.getConsumedOffDutyTime());
                    playerData.setConsumedOffDutyTime((Long) result.getValue());
                    break;
                    
                case "penaltystarttime":
                    result.setOldValue(playerData.getPenaltyStartTime());
                    playerData.setPenaltyStartTime((Long) result.getValue());
                    break;
                    
                case "lastpenaltytime":
                    result.setOldValue(playerData.getLastPenaltyTime());
                    playerData.setLastPenaltyTime((Long) result.getValue());
                    break;
                    
                case "lastslownessapplication":
                    result.setOldValue(playerData.getLastSlownessApplication());
                    playerData.setLastSlownessApplication((Long) result.getValue());
                    break;
                    
                case "wantedexpiretime":
                    result.setOldValue(playerData.getWantedExpireTime());
                    playerData.setWantedExpireTime((Long) result.getValue());
                    break;
                    
                case "chasestarttime":
                    result.setOldValue(playerData.getChaseStartTime());
                    playerData.setChaseStartTime((Long) result.getValue());
                    break;
                    
                case "totaldutytime":
                    result.setOldValue(playerData.getTotalDutyTime());
                    playerData.setTotalDutyTime((Long) result.getValue());
                    break;
                    
                
                case "sessionsearches":
                    result.setOldValue(playerData.getSessionSearches());
                    playerData.setSessionSearches((Integer) result.getValue());
                    break;
                    
                case "sessionsuccessfulsearches":
                    result.setOldValue(playerData.getSessionSuccessfulSearches());
                    playerData.setSessionSuccessfulSearches((Integer) result.getValue());
                    break;
                    
                case "sessionarrests":
                    result.setOldValue(playerData.getSessionArrests());
                    playerData.setSessionArrests((Integer) result.getValue());
                    break;
                    
                case "sessionkills":
                    result.setOldValue(playerData.getSessionKills());
                    playerData.setSessionKills((Integer) result.getValue());
                    break;
                    
                case "sessiondetections":
                    result.setOldValue(playerData.getSessionDetections());
                    playerData.setSessionDetections((Integer) result.getValue());
                    break;
                    
                case "currentpenaltystage":
                    result.setOldValue(playerData.getCurrentPenaltyStage());
                    playerData.setCurrentPenaltyStage((Integer) result.getValue());
                    break;
                    
                case "wantedlevel":
                    result.setOldValue(playerData.getWantedLevel());
                    playerData.setWantedLevel((Integer) result.getValue());
                    break;
                    
                case "totalarrests":
                    result.setOldValue(playerData.getTotalArrests());
                    playerData.setTotalArrests((Integer) result.getValue());
                    break;
                    
                case "totalviolations":
                    result.setOldValue(playerData.getTotalViolations());
                    playerData.setTotalViolations((Integer) result.getValue());
                    break;

                case "totalkills":
                case "totalqualifyingkills":
                case "qualifyingkills":
                    result.setOldValue(playerData.getTotalQualifyingKills());
                    playerData.setTotalQualifyingKills((Integer) result.getValue());
                    break;
                    
                
                case "isonduty":
                    result.setOldValue(playerData.isOnDuty());
                    playerData.setOnDuty((Boolean) result.getValue());
                    break;
                    
                case "hasearnedbasetime":
                    result.setOldValue(playerData.hasEarnedBaseTime());
                    playerData.setHasEarnedBaseTime((Boolean) result.getValue());
                    break;
                    
                case "hasbeennotifiedofexpiredtime":
                    result.setOldValue(playerData.hasBeenNotifiedOfExpiredTime());
                    playerData.setHasBeenNotifiedOfExpiredTime((Boolean) result.getValue());
                    break;
                    
                case "beingchased":
                    result.setOldValue(playerData.isBeingChased());
                    playerData.setBeingChased((Boolean) result.getValue());
                    break;
                    
                case "hasactivepenaltybossbar":
                    result.setOldValue(playerData.hasActivePenaltyBossBar());
                    playerData.setHasActivePenaltyBossBar((Boolean) result.getValue());
                    break;
                    
                
                case "guardrank":
                    result.setOldValue(playerData.getGuardRank());
                    playerData.setGuardRank((String) result.getValue());
                    break;
                    
                case "wantedreason":
                    result.setOldValue(playerData.getWantedReason());
                    playerData.setWantedReason((String) result.getValue());
                    break;
                    
                
                case "chaser":
                    result.setOldValue(playerData.getChaserGuard());
                    playerData.setChaserGuard((UUID) result.getValue());
                    break;
                    
                default:
                    return false;
            }
            
            result.setNewValue(result.getValue());
            return true;
            
        } catch (Exception e) {
            logger.warning("Failed to apply stat modification: " + e.getMessage());
            return false;
        }
    }
    
    
    private void updateOnlinePlayerSystems(Player targetPlayer, PlayerData playerData, StatModificationResult result) {
        try {
            
            if (result.getStatName().equals("wantedlevel") || result.getStatName().equals("wantedexpiretime") || result.getStatName().equals("wantedreason")) {
                if (playerData.isWanted()) {
                    plugin.getWantedManager().setWantedLevel(targetPlayer, playerData.getWantedLevel(), playerData.getWantedReason());
                } else {
                    plugin.getWantedManager().clearWantedLevel(targetPlayer);
                }
            }
            
            
            if (result.getStatName().equals("beingchased") || result.getStatName().equals("chaser") || result.getStatName().equals("chasestarttime")) {
                if (playerData.isBeingChased()) {
                    
                    Player chaser = Bukkit.getPlayer(playerData.getChaserGuard());
                    if (chaser != null && chaser.isOnline()) {
                        plugin.getChaseManager().startChase(chaser, targetPlayer);
                    }
                } else {
                    
                    ChaseData active = plugin.getDataManager().getChaseByTarget(playerData.getPlayerId());
                    if (active != null) {
                        plugin.getChaseManager().endChase(active.getChaseId(), "Admin modification");
                    }
                }
            }
            
            
            if (result.getStatName().equals("isonduty") || result.getStatName().equals("guardrank")) {
                

                
                if (result.getStatName().equals("guardrank")) {
                    try {
                        org.bukkit.plugin.RegisteredServiceProvider<net.luckperms.api.LuckPerms> reg = org.bukkit.Bukkit.getServicesManager().getRegistration(net.luckperms.api.LuckPerms.class);
                        if (reg != null) {
                            net.luckperms.api.LuckPerms luckPerms = reg.getProvider();
                            String trackName = plugin.getConfigManager().getConfig().getString("progression.rankup.track", "guard");
                            net.luckperms.api.track.Track track = luckPerms.getTrackManager().getTrack(trackName);
                            String targetGroup = playerData.getGuardRank();
                            luckPerms.getUserManager().modifyUser(targetPlayer.getUniqueId(), user -> {
                                if (track != null) {
                                    for (String group : track.getGroups()) {
                                        user.data().remove(net.luckperms.api.node.types.InheritanceNode.builder(group).build());
                                    }
                                }
                                if (targetGroup != null && !targetGroup.trim().isEmpty() && !"none".equalsIgnoreCase(targetGroup)) {
                                    user.data().add(net.luckperms.api.node.types.InheritanceNode.builder(targetGroup).build());
                                }
                            });
                            
                            if (targetGroup == null || targetGroup.trim().isEmpty() || "none".equalsIgnoreCase(targetGroup)) {
                                if (plugin.getDutyManager().isOnDuty(targetPlayer)) {
                                    PlayerData data = plugin.getDataManager().getOrCreatePlayerData(targetPlayer.getUniqueId(), targetPlayer.getName());
                                    plugin.getDutyManager().goOffDuty(targetPlayer, data);
                                } else {
                                    plugin.getDutyManager().restorePlayerInventoryPublic(targetPlayer);
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.warning("Failed to synchronize LuckPerms guard rank: " + e.getMessage());
                    }
                }
            }
            
            
            if (result.getStatName().startsWith("penalty") || result.getStatName().equals("currentpenaltystage")) {
                
                if (playerData.getCurrentPenaltyStage() == 0) {
                    plugin.getDutyManager().adminClearPenalties(targetPlayer);
                }
            }
            
            
            if (result.getStatName().equals("hasactivepenaltybossbar")) {
                if (!playerData.hasActivePenaltyBossBar()) {
                    plugin.getBossBarManager().hideBossBarByType(targetPlayer, "penalty");
                }
            }
            
        } catch (Exception e) {
            logger.warning("Failed to update online player systems: " + e.getMessage());
        }
    }
    
    
    private static class StatModificationResult {
        private String statName;
        private Object value;
        private StatType type;
        private boolean valid;
        private String errorMessage;
        private Object oldValue;
        private Object newValue;
        
        public String getStatName() { return statName; }
        public void setStatName(String statName) { this.statName = statName; }
        
        public Object getValue() { return value; }
        public void setValue(Object value) { this.value = value; }
        
        public StatType getType() { return type; }
        public void setType(StatType type) { this.type = type; }
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public Object getOldValue() { return oldValue; }
        public void setOldValue(Object oldValue) { this.oldValue = oldValue; }
        
        public Object getNewValue() { return newValue; }
        public void setNewValue(Object newValue) { this.newValue = newValue; }
    }
    
    
    private enum StatType {
        INTEGER, LONG, BOOLEAN, STRING, UUID
    }
    
    
    
    private boolean handleGuardSpawnCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("edencorrections.admin.guardspawn")) {
            plugin.getMessageManager().sendMessage(sender, "universal.no-permission");
            return true;
        }
        
        if (args.length == 0) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/guardspawn <set|clear|info>"));
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "set":
                return handleGuardSpawnSet(sender, args);
            case "clear":
                return handleGuardSpawnClear(sender, args);
            case "info":
                return handleGuardSpawnInfo(sender, args);
            default:
                plugin.getMessageManager().sendMessage(sender, "universal.unknown-subcommand",
                    stringPlaceholder("subcommand", subCommand));
                return true;
        }
    }
    
    private boolean handleGuardSpawnSet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        org.bukkit.Location location = player.getLocation();
        
        plugin.getConfigManager().setGuardSpawnLocation(location);
        
        plugin.getMessageManager().sendMessage(player, "guard-spawn.set-success",
            stringPlaceholder("world", location.getWorld().getName()),
            numberPlaceholder("x", (int) location.getX()),
            numberPlaceholder("y", (int) location.getY()),
            numberPlaceholder("z", (int) location.getZ()));
        
        logger.info("Guard spawn location set by " + player.getName() + " at " + 
            location.getWorld().getName() + " " + 
            (int) location.getX() + "," + (int) location.getY() + "," + (int) location.getZ());
        
        return true;
    }
    
    private boolean handleGuardSpawnClear(CommandSender sender, String[] args) {
        plugin.getConfigManager().setGuardSpawnLocation(null);
        
        plugin.getMessageManager().sendMessage(sender, "guard-spawn.cleared");
        logger.info("Guard spawn location cleared by " + sender.getName());
        
        return true;
    }
    
    private boolean handleGuardSpawnInfo(CommandSender sender, String[] args) {
        org.bukkit.Location spawnLocation = plugin.getConfigManager().getGuardSpawnLocation();
        
        if (spawnLocation == null) {
            plugin.getMessageManager().sendMessage(sender, "guard-spawn.not-set");
            return true;
        }
        
        plugin.getMessageManager().sendMessage(sender, "guard-spawn.info",
            stringPlaceholder("world", spawnLocation.getWorld().getName()),
            numberPlaceholder("x", (int) spawnLocation.getX()),
            numberPlaceholder("y", (int) spawnLocation.getY()),
            numberPlaceholder("z", (int) spawnLocation.getZ()),
            numberPlaceholder("yaw", (int) spawnLocation.getYaw()),
            numberPlaceholder("pitch", (int) spawnLocation.getPitch()));
        
        return true;
    }
    
    private List<String> handleGuardSpawnTabComplete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("edencorrections.admin.guardspawn")) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            List<String> completions = Arrays.asList("set", "clear", "info");
            return filterCompletions(completions, args);
        }
        
        return new ArrayList<>();
    }
    
    
    
    private boolean handleTransferCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        
        
        if (!plugin.getDutyManager().isSubjectToGuardRestrictions(player)) {
            plugin.getMessageManager().sendMessage(player, "transfer.not-guard");
            return true;
        }
        
        
        if (!plugin.getConfigManager().isGuardDefaultOnDuty()) {
            plugin.getMessageManager().sendMessage(player, "transfer.system-disabled");
            return true;
        }
        
        if (args.length == 0) {
            plugin.getMessageManager().sendMessage(player, "universal.invalid-usage",
                stringPlaceholder("command", "/transfer <hand|held> [to-offduty|to-onduty]"));
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "hand":
            case "held":
                return handleTransferHeld(player, args);
            case "info":
            case "help":
                return handleTransferInfo(player);
            default:
                plugin.getMessageManager().sendMessage(player, "universal.unknown-subcommand",
                    stringPlaceholder("subcommand", subCommand));
                return true;
        }
    }
    
    private boolean handleTransferHeld(Player player, String[] args) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        
        if (heldItem == null || heldItem.getType() == Material.AIR) {
            plugin.getMessageManager().sendMessage(player, "transfer.no-item-held");
            return true;
        }
        
        
        if (plugin.getDutyManager().isPlayerGuardKitItem(player, heldItem)) {
            plugin.getMessageManager().sendMessage(player, "transfer.guard-kit-restricted",
                stringPlaceholder("item", heldItem.getType().name()));
            return true;
        }
        
        
        List<String> blacklistedItems = plugin.getConfigManager().getTransferBlacklistedItems();
        if (blacklistedItems.contains(heldItem.getType().name())) {
            plugin.getMessageManager().sendMessage(player, "transfer.blacklisted-item",
                stringPlaceholder("item", heldItem.getType().name()));
            return true;
        }
        
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data == null) {
            plugin.getMessageManager().sendMessage(player, "universal.failed");
            return true;
        }
        
        
        String direction = "auto";
        if (args.length > 1) {
            String dirArg = args[1].toLowerCase();
            if (dirArg.equals("to-offduty") || dirArg.equals("offduty") || dirArg.equals("off")) {
                direction = "to-offduty";
            } else if (dirArg.equals("to-onduty") || dirArg.equals("onduty") || dirArg.equals("on")) {
                direction = "to-onduty";
            }
        }
        
        
        if (direction.equals("auto")) {
            if (data.isOnDuty()) {
                direction = "to-offduty"; 
            } else {
                direction = "to-onduty"; 
            }
        }
        
        
        return performItemTransfer(player, heldItem, direction, data);
    }
    
    private boolean performItemTransfer(Player player, ItemStack item, String direction, PlayerData data) {
        try {
            if (direction.equals("to-offduty")) {
                
                if (!data.isOnDuty()) {
                    plugin.getMessageManager().sendMessage(player, "transfer.must-be-on-duty");
                    return true;
                }
                
                
                if (plugin.getDutyManager().addItemToOffDutyInventory(player, item)) {
                    
                    player.getInventory().setItemInMainHand(null);
                    
                    plugin.getMessageManager().sendMessage(player, "transfer.to-offduty-success",
                        stringPlaceholder("item", item.getType().name()),
                        numberPlaceholder("amount", item.getAmount()));
                    
                    return true;
                } else {
                    plugin.getMessageManager().sendMessage(player, "transfer.to-offduty-failed");
                    return true;
                }
                
            } else if (direction.equals("to-onduty")) {
                
                if (data.isOnDuty()) {
                    plugin.getMessageManager().sendMessage(player, "transfer.must-be-off-duty");
                    return true;
                }
                
                
                if (plugin.getDutyManager().addItemToOnDutyInventory(player, item)) {
                    
                    player.getInventory().setItemInMainHand(null);
                    
                    plugin.getMessageManager().sendMessage(player, "transfer.to-onduty-success",
                        stringPlaceholder("item", item.getType().name()),
                        numberPlaceholder("amount", item.getAmount()));
                    
                    return true;
                } else {
                    plugin.getMessageManager().sendMessage(player, "transfer.to-onduty-failed");
                    return true;
                }
            }
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error during item transfer for " + player.getName() + ": " + e.getMessage());
            plugin.getMessageManager().sendMessage(player, "universal.failed");
            return true;
        }
    }
    
    private boolean handleTransferInfo(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data == null) {
            plugin.getMessageManager().sendMessage(player, "universal.failed");
            return true;
        }
        
        plugin.getMessageManager().sendMessage(player, "transfer.info-header");
        plugin.getMessageManager().sendMessage(player, "transfer.info-description");
        plugin.getMessageManager().sendMessage(player, "transfer.info-restrictions");
        
        if (data.isOnDuty()) {
            plugin.getMessageManager().sendMessage(player, "transfer.info-current-onduty");
        } else {
            plugin.getMessageManager().sendMessage(player, "transfer.info-current-offduty");
        }
        
        plugin.getMessageManager().sendMessage(player, "transfer.info-usage");
        
        return true;
    }
    
    private List<String> handleTransferTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("hand", "held", "info", "help");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("hand") || args[0].equalsIgnoreCase("held"))) {
            return Arrays.asList("to-offduty", "to-onduty", "offduty", "onduty");
        }
        return new ArrayList<>();
    }
    
    private boolean handleBuybackNpcCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("edencorrections.admin.buybacknpc")) {
            plugin.getMessageManager().sendMessage(sender, "universal.no-permission");
            return true;
        }
        
        if (args.length == 0) {
            return handleBuybackNpcHelp(sender);
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "add":
                return handleBuybackNpcAdd(sender, args);
            case "remove":
                return handleBuybackNpcRemove(sender, args);
            case "list":
                return handleBuybackNpcList(sender);
            case "info":
                return handleBuybackNpcInfo(sender);
            case "reload":
                return handleBuybackNpcReload(sender);
            case "help":
            default:
                return handleBuybackNpcHelp(sender);
        }
    }
    
    private boolean handleBuybackNpcAdd(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        Location location = player.getLocation();
        
        
        String locationString = String.format("%s:%.1f,%.1f,%.1f,%.1f,%.1f",
            location.getWorld().getName(),
            location.getX(),
            location.getY(),
            location.getZ(),
            location.getYaw(),
            location.getPitch()
        );
        
        
        if (plugin.getBuybackNpcManager() != null) {
            
            List<String> current = plugin.getBuybackNpcManager().getConfiguredLocationSummaries();
            
            
            
            List<String> toStore = new ArrayList<>();
            for (String s : current) {
                String base = s.endsWith("(current)") ? s.substring(0, s.length() - "(current)".length()).trim() : s;
                toStore.add(base);
            }
            toStore.add(locationString);
            plugin.getBuybackNpcManager().replaceStoredLocations(toStore);
        }
        
        
        if (plugin.getBuybackNpcManager() != null) {
            plugin.getBuybackNpcManager().reload();
        }
        
        plugin.getMessageManager().sendMessage(player, "system.info", 
            stringPlaceholder("message", "Added buyback NPC location: " + locationString));
        
        return true;
    }
    
    private boolean handleBuybackNpcRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/buybacknpc remove <index>"));
            return true;
        }
        
        try {
            int index = Integer.parseInt(args[1]);
            List<String> locations = new ArrayList<>();
            if (plugin.getBuybackNpcManager() != null) {
                
                for (String s : plugin.getBuybackNpcManager().getConfiguredLocationSummaries()) {
                    locations.add(s.endsWith("(current)") ? s.substring(0, s.length() - "(current)".length()).trim() : s);
                }
            }
            
            if (index < 0 || index >= locations.size()) {
                plugin.getMessageManager().sendMessage(sender, "system.error",
                    stringPlaceholder("message", "Invalid index. Use /buybacknpc list to see available locations."));
                return true;
            }
            
            String removed = locations.remove(index);
            if (plugin.getBuybackNpcManager() != null) {
                plugin.getBuybackNpcManager().replaceStoredLocations(locations);
            }
            
            
            if (plugin.getBuybackNpcManager() != null) {
                plugin.getBuybackNpcManager().reload();
            }
            
            plugin.getMessageManager().sendMessage(sender, "system.info",
                stringPlaceholder("message", "Removed buyback NPC location: " + removed));
            
        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(sender, "system.error",
                stringPlaceholder("message", "Invalid index number."));
        }
        
        return true;
    }
    
    private boolean handleBuybackNpcList(CommandSender sender) {
        if (plugin.getBuybackNpcManager() == null) {
            plugin.getMessageManager().sendMessage(sender, "system.error",
                stringPlaceholder("message", "Buyback NPC manager is not available."));
            return true;
        }
        
        List<String> summaries = plugin.getBuybackNpcManager().getConfiguredLocationSummaries();
        if (summaries.isEmpty()) {
            plugin.getMessageManager().sendMessage(sender, "system.info",
                stringPlaceholder("message", "No buyback NPC locations configured. Use '/corrections buybacknpc add' to add locations."));
            return true;
        }

        
        if (sender instanceof Player) {
            Player player = (Player) sender;
            plugin.getMessageManager().sendRawString(player,
                "<gradient:#9d4edd:#06ffa5>📍 Buyback NPC Locations</gradient> <color:#adb5bd>(" + summaries.size() + " total)</color>");
            for (int i = 0; i < summaries.size(); i++) {
                String s = summaries.get(i);
                boolean isCurrent = s.endsWith("(current)");
                String base = isCurrent ? s.substring(0, s.length() - "(current)".length()).trim() : s;
                String line = isCurrent
                    ? "<color:#51cf66>● " + (i + 1) + ": " + base + " (current)</color>"
                    : "<color:#adb5bd>○ " + (i + 1) + ": " + base + "</color>";
                plugin.getMessageManager().sendRawString(player, line);
            }
            return true;
        }

        
        plugin.getMessageManager().sendMessage(sender, "system.info",
            stringPlaceholder("message", "Buyback NPC Locations (" + summaries.size() + "):"));
        for (int i = 0; i < summaries.size(); i++) {
            plugin.getMessageManager().sendMessage(sender, "system.info",
                stringPlaceholder("message", "  " + (i + 1) + ": " + summaries.get(i)));
        }
        return true;
    }
    
    private boolean handleBuybackNpcInfo(CommandSender sender) {
        boolean enabled = plugin.getConfig().getBoolean("integration.buyback-npc.enabled", false);
        int interval = plugin.getConfig().getInt("integration.buyback-npc.spawn-interval-seconds", 300);
        String npcName = plugin.getConfig().getString("integration.buyback-npc.npc-name", "§dBuyback Broker");
        String npcType = plugin.getConfig().getString("integration.buyback-npc.npc-type", "WANDERING_TRADER");
        List<String> locations = plugin.getConfig().getStringList("integration.buyback-npc.locations");
        
        plugin.getMessageManager().sendMessage(sender, "system.info",
            stringPlaceholder("message", "Buyback NPC Configuration:"));
        plugin.getMessageManager().sendMessage(sender, "system.info",
            stringPlaceholder("message", "  Enabled: " + (enabled ? "Yes" : "No")));
        plugin.getMessageManager().sendMessage(sender, "system.info",
            stringPlaceholder("message", "  Interval: " + interval + " seconds"));
        plugin.getMessageManager().sendMessage(sender, "system.info",
            stringPlaceholder("message", "  NPC Name: " + npcName));
        plugin.getMessageManager().sendMessage(sender, "system.info",
            stringPlaceholder("message", "  NPC Type: " + npcType));
        plugin.getMessageManager().sendMessage(sender, "system.info",
            stringPlaceholder("message", "  Locations: " + locations.size()));
        
        return true;
    }
    
    private boolean handleBuybackNpcReload(CommandSender sender) {
        if (plugin.getBuybackNpcManager() != null) {
            plugin.getBuybackNpcManager().reload();
            plugin.getMessageManager().sendMessage(sender, "system.info",
                stringPlaceholder("message", "Buyback NPC manager reloaded successfully."));
        } else {
            plugin.getMessageManager().sendMessage(sender, "system.error",
                stringPlaceholder("message", "Buyback NPC manager not available."));
        }
        
        return true;
    }
    
    private boolean handleBuybackNpcHelp(CommandSender sender) {
        plugin.getMessageManager().sendMessage(sender, "system.info",
            stringPlaceholder("message", "Buyback NPC Commands:"));
        plugin.getMessageManager().sendMessage(sender, "system.info",
            stringPlaceholder("message", "  /buybacknpc add - Add current location as spawn point"));
        plugin.getMessageManager().sendMessage(sender, "system.info",
            stringPlaceholder("message", "  /buybacknpc remove <index> - Remove location by index"));
        plugin.getMessageManager().sendMessage(sender, "system.info",
            stringPlaceholder("message", "  /buybacknpc list - List all configured locations"));
        plugin.getMessageManager().sendMessage(sender, "system.info",
            stringPlaceholder("message", "  /buybacknpc info - Show current configuration"));
        plugin.getMessageManager().sendMessage(sender, "system.info",
            stringPlaceholder("message", "  /buybacknpc reload - Reload NPC manager"));
        plugin.getMessageManager().sendMessage(sender, "system.info",
            stringPlaceholder("message", "  /buybacknpc help - Show this help"));
        
        return true;
    }
    
    private List<String> handleBuybackNpcTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("add", "remove", "list", "info", "reload", "help");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            List<String> locations = plugin.getConfig().getStringList("integration.buyback-npc.locations");
            List<String> indices = new ArrayList<>();
            for (int i = 0; i < locations.size(); i++) {
                indices.add(String.valueOf(i));
            }
            return indices;
        }
        return new ArrayList<>();
    }

    private List<String> handleLockerTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("tag", "untag", "info"));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if ("tag".equals(subCommand)) {
                
                completions.addAll(getOnlinePlayerNames());
            }
        }
        
        return filterCompletions(completions, args);
    }

    private List<String> handleDemoteTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(getOnlinePlayerNames());
        } else if (args.length == 2) {
            completions.addAll(Arrays.asList("none", "trainee", "private", "officer", "sergeant", "captain"));
        }
        return filterCompletions(completions, args);
    }
} 