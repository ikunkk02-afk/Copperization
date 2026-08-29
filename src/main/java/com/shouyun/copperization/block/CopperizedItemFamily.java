package com.shouyun.copperization.block;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopperCollection;

/** Eight placeable item variants for sources that must remain their original block class in-world. */
public record CopperizedItemFamily(String name, Block source, WeatheringCopperCollection<Item> items) {
}
