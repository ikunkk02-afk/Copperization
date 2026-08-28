package com.shouyun.copperization.client.render;

import com.shouyun.copperization.copper.CopperizationManager;
import com.shouyun.copperization.registry.ModAttachments;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public final class CopperRenderStateExtractor {
	public static final RenderStateDataKey<CopperRenderState> KEY = RenderStateDataKey.create(() -> "copperization:living_state");

	private CopperRenderStateExtractor() {
	}

	public static void extract(LivingEntity entity, LivingEntityRenderState renderState) {
		FabricRenderState fabricState = (FabricRenderState) renderState;
		if (!entity.hasAttached(ModAttachments.COPPERIZATION_STATE)) {
			fabricState.setData(KEY, null);
			return;
		}
		var state = CopperizationManager.getState(entity);
		if (state.copperizationProgress() <= 0.0F) {
			fabricState.setData(KEY, null);
			return;
		}
		CopperRenderState copperState = new CopperRenderState(state.copperizationProgress(), state.weatherState(), state.copperStatue(), state.waxed(), state.frozenPose());
		fabricState.setData(KEY, copperState);
		if (state.copperStatue()) {
			state.frozenPose().ifPresent(snapshot -> applyFrozenPose(renderState, snapshot));
		}
	}

	public static void applyArmedPose(LivingEntity entity, ArmedEntityRenderState state) {
		if (!entity.hasAttached(ModAttachments.COPPERIZATION_STATE)) return;
		var copper = CopperizationManager.getState(entity);
		if (copper.copperStatue()) {
			copper.frozenPose().ifPresent(snapshot -> state.attackTime = snapshot.attackAnimation());
		}
	}

	private static void applyFrozenPose(LivingEntityRenderState state, com.shouyun.copperization.copper.FrozenPoseSnapshot snapshot) {
		state.bodyRot = snapshot.bodyYaw();
		state.yRot = Mth.wrapDegrees(snapshot.headYaw() - snapshot.bodyYaw());
		state.xRot = snapshot.pitch();
		state.walkAnimationPos = snapshot.limbPosition();
		state.walkAnimationSpeed = snapshot.limbSpeed();
		state.pose = snapshot.pose();
		state.ageInTicks = snapshot.ageInTicks();
		if (state instanceof ArmedEntityRenderState armed) armed.attackTime = snapshot.attackAnimation();
	}
}
