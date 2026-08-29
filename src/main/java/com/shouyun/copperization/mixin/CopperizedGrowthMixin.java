package com.shouyun.copperization.mixin;

import com.shouyun.copperization.block.CopperizableBlockClassifier;
import com.shouyun.copperization.block.CopperizedBlockStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stops random growth only while the position is metalized; the vanilla block stays untouched. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class CopperizedGrowthMixin {
	@Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
	private void copperization$freezeMetalizedGrowth(ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
		BlockState state = (BlockState) (Object) this;
		if (CopperizedBlockStorage.isCopperized(level, pos) && CopperizableBlockClassifier.freezesGrowth(state)) ci.cancel();
	}
}
