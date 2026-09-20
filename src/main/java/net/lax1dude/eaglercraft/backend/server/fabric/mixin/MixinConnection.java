/*
 * Fabric platform bridge for EaglercraftX - Minecraft 26.2
 *
 * Licensed under the MIT License.
 */

package net.lax1dude.eaglercraft.backend.server.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import net.lax1dude.eaglercraft.backend.server.fabric.PlatformPluginFabric;
import net.minecraft.network.BandwidthDebugMonitor;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;

@Mixin(Connection.class)
public abstract class MixinConnection {

	@Inject(method = "configureSerialization", at = @At("RETURN"), require = 1)
	private static void eaglerxserver$onConfigureSerialization(ChannelPipeline pipeline, PacketFlow side,
			boolean local, BandwidthDebugMonitor packetSizeLogger, CallbackInfo ci) {
		if (!PlatformPluginFabric.isInjectionEnabled()) {
			return;
		}
		Channel channel = pipeline.channel();
		if (!(channel instanceof SocketChannel)) {
			return;
		}
		PlatformPluginFabric.injectChannel(channel, pipeline);
	}

}