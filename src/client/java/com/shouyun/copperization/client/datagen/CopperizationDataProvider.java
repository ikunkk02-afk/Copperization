package com.shouyun.copperization.client.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.shouyun.copperization.Copperization;
import com.shouyun.copperization.block.CopperizableBlockClassifier;
import com.shouyun.copperization.block.CopperizedBlockFamily;
import com.shouyun.copperization.registry.ModBlocks;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class CopperizationDataProvider implements DataProvider {
	private static final String[] STAGE_PREFIXES = {"", "exposed_", "weathered_", "oxidized_"};
	private static final String[] STAGE_EN = {"", "Exposed ", "Weathered ", "Oxidized "};
	private static final String[] STAGE_ZH = {"", "斑驳的", "锈蚀的", "氧化的"};
	private final Path root;

	public CopperizationDataProvider(PackOutput output) {
		this.root = output.getOutputFolder();
	}

	@Override
	public CompletableFuture<?> run(CachedOutput cache) {
		List<CompletableFuture<?>> writes = new ArrayList<>();
		Map<String, String> en = new LinkedHashMap<>();
		Map<String, String> zh = new LinkedHashMap<>();

		writeCommonData(cache, writes);
		writeItemAssets(cache, writes);
		for (CopperizedBlockFamily family : ModBlocks.families()) {
			writeFamily(cache, writes, family, en, zh);
		}
		addTranslations(en, zh);
		writes.add(save(cache, language(en), asset("lang/en_us.json")));
		writes.add(save(cache, language(zh), asset("lang/zh_cn.json")));
		return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
	}

	private void writeCommonData(CachedOutput cache, List<CompletableFuture<?>> writes) {
		JsonObject enchantment = new JsonObject();
		enchantment.addProperty("anvil_cost", 2);
		JsonObject description = new JsonObject();
		description.addProperty("translate", "enchantment.copperization.copperization");
		enchantment.add("description", description);
		enchantment.add("effects", new JsonObject());
		enchantment.addProperty("max_level", 3);
		enchantment.add("min_cost", cost(8, 10));
		enchantment.add("max_cost", cost(28, 10));
		enchantment.addProperty("primary_items", "#copperization:copperization_enchantable");
		enchantment.addProperty("supported_items", "#copperization:copperization_enchantable");
		JsonArray slots = new JsonArray();
		slots.add("mainhand");
		enchantment.add("slots", slots);
		enchantment.addProperty("weight", 5);
		writes.add(save(cache, enchantment, data("enchantment/copperization.json")));

		writes.add(save(cache, tag(false, "minecraft:copper_sword"), data("tags/item/copperization_enchantable.json")));
		JsonObject blockTag = new JsonObject();
		blockTag.addProperty("replace", false);
		JsonArray sources = new JsonArray();
		for (CopperizedBlockFamily family : ModBlocks.families()) sources.add(BuiltInRegistries.BLOCK.getKey(family.source()).toString());
		blockTag.add("values", sources);
		writes.add(save(cache, blockTag, data("tags/block/copperizable_blocks.json")));
		JsonObject uncopperizable = new JsonObject();
		uncopperizable.addProperty("replace", false);
		JsonArray excluded = new JsonArray();
		for (String path : List.of("air", "cave_air", "void_air", "water", "lava", "fire", "soul_fire", "nether_portal", "end_portal", "end_gateway", "moving_piston", "piston_head", "piston", "sticky_piston", "command_block", "chain_command_block", "repeating_command_block", "structure_block", "structure_void", "jigsaw", "barrier", "light", "spawner", "trial_spawner", "vault")) {
			excluded.add("minecraft:" + path);
		}
		uncopperizable.add("values", excluded);
		writes.add(save(cache, uncopperizable, data("tags/block/uncopperizable.json")));
		writes.add(save(cache, tag(false, "copperization:copperization"), root.resolve("data/minecraft/tags/enchantment/non_treasure.json")));
		writes.add(save(cache, tag(false, "copperization:copperization"), root.resolve("data/minecraft/tags/enchantment/tradeable.json")));
		JsonObject pickaxeTag = new JsonObject();
		pickaxeTag.addProperty("replace", false);
		JsonArray pickaxeValues = new JsonArray();
		for (CopperizedBlockFamily family : ModBlocks.families()) {
			family.blocks().forEach(block -> pickaxeValues.add(BuiltInRegistries.BLOCK.getKey(block).toString()));
		}
		pickaxeTag.add("values", pickaxeValues);
		writes.add(save(cache, pickaxeTag, root.resolve("data/minecraft/tags/block/mineable/pickaxe.json")));

		JsonObject recipe = new JsonObject();
		recipe.addProperty("type", "minecraft:crafting_shaped");
		recipe.addProperty("category", "equipment");
		JsonObject key = new JsonObject();
		key.addProperty("C", "minecraft:copper_ingot");
		key.addProperty("A", "minecraft:amethyst_shard");
		key.addProperty("B", "minecraft:blaze_rod");
		recipe.add("key", key);
		JsonArray pattern = new JsonArray();
		pattern.add("C"); pattern.add("A"); pattern.add("B");
		recipe.add("pattern", pattern);
		JsonObject result = new JsonObject();
		result.addProperty("id", "copperization:copperization_wand");
		recipe.add("result", result);
		writes.add(save(cache, recipe, data("recipe/copperization_wand.json")));

		JsonObject restorationRecipe = recipe.deepCopy();
		JsonArray restorationPattern = new JsonArray();
		restorationPattern.add("B"); restorationPattern.add("A"); restorationPattern.add("C");
		restorationRecipe.add("pattern", restorationPattern);
		restorationRecipe.getAsJsonObject("result").addProperty("id", "copperization:restoration_wand");
		writes.add(save(cache, restorationRecipe, data("recipe/restoration_wand.json")));
		writes.add(save(cache, coverageReport(), root.resolve("reports/copperization-coverage.json")));
	}

	private void writeItemAssets(CachedOutput cache, List<CompletableFuture<?>> writes) {
		writes.add(save(cache, itemDefinition("copperization:item/copperization_wand"), asset("items/copperization_wand.json")));
		writes.add(save(cache, itemDefinition("copperization:item/restoration_wand"), asset("items/restoration_wand.json")));
		writes.add(save(cache, itemDefinition("copperization:item/copper_statue"), asset("items/copper_statue.json")));
		writes.add(save(cache, flatItemModel("minecraft:item/handheld", "copperization:item/copperization_wand"), asset("models/item/copperization_wand.json")));
		writes.add(save(cache, flatItemModel("minecraft:item/handheld", "copperization:item/restoration_wand"), asset("models/item/restoration_wand.json")));
		writes.add(save(cache, flatItemModel("minecraft:item/generated", "copperization:item/copper_statue"), asset("models/item/copper_statue.json")));
	}

	private void writeFamily(CachedOutput cache, List<CompletableFuture<?>> writes, CopperizedBlockFamily family, Map<String, String> en, Map<String, String> zh) {
		List<Block> blocks = family.blocks().asList();
		List<Item> items = family.items().asList();
		for (int i = 0; i < blocks.size(); i++) {
			Block block = blocks.get(i);
			Identifier id = BuiltInRegistries.BLOCK.getKey(block);
			String name = id.getPath();
			String textureName = name.startsWith("waxed_") ? name.substring("waxed_".length()) : name;
			writes.add(save(cache, blockState(name), asset("blockstates/" + name + ".json")));
			writes.add(save(cache, blockModel(textureName), asset("models/block/" + name + ".json")));
			writes.add(save(cache, itemDefinition("copperization:block/" + name), asset("items/" + name + ".json")));
			writes.add(save(cache, selfDrop(id.toString()), data("loot_table/blocks/" + name + ".json")));
			int stage = i % 4;
			boolean waxed = i >= 4;
			String baseEn = displayName(family.name());
			String baseZh = chineseName(family.name());
			en.put("block.copperization." + name, (waxed ? "Waxed " : "") + STAGE_EN[stage] + "Copperized " + baseEn);
			zh.put("block.copperization." + name, (waxed ? "上蜡的" : "") + STAGE_ZH[stage] + "铜化" + baseZh);
		}
	}

	private static void addTranslations(Map<String, String> en, Map<String, String> zh) {
		en.put("item.copperization.copperization_wand", "Copperization Wand");
		en.put("item.copperization.restoration_wand", "Restoration Wand");
		en.put("item.copperization.copper_statue", "Copper Statue");
		en.put("item.copperization.copper_statue.named", "%s Copper Statue");
		en.put("itemGroup.copperization.copperization", "Copperization");
		en.put("enchantment.copperization.copperization", "Copperization");
		en.put("tooltip.copperization.oxidation", "Oxidation: %s");
		en.put("tooltip.copperization.waxed", "Waxed");
		en.put("tooltip.copperization.unwaxed", "Unwaxed");
		en.put("copperization.oxidation.unaffected", "Fresh");
		en.put("copperization.oxidation.exposed", "Exposed");
		en.put("copperization.oxidation.weathered", "Weathered");
		en.put("copperization.oxidation.oxidized", "Oxidized");
		en.put("tag.item.copperization.copperization_enchantable", "Copperization Enchantable Items");
		en.put("tag.block.copperization.copperizable_blocks", "Copperizable Blocks");
		en.put("tag.block.copperization.uncopperizable", "Uncopperizable Blocks");

		zh.put("item.copperization.copperization_wand", "铜化法杖");
		zh.put("item.copperization.restoration_wand", "复原法杖");
		zh.put("item.copperization.copper_statue", "铜雕像");
		zh.put("item.copperization.copper_statue.named", "%s铜雕像");
		zh.put("itemGroup.copperization.copperization", "铜化");
		zh.put("enchantment.copperization.copperization", "铜化");
		zh.put("tooltip.copperization.oxidation", "氧化阶段：%s");
		zh.put("tooltip.copperization.waxed", "已上蜡");
		zh.put("tooltip.copperization.unwaxed", "未上蜡");
		zh.put("copperization.oxidation.unaffected", "新鲜");
		zh.put("copperization.oxidation.exposed", "斑驳");
		zh.put("copperization.oxidation.weathered", "锈蚀");
		zh.put("copperization.oxidation.oxidized", "氧化");
		zh.put("tag.item.copperization.copperization_enchantable", "可铜化附魔物品");
		zh.put("tag.block.copperization.copperizable_blocks", "可铜化方块");
		zh.put("tag.block.copperization.uncopperizable", "不可铜化方块");
	}

	/** Machine-readable final accounting, based on the same classifier used at runtime. */
	private static JsonObject coverageReport() {
		Map<CopperizableBlockClassifier.BlockCategory, int[]> categories = new LinkedHashMap<>();
		for (CopperizableBlockClassifier.BlockCategory category : CopperizableBlockClassifier.BlockCategory.values()) categories.put(category, new int[2]);
		int total = 0;
		int supported = 0;
		for (Block block : BuiltInRegistries.BLOCK) {
			Identifier id = BuiltInRegistries.BLOCK.getKey(block);
			if (id == null || !"minecraft".equals(id.getNamespace())) continue;
			var state = block.defaultBlockState();
			boolean accepted = CopperizableBlockClassifier.supports(state);
			CopperizableBlockClassifier.BlockCategory category = CopperizableBlockClassifier.category(state);
			int[] values = categories.get(category);
			values[0]++;
			if (accepted) values[1]++;
			total++;
			if (accepted) supported++;
		}
		JsonObject report = new JsonObject();
		report.addProperty("vanilla_blocks_total", total);
		report.addProperty("supported", supported);
		report.addProperty("unsupported", total - supported);
		report.addProperty("coverage_percent", total == 0 ? 0.0D : Math.round(supported * 10000.0D / total) / 100.0D);
		JsonObject categoryReport = new JsonObject();
		categories.forEach((category, values) -> {
			JsonObject row = new JsonObject();
			row.addProperty("total", values[0]);
			row.addProperty("supported", values[1]);
			row.addProperty("unsupported", values[0] - values[1]);
			categoryReport.add(category.displayName(), row);
		});
		report.add("categories", categoryReport);
		return report;
	}

	private static JsonObject cost(int base, int perLevel) {
		JsonObject value = new JsonObject(); value.addProperty("base", base); value.addProperty("per_level_above_first", perLevel); return value;
	}

	private static JsonObject tag(boolean replace, String value) {
		JsonObject tag = new JsonObject(); tag.addProperty("replace", replace); JsonArray values = new JsonArray(); values.add(value); tag.add("values", values); return tag;
	}

	private static JsonObject itemDefinition(String model) {
		JsonObject root = new JsonObject(); JsonObject inner = new JsonObject(); inner.addProperty("type", "minecraft:model"); inner.addProperty("model", model); root.add("model", inner); return root;
	}

	private static JsonObject flatItemModel(String parent, String texture) {
		JsonObject model = new JsonObject(); model.addProperty("parent", parent); JsonObject textures = new JsonObject(); textures.addProperty("layer0", texture); model.add("textures", textures); return model;
	}

	private static JsonObject blockState(String name) {
		JsonObject root = new JsonObject(); JsonObject variants = new JsonObject(); JsonObject variant = new JsonObject(); variant.addProperty("model", "copperization:block/" + name); variants.add("", variant); root.add("variants", variants); return root;
	}

	private static JsonObject blockModel(String texture) {
		JsonObject model = new JsonObject(); model.addProperty("parent", "minecraft:block/cube_all"); JsonObject textures = new JsonObject(); textures.addProperty("all", "copperization:block/" + texture); model.add("textures", textures); return model;
	}

	private static JsonObject selfDrop(String id) {
		JsonObject root = new JsonObject(); root.addProperty("type", "minecraft:block"); JsonArray pools = new JsonArray(); JsonObject pool = new JsonObject(); pool.addProperty("rolls", 1.0); JsonArray entries = new JsonArray(); JsonObject entry = new JsonObject(); entry.addProperty("type", "minecraft:item"); entry.addProperty("name", id); entries.add(entry); pool.add("entries", entries); JsonArray conditions = new JsonArray(); JsonObject condition = new JsonObject(); condition.addProperty("condition", "minecraft:survives_explosion"); conditions.add(condition); pool.add("conditions", conditions); pools.add(pool); root.add("pools", pools); return root;
	}

	private static JsonObject language(Map<String, String> translations) {
		JsonObject json = new JsonObject(); translations.forEach(json::addProperty); return json;
	}

	private static String displayName(String value) {
		StringBuilder result = new StringBuilder(); for (String part : value.split("_")) { if (!result.isEmpty()) result.append(' '); result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)); } return result.toString();
	}

	private static String chineseName(String value) {
		return switch (value) {
			case "stone" -> "石头"; case "cobblestone" -> "圆石"; case "stone_bricks" -> "石砖"; case "deepslate" -> "深板岩";
			case "cobbled_deepslate" -> "深板岩圆石"; case "deepslate_bricks" -> "深板岩砖"; case "bricks" -> "红砖";
			case "blackstone" -> "黑石"; case "polished_blackstone" -> "磨制黑石"; case "end_stone" -> "末地石"; case "nether_bricks" -> "下界砖";
			default -> value;
		};
	}

	private Path asset(String path) { return root.resolve("assets/copperization").resolve(path); }
	private Path data(String path) { return root.resolve("data/copperization").resolve(path); }
	private static CompletableFuture<?> save(CachedOutput cache, JsonObject json, Path path) { return DataProvider.saveStable(cache, json, path); }

	@Override
	public String getName() {
		return "Copperization resources and data";
	}
}
