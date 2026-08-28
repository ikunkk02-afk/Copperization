package com.shouyun.copperization.copper;

import com.shouyun.copperization.CopperizationConstants;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.WeatheringCopper;

public final class CopperOxidationManager {
	private CopperOxidationManager() {
	}

	public static void tickStatue(ServerLevel level, LivingEntity statue) {
		CopperizationState state = CopperizationManager.getState(statue);
		state.frozenPose().ifPresent(snapshot -> snapshot.applyTransform(statue));
		if (state.waxed() || state.weatherState() == WeatheringCopper.WeatherState.OXIDIZED || level.getGameTime() < state.nextWeatheringTick()) {
			return;
		}

		long nextTick = level.getGameTime() + CopperizationConstants.OXIDATION_SAMPLE_INTERVAL;
		float modifier = state.weatherState() == WeatheringCopper.WeatherState.UNAFFECTED
			? CopperizationConstants.FRESH_OXIDATION_MODIFIER : 1.0F;
		float progress = state.oxidationProgress();
		if (level.getRandom().nextFloat() < CopperizationConstants.OXIDATION_CHANCE * modifier) {
			progress = Math.min(1.0F, progress + CopperizationConstants.OXIDATION_STEP);
		}
		CopperizationManager.setState(statue, state.withOxidation(progress, nextTick));
	}

	public static Optional<CopperizationState> scrape(CopperizationState state) {
		if (state.waxed()) return Optional.of(state.withWaxed(false));
		WeatheringCopper.WeatherState current = state.weatherState();
		if (current == WeatheringCopper.WeatherState.UNAFFECTED) return Optional.empty();
		float previous = current.previous().ordinal() / 3.0F;
		return Optional.of(state.withOxidation(previous, state.nextWeatheringTick()));
	}
}
