package com.shouyun.copperization.item;

import com.shouyun.copperization.CopperizationConstants;
import com.shouyun.copperization.block.CopperizableBlockRegistry;
import com.shouyun.copperization.block.CopperizedBlockStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class CopperizationWandItem extends Item {
	public CopperizationWandItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		BlockPos pos = context.getClickedPos();
		BlockState current = context.getLevel().getBlockState(pos);
		var target = CopperizableBlockRegistry.copperize(current);
		if (target.isEmpty()) return InteractionResult.PASS;
		// Existing families remain physical blocks for save/item compatibility. Everything else
		// uses the sparse positional state and therefore keeps its own vanilla implementation.
		if (CopperizableBlockRegistry.get(current.getBlock()).isEmpty()) {
			if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
			if (!CopperizedBlockStorage.copperize(level, pos)) return InteractionResult.FAIL;
			finish(context.getPlayer(), context.getItemInHand(), context.getHand());
			level.sendParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 24, 0.45D, 0.45D, 0.45D, 0.02D);
			level.playSound(null, pos, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 1.0F, 1.15F);
			return InteractionResult.SUCCESS_SERVER;
		}
		if (!(context.getLevel() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}

		if (!level.setBlock(pos, target.get(), Block.UPDATE_ALL)) {
			return InteractionResult.FAIL;
		}

		Player player = context.getPlayer();
		ItemStack stack = context.getItemInHand();
		finish(player, stack, context.getHand());
		level.sendParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 24, 0.45D, 0.45D, 0.45D, 0.02D);
		level.playSound(null, pos, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 1.0F, 1.15F);
		return InteractionResult.SUCCESS_SERVER;
	}

	private static void finish(Player player, ItemStack stack, net.minecraft.world.InteractionHand hand) {
		if (player == null) return;
		if (!player.getAbilities().instabuild) stack.hurtAndBreak(1, player, hand);
		player.getCooldowns().addCooldown(stack, CopperizationConstants.WAND_COOLDOWN_TICKS);
	}
}
