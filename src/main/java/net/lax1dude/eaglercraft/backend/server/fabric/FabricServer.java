/*
 * EaglerXServer - Fabric platform bridge for Minecraft 26.2
 *
 * Licensed under the MIT License.
 */

package net.lax1dude.eaglercraft.backend.server.fabric;

import java.util.Collection;
import java.util.function.Consumer;

import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformPlayer;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class FabricServer implements IPlatformServer<ServerPlayer> {

	protected final PlatformPluginFabric plugin;
	protected final MinecraftServer server;

	public FabricServer(PlatformPluginFabric plugin, MinecraftServer server) {
		this.plugin = plugin;
		this.server = server;
	}

	@Override
	public boolean isEaglerRegistered() {
		return true;
	}

	@Override
	public String getServerConfName() {
		return "main";
	}

	@Override
	public Collection<IPlatformPlayer<ServerPlayer>> getAllPlayers() {
		return plugin.getAllPlayers();
	}

	@Override
	public void forEachPlayer(Consumer<IPlatformPlayer<ServerPlayer>> callback) {
		plugin.forEachPlayer(callback);
	}

	@Override
	public String toString() {
		return "FabricServer{name=main, players=" + plugin.getPlayerTotal() + "}";
	}

}