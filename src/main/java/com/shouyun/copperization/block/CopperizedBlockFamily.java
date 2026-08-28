package com.shouyun.copperization.block;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopperCollection;

public record CopperizedBlockFamily(
	String name,
	Block source,
	WeatheringCopperCollection<Block> blocks,
	WeatheringCopperCollection<Item> items
) {
	public Block freshBlock() {
		return blocks.weathering().unaffected();
	}

	public Item freshItem() {
		return items.weathering().unaffected();
	}
}
