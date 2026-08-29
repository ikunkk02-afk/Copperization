package com.shouyun.copperization.item;

import com.shouyun.copperization.CopperizationConstants;
import com.shouyun.copperization.block.CopperizableBlockRegistry;
import com.shouyun.copperization.block.CopperizedBlockStorage;
import com.shouyun.copperization.copper.CopperizationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Reverses Copperization without reconstructing entities or dropping safe block-state properties. */
public class RestorationWandItem extends Item {
	private static final DustParticleOptions RESTORATION_PARTICLE = new DustParticleOptions(0x78D6C7, 0.85F);

	public RestorationWandItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		BlockPos pos = context.getClickedPos();
		BlockState current = context.getLevel().getBlockState(pos);
		if (CopperizedBlockStorage.isCopperized(context.getLevel(), pos)) {
			if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
			if (!CopperizedBlockStorage.restore(level, pos)) return InteractionResult.FAIL;
			finish(context.getPlayer(), context.getItemInHand(), context.getHand());
			playFeedback(level, pos);
			return InteractionResult.SUCCESS_SERVER;
		}
		var restored = CopperizableBlockRegistry.restore(current);
		if (restored.isEmpty()) return InteractionResult.PASS;
		if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
		if (!level.setBlock(pos, restored.get(), Block.UPDATE_ALL)) return InteractionResult.FAIL;

		finish(context.getPlayer(), context.getItemInHand(), context.getHand());
		playFeedback(level, pos);
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) return InteractionResult.PASS;
		ItemStack offhand = player.getOffhandItem();
		var restored = CopperizableBlockRegistry.restore(offhand);
		if (restored.isEmpty()) return InteractionResult.PASS;
		if (level.isClientSide()) return InteractionResult.SUCCESS;

		player.setItemInHand(InteractionHand.OFF_HAND, restored.get());
		finish(player, player.getItemInHand(hand), hand);
		level.playSound(null, player.blockPosition(), SoundEvents.AXE_WAX_OFF, SoundSource.PLAYERS, 0.85F, 1.25F);
		return InteractionResult.SUCCESS_SERVER;
	}

	/** Called before statue wax/scrape handling, so restoration is the wand's unambiguous entity action. */
	public static InteractionResult tryRestoreEntity(Player player, Level level, InteractionHand hand, Entity target) {
		ItemStack held = player.getItemInHand(hand);
		if (!(held.getItem() instanceof RestorationWandItem) || !(target instanceof LivingEntity living)
			|| !CopperizationManager.getState(living).copperizationActive()
				&& !CopperizationManager.getState(living).copperStatue()
				&& CopperizationManager.getState(living).copperizationProgress() <= 0.0F) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		if (!(level instanceof ServerLevel serverLevel) || !CopperizationManager.restore(living)) return InteractionResult.FAIL;

		finish(player, held, hand);
		serverLevel.sendParticles(RESTORATION_PARTICLE, living.getX(), living.getY(0.55D), living.getZ(), 18,
			living.getBbWidth() * 0.45D, living.getBbHeight() * 0.35D, living.getBbWidth() * 0.45D, 0.02D);
		serverLevel.playSound(null, living.blockPosition(), SoundEvents.AXE_WAX_OFF, SoundSource.PLAYERS, 0.95F, 1.20F);
		return InteractionResult.SUCCESS_SERVER;
	}

	private static void finish(Player player, ItemStack wand, InteractionHand hand) {
		if (player == null) return;
		if (!player.getAbilities().instabuild) wand.hurtAndBreak(1, player, hand);
		player.getCooldowns().addCooldown(wand, CopperizationConstants.WAND_COOLDOWN_TICKS);
	}

	private static void playFeedback(ServerLevel level, BlockPos pos) {
		level.sendParticles(RESTORATION_PARTICLE, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
			18, 0.40D, 0.40D, 0.40D, 0.02D);
		level.playSound(null, pos, SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 0.90F, 1.20F);
	}
}
