package com.shouyun.copperization.block;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class CopperizableBlockRegistry {
	private static final Map<Block, CopperizationMapping> ORIGINAL_MAPPINGS = new IdentityHashMap<>();
	private static final Map<Block, CopperizationMapping> COPPERIZED_BLOCK_MAPPINGS = new IdentityHashMap<>();
	private static final Map<Item, CopperizationMapping> COPPERIZED_ITEM_MAPPINGS = new IdentityHashMap<>();

	private CopperizableBlockRegistry() {
	}

	public static void register(Block source, CopperizedBlockFamily family) {
		CopperizationMapping mapping = CopperizationMapping.legacy(family);
		CopperizationMapping previous = ORIGINAL_MAPPINGS.putIfAbsent(source, mapping);
		if (previous != null) {
			throw new IllegalStateException("Duplicate copperization mapping for " + source);
		}
		for (Block copperized : family.blocks().asList()) COPPERIZED_BLOCK_MAPPINGS.put(copperized, mapping);
		for (Item copperized : family.items().asList()) COPPERIZED_ITEM_MAPPINGS.put(copperized, mapping);
	}

	public static Optional<CopperizedBlockFamily> get(Block source) {
		return Optional.ofNullable(ORIGINAL_MAPPINGS.get(source)).flatMap(CopperizationMapping::legacyFamily);
	}

	public static boolean hasPhysicalMapping(Block source) {
		return ORIGINAL_MAPPINGS.containsKey(source);
	}

	public static Optional<BlockState> copperize(BlockState source) {
		return mappingForOriginal(source).flatMap(mapping -> mapping.copperize(source));
	}

	public static Optional<BlockState> restore(BlockState copperized) {
		return Optional.ofNullable(COPPERIZED_BLOCK_MAPPINGS.get(copperized.getBlock())).flatMap(mapping -> mapping.restore(copperized));
	}

	public static Optional<ItemStack> restore(ItemStack copperized) {
		CopperizationMapping mapping = COPPERIZED_ITEM_MAPPINGS.get(copperized.getItem());
		if (mapping == null) return Optional.empty();
		Item original = mapping.originalBlock().asItem();
		return original == null ? Optional.empty() : Optional.of(copperized.transmuteCopy(original, copperized.getCount()));
	}

	public static Optional<CopperizationMapping> mappingForOriginal(BlockState state) {
		CopperizationMapping known = ORIGINAL_MAPPINGS.get(state.getBlock());
		if (known != null) return Optional.of(known);
		return CopperizableBlockClassifier.supports(state) ? Optional.of(CopperizationMapping.positional(state.getBlock())) : Optional.empty();
	}

	public static Optional<CopperizationMapping> mappingForCopperized(BlockState state) {
		return Optional.ofNullable(COPPERIZED_BLOCK_MAPPINGS.get(state.getBlock()));
	}

	public static boolean supportsPositionCopperization(BlockState state) {
		return CopperizableBlockClassifier.supports(state);
	}

	/** Returns the visual stage encoded by a real registered copperized block variant. */
	public static Optional<CopperizedBlockData> physicalCopperData(BlockState state) {
		CopperizationMapping mapping = COPPERIZED_BLOCK_MAPPINGS.get(state.getBlock());
		if (mapping == null || mapping.legacyFamily().isEmpty()) return Optional.empty();
		int index = mapping.legacyFamily().get().blocks().asList().indexOf(state.getBlock());
		return index < 0 ? Optional.empty() : Optional.of(new CopperizedBlockData(index % 4, index >= 4, Long.MAX_VALUE));
	}

	public static Map<Block, CopperizedBlockFamily> mappings() {
		Map<Block, CopperizedBlockFamily> result = new IdentityHashMap<>();
		ORIGINAL_MAPPINGS.forEach((block, mapping) -> mapping.legacyFamily().ifPresent(family -> result.put(block, family)));
		return Collections.unmodifiableMap(result);
	}
}
