# TradeEverything

**TradeEverything is a server-only Fabric mod. Players join with an unmodified Minecraft Java Edition 26.2 client: they do not install Fabric Loader, Fabric API, TradeEverything, a resource pack, or a companion mod.** The server requires Fabric Loader, Fabric API, and TradeEverything.

The mod adds a naturally generated Trading Post (`tradeeverything:trading_post`, Japanese: `交易所`). Its clerks are specially configured vanilla villagers using the vanilla merchant screen and vanilla packets. Collectively they sell every item registered in the `minecraft` item registry except `minecraft:air`; they never buy arbitrary player items.

> **Technical-item warning:** command blocks, barriers, structure blocks, spawn eggs, operator-only items, and other technical or normally unobtainable vanilla items are intentionally purchasable. This can materially affect progression and server security.

## Requirements and installation

The server requires Minecraft 26.2, Fabric Loader 0.19.3 or a newer compatible stable version, Fabric API 0.158.0+26.2, TradeEverything 0.1.0, and Azul Zulu Java 25. Put Fabric API and `tradeeverything-0.1.0.jar` in the server's `mods` directory. Do not install anything on connecting clients.

## Trading Posts and commands

The Trading Post is a vanilla `minecraft:jigsaw` structure backed by a generated vanilla structure template. Its random-spread structure set defaults to spacing 40 and separation 12 chunks and targets Overworld land biomes accepted by vanilla village biome tags—not oceans, river-only biomes, the Nether, or the End. It uses vanilla blocks for a fenced, lit market, storage desks, paths, and a central bell pavilion. Temporary vanilla armor-stand data markers are consumed once and replaced idempotently with vanilla villagers; no custom registry entry is exposed to clients and chunk reload does not respawn clerks.

Operator commands require permission level 2:

```text
/locate structure tradeeverything:trading_post
/tradeeverything place
/tradeeverything place <x> <y> <z>
/tradeeverything verify
/tradeeverything reload
```

`place` invokes the same jigsaw structure used by natural generation. With no position it chooses the surface at the source X/Z. `verify` reports eligible, assigned, missing and duplicate counts, category/page counts, maximum offers, pricing health, and server-only status. `reload` validates configuration and refreshes clerks after any currently open trade closes.

## Catalog and clerks

The runtime item registry is the catalog source, so later vanilla additions are included automatically. Reliable vanilla tags and explicit technical-item rules are applied first, registry-path heuristics are secondary, and unmatched items go to Miscellaneous. The eight top-level categories are Building Blocks, Natural Resources, Tools and Combat, Food and Brewing, Redstone and Transportation, Decoration and Utility, Rare and Technical, and Miscellaneous.

Large categories are divided deterministically between numbered vanilla-villager clerks, such as `Building Blocks 1/6`. The default is 48 offers per clerk (configurable from 32 to 64); items are never dropped. Vanilla scoreboard tags persist category/page/version identity. Offers rebuild on initialization or after a catalog/configuration version change, not every tick. Clerks use nitwit profession data, cannot pick up breeding food, and persist with chunks; protection is configurable.

## Server configuration

The first server start creates `config/tradeeverything.json` from bundled defaults:

```json
{
  "language": "en_us",
  "max_offers_per_clerk": 48,
  "protect_npcs": true,
  "catalog_version": 1,
  "structure_spacing": 40,
  "structure_separation": 12,
  "items": {"minecraft:elytra": {"emeralds": 288, "output": 1}}
}
```

The custom message/name language is selected globally by the server (`en_us` or `ja_jp`) because a server-only mod cannot supply translation keys to unmodified clients. Vanilla item names use keys already present in vanilla clients. Price, output, language, protection, and catalog-version changes apply with `/tradeeverything reload`; changing `max_offers_per_clerk` requires a restart so an existing post's deterministic page layout is never corrupted.

Each item can override price and output independently. Prices must be 1–584 emerald-equivalent and output 1 through the item's maximum stack. Values above 64 use up to 64 emerald blocks in the first input plus up to eight emeralds in the second. Invalid entries are warned about individually and ignored; stack-size-aware deterministic defaults remain active. There is no demand inflation.

Spacing/separation fields are validated and reported. Bundled worldgen uses matching 40/12 defaults; changing natural-generation spacing for an existing world requires a server data pack and restart because dynamic worldgen registries load before `/tradeeverything reload`.

## Development on macOS

```zsh
export JAVA_HOME="/Library/Java/JavaVirtualMachines/zulu-25.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
./gradlew clean build
./gradlew generateStructureTemplates
./gradlew runGametest
./gradlew runServer
```

The project follows Fabric's official 26.2 baseline: Loom 1.17, Gradle 9.5.1, Loader 0.19.3, Fabric API 0.158.0+26.2, Java 25, and Mojang's unobfuscated names. `TradingPostTemplateGenerator` is reproducible source for the generated NBT included in the JAR; no copied Minecraft asset is committed.

Automated tests cover exact registry assignment, valid vanilla-only offer stacks, page limits, server-only metadata, absence of custom synchronized registries/networking hooks, vanilla-only template contents, persistent tag serialization, and idempotent clerk initialization. A dedicated-server smoke test covers data-pack loading and startup. Visual terrain quality, real-client joining, merchant interaction/purchases, `/locate`, and natural generation distance remain manual in-game checks.

## License

Copyright 2026 COSHIANofc. Licensed under the [Apache License 2.0](LICENSE).
