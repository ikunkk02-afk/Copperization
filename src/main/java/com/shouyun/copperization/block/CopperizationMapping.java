package com.shouyun.copperization.block;

import java.util.Optional;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A reversible relationship between an original block and its copper representation.
 * Positional mappings intentionally return the original state: their representation lives in
 * {@link CopperizedBlockStorage}, so every original property remains authoritative.
 */
public record CopperizationMapping(Block originalBlock, Optional<CopperizedBlockFamily> legacyFamily) {
	public static CopperizationMapping positional(Block originalBlock) {
		return new CopperizationMapping(originalBlock, Optional.empty());
	}

	public static CopperizationMapping legacy(CopperizedBlockFamily family) {
		return new CopperizationMapping(family.source(), Optional.of(family));
	}

	public boolean isLegacy() {
		return legacyFamily.isPresent();
	}

	public Optional<BlockState> copperize(BlockState source) {
		if (!source.is(originalBlock)) return Optional.empty();
		return Optional.of(legacyFamily.map(family -> family.freshBlock().withPropertiesOf(source)).orElse(source));
	}

	public Optional<BlockState> restore(BlockState copperized) {
		if (legacyFamily.isEmpty()) return copperized.is(originalBlock) ? Optional.of(copperized) : Optional.empty();
		CopperizedBlockFamily family = legacyFamily.get();
		return family.blocks().asList().contains(copperized.getBlock())
			? Optional.of(originalBlock.withPropertiesOf(copperized))
			: Optional.empty();
	}
}
