package com.shouyun.copperization.registry;

import com.shouyun.copperization.Copperization;
import com.shouyun.copperization.block.CopperizableBlockRegistry;
import com.shouyun.copperization.block.CopperizedBlockFamily;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.registry.OxidizableBlocksRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.WeatheringCopperCollection;
import net.minecraft.world.level.block.WeatheringCopperFullBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {
	private static final List<CopperizedBlockFamily> FAMILIES = new ArrayList<>();

	private ModBlocks() {
	}

	public static void register() {
		if (!FAMILIES.isEmpty()) return;
		registerFamily("stone", Blocks.STONE);
		registerFamily("cobblestone", Blocks.COBBLESTONE);
		registerFamily("stone_bricks", Blocks.STONE_BRICKS);
		registerFamily("deepslate", Blocks.DEEPSLATE);
		registerFamily("cobbled_deepslate", Blocks.COBBLED_DEEPSLATE);
		registerFamily("deepslate_bricks", Blocks.DEEPSLATE_BRICKS);
		registerFamily("bricks", Blocks.BRICKS);
		registerFamily("blackstone", Blocks.BLACKSTONE);
		registerFamily("polished_blackstone", Blocks.POLISHED_BLACKSTONE);
		registerFamily("end_stone", Blocks.END_STONE);
		registerFamily("nether_bricks", Blocks.NETHER_BRICKS);
		Copperization.LOGGER.info("Registered {} copperizable block families ({} blocks)", FAMILIES.size(), FAMILIES.size() * 8);
	}

	private static void registerFamily(String name, Block source) {
		WeatheringCopperCollection.ByState<Block> weathering = new WeatheringCopperCollection.ByState<>(
			registerWeatheringBlock("copperized_" + name, source, WeatheringCopper.WeatherState.UNAFFECTED),
			registerWeatheringBlock("exposed_copperized_" + name, source, WeatheringCopper.WeatherState.EXPOSED),
			registerWeatheringBlock("weathered_copperized_" + name, source, WeatheringCopper.WeatherState.WEATHERED),
			registerWeatheringBlock("oxidized_copperized_" + name, source, WeatheringCopper.WeatherState.OXIDIZED)
		);
		WeatheringCopperCollection.ByState<Block> waxed = new WeatheringCopperCollection.ByState<>(
			registerWaxedBlock("waxed_copperized_" + name, source),
			registerWaxedBlock("waxed_exposed_copperized_" + name, source),
			registerWaxedBlock("waxed_weathered_copperized_" + name, source),
			registerWaxedBlock("waxed_oxidized_copperized_" + name, source)
		);
		WeatheringCopperCollection<Block> blocks = new WeatheringCopperCollection<>(weathering, waxed);
		WeatheringCopperCollection<Item> items = blocks.map(ModBlocks::registerBlockItem);
		CopperizedBlockFamily family = new CopperizedBlockFamily(name, source, blocks, items);
		OxidizableBlocksRegistry.registerWeatheringCopperBlocks(blocks);
		CopperizableBlockRegistry.register(source, family);
		FAMILIES.add(family);
	}

	private static Block registerWeatheringBlock(String name, Block source, WeatheringCopper.WeatherState state) {
		Identifier id = Copperization.id(name);
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
		BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofFullCopy(source).randomTicks().setId(key);
		return Registry.register(BuiltInRegistries.BLOCK, key, new WeatheringCopperFullBlock(state, properties));
	}

	private static Block registerWaxedBlock(String name, Block source) {
		Identifier id = Copperization.id(name);
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
		return Registry.register(BuiltInRegistries.BLOCK, key, new Block(BlockBehaviour.Properties.ofFullCopy(source).setId(key)));
	}

	private static Item registerBlockItem(Block block) {
		Identifier id = BuiltInRegistries.BLOCK.getKey(block);
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
		BlockItem item = new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(key));
		item.registerBlocks(Item.BY_BLOCK, item);
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	public static List<CopperizedBlockFamily> families() {
		return List.copyOf(FAMILIES);
	}
}
