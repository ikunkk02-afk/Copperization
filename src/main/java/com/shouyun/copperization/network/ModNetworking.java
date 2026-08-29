package com.shouyun.copperization.network;

import com.shouyun.copperization.block.CopperizedBlockData;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Registers and sends live deltas; chunk attachments remain the persistent source of truth. */
public final class ModNetworking {
	private ModNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(CopperizedBlockSyncPayload.TYPE, CopperizedBlockSyncPayload.STREAM_CODEC);
	}

	public static void syncCopperizedBlock(ServerLevel level, BlockPos pos, CopperizedBlockData data) {
		CopperizedBlockSyncPayload payload = new CopperizedBlockSyncPayload(pos, data);
		// The attachment remains the persisted/initial-chunk source of truth. Explicit live deltas
		// are sent to players in this level: chunk tracker acknowledgement can lag one tick behind a
		// just-loaded client chunk, while a position-state change must become visible immediately.
		PlayerLookup.level(level).forEach(player -> ServerPlayNetworking.send(player, payload));
	}
}
