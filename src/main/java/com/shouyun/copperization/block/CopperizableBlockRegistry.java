package com.shouyun.copperization.block;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class CopperizableBlockRegistry {
	private static final Map<Block, CopperizedBlockFamily> MAPPINGS = new IdentityHashMap<>();

	private CopperizableBlockRegistry() {
	}

	public static void register(Block source, CopperizedBlockFamily family) {
		if (source.defaultBlockState().hasBlockEntity()) {
			throw new IllegalArgumentException("Copperizable source must not have a block entity: " + source);
		}
		CopperizedBlockFamily previous = MAPPINGS.putIfAbsent(source, family);
		if (previous != null) {
			throw new IllegalStateException("Duplicate copperization mapping for " + source);
		}
	}

	public static Optional<CopperizedBlockFamily> get(Block source) {
		return Optional.ofNullable(MAPPINGS.get(source));
	}

	public static Optional<BlockState> copperize(BlockState source) {
		return get(source.getBlock()).map(family -> family.freshBlock().withPropertiesOf(source));
	}

	public static Map<Block, CopperizedBlockFamily> mappings() {
		return Collections.unmodifiableMap(MAPPINGS);
	}
}
