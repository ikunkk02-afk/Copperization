package com.shouyun.copperization.client;

import com.shouyun.copperization.client.render.CopperizationRenderLayer;
import com.shouyun.copperization.client.render.CopperizedClientChunkState;
import com.shouyun.copperization.client.render.CopperizedBlockModelPlugin;
import com.shouyun.copperization.block.CopperizedBlockStorage;
import com.shouyun.copperization.network.CopperizedBlockSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import com.shouyun.copperization.block.CopperizedChunkState;
import com.shouyun.copperization.registry.ModAttachments;

public class CopperizationClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		CopperizedBlockModelPlugin.register();
		ClientPlayNetworking.registerGlobalReceiver(CopperizedBlockSyncPayload.TYPE, (payload, context) -> {
			if (context.client().level != null) {
				CopperizedClientChunkState.apply(context.client().level, payload.pos(), payload.data());
				CopperizedBlockStorage.applyClientSync(context.client().level, payload.pos(), payload.data());
			}
		});
		ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
			CopperizedClientChunkState.replace(level, chunk.getPos(),
				chunk.getAttachedOrElse(ModAttachments.COPPERIZED_BLOCKS, CopperizedChunkState.EMPTY));
			chunk.onAttachedSet(ModAttachments.COPPERIZED_BLOCKS).register((oldValue, newValue) ->
				CopperizedClientChunkState.replace(level, chunk.getPos(),
					newValue == null ? CopperizedChunkState.EMPTY : newValue));
		});
		ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) ->
			CopperizedClientChunkState.unload(level, chunk.getPos()));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			if (client.level != null) CopperizedClientChunkState.clear(client.level);
		});
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, renderer, helper, context) ->
			CopperizationRenderLayer.register(renderer, helper));
	}
}
