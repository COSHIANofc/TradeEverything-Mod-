package com.coshian.tradeeverything.catalog;

import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/** Central Minecraft 26.2 audit of registered items that have no normal Survival acquisition. */
public final class SurvivalEligibility {
	private static final Set<String> UNOBTAINABLE = Set.of(
		"air", "bedrock", "barrier", "light", "debug_stick", "knowledge_book",
		"command_block", "chain_command_block", "repeating_command_block", "command_block_minecart",
		"structure_block", "structure_void", "jigsaw", "test_block", "test_instance_block",
		"spawner", "trial_spawner", "vault", "end_portal_frame", "reinforced_deepslate", "budding_amethyst",
		"suspicious_sand", "suspicious_gravel", "frogspawn",
		"farmland", "dirt_path", "petrified_oak_slab", "chorus_plant", "player_head",
		"infested_stone", "infested_cobblestone", "infested_stone_bricks", "infested_mossy_stone_bricks",
		"infested_cracked_stone_bricks", "infested_chiseled_stone_bricks", "infested_deepslate"
	);

	private SurvivalEligibility() {}
	public static boolean isEligible(Item item) { return isEligible(BuiltInRegistries.ITEM.getKey(item)); }
	public static boolean isEligible(Identifier id) {
		return id != null && id.getNamespace().equals("minecraft") && !UNOBTAINABLE.contains(id.getPath()) && !id.getPath().endsWith("_spawn_egg");
	}
	public static Set<String> explicitExclusions() { return UNOBTAINABLE; }
}
