package com.shouyun.copperization;

public final class CopperizationConstants {
	public static final int COPPERIZATION_DURATION_LEVEL_1 = 12 * 20;
	public static final int COPPERIZATION_DURATION_LEVEL_2 = 8 * 20;
	public static final int COPPERIZATION_DURATION_LEVEL_3 = 5 * 20;
	public static final int COPPERIZATION_SYNC_INTERVAL_TICKS = 4;
	public static final int COPPERIZATION_PARTICLE_INTERVAL_TICKS = 20;
	public static final float COPPERIZATION_COMPLETION_EPSILON = 1.0E-5F;
	public static final float LIGHT_THRESHOLD = 0.25F;
	public static final float MEDIUM_THRESHOLD = 0.50F;
	public static final float HEAVY_THRESHOLD = 0.75F;
	public static final float STATUE_THRESHOLD = 1.0F;

	public static final double MINIMUM_MOVEMENT_MULTIPLIER = 0.05D;
	public static final double MINIMUM_COMBAT_MULTIPLIER = 0.20D;

	public static final int WAND_DURABILITY = 256;
	public static final int WAND_COOLDOWN_TICKS = 20;
	public static final long OXIDATION_SAMPLE_INTERVAL = 1200L;
	public static final float OXIDATION_CHANCE = 0.05688889F;
	public static final float FRESH_OXIDATION_MODIFIER = 0.75F;
	public static final float OXIDATION_STEP = 1.0F / 3.0F;

	private CopperizationConstants() {
	}

	public static int durationTicksForLevel(int level) {
		return switch (Math.clamp(level, 1, 3)) {
			case 1 -> COPPERIZATION_DURATION_LEVEL_1;
			case 2 -> COPPERIZATION_DURATION_LEVEL_2;
			default -> COPPERIZATION_DURATION_LEVEL_3;
		};
	}

	public static float progressPerTick(int level) {
		return 1.0F / durationTicksForLevel(level);
	}

	public static double movementModifier(float progress) {
		return multiplierModifier(progress, MINIMUM_MOVEMENT_MULTIPLIER);
	}

	public static double combatModifier(float progress) {
		return multiplierModifier(Math.max(0.0F, (progress - LIGHT_THRESHOLD) / (1.0F - LIGHT_THRESHOLD)), MINIMUM_COMBAT_MULTIPLIER);
	}

	private static double multiplierModifier(float normalizedProgress, double minimumMultiplier) {
		double progress = Math.clamp(normalizedProgress, 0.0F, 1.0F);
		double eased = progress * progress * (3.0D - 2.0D * progress);
		return (1.0D + (minimumMultiplier - 1.0D) * eased) - 1.0D;
	}
}
