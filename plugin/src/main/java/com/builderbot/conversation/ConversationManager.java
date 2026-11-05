package com.builderbot.conversation;

import com.builderbot.BuilderBotPlugin;
import com.builderbot.structure.StructureType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages multi-turn conversations with players
 * Handles the flow: command → location → size → build
 */
public class ConversationManager {

    private final BuilderBotPlugin plugin;
    private final Map<UUID, ConversationContext> activeConversations;
    private final long timeoutMs;
    // private final StructureBuilderFactory builderFactory; // Will add in Phase 5

    public ConversationManager(BuilderBotPlugin plugin) {
        this.plugin = plugin;
        this.activeConversations = new HashMap<>();
        this.timeoutMs = plugin.getConfig().getLong("bot.conversation-timeout-seconds", 120) * 1000;

        // Start timeout checker task
        startTimeoutChecker();
    }

    /**
     * Start a new build conversation
     *
     * @param player The player initiating the conversation
     * @param type   The structure type to build
     */
    public void startBuildConversation(Player player, StructureType type) {
        UUID playerId = player.getUniqueId();

        // Create new conversation context
        ConversationContext context = new ConversationContext(playerId, type);
        context.setState(ConversationState.AWAITING_LOCATION);
        activeConversations.put(playerId, context);

        // Send initial prompt
        player.sendMessage("§a§lOkay, a " + type.getPrimaryName() + "!");
        player.sendMessage("§eStand where you want it built, face the direction you want it to face,");
        player.sendMessage("§ethen type §6'here' §ein chat");
    }

    /**
     * Handle a response from a player in an active conversation
     *
     * @param player  The player
     * @param message Their message
     */
    public void handleResponse(Player player, String message) {
        UUID playerId = player.getUniqueId();
        ConversationContext context = activeConversations.get(playerId);

        if (context == null) {
            return; // No active conversation
        }

        switch (context.getState()) {
            case AWAITING_LOCATION:
                handleLocationResponse(player, context, message);
                break;

            case AWAITING_SIZE:
                handleSizeResponse(player, context, message);
                break;

            default:
                // Ignore messages in other states
                break;
        }
    }

    /**
     * Handle location response ("here")
     */
    private void handleLocationResponse(Player player, ConversationContext context, String message) {
        if (!message.equalsIgnoreCase("here")) {
            player.sendMessage("§cType §e'here' §cwhen you're in position");
            return;
        }

        // Capture player's location and yaw
        context.setOrigin(player.getLocation());
        context.setPlayerYaw(player.getLocation().getYaw());
        context.setState(ConversationState.AWAITING_SIZE);

        player.sendMessage("§aGot it! Location captured.");
        player.sendMessage("§eWhat size do you want?");
        player.sendMessage("§6small §7/ §6medium §7/ §6large");
    }

    /**
     * Handle size response
     */
    private void handleSizeResponse(Player player, ConversationContext context, String message) {
        String size = message.toLowerCase().trim();

        if (!size.matches("small|medium|large")) {
            player.sendMessage("§cPlease choose: §esmall§c, §emedium§c, or §elarge");
            return;
        }

        context.setSize(size);
        context.setState(ConversationState.BUILDING);

        player.sendMessage("§a§l" + capitalize(size) + " " +
                context.getStructureType().getPrimaryName() + " coming up!");

        // Start building (Phase 5+)
        startBuilding(player, context);

        // Remove conversation
        activeConversations.remove(player.getUniqueId());
    }

    /**
     * Start the actual building process
     * TODO: Wire up to StructureBuilder in Phase 5
     */
    private void startBuilding(Player player, ConversationContext context) {
        // For Phase 3, just show a placeholder message
        player.sendMessage("§7[Building system coming in Phases 4-6]");
        player.sendMessage("§7Would build: §e" + context.getSize() + " " +
                context.getStructureType().getPrimaryName());
        player.sendMessage("§7At: §e" + formatLocation(context.getOrigin()));
        player.sendMessage("§7Facing: §e" + formatYaw(context.getPlayerYaw()));

        // TODO: In Phase 5, create StructureParameters and call builder:
        // StructureParameters params = new StructureParameters(context.getSize(), context.getStructureType());
        // StructureBuilder builder = builderFactory.getBuilder(context.getStructureType());
        // builder.build(player.getWorld(), context.getOrigin(), context.getPlayerYaw(), params);
    }

    /**
     * Check if a player has an active conversation
     */
    public boolean hasActiveConversation(Player player) {
        return activeConversations.containsKey(player.getUniqueId());
    }

    /**
     * Start the timeout checker task
     */
    private void startTimeoutChecker() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            activeConversations.entrySet().removeIf(entry -> {
                if (entry.getValue().isExpired(timeoutMs)) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null && player.isOnline()) {
                        player.sendMessage("§cBuild conversation timed out");
                    }
                    return true;
                }
                return false;
            });
        }, 0L, 20L * 30); // Check every 30 seconds
    }

    /**
     * Helper: Capitalize first letter
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Helper: Format location for display
     */
    private String formatLocation(org.bukkit.Location loc) {
        return String.format("X:%.1f Y:%.1f Z:%.1f", loc.getX(), loc.getY(), loc.getZ());
    }

    /**
     * Helper: Format yaw for display
     */
    private String formatYaw(float yaw) {
        // Normalize to 0-360
        yaw = (yaw % 360 + 360) % 360;

        if (yaw >= 315 || yaw < 45) return "South";
        if (yaw >= 45 && yaw < 135) return "West";
        if (yaw >= 135 && yaw < 225) return "North";
        return "East";
    }
}
