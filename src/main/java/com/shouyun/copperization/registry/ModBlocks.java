package com.shouyun.copperization.registry;

import com.shouyun.copperization.Copperization;
import com.shouyun.copperization.block.CopperizableBlockRegistry;
import com.shouyun.copperization.block.CopperizedBlockFamily;
import com.shouyun.copperization.block.CopperizableBlockClassifier;
import com.shouyun.copperization.block.WeatheringCopperFenceBlock;
import com.shouyun.copperization.block.WeatheringCopperFenceGateBlock;
import com.shouyun.copperization.block.WeatheringCopperDoorBlock;
import com.shouyun.copperization.block.WeatheringCopperTrapDoorBlock;
import com.shouyun.copperization.block.WeatheringCopperSlabBlock;
import com.shouyun.copperization.block.WeatheringCopperStairBlock;
import com.shouyun.copperization.block.WeatheringCopperWallBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.registry.OxidizableBlocksRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.WeatheringCopperCollection;
import net.minecraft.world.level.block.WeatheringCopperFullBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

public final class ModBlocks {
	private static final List<CopperizedBlockFamily> FAMILIES = new ArrayList<>();
	private static final Set<String> LEGACY_TEXTURE_FAMILIES = Set.of(
		"stone", "cobblestone", "stone_bricks", "deepslate", "cobbled_deepslate", "deepslate_bricks",
		"bricks", "blackstone", "polished_blackstone", "end_stone", "nether_bricks"
	);

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
		registerFamily("grass_block", Blocks.GRASS_BLOCK);
		registerBasicFullBlockFamilies();
		registerShapeFamilies();
		registerVanillaFamily("chest", Blocks.CHEST, Blocks.COPPER_CHEST, Items.COPPER_CHEST);
		Copperization.LOGGER.info("Registered {} copperizable block families ({} blocks)", FAMILIES.size(), FAMILIES.size() * 8);
	}

	private static void registerShapeFamilies() {
		List<Block> vanillaSnapshot = BuiltInRegistries.BLOCK.stream()
			.filter(block -> "minecraft".equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace()))
			.toList();
		for (Block source : vanillaSnapshot) {
			Identifier id = BuiltInRegistries.BLOCK.getKey(source);
			if (!(source instanceof StairBlock || source instanceof SlabBlock || source instanceof WallBlock || source instanceof FenceBlock
				|| source instanceof FenceGateBlock || source instanceof DoorBlock || source instanceof TrapDoorBlock)
				|| source.asItem() == Items.AIR || source instanceof WeatheringCopper || id.getPath().contains("copper")
				|| CopperizableBlockRegistry.hasPhysicalMapping(source)) continue;
			registerFamily(id.getPath(), source);
		}
	}

	private static void registerBasicFullBlockFamilies() {
		List<Block> vanillaSnapshot = BuiltInRegistries.BLOCK.stream()
			.filter(block -> "minecraft".equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace()))
			.toList();
		for (Block source : vanillaSnapshot) {
			Identifier id = BuiltInRegistries.BLOCK.getKey(source);
			if (source.getClass() != Block.class || source.asItem() == Items.AIR
				|| source.defaultBlockState().hasBlockEntity() || source instanceof WeatheringCopper
				|| id.getPath().contains("copper") || !CopperizableBlockClassifier.supportsDuringRegistration(source.defaultBlockState())
				|| CopperizableBlockRegistry.hasPhysicalMapping(source)) continue;
			registerFamily(id.getPath(), source);
		}
	}

	public static boolean usesLegacyTexture(CopperizedBlockFamily family) {
		return LEGACY_TEXTURE_FAMILIES.contains(family.name());
	}

	private static void registerVanillaFamily(
		String name,
		Block source,
		WeatheringCopperCollection<Block> blocks,
		WeatheringCopperCollection<Item> items
	) {
		CopperizedBlockFamily family = new CopperizedBlockFamily(name, source, blocks, items);
		CopperizableBlockRegistry.register(source, family);
		FAMILIES.add(family);
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
		Block block = switch (source) {
			case StairBlock stair -> new WeatheringCopperStairBlock(state, stair.defaultBlockState(), properties);
			case SlabBlock ignored -> new WeatheringCopperSlabBlock(state, properties);
			case WallBlock ignored -> new WeatheringCopperWallBlock(state, properties);
			case FenceBlock ignored -> new WeatheringCopperFenceBlock(state, properties);
			case FenceGateBlock ignored -> new WeatheringCopperFenceGateBlock(state, woodTypeFor(source), properties);
			case DoorBlock door -> new WeatheringCopperDoorBlock(state, door.type(), properties);
			case TrapDoorBlock ignored -> new WeatheringCopperTrapDoorBlock(state, blockSetTypeFor(source), properties);
			default -> new WeatheringCopperFullBlock(state, properties);
		};
		return Registry.register(BuiltInRegistries.BLOCK, key, block);
	}

	private static Block registerWaxedBlock(String name, Block source) {
		Identifier id = Copperization.id(name);
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
		BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofFullCopy(source).setId(key);
		Block block = switch (source) {
			case StairBlock stair -> new StairBlock(stair.defaultBlockState(), properties);
			case SlabBlock ignored -> new SlabBlock(properties);
			case WallBlock ignored -> new WallBlock(properties);
			case FenceBlock ignored -> new FenceBlock(properties);
			case FenceGateBlock ignored -> new FenceGateBlock(woodTypeFor(source), properties);
			case DoorBlock door -> new DoorBlock(door.type(), properties);
			case TrapDoorBlock ignored -> new TrapDoorBlock(blockSetTypeFor(source), properties);
			default -> new Block(properties);
		};
		return Registry.register(BuiltInRegistries.BLOCK, key, block);
	}

	private static WoodType woodTypeFor(Block source) {
		String path = BuiltInRegistries.BLOCK.getKey(source).getPath();
		return WoodType.values().filter(type -> path.startsWith(type.name() + "_")).findFirst().orElse(WoodType.OAK);
	}

	private static BlockSetType blockSetTypeFor(Block source) {
		String path = BuiltInRegistries.BLOCK.getKey(source).getPath();
		return BlockSetType.values().filter(type -> path.startsWith(type.name() + "_")).findFirst().orElse(BlockSetType.OAK);
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
