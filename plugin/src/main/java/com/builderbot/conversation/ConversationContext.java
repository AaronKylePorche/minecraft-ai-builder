package com.builderbot.conversation;

import com.builderbot.structure.StructureType;
import org.bukkit.Location;

import java.util.UUID;

/**
 * Holds the conversation data for a single player
 * Tracks what structure they want to build, where, and what size
 */
public class ConversationContext {

    private final UUID playerId;
    private final StructureType structureType;
    private ConversationState state;
    private Location origin;
    private float playerYaw;
    private String size;
    private final long startTime;

    public ConversationContext(UUID playerId, StructureType structureType) {
        this.playerId = playerId;
        this.structureType = structureType;
        this.state = ConversationState.IDLE;
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Check if this conversation has expired
     * @param timeoutMs Timeout in milliseconds
     * @return true if expired
     */
    public boolean isExpired(long timeoutMs) {
        return System.currentTimeMillis() - startTime > timeoutMs;
    }

    // Getters and setters

    public UUID getPlayerId() {
        return playerId;
    }

    public StructureType getStructureType() {
        return structureType;
    }

    public ConversationState getState() {
        return state;
    }

    public void setState(ConversationState state) {
        this.state = state;
    }

    public Location getOrigin() {
        return origin;
    }

    public void setOrigin(Location origin) {
        this.origin = origin;
    }

    public float getPlayerYaw() {
        return playerYaw;
    }

    public void setPlayerYaw(float playerYaw) {
        this.playerYaw = playerYaw;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public long getStartTime() {
        return startTime;
    }
}
