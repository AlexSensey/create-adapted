package com.simibubi.create.foundation.gui;

import org.joml.Vector3f;

import com.mojang.math.Axis;

import net.createmod.catnip.api.client.gui.ILightingSettings;

public class CustomLightingSettings implements ILightingSettings {

	private Vector3f light1;
	private Vector3f light2;

	protected CustomLightingSettings(float yRot, float xRot) {
		init(yRot, xRot, 0, 0, false);
	}

	protected CustomLightingSettings(float yRot1, float xRot1, float yRot2, float xRot2) {
		init(yRot1, xRot1, yRot2, xRot2, true);
	}

	protected void init(float yRot1, float xRot1, float yRot2, float xRot2, boolean doubleLight) {
		light1 = new Vector3f(0, 0, 1);
		light1.rotate(Axis.YP.rotationDegrees(yRot1));
		light1.rotate(Axis.XN.rotationDegrees(xRot1));

		if (doubleLight) {
			light2 = new Vector3f(0, 0, 1);
			light2.rotate(Axis.YP.rotationDegrees(yRot2));
			light2.rotate(Axis.XN.rotationDegrees(xRot2));
		} else {
			light2 = new Vector3f();
		}
	}

	@Override
	public void apply() {
		// Minecraft 26.2 no longer exposes RenderSystem#setShaderLights, so the
		// original pair of custom light vectors cannot be uploaded through the
		// public rendering API. Do not leave callers with whichever lighting mode
		// happened to be active previously: JEI's animated 3D recipe previews need
		// the normal item-model lighting as their closest supported equivalent.
		ILightingSettings.ITEMS_3D.apply();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private float yRot1, xRot1;
		private float yRot2, xRot2;
		private boolean doubleLight;

		public Builder firstLightRotation(float yRot, float xRot) {
			yRot1 = yRot;
			xRot1 = xRot;
			return this;
		}

		public Builder secondLightRotation(float yRot, float xRot) {
			yRot2 = yRot;
			xRot2 = xRot;
			doubleLight = true;
			return this;
		}

		public Builder doubleLight() {
			doubleLight = true;
			return this;
		}

		public CustomLightingSettings build() {
			if (doubleLight) {
				return new CustomLightingSettings(yRot1, xRot1, yRot2, xRot2);
			} else {
				return new CustomLightingSettings(yRot1, xRot1);
			}
		}

	}

}
