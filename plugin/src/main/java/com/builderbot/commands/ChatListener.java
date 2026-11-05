package com.builderbot.commands;

import com.builderbot.conversation.ConversationManager;
import com.builderbot.structure.StructureType;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Listens to chat messages and detects natural language bot commands
 * Example: "bot build me a treehouse"
 */
public class ChatListener implements Listener {

    // Pattern to match: "bot build [me] [a] <structure>"
    // Examples: "bot build treehouse", "bot build me a tree", "bot build a masjid"
    private static final Pattern BOT_BUILD_PATTERN =
        Pattern.compile("bot\\s+build\\s+(?:me\\s+)?(?:a\\s+)?(\\w+(?:\\s+\\w+)*)");

    private final ConversationManager conversationManager;

    public ChatListener(ConversationManager conversationManager) {
        this.conversationManager = conversationManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        // Extract message text from Adventure API
        String message;
        if (event.message() instanceof TextComponent) {
            message = ((TextComponent) event.message()).content();
        } else {
            message = event.message().toString();
        }

        String lowerMessage = message.toLowerCase();

        // Check if player has active conversation
        if (conversationManager.hasActiveConversation(player)) {
            event.setCancelled(true);
            conversationManager.handleResponse(player, message);
            return;
        }

        // Try to parse as bot command
        Matcher matcher = BOT_BUILD_PATTERN.matcher(lowerMessage);
        if (matcher.find()) {
            String structureInput = matcher.group(1).trim();

            // Cancel the event so the command isn't broadcast in chat
            event.setCancelled(true);

            // Validate structure type
            StructureType type = StructureType.fromString(structureInput);
            if (type == null) {
                player.sendMessage("§cI don't know how to build a §e" + structureInput);
                player.sendMessage("§7Try: treehouse, bridge, or masjid");
                return;
            }

            // Start conversation
            conversationManager.startBuildConversation(player, type);
        }
    }
}
