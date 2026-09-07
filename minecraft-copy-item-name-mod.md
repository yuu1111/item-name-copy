---
created: 2026-09-07T14:35:37
updated: 2026-09-07T15:40:04
---

# Minecraft アイテム名コピーMod計画

## 概要

インベントリやコンテナ画面でアイテムへマウスカーソルを合わせて `Ctrl+C` を押すと、表示名をOSのクリップボードへコピーするクライアント専用Modを作る

Project名は `ItemNameCopy`、Mod IDは `itemnamecopy`、Repository名は `item-name-copy` とする
マルチバージョンとマルチローダーの管理にはStonecutterを使い、同じソースを基準にFabric、NeoForge、Forge向けの成果物を生成する

## 目標

- アイテム名をチャット、検索、メモへすぐ貼り付けられる
- バニラと通常のModコンテナ画面で同じ操作を使える
- Minecraftの互換境界ごとに必要な差分だけを分離する
- Fabric、NeoForge、Forgeが提供されているMinecraft版を可能な限り広く対象にする
- サーバー導入を不要にし、マルチプレイでもクライアントだけで完結させる
- Loaderや対応バージョンを追加したときに、既存対象のビルド失敗をCIで検出する

## 対象範囲

### 実用最小版

- Fabric、NeoForge、Forge向けのクライアント専用Mod
- 各Loaderのコンテナ画面基底クラスに表示されたスロットを対象にする
  - プレイヤーインベントリ
  - チェストなどのコンテナ
  - 作業台、かまど、金床などの作業画面
  - 標準のコンテナ画面基底クラスを継承するMod追加画面
- マウスカーソル下の空でないスロットを対象にする
- `ItemStack`の現在の表示名を装飾のないプレーンテキストとしてコピーする
- コピー成功時だけ、コピーした名前をアクションバーへ短く表示する
- 日本語名、カスタム名、リソースパックや言語設定で変わる翻訳結果をそのまま扱う

### 対象外

- アイテムID、個数、Lore、NBTやData Componentのコピー
- ワールド画面で手に持っているアイテムのコピー
- レシピビューアー独自の疑似スロットへの個別対応
- 設定画面とキー割り当て変更
- サーバーとの通信
- Snapshot版と開発版Minecraft
- Loader自体が提供されていないMinecraft版との組み合わせ

コピー形式や画面連携などの機能項目は、実用最小版の利用結果から必要性を確認して追加する

## 操作仕様

### コピー条件

次の条件をすべて満たしたキー押下を一回のコピーとして扱う

- 現在の画面が `HandledScreen`または`AbstractContainerScreen`系
- `C`の押下時に左右どちらかの `Ctrl` が押されている
- マウスカーソル下にスロットがある
- スロットの `ItemStack` が空ではない
- テキスト入力欄の選択文字列をコピーする場面ではない
- 同じ押下をすでに処理していない

コピーに成功したときだけ入力を処理済みにする
条件を満たさない場合はMinecraftや他Modへそのまま入力を渡す

### コピーする文字列

- `ItemStack`が画面上で持つ表示名を取得する
- Textの装飾やクリックイベントを除き、`getString`相当のプレーンテキストへ変換する
- 前後の空白や記号を独自に削除しない
- 改行を含む異常な名前は一行へ正規化せず、そのままコピーする

### 競合回避

- クリエイティブ検索、レシピブック検索、金床の名前入力など、文字入力中の `Ctrl+C` を優先する
- キーイベントはコピー対象を確定できる時だけ横取りする
- 長押しによる連続実行はキーリピートを無視して防ぐ
- 他Modが先に入力を処理した場合は上書きしない

入力処理の順序はLoaderと画面ごとに差があるため、実装前の技術検証でLoader固有のScreen EventとMixinを比較する
Loaderごとにテキスト入力との共存を確実に判定できる方式を採用し、方式自体は公開仕様にしない

## マルチバージョン方針

### 採用構成

Stonecutterの公式テンプレートとFabric、NeoForge、Forge対応のマルチローダーテンプレートを基準にする
共通ソース、Loader別ビルドスクリプト、Minecraft版とLoaderの組み合わせごとのGradleプロパティを一つのリポジトリで管理する
Stonecutterは開発時の前処理とGradleサブプロジェクト生成にだけ使い、利用者へ追加Modとして要求しない

```text
repository/
├─ src/main/java/                              共通実装、Adapter、Stonecutter条件分岐
├─ src/main/resources/                         各Loaderのmetadata、Mixin、翻訳、アイコン
├─ versions/<minecraft>-<loader>/              組み合わせ別プロジェクト
│  └─ gradle.properties                        Minecraft、Loader API、Javaの固定値
├─ build.fabric.gradle.kts                     Fabric用ビルド定義
├─ build.neoforge.gradle.kts                   NeoForge用ビルド定義
├─ build.forge.gradle.kts                      Forge用ビルド定義
├─ settings.gradle.kts                         対応行列
├─ stonecutter.gradle.kts                      active targetと収集タスク
└─ .github/workflows/build.yml                  全対象のビルド
```

StonecutterのNode名は `<minecraft>-<loader>` とし、比較に使う論理VersionはMinecraft版だけにする
これにより `forge` などの接尾辞がVersion比較へ混ざることを防ぐ

### 対応範囲

対応数を優先し、主要版だけに限定せず、正式リリースされたMinecraft版とLoaderの有効な組み合わせを追加する
ただし、対応済みとするのは依存解決、ビルド、クライアント起動、コピー操作の確認をすべて通した組み合わせだけとする

| Minecraft系列 | Fabric | Forge | NeoForge | Java基準 | 位置付け |
|---|---|---|---|---:|---|
| `1.14.x`、`1.15.x` | 全正式版を調査 | 全正式版を調査 | 対象外 | 8 | Legacy拡張 |
| `1.16.x` | 全正式版を調査 | 全正式版を調査 | 対象外 | 8 | 主要な旧世代 |
| `1.17.x` | 全正式版を調査 | 全正式版を調査 | 対象外 | 16 | Java移行境界 |
| `1.18.x` | 全正式版を調査 | 全正式版を調査 | 対象外 | 17 | 主要対象 |
| `1.19.x` | 全正式版を調査 | 全正式版を調査 | 対象外 | 17 | 主要対象 |
| `1.20.1` | 対象 | 対象 | 拡張対象 | 17 | 三Loader分岐の開始点 |
| `1.20.2`から`1.20.4` | 対象 | 対象 | 対象 | 17 | NeoForge標準対象の開始点 |
| `1.20.5`以降の`1.20.x` | 対象 | 提供時に対象 | 対象 | 21 | Item Component移行境界 |
| `1.21.x` | 対象 | 提供時に対象 | 対象 | 21 | 全正式版を調査 |
| `26.x`以降 | 提供時に対象 | 提供時に対象 | 提供時に対象 | 公式要件 | 非難読化後の現行系列 |

表の範囲内でもLoader配布物が存在しない組み合わせはNodeを作らない
NeoForge `1.20.1`は配布物が存在するため拡張対象に含めるが、NeoForge公式がForgeの利用を推奨しているため、Forge版を優先して検証する

### 全版対応の判定規則

- Snapshot、Pre-release、Release Candidateは対象外にする
- 正式版ごとにLoader配布物と開発Pluginの解決可否を調べる
- APIとBinary互換性が同じ複数版は一つのJarへまとめられるか実機で確認する
- 一つのJarを複数版へ配布する場合も、宣言する各Minecraft版で起動とコピー操作を確認する
- 互換性がない版はStonecutter NodeとJarを分ける
- 新しい正式版は既存Adapterで通るか調べ、通らなければ新しい互換境界として追加する
- 取得不能な開発Plugin、再配布不能な依存、起動不能な古い環境は未対応理由をIssueへ記録する

### 導入順序

組み合わせを一度に増やさず、共通設計を保ったまま互換境界ごとに広げる

1. `1.20.1`と`1.21.1`でFabric、Forge、NeoForgeのLoader Adapterを確立する
2. 現行系列を追加し、非難読化前後の差分を確立する
3. `1.20.x`と`1.21.x`の正式版を埋める
4. `1.18.2`、`1.19.2`、`1.19.4`へ遡る
5. `1.16.5`と`1.17.1`へ遡り、Java 8と16の境界を確認する
6. `1.14.4`と`1.15.2`をLegacy拡張として検証する
7. Forge `1.12.2`以前は別Gradle世代の実現可能性を調査し、同じリポジトリで再現可能な場合だけ追加する

### Mapping方針

Fabric、NeoForge、Forge間でMinecraftクラス名を揃えるため、難読化されている版ではMojang mappingsを共通基準にする
Fabric固有APIだけFabricの名前を使い、Minecraft本体の型をYarn名で共通層へ持ち込まない

- Mojang mappingsが提供される版は全Loaderで同じ名前空間を使う
- 非難読化された版は公式のクラス名を使う
- Mapping提供前のLegacy版を追加する場合は、その版専用Adapterへ名前変換を閉じ込める
- Mapping差分はLoader判定とVersion判定から分離する

### 互換境界

共通ロジックへLoader APIとMinecraftクラスを広げず、差分が出やすい箇所をAdapterへ閉じ込める

| 境界 | 共通化する内容 | Adapterが所有する内容 |
|---|---|---|
| 初期化 | コピー機能を登録する契約 | LoaderのClient entrypointとEvent Bus |
| キー入力 | `Ctrl+C`か、再入力でないかの判定 | Loader Eventとキー引数の変換 |
| 対象取得 | 空でない対象を返す契約 | コンテナ画面からホバー中のSlotを読む方法 |
| 表示名 | プレーンテキストを返す契約 | `Text` APIの名称差分 |
| Clipboard | 文字列を書き込む契約 | MinecraftクライアントAPIの呼び出し差分 |
| 通知 | 成功内容を表示する契約 | Action Bar APIとLoader差分 |
| metadata | クライアント専用という宣言 | `fabric.mod.json`、`neoforge.mods.toml`、`mods.toml` |

Stonecutterの条件コメントはLoaderまたはAPI差分がある行の近くに限定する
大きな処理全体を組み合わせごとに複製せず、同じ分岐が複数箇所へ増えたらAdapterへまとめる

### Loader方針

- FabricはFabric Screen APIを第一候補とし、共存条件を満たせない版だけMixinへ切り替える
- NeoForgeはScreen Eventを第一候補とし、`1.20.1`も拡張対象として検証する
- Forgeは各世代のGUIまたはScreen Eventを使い、Event APIが異なるLegacy版は専用Adapterへ分ける
- Architectury APIなどの実行時共通化依存は追加せず、利用者側の必須Modを増やさない
- Loader軸とVersion軸の条件を深くネストせず、Loader別Adapterの内部でVersion差分を処理する

## 実装設計

### 責務

```text
キーイベント
    ↓
CopyShortcutHandler
    ├─ 入力競合とリピートを判定
    ├─ HoveredItemProviderからItemStackを取得
    ├─ ItemNameFormatterでプレーンテキスト化
    ├─ ClipboardWriterへ書き込み
    └─ CopyFeedbackへ成功を通知
```

- `CopyShortcutHandler`はコピーを実行したかを真偽値で返す
- `HoveredItemProvider`はMinecraft画面API、Loader Event、Mixinへの依存を所有する
- `ItemNameFormatter`は空Stackを拒否し、表示名だけを返す
- `ClipboardWriter`はOS Clipboardへの書き込みをMinecraftのクライアントスレッドで行う
- `CopyFeedback`は成功時だけアクションバーを更新する

### Mixinの利用

コンテナ画面のホバー中Slotは保護フィールドで、外部ハンドラーから直接読めない版がある
必要な組み合わせではAccessor Mixinまたは対象メソッドへのMixinで最小限に取得する

- Mixin対象、フィールド名、適用環境は各LoaderとMinecraft版で検証する
- privateメソッドの完全な置き換えは行わない
- 描画処理やSlot操作へ介入しない
- アイテムを変更するパケットを送らない
- Fabric、NeoForge、Forgeで同じMixinを利用できない場合はMixin設定を分ける

## 実装段階

### 0 技術検証

- Stonecutterマルチローダーテンプレートから空の対応行列を作る
- Fabric、Forge、NeoForgeのLoader別ビルドスクリプトを分離する
- `1.20.1`と`1.21.1`で依存関係とJava toolchainを固定する
- 各Loaderでクライアントを起動し、キーイベントとホバー中Slotの取得方法を確認する
- 文字入力欄が選択文字列を持つ場合のキーイベント順序を確認する
- Loader EventとMixinから、既存のコピー操作を上書きしない方式を決める

完了条件は、三Loaderで同じコア処理が起動し、インベントリ上のSlotを読み取れ、テキスト欄の `Ctrl+C` を壊さないこととする

### 1 マルチローダー基準版

- コピー条件と表示名取得を実装する
- OS Clipboardへ書き込む
- 成功時のアクションバー通知を追加する
- 日本語と英語の翻訳を追加する
- `1.20.1`と`1.21.1`で利用可能な全Loaderの手動テストを完了する
- Loader別metadataとクライアント専用宣言を検証する

完了条件は、基準版の全組み合わせで受け入れ条件を満たし、成果物を個別に導入できることとする

### 2 対応版の拡張

- 導入順序に沿ってMinecraft版を前後へ拡張する
- 各正式版で利用可能なLoaderを調査する
- 既存Jarの互換範囲と新しいNodeが必要な境界を実機で判定する
- Java 8、16、17、21と現行要件のCI Jobを分ける
- Forge `1.12.2`以前を同じ再現可能なBuildへ含められるか調査する

完了条件は、対応範囲の各組み合わせについて対応済みまたは根拠付き未対応を記録し、対応済みの全成果物が受け入れ条件を満たすこととする

### 3 配布準備

- ModrinthとCurseForge用のmetadata、説明、アイコン、ライセンスを整える
- 成果物名へMod版、Minecraft対象、Loaderを含める
- GitHub Actionsで全対象をビルドし、成果物をまとめて保存する
- リリース前チェックリストをREADMEへ作る
- 配布先のMinecraft版とLoader指定を生成行列と照合する

完了条件は、一つのタグから対応行列の全Jarを再現でき、配布先でMinecraft版とLoaderを誤表示しないこととする

### 4 機能拡張

- コピー対象をアイテムID、Lore、詳細情報から選ぶ設定
- キー割り当て変更
- EMIやREIなどの疑似スロット連携

追加はIssueや実利用で要求を確認し、入力競合と対応行列の増加を受け入れられる項目に限定する

## テスト計画

### 自動テスト

- `Ctrl+C`だけが発火し、単独の `C` や別Modifierでは発火しない
- キーリピートでは二重実行しない
- 空Slotと対象なしではClipboardを書き換えない
- 通常名、翻訳名、カスタム名、日本語名を期待どおりの文字列へ変換する
- コピー失敗時は成功通知を出さない
- Stonecutterの全対象をクリーンビルドする
- 各JarのLoader metadataが対象Minecraft、Loader、必要Javaを正しく宣言する
- 対応行列のNode数と生成Jar数が一致する

Clipboardと通知はインターフェース越しに差し替え、単体テストで実OSのClipboardを変更しない

### 手動テスト行列

各対応済みのMinecraft版とLoaderの組み合わせで次を確認する

- サバイバルのインベントリ
- クリエイティブのインベントリと検索欄
- チェスト、作業台、かまど、金床
- レシピブックの検索欄
- 空Slot、通常名、カスタム名、日本語名
- 左Ctrlと右Ctrl
- 長押し
- シングルプレイとマルチプレイ接続中
- 各Loaderで標準のコンテナ画面基底クラスを継承するMod画面を一つ

### 受け入れ条件

- アイテムへカーソルを合わせた `Ctrl+C` 一回で表示名だけがClipboardへ入る
- 空Slotでは既存のClipboardを変更しない
- テキスト入力欄では選択文字列のコピーを妨げない
- コピー操作でアイテム、Slot、コンテナの状態を変更しない
- 成功通知は一回だけ表示される
- サーバーにModがなくても動作する
- 対応対象の全ビルドがCIで成功する
- 各成果物が宣言したMinecraft版とLoaderで起動する
- 対応行列に未検証の組み合わせを対応済みとして含めない

## CIとリリース

- Pull Requestでは単体テストと全Stonecutter Nodeのビルドを行う
- Java実行環境はMinecraft版とLoaderの開発Plugin要件に合わせて行列化する
- `buildAndCollect`または同等の収集タスクでJarを一箇所へ集める
- 成果物名は `item-name-copy-<mod-version>+<loader>-mc<target>.jar` とする
- リリース時は各Jarを確認済みのMinecraft版とLoaderだけへ紐付ける
- 一つのJarを未検証の近接バージョンへ互換として登録しない
- Loaderごとの公開Jobを分け、一つの失敗で別Loaderの成果物を取り違えないようにする
- 依存更新は対象行列のビルドと代表的な起動確認を通してから取り込む

## リスクと対策

| リスク | 影響 | 対策 |
|---|---|---|
| テキスト入力の `Ctrl+C` を奪う | 検索欄や名前入力が使いにくくなる | 既存処理済み入力を優先し、該当画面を受け入れテストへ含める |
| コンテナ画面の内部APIが変わる | 一部の組み合わせだけコンパイルまたは実行に失敗する | Slot取得をAdapterへ限定し、Stonecutter分岐を局所化する |
| Loader Eventの世代差が大きい | 共通の入力処理を登録できない | Fabric、NeoForge、ForgeのAdapterを分け、必要な世代だけMixinを使う |
| Mod独自画面がSlotを使わない | 一部Modのアイテムをコピーできない | 実用最小版では非対応を明記し、利用要求ごとに連携を追加する |
| 多数の成果物を取り違える | 起動時に互換性エラーが出る | Jar名と配布metadataへMinecraft版とLoaderを含める |
| Clipboardアクセスが失敗する | コピーされないのに成功表示が出る | 書き込み成否を返し、失敗時は通知とログを分ける |
| 対応行列が大きくなる | 検証漏れとCI時間が増える | Node生成元を一つにし、変更対象のJobと定期全件Jobを分ける |
| 古いForgeGradleが現行Gradleで動かない | Legacy版を同じBuildへ含められない | 別Build Rootを調査し、再現性を確保できない版は根拠付き未対応にする |
| 一つのJarへ広い互換範囲を宣言する | 特定の中間版だけ起動しない | 宣言する全Minecraft版で起動確認し、未確認版を追加しない |

## 未決事項

- 正式なMod名とpackage名
- 公開先とリポジトリ所有者
- ライセンス
- 成功通知の既定文言
- 最低対応版を `1.14.4`より前へ広げられるか
- Minecraft正式版ごとの有効なLoader組み合わせ
- 各LoaderでScreen EventとMixinのどちらを使うか
- 同じJarで安全にまとめられるMinecraft版の範囲

入力捕捉方式、対応行列、Jar互換範囲は技術検証と起動確認で決める
名前、公開先、ライセンスは配布準備へ入る前に決める

## 参考資料

- [Stonecutter Fabric template](https://github.com/stonecutter-versioning/stonecutter-template-fabric)
- [Stonecutter NeoForge template](https://github.com/stonecutter-versioning/stonecutter-template-neoforge)
- [Stonecutter Fabric・NeoForge template](https://github.com/stonecutter-versioning/stonecutter-template-multiloader)
- [Stonecutter Fabric・NeoForge・Forge template](https://github.com/rotgruengelb/stonecutter-mod-template)
- [Stonecutter](https://github.com/stonecutter-versioning/stonecutter)
- [Fabric Key Mappings](https://docs.fabricmc.net/develop/key-mappings)
- [Fabric ScreenKeyboardEvents 1.21.1](https://maven.fabricmc.net/docs/fabric-api-0.110.0%2B1.21.1/net/fabricmc/fabric/api/client/screen/v1/ScreenKeyboardEvents.html)
- [Fabric ScreenKeyboardEvents 26.2](https://maven.fabricmc.net/docs/fabric-api-0.154.2%2B26.2/net/fabricmc/fabric/api/client/screen/v1/ScreenKeyboardEvents.html)
- [Fabric Mapping移行ガイド](https://docs.fabricmc.net/develop/porting/mappings/)
- [Fabric Example Mod](https://github.com/FabricMC/fabric-example-mod)
- [NeoForge User Guide](https://docs.neoforged.net/user/docs/)
- [Forge Documentation](https://docs.minecraftforge.net/)
- [Forge Legacy Documentation](https://docs.minecraftforge.net/en/latest/legacy/)
