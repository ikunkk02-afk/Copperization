import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;

/** Generates Copperization assets from Minecraft 26.2's vanilla block textures. */
public final class GenerateTextures {
	private static final double MIN_LUMA_CORRELATION = 0.90;
	private static final double MIN_ADJACENT_STAGE_DISTANCE = 12.0;
	private static final double[] LUMA_WEIGHTS = {0.2126, 0.7152, 0.0722};
	private static final double[] FRESH_COPPER = rgb(0xC06C50);
	private static final List<String> FAMILIES = List.of(
		"stone", "cobblestone", "stone_bricks", "deepslate", "cobbled_deepslate", "deepslate_bricks",
		"bricks", "blackstone", "polished_blackstone", "end_stone", "nether_bricks"
	);
	private static final List<CopperVisualProfile> PROFILES = List.of(
		new CopperVisualProfile("copperized_", 0xC06C50, 0.00, 0.62, 1.08, 1.04),
		new CopperVisualProfile("exposed_copperized_", 0xA17E68, 0.18, 0.60, 0.88, 1.08),
		new CopperVisualProfile("weathered_copperized_", 0x6C996E, 0.30, 0.56, 0.92, 1.03),
		new CopperVisualProfile("oxidized_copperized_", 0x52A385, 0.10, 0.54, 1.02, 1.06)
	);

	public static void main(String[] args) throws IOException {
		Options options = Options.parse(args);
		Path root = options.root();
		Path textureRoot = root.resolve("src/main/resources/assets/copperization/textures");
		Path blocks = textureRoot.resolve("block");
		Path items = textureRoot.resolve("item");
		Files.createDirectories(blocks);
		Files.createDirectories(items);

		try (ZipFile minecraft = new ZipFile(options.minecraftJar().toFile())) {
			if (!options.verifyOnly()) {
				for (String family : FAMILIES) {
					BufferedImage source = readVanillaBlock(minecraft, family);
					for (CopperVisualProfile profile : PROFILES) {
						writeBlock(blocks.resolve(profile.prefix() + family + ".png"), source, profile);
					}
				}
				writeRestorationWand(items.resolve("restoration_wand.png"));
			}
			verifyGeneratedTextures(minecraft, blocks);
		}
		System.out.println((options.verifyOnly() ? "Verified" : "Generated and verified")
			+ " 44 vanilla-derived copperized block textures.");
	}

	private static BufferedImage readVanillaBlock(ZipFile minecraft, String family) throws IOException {
		String path = "assets/minecraft/textures/block/" + family + ".png";
		ZipEntry entry = minecraft.getEntry(path);
		if (entry == null) throw new IOException("Missing vanilla texture in Minecraft jar: " + path);
		try (InputStream input = minecraft.getInputStream(entry)) {
			BufferedImage image = ImageIO.read(input);
			if (image == null) throw new IOException("Could not decode vanilla texture: " + path);
			if (image.getWidth() != 16 || image.getHeight() != 16) {
				throw new IOException("Expected 16x16 vanilla texture, got " + image.getWidth() + "x" + image.getHeight() + ": " + path);
			}
			return image;
		}
	}

	private static void writeBlock(Path path, BufferedImage source, CopperVisualProfile profile) throws IOException {
		BufferedImage output = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < source.getHeight(); y++) {
			for (int x = 0; x < source.getWidth(); x++) {
				Color original = new Color(source.getRGB(x, y), true);
				double[] base = {original.getRed() / 255.0, original.getGreen() / 255.0, original.getBlue() / 255.0};
				double luminance = luminance(base);
				double localCopper = profile.copperUndertone() * (0.35 + 0.65 * (1.0 - luminance));
				double[] copperBase = mix(profile.stageColor(), FRESH_COPPER, localCopper);
				double copperLuminance = Math.max(luminance(copperBase), 0.001);
				double[] multiplied = new double[3];
				for (int channel = 0; channel < 3; channel++) {
					double neutralTint = copperBase[channel] / copperLuminance;
					multiplied[channel] = base[channel] * mix(1.0, neutralTint, 0.72);
				}
				double multipliedLuminance = Math.max(luminance(multiplied), 0.025);
				double detailCorrection = mix(1.0, luminance / multipliedLuminance, 0.72);
				double[] converted = new double[3];
				for (int channel = 0; channel < 3; channel++) {
					double multipliedDetail = multiplied[channel] * detailCorrection;
					double tonalCopper = copperBase[channel] * (0.36 + luminance * 1.08)
						+ new double[]{0.055, 0.022, 0.006}[channel] * Math.pow(luminance, 3.0);
					converted[channel] = mix(tonalCopper, multipliedDetail, profile.textureRetention());
				}
				double convertedLuminance = luminance(converted);
				for (int channel = 0; channel < 3; channel++) {
					converted[channel] = clamp(mix(convertedLuminance, converted[channel], profile.saturation()) * profile.valueScale());
				}
				output.setRGB(x, y, new Color(toByte(converted[0]), toByte(converted[1]), toByte(converted[2]), original.getAlpha()).getRGB());
			}
		}
		ImageIO.write(output, "png", path.toFile());
	}

	private static void verifyGeneratedTextures(ZipFile minecraft, Path blocks) throws IOException {
		List<String> failures = new ArrayList<>();
		double lowestCorrelation = 1.0;
		double lowestStageDistance = Double.MAX_VALUE;
		for (String family : FAMILIES) {
			BufferedImage source = readVanillaBlock(minecraft, family);
			List<double[]> stageMeans = new ArrayList<>();
			for (CopperVisualProfile profile : PROFILES) {
				Path outputPath = blocks.resolve(profile.prefix() + family + ".png");
				BufferedImage output = ImageIO.read(outputPath.toFile());
				if (output == null || output.getWidth() != source.getWidth() || output.getHeight() != source.getHeight()) {
					failures.add(outputPath.getFileName() + " does not match the vanilla 16x16 dimensions");
					continue;
				}
				for (int y = 0; y < source.getHeight(); y++) for (int x = 0; x < source.getWidth(); x++) {
					if ((source.getRGB(x, y) >>> 24) != (output.getRGB(x, y) >>> 24)) {
						failures.add(outputPath.getFileName() + " changed source alpha at " + x + "," + y);
						y = source.getHeight();
						break;
					}
				}
				double correlation = lumaCorrelation(source, output);
				lowestCorrelation = Math.min(lowestCorrelation, correlation);
				if (correlation < MIN_LUMA_CORRELATION) {
					failures.add(outputPath.getFileName() + " luma correlation " + String.format("%.3f", correlation)
						+ " is below " + MIN_LUMA_CORRELATION);
				}
				stageMeans.add(meanRgb(output));
			}
			for (int stage = 1; stage < stageMeans.size(); stage++) {
				double distance = distance(stageMeans.get(stage - 1), stageMeans.get(stage));
				lowestStageDistance = Math.min(lowestStageDistance, distance);
				if (distance < MIN_ADJACENT_STAGE_DISTANCE) {
					failures.add(family + " stage " + (stage - 1) + "->" + stage + " mean RGB distance "
						+ String.format("%.2f", distance) + " is below " + MIN_ADJACENT_STAGE_DISTANCE);
				}
			}
		}
		if (!failures.isEmpty()) throw new IOException("Texture verification failed:\n - " + String.join("\n - ", failures));
		System.out.printf("Texture checks: lowest luma correlation %.3f, lowest adjacent stage distance %.2f%n",
			lowestCorrelation, lowestStageDistance);
	}

	private static double lumaCorrelation(BufferedImage first, BufferedImage second) {
		double[] firstValues = new double[first.getWidth() * first.getHeight()];
		double[] secondValues = new double[firstValues.length];
		double firstMean = 0.0;
		double secondMean = 0.0;
		int index = 0;
		for (int y = 0; y < first.getHeight(); y++) for (int x = 0; x < first.getWidth(); x++) {
			firstValues[index] = luminance(first.getRGB(x, y));
			secondValues[index] = luminance(second.getRGB(x, y));
			firstMean += firstValues[index];
			secondMean += secondValues[index];
			index++;
		}
		firstMean /= firstValues.length;
		secondMean /= secondValues.length;
		double numerator = 0.0;
		double firstVariance = 0.0;
		double secondVariance = 0.0;
		for (int i = 0; i < firstValues.length; i++) {
			double firstDelta = firstValues[i] - firstMean;
			double secondDelta = secondValues[i] - secondMean;
			numerator += firstDelta * secondDelta;
			firstVariance += firstDelta * firstDelta;
			secondVariance += secondDelta * secondDelta;
		}
		return numerator / Math.sqrt(firstVariance * secondVariance);
	}

	private static double[] meanRgb(BufferedImage image) {
		double[] mean = new double[3];
		int count = image.getWidth() * image.getHeight();
		for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
			int argb = image.getRGB(x, y);
			mean[0] += argb >> 16 & 0xFF;
			mean[1] += argb >> 8 & 0xFF;
			mean[2] += argb & 0xFF;
		}
		for (int channel = 0; channel < 3; channel++) mean[channel] /= count;
		return mean;
	}

	private static double distance(double[] first, double[] second) {
		double sum = 0.0;
		for (int channel = 0; channel < 3; channel++) sum += Math.pow(first[channel] - second[channel], 2.0);
		return Math.sqrt(sum);
	}

	private static double luminance(int argb) {
		return luminance(new double[]{(argb >> 16 & 0xFF) / 255.0, (argb >> 8 & 0xFF) / 255.0, (argb & 0xFF) / 255.0});
	}

	private static double luminance(double[] color) {
		return color[0] * LUMA_WEIGHTS[0] + color[1] * LUMA_WEIGHTS[1] + color[2] * LUMA_WEIGHTS[2];
	}

	private static double[] rgb(int color) {
		return new double[]{(color >> 16 & 0xFF) / 255.0, (color >> 8 & 0xFF) / 255.0, (color & 0xFF) / 255.0};
	}

	private static double[] mix(double[] first, double[] second, double amount) {
		return new double[]{mix(first[0], second[0], amount), mix(first[1], second[1], amount), mix(first[2], second[2], amount)};
	}

	private static double mix(double first, double second, double amount) {
		return first + (second - first) * amount;
	}

	private static double clamp(double value) {
		return Math.max(0.0, Math.min(1.0, value));
	}

	private static int toByte(double value) {
		return (int)Math.round(clamp(value) * 255.0);
	}

	private record CopperVisualProfile(
		String prefix,
		double[] stageColor,
		double copperUndertone,
		double textureRetention,
		double saturation,
		double valueScale
	) {
		private CopperVisualProfile(String prefix, int stageColor, double copperUndertone, double textureRetention, double saturation, double valueScale) {
			this(prefix, rgb(stageColor), copperUndertone, textureRetention, saturation, valueScale);
		}
	}

	private record Options(Path root, Path minecraftJar, boolean verifyOnly) {
		private static Options parse(String[] args) {
			Path root = Path.of(".").toAbsolutePath().normalize();
			Path minecraftJar = null;
			boolean verifyOnly = false;
			for (int i = 0; i < args.length; i++) {
				switch (args[i]) {
					case "--root" -> root = Path.of(requireValue(args, ++i, "--root")).toAbsolutePath().normalize();
					case "--minecraft-jar" -> minecraftJar = Path.of(requireValue(args, ++i, "--minecraft-jar")).toAbsolutePath().normalize();
					case "--verify-only" -> verifyOnly = true;
					default -> throw usage("Unknown option: " + args[i]);
				}
			}
			if (minecraftJar == null) throw usage("--minecraft-jar is required");
			if (!Files.isRegularFile(minecraftJar)) throw usage("Minecraft client jar does not exist: " + minecraftJar);
			return new Options(root, minecraftJar, verifyOnly);
		}

		private static String requireValue(String[] args, int index, String option) {
			if (index >= args.length) throw usage("Missing value for " + option);
			return args[index];
		}

		private static IllegalArgumentException usage(String message) {
			return new IllegalArgumentException(message
				+ "\nUsage: java tools/GenerateTextures.java --minecraft-jar <minecraft-client.jar> [--root <repo>] [--verify-only]");
		}
	}

	private static void writeWand(Path path) throws IOException {
		BufferedImage image = transparent(16);
		for (int i = 2; i < 13; i++) {
			set(image, i, 15 - i, 0xFF7D3E26);
			if (i % 2 == 0) set(image, i, 14 - i, 0xFFD47C45);
		}
		int[][] crystal = {{11,2},{12,1},{13,2},{14,3},{13,4},{12,5},{11,4},{10,3}};
		for (int[] p : crystal) set(image, p[0], p[1], 0xFF9B6BCE);
		set(image, 12, 2, 0xFFE7C9FF); set(image, 13, 3, 0xFFC79BEA);
		set(image, 2, 13, 0xFF9B5530); set(image, 1, 14, 0xFFD47C45); set(image, 3, 14, 0xFF5F2E20);
		ImageIO.write(image, "png", path.toFile());
	}

	/** A deliberately distinct patina-and-amethyst counterpart to the warm copperization wand. */
	private static void writeRestorationWand(Path path) throws IOException {
		BufferedImage image = transparent(16);
		for (int i = 2; i < 13; i++) {
			set(image, i, 15 - i, 0xFF2E756E);
			if (i % 2 == 0) set(image, i, 14 - i, 0xFF73B9A2);
		}
		int[][] crystal = {{11,2},{12,1},{13,2},{14,3},{13,4},{12,5},{11,4},{10,3}};
		for (int[] p : crystal) set(image, p[0], p[1], 0xFF9B6BCE);
		set(image, 12, 2, 0xFFF0D9FF); set(image, 13, 3, 0xFFCDA5EE);
		set(image, 2, 13, 0xFF356E66); set(image, 1, 14, 0xFF8ED6C3); set(image, 3, 14, 0xFF17443F);
		ImageIO.write(image, "png", path.toFile());
	}

	private static void writeStatue(Path path) throws IOException {
		BufferedImage image = transparent(16);
		for (int x = 2; x <= 13; x++) { set(image, x, 14, 0xFF653A31); set(image, x, 13, 0xFFD47C45); }
		for (int y = 5; y <= 12; y++) for (int x = 5; x <= 10; x++) set(image, x, y, ((x + y) % 4 == 0) ? 0xFF72977B : 0xFFB86E53);
		for (int y = 2; y <= 5; y++) for (int x = 6; x <= 9; x++) set(image, x, y, 0xFFD47C45);
		set(image, 6, 1, 0xFF7D3E26); set(image, 9, 1, 0xFF7D3E26); set(image, 7, 3, 0xFF35251F); set(image, 9, 3, 0xFF35251F);
		set(image, 5, 12, 0xFF7D3E26); set(image, 10, 12, 0xFF7D3E26);
		ImageIO.write(image, "png", path.toFile());
	}

	private static void writeMask(Path path, int stage) throws IOException {
		BufferedImage image = transparent(64);
		for (int y = 0; y < 64; y++) {
			for (int x = 0; x < 64; x++) {
				int noise = Math.floorMod(x * 37 + y * 17 + x * y * 3 + (x >> 2) * 11, 29) - 14;
				int boundary = switch (stage) { case 0 -> 49; case 1 -> 35; case 2 -> 20; default -> -20; };
				boolean covered = y + noise / 2 >= boundary || (stage > 0 && Math.floorMod(x * 11 + y * 5, 41) < stage * 2);
				if (covered) image.setRGB(x, y, 0xFFFFFFFF);
			}
		}
		ImageIO.write(image, "png", path.toFile());
	}

	private static void writeIcon(Path path) throws IOException {
		BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) {
			int d = Math.abs(x - 31) + Math.abs(y - 31);
			int bg = d < 39 ? 0xFF173C37 : 0xFF101918;
			image.setRGB(x, y, bg);
		}
		for (int y = 13; y <= 50; y++) for (int x = 13; x <= 50; x++) {
			boolean edge = x < 17 || x > 46 || y < 17 || y > 46;
			int noise = Math.floorMod(x * 19 + y * 31, 13);
			int color = edge ? 0xFF7D3E26 : (noise < 3 ? 0xFF4FA88C : noise < 7 ? 0xFFD47C45 : 0xFFA95632);
			image.setRGB(x, y, color);
		}
		for (int i = 20; i < 44; i++) { image.setRGB(i, 63 - i, 0xFFFFC477); image.setRGB(i, 62 - i, 0xFF653A31); }
		ImageIO.write(image, "png", path.toFile());
	}

	private static BufferedImage transparent(int size) { return new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB); }
	private static void set(BufferedImage image, int x, int y, int color) { if (x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight()) image.setRGB(x, y, color); }
}
