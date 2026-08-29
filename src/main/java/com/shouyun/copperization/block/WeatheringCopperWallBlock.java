package com.shouyun.copperization.block;

import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class WeatheringCopperWallBlock extends WallBlock implements WeatheringCopper {
	private final WeatherState weatherState;
	public WeatheringCopperWallBlock(WeatherState weatherState, BlockBehaviour.Properties properties) {
		super(properties); this.weatherState = weatherState;
	}
	@Override public WeatherState getAge() { return weatherState; }
}
