/*
 * EaglerXServer - Fabric platform bridge for Minecraft 26.2
 *
 * Licensed under the MIT License.
 */

package net.lax1dude.eaglercraft.backend.server.fabric;

import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformCommandSender;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class FabricConsole implements IPlatformCommandSender<ServerPlayer> {

	protected final PlatformPluginFabric plugin;
	protected final MinecraftServer server;

	public FabricConsole(PlatformPluginFabric plugin, MinecraftServer server) {
		this.plugin = plugin;
		this.server = server;
	}

	public MinecraftServer getMinecraftServer() {
		return server;
	}

	@Override
	public boolean checkPermission(String permission) {
		return true;
	}

	@Override
	public <ComponentObject> void sendMessage(ComponentObject component) {
		server.sendSystemMessage((Component) component);
	}

	@Override
	public boolean isPlayer() {
		return false;
	}

	@Override
	public IPlatformPlayer<ServerPlayer> asPlayer() {
		return null;
	}

	CommandSourceStack createCommandSource() {
		return server.createCommandSourceStack();
	}

}