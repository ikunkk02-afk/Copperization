package com.shouyun.copperization.block;

import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class WeatheringCopperStairBlock extends StairBlock implements WeatheringCopper {
	private final WeatherState weatherState;

	public WeatheringCopperStairBlock(WeatherState weatherState, BlockState baseState, BlockBehaviour.Properties properties) {
		super(baseState, properties);
		this.weatherState = weatherState;
	}

	@Override public WeatherState getAge() { return weatherState; }
}
