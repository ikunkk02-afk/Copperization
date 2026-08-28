package com.shouyun.copperization.copper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import com.shouyun.copperization.CopperizationConstants;
import java.util.Optional;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.WeatheringCopper;
import org.junit.jupiter.api.Test;

class CopperizationStateTest {
	@Test
	void clampsNormalizedProgress() {
		CopperizationState state = new CopperizationState(1.5F, -2.0F, false, false, Optional.empty(), 0L);
		assertEquals(1.0F, state.copperizationProgress());
		assertEquals(0.0F, state.oxidationProgress());
	}

	@Test
	void mapsOxidationStages() {
		assertEquals(WeatheringCopper.WeatherState.UNAFFECTED, CopperizationState.EMPTY.weatherState());
		assertEquals(WeatheringCopper.WeatherState.EXPOSED, CopperizationState.EMPTY.withOxidation(1.0F / 3.0F, 0L).weatherState());
		assertEquals(WeatheringCopper.WeatherState.WEATHERED, CopperizationState.EMPTY.withOxidation(2.0F / 3.0F, 0L).weatherState());
		assertEquals(WeatheringCopper.WeatherState.OXIDIZED, CopperizationState.EMPTY.withOxidation(1.0F, 0L).weatherState());
	}

	@Test
	void codecRoundTripsFrozenState() {
		FrozenPoseSnapshot pose = new FrozenPoseSnapshot(1, 2, 3, 45, 12, 40, 50, Pose.CROUCHING, 2.5F, 0.4F, 0.75F, 91, true, true);
		CopperizationState expected = new CopperizationState(1.0F, 2.0F / 3.0F, true, true, Optional.of(pose), 12345L);
		var json = CopperizationState.CODEC.encodeStart(JsonOps.INSTANCE, expected).getOrThrow();
		CopperizationState actual = CopperizationState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
		assertEquals(expected, actual);
	}

	@Test
	void constantsDefineExpectedBalanceTable() {
		assertEquals(0.15F, CopperizationConstants.progressForLevel(1));
		assertEquals(0.22F, CopperizationConstants.progressForLevel(2));
		assertEquals(0.30F, CopperizationConstants.progressForLevel(3));
		assertEquals(0, CopperizationConstants.debuffStage(0.24F));
		assertEquals(3, CopperizationConstants.debuffStage(0.75F));
		assertTrue(CopperOxidationManager.scrape(CopperizationState.EMPTY.withWaxed(true)).orElseThrow().waxed() == false);
		assertFalse(CopperOxidationManager.scrape(CopperizationState.EMPTY).isPresent());
	}
}
