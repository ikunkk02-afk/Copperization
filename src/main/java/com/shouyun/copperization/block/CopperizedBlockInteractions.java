package com.shouyun.copperization.block;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;

/** Wax, scrape and growth-freeze interactions for the generic positional form. */
public final class CopperizedBlockInteractions {
	private CopperizedBlockInteractions() {
	}

	public static void registerEvents() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> interact(player.getItemInHand(hand), player, hand, level, hit.getBlockPos()));
	}

	private static InteractionResult interact(ItemStack held, net.minecraft.world.entity.player.Player player, InteractionHand hand, Level level, net.minecraft.core.BlockPos pos) {
		if (!CopperizedBlockStorage.isCopperized(level, pos)) return InteractionResult.PASS;
		BlockState state = level.getBlockState(pos);
		if (held.is(Items.BONE_MEAL) && CopperizableBlockClassifier.freezesGrowth(state)) return InteractionResult.FAIL;
		if (held.is(Items.HONEYCOMB)) {
			if (level.isClientSide()) return InteractionResult.SUCCESS;
			if (!(level instanceof ServerLevel serverLevel) || !CopperizedBlockStorage.wax(serverLevel, pos)) return InteractionResult.PASS;
			if (!player.getAbilities().instabuild) held.shrink(1);
			serverLevel.levelEvent(player, LevelEvent.PARTICLES_AND_SOUND_WAX_ON, pos, 0);
			return InteractionResult.SUCCESS_SERVER;
		}
		if (held.is(ItemTags.AXES)) {
			if (level.isClientSide()) return InteractionResult.SUCCESS;
			if (!(level instanceof ServerLevel serverLevel) || !CopperizedBlockStorage.scrape(serverLevel, pos)) return InteractionResult.PASS;
			if (!player.getAbilities().instabuild) held.hurtAndBreak(1, player, hand);
			serverLevel.playSound(null, pos, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0F, 1.0F);
			serverLevel.levelEvent(player, LevelEvent.PARTICLES_SCRAPE, pos, 0);
			return InteractionResult.SUCCESS_SERVER;
		}
		return InteractionResult.PASS;
	}
}
