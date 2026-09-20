/*
 * EaglerXServer - Fabric platform bridge for Minecraft 26.2
 *
 * Licensed under the MIT License.
 */

package net.lax1dude.eaglercraft.backend.server.fabric;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapMaker;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.lax1dude.eaglercraft.backend.server.adapter.AbortLoadException;
import net.lax1dude.eaglercraft.backend.server.adapter.EnumAdapterPlatformType;
import net.lax1dude.eaglercraft.backend.server.adapter.IEaglerXServerCommandType;
import net.lax1dude.eaglercraft.backend.server.adapter.IEaglerXServerJoinListener;
import net.lax1dude.eaglercraft.backend.server.adapter.IEaglerXServerListener;
import net.lax1dude.eaglercraft.backend.server.adapter.IEaglerXServerLoginInitializer;
import net.lax1dude.eaglercraft.backend.server.adapter.IEaglerXServerMessageChannel;
import net.lax1dude.eaglercraft.backend.server.adapter.IEaglerXServerMessageHandler;
import net.lax1dude.eaglercraft.backend.server.adapter.IEaglerXServerNettyPipelineInitializer;
import net.lax1dude.eaglercraft.backend.server.adapter.IEaglerXServerPlayerCountHandler;
import net.lax1dude.eaglercraft.backend.server.adapter.IEaglerXServerPlayerInitializer;
import net.lax1dude.eaglercraft.backend.server.adapter.IPipelineComponent;
import net.lax1dude.eaglercraft.backend.server.adapter.IPipelineData;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatform;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformCommandSender;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformComponentHelper;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformLogger;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformNettyPipelineInitializer;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformPlayer;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformPlayerInitializer;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformScheduler;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformServer;
import net.lax1dude.eaglercraft.backend.server.adapter.PipelineAttributes;
import net.lax1dude.eaglercraft.backend.server.adapter.SLF4JLogger;
import net.lax1dude.eaglercraft.backend.server.adapter.IPipelineComponent.EnumPipelineComponent;
import net.lax1dude.eaglercraft.backend.server.adapter.event.IEventDispatchAdapter;
import net.lax1dude.eaglercraft.backend.server.api.EnumPipelineEvent;
import net.lax1dude.eaglercraft.backend.server.base.EaglerXServer;
import net.lax1dude.eaglercraft.backend.server.base.EaglerXServerVersion;
import net.lax1dude.eaglercraft.backend.server.base.NettyPipelineData;
import net.lax1dude.eaglercraft.backend.server.config.EnumConfigFormat;
import net.lax1dude.eaglercraft.backend.server.fabric.event.FabricEventDispatchAdapter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket;
import net.minecraft.network.protocol.login.ServerLoginPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.SharedConstants;
import net.minecraft.network.HandlerNames;

public class PlatformPluginFabric implements IPlatform<ServerPlayer>, ModInitializer {

	public static final String PLUGIN_NAME = EaglerXServerVersion.BRAND;
	public static final String PLUGIN_AUTHOR = EaglerXServerVersion.AUTHOR;
	public static final String PLUGIN_VERSION = EaglerXServerVersion.VERSION;

	private static final Logger LOGGER = LoggerFactory.getLogger("EaglerXServer");

	private static volatile boolean injectionEnabled = false;
	private static PlatformPluginFabric INSTANCE = null;

	private MinecraftServer server;
	private IPlatformLogger loggerImpl;
	private IEventDispatchAdapter<ServerPlayer, Component> eventDispatcherImpl;
	private IPlatformScheduler schedulerImpl;
	private IPlatformComponentHelper componentHelperImpl;
	private IPlatformCommandSender<ServerPlayer> cacheConsoleCommandSenderHandle;
	private File dataFolder;
	private EventLoopGroup eventLoopGroup;
	private boolean enableNativeTransport;

	protected boolean aborted = false;
	protected Runnable onServerEnable;
	protected Runnable onServerDisable;
	protected IEaglerXServerNettyPipelineInitializer<IPipelineData> pipelineInitializer;
	protected IEaglerXServerLoginInitializer<IPipelineData> loginInitializer;
	protected IEaglerXServerPlayerInitializer<IPipelineData, Object, ServerPlayer> playerInitializer;
	protected IEaglerXServerJoinListener<ServerPlayer> serverJoinListener;
	protected Collection<IEaglerXServerCommandType<ServerPlayer>> commandsList;
	protected IEaglerXServerListener listenerConf;

	private final ConcurrentMap<ServerPlayer, FabricPlayer> playerInstanceMap = (new MapMaker())
			.initialCapacity(512).concurrencyLevel(16).makeMap();

	@Override
	public void onInitialize() {
		INSTANCE = this;
		ServerLifecycleEvents.SERVER_STARTING.register(ms -> {
			server = ms;
			onServerStarting();
		});
		ServerLifecycleEvents.SERVER_STARTED.register(ms -> {
			if (!aborted && onServerEnable != null) {
				onServerEnable.run();
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(ms -> {
			if (!aborted && onServerDisable != null) {
				onServerDisable.run();
			}
			aborted = true;
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, ms) -> handlePlayerJoin(handler));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, ms) -> handlePlayerDisconnect(handler));
	}

	private void onServerStarting() {
		aborted = true;
		dataFolder = new File("eaglerxserver");
		if (!dataFolder.isDirectory()) {
			dataFolder.mkdirs();
		}
		loggerImpl = new SLF4JLogger(LOGGER);
		eventDispatcherImpl = new FabricEventDispatchAdapter();
		schedulerImpl = new FabricScheduler(this);
		componentHelperImpl = new FabricComponentHelper(
				Component.literal("Username is already connected to this server!"));
		cacheConsoleCommandSenderHandle = new FabricConsole(this, server);
		enableNativeTransport = Epoll.isAvailable() && !server.isSingleplayer();
		eventLoopGroup = createEpollEventLoopGroup();

		Init<ServerPlayer> init = new InitNonProxying<ServerPlayer>() {

			@Override
			public void setOnServerEnable(Runnable enable) {
				onServerEnable = enable;
			}

			@Override
			public void setOnServerDisable(Runnable disable) {
				onServerDisable = disable;
			}

			@Override
			public void setEaglerPlayerChannels(Collection<IEaglerXServerMessageChannel<ServerPlayer>> channels) {
			}

			@Override
			public void setPipelineInitializer(
					IEaglerXServerNettyPipelineInitializer<? extends IPipelineData> initializer) {
				pipelineInitializer = (IEaglerXServerNettyPipelineInitializer<IPipelineData>) initializer;
			}

			@Override
			public void setConnectionInitializer(IEaglerXServerLoginInitializer<? extends IPipelineData> initializer) {
				loginInitializer = (IEaglerXServerLoginInitializer<IPipelineData>) initializer;
			}

			@Override
			public void setPlayerInitializer(
					IEaglerXServerPlayerInitializer<? extends IPipelineData, ?, ServerPlayer> initializer) {
				playerInitializer = (IEaglerXServerPlayerInitializer<IPipelineData, Object, ServerPlayer>) initializer;
			}

			@Override
			public void setServerJoinListener(IEaglerXServerJoinListener<ServerPlayer> listener) {
				serverJoinListener = listener;
			}

			@Override
			public void setCommandRegistry(Collection<IEaglerXServerCommandType<ServerPlayer>> commands) {
				commandsList = commands;
			}

			@Override
			public IPlatform<ServerPlayer> getPlatform() {
				return PlatformPluginFabric.this;
			}

			@Override
			public void setEaglerListener(IEaglerXServerListener listener) {
				listenerConf = listener;
			}

			@Override
			public SocketAddress getListenerAddress() {
				String ip = server.getLocalIp() != null ? server.getLocalIp() : "0.0.0.0";
				int port = -1;
				if (server instanceof net.minecraft.server.dedicated.DedicatedServer ds) {
					port = ds.getServerPort();
				} else {
					port = server.getPort();
				}
				if (port <= 0) {
					port = 25565;
				}
				return new InetSocketAddress(ip, port);
			}

		};
		try {
			new EaglerXServer<ServerPlayer>().load(init);
		} catch (AbortLoadException ex) {
			loggerImpl.error("Server startup aborted: " + ex.getMessage());
			Throwable t = ex.getCause();
			if (t != null) {
				loggerImpl.error("Caused by: ", t);
			}
			throw new IllegalArgumentException("EaglerXServer startup aborted", ex);
		}
		registerCommands();
		injectionEnabled = true;
		aborted = false;
	}

	private EventLoopGroup createEpollEventLoopGroup() {
		if (enableNativeTransport) {
			try {
				return new EpollEventLoopGroup(0);
			} catch (Throwable t) {
				enableNativeTransport = false;
				LOGGER.warn("Native Epoll transport not available, falling back to NIO", t);
			}
		}
		return null;
	}

	private static class PipelineComponentImpl implements IPipelineComponent {

		private final EnumPipelineComponent type;
		private final String name;
		private final ChannelHandler handle;

		PipelineComponentImpl(EnumPipelineComponent type, String name, ChannelHandler handle) {
			this.type = type;
			this.name = name;
			this.handle = handle;
		}

		@Override
		public EnumPipelineComponent getIdentifiedType() {
			return type;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public ChannelHandler getHandle() {
			return handle;
		}

	}

	private static final ImmutableMap<String, EnumPipelineComponent> PIPELINE_COMPONENTS_MAP = ImmutableMap
			.<String, EnumPipelineComponent>builder().put(HandlerNames.SPLITTER, EnumPipelineComponent.FRAME_DECODER)
			.put(HandlerNames.PREPENDER, EnumPipelineComponent.FRAME_ENCODER)
			.put(HandlerNames.ENCODER, EnumPipelineComponent.MINECRAFT_ENCODER)
			.put(HandlerNames.DECODER, EnumPipelineComponent.MINECRAFT_DECODER)
			.put(HandlerNames.TIMEOUT, EnumPipelineComponent.READ_TIMEOUT_HANDLER)
			.put(HandlerNames.LEGACY_QUERY, EnumPipelineComponent.BUKKIT_LEGACY_HANDLER)
			.put(HandlerNames.PACKET_HANDLER, EnumPipelineComponent.INBOUND_PACKET_HANDLER).build();

	/** The only place in the pipeline where a login listener can be driven after the
	 *  vanilla login has begun; also see {@link net.minecraft.network.HandlerNames}. */
	private static final class LoginAckForwardingHandler extends ChannelInboundHandlerAdapter {

		@Override
		public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
			if (evt == EnumPipelineEvent.EAGLER_HANDSHAKE_COMPLETE
					|| evt == EnumPipelineEvent.EAGLER_ENTERED_PLAY_STATE) {
				IPipelineData pd = ctx.channel().attr(PipelineAttributes.<IPipelineData>pipelineData()).get();
				if (pd instanceof NettyPipelineData npd) {
					ChannelHandler packetHandler = ctx.pipeline().get(HandlerNames.PACKET_HANDLER);
					if (packetHandler instanceof Connection conn) {
						boolean handshakeComplete = evt == EnumPipelineEvent.EAGLER_HANDSHAKE_COMPLETE;
						if (handshakeComplete && npd.minecraftProtocol < 764) {
							tryForceStartConfiguration(npd, conn);
						}
						if (handshakeComplete && npd.minecraftProtocol < 764
								&& isLoginListener(conn)) {
							injectLoginAck(ctx, conn, npd);
						}
					}
				}
			}
			ctx.fireUserEventTriggered(evt);
		}

		private static void tryForceStartConfiguration(NettyPipelineData npd, Connection conn) {
			if (LOGIN_LISTENER_FIELD == null || START_CONFIGURATION_METHOD == null) {
				return;
			}
			try {
				Object listener = LOGIN_LISTENER_FIELD.get(conn);
				if (listener == null) {
					return;
				}
				if (!listener.getClass().getName().contains("ServerConfigurationPacketListenerImpl")) {
					return;
				}
				if (SCPLI_ADDON_FIELD != null && ADDON_REGISTER_STATE_FIELD != null
						&& REGISTER_STATE_SENT != null && REGISTER_STATE_NOT_RECEIVED != null) {
					Object addon = SCPLI_ADDON_FIELD.get(listener);
					if (addon != null) {
						Object rs = ADDON_REGISTER_STATE_FIELD.get(addon);
						if (rs == REGISTER_STATE_SENT) {
							ADDON_REGISTER_STATE_FIELD.set(addon, REGISTER_STATE_NOT_RECEIVED);
						}
					}
				}
				START_CONFIGURATION_METHOD.invoke(listener);
			} catch (Throwable t) {
				if (npd.connectionLogger != null) {
					npd.connectionLogger.error("forced startConfiguration() FAILED: " + t, t);
				}
			}
		}

		private static void injectLoginAck(ChannelHandlerContext ctx, Connection conn, NettyPipelineData npd) {
			ChannelHandlerContext connCtx = ctx.pipeline().context(conn);
			if (connCtx != null && LOGIN_ACK_CHANNEL_READ0 != null) {
				try {
					LOGIN_ACK_CHANNEL_READ0.invoke(conn, connCtx, ServerboundLoginAcknowledgedPacket.INSTANCE);
				} catch (Throwable t) {
					if (npd.connectionLogger != null) {
						npd.connectionLogger.error("Failed to deliver login ack: ", t);
					}
				}
			}
		}

		private static boolean isLoginListener(Connection conn) {
			if (LOGIN_LISTENER_FIELD == null) {
				return false;
			}
			try {
				return LOGIN_LISTENER_FIELD.get(conn) instanceof ServerLoginPacketListener;
			} catch (Throwable t) {
				return false;
			}
		}

	}

	private static final java.lang.reflect.Method LOGIN_ACK_CHANNEL_READ0;
	private static final java.lang.reflect.Field LOGIN_LISTENER_FIELD;
	private static final java.lang.reflect.Method START_CONFIGURATION_METHOD;
	private static final java.lang.reflect.Field SCPLI_ADDON_FIELD;
	private static final java.lang.reflect.Field ADDON_REGISTER_STATE_FIELD;
	private static final java.lang.Object REGISTER_STATE_SENT;
	private static final java.lang.Object REGISTER_STATE_NOT_RECEIVED;
	static {
		java.lang.reflect.Method m = null;
		try {
			m = Connection.class.getDeclaredMethod("channelRead0", ChannelHandlerContext.class,
					net.minecraft.network.protocol.Packet.class);
			m.setAccessible(true);
		} catch (Throwable t) {
			LOGGER.error("Failed to resolve Connection#channelRead0 for login ack injection", t);
		}
		LOGIN_ACK_CHANNEL_READ0 = m;

		java.lang.reflect.Field f = null;
		try {
			f = Connection.class.getDeclaredField("packetListener");
			f.setAccessible(true);
		} catch (Throwable t) {
			LOGGER.error("Failed to resolve Connection#packetListener for login ack injection", t);
		}
		LOGIN_LISTENER_FIELD = f;

		java.lang.reflect.Method sc = null;
		try {
			sc = net.minecraft.server.network.ServerConfigurationPacketListenerImpl.class.getMethod("startConfiguration");
		} catch (Throwable t) {
			LOGGER.error("Failed to resolve ServerConfigurationPacketListenerImpl#startConfiguration", t);
		}
		START_CONFIGURATION_METHOD = sc;

		java.lang.reflect.Field af = null;
		try {
			af = net.minecraft.server.network.ServerConfigurationPacketListenerImpl.class.getDeclaredField("addon");
			af.setAccessible(true);
		} catch (Throwable t) {
			LOGGER.error("Failed to resolve fabric addon field", t);
		}
		SCPLI_ADDON_FIELD = af;

		java.lang.reflect.Field rsf = null;
		Object rsSent = null;
		Object rsNotReceived = null;
		if (af != null) {
			try {
				Class<?> ac = af.getType();
				rsf = ac.getDeclaredField("registerState");
				rsf.setAccessible(true);
				Class<?> rs = rsf.getType();
				if (rs.isEnum()) {
					for (Object c : rs.getEnumConstants()) {
						if (c.toString().equals("SENT")) {
							rsSent = c;
						} else if (c.toString().equals("NOT_RECEIVED")) {
							rsNotReceived = c;
						}
					}
				}
			} catch (Throwable t) {
				LOGGER.error("Failed to resolve fabric addon registerState", t);
			}
		}
		ADDON_REGISTER_STATE_FIELD = rsf;
		REGISTER_STATE_SENT = rsSent;
		REGISTER_STATE_NOT_RECEIVED = rsNotReceived;
	}

	public static boolean isInjectionEnabled() {
		return injectionEnabled;
	}

	public static void injectChannel(Channel channel, ChannelPipeline inPipeline) {
		PlatformPluginFabric inst = INSTANCE;
		if (inst == null || inst.pipelineInitializer == null) {
			return;
		}
		if (channel.pipeline().get("eagler-multistack-initial") != null) {
			return;
		}
		try {
			final List<IPipelineComponent> pipelineList = new ArrayList<>();
			ChannelPipeline pipeline = channel.pipeline();
			for (String name : pipeline.names()) {
				ChannelHandler handler = pipeline.get(name);
				if (handler != null) {
					pipelineList.add(new PipelineComponentImpl(
							PIPELINE_COMPONENTS_MAP.getOrDefault(name, EnumPipelineComponent.UNIDENTIFIED), name,
							handler));
				}
			}
inst.pipelineInitializer.initialize(new IPlatformNettyPipelineInitializer<IPipelineData>() {

						@Override
						public void setAttachment(IPipelineData object) {
							channel.attr(PipelineAttributes.<IPipelineData>pipelineData()).set(object);
						}

						@Override
						public List<IPipelineComponent> getPipeline() {
							return pipelineList;
						}

						@Override
						public IEaglerXServerListener getListener() {
							return inst.listenerConf;
						}

						@Override
						public Consumer<SocketAddress> realAddressHandle() {
							return addr -> {
							};
						}

						@Override
						public Channel getChannel() {
							return channel;
						}

					});
					if (channel.pipeline().get("eagler-login-ack") == null) {
						channel.pipeline().addLast("eagler-login-ack", new LoginAckForwardingHandler());
					}
		} catch (Throwable t) {
			if (inst.loggerImpl != null) {
				inst.loggerImpl.error("Failed to inject Eagler pipeline for connection: ", t);
			}
			try {
				channel.close();
			} catch (Throwable t2) {
			}
		}
	}

	private void registerCommands() {
		if (commandsList == null) {
			return;
		}
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			for (IEaglerXServerCommandType<ServerPlayer> cmd : commandsList) {
				registerCommand(dispatcher, cmd, cmd.getCommandName());
				String[] aliases = cmd.getCommandAliases();
				if (aliases != null) {
					for (String alias : aliases) {
						registerCommand(dispatcher, cmd, alias);
					}
				}
			}
		});
	}

	private void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher,
			IEaglerXServerCommandType<ServerPlayer> cmd, String name) {
		LiteralArgumentBuilder<CommandSourceStack> root = LiteralArgumentBuilder.<CommandSourceStack>literal(name)
				.requires(src -> src.permissions().hasPermission(
						new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
				.executes(ctx -> runCommand(cmd, ctx.getSource(), new String[0]))
				.then(com.mojang.brigadier.builder.RequiredArgumentBuilder
						.<CommandSourceStack, String>argument("args", StringArgumentType.greedyString())
						.executes(ctx -> runCommand(cmd, ctx.getSource(),
								ctx.getArgument("args", String.class).split(" "))));
		dispatcher.register(root);
	}

	@SuppressWarnings("unchecked")
	private int runCommand(IEaglerXServerCommandType<ServerPlayer> cmd, CommandSourceStack source, String[] args) {
		IPlatformCommandSender<ServerPlayer> sender;
		if (source.getPlayer() != null) {
			IPlatformPlayer<ServerPlayer> player = getPlayer(source.getPlayer());
			sender = player != null ? player : new FabricConsole(this, server);
		} else {
			sender = cacheConsoleCommandSenderHandle;
		}
		cmd.getHandler().handle(cmd, sender, args);
		return 1;
	}

	private void handlePlayerJoin(ServerGamePacketListenerImpl handler) {
		if (aborted) {
			return;
		}
		ServerPlayer player = handler.getPlayer();
		if (player == null) {
			return;
		}
		try {
			Connection connection = handler.connection;
			if (connection == null) {
				return;
			}
			Channel channel = connection.channel;
			if (channel == null) {
				return;
			}
			IPipelineData pipelineData = channel.attr(PipelineAttributes.<IPipelineData>pipelineData()).get();
			initializePlayer(player, channel, pipelineData);
			confirmPlayer(player);
		} catch (Throwable t) {
			if (loggerImpl != null) {
				loggerImpl.error("Error initializing player " + player.getGameProfile().name() + ": ", t);
			}
		}
	}

	private void handlePlayerDisconnect(ServerGamePacketListenerImpl handler) {
		ServerPlayer player = handler.getPlayer();
		if (player == null) {
			return;
		}
		dropPlayer(player);
	}

	@SuppressWarnings("unchecked")
	public void initializePlayer(ServerPlayer player, Channel channel, IPipelineData pipelineData) {
		FabricLoginData loginData = new FabricLoginData(pipelineData);
		loginInitializer.initializeLogin(loginData);
		if (pipelineData instanceof net.lax1dude.eaglercraft.backend.server.base.NettyPipelineData nettyPipelineData
				&& nettyPipelineData.isEaglerPlayer()) {
			String texturesValue = loginData.getTexturesPropertyValue();
			if (texturesValue != null) {
				player.getGameProfile().properties().put("textures",
						new com.mojang.authlib.properties.Property("textures", texturesValue,
								loginData.getTexturesPropertySignature()));
			}
		}
		final FabricPlayer p = new FabricPlayer(this, player, channel, pipelineData);
		playerInitializer.initializePlayer(new IPlatformPlayerInitializer<IPipelineData, Object, ServerPlayer>() {

			@Override
			public void setPlayerAttachment(Object attachment) {
				p.attachment = attachment;
			}

			@Override
			public IPipelineData getPipelineAttachment() {
				return pipelineData;
			}

			@Override
			public IPlatformPlayer<ServerPlayer> getPlayer() {
				return p;
			}

			@Override
			public void complete() {
				playerInstanceMap.put(player, p);
				IEaglerXServerJoinListener<ServerPlayer> listener = serverJoinListener;
				if (listener != null) {
					listener.handlePreConnect(p);
				}
			}

			@Override
			public void cancel() {
			}

		});
	}

	public void confirmPlayer(ServerPlayer player) {
		FabricPlayer p = playerInstanceMap.get(player);
		if (p != null) {
			IEaglerXServerJoinListener<ServerPlayer> listener = serverJoinListener;
			if (listener != null) {
				listener.handlePostConnect(p, p.getServer());
			}
		}
	}

	public void dropPlayer(ServerPlayer player) {
		FabricPlayer p = playerInstanceMap.remove(player);
		if (p != null && playerInitializer != null) {
			playerInitializer.destroyPlayer(p);
		}
	}

	@Override
	public EnumAdapterPlatformType getType() {
		return EnumAdapterPlatformType.FABRIC;
	}

	@Override
	public String getVersion() {
		return SharedConstants.getCurrentVersion().name();
	}

	@Override
	public Class<ServerPlayer> getPlayerClass() {
		return ServerPlayer.class;
	}

	@Override
	public String getPluginId() {
		return "eaglerxserver";
	}

	@Override
	public File getDataFolder() {
		return dataFolder;
	}

	@Override
	public IPlatformLogger logger() {
		return loggerImpl;
	}

	@Override
	public IPlatformCommandSender<ServerPlayer> getConsole() {
		return cacheConsoleCommandSenderHandle;
	}

	@Override
	public void forEachPlayer(Consumer<IPlatformPlayer<ServerPlayer>> playerCallback) {
		playerInstanceMap.values().forEach(playerCallback);
	}

	@Override
	public Collection<IPlatformPlayer<ServerPlayer>> getAllPlayers() {
		return ImmutableList.copyOf(playerInstanceMap.values());
	}

	@Override
	public IPlatformPlayer<ServerPlayer> getPlayer(ServerPlayer playerObj) {
		return playerInstanceMap.get(playerObj);
	}

	@Override
	public IPlatformPlayer<ServerPlayer> getPlayer(String username) {
		ServerPlayer player = server.getPlayerList().getPlayerByName(username);
		if (player != null) {
			return playerInstanceMap.get(player);
		}
		return null;
	}

	@Override
	public IPlatformPlayer<ServerPlayer> getPlayer(UUID uuid) {
		ServerPlayer player = server.getPlayerList().getPlayer(uuid);
		if (player != null) {
			return playerInstanceMap.get(player);
		}
		return null;
	}

	@Override
	public Map<String, IPlatformServer<ServerPlayer>> getRegisteredServers() {
		return Collections.emptyMap();
	}

	@Override
	public IPlatformServer<ServerPlayer> getServer(String serverName) {
		return server != null ? new FabricServer(this, server) : null;
	}

	@Override
	public IEventDispatchAdapter<ServerPlayer, ?> eventDispatcher() {
		return eventDispatcherImpl;
	}

	@Override
	public IPlatformScheduler getScheduler() {
		return schedulerImpl;
	}

	@Override
	public Set<EnumConfigFormat> getConfigFormats() {
		return EnumConfigFormat.getSupported();
	}

	@Override
	public IPlatformComponentHelper getComponentHelper() {
		return componentHelperImpl;
	}

	@Override
	public boolean isOnlineMode() {
		return server != null && server.isDedicatedServer() && server.usesAuthentication();
	}

	@Override
	public boolean isModernPluginChannelNamesOnly() {
		return true;
	}

	@Override
	public int getPlayerTotal() {
		return server != null ? server.getPlayerList().getPlayerCount() : 0;
	}

	@Override
	public int getPlayerMax() {
		return server != null ? server.getPlayerList().getMaxPlayers() : 0;
	}

	@Override
	public void setPlayerCountHandler(IEaglerXServerPlayerCountHandler playerCountHandler) {
	}

	@Override
	public Bootstrap setChannelFactory(Bootstrap bootstrap, SocketAddress address) {
		if (address instanceof DomainSocketAddress) {
			throw new UnsupportedOperationException("Unix sockets not supported by this platform!");
		}
		if (enableNativeTransport) {
			return bootstrap.channel(EpollSocketChannel.class);
		}
		return bootstrap.channel(NioSocketChannel.class);
	}

	@Override
	public ServerBootstrap setServerChannelFactory(ServerBootstrap bootstrap, SocketAddress address) {
		if (address instanceof DomainSocketAddress) {
			throw new UnsupportedOperationException("Unix sockets not supported by this platform!");
		}
		if (enableNativeTransport) {
			return bootstrap.channel(EpollServerSocketChannel.class);
		}
		return bootstrap.channel(NioServerSocketChannel.class);
	}

	@Override
	public EventLoopGroup getBossEventLoopGroup() {
		return null;
	}

	@Override
	public EventLoopGroup getWorkerEventLoopGroup() {
		return eventLoopGroup;
	}

	MinecraftServer getMinecraftServer() {
		return server;
	}

	public static PlatformPluginFabric getInstance() {
		return INSTANCE;
	}

}