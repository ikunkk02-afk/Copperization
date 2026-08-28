import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;

/** Deterministic original pixel-art generator for Copperization's MIT-licensed assets. */
public final class GenerateTextures {
	private static final List<String> FAMILIES = List.of(
		"stone", "cobblestone", "stone_bricks", "deepslate", "cobbled_deepslate", "deepslate_bricks",
		"bricks", "blackstone", "polished_blackstone", "end_stone", "nether_bricks"
	);
	private static final String[] PREFIXES = {"copperized_", "exposed_copperized_", "weathered_copperized_", "oxidized_copperized_"};
	private static final int[][] PALETTES = {
		{0x7D3E26, 0xA95632, 0xD47C45, 0xF0A064},
		{0x653A31, 0x865044, 0xB36B52, 0xD18A68},
		{0x4C5547, 0x61735A, 0x7D9472, 0xA98562},
		{0x235E55, 0x32796A, 0x4FA88C, 0x78C6A6}
	};

	public static void main(String[] args) throws IOException {
		Path root = Path.of(args.length == 0 ? "." : args[0]).toAbsolutePath().normalize();
		Path textureRoot = root.resolve("src/main/resources/assets/copperization/textures");
		Path blocks = textureRoot.resolve("block");
		Path items = textureRoot.resolve("item");
		Path entities = textureRoot.resolve("entity");
		Files.createDirectories(blocks);
		Files.createDirectories(items);
		Files.createDirectories(entities);
		for (int familyIndex = 0; familyIndex < FAMILIES.size(); familyIndex++) {
			for (int stage = 0; stage < 4; stage++) {
				writeBlock(blocks.resolve(PREFIXES[stage] + FAMILIES.get(familyIndex) + ".png"), familyIndex, stage);
			}
		}
		writeWand(items.resolve("copperization_wand.png"));
		writeStatue(items.resolve("copper_statue.png"));
		for (int i = 0; i < 4; i++) writeMask(entities.resolve("copper_mask_" + new int[]{25, 50, 75, 100}[i] + ".png"), i);
		writeIcon(root.resolve("src/main/resources/assets/copperization/icon.png"));
	}

	private static void writeBlock(Path path, int family, int stage) throws IOException {
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Random random = new Random(0xC0FFEE + family * 7919L + stage * 101L);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int pattern = pattern(family, x, y);
				int noise = random.nextInt(3) - 1;
				int palette = Math.max(0, Math.min(3, 2 + pattern + noise));
				int color = PALETTES[stage][palette];
				if (stage == 2 && ((x * 13 + y * 7 + family * 5) % 23 < 6)) color = PALETTES[1][Math.max(0, palette - 1)];
				image.setRGB(x, y, 0xFF000000 | color);
			}
		}
		ImageIO.write(image, "png", path.toFile());
	}

	private static int pattern(int family, int x, int y) {
		return switch (family) {
			case 0 -> ((x * 3 + y * 5) % 7 == 0) ? -1 : 0;
			case 1, 4 -> ((x + (y / 4) * 3) % 6 == 0 || y % 5 == 0) ? -2 : ((x * y) % 9 == 0 ? 1 : 0);
			case 2, 5, 6, 10 -> (y % 5 == 0 || (x + (y / 5 % 2) * 4) % 8 == 0) ? -2 : 0;
			case 3 -> ((x + y * 2) % 6 == 0) ? -1 : (x % 7 == 0 ? 1 : 0);
			case 7 -> ((x / 4 + y / 4) % 2 == 0) ? -1 : 0;
			case 8 -> (x % 8 == 0 || y % 8 == 0) ? -2 : ((x + y) % 6 == 0 ? 1 : 0);
			case 9 -> ((x * 7 + y * 11) % 17 < 3) ? 1 : 0;
			default -> 0;
		};
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
