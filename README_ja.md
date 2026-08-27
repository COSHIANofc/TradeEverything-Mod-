# TradeEverything

**TradeEverything はサーバー専用 Fabric Mod です。プレイヤーは未改造の Minecraft Java Edition 26.2 クライアントで参加でき、Fabric Loader、Fabric API、TradeEverything、リソースパック、補助クライアント Mod は不要です。** サーバー側だけに Fabric Loader、Fabric API、TradeEverything を導入します。

自然生成される**交易所** (`tradeeverything:trading_post`) を追加します。商人は特別設定されたバニラ Villager で、バニラ取引画面とバニラ通信だけを使います。全員で `minecraft` アイテムレジストリの全登録項目（`minecraft:air` のみ除外）を販売し、任意アイテムは買い取りません。

> **技術アイテム警告:** コマンドブロック、バリア、ストラクチャーブロック、スポーンエッグ、管理者専用・通常入手不能アイテムも意図的に購入可能です。進行バランスとサーバー安全性に大きく影響する場合があります。

## 必要環境と導入

サーバーに Minecraft 26.2、Fabric Loader 0.19.3 以上の互換安定版、Fabric API 0.158.0+26.2、TradeEverything 0.1.0、Azul Zulu Java 25 が必要です。サーバーの `mods` に Fabric API と `tradeeverything-0.1.0.jar` を配置します。接続クライアントには何も導入しません。

## 自然生成とコマンド

交易所は生成済みバニラ構造テンプレートを使う `minecraft:jigsaw` Structure です。既定 random-spread は間隔 40、分離 12 チャンクで、バニラ村対象のオーバーワールド陸上バイオームに生成されます。海・川専用バイオーム、ネザー、エンドは対象外です。バニラブロックだけで柵付き市場、照明、保管机、道、鐘の中央広場を構成します。一時的なバニラ Armor Stand マーカーは一度だけ冪等にバニラ Villager へ置換され、チャンク再読込で増殖しません。

権限レベル 2 のコマンド:

```text
/locate structure tradeeverything:trading_post
/tradeeverything place
/tradeeverything place <x> <y> <z>
/tradeeverything verify
/tradeeverything reload
```

`place` は自然生成と同じ jigsaw Structure を使い、省略時は実行者 X/Z の地表を選びます。`verify` は対象、割当、欠落、重複、カテゴリ、ページ、最大取引数、価格状態、サーバー専用状態を表示します。`reload` は設定を検証し、開いている取引が閉じた後に商人を更新します。

## カタログと商人

カタログは実行時レジストリから構築され、将来のバニラ追加も自動対象です。バニラタグと技術アイテム明示ルールを優先し、ID パス判定は補助、未一致は「その他」です。8 カテゴリは建築ブロック、天然資源、道具・戦闘、食料・醸造、レッドストーン・交通、装飾・実用品、希少・技術、その他です。

大カテゴリは `建築ブロック 1/6` のような番号付きバニラ Villager に決定的に分割します。既定上限は 1 人 48 取引（32～64）で、アイテムを削除しません。バニラ Scoreboard Tag がカテゴリページとバージョンを永続化し、取引は初期化時または設定／カタログバージョン変更時だけ再構築されます。商人はニート職、食料を拾わない永続 Villager で、保護は設定可能です。

## サーバー設定

初回起動で `config/tradeeverything.json` を作成します。`language` (`en_us` / `ja_jp`)、`max_offers_per_clerk`、`protect_npcs`、`catalog_version`、`structure_spacing`、`structure_separation`、アイテム別 `emeralds` / `output` を設定できます。

独自メッセージと商人名の言語はサーバー全体で選びます。サーバー専用 Mod は未改造クライアントへ独自翻訳キーを追加できないためです。バニラアイテム名は既存のバニラ翻訳キーを利用します。価格、出力、言語、保護、カタログバージョンは `/tradeeverything reload` で反映されます。既存交易所のページ構成を壊さないため、`max_offers_per_clerk` の変更には再起動が必要です。

価格は 1～584 エメラルド相当、出力は 1～最大スタック数です。64 超は第 1 入力のエメラルドブロック（最大 64）と第 2 入力のエメラルド（最大 8）で表現します。不正項目だけを警告して安全な既定値へ戻し、需要インフレはありません。同梱 worldgen は既定 40/12 です。既存ワールドの自然生成間隔変更には、動的レジストリ読込順のためデータパックと再起動が必要です。

## macOS 開発・テスト

```zsh
export JAVA_HOME="/Library/Java/JavaVirtualMachines/zulu-25.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew clean build
./gradlew generateStructureTemplates
./gradlew runGametest
./gradlew runServer
```

Fabric 26.2 公式基準（Loom 1.17、Gradle 9.5.1、Loader 0.19.3、Fabric API 0.158.0+26.2、Java 25、非難読化名）に従います。`TradingPostTemplateGenerator` が JAR 内 NBT の再現可能なソースで、Minecraft 資産のコピーはコミットしません。

自動テストは全対象の一意割当、バニラ限定取引、ページ上限、server-only metadata、カスタム同期レジストリ／通信の不在、テンプレート内容、タグ直列化、初期化の冪等性を検証します。専用サーバー smoke test でデータパック読込と起動を確認します。景観、実クライアント接続、取引・購入、`/locate`、自然生成距離は手動確認事項です。

## ライセンス

Copyright 2026 COSHIANofc. [Apache License 2.0](LICENSE) で提供します。
