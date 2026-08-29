package com.shouyun.copperization.block;

import com.shouyun.copperization.Copperization;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Conservative automatic eligibility policy used by the positional copperization path. */
public final class CopperizableBlockClassifier {
	public static final TagKey<Block> UNCOPPERIZABLE = TagKey.create(Registries.BLOCK, Copperization.id("uncopperizable"));
	private static final Set<String> TECHNICAL_PATHS = Set.of(
		"air", "cave_air", "void_air", "water", "lava", "fire", "soul_fire",
		"nether_portal", "end_portal", "end_gateway", "moving_piston", "piston_head", "piston", "sticky_piston",
		"command_block", "chain_command_block", "repeating_command_block", "structure_block", "structure_void", "jigsaw",
		"barrier", "light", "spawner", "trial_spawner", "vault"
	);

	private CopperizableBlockClassifier() {
	}

	public static boolean supports(BlockState state) {
		Block block = state.getBlock();
		if (state.isAir() || block instanceof AirBlock || block instanceof LiquidBlock || state.hasBlockEntity() || state.is(UNCOPPERIZABLE)) {
			return false;
		}
		var id = BuiltInRegistries.BLOCK.getKey(block);
		return id == null || !"minecraft".equals(id.getNamespace()) || !TECHNICAL_PATHS.contains(id.getPath());
	}

	/** Copperized vegetation is metalized: random growth and bone-meal growth are both disabled. */
	public static boolean freezesGrowth(BlockState state) {
		if (state.getBlock() instanceof BonemealableBlock || state.getBlock() instanceof LeavesBlock) return true;
		String name = state.getBlock().getClass().getSimpleName();
		return name.contains("Vine") || name.contains("Kelp") || name.contains("SugarCane")
			|| name.contains("Cactus") || name.contains("Bamboo") || name.contains("Mushroom") || name.contains("Fungus");
	}

	public static BlockCategory category(BlockState state) {
		Block block = state.getBlock();
		if (state.hasBlockEntity()) return BlockCategory.BLOCK_ENTITIES;
		if (!supports(state)) return BlockCategory.TECHNICAL;
		String name = block.getClass().getSimpleName();
		if (name.contains("Stair")) return BlockCategory.STAIRS;
		if (name.contains("Slab")) return BlockCategory.SLABS;
		if (name.contains("Wall")) return BlockCategory.WALLS;
		if (name.contains("FenceGate")) return BlockCategory.FENCE_GATES;
		if (name.contains("Fence")) return BlockCategory.FENCES;
		if (name.contains("TrapDoor")) return BlockCategory.TRAPDOORS;
		if (name.contains("Door")) return BlockCategory.DOORS;
		if (block instanceof LeavesBlock) return BlockCategory.LEAVES;
		if (name.contains("Crop") || name.contains("Stem") || name.contains("NetherWart")) return BlockCategory.CROPS;
		if (freezesGrowth(state) || name.contains("Bush") || name.contains("Grass") || name.contains("Flower")) return BlockCategory.PLANTS;
		if (name.contains("RedStone") || name.contains("Diode") || name.contains("Observer") || name.contains("Lever")
			|| name.contains("Button") || name.contains("PressurePlate") || name.contains("Target") || name.contains("Note")) {
			return BlockCategory.REDSTONE;
		}
		return BlockCategory.FULL_BLOCKS;
	}

	public enum BlockCategory {
		FULL_BLOCKS("Full Blocks"), STAIRS("Stairs"), SLABS("Slabs"), WALLS("Walls"), FENCES("Fences"),
		FENCE_GATES("Fence Gates"), DOORS("Doors"), TRAPDOORS("Trapdoors"), PLANTS("Plants"),
		LEAVES("Leaves"), CROPS("Crops"), BLOCK_ENTITIES("BlockEntities"), REDSTONE("Redstone"), TECHNICAL("Technical Blocks");

		private final String displayName;

		BlockCategory(String displayName) {
			this.displayName = displayName;
		}

		public String displayName() {
			return displayName;
		}
	}
}
