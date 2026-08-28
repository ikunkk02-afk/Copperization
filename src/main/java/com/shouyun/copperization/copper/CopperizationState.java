package com.shouyun.copperization.copper;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.WeatheringCopper;

public record CopperizationState(
	float copperizationProgress,
	float oxidationProgress,
	boolean copperStatue,
	boolean waxed,
	Optional<FrozenPoseSnapshot> frozenPose,
	long nextWeatheringTick
) {
	public static final CopperizationState EMPTY = new CopperizationState(0.0F, 0.0F, false, false, Optional.empty(), 0L);

	public static final Codec<CopperizationState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.FLOAT.optionalFieldOf("copperization_progress", 0.0F).forGetter(CopperizationState::copperizationProgress),
		Codec.FLOAT.optionalFieldOf("oxidation_progress", 0.0F).forGetter(CopperizationState::oxidationProgress),
		Codec.BOOL.optionalFieldOf("copper_statue", false).forGetter(CopperizationState::copperStatue),
		Codec.BOOL.optionalFieldOf("waxed", false).forGetter(CopperizationState::waxed),
		FrozenPoseSnapshot.CODEC.optionalFieldOf("frozen_pose").forGetter(CopperizationState::frozenPose),
		Codec.LONG.optionalFieldOf("next_weathering_tick", 0L).forGetter(CopperizationState::nextWeatheringTick)
	).apply(instance, CopperizationState::new));

	public CopperizationState {
		copperizationProgress = Mth.clamp(copperizationProgress, 0.0F, 1.0F);
		oxidationProgress = Mth.clamp(oxidationProgress, 0.0F, 1.0F);
		frozenPose = frozenPose == null ? Optional.empty() : frozenPose;
	}

	public CopperizationState withProgress(float progress) {
		return new CopperizationState(progress, oxidationProgress, copperStatue, waxed, frozenPose, nextWeatheringTick);
	}

	public CopperizationState asStatue(FrozenPoseSnapshot snapshot, long weatheringTick) {
		return new CopperizationState(1.0F, oxidationProgress, true, waxed, Optional.of(snapshot), weatheringTick);
	}

	public CopperizationState withOxidation(float progress, long weatheringTick) {
		return new CopperizationState(copperizationProgress, progress, copperStatue, waxed, frozenPose, weatheringTick);
	}

	public CopperizationState withWaxed(boolean value) {
		return new CopperizationState(copperizationProgress, oxidationProgress, copperStatue, value, frozenPose, nextWeatheringTick);
	}

	public CopperizationState withFrozenPose(FrozenPoseSnapshot snapshot) {
		return new CopperizationState(copperizationProgress, oxidationProgress, copperStatue, waxed, Optional.of(snapshot), nextWeatheringTick);
	}

	public WeatheringCopper.WeatherState weatherState() {
		if (oxidationProgress >= 1.0F) return WeatheringCopper.WeatherState.OXIDIZED;
		if (oxidationProgress >= 2.0F / 3.0F) return WeatheringCopper.WeatherState.WEATHERED;
		if (oxidationProgress >= 1.0F / 3.0F) return WeatheringCopper.WeatherState.EXPOSED;
		return WeatheringCopper.WeatherState.UNAFFECTED;
	}
}
