package com.shouyun.copperization.client.render;

import com.shouyun.copperization.block.CopperizedBlockData;
import com.shouyun.copperization.block.CopperizedChunkState;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

/**
 * Render-thread-safe mirror of the synchronized chunk attachments.
 *
 * <p>Chunk model emission receives a render-region view rather than the {@link ClientLevel}, so a
 * baked model cannot safely walk back to the chunk attachment through its render context. Keeping
 * the already-synchronized, immutable per-chunk values here makes the lookup local and avoids any
 * global position scan.</p>
 */
public final class CopperizedClientChunkState {
	private static final ConcurrentMap<Long, CopperizedChunkState> CHUNKS = new ConcurrentHashMap<>();
	private static volatile ClientLevel activeLevel;

	private CopperizedClientChunkState() {
	}

	public static void replace(ClientLevel level, ChunkPos pos, CopperizedChunkState state) {
		bind(level);
		if (state == null || state.entries().isEmpty()) CHUNKS.remove(key(pos.x(), pos.z()));
		else CHUNKS.put(key(pos.x(), pos.z()), state);
		if (state != null) {
			state.entries().keySet().forEach(localKey -> invalidate(level,
				CopperizedChunkState.unpack(localKey, pos.x(), pos.z())));
		}
	}

	public static void apply(ClientLevel level, BlockPos pos, CopperizedBlockData data) {
		bind(level);
		long chunkKey = key(pos.getX() >> 4, pos.getZ() >> 4);
		CHUNKS.compute(chunkKey, (ignored, current) -> {
			CopperizedChunkState state = current == null ? CopperizedChunkState.EMPTY : current;
			CopperizedChunkState changed = data == null ? state.remove(pos) : state.put(pos, data);
			return changed.entries().isEmpty() ? null : changed;
		});
		invalidate(level, pos);
	}

	public static CopperizedBlockData get(BlockPos pos) {
		CopperizedChunkState state = CHUNKS.get(key(pos.getX() >> 4, pos.getZ() >> 4));
		return state == null ? null : state.get(pos);
	}

	public static void unload(ClientLevel level, ChunkPos pos) {
		if (activeLevel == level) CHUNKS.remove(key(pos.x(), pos.z()));
	}

	public static void clear(ClientLevel level) {
		if (activeLevel == level) {
			CHUNKS.clear();
			activeLevel = null;
		}
	}

	private static synchronized void bind(ClientLevel level) {
		if (activeLevel != level) {
			CHUNKS.clear();
			activeLevel = level;
		}
	}

	private static long key(int chunkX, int chunkZ) {
		return (chunkX & 0xffffffffL) | ((long) chunkZ << 32);
	}

	private static void invalidate(ClientLevel level, BlockPos pos) {
		level.setSectionDirtyWithNeighbors(
			SectionPos.blockToSectionCoord(pos.getX()),
			SectionPos.blockToSectionCoord(pos.getY()),
			SectionPos.blockToSectionCoord(pos.getZ())
		);
	}
}
