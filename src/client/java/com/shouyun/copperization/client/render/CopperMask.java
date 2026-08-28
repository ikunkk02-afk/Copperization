package com.shouyun.copperization.client.render;

import com.shouyun.copperization.Copperization;
import net.minecraft.resources.Identifier;

public final class CopperMask {
	private static final Identifier[] MASKS = {
		Copperization.id("textures/entity/copper_mask_25.png"),
		Copperization.id("textures/entity/copper_mask_50.png"),
		Copperization.id("textures/entity/copper_mask_75.png"),
		Copperization.id("textures/entity/copper_mask_100.png")
	};

	private CopperMask() {
	}

	public static Identifier forProgress(float progress) {
		if (progress >= 1.0F) return MASKS[3];
		if (progress >= 0.75F) return MASKS[3];
		if (progress >= 0.50F) return MASKS[2];
		if (progress >= 0.25F) return MASKS[1];
		return MASKS[0];
	}
}
