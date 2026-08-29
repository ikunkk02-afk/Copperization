package com.shouyun.copperization.client.render;

import com.shouyun.copperization.block.CopperizedBlockData;

/** Palette-only visual policy. It operates on the active resource-pack model and texture. */
public final class CopperizedBlockVisualProfile {
	private static final int[] STAGE_TINTS = {
		0xFFE68A4B, // fresh copper
		0xFFC3845A, // exposed copper
		0xFF7E9D79, // weathered copper
		0xFF59A98F  // oxidized copper
	};

	private CopperizedBlockVisualProfile() {
	}

	public static int tint(CopperizedBlockData data) {
		return STAGE_TINTS[data.oxidationStage()];
	}
}
