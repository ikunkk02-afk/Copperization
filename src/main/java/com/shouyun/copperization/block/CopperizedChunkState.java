package com.shouyun.copperization.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;

/** Persistent sparse state for a single chunk. Keys are packed local X/Z plus absolute Y. */
public record CopperizedChunkState(Map<Integer, CopperizedBlockData> entries) {
	public static final CopperizedChunkState EMPTY = new CopperizedChunkState(Map.of());
	public static final Codec<CopperizedChunkState> CODEC = SavedEntry.CODEC.listOf().xmap(
		values -> {
			Map<Integer, CopperizedBlockData> entries = new HashMap<>();
			for (SavedEntry value : values) entries.put(value.key(), value.data());
			return entries.isEmpty() ? EMPTY : new CopperizedChunkState(entries);
		},
		state -> state.entries().entrySet().stream().map(entry -> new SavedEntry(entry.getKey(), entry.getValue())).toList()
	);

	public CopperizedChunkState {
		entries = Map.copyOf(entries);
	}

	public CopperizedBlockData get(BlockPos pos) {
		return entries.get(key(pos));
	}

	public CopperizedChunkState put(BlockPos pos, CopperizedBlockData value) {
		Map<Integer, CopperizedBlockData> changed = new HashMap<>(entries);
		changed.put(key(pos), value);
		return new CopperizedChunkState(changed);
	}

	public CopperizedChunkState remove(BlockPos pos) {
		int key = key(pos);
		if (!entries.containsKey(key)) return this;
		Map<Integer, CopperizedBlockData> changed = new HashMap<>(entries);
		changed.remove(key);
		return changed.isEmpty() ? EMPTY : new CopperizedChunkState(changed);
	}

	public static int key(BlockPos pos) {
		return (pos.getY() << 8) | ((pos.getZ() & 15) << 4) | (pos.getX() & 15);
	}

	public static BlockPos unpack(int key, int chunkX, int chunkZ) {
		return new BlockPos((chunkX << 4) | (key & 15), key >> 8, (chunkZ << 4) | ((key >>> 4) & 15));
	}

	private record SavedEntry(int key, CopperizedBlockData data) {
		private static final Codec<SavedEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("local_pos").forGetter(SavedEntry::key),
			CopperizedBlockData.CODEC.fieldOf("data").forGetter(SavedEntry::data)
		).apply(instance, SavedEntry::new));
	}
}
