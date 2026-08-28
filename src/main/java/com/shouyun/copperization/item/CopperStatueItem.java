package com.shouyun.copperization.item;

import com.shouyun.copperization.CopperizationConstants;
import com.shouyun.copperization.copper.CopperizationManager;
import com.shouyun.copperization.copper.CopperizationState;
import com.shouyun.copperization.data.CopperStatueData;
import com.shouyun.copperization.registry.ModAttachments;
import com.shouyun.copperization.registry.ModDataComponents;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.storage.TagValueInput;

public class CopperStatueItem extends Item {
	public CopperStatueItem(Properties properties) {
		super(properties);
	}

	@Override
	public Component getName(ItemStack stack) {
		CopperStatueData data = stack.get(ModDataComponents.COPPER_STATUE_DATA);
		if (data == null) return super.getName(stack);
		var type = BuiltInRegistries.ENTITY_TYPE.getOptional(data.entityType());
		return type.isPresent()
			? Component.translatable("item.copperization.copper_statue.named", type.get().getDescription())
			: super.getName(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		CopperStatueData data = stack.get(ModDataComponents.COPPER_STATUE_DATA);
		if (data == null) return;
		builder.accept(Component.translatable("tooltip.copperization.oxidation", Component.translatable("copperization.oxidation." + data.state().weatherState().getSerializedName())));
		builder.accept(Component.translatable(data.state().waxed() ? "tooltip.copperization.waxed" : "tooltip.copperization.unwaxed"));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		ItemStack stack = context.getItemInHand();
		CopperStatueData data = stack.get(ModDataComponents.COPPER_STATUE_DATA);
		if (data == null) return InteractionResult.FAIL;
		if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;

		var type = BuiltInRegistries.ENTITY_TYPE.getOptional(data.entityType()).orElse(null);
		if (type == null) return InteractionResult.FAIL;
		Entity entity = type.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
		if (!(entity instanceof LivingEntity statue)) return InteractionResult.FAIL;

		statue.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data.entityData()));
		BlockPos targetPos = context.getClickedPos().relative(context.getClickedFace());
		float yaw = context.getPlayer() == null ? 0.0F : context.getPlayer().getYRot() + 180.0F;
		CopperizationState state = data.state();
		var pose = state.frozenPose().orElseGet(() -> com.shouyun.copperization.copper.FrozenPoseSnapshot.capture(statue))
			.at(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D, yaw);
		state = new CopperizationState(1.0F, state.oxidationProgress(), true, state.waxed(), java.util.Optional.of(pose),
			level.getGameTime() + CopperizationConstants.OXIDATION_SAMPLE_INTERVAL);
		statue.setAttached(ModAttachments.COPPERIZATION_STATE, state);
		CopperizationManager.applyGameplayState(statue, state);
		if (!level.noCollision(statue, statue.getBoundingBox()) || !level.addFreshEntity(statue)) return InteractionResult.FAIL;

		Player player = context.getPlayer();
		if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
		level.playSound(null, targetPos, SoundEvents.COPPER_GOLEM_STATUE_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
		return InteractionResult.SUCCESS_SERVER;
	}
}
