package com.builderbot.bot;

import org.bukkit.Location;

import java.util.UUID;

/**
 * Data container for a player's bot
 */
public class BotData {

    private final UUID playerId;
    private boolean isFollowing;
    private Location lastKnownPosition;

    public BotData(UUID playerId) {
        this.playerId = playerId;
        this.isFollowing = true; // Follow by default when summoned
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public boolean isFollowing() {
        return isFollowing;
    }

    public void setFollowing(boolean following) {
        isFollowing = following;
    }

    public Location getLastKnownPosition() {
        return lastKnownPosition;
    }

    public void setLastKnownPosition(Location lastKnownPosition) {
        this.lastKnownPosition = lastKnownPosition;
    }
}
