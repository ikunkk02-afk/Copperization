package com.shouyun.copperization;

public final class CopperizationConstants {
	public static final float[] ENCHANTMENT_PROGRESS = {0.0F, 0.15F, 0.22F, 0.30F};
	public static final float LIGHT_THRESHOLD = 0.25F;
	public static final float MEDIUM_THRESHOLD = 0.50F;
	public static final float HEAVY_THRESHOLD = 0.75F;
	public static final float STATUE_THRESHOLD = 1.0F;

	public static final double[] MOVEMENT_MULTIPLIERS = {0.0D, -0.10D, -0.30D, -0.60D};
	public static final double[] COMBAT_MULTIPLIERS = {0.0D, 0.0D, -0.15D, -0.40D};

	public static final int WAND_DURABILITY = 256;
	public static final int WAND_COOLDOWN_TICKS = 20;
	public static final long OXIDATION_SAMPLE_INTERVAL = 1200L;
	public static final float OXIDATION_CHANCE = 0.05688889F;
	public static final float FRESH_OXIDATION_MODIFIER = 0.75F;
	public static final float OXIDATION_STEP = 1.0F / 3.0F;

	private CopperizationConstants() {
	}

	public static float progressForLevel(int level) {
		return ENCHANTMENT_PROGRESS[Math.clamp(level, 0, 3)];
	}

	public static int debuffStage(float progress) {
		if (progress >= HEAVY_THRESHOLD) return 3;
		if (progress >= MEDIUM_THRESHOLD) return 2;
		if (progress >= LIGHT_THRESHOLD) return 1;
		return 0;
	}
}
