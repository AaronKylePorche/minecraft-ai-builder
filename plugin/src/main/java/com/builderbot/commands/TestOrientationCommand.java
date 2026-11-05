package com.builderbot.commands;

import com.builderbot.util.OrientationUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Test command to verify orientation system works correctly.
 * Places colored concrete blocks to show the coordinate system.
 */
public class TestOrientationCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players");
            return true;
        }

        Player player = (Player) sender;
        Location origin = player.getLocation();
        float yaw = origin.getYaw();

        player.sendMessage("§e§l=== Orientation Test ===");
        player.sendMessage("§7Your yaw: §f" + String.format("%.1f°", yaw));
        player.sendMessage("§7Facing: §f" + OrientationUtil.getCardinalDirection(yaw));
        player.sendMessage("§7Building will face: §f" + OrientationUtil.getBuildingFacingDirection(yaw));
        player.sendMessage("");
        player.sendMessage("§aPlacing test blocks:");
        player.sendMessage("§c■ RED §7= Building back (far from you)");
        player.sendMessage("§9■ BLUE §7= Building front (close to you)");
        player.sendMessage("§e■ YELLOW §7= Right (from your perspective facing building)");
        player.sendMessage("§f■ WHITE §7= Left (from your perspective facing building)");
        player.sendMessage("§2■ GREEN §7= Up");

        // Place test blocks using our coordinate system (all blocks in front of player)

        // Building front (close) - BLUE at +3 blocks forward
        Location close = OrientationUtil.toWorldCoordinates(origin, yaw, 3, 0, 0);
        close.getBlock().setType(Material.BLUE_CONCRETE);

        // Building back (far) - RED at +10 blocks forward
        Location far = OrientationUtil.toWorldCoordinates(origin, yaw, 10, 0, 0);
        far.getBlock().setType(Material.RED_CONCRETE);

        // Right side - YELLOW at +6 forward, +3 right
        Location right = OrientationUtil.toWorldCoordinates(origin, yaw, 6, 0, 3);
        right.getBlock().setType(Material.YELLOW_CONCRETE);

        // Left side - WHITE at +6 forward, -3 left
        Location left = OrientationUtil.toWorldCoordinates(origin, yaw, 6, 0, -3);
        left.getBlock().setType(Material.WHITE_CONCRETE);

        // Above - GREEN at +6 forward, +3 up
        Location up = OrientationUtil.toWorldCoordinates(origin, yaw, 6, 3, 0);
        up.getBlock().setType(Material.GREEN_CONCRETE);

        // Origin marker - GOLD at +6 forward (center of test structure)
        Location center = OrientationUtil.toWorldCoordinates(origin, yaw, 6, 0, 0);
        center.getBlock().setType(Material.GOLD_BLOCK);

        player.sendMessage("");
        player.sendMessage("§aTest blocks placed!");
        player.sendMessage("§7BLUE block should be closest to you (building front)");
        player.sendMessage("§7RED block should be furthest from you (building back)");
        player.sendMessage("§7Both RED and BLUE should appear in FRONT of you");

        return true;
    }
}
