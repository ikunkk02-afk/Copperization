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
		CopperizationState state = new CopperizationState(1.5F, true, 3, -2.0F, false, false, Optional.empty(), 0L);
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
		CopperizationState expected = new CopperizationState(1.0F, false, 3, 2.0F / 3.0F, true, true, Optional.of(pose), 12345L);
		var json = CopperizationState.CODEC.encodeStart(JsonOps.INSTANCE, expected).getOrThrow();
		CopperizationState actual = CopperizationState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
		assertEquals(expected, actual);
	}

	@Test
	void constantsDefineExpectedBalanceTable() {
		assertEquals(240, CopperizationConstants.durationTicksForLevel(1));
		assertEquals(160, CopperizationConstants.durationTicksForLevel(2));
		assertEquals(100, CopperizationConstants.durationTicksForLevel(3));
		assertEquals(1.0F / 240.0F, CopperizationConstants.progressPerTick(1));
		assertTrue(CopperizationConstants.movementModifier(0.20F) > CopperizationConstants.movementModifier(0.75F));
		assertTrue(CopperOxidationManager.scrape(CopperizationState.EMPTY.withWaxed(true)).orElseThrow().waxed() == false);
		assertFalse(CopperOxidationManager.scrape(CopperizationState.EMPTY).isPresent());
	}
}
