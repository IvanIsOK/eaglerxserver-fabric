/*
 * EaglerXServer - Fabric platform bridge for Minecraft 26.2
 *
 * Licensed under the MIT License.
 */

package net.lax1dude.eaglercraft.backend.server.fabric;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

final class PermissionUtil {

	private PermissionUtil() {
	}

	static boolean hasPermission(ServerPlayer player, String permission) {
		if (player == null) {
			return true;
		}
		return player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS))
				|| player.level() != null && player.level().getServer().getPlayerList()
						.isOp(new NameAndId(player.getGameProfile()));
	}

}