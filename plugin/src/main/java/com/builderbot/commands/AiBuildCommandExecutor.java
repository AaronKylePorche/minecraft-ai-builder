package com.builderbot.commands;

import com.builderbot.BuilderBotPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command executor for /bot aibuild <description>
 * Triggers AI-powered structure generation and pasting.
 */
public class AiBuildCommandExecutor implements CommandExecutor {

    private final BuilderBotPlugin plugin;

    public AiBuildCommandExecutor(BuilderBotPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players");
            return true;
        }

        Player player = (Player) sender;

        // Require at least one word for the description
        if (args.length < 2) {  // args[0] = "aibuild", args[1+] = description
            player.sendMessage("§c§lUsage: §7/bot aibuild <structure description>");
            player.sendMessage("§7Examples:");
            player.sendMessage("  §f/bot aibuild small wizard tower with balcony");
            player.sendMessage("  §f/bot aibuild desert fortress with two towers");
            player.sendMessage("  §f/bot aibuild spooky treehouse with cobwebs");
            return true;
        }

        // Join description words (skip "aibuild" arg)
        StringBuilder description = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) description.append(" ");
            description.append(args[i]);
        }

        String prompt = description.toString().trim();

        // Validate prompt length
        if (prompt.length() < 3) {
            player.sendMessage("§c§lError: §7Description too short. Be more specific!");
            return true;
        }

        if (prompt.length() > 200) {
            player.sendMessage("§c§lError: §7Description too long (max 200 characters)");
            return true;
        }

        // Check if microservice is available
        if (!plugin.getAiBuildService().isServiceAvailable()) {
            player.sendMessage("§c§lError: §7BuilderGPT service is not running!");
            player.sendMessage("§7Please start the AI stack first:");
            player.sendMessage("  §fRun Start.bat in the Minecraft AI Builder folder");
            return true;
        }

        // Send to AI service
        player.sendMessage("§e§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§e§lBuilderGPT AI Build");
        player.sendMessage("§7Request: §f" + prompt);
        player.sendMessage("§e§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        plugin.getAiBuildService().requestBuild(player, prompt);

        return true;
    }
}
