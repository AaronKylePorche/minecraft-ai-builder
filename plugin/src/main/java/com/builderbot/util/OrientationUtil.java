package com.builderbot.util;

import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * Utility for converting player orientation (yaw) into building placement coordinates.
 *
 * Coordinate System (Mirror Orientation):
 * - Local (0,0,0) = origin point (where player stands)
 * - +X = forward (away from player in facing direction - building depth)
 * - -X = backward (toward player - building's front/entrance)
 * - +Y = up
 * - +Z = right (player's right = building's LEFT side, mirror-like)
 * - -Z = left (player's left = building's RIGHT side, mirror-like)
 *
 * Buildings extend away from player but face back (like looking at house from driveway).
 */
public class OrientationUtil {

    /**
    * Get the forward vector (player's facing direction).
    * Buildings extend in this direction (away from player).
    *
    * Minecraft yaw reference (Bukkit):
    * - 0°   = South (+Z)
    * - 90°  = West  (-X)
    * - 180° = North (-Z)
    * - 270° = East  (+X)
    *
    * @param yaw Player's yaw angle in degrees
    * @return Normalized vector in player's facing direction
    */
    public static Vector getForwardVector(float yaw) {
        // Standard Bukkit yaw: 0° = +Z, increases clockwise
        double radians = Math.toRadians(yaw);

        // Player's facing direction (building extends away from player)
        return new Vector(
            -Math.sin(radians),
            0,
            Math.cos(radians)
        ).normalize();
    }

    /**
     * Get the right vector (perpendicular to forward, on player's right side).
     *
     * @param yaw Player's yaw angle in degrees
     * @return Normalized vector pointing to the player's right
     */
    public static Vector getRightVector(float yaw) {
        Vector forward = getForwardVector(yaw);
        // Right vector is forward rotated 90° clockwise
        return new Vector(-forward.getZ(), 0, forward.getX()).normalize();
    }

    /**
     * Get the up vector (always positive Y).
     *
     * @return Unit vector pointing up
     */
    public static Vector getUpVector() {
        return new Vector(0, 1, 0);
    }

    /**
     * Transform local building coordinates to world coordinates.
     *
     * @param origin The origin point (where player typed "here")
     * @param yaw Player's yaw angle in degrees
     * @param localX Forward distance (away from player)
     * @param localY Up distance
     * @param localZ Right distance (player's right)
     * @return World location for the block
     */
    public static Location toWorldCoordinates(Location origin, float yaw,
                                              double localX, double localY, double localZ) {
        Vector forward = getForwardVector(yaw);
        Vector right = getRightVector(yaw);
        Vector up = getUpVector();

        // Calculate offset from origin using local coordinates
        Vector offset = forward.clone().multiply(localX)
            .add(right.clone().multiply(localZ))
            .add(up.clone().multiply(localY));

        // Apply offset to origin and return new location
        return origin.clone().add(offset);
    }

    /**
     * Get cardinal direction name from yaw angle.
     * Useful for debugging and user feedback.
     *
     * @param yaw Player's yaw angle in degrees
     * @return Cardinal direction name (North, South, East, West, or intercardinal)
     */
    public static String getCardinalDirection(float yaw) {
        // Normalize to 0-360
        yaw = (yaw % 360 + 360) % 360;

        if (yaw >= 337.5 || yaw < 22.5) return "South";
        if (yaw >= 22.5 && yaw < 67.5) return "Southwest";
        if (yaw >= 67.5 && yaw < 112.5) return "West";
        if (yaw >= 112.5 && yaw < 157.5) return "Northwest";
        if (yaw >= 157.5 && yaw < 202.5) return "North";
        if (yaw >= 202.5 && yaw < 247.5) return "Northeast";
        if (yaw >= 247.5 && yaw < 292.5) return "East";
        return "Southeast"; // 292.5 to 337.5
    }

    /**
     * Get the direction the building will face (opposite of player facing).
     *
     * @param yaw Player's yaw angle in degrees
     * @return Direction the building's front will face
     */
    public static String getBuildingFacingDirection(float yaw) {
        // Building faces opposite direction from player
        float oppositeYaw = (yaw + 180) % 360;
        return getCardinalDirection(oppositeYaw);
    }
}
