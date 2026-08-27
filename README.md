# TradeEverything

TradeEverything is a Fabric mod for Minecraft Java Edition 26.2. It adds a naturally generated **Trading Post** (`tradeeverything:trading_post`, Japanese: `交易所`) staffed by eight persistent, villager-like merchants. Together they sell every item registered in the vanilla `minecraft` namespace except `minecraft:air`, for emeralds or emerald blocks. Merchants never buy catalog items from players.

> **Technical-item warning:** command blocks, barriers, structure blocks, spawn eggs, operator-only items, and other technical or normally unobtainable registered vanilla items are intentionally purchasable. This is required by the mod's “every registered vanilla item” promise and can affect progression and server security.

## Requirements and installation

- Minecraft Java Edition 26.2
- Fabric Loader 0.19.3 or newer compatible stable release
- Fabric API 0.158.0+26.2
- Azul Zulu Java 25

Install Fabric Loader, place Fabric API and `tradeeverything-0.1.0.jar` in the client and server `mods` folders, then start the game/server. The same JAR supports both environments.

## Trading Posts and commands

Trading Posts use a registered custom world structure and a deterministic random-spread structure set (spacing 40 chunks, separation 12). They generate on safe Overworld land biomes used by vanilla villages, never in ocean/river-only biomes, the Nether, or the End. The code-native structure piece adapts to surface height and builds a lit central market, paths, workstations, storage, stalls, and one merchant for each category. NPCs are spawned during structure placement, persist with the chunk, and are not recreated by chunk reload.

Operator commands require permission level 2:

```text
/locate structure tradeeverything:trading_post
/tradeeverything place
/tradeeverything place <x> <y> <z>
/tradeeverything verify
```

`place` invokes the same registered structure generator used by natural generation. Without a position, the current chunk and its safe surface are used. `verify` reports eligible/categorized/duplicate/missing counts, category count, and price configuration health.

## Catalog categories and pagination

The catalog is rebuilt from the runtime item registry and sorted deterministically. Ordered vanilla-tag rules and explicit safety-sensitive rules are applied first, path heuristics are secondary, and unmatched items go to Miscellaneous: Building Blocks; Natural Resources; Tools and Combat; Food and Brewing; Redstone and Transportation; Decoration and Utility; Rare and Technical; and Miscellaneous.

Each merchant sends at most 40 offers. Sneak-use (hold Shift and interact) cycles deterministically through that merchant's pages; normal interaction opens the standard merchant screen. Category and page survive save/reload.

## Price configuration

On first server start, `config/tradeeverything-prices.json` is created from bundled defaults. Each registry ID can independently override `emeralds` and `output`:

```json
{"items":{"minecraft:elytra":{"emeralds":288,"output":1}}}
```

Prices must be 1–584 emerald-equivalent and output must be 1 through the item's maximum stack size. Invalid individual entries are ignored with a warning; safe deterministic stack-size defaults remain active. Values over 64 use emerald blocks plus emeralds in the second input slot. Offers have unlimited uses, zero demand multiplier, and are generated only by the authoritative server.

## Development on macOS

```zsh
export JAVA_HOME="/Library/Java/JavaVirtualMachines/zulu-25.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
./gradlew clean build
./gradlew runGametest
./gradlew runServer
```

The project follows Fabric's official 26.2 example: Loom 1.17, Gradle 9.5.1, Loader 0.19.3, Fabric API 0.158.0+26.2, and Mojang's unobfuscated names without obsolete Yarn configuration.

## Testing and limitations

`clean build` compiles common/client code and runs server GameTests. The invariant test checks total runtime registry coverage, uniqueness, air exclusion, and every generated offer's inputs/output. Automated CI cannot visually inspect the merchant UI or terrain aesthetics; those remain on the manual in-game checklist. Structure source is reproducible Java in `TradingPostStructure`, not an unexplained binary template.

## License

Copyright 2026 COSHIANofc. Licensed under the [Apache License 2.0](LICENSE).
