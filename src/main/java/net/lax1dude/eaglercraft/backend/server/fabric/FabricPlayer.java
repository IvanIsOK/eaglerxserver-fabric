/*
 * EaglerXServer - Fabric platform bridge for Minecraft 26.2
 *
 * Licensed under the MIT License.
 */

package net.lax1dude.eaglercraft.backend.server.fabric;

import java.net.SocketAddress;
import java.util.UUID;

import io.netty.channel.Channel;
import net.lax1dude.eaglercraft.backend.server.adapter.IPipelineData;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformPlayer;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformServer;
import net.lax1dude.eaglercraft.backend.server.api.IEaglerListenerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class FabricPlayer implements IPlatformPlayer<ServerPlayer> {

	protected final PlatformPluginFabric plugin;
	protected final ServerPlayer player;
	protected final Channel channel;
	protected final IPipelineData pipelineData;
	protected Object attachment;

	FabricPlayer(PlatformPluginFabric plugin, ServerPlayer player, Channel channel, IPipelineData pipelineData) {
		this.plugin = plugin;
		this.player = player;
		this.channel = channel;
		this.pipelineData = pipelineData;
	}

	@Override
	public ServerPlayer getPlayerObject() {
		return player;
	}

	@Override
	public Channel getChannel() {
		return channel;
	}

	@Override
	public IPlatformServer<ServerPlayer> getServer() {
		return player.level() != null ? new FabricServer(plugin, player.level().getServer()) : null;
	}

	@Override
	public String getUsername() {
		return player.getGameProfile().name();
	}

	@Override
	public UUID getUniqueId() {
		return player.getGameProfile().id();
	}

	@Override
	public SocketAddress getSocketAddress() {
		if (pipelineData instanceof net.lax1dude.eaglercraft.backend.server.base.NettyPipelineData nettyData
				&& nettyData.isEaglerPlayer() && nettyData.realSocketAddressInstance != null) {
			return nettyData.realSocketAddressInstance;
		}
		if (channel != null && channel.remoteAddress() != null) {
			return channel.remoteAddress();
		}
		return player.connection != null ? player.connection.connection.getRemoteAddress() : null;
	}

	@Override
	public int getMinecraftProtocol() {
		if (pipelineData instanceof net.lax1dude.eaglercraft.backend.server.base.NettyPipelineData nettyData
				&& nettyData.isEaglerPlayer()) {
			return nettyData.minecraftProtocol;
		}
		return 47;
	}

	@Override
	public boolean isConnected() {
		return channel.isActive() && player.connection != null && player.connection.connection.isConnected();
	}

	@Override
	public boolean isOnlineMode() {
		return plugin.isOnlineMode();
	}

	@Override
	public String getMinecraftBrand() {
		if (pipelineData instanceof net.lax1dude.eaglercraft.backend.server.base.NettyPipelineData nettyData
				&& nettyData.isEaglerPlayer() && nettyData.eaglerBrandString != null) {
			return nettyData.eaglerBrandString;
		}
		return "vanilla";
	}

	@Override
	public String getTexturesProperty() {
		com.mojang.authlib.properties.Property prop = null;
		java.util.Collection<com.mojang.authlib.properties.Property> props = player.getGameProfile().properties()
				.get("textures");
		if (props != null) {
			for (com.mojang.authlib.properties.Property p : props) {
				if ("textures".equals(p.name())) {
					prop = p;
					break;
				}
			}
		}
		return prop != null ? prop.value() : null;
	}

	@Override
	public void sendDataClient(String channel, byte[] message) {
		plugin.logger().warn("sendDataClient is not implemented on the Fabric platform (channel '" + channel + "')");
	}

	@Override
	public void sendDataBackend(String channel, byte[] message) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isSetViewDistanceSupportedPaper() {
		return false;
	}

	@Override
	public void setViewDistancePaper(int distance) {
	}

	@Override
	public void sendMessage(String message) {
		player.sendSystemMessage(Component.literal(message));
	}

	@Override
	public <ComponentObject> void sendMessage(ComponentObject component) {
		player.sendSystemMessage((Component) component);
	}

	@Override
	public void disconnect() {
		disconnect((Component) null);
	}

	@Override
	public void disconnect(String kickMessage) {
		disconnect(kickMessage != null ? Component.literal(kickMessage) : null);
	}

	@Override
	public <ComponentObject> void disconnect(ComponentObject kickMessage) {
		if (player.connection != null) {
			if (kickMessage != null) {
				player.connection.disconnect((Component) kickMessage);
			} else {
				player.connection.disconnect(Component.literal("Connection Closed"));
			}
		} else if (channel != null) {
			channel.close();
		}
	}

	@Override
	public boolean checkPermission(String permission) {
		return PermissionUtil.hasPermission(player, permission);
	}

	@Override
	public boolean isPlayer() {
		return true;
	}

	@Override
	public IPlatformPlayer<ServerPlayer> asPlayer() {
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getPlayerAttachment() {
		return (T) attachment;
	}

	IEaglerListenerInfo getListenerInfo() {
		return null;
	}

}