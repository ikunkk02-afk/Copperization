package com.shouyun.copperization.client.render;

import com.shouyun.copperization.block.CopperizableBlockRegistry;
import com.shouyun.copperization.Copperization;
import com.shouyun.copperization.item.PositionalCopperizedBlockItem;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.level.block.state.BlockState;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;

/** Registers once; Fabric rebuilds wrappers safely on every model/resource reload. */
public final class CopperizedBlockModelPlugin {
	private static final Map<BlockState, BlockStateModel> SOURCE_MODELS = new ConcurrentHashMap<>();
	private CopperizedBlockModelPlugin() {
	}

	public static void register() {
		ModelLoadingPlugin.register(context -> {
			context.modifyBlockModelAfterBake().register(ModelModifier.WRAP_LAST_PHASE, (model, modelContext) -> {
				BlockState state = modelContext.state();
				var physical = CopperizableBlockRegistry.mappingForCopperized(state);
				if (physical.isPresent()) {
					BlockState sourceState = physical.get().originalBlock().withPropertiesOf(state);
					return new CopperizedBlockStateModel(model, sourceState);
				}
				SOURCE_MODELS.put(state, model);
				return CopperizableBlockRegistry.supportsPositionCopperization(state)
					? new CopperizedBlockStateModel(model) : model;
			});
			context.modifyItemModelAfterBake().register(ModelModifier.WRAP_LAST_PHASE, (model, modelContext) -> {
				if (!Copperization.MOD_ID.equals(modelContext.itemId().getNamespace())) return model;
				var item = BuiltInRegistries.ITEM.getValue(modelContext.itemId());
				if (item instanceof PositionalCopperizedBlockItem positional) {
					return new CopperizedItemModel(model, positional.copperData());
				}
				if (item instanceof net.minecraft.world.item.BlockItem blockItem) {
					return CopperizableBlockRegistry.physicalCopperData(blockItem.getBlock().defaultBlockState())
						.<net.minecraft.client.renderer.item.ItemModel>map(data -> new CopperizedItemModel(model, data)).orElse(model);
				}
				return model;
			});
		});
	}

	static BlockStateModel sourceModel(BlockState state, BlockStateModel fallback) {
		return SOURCE_MODELS.getOrDefault(state, fallback);
	}
}
