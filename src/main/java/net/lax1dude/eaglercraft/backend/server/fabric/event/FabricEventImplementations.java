/*
 * EaglerXServer - Fabric platform bridge for Minecraft 26.2
 *
 * Default (allow-all) implementations of the EaglerXServer API events.
 * On Fabric there is no plugin event bus, so dispatch completes immediately
 * with these defaults; API consumers hook behaviour through the API itself.
 *
 * Licensed under the MIT License.
 */

package net.lax1dude.eaglercraft.backend.server.fabric.event;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.netty.handler.codec.http.FullHttpRequest;
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
import net.lax1dude.eaglercraft.backend.server.api.skins.EnumSkinModel;
import net.lax1dude.eaglercraft.backend.server.api.skins.IEaglerPlayerCape;
import net.lax1dude.eaglercraft.backend.server.api.skins.IEaglerPlayerSkin;
import net.lax1dude.eaglercraft.backend.server.api.voice.EnumVoiceState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class FabricEventImplementations {

	private FabricEventImplementations() {
	}

	static class AuthCheckRequiredEventImpl implements IEaglercraftAuthCheckRequiredEvent<ServerPlayer, Component> {

		private final IEaglerXServerAPI<ServerPlayer> api;
		private final IEaglerPendingConnection pendingConnection;
		private final boolean clientSolicitingPassword;
		private final byte[] authUsername;
		private boolean nicknameSelectionEnabled;
		private byte[] saltingData;
		private byte authType;
		private IEaglercraftAuthCheckRequiredEvent.EnumAuthResponse authRequired;
		private String authMessage = "enter the code:";
		private Component kickMessage;
		private boolean cookieAuth;

		AuthCheckRequiredEventImpl(IEaglerXServerAPI<ServerPlayer> api, IEaglerPendingConnection pendingConnection,
				boolean clientSolicitingPassword, byte[] authUsername) {
			this.api = api;
			this.pendingConnection = pendingConnection;
			this.clientSolicitingPassword = clientSolicitingPassword;
			this.authUsername = authUsername;
		}

		@Override
		public IEaglerXServerAPI<ServerPlayer> getServerAPI() {
			return api;
		}

		@Override
		public IEaglerPendingConnection getPendingConnection() {
			return pendingConnection;
		}

		@Override
		public boolean isClientSolicitingPassword() {
			return clientSolicitingPassword;
		}

		@Override
		public byte[] getAuthUsername() {
			return authUsername;
		}

		@Override
		public boolean isNicknameSelectionEnabled() {
			return nicknameSelectionEnabled;
		}

		@Override
		public void setNicknameSelectionEnabled(boolean enable) {
			nicknameSelectionEnabled = enable;
		}

		@Override
		public byte[] getSaltingData() {
			return saltingData;
		}

		@Override
		public void setSaltingData(byte[] saltingData) {
			this.saltingData = saltingData;
		}

		@Override
		public byte getUseAuthTypeRaw() {
			return authType;
		}

		@Override
		public void setUseAuthTypeRaw(byte authType) {
			this.authType = authType;
		}

		@Override
		public IEaglercraftAuthCheckRequiredEvent.EnumAuthResponse getAuthRequired() {
			return authRequired;
		}

		@Override
		public void setAuthRequired(IEaglercraftAuthCheckRequiredEvent.EnumAuthResponse authRequired) {
			this.authRequired = authRequired;
		}

		@Override
		public String getAuthMessage() {
			return authMessage;
		}

		@Override
		public void setAuthMessage(String authMessage) {
			if (authMessage == null) {
				throw new NullPointerException("authMessage");
			}
			this.authMessage = authMessage;
		}

		@Override
		public boolean getEnableCookieAuth() {
			return cookieAuth;
		}

		@Override
		public void setEnableCookieAuth(boolean enable) {
			this.cookieAuth = enable;
		}

		@Override
		public Component getKickMessage() {
			return kickMessage;
		}

		@Override
		public void setKickMessage(Component kickMessage) {
			this.kickMessage = kickMessage;
		}

		@Override
		public void setKickMessage(String kickMessage) {
			this.kickMessage = kickMessage != null ? Component.literal(kickMessage) : null;
		}

	}

	private abstract static class AuthBaseEventImpl {

		protected final IEaglerXServerAPI<ServerPlayer> api;
		protected final IEaglerLoginConnection loginConnection;
		protected final byte[] authUsername;
		protected final boolean nicknameSelectionEnabled;
		protected final boolean cookiesEnabled;
		protected final byte[] cookieData;
		protected final String requestedUsername;
		protected final byte authType;
		protected final String authMessage;
		protected String profileUsername;
		protected UUID profileUUID;
		protected String authRequestedServer;
		protected String authResponseName;
		protected Component kickMessage;
		protected String texturesPropertyValue;
		protected String texturesPropertySignature;
		protected boolean forceVanillaSkin;

		AuthBaseEventImpl(IEaglerXServerAPI<ServerPlayer> api, IEaglerLoginConnection loginConnection,
				byte[] authUsername, boolean nicknameSelectionEnabled, boolean cookiesEnabled, byte[] cookieData,
				String requestedUsername, String profileUsername, UUID profileUUID, byte authType, String authMessage,
				String authRequestedServer) {
			this.api = api;
			this.loginConnection = loginConnection;
			this.authUsername = authUsername;
			this.nicknameSelectionEnabled = nicknameSelectionEnabled;
			this.cookiesEnabled = cookiesEnabled;
			this.cookieData = cookieData;
			this.requestedUsername = requestedUsername;
			this.profileUsername = profileUsername;
			this.profileUUID = profileUUID;
			this.authType = authType;
			this.authMessage = authMessage;
			this.authRequestedServer = authRequestedServer;
		}

		public IEaglerXServerAPI<ServerPlayer> getServerAPI() {
			return api;
		}

		public IEaglerLoginConnection getLoginConnection() {
			return loginConnection;
		}

		public byte[] getAuthUsername() {
			return authUsername;
		}

		public boolean isNicknameSelectionEnabled() {
			return nicknameSelectionEnabled;
		}

		public boolean getCookiesEnabled() {
			return cookiesEnabled;
		}

		public byte[] getCookieData() {
			return cookieData;
		}

		public String getRequestedNickname() {
			return requestedUsername;
		}

		public String getProfileUsername() {
			return profileUsername;
		}

		public void setProfileUsername(String username) {
			if (username == null) {
				throw new NullPointerException("username");
			}
			profileUsername = username;
		}

		public UUID getProfileUUID() {
			return profileUUID;
		}

		public void setProfileUUID(UUID uuid) {
			throw new UnsupportedOperationException("Cannot change player UUID on the Fabric platform!");
		}

		public byte getAuthTypeRaw() {
			return authType;
		}

		public String getAuthMessage() {
			return authMessage;
		}

		public String getAuthRequestedServer() {
			return authRequestedServer;
		}

		public void setAuthRequestedServer(String server) {
			if (server == null) {
				throw new NullPointerException("server");
			}
			authRequestedServer = server;
		}

		public Component getKickMessage() {
			return kickMessage;
		}

		public void setKickMessage(Component kickMessage) {
			this.kickMessage = kickMessage;
		}

		public void setKickMessage(String kickMessage) {
			this.kickMessage = kickMessage != null ? Component.literal(kickMessage) : null;
		}

		public void applyTexturesProperty(String value, String signature) {
			texturesPropertyValue = value;
			texturesPropertySignature = signature;
		}

		public String getAppliedTexturesPropertyValue() {
			return texturesPropertyValue;
		}

		public String getAppliedTexturesPropertySignature() {
			return texturesPropertySignature;
		}

		public void setOverrideEaglerToVanillaSkins(boolean flag) {
			forceVanillaSkin = flag;
		}

		public boolean isOverrideEaglerToVanillaSkins() {
			return forceVanillaSkin;
		}

	}

	static class AuthCookieEventImpl extends AuthBaseEventImpl
			implements IEaglercraftAuthCookieEvent<ServerPlayer, Component> {

		AuthCookieEventImpl(IEaglerXServerAPI<ServerPlayer> api, IEaglerLoginConnection loginConnection,
				byte[] authUsername, boolean nicknameSelectionEnabled, boolean cookiesEnabled, byte[] cookieData,
				String requestedUsername, String profileUsername, UUID profileUUID, byte authType, String authMessage,
				String authRequestedServer) {
			super(api, loginConnection, authUsername, nicknameSelectionEnabled, cookiesEnabled, cookieData,
					requestedUsername, profileUsername, profileUUID, authType, authMessage, authRequestedServer);
		}

		@Override
		public IEaglercraftAuthCookieEvent.EnumAuthResponse getAuthResponse() {
			return authResponseName != null
					? IEaglercraftAuthCookieEvent.EnumAuthResponse.valueOf(authResponseName)
					: null;
		}

		@Override
		public void setAuthResponse(IEaglercraftAuthCookieEvent.EnumAuthResponse response) {
			authResponseName = response != null ? response.name() : null;
		}

	}

	static class AuthPasswordEventImpl extends AuthBaseEventImpl
			implements IEaglercraftAuthPasswordEvent<ServerPlayer, Component> {

		private final byte[] authPasswordData;
		private final byte[] authSaltingData;

		AuthPasswordEventImpl(IEaglerXServerAPI<ServerPlayer> api, IEaglerLoginConnection loginConnection,
				byte[] authUsername, boolean nicknameSelectionEnabled, byte[] authSaltingData, byte[] authPasswordData,
				boolean cookiesEnabled, byte[] cookieData, String requestedUsername, String profileUsername,
				UUID profileUUID, byte authType, String authMessage, String authRequestedServer) {
			super(api, loginConnection, authUsername, nicknameSelectionEnabled, cookiesEnabled, cookieData,
					requestedUsername, profileUsername, profileUUID, authType, authMessage, authRequestedServer);
			this.authSaltingData = authSaltingData;
			this.authPasswordData = authPasswordData;
		}

		@Override
		public byte[] getAuthSaltingData() {
			return authSaltingData;
		}

		@Override
		public byte[] getAuthPasswordDataResponse() {
			return authPasswordData;
		}

		@Override
		public IEaglercraftAuthPasswordEvent.EnumAuthResponse getAuthResponse() {
			return authResponseName != null ? IEaglercraftAuthPasswordEvent.EnumAuthResponse.valueOf(authResponseName)
					: null;
		}

		@Override
		public void setAuthResponse(IEaglercraftAuthPasswordEvent.EnumAuthResponse response) {
			authResponseName = response != null ? response.name() : null;
		}

	}

	static class ClientBrandEventImpl implements IEaglercraftClientBrandEvent<ServerPlayer, Component> {

		private final IEaglerXServerAPI<ServerPlayer> api;
		private boolean cancelled;
		private final IEaglerPendingConnection pendingConnection;
		private Component message;

		ClientBrandEventImpl(IEaglerXServerAPI<ServerPlayer> api, IEaglerPendingConnection pendingConnection) {
			this.api = api;
			this.pendingConnection = pendingConnection;
		}

		@Override
		public IEaglerXServerAPI<ServerPlayer> getServerAPI() {
			return api;
		}

		@Override
		public boolean isCancelled() {
			return cancelled;
		}

		@Override
		public void setCancelled(boolean cancelled) {
			this.cancelled = cancelled;
		}

		@Override
		public IEaglerPendingConnection getPendingConnection() {
			return pendingConnection;
		}

		@Override
		public Component getMessage() {
			return message;
		}

		@Override
		public void setMessage(String message) {
			this.message = message != null ? Component.literal(message) : null;
		}

		@Override
		public void setMessage(Component message) {
			this.message = message;
		}

	}

	static class LoginEventImpl implements IEaglercraftLoginEvent<ServerPlayer, Component> {

		private final IEaglerXServerAPI<ServerPlayer> api;
		private boolean cancelled;
		private final IEaglerLoginConnection loginConnection;
		private final boolean redirectSupport;
		private Component message;
		private String redirect;
		private String username;
		private String requestedServer;

		LoginEventImpl(IEaglerXServerAPI<ServerPlayer> api, IEaglerLoginConnection loginConnection,
				boolean redirectSupport, String requestedServer) {
			this.api = api;
			this.loginConnection = loginConnection;
			this.redirectSupport = redirectSupport;
			if (loginConnection != null && loginConnection.getUsername() != null) {
				this.username = loginConnection.getUsername();
			} else {
				this.username = "";
			}
			this.requestedServer = requestedServer;
		}

		@Override
		public IEaglerXServerAPI<ServerPlayer> getServerAPI() {
			return api;
		}

		@Override
		public boolean isCancelled() {
			return cancelled;
		}

		@Override
		public void setCancelled(boolean cancelled) {
			this.cancelled = cancelled;
		}

		@Override
		public IEaglerLoginConnection getLoginConnection() {
			return loginConnection;
		}

		@Override
		public Component getMessage() {
			return message;
		}

		@Override
		public void setMessage(String message) {
			this.message = message != null ? Component.literal(message) : null;
		}

		@Override
		public void setMessage(Component message) {
			this.message = message;
		}

		@Override
		public boolean isLoginStateRedirectSupported() {
			return redirectSupport;
		}

		@Override
		public String getRedirectAddress() {
			return redirect;
		}

		@Override
		public void setRedirectAddress(String addr) {
			this.redirect = addr;
		}

		@Override
		public String getProfileUsername() {
			return username;
		}

		@Override
		public void setProfileUsername(String username) {
			this.username = username;
		}

		@Override
		public UUID getProfileUUID() {
			return loginConnection != null ? loginConnection.getUniqueId() : null;
		}

		@Override
		public void setProfileUUID(UUID uuid) {
			throw new UnsupportedOperationException("Cannot change player UUID on the Fabric platform!");
		}

		@Override
		public String getRequestedServer() {
			return requestedServer;
		}

		@Override
		public void setRequestedServer(String server) {
			if (server == null) {
				throw new NullPointerException("server");
			}
			this.requestedServer = server;
		}

	}

	static class InitializePlayerEventImpl implements IEaglercraftInitializePlayerEvent<ServerPlayer> {

		private final IEaglerXServerAPI<ServerPlayer> api;
		private final IEaglerPlayer<ServerPlayer> player;
		private final Map<String, byte[]> extraProfileData;

		InitializePlayerEventImpl(IEaglerXServerAPI<ServerPlayer> api, IEaglerPlayer<ServerPlayer> player,
				Map<String, byte[]> extraProfileData) {
			this.api = api;
			this.player = player;
			this.extraProfileData = extraProfileData;
		}

		@Override
		public IEaglerXServerAPI<ServerPlayer> getServerAPI() {
			return api;
		}

		@Override
		public IEaglerPlayer<ServerPlayer> getPlayer() {
			return player;
		}

		@Override
		public Map<String, byte[]> getExtraProfileData() {
			return extraProfileData;
		}

	}

	static class DestroyPlayerEventImpl implements IEaglercraftDestroyPlayerEvent<ServerPlayer> {

		private final IEaglerXServerAPI<ServerPlayer> api;
		private final IEaglerPlayer<ServerPlayer> player;

		DestroyPlayerEventImpl(IEaglerXServerAPI<ServerPlayer> api, IEaglerPlayer<ServerPlayer> player) {
			this.api = api;
			this.player = player;
		}

		@Override
		public IEaglerXServerAPI<ServerPlayer> getServerAPI() {
			return api;
		}

		@Override
		public IEaglerPlayer<ServerPlayer> getPlayer() {
			return player;
		}

	}

	static class MOTDEventImpl implements IEaglercraftMOTDEvent<ServerPlayer> {

		private final IEaglerXServerAPI<ServerPlayer> api;
		private final IMOTDConnection connection;

		MOTDEventImpl(IEaglerXServerAPI<ServerPlayer> api, IMOTDConnection connection) {
			this.api = api;
			this.connection = connection;
		}

		@Override
		public IEaglerXServerAPI<ServerPlayer> getServerAPI() {
			return api;
		}

		@Override
		public IMOTDConnection getMOTDConnection() {
			return connection;
		}

	}

	static class RegisterSkinEventImpl implements IEaglercraftRegisterSkinEvent<ServerPlayer> {

		private final IEaglerXServerAPI<ServerPlayer> api;
		private final IEaglerLoginConnection loginConnection;
		private final IRegisterSkinDelegate delegate;

		RegisterSkinEventImpl(IEaglerXServerAPI<ServerPlayer> api, IEaglerLoginConnection loginConnection,
				IRegisterSkinDelegate delegate) {
			this.api = api;
			this.loginConnection = loginConnection;
			this.delegate = delegate;
		}

		@Override
		public IEaglerXServerAPI<ServerPlayer> getServerAPI() {
			return api;
		}

		@Override
		public IEaglerLoginConnection getLoginConnection() {
			return loginConnection;
		}

		@Override
		public IEaglerPlayerSkin getEaglerSkin() {
			return delegate.getEaglerSkin();
		}

		@Override
		public IEaglerPlayerCape getEaglerCape() {
			return delegate.getEaglerCape();
		}

		@Override
		public void forceFromVanillaTexturesProperty(String value) {
			if (value == null) {
				throw new NullPointerException("value");
			}
			delegate.forceFromVanillaTexturesProperty(value);
		}

		@Override
		public void forceFromVanillaLoginProfile() {
			delegate.forceFromVanillaLoginProfile();
		}

		@Override
		public void forceSkinFromURL(String url, EnumSkinModel skinModel) {
			if (url == null) {
				throw new NullPointerException("url");
			}
			if (skinModel == null) {
				throw new NullPointerException("skinModel");
			}
			delegate.forceSkinFromURL(url, skinModel);
		}

		@Override
		public void forceSkinFromVanillaTexturesProperty(String value) {
			delegate.forceSkinFromVanillaTexturesProperty(value);
		}

		@Override
		public void forceSkinFromVanillaLoginProfile() {
			delegate.forceSkinFromVanillaLoginProfile();
		}

		@Override
		public void forceCapeFromURL(String url) {
			if (url == null) {
				throw new NullPointerException("url");
			}
			delegate.forceCapeFromURL(url);
		}

		@Override
		public void forceCapeFromVanillaTexturesProperty(String value) {
			if (value == null) {
				throw new NullPointerException("value");
			}
			delegate.forceCapeFromVanillaTexturesProperty(value);
		}

		@Override
		public void forceCapeFromVanillaLoginProfile() {
			delegate.forceCapeFromVanillaLoginProfile();
		}

		@Override
		public void forceSkinEagler(IEaglerPlayerSkin skin) {
			if (skin == null) {
				throw new NullPointerException("skin");
			}
			delegate.forceSkinEagler(skin);
		}

		@Override
		public void forceCapeEagler(IEaglerPlayerCape cape) {
			if (cape == null) {
				throw new NullPointerException("cape");
			}
			delegate.forceCapeEagler(cape);
		}

	}

	static class RevokeSessionQueryEventImpl implements IEaglercraftRevokeSessionQueryEvent<ServerPlayer> {

		private final IEaglerXServerAPI<ServerPlayer> api;
		private final IQueryConnection query;
		private final byte[] cookieData;
		private EnumSessionRevokeStatus result;
		private boolean shouldDelete;

		RevokeSessionQueryEventImpl(IEaglerXServerAPI<ServerPlayer> api, IQueryConnection query, byte[] cookieData) {
			this.api = api;
			this.query = query;
			this.cookieData = cookieData;
			this.result = EnumSessionRevokeStatus.FAILED_NOT_SUPPORTED;
			this.shouldDelete = false;
		}

		@Override
		public IEaglerXServerAPI<ServerPlayer> getServerAPI() {
			return api;
		}

		@Override
		public IQueryConnection getSocket() {
			return query;
		}

		@Override
		public byte[] getCookieData() {
			return cookieData;
		}

		@Override
		public EnumSessionRevokeStatus getResultStatus() {
			return result;
		}

		@Override
		public void setResultStatus(EnumSessionRevokeStatus result) {
			if (result == null) {
				throw new NullPointerException("result");
			}
			this.result = result;
		}

		@Override
		public boolean getShouldDeleteCookie() {
			return shouldDelete;
		}

		@Override
		public void setShouldDeleteCookie(boolean flag) {
			shouldDelete = flag;
		}

	}

	static class VoiceChangeEventImpl implements IEaglercraftVoiceChangeEvent<ServerPlayer> {

		private final IEaglerXServerAPI<ServerPlayer> api;
		private final IEaglerPlayer<ServerPlayer> player;
		private final EnumVoiceState voiceStateOld;
		private final EnumVoiceState voiceStateNew;

		VoiceChangeEventImpl(IEaglerXServerAPI<ServerPlayer> api, IEaglerPlayer<ServerPlayer> player,
				EnumVoiceState voiceStateOld, EnumVoiceState voiceStateNew) {
			this.api = api;
			this.player = player;
			this.voiceStateOld = voiceStateOld;
			this.voiceStateNew = voiceStateNew;
		}

		@Override
		public IEaglerXServerAPI<ServerPlayer> getServerAPI() {
			return api;
		}

		@Override
		public IEaglerPlayer<ServerPlayer> getPlayer() {
			return player;
		}

		@Override
		public EnumVoiceState getVoiceStateOld() {
			return voiceStateOld;
		}

		@Override
		public EnumVoiceState getVoiceStateNew() {
			return voiceStateNew;
		}

	}

	static class WebSocketOpenEventImpl
			implements IEaglercraftWebSocketOpenEvent<ServerPlayer>, IEaglercraftWebSocketOpenEvent.NettyUnsafe {

		private final IEaglerXServerAPI<ServerPlayer> api;
		private boolean cancelled;
		private final IEaglerConnection connection;
		private final FullHttpRequest request;

		WebSocketOpenEventImpl(IEaglerXServerAPI<ServerPlayer> api, IEaglerConnection connection,
				FullHttpRequest request) {
			this.api = api;
			this.connection = connection;
			this.request = request;
		}

		@Override
		public IEaglerXServerAPI<ServerPlayer> getServerAPI() {
			return api;
		}

		@Override
		public boolean isCancelled() {
			return cancelled;
		}

		@Override
		public void setCancelled(boolean cancelled) {
			this.cancelled = cancelled;
		}

		@Override
		public IEaglerConnection getConnection() {
			return connection;
		}

		@Override
		public String getRawHeader(String name) {
			if (name == null) {
				throw new NullPointerException("name");
			}
			return request.headers().get(name);
		}

		@Override
		public List<String> getRawHeaders(String name) {
			if (name == null) {
				throw new NullPointerException("name");
			}
			return request.headers().getAll(name);
		}

		@Override
		public IEaglercraftWebSocketOpenEvent.NettyUnsafe netty() {
			return this;
		}

		@Override
		public FullHttpRequest getHttpRequest() {
			return request;
		}

	}

	static class WebViewChannelEventImpl implements IEaglercraftWebViewChannelEvent<ServerPlayer> {

		private final IEaglerXServerAPI<ServerPlayer> api;
		private final IEaglerPlayer<ServerPlayer> player;
		private final EnumEventType type;
		private final String channel;

		WebViewChannelEventImpl(IEaglerXServerAPI<ServerPlayer> api, IEaglerPlayer<ServerPlayer> player,
				EnumEventType type, String channel) {
			this.api = api;
			this.player = player;
			this.type = type;
			this.channel = channel;
		}

		@Override
		public IEaglerXServerAPI<ServerPlayer> getServerAPI() {
			return api;
		}

		@Override
		public IEaglerPlayer<ServerPlayer> getPlayer() {
			return player;
		}

		@Override
		public EnumEventType getType() {
			return type;
		}

		@Override
		public String getChannel() {
			return channel;
		}

	}

	static class WebViewMessageEventImpl implements IEaglercraftWebViewMessageEvent<ServerPlayer> {

		private final IEaglerXServerAPI<ServerPlayer> api;
		private final IEaglerPlayer<ServerPlayer> player;
		private final String channel;
		private final EnumMessageType type;
		private final byte[] data;
		private String asString;

		WebViewMessageEventImpl(IEaglerXServerAPI<ServerPlayer> api, IEaglerPlayer<ServerPlayer> player, String channel,
				EnumMessageType type, byte[] data) {
			this.api = api;
			this.player = player;
			this.channel = channel;
			this.type = type;
			this.data = data;
		}

		@Override
		public IEaglerXServerAPI<ServerPlayer> getServerAPI() {
			return api;
		}

		@Override
		public IEaglerPlayer<ServerPlayer> getPlayer() {
			return player;
		}

		@Override
		public String getChannel() {
			return channel;
		}

		@Override
		public EnumMessageType getType() {
			return type;
		}

		@Override
		public String getAsString() {
			if (asString == null) {
				asString = new String(data, StandardCharsets.UTF_8);
			}
			return asString;
		}

		@Override
		public byte[] getAsBinary() {
			return data;
		}

	}

}