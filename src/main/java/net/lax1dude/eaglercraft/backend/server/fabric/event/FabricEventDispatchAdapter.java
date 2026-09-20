/*
 * EaglerXServer - Fabric platform bridge for Minecraft 26.2
 *
 * Licensed under the MIT License.
 */

package net.lax1dude.eaglercraft.backend.server.fabric.event;

import java.util.Map;
import java.util.UUID;

import io.netty.handler.codec.http.FullHttpRequest;
import net.lax1dude.eaglercraft.backend.server.adapter.event.IEventDispatchAdapter;
import net.lax1dude.eaglercraft.backend.server.adapter.event.IEventDispatchCallback;
import net.lax1dude.eaglercraft.backend.server.adapter.event.IRegisterSkinDelegate;
import net.lax1dude.eaglercraft.backend.server.api.IEaglerConnection;
import net.lax1dude.eaglercraft.backend.server.api.IEaglerLoginConnection;
import net.lax1dude.eaglercraft.backend.server.api.IEaglerPendingConnection;
import net.lax1dude.eaglercraft.backend.server.api.IEaglerPlayer;
import net.lax1dude.eaglercraft.backend.server.api.IEaglerXServerAPI;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftAuthCheckRequiredEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftAuthCookieEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftAuthPasswordEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftClientBrandEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftDestroyPlayerEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftInitializePlayerEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftLoginEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftMOTDEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftRegisterSkinEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftRevokeSessionQueryEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftVoiceChangeEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftWebSocketOpenEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftWebViewChannelEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftWebViewChannelEvent.EnumEventType;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftWebViewMessageEvent;
import net.lax1dude.eaglercraft.backend.server.api.event.IEaglercraftWebViewMessageEvent.EnumMessageType;
import net.lax1dude.eaglercraft.backend.server.api.query.IMOTDConnection;
import net.lax1dude.eaglercraft.backend.server.api.query.IQueryConnection;
import net.lax1dude.eaglercraft.backend.server.api.voice.EnumVoiceState;
import net.lax1dude.eaglercraft.backend.server.fabric.event.FabricEventImplementations.AuthCheckRequiredEventImpl;
import net.lax1dude.eaglercraft.backend.server.fabric.event.FabricEventImplementations.AuthCookieEventImpl;
import net.lax1dude.eaglercraft.backend.server.fabric.event.FabricEventImplementations.AuthPasswordEventImpl;
import net.lax1dude.eaglercraft.backend.server.fabric.event.FabricEventImplementations.ClientBrandEventImpl;
import net.lax1dude.eaglercraft.backend.server.fabric.event.FabricEventImplementations.DestroyPlayerEventImpl;
import net.lax1dude.eaglercraft.backend.server.fabric.event.FabricEventImplementations.InitializePlayerEventImpl;
import net.lax1dude.eaglercraft.backend.server.fabric.event.FabricEventImplementations.LoginEventImpl;
import net.lax1dude.eaglercraft.backend.server.fabric.event.FabricEventImplementations.MOTDEventImpl;
import net.lax1dude.eaglercraft.backend.server.fabric.event.FabricEventImplementations.RegisterSkinEventImpl;
import net.lax1dude.eaglercraft.backend.server.fabric.event.FabricEventImplementations.RevokeSessionQueryEventImpl;
import net.lax1dude.eaglercraft.backend.server.fabric.event.FabricEventImplementations.VoiceChangeEventImpl;
import net.lax1dude.eaglercraft.backend.server.fabric.event.FabricEventImplementations.WebSocketOpenEventImpl;
import net.lax1dude.eaglercraft.backend.server.fabric.event.FabricEventImplementations.WebViewChannelEventImpl;
import net.lax1dude.eaglercraft.backend.server.fabric.event.FabricEventImplementations.WebViewMessageEventImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class FabricEventDispatchAdapter implements IEventDispatchAdapter<ServerPlayer, Component> {

	private volatile IEaglerXServerAPI<ServerPlayer> api;

	private <I, T> void fire(T event, IEventDispatchCallback<I> cont) {
		if (cont != null) {
			cont.complete((I) event, null);
		}
	}

	@Override
	public void setAPI(IEaglerXServerAPI<ServerPlayer> api) {
		this.api = api;
	}

	@Override
	public void dispatchAuthCheckRequired(IEaglerPendingConnection pendingConnection, boolean clientSolicitingPassword,
			byte[] authUsername,
			IEventDispatchCallback<IEaglercraftAuthCheckRequiredEvent<ServerPlayer, Component>> onComplete) {
		fire(new AuthCheckRequiredEventImpl(api, pendingConnection, clientSolicitingPassword, authUsername),
				onComplete);
	}

	@Override
	public void dispatchAuthCookieEvent(IEaglerLoginConnection loginConnection, byte[] authUsername,
			boolean nicknameSelectionEnabled, boolean cookiesEnabled, byte[] cookieData, String requestedUsername,
			String profileUsername, UUID profileUUID, byte authType, String authMessage, String authRequestedServer,
			IEventDispatchCallback<IEaglercraftAuthCookieEvent<ServerPlayer, Component>> onComplete) {
		fire(new AuthCookieEventImpl(api, loginConnection, authUsername, nicknameSelectionEnabled, cookiesEnabled,
				cookieData, requestedUsername, profileUsername, profileUUID, authType, authMessage,
				authRequestedServer), onComplete);
	}

	@Override
	public void dispatchAuthPasswordEvent(IEaglerLoginConnection loginConnection, byte[] authUsername,
			boolean nicknameSelectionEnabled, byte[] authSaltingData, byte[] authPasswordData, boolean cookiesEnabled,
			byte[] cookieData, String requestedUsername, String profileUsername, UUID profileUUID, byte authType,
			String authMessage, String authRequestedServer,
			IEventDispatchCallback<IEaglercraftAuthPasswordEvent<ServerPlayer, Component>> onComplete) {
		fire(new AuthPasswordEventImpl(api, loginConnection, authUsername, nicknameSelectionEnabled, authSaltingData,
				authPasswordData, cookiesEnabled, cookieData, requestedUsername, profileUsername, profileUUID, authType,
				authMessage, authRequestedServer), onComplete);
	}

	@Override
	public void dispatchClientBrandEvent(IEaglerPendingConnection pendingConnection,
			IEventDispatchCallback<IEaglercraftClientBrandEvent<ServerPlayer, Component>> onComplete) {
		fire(new ClientBrandEventImpl(api, pendingConnection), onComplete);
	}

	@Override
	public void dispatchLoginEvent(IEaglerLoginConnection loginConnection, boolean redirectSupport,
			String requestedServer,
			IEventDispatchCallback<IEaglercraftLoginEvent<ServerPlayer, Component>> onComplete) {
		fire(new LoginEventImpl(api, loginConnection, redirectSupport, requestedServer), onComplete);
	}

	@Override
	public void dispatchInitializePlayerEvent(IEaglerPlayer<ServerPlayer> player, Map<String, byte[]> extraProfileData,
			IEventDispatchCallback<IEaglercraftInitializePlayerEvent<ServerPlayer>> onComplete) {
		fire(new InitializePlayerEventImpl(api, player, extraProfileData), onComplete);
	}

	@Override
	public void dispatchDestroyPlayerEvent(IEaglerPlayer<ServerPlayer> player,
			IEventDispatchCallback<IEaglercraftDestroyPlayerEvent<ServerPlayer>> onComplete) {
		fire(new DestroyPlayerEventImpl(api, player), onComplete);
	}

	@Override
	public void dispatchMOTDEvent(IMOTDConnection connection,
			IEventDispatchCallback<IEaglercraftMOTDEvent<ServerPlayer>> onComplete) {
		fire(new MOTDEventImpl(api, connection), onComplete);
	}

	@Override
	public void dispatchRegisterSkinEvent(IEaglerLoginConnection loginConnection, IRegisterSkinDelegate delegate,
			IEventDispatchCallback<IEaglercraftRegisterSkinEvent<ServerPlayer>> onComplete) {
		fire(new RegisterSkinEventImpl(api, loginConnection, delegate), onComplete);
	}

	@Override
	public void dispatchRevokeSessionQueryEvent(IQueryConnection query, byte[] cookieData,
			IEventDispatchCallback<IEaglercraftRevokeSessionQueryEvent<ServerPlayer>> onComplete) {
		fire(new RevokeSessionQueryEventImpl(api, query, cookieData), onComplete);
	}

	@Override
	public void dispatchVoiceChangeEvent(IEaglerPlayer<ServerPlayer> player, EnumVoiceState voiceStateOld,
			EnumVoiceState voiceStateNew,
			IEventDispatchCallback<IEaglercraftVoiceChangeEvent<ServerPlayer>> onComplete) {
		fire(new VoiceChangeEventImpl(api, player, voiceStateOld, voiceStateNew), onComplete);
	}

	@Override
	public void dispatchWebSocketOpenEvent(IEaglerConnection delegate, FullHttpRequest request,
			IEventDispatchCallback<IEaglercraftWebSocketOpenEvent<ServerPlayer>> onComplete) {
		fire(new WebSocketOpenEventImpl(api, delegate, request), onComplete);
	}

	@Override
	public void dispatchWebViewChannelEvent(IEaglerPlayer<ServerPlayer> player, EnumEventType type, String channel,
			IEventDispatchCallback<IEaglercraftWebViewChannelEvent<ServerPlayer>> onComplete) {
		fire(new WebViewChannelEventImpl(api, player, type, channel), onComplete);
	}

	@Override
	public void dispatchWebViewMessageEvent(IEaglerPlayer<ServerPlayer> player, String channel, EnumMessageType type,
			byte[] data, IEventDispatchCallback<IEaglercraftWebViewMessageEvent<ServerPlayer>> onComplete) {
		fire(new WebViewMessageEventImpl(api, player, channel, type, data), onComplete);
	}

}