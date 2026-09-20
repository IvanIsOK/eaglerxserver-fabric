/*
 * EaglerXServer - Fabric platform bridge for Minecraft 26.2
 *
 * Licensed under the MIT License.
 */

package net.lax1dude.eaglercraft.backend.server.fabric;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformScheduler;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformTask;
import net.lax1dude.eaglercraft.backend.server.api.IScheduler;
import net.lax1dude.eaglercraft.backend.server.api.ITask;

public class FabricScheduler implements IPlatformScheduler {

	private static final class FabricThreadFactory implements ThreadFactory {

		private final AtomicInteger id = new AtomicInteger();

		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "EaglerXServer-Worker-" + id.incrementAndGet());
			t.setDaemon(true);
			return t;
		}

	}

	private final PlatformPluginFabric plugin;
	private final ThreadFactory threadFactory = new FabricThreadFactory();
	private final ScheduledThreadPoolExecutor asyncExecutor = new ScheduledThreadPoolExecutor(4, threadFactory);

	FabricScheduler(PlatformPluginFabric plugin) {
		this.plugin = plugin;
	}

	private boolean runningOnMainThread() {
		net.minecraft.server.MinecraftServer server = plugin.getMinecraftServer();
		return server != null && server.isSameThread();
	}

	private void executeSync0(Runnable runnable) {
		net.minecraft.server.MinecraftServer server = plugin.getMinecraftServer();
		if (server == null || runningOnMainThread()) {
			runnable.run();
		} else {
			server.execute(runnable);
		}
	}

	private void executeAsync0(Runnable runnable) {
		asyncExecutor.execute(runnable);
	}

	@Override
	public void execute(Runnable runnable) {
		executeSync0(runnable);
	}

	@Override
	public void executeAsync(Runnable runnable) {
		executeAsync0(runnable);
	}

	@Override
	public void executeDelayed(Runnable runnable, long delay) {
		asyncExecutor.schedule(() -> executeSync0(runnable), delay, TimeUnit.MILLISECONDS);
	}

	@Override
	public void executeAsyncDelayed(Runnable runnable, long delay) {
		asyncExecutor.schedule(runnable, delay, TimeUnit.MILLISECONDS);
	}

	@Override
	public IPlatformTask executeDelayedTask(Runnable runnable, long delay) {
		return new FabricTask(() -> executeSync0(runnable), delay, TimeUnit.MILLISECONDS);
	}

	@Override
	public IPlatformTask executeAsyncDelayedTask(Runnable runnable, long delay) {
		return new FabricTask(runnable, delay, TimeUnit.MILLISECONDS);
	}

	@Override
	public IPlatformTask executeRepeatingTask(Runnable runnable, long delay, long interval) {
		Runnable wrapped = () -> {
			try {
				runnable.run();
			} catch (Throwable t) {
				plugin.logger().error("Uncaught exception in repeating scheduler task", t);
			}
		};
		ScheduledFuture<?> fut = asyncExecutor.scheduleAtFixedRate(wrapped, delay, interval, TimeUnit.MILLISECONDS);
		return new FabricTask(fut, wrapped);
	}

	@Override
	public IPlatformTask executeAsyncRepeatingTask(Runnable runnable, long delay, long interval) {
		ScheduledFuture<?> fut = asyncExecutor.scheduleAtFixedRate(runnable, delay, interval, TimeUnit.MILLISECONDS);
		return new FabricTask(fut, runnable);
	}

	private final class FabricTask implements IPlatformTask {

		private final ScheduledFuture<?> future;
		private final Runnable wakeup;

		FabricTask(ScheduledFuture<?> future, Runnable runnable) {
			this.future = future;
			this.wakeup = runnable;
		}

		FabricTask(Runnable runnable, long delay, TimeUnit unit) {
			this.future = asyncExecutor.schedule(() -> executeSync0(runnable), delay, unit);
			this.wakeup = runnable;
		}

		@Override
		public void cancel() {
			future.cancel(false);
		}

		@Override
		public Runnable getTask() {
			return wakeup;
		}

	}

}