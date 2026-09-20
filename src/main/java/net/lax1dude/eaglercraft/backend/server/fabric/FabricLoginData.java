/*
 * EaglerXServer - Fabric platform bridge for Minecraft 26.2
 *
 * Licensed under the MIT License.
 */

package net.lax1dude.eaglercraft.backend.server.fabric;

import java.util.UUID;

import net.lax1dude.eaglercraft.backend.server.adapter.IPlatformLoginInitializer;
import net.lax1dude.eaglercraft.backend.server.adapter.IPipelineData;

class FabricLoginData implements IPlatformLoginInitializer<IPipelineData> {

	private final IPipelineData pipelineData;
	private String texturesPropertyValue;
	private String texturesPropertySignature;
	private byte eaglerPlayerProperty = 0;

	protected FabricLoginData(IPipelineData pipelineData) {
		this.pipelineData = pipelineData;
	}

	@Override
	public IPipelineData getPipelineAttachment() {
		return pipelineData;
	}

	@Override
	public void setUniqueId(UUID uuid) {
		// no-op: the login UUID is fixed by the vanilla login flow on Fabric
	}

	@Override
	public void setTexturesProperty(String propertyValue, String propertySignature) {
		if (propertyValue != null) {
			this.texturesPropertyValue = propertyValue;
			this.texturesPropertySignature = propertySignature;
		}
	}

	@Override
	public void setEaglerPlayerProperty(boolean enable) {
		this.eaglerPlayerProperty = (byte) (enable ? 2 : 1);
	}

	String getTexturesPropertyValue() {
		return texturesPropertyValue;
	}

	String getTexturesPropertySignature() {
		return texturesPropertySignature;
	}

	@SuppressWarnings("unused")
	byte getEaglerPlayerProperty() {
		return eaglerPlayerProperty;
	}

}