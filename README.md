# TradeEverything

**TradeEverything is a Fabric mod for both the server and connecting clients.** Minecraft 26.2, Fabric Loader, Fabric API, and the same TradeEverything version are required on both sides; no external resource pack is required.

The mod adds a naturally generated Trading Post (`tradeeverything:trading_post`, Japanese: `交易所`). Each post has one canonical vanilla-villager merchant. Interacting with it opens a searchable TradeEverything screen containing every enabled, audited Survival-obtainable vanilla item. Air, command/debug/operator items, spawn eggs, and other items without a legitimate Survival acquisition are excluded.

## Requirements and installation

Minecraft 26.2, Fabric Loader 0.19.3 or a newer compatible stable version, Fabric API 0.158.0+26.2, TradeEverything 0.5.a-dev, and Azul Zulu Java 25 are required. Put Fabric API and `tradeeverything-0.5.a-dev.jar` in both the server and client `mods` directories.

## Trading Posts and commands

The Trading Post is a vanilla `minecraft:jigsaw` structure backed by a reproducibly generated vanilla structure template. Its random-spread structure set defaults to spacing 40 and separation 12 chunks and targets Overworld land biomes accepted by vanilla village biome tags—not oceans, river-only biomes, the Nether, or the End. Before placement, server worldgen samples `WORLD_SURFACE_WG` and `OCEAN_FLOOR_WG` across the rotated 35×35 footprint. Waterlogged or excessively steep sites are rejected; the floor is aligned to the highest accepted surface and a 12-block vanilla cobblestone foundation supports lower points. This prevents buried or unsupported markets without a post-generation scan.

The market uses vanilla blocks for fenced stalls, lighting, storage desks, paths, and a central bell pavilion. One temporary vanilla armor-stand data marker is consumed and replaced idempotently with the canonical merchant. Natural generation and `/tre place` both pass through the same registered jigsaw placement and marker initialization path.

Operator commands require permission level 2:

```text
/locate structure tradeeverything:trading_post
/tre place
/tre place <x> <y> <z>
/tre summon
/tre summon <x> <y> <z>
/tre verify
/tre reload
```

`place` invokes the same jigsaw structure used by natural generation. With no position it chooses a nearby suitable surface. `summon` creates an additional standalone canonical merchant at the source or supplied loaded block position. `verify` reports registered, enabled, disabled and duplicate counts, pricing health, the single-merchant invariant, and searchable-UI status. `reload` validates configuration, rebuilds the catalog, and invalidates stale open purchase sessions safely.

## Searchable catalog and merchant

The runtime item registry and the validated server configuration form a centralized `TradeCatalog`. Each entry exposes registry ID, enabled state, price, and output quantity, allowing a generated JSON source to replace the current loader later without changing the GUI or protocol. No spreadsheet reader is included.

The complete catalog is not stored in `MerchantOffers`. It is sent once when the screen opens using a bounded large-payload codec, cached for that menu session, and filtered locally without sending a packet per keystroke. Results are sorted by the client's localized item name with registry ID as the tie-breaker. Search is case-insensitive, trims whitespace, matches partial localized names, and also matches IDs such as `minecraft:diamond`.

The client sends only menu ID, catalog version, selected registry ID, and bounded quantity when buying or selling. The server independently validates the open session, merchant identity and distance, current catalog/version, enabled state, price or reward, quantity, inventory contents, and capacity before atomically committing a transaction. Client-provided price, reward, output, or inventory totals do not exist in the protocol.

The merchant retains persistent vanilla scoreboard tags and anchor coordinates and uses normal Villager AI outside active TradeEverything sessions. While its UI is open, navigation and horizontal movement are suppressed; closing the final active session restores normal behavior. Existing page-0 TradeEverything merchants migrate to the canonical role; other legacy TradeEverything page merchants are retired. Ordinary villagers are never selected by proximity alone.

## Server configuration

The first server start creates the extensionless JSON file `config/config` from bundled defaults. The exact user-facing item-management filename is `config`; the first `config/` is Fabric's normal configuration directory:

```json
{
  "language": "en_us",
  "protect_npcs": true,
  "catalog_version": 3,
  "structure_spacing": 40,
  "structure_separation": 12,
  "items": {
    "minecraft:diamond": {"enabled": true, "emeralds": 24, "output": 1},
    "minecraft:cobblestone": {"enabled": true, "emeralds": 1, "output": 16},
    "minecraft:elytra": {"enabled": false}
  }
}
```

The file remains JSON despite having no `.json` suffix. Each registry-ID entry may independently set `enabled`, `emeralds`, and `output`; omitted fields retain the bundled item override when one exists, then the stack-size-aware default. Thus `1` emerald and `output: 16` means one purchase consumes one emerald and delivers 16 cobblestone. Missing files are generated automatically. Unknown IDs and invalid individual fields are warned about and ignored. Malformed JSON logs the exact path and parser diagnostic, is never overwritten, and startup continues with bundled defaults.

`SurvivalEligibility` is applied before this override layer. A configuration entry cannot enable air, command/debug/operator items, spawn eggs, or another item audited as unobtainable in ordinary Survival. The custom GUI and item names follow each client's active Minecraft language using bundled English and Japanese translations plus vanilla item components. Server command text can still use the global `language` setting. Catalog changes apply with `/tre reload`; an already-open screen whose catalog version changed must be reopened before another purchase.

Each item can override price and output independently. Prices must be 1–584 emerald-equivalent and output 1 through the item's maximum stack. Values above 64 use up to 64 emerald blocks in the first input plus up to eight emeralds in the second. Invalid entries are warned about individually and ignored; stack-size-aware deterministic defaults remain active. There is no demand inflation.

Spacing/separation fields are validated and reported. Bundled worldgen uses matching 40/12 defaults; changing natural-generation spacing for an existing world requires a server data pack and restart because dynamic worldgen registries load before `/tre reload`.

## Development on macOS

```zsh
export JAVA_HOME="/Library/Java/JavaVirtualMachines/zulu-25.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
./gradlew clean build
./gradlew generateStructureTemplates
```

The project follows Fabric's official 26.2 baseline: Loom 1.17, Gradle 9.5.1, Loader 0.19.3, Fabric API 0.158.0+26.2, Java 25, and Mojang's unobfuscated names. `TradingPostTemplateGenerator` is reproducible source for the generated NBT included in the JAR; no copied Minecraft asset is committed.

Automated tests cover search normalization/filtering, registry-ID search, disabled-entry exclusion, catalog uniqueness and value validation, forged/stale/disabled/underfunded purchase rejection, atomic delivery, one-marker structure generation, merchant migration/idempotence, persistent anchors, normal merchant AI, terrain selection, client/common metadata, and payload shape. Exact GUI layout, focus, scrolling feel, tooltips, live packet flow, right-click opening, and natural-generation appearance require in-game verification.

## Versioning

The current version is `0.5.a-dev`. TradeEverything uses `MAJOR.MINOR.REVISION-STATE`: major changes reset minor to `1` and revision to `a`; normal user-visible changes increment numeric minor and reset revision to `a`; internal/runtime-only fixes advance lowercase revision. `-dev`, `-beta`, and `-pre` identify development states; stable releases omit the suffix. Examples: `0.1.a-dev`, `0.2.a-beta`, `1.1.a`.

For purchases, one Emerald Block counts as nine Emeralds. Emeralds and Emerald Blocks are combined automatically, including exact Emerald change when a block is converted.

## License

Copyright 2026 COSHIAN. Licensed under the [Apache License 2.0](LICENSE).
