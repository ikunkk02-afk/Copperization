package com.shouyun.copperization.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;

/** Safely replaces a mapped block while migrating compatible block-entity data. */
public final class CopperizedBlockReplacement {
	private CopperizedBlockReplacement() {
	}

	public static boolean replace(ServerLevel level, BlockPos pos, BlockState target) {
		BlockEntity oldEntity = level.getBlockEntity(pos);
		var saved = oldEntity == null ? null : oldEntity.saveWithoutMetadata(level.registryAccess());
		if (!level.setBlock(pos, target, Block.UPDATE_ALL)) return false;
		if (saved != null) {
			BlockEntity replacement = level.getBlockEntity(pos);
			if (replacement != null) {
				replacement.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
				replacement.setChanged();
			}
		}
		return true;
	}
}
