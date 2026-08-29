package com.shouyun.copperization.block;

import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class WeatheringCopperFenceBlock extends FenceBlock implements WeatheringCopper {
	private final WeatherState weatherState;
	public WeatheringCopperFenceBlock(WeatherState weatherState, BlockBehaviour.Properties properties) {
		super(properties); this.weatherState = weatherState;
	}
	@Override public WeatherState getAge() { return weatherState; }
}
