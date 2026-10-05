# EaglerXServer-Fabric

EaglercraftX 1.8 **dual-stack WebSocket/TCP server platform**, ported to run inside a **Fabric 26.2** Minecraft server.

This mod lets **browser players** (Eaglercraft, a free clone of Minecraft 1.8 that runs in the web browser) join the **same server** as normal Minecraft Java Edition players. No separate proxy (BungeeCord/Velocity) or dedicated EaglerXServer process is needed — everything runs inside your Fabric server.

## What it does

- Opens a WebSocket listener on your server's port alongside the vanilla TCP listener (dual-stack). The connection type is detected from the first packet: an HTTP/1.1 request is treated as an Eaglercraft connection, anything else is a normal Java Edition connection.
- Shaded bundle of the full EaglerX 1.1.1 server core plus the Fabric 26.2 platform adapter.
- WebRTC voice chat support (via `ice_servers.toml`).
- Skin cache, MOTD server list, query responses, rate-limiting, and the rest of the EaglerXServer feature set.

```
            ┌──────────────┐   wss:// or ws://   ┌──────────────────────────┐
 Web browser │ Eaglercraft  │ ──────────────────► │                        │
(no Java)    │ 1.8 client   │                    │    Fabric 26.2 server   │
            └──────────────┘                    │   + EaglerXServer-Fabric │
            ┌──────────────┐    Java TCP        │                        │
 Java launcher │ normal MC   │ ──────────────────► │      (port 25565)     │
               └──────────────┘                    └──────────────────────────┘
```

## Requirements

- **Minecraft:** 26.2 (Fabric)
- **Fabric Loader:** `>= 0.19.3` (comes with the server launcher)(probally stay on this loader version for most compat)
- **Fabric API:** `0.156.0+26.2` (must be in `mods/`)
- **Java:** 21+ (build with JDK 25)

## Included mods (`mods/`)

These jars are bundled in the `mods/` directory for convenience — copy them to your server's `mods/` folder:

| File | Purpose |
|---|---|
| `EaglerXServer-Fabric-1.1.1-fabric-26.2.jar` | This mod (pre-built) |
| `ViaFabric-0.4.21+183-26.x.jar` | Required by EaglerXServer — lets clients on other Eaglercraft/Java versions connect (protocol translation) |
| `ViaBackwards-5.12.1-SNAPSHOT.jar` | Backwards protocol translation so older clients can join |
| `ViaRewind-4.2.0.jar` | Translates pre-1.8 / 1.7 clients so they can join too |

> **Via should be present.** EaglerXServer depends on Via's protocol translation at the network layer (it intercepts both WebSocket and TCP traffic). Without it some clients will fail to join.

You still also need **Fabric API** (`fabric-api-0.156.0+26.2.jar`) and optionally a TURN/STUN config for working voice chat.

## Building from source

```bash
./gradlew build
```

Output: `build/libs/EaglerXServer-Fabric-1.1.1-fabric-26.2.jar`

The fabric platform code is the `src/` of this repo. The EaglerX 1.1.1 core jars it shades are in `libs/` (kept here so the build is self-contained).

## Installing

1. Install a Fabric 26.2 server (loader 0.19.3+).(probally stay on this loader version for most compat)
2. Copy into your server's `mods/` folder:
   - `EaglerXServer-Fabric-1.1.1-fabric-26.2.jar`
   - `fabric-api-0.156.0+26.2.jar`
   - the `Via` mods listed above
3. Accept the EULA and start the server once. It creates an `eaglerxserver/` folder next to the world with:
   - `listeners.toml` — listener settings (listener name, inject address, dual-stack, MOTD, rate limits, TLS/WSS config, ...)
   - `settings.toml` — server name/UUID, HTTP + websocket limits, voice chat, Eagler skin properties, ...
   - `ice_servers.toml` — STUN/TURN servers for WebRTC voice chat
4. Restart or leave running — the listener is bound on the server's port.

## Connecting

- **From the browser:** in the Eaglercraft client's server list add the address `ws://<host>:25565` (or `wss://<host>` when set up with a reverse proxy, see below).
- **From the Java launcher:** connect to `<host>:25565` exactly as normal.

Both player types appear and play together in the same world.

### WSS (secure WebSocket) via Caddy

Eaglercraft pages served over `https://` **require** `wss://` (browsers block plain `ws://` from secure pages). The simplest way is a reverse proxy that terminates TLS:

```caddy
ivanminecraft.example.com {
	reverse_proxy 127.0.0.1:25565
}
```

Caddy obtains a Let's Encrypt certificate automatically. Then use `wss://ivanminecraft.example.com` in the client. Point TCP ports `80` and `443` at the server in your router. Note the Fabric server must still accept plain websockets on `25565` (dual-stack default) — the proxy just adds TLS in front.

## Configuration highlights

The config files are documented inline (`eaglerxserver/listeners.toml` etc.). A few useful options:

- **`listener_name` / `inject_address`** — name of the listener and which address it injects into (`0.0.0.0:25565` typical for a dedicated server).
- **`dual_stack`** (default `true`) — serve both WebSockets and vanilla TCP on the same port.
- **`[listener_list.tls_config]`** — built-in HTTPS/WSS termination if you are NOT using a reverse proxy (`enable_tls`, `require_tls`, cert/key file paths).
- **`forward_ip` + `forward_ip_header`** — real-IP forwarding when running behind a reverse proxy / CloudFlare.
- **`[listener_list.ratelimit]`** — per-IP rate limiting on connections, login attempts, MOTD and other queries.

## Authentication / registration

`/register` and `/login` commands are **not** part of this mod. For password-based player registration use a separate auth plugin (e.g. a Fabric authentication mod). Handle the core's auth events (`enable_authentication_events = true` in `settings.toml`) to integrate your own system.

> **The auth mod needs editing before it works properly here.** The stock plugin hard-blocks unregistered players in a way that fights the Eagler websocket login flow. At minimum you must patch its `lang/en_us.json` so the kick/no-privilege messages reflect the real restrictions (the default text falsely claims the player "CANNOT move"), and tune the auth timeout. An example tested setup is the community `Authenticate` mod (`cn.enaium.authenticate`):
> - pin `fabric-orm-jimmer` to `1.0.5+jimmer.0.10.10` (the `.jimmer.0.11.2` line breaks its entity codegen at build/runtime on this stack);
> - install the matching `fabric-database-h2`, `fabric-language-kotlin`, and `Authenticate` jars together;
> - reword `authenticate.message.unregistered` / `authenticate.message.unauthenticate` so they don't over-claim (e.g. login restrictions, not "move");
> - set `authExpire` in its `Authenticate.json` (e.g. `60000` ms idle auto-logout, tune to taste).
> Expect a full server restart after editing, and confirm `/register`/`/login`/`/logout`/`/ban` actually fire through the websocket connection.

## Troubleshooting

- **502 / connection refused from the reverse proxy** — the Fabric server isn't running or isn't listening on the port Caddy proxies to.
- **Browsers "Timed out" on a `wss://` address** — TLS isn't terminated before port 25565 (the server sees raw TLS bytes, not a websocket). Put Caddy/nginx in front.
- **Clients on an older Minecraft/Eaglercraft version can't join** — make sure the Via mods are installed.
- **Voice chat doesn't work** — configure a working TURN server in `ice_servers.toml` (the Google STUN servers only help NAT discovery; on restricted networks TURN is required).

## Credits / licensing

- EaglercraftX and the EaglerXServer core are by [lax1dude](https://lax1dude.net/eaglerxserver/), copyright (c) 2024-2025 lax1dude, ayunami2000. See `LICENSE` (same terms as the upstream EaglerXServer).
- The **Fabric 26.2 port** (this platform adapter) is by [IvanIsOK](https://github.com/IvanIsOK).
- This repo is a **platform port** (Fabric adapter) of the EaglerX server core, not the Eaglercraft client itself. The browser client is a separate project.
