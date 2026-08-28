package com.shouyun.copperization.registry;

import com.shouyun.copperization.Copperization;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {
	public static final TagKey<Item> COPPERIZATION_ENCHANTABLE = TagKey.create(Registries.ITEM, Copperization.id("copperization_enchantable"));
	public static final TagKey<Block> COPPERIZABLE_BLOCKS = TagKey.create(Registries.BLOCK, Copperization.id("copperizable_blocks"));

	private ModTags() {
	}
}
