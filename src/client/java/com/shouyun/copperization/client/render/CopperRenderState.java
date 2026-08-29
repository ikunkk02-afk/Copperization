package com.shouyun.copperization.client.render;

import com.shouyun.copperization.copper.FrozenPoseSnapshot;
import java.util.Optional;
import net.minecraft.world.level.block.WeatheringCopper;

public record CopperRenderState(
	float progress,
	float previousProgress,
	WeatheringCopper.WeatherState oxidation,
	boolean statue,
	boolean waxed,
	Optional<FrozenPoseSnapshot> frozenPose
) {
}
