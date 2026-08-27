# TradeEverything

TradeEverything は Minecraft Java Edition 26.2 用 Fabric Mod です。自然生成される**交易所** (`tradeeverything:trading_post`) と 8 種類の常駐商人を追加し、`minecraft` 名前空間の全登録アイテム（`minecraft:air` のみ除外）をエメラルドまたはエメラルドブロックで購入できます。買い取り取引はありません。

> **技術アイテム警告:** コマンドブロック、バリア、ストラクチャーブロック、スポーンエッグ、管理者専用・通常入手不能アイテムも意図的に購入可能です。「登録済みバニラアイテムをすべて扱う」仕様のため、進行バランスやサーバー安全性に影響します。

## 必要環境と導入

Minecraft 26.2、Fabric Loader 0.19.3 以上の互換安定版、Fabric API 0.158.0+26.2、Azul Zulu Java 25 が必要です。Fabric API と `tradeeverything-0.1.0.jar` をクライアントとサーバーの `mods` フォルダーへ配置します。

## 自然生成とコマンド

交易所は登録済みカスタム Structure と random-spread Structure Set（間隔 40、分離 12 チャンク）を使用します。バニラ村が生成可能なオーバーワールド陸上バイオームを対象とし、海・川専用バイオーム、ネザー、エンドには生成されません。地表へ追従し、中央市場、道、照明、作業場所、倉庫、露店、各カテゴリ 1 体の商人を配置します。チャンク再読込では NPC を複製しません。

```text
/locate structure tradeeverything:trading_post
/tradeeverything place
/tradeeverything place <x> <y> <z>
/tradeeverything verify
```

権限レベル 2 が必要です。`place` は自然生成と同じ Structure を使い、座標省略時は現在チャンクの地表へ配置します。`verify` は対象数、分類数、重複、欠落、カテゴリ数、価格状態を表示します。

## カテゴリ・ページ・価格

カタログは実行時レジストリから自動構築します。バニラタグと明示ルールを優先し、パス名を補助に使い、未一致は「その他」になります。カテゴリは建築、天然資源、道具・戦闘、食料・醸造、レッドストーン・交通、装飾・実用品、希少・技術、その他です。

送信取引は最大 40 件です。Shift を押しながら使用するとページを循環し、通常使用で標準取引画面を開きます。カテゴリとページは保存されます。

初回起動で `config/tradeeverything-prices.json` を生成します。ID ごとに `emeralds`（1～584）と `output`（1～最大スタック数）を独立設定できます。不正項目だけを警告して無視し、安全な既定値へ戻します。64 超はエメラルドブロックと第 2 入力欄で表現し、需要インフレはありません。

## macOS 開発・テスト

```zsh
export JAVA_HOME="/Library/Java/JavaVirtualMachines/zulu-25.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew clean build
./gradlew runGametest
./gradlew runServer
```

Fabric 26.2 公式例（Loom 1.17、Gradle 9.5.1、Loader 0.19.3、Fabric API 0.158.0+26.2、非難読化名）に従います。GameTest は全対象の一意分類、欠落・重複・air 除外、全取引の価格・入力・出力を検証します。UI と景観、実購入は手動確認が残ります。Structure は再現可能な Java ソースです。

## ライセンス

Copyright 2026 COSHIANofc. [Apache License 2.0](LICENSE) で提供します。
