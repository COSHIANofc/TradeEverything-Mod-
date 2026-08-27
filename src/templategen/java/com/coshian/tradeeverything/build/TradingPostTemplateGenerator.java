package com.coshian.tradeeverything.build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;

/** Reproducible source for the vanilla-only Trading Post structure template. */
public final class TradingPostTemplateGenerator {
	private static final int SIZE = 35;
	private static final Map<String, Integer> PALETTE = new LinkedHashMap<>();
	private static final ListTag BLOCKS = new ListTag();

	private TradingPostTemplateGenerator() {}

	public static void main(String[] args) throws IOException {
		if (args.length != 1) throw new IllegalArgumentException("Expected output path");
		buildFloor();
		buildMarket();
		CompoundTag root = new CompoundTag();
		root.put("size", ints(SIZE, 9, SIZE));
		root.put("palette", palette());
		root.put("blocks", BLOCKS);
		root.put("entities", markers());
		Path output = Path.of(args[0]);
		Files.createDirectories(output.getParent());
		NbtIo.writeCompressed(root, output);
	}

	private static void buildFloor() {
		for (int x = 0; x < SIZE; x++) for (int z = 0; z < SIZE; z++) {
			boolean path = x == 16 || x == 17 || x == 18 || z == 16 || z == 17 || z == 18;
			block(x, 0, z, path ? "minecraft:stone_bricks" : "minecraft:oak_planks");
		}
	}

	private static void buildMarket() {
		for (int row = 0; row < 8; row++) for (int column = 0; column < 8; column++) {
			int x = 2 + column * 4;
			int z = 2 + row * 4;
			block(x + 1, 1, z, "minecraft:barrel");
			block(x + 1, 2, z, "minecraft:oak_slab[type=top,waterlogged=false]");
			block(x - 1, 1, z - 1, "minecraft:oak_fence");
			block(x - 1, 2, z - 1, "minecraft:lantern[hanging=false,waterlogged=false]");
		}
		for (int x = 14; x <= 20; x++) for (int z = 14; z <= 20; z++) block(x, 1, z, "minecraft:smooth_stone");
		block(17, 2, 17, "minecraft:bell[attachment=floor,facing=north,powered=false]");
		for (int[] p : new int[][] {{14,14}, {20,14}, {14,20}, {20,20}}) {
			for (int y = 2; y <= 5; y++) block(p[0], y, p[1], "minecraft:oak_log[axis=y]");
		}
		for (int x = 14; x <= 20; x++) for (int z = 14; z <= 20; z++) block(x, 6, z, "minecraft:red_wool");
		for (int x = 0; x < SIZE; x++) { block(x, 1, 0, "minecraft:oak_fence"); block(x, 1, SIZE - 1, "minecraft:oak_fence"); }
		for (int z = 1; z < SIZE - 1; z++) { block(0, 1, z, "minecraft:oak_fence"); block(SIZE - 1, 1, z, "minecraft:oak_fence"); }
		block(17, 1, 0, "minecraft:air"); block(17, 1, SIZE - 1, "minecraft:air");
	}

	private static ListTag markers() {
		ListTag entities = new ListTag();
		for (int index = 0; index < 64; index++) {
			int x = 2 + (index % 8) * 4;
			int z = 2 + (index / 8) * 4;
			CompoundTag nbt = new CompoundTag();
			nbt.putString("id", "minecraft:armor_stand");
			nbt.putBoolean("Invisible", true);
			nbt.putBoolean("Invulnerable", true);
			nbt.putBoolean("Marker", true);
			nbt.putBoolean("NoGravity", true);
			ListTag tags = new ListTag(); tags.add(StringTag.valueOf("tradeeverything.marker." + index)); nbt.put("Tags", tags);
			CompoundTag entity = new CompoundTag();
			entity.put("pos", doubles(x + 0.5, 1.0, z + 0.5));
			entity.put("blockPos", ints(x, 1, z));
			entity.put("nbt", nbt);
			entities.add(entity);
		}
		return entities;
	}

	private static void block(int x, int y, int z, String state) {
		int stateIndex = PALETTE.computeIfAbsent(state, ignored -> PALETTE.size());
		CompoundTag block = new CompoundTag(); block.put("pos", ints(x, y, z)); block.putInt("state", stateIndex); BLOCKS.add(block);
	}

	private static ListTag palette() {
		ListTag palette = new ListTag();
		PALETTE.forEach((state, ignored) -> {
			String[] split = state.split("\\[", 2);
			CompoundTag entry = new CompoundTag(); entry.putString("Name", split[0]);
			if (split.length == 2) {
				CompoundTag properties = new CompoundTag();
				for (String pair : split[1].substring(0, split[1].length() - 1).split(",")) {
					String[] parts = pair.split("=", 2); properties.putString(parts[0], parts[1]);
				}
				entry.put("Properties", properties);
			}
			palette.add(entry);
		});
		return palette;
	}

	private static ListTag ints(int... values) { ListTag list = new ListTag(); for (int value : values) list.add(IntTag.valueOf(value)); return list; }
	private static ListTag doubles(double... values) { ListTag list = new ListTag(); for (double value : values) list.add(DoubleTag.valueOf(value)); return list; }
}
