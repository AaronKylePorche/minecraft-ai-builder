package com.builderbot.bot;

import com.builderbot.BuilderBotPlugin;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages NPC bots for players
 * Handles spawning, despawning, and following behavior
 */
public class BotManager implements Listener {

    private final BuilderBotPlugin plugin;
    private final Map<UUID, NPC> playerBots;
    private final Map<UUID, BotData> botData;
    private final double followDistance;
    private int followTaskId = -1;

    public BotManager(BuilderBotPlugin plugin) {
        this.plugin = plugin;
        this.playerBots = new HashMap<>();
        this.botData = new HashMap<>();
        this.followDistance = plugin.getConfig().getDouble("bot.follow-distance", 3.0);

        // Register event listener for player quit
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Schedule delayed cleanup (wait for Citizens to load NPCs from disk)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            cleanupOrphanedBots();
        }, 20L); // 20 ticks = 1 second delay

        // Start follow task
        startFollowTask();
    }

    /**
     * Spawn a bot for the given player
     */
    public NPC spawnBot(Player player) {
        UUID playerId = player.getUniqueId();

        // If bot already exists, just teleport it to player
        if (playerBots.containsKey(playerId)) {
            NPC existing = playerBots.get(playerId);
            existing.spawn(player.getLocation());
            return existing;
        }

        // Create new NPC
        NPCRegistry registry = CitizensAPI.getNPCRegistry();
        NPC npc = registry.createNPC(EntityType.PLAYER, "§e" + player.getName() + "'s Builder");

        // Spawn at player location
        npc.spawn(player.getLocation());

        // Store NPC and data
        playerBots.put(playerId, npc);
        BotData data = new BotData(playerId);
        data.setLastKnownPosition(player.getLocation());
        botData.put(playerId, data);

        plugin.getLogger().info("Spawned bot for player: " + player.getName());

        return npc;
    }

    /**
     * Despawn a player's bot
     */
    public void despawnBot(Player player) {
        UUID playerId = player.getUniqueId();
        despawnBot(playerId);
    }

    /**
     * Despawn a bot by player UUID
     */
    public void despawnBot(UUID playerId) {
        NPC npc = playerBots.remove(playerId);
        if (npc != null) {
            npc.despawn();
            npc.destroy();
        }
        botData.remove(playerId);
    }

    /**
     * Get a player's bot NPC
     */
    public NPC getBot(Player player) {
        return playerBots.get(player.getUniqueId());
    }

    /**
     * Check if player has a bot
     */
    public boolean hasBot(Player player) {
        return playerBots.containsKey(player.getUniqueId());
    }

    /**
     * Set whether the bot should follow the player
     */
    public void setFollowing(Player player, boolean following) {
        UUID playerId = player.getUniqueId();
        BotData data = botData.get(playerId);
        if (data != null) {
            data.setFollowing(following);
        }
    }

    /**
     * Check if a player's bot is following
     */
    public boolean isFollowing(Player player) {
        BotData data = botData.get(player.getUniqueId());
        return data != null && data.isFollowing();
    }

    /**
     * Despawn all bots (called on plugin disable)
     */
    public void despawnAllBots() {
        // Stop follow task
        if (followTaskId != -1) {
            Bukkit.getScheduler().cancelTask(followTaskId);
            followTaskId = -1;
        }

        // Despawn all NPCs
        for (UUID playerId : new HashMap<>(playerBots).keySet()) {
            despawnBot(playerId);
        }
    }

    /**
     * Clean up orphaned bots from previous sessions
     * Citizens persists NPCs, but our tracking is in-memory only
     */
    private void cleanupOrphanedBots() {
        NPCRegistry registry = CitizensAPI.getNPCRegistry();
        int removed = 0;

        // Find all NPCs with our naming pattern
        for (NPC npc : registry) {
            String name = npc.getName();
            // Our bots are named "{PlayerName}'s Builder"
            if (name != null && name.endsWith("'s Builder")) {
                npc.destroy();
                removed++;
            }
        }

        if (removed > 0) {
            plugin.getLogger().info("Cleaned up " + removed + " orphaned builder bot(s)");
        }
    }

    /**
     * Start the periodic task that makes bots follow their players
     */
    private void startFollowTask() {
        followTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<UUID, NPC> entry : playerBots.entrySet()) {
                UUID playerId = entry.getKey();
                NPC npc = entry.getValue();
                BotData data = botData.get(playerId);

                // Skip if not following
                if (data == null || !data.isFollowing()) {
                    continue;
                }

                // Get player
                Player player = Bukkit.getPlayer(playerId);
                if (player == null || !player.isOnline()) {
                    continue;
                }

                // Check if NPC is spawned
                if (!npc.isSpawned()) {
                    npc.spawn(player.getLocation());
                    continue;
                }

                // Get NPC location
                Location npcLoc = npc.getStoredLocation();
                Location playerLoc = player.getLocation();

                // Check if they're in the same world
                if (npcLoc.getWorld() != playerLoc.getWorld()) {
                    npc.despawn();
                    npc.spawn(playerLoc);
                    continue;
                }

                // Calculate distance
                double distance = npcLoc.distance(playerLoc);

                // If too far, move towards player
                if (distance > followDistance) {
                    // Use Citizens navigator - pass the player entity, not location
                    npc.getNavigator().setTarget(player, false);
                    npc.getNavigator().getLocalParameters().speedModifier(1.2f); // Slightly faster than player

                    // Update last known position
                    data.setLastKnownPosition(playerLoc);
                } else if (distance < 2.5) {
                    // Too close, cancel navigation and maintain distance
                    npc.getNavigator().cancelNavigation();
                } else {
                    // In range, look at player
                    npc.faceLocation(playerLoc);
                }
            }
        }, 0L, 10L).getTaskId(); // Run every 10 ticks (0.5 seconds)
    }

    /**
     * Handle player quit - despawn their bot
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        despawnBot(event.getPlayer());
    }
}
