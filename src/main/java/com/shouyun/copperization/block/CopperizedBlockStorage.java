package com.shouyun.copperization.block;

import com.shouyun.copperization.CopperizationConstants;
import com.shouyun.copperization.network.ModNetworking;
import com.shouyun.copperization.registry.ModAttachments;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.PriorityQueue;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Authoritative, chunk-attached store for generic copperized positions. It is deliberately sparse:
 * no global world scan is performed and unloaded chunks do not receive oxidation work.
 */
public final class CopperizedBlockStorage {
	private static final int MAX_OXIDATION_SAMPLES_PER_LEVEL_TICK = 64;
	private static final Map<ServerLevel, PriorityQueue<ScheduledOxidation>> QUEUES = new IdentityHashMap<>();

	private CopperizedBlockStorage() {
	}

	public static void registerEvents() {
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk, newlyGenerated) -> scheduleLoadedChunk(level, chunk));
		ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
			// Stale queue entries are intentionally cheap and discarded when observed. This avoids
			// a global coordinate set and means an unloaded chunk has no active processing cost.
		});
		ServerTickEvents.END_LEVEL_TICK.register(CopperizedBlockStorage::tickOxidation);
	}

	public static CopperizedBlockData get(Level level, BlockPos pos) {
		LevelChunk chunk = level.getChunkAt(pos);
		return chunk.getAttachedOrElse(ModAttachments.COPPERIZED_BLOCKS, CopperizedChunkState.EMPTY).get(pos);
	}

	public static boolean isCopperized(Level level, BlockPos pos) {
		return get(level, pos) != null;
	}

	public static boolean copperize(ServerLevel level, BlockPos pos) {
		BlockState original = level.getBlockState(pos);
		if (!CopperizableBlockRegistry.supportsPositionCopperization(original) || isCopperized(level, pos)) return false;
		CopperizedBlockData data = CopperizedBlockData.fresh(level.getGameTime() + CopperizationConstants.OXIDATION_SAMPLE_INTERVAL);
		put(level.getChunkAt(pos), pos, data);
		schedule(level, pos, data.nextOxidationTick());
		notifyVisualChange(level, pos, original);
		return true;
	}

	/** Installs the stage carried by a positional copperized BlockItem after vanilla placement succeeds. */
	public static void place(ServerLevel level, BlockPos pos, CopperizedBlockData requested) {
		long nextTick = requested.waxed() || requested.oxidationStage() >= 3
			? Long.MAX_VALUE : level.getGameTime() + CopperizationConstants.OXIDATION_SAMPLE_INTERVAL;
		CopperizedBlockData data = new CopperizedBlockData(requested.oxidationStage(), requested.waxed(), nextTick);
		put(level.getChunkAt(pos), pos, data);
		if (nextTick != Long.MAX_VALUE) schedule(level, pos, nextTick);
		notifyVisualChange(level, pos, level.getBlockState(pos));
	}

	public static boolean restore(ServerLevel level, BlockPos pos) {
		LevelChunk chunk = level.getChunkAt(pos);
		CopperizedChunkState current = chunk.getAttachedOrElse(ModAttachments.COPPERIZED_BLOCKS, CopperizedChunkState.EMPTY);
		if (current.get(pos) == null) return false;
		chunk.setAttached(ModAttachments.COPPERIZED_BLOCKS, current.remove(pos));
		notifyVisualChange(level, pos, level.getBlockState(pos));
		return true;
	}

	public static boolean wax(ServerLevel level, BlockPos pos) {
		CopperizedBlockData data = get(level, pos);
		if (data == null || data.waxed()) return false;
		put(level.getChunkAt(pos), pos, data.withWaxed(true));
		notifyVisualChange(level, pos, level.getBlockState(pos));
		return true;
	}

	/** Scraping first removes wax, then reduces oxidation. */
	public static boolean scrape(ServerLevel level, BlockPos pos) {
		CopperizedBlockData data = get(level, pos);
		if (data == null) return false;
		CopperizedBlockData changed;
		if (data.waxed()) {
			changed = data.withWaxed(false);
		} else if (data.oxidationStage() > 0) {
			changed = data.withStage(data.oxidationStage() - 1, level.getGameTime() + CopperizationConstants.OXIDATION_SAMPLE_INTERVAL);
		} else {
			return false;
		}
		put(level.getChunkAt(pos), pos, changed);
		if (!changed.waxed() && changed.oxidationStage() < 3) schedule(level, pos, changed.nextOxidationTick());
		notifyVisualChange(level, pos, level.getBlockState(pos));
		return true;
	}

	/** Invoked by the chunk palette mixin when a generic copperized block is replaced. */
	public static void forget(LevelChunk chunk, BlockPos pos) {
		CopperizedChunkState current = chunk.getAttachedOrElse(ModAttachments.COPPERIZED_BLOCKS, CopperizedChunkState.EMPTY);
		if (current.get(pos) != null) {
			chunk.setAttached(ModAttachments.COPPERIZED_BLOCKS, current.remove(pos));
			if (chunk.getLevel() instanceof ServerLevel serverLevel) ModNetworking.syncCopperizedBlock(serverLevel, pos, null);
		}
	}

	/** Applies an authoritative server delta on the client and invalidates only this block's render data. */
	public static void applyClientSync(Level level, BlockPos pos, CopperizedBlockData data) {
		LevelChunk chunk = level.getChunkAt(pos);
		CopperizedChunkState current = chunk.getAttachedOrElse(ModAttachments.COPPERIZED_BLOCKS, CopperizedChunkState.EMPTY);
		chunk.setAttached(ModAttachments.COPPERIZED_BLOCKS, data == null ? current.remove(pos) : current.put(pos, data));
		BlockState state = level.getBlockState(pos);
		level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
	}

	private static void put(LevelChunk chunk, BlockPos pos, CopperizedBlockData data) {
		CopperizedChunkState current = chunk.getAttachedOrElse(ModAttachments.COPPERIZED_BLOCKS, CopperizedChunkState.EMPTY);
		chunk.setAttached(ModAttachments.COPPERIZED_BLOCKS, current.put(pos, data));
	}

	private static void scheduleLoadedChunk(ServerLevel level, LevelChunk chunk) {
		CopperizedChunkState state = chunk.getAttachedOrElse(ModAttachments.COPPERIZED_BLOCKS, CopperizedChunkState.EMPTY);
		for (Map.Entry<Integer, CopperizedBlockData> entry : state.entries().entrySet()) {
			CopperizedBlockData data = entry.getValue();
			if (!data.waxed() && data.oxidationStage() < 3) {
				schedule(level, CopperizedChunkState.unpack(entry.getKey(), chunk.getPos().x(), chunk.getPos().z()), data.nextOxidationTick());
			}
		}
	}

	private static void schedule(ServerLevel level, BlockPos pos, long tick) {
		QUEUES.computeIfAbsent(level, ignored -> new PriorityQueue<>()).add(new ScheduledOxidation(pos.immutable(), tick));
	}

	private static void tickOxidation(ServerLevel level) {
		PriorityQueue<ScheduledOxidation> queue = QUEUES.get(level);
		if (queue == null) return;
		long now = level.getGameTime();
		for (int processed = 0; processed < MAX_OXIDATION_SAMPLES_PER_LEVEL_TICK && !queue.isEmpty() && queue.peek().tick() <= now; processed++) {
			ScheduledOxidation scheduled = queue.poll();
			if (!level.hasChunkAt(scheduled.pos())) continue;
			CopperizedBlockData current = get(level, scheduled.pos());
			if (current == null || current.waxed() || current.oxidationStage() >= 3 || current.nextOxidationTick() != scheduled.tick()) continue;

			int nextStage = current.oxidationStage();
			float modifier = nextStage == 0 ? CopperizationConstants.FRESH_OXIDATION_MODIFIER : 1.0F;
			if (level.getRandom().nextFloat() < CopperizationConstants.OXIDATION_CHANCE * modifier) nextStage++;
			long nextTick = now + CopperizationConstants.OXIDATION_SAMPLE_INTERVAL;
			CopperizedBlockData changed = current.withStage(nextStage, nextTick);
			put(level.getChunkAt(scheduled.pos()), scheduled.pos(), changed);
			notifyVisualChange(level, scheduled.pos(), level.getBlockState(scheduled.pos()));
			if (nextStage < 3) schedule(level, scheduled.pos(), nextTick);
		}
	}

	private static void notifyVisualChange(ServerLevel level, BlockPos pos, BlockState state) {
		ModNetworking.syncCopperizedBlock(level, pos, get(level, pos));
		level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
	}

	private record ScheduledOxidation(BlockPos pos, long tick) implements Comparable<ScheduledOxidation> {
		@Override
		public int compareTo(ScheduledOxidation other) {
			return Long.compare(tick, other.tick);
		}
	}
}
