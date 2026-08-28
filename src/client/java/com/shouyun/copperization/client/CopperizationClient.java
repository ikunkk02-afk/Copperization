package com.shouyun.copperization.client;

import com.shouyun.copperization.client.render.CopperizationRenderLayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;

public class CopperizationClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, renderer, helper, context) ->
			CopperizationRenderLayer.register(renderer, helper));
	}
}
