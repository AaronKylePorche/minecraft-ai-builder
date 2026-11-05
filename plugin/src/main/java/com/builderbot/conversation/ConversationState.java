package com.builderbot.conversation;

/**
 * Represents the current state of a player's conversation with the bot
 */
public enum ConversationState {

    /**
     * No active conversation
     */
    IDLE,

    /**
     * Waiting for player to stand at location and type "here"
     */
    AWAITING_LOCATION,

    /**
     * Waiting for player to specify size (small/medium/large)
     */
    AWAITING_SIZE,

    /**
     * Building in progress
     */
    BUILDING
}
