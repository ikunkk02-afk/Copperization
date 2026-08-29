package com.shouyun.copperization.mixin;

import com.shouyun.copperization.block.CopperizedBlockStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A replacement block must never inherit the copperization overlay from the old occupant. */
@Mixin(LevelChunk.class)
public abstract class LevelChunkCopperizationMixin {
	@Inject(method = "setBlockState", at = @At("HEAD"))
	private void copperization$clearReplacementOverlay(BlockPos pos, BlockState replacement, int flags, CallbackInfoReturnable<BlockState> cir) {
		LevelChunk chunk = (LevelChunk) (Object) this;
		// The server owns this state. A live copperization delta can arrive immediately before
		// the vanilla block update; clearing on the client here would erase that newer delta.
		if (!chunk.getLevel().isClientSide() && chunk.getBlockState(pos).getBlock() != replacement.getBlock()) {
			CopperizedBlockStorage.forget(chunk, pos);
		}
	}
}
