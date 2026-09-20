/*
 * EaglerXServer - Fabric platform bridge for Minecraft 26.2
 *
 * Licensed under the MIT License.
 */

package net.lax1dude.eaglercraft.backend.server.fabric;

import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformComponentBuilder;
import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformComponentHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

public class FabricComponentHelper implements IPlatformComponentHelper {

	protected final Component prefix;

	public FabricComponentHelper(Component prefix) {
		this.prefix = prefix;
	}

	@Override
	public IPlatformComponentBuilder builder() {
		return new FabricComponentBuilder();
	}

	@Override
	public Class<?> getComponentType() {
		return Component.class;
	}

	@Override
	public Object getStandardKickAlreadyPlaying() {
		return prefix;
	}

	@Override
	public String serializeLegacySection(Object component) {
		return ((Component) component).getString();
	}

	@Override
	public String serializePlainText(Object component) {
		return ((Component) component).getString();
	}

	@Override
	public String serializeGenericJSON(Object component) {
		return serializeModernJSON(component);
	}

	@Override
	public String serializeLegacyJSON(Object component) {
		return serializeModernJSON(component);
	}

	@Override
	public String serializeModernJSON(Object component) {
		return ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, (Component) component)
				.result().map(Object::toString).orElse(null);
	}

	@Override
	public Object parseGenericJSON(String json) throws IllegalArgumentException {
		return parseModernJSON(json);
	}

	@Override
	public Object parseLegacyJSON(String json) throws IllegalArgumentException {
		return parseModernJSON(json);
	}

	@Override
	public Object parseModernJSON(String json) throws IllegalArgumentException {
		return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
				.result().orElse(null);
	}

}