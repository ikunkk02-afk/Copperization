package com.shouyun.copperization.item;

import com.shouyun.copperization.block.CopperizedBlockData;
import com.shouyun.copperization.block.CopperizedBlockStorage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Places the original block and attaches its copper stage, preserving every vanilla placement behavior. */
public final class PositionalCopperizedBlockItem extends BlockItem {
	private final CopperizedBlockData copperData;

	public PositionalCopperizedBlockItem(Block source, int stage, boolean waxed, Properties properties) {
		super(source, properties);
		this.copperData = new CopperizedBlockData(stage, waxed, Long.MAX_VALUE);
	}

	public CopperizedBlockData copperData() {
		return copperData;
	}

	@Override
	protected boolean placeBlock(BlockPlaceContext context, BlockState placementState) {
		if (!super.placeBlock(context, placementState)) return false;
		if (context.getLevel() instanceof ServerLevel level) {
			CopperizedBlockStorage.place(level, context.getClickedPos(), copperData);
		}
		return true;
	}
}
