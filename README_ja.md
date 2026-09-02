# TradeEverything

**TradeEverything はサーバーと接続クライアントの両方へ導入する Fabric Mod です。** 双方に Minecraft 26.2、Fabric Loader、Fabric API、同じバージョンの TradeEverything が必要です。外部リソースパックは不要です。

自然生成される**交易所** (`tradeeverything:trading_post`) を追加します。各交易所にはcanonicalなバニラ Villager商人が1人だけ存在し、操作すると検索可能な専用画面が開きます。監査済みの通常Survival入手可能な有効アイテムをすべて売買できます。

## 必要環境と導入

Minecraft 26.2、Fabric Loader 0.19.3以上の互換安定版、Fabric API 0.158.0+26.2、TradeEverything 0.5.a-dev、Azul Zulu Java 25が必要です。サーバーとクライアント双方の `mods` にFabric APIと `tradeeverything-0.5.a-dev.jar` を配置します。

## 自然生成とコマンド

交易所は再現可能な生成元を持つバニラ構造テンプレートを使う `minecraft:jigsaw` Structure です。既定 random-spread は間隔 40、分離 12 チャンクで、バニラ村対象のオーバーワールド陸上バイオームに生成されます。海・川専用バイオーム、ネザー、エンドは対象外です。設置前に、回転後の 35×35 フットプリント全体で `WORLD_SURFACE_WG` と `OCEAN_FLOOR_WG` を標本化します。水没地点と傾斜が大きい地点を拒否し、床を最高地表へ合わせ、12 ブロック深の丸石基礎で低い部分を支持するため、地下埋没や浮遊を事後スキャンなしで防ぎます。

バニラブロックだけで市場を構成し、1個の一時的なバニラArmor Standマーカーを冪等にcanonical商人へ置換します。自然生成と `/tre place` は同じ登録済みjigsaw配置と初期化経路を使います。

権限レベル 2 のコマンド:

```text
/locate structure tradeeverything:trading_post
/tre place
/tre place <x> <y> <z>
/tre summon
/tre summon <x> <y> <z>
/tre verify
/tre reload
```

`place` は自然生成と同じjigsaw Structureを使い、位置なしでは近くの安全な地表を選択します。`summon` は指定したロード済みブロック座標、または実行者の位置に、追加のcanonical商人を召喚します。`verify` は登録数、有効数、無効数、重複、価格状態、単一商人、検索UI状態を表示します。`reload` は設定を検証してカタログを再構築し、古い開画面からの購入要求を安全に拒否します。

## 検索カタログと商人

実行時レジストリと検証済みサーバー設定から一元化された `TradeCatalog` を構築します。各項目はregistry ID、有効状態、価格、出力数量を持ち、将来はGUIや通信を書き換えずに生成JSONへ置換できます。スプレッドシート読込機能は含みません。

完全カタログは巨大な `MerchantOffers` に保存しません。画面を開いた時だけ上限付きlarge payloadで同期し、そのセッション中はクライアントでキャッシュ・検索します。入力ごとの通信はありません。表示はクライアント言語のアイテム名順、同名時はregistry ID順です。検索は大文字小文字を区別せず、前後空白を除き、部分一致と `minecraft:diamond` のようなID一致に対応します。

売買時にクライアントが送るのはmenu ID、catalog version、選択registry ID、上限付き数量だけです。サーバーがセッション、商人と距離、最新カタログ、有効状態、価格・報酬、数量、inventory内容、容量を再検証し、transaction全体をatomicにcommitします。価格、報酬、出力、inventory合計を偽装するpacket項目はありません。

商人は永続的な識別・固定座標タグを保持し、通常時はバニラVillager AIで行動します。TradeEverything画面が開いている間だけ移動を抑制し、最後のセッションが閉じると通常行動へ戻ります。既存worldではpage 0の管理商人をcanonicalへ移行し、他のTradeEverything管理ページだけを退役させます。通常Villagerは変更しません。

## サーバー設定

初回起動で、拡張子なしのJSONファイル `config/config` を同梱既定値から作成します。先頭の `config/` はFabric標準の設定ディレクトリで、利用者向けファイル名は正確に `config` です。

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

拡張子はありませんが内容はJSONです。各registry IDの `enabled`、`emeralds`、`output` は独立して省略でき、未指定フィールドは同梱の個別設定、次にスタックサイズ依存の既定値を保持します。上例の丸石は、1回につき1エメラルドを消費して16個を渡します。ファイルがなければ自動生成します。不明IDや不正な個別フィールドは警告して無視します。JSON全体が壊れている場合は、正確なパスとparseエラーをログへ出し、元ファイルを書き換えずに同梱既定値で起動を継続します。

`SurvivalEligibility` はこの上書きより先に適用されます。そのため設定で有効化しても、air、command/debug/operatorアイテム、spawn eggなど通常Survivalで入手不能と監査されたアイテムは販売されません。

GUIとアイテム名は各クライアントのMinecraft言語に従い、英語・日本語UIを同梱します。コマンド文言にはサーバー側 `language` を利用できます。`/tre reload` 後、既に開いていた古い画面は再度開く必要があります。

価格は 1～584 エメラルド相当、出力は 1～最大スタック数です。64 超は第 1 入力のエメラルドブロック（最大 64）と第 2 入力のエメラルド（最大 8）で表現します。不正項目だけを警告して安全な既定値へ戻し、需要インフレはありません。同梱 worldgen は既定 40/12 です。既存ワールドの自然生成間隔変更には、動的レジストリ読込順のためデータパックと再起動が必要です。

## macOS 開発・テスト

```zsh
export JAVA_HOME="/Library/Java/JavaVirtualMachines/zulu-25.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew clean build
./gradlew generateStructureTemplates
```

Fabric 26.2 公式基準（Loom 1.17、Gradle 9.5.1、Loader 0.19.3、Fabric API 0.158.0+26.2、Java 25、非難読化名）に従います。`TradingPostTemplateGenerator` が JAR 内 NBT の再現可能なソースで、Minecraft 資産のコピーはコミットしません。

自動テストは検索正規化、ID検索、無効項目排除、カタログ一意性、値検証、偽装・古い・無効・支払不足要求の拒否、出力1回、単一マーカー、商人移行／冪等性、anchor永続化、静止、地形選択、client/common metadata、payload形状を検証します。GUI配置、フォーカス、スクロール感、tooltip、実packet通信、右クリック開画面、自然生成外観はゲーム内確認が必要です。

## バージョニング

現在のバージョンは `0.5.a-dev` です。形式は `MAJOR.MINOR.REVISION-STATE` です。MAJOR更新時はMINORを `1`、REVISIONを `a` に戻します。通常の利用者向け変更は数値MINORを増やしてREVISIONを `a` に戻し、仕様を変えない内部・実行時修正だけが小文字REVISIONを進めます。`-dev`、`-beta`、`-pre` は開発状態を表し、安定版は接尾辞を付けません。例: `0.1.a-dev`、`0.2.a-beta`、`1.1.a`。

購入時はエメラルドブロック1個をエメラルド9個として扱います。エメラルドとエメラルドブロックの所持数を合算し、必要ならエメラルドのお釣りを自動で返します。

## ライセンス

Copyright 2026 COSHIAN. [Apache License 2.0](LICENSE) で提供します。
