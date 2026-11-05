package com.builderbot;

import com.builderbot.ai.AiBuildService;
import com.builderbot.bot.BotManager;
import com.builderbot.commands.BotCommandExecutor;
import com.builderbot.commands.ChatListener;
import com.builderbot.conversation.ConversationManager;
import com.builderbot.worldedit.WorldEditPasteService;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for BuilderBot
 * A Minecraft bot that follows players and builds structures based on chat commands
 */
public class BuilderBotPlugin extends JavaPlugin {

    private static BuilderBotPlugin instance;

    // Managers
    private BotManager botManager;
    private ConversationManager conversationManager;
    private AiBuildService aiBuildService;
    private WorldEditPasteService worldEditPasteService;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Check for Citizens dependency
        if (!getServer().getPluginManager().isPluginEnabled("Citizens")) {
            getLogger().severe("==========================================");
            getLogger().severe("Citizens plugin not found!");
            getLogger().severe("BuilderBot requires Citizens to function.");
            getLogger().severe("Download it from: https://www.spigotmc.org/resources/citizens.13811/");
            getLogger().severe("==========================================");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("Citizens dependency found!");

        // Initialize Bot Manager (Phase 1)
        botManager = new BotManager(this);
        getLogger().info("BotManager initialized!");

        // Initialize Conversation Manager (Phase 3)
        conversationManager = new ConversationManager(this);
        getLogger().info("ConversationManager initialized!");

        // Check for WorldEdit dependency (required for AI building)
        if (getServer().getPluginManager().isPluginEnabled("WorldEdit")) {
            // Initialize AI services (Phase 8)
            worldEditPasteService = new WorldEditPasteService(this);
            aiBuildService = new AiBuildService(this);
            getLogger().info("WorldEdit found! AI building enabled.");
        } else {
            getLogger().warning("WorldEdit not found. AI building (/bot aibuild) will be disabled.");
            getLogger().warning("Download WorldEdit from: https://hangar.papermc.io/EngineHub/WorldEdit");
        }

        // Register commands (Phase 1)
        BotCommandExecutor commandExecutor = new BotCommandExecutor(this, botManager);
        getCommand("bot").setExecutor(commandExecutor);
        getCommand("bot").setTabCompleter(commandExecutor);
        getLogger().info("Commands registered!");

        // Register test orientation command (Phase 4)
        getCommand("testorientation").setExecutor(new com.builderbot.commands.TestOrientationCommand());
        getLogger().info("Test commands registered!");

        // Register chat listener (Phase 2 & 3)
        getServer().getPluginManager().registerEvents(new ChatListener(conversationManager), this);
        getLogger().info("ChatListener registered!");

        getLogger().info("BuilderBot v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        // Cleanup all bots
        if (botManager != null) {
            botManager.despawnAllBots();
            getLogger().info("All bots despawned!");
        }

        getLogger().info("BuilderBot disabled!");
    }

    /**
     * Get the plugin instance
     */
    public static BuilderBotPlugin getInstance() {
        return instance;
    }

    /**
     * Get the AI build service (may be null if WorldEdit not loaded)
     */
    public AiBuildService getAiBuildService() {
        return aiBuildService;
    }

    /**
     * Get the WorldEdit paste service (may be null if WorldEdit not loaded)
     */
    public WorldEditPasteService getWorldEditPasteService() {
        return worldEditPasteService;
    }
}
