# ItemNameCopyの設計と対応方針

## 設計目標

ItemNameCopyは、コンテナ画面に表示されたアイテム名を`Ctrl+C`でコピーするクライアント専用Mod
Mod IDは`itemnamecopy`、Java packageは`com.github.yuu1111.itemnamecopy`とする

- Fabric、Forge、NeoForgeで同じ操作とコピー結果を提供する
- サーバーとの通信や、利用者側の共通化Modを必要としない
- MinecraftやLoaderのAPI差分を境界へ閉じ込め、コピー条件を共通コードで管理する
- 対応を宣言するMinecraft版とLoaderごとに、ビルドとゲーム内動作を検証できるようにする

## コピー処理の契約

次の条件をすべて満たしたキー押下を、一回のコピーとして扱う

- 現在表示されている画面がコンテナ画面である
- `C`の押下時に左右どちらかのCtrlだけが押されている
- テキスト入力欄へフォーカスがなく、他の処理がキー入力を処理済みではない
- カーソル下に空でないスロットがある
- 同じキー押下またはキーリピートを処理していない

対象の`ItemStack`から表示名を取得し、装飾を除いたプレーンテキストをクリップボードへ書き込む
独自のトリムや改行の正規化は行わない

クリップボードへ書き込んだ値を読み戻して一致した場合だけ処理済みとし、成功通知を表示する
対象がない場合は入力と既存のクリップボードを維持する
書き込みに失敗した場合は入力を消費せず、成功通知を表示しない

## 実装構成

```text
キーイベント
    ↓
ItemNameCopyClient
    ├─ CopyShortcutHandler       コピー条件とキー押下状態
    └─ ContainerCopyTarget
        ├─ MinecraftAccess       画面、Slot、Clipboard、通知の版差分
        └─ BundledFeedback       翻訳リソースを利用できない場合の通知
```

- `core`はMinecraftとLoaderへ依存しないコピー条件と単体テストを持つ
- `src/main`はStonecutterの条件分岐を含む共通のクライアント実装とリソースを持つ
- `src/legacy112`はForge 1.12.2固有の起動、画面、Clipboard処理を持つ
- `versions/<minecraft>-<loader>`は組み合わせごとのGradleプロパティを持つ
- `minecraft-client-testkit`はMinecraftクライアント内でテストを実行する汎用ランナーを持つ
- `tests/client`はItemNameCopy固有のクライアントテストを持ち、配布Jarには含めない

コンテナ画面のホバー中Slotとレシピ検索欄は、Mixin Accessorまたは対象世代の公開フィールドから取得する
描画、Slot操作、アイテムを変更するパケットには介入しない

## マルチバージョン構成

Stonecutterで共通ソースを前処理し、Minecraft版とLoaderの組み合わせごとにGradleサブプロジェクトを生成する
Node名は`<minecraft>-<loader>`とし、Minecraft版、Loader、Mapping、必要Java、ビルドスクリプトの固定値は各Nodeの`gradle.properties`に置く

`versions`に存在するNodeがビルド対象の正本となる
対象一覧を文書へ転記せず、`scripts/Get-BuildMatrix.ps1`とCIも同じ定義から行列を生成する

### 対応を追加する条件

- Snapshot、Pre-release、Release Candidateではなく、正式リリースされたMinecraft版である
- 対象Loaderの配布物と開発Pluginを解決できる
- 既存Nodeと互換であっても、対象版でクライアント起動とコピー操作を確認する
- APIまたはBinary互換性がない場合はNodeとJarを分ける
- 再現可能なビルドや起動環境を用意できない版は対応済みに含めない

一つのJarを複数のMinecraft版へ配布する場合も、宣言するすべての版で起動とコピー操作を確認する

### Mappingと互換境界

難読化されている版ではMojang mappingsを共通基準とし、非難読化された版では公式のクラス名を使う
Mojang mappingsを利用できない旧版のクラス名とメンバー名は、Gradleの変換定義と旧版専用実装へ閉じ込める

Stonecutterの条件分岐は、次のAPI差分がある箇所の近くへ限定する

- Loaderのクライアント初期化とキーイベント
- コンテナ画面、ホバー中Slot、テキスト入力欄の取得
- Clipboard APIとWindow handle
- ComponentとAction Bar API
- Loader metadataとMixinの利用可否

大きな処理を版ごとに複製せず、複数箇所へ広がる差分は`MinecraftAccess`またはLoader固有の入口へまとめる

## Loaderとの統合

- FabricはKeyboard handlerへのMixinから入力を受け取り、既存画面の処理結果をコピー条件へ渡す
- ForgeとNeoForgeは各世代の画面キーイベントを使い、イベントAPIが異なる旧Forgeは専用の入口を使う
- Mixinを利用できない旧ForgeではAccess Transformerと公開フィールドを使う
- Loader側で処理済みの入力を優先し、ItemNameCopyがコピー対象を確定できた場合だけイベントを消費する

Loaderや版ごとに入力経路が異なっても、最終的な判定は`CopyShortcutHandler`へ集約する

## 検証と配布

### 自動検証

- `core`の単体テストで修飾キー、リピート、処理済み入力、空Slot、Clipboard失敗、通知条件を確認する
- 各Nodeのビルドでコンパイル、Loader metadata、Mixin、Jar名を検証する
- `buildAndCollect`でNode数と収集したJar数を照合し、SHA-256を含む`verification-manifest.json`を生成する
- GitHub Actionsで対象行列を生成し、各Nodeを独立してビルドする
- クライアント自動テストで本体の入力処理、Clipboard、翻訳通知、検索欄との競合を確認する

クライアント自動テストの環境、実行方法、対象外は[tests/README.md](../tests/README.md)に記載する
自動化できない左右Ctrlの物理入力、Mod画面、マルチプレイなどは[リリース前チェックリスト](todo.md#リリース前チェックリスト)で確認する

### 配布規則

- 成果物名は`item-name-copy-<mod-version>+<loader>-mc<minecraft>.jar`とする
- 配布先では確認済みのMinecraft版とLoaderだけを成果物へ紐付ける
- ビルド成功とゲーム内動作確認を区別し、未確認の成果物を対応済みとして扱わない
- Loaderごとの成果物を名前と配布metadataの両方で識別し、別LoaderのJarを取り違えない

## 参考資料

- [Stonecutter](https://codeberg.org/stonecutter/stonecutter)
- [Fabric Documentation](https://docs.fabricmc.net/)
- [NeoForge User Guide](https://docs.neoforged.net/user/docs/)
- [Forge Documentation](https://docs.minecraftforge.net/)
