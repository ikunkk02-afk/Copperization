package com.shouyun.copperization.block;

import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class WeatheringCopperSlabBlock extends SlabBlock implements WeatheringCopper {
	private final WeatherState weatherState;
	public WeatheringCopperSlabBlock(WeatherState weatherState, BlockBehaviour.Properties properties) {
		super(properties); this.weatherState = weatherState;
	}
	@Override public WeatherState getAge() { return weatherState; }
}
