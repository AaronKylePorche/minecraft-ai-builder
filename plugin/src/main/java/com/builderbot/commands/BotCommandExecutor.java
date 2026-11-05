package com.builderbot.commands;

import com.builderbot.BuilderBotPlugin;
import com.builderbot.bot.BotManager;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles /bot commands
 */
public class BotCommandExecutor implements CommandExecutor, TabCompleter {

    private final BuilderBotPlugin plugin;
    private final BotManager botManager;
    private final AiBuildCommandExecutor aiBuildExecutor;

    public BotCommandExecutor(BuilderBotPlugin plugin, BotManager botManager) {
        this.plugin = plugin;
        this.botManager = botManager;
        this.aiBuildExecutor = new AiBuildCommandExecutor(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Only players can use this command
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        // Show help if no arguments
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "summon":
                handleSummon(player);
                break;

            case "follow":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /bot follow <on|off>");
                    return true;
                }
                handleFollow(player, args[1]);
                break;

            case "despawn":
            case "dismiss":
                handleDespawn(player);
                break;

            case "build":
                // TODO: Phase 3 - will connect to conversation system
                player.sendMessage("§eBuilding feature coming soon!");
                player.sendMessage("§7(Will be implemented in Phase 3)");
                break;

            case "aibuild":
                // Phase 8 - AI-powered building
                if (plugin.getAiBuildService() == null) {
                    player.sendMessage("§c§lError: §7WorldEdit not installed! AI building disabled.");
                    player.sendMessage("§7Download WorldEdit from: https://hangar.papermc.io/EngineHub/WorldEdit");
                    return true;
                }
                // Delegate to AiBuildCommandExecutor
                aiBuildExecutor.onCommand(sender, command, label, args);
                break;

            case "help":
                sendHelp(player);
                break;

            default:
                player.sendMessage("§cUnknown command: §e/bot " + subCommand);
                player.sendMessage("§7Type §e/bot help §7for available commands");
                break;
        }

        return true;
    }

    /**
     * Handle /bot summon
     */
    private void handleSummon(Player player) {
        if (botManager.hasBot(player)) {
            player.sendMessage("§eYou already have a builder bot!");
            player.sendMessage("§7Use §e/bot despawn §7to remove it first");
            return;
        }

        NPC npc = botManager.spawnBot(player);
        player.sendMessage("§aBuilder bot summoned!");
        player.sendMessage("§7Your bot will follow you around");
        player.sendMessage("§7Use §e/bot follow off §7to stop following");
    }

    /**
     * Handle /bot follow <on|off>
     */
    private void handleFollow(Player player, String mode) {
        if (!botManager.hasBot(player)) {
            player.sendMessage("§cYou don't have a builder bot!");
            player.sendMessage("§7Use §e/bot summon §7to spawn one");
            return;
        }

        boolean follow;
        if (mode.equalsIgnoreCase("on") || mode.equalsIgnoreCase("true")) {
            follow = true;
        } else if (mode.equalsIgnoreCase("off") || mode.equalsIgnoreCase("false")) {
            follow = false;
        } else {
            player.sendMessage("§cUsage: /bot follow <on|off>");
            return;
        }

        botManager.setFollowing(player, follow);

        if (follow) {
            player.sendMessage("§aBot will now follow you!");
        } else {
            player.sendMessage("§eBot will stay in place");
        }
    }

    /**
     * Handle /bot despawn
     */
    private void handleDespawn(Player player) {
        if (!botManager.hasBot(player)) {
            player.sendMessage("§cYou don't have a builder bot!");
            return;
        }

        botManager.despawnBot(player);
        player.sendMessage("§7Builder bot despawned");
    }

    /**
     * Send help message
     */
    private void sendHelp(Player player) {
        player.sendMessage("§6§l=== Builder Bot Commands ===");
        player.sendMessage("§e/bot summon §7- Spawn your builder bot");
        player.sendMessage("§e/bot follow <on|off> §7- Toggle bot following");
        player.sendMessage("§e/bot despawn §7- Remove your bot");
        player.sendMessage("§e/bot build <type> §7- Build a structure (coming soon)");

        // Show AI build command only if WorldEdit is available
        if (plugin.getAiBuildService() != null) {
            player.sendMessage("§e/bot aibuild <description> §7- AI-powered building");
            player.sendMessage("  §7Example: §e/bot aibuild small wizard tower");
        }

        player.sendMessage("§7Or type: §ebot build me a <structure>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // First argument - subcommands
            List<String> subCommands = new ArrayList<>(Arrays.asList("summon", "follow", "despawn", "build", "help"));

            // Add aibuild if WorldEdit is available
            if (plugin.getAiBuildService() != null) {
                subCommands.add("aibuild");
            }

            String partial = args[0].toLowerCase();
            for (String sub : subCommands) {
                if (sub.startsWith(partial)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            // Second argument
            if (args[0].equalsIgnoreCase("follow")) {
                // /bot follow <on|off>
                completions.addAll(Arrays.asList("on", "off"));
            } else if (args[0].equalsIgnoreCase("build")) {
                // /bot build <type> - will add structure types in Phase 2
                completions.add("treehouse");
            }
        }

        return completions;
    }
}
