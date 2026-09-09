# item-name-copy

## プロジェクト構成

`src/main/java`と`src/main/resources`には、Fabric・Forge・NeoForgeで共有するMod実装を置く
`//? if <1.19`などのStonecutterディレクティブを維持し、対象は`versions/<Minecraft版>-<Loader>/gradle.properties`で定義する
生成される`versions/**/src`は編集しない

Loader別のGradle設定は`build.*.gradle.kts`、共通のビルド検証は`gradle/`に置く
入力処理とJUnitテストは`core/`、再利用するクライアントテスト基盤は`minecraft-client-testkit/`、ゲーム内テストは`tests/client/`、Docker/X11テストは`tests/e2e/`に置く
文書、画像、配布メタデータはそれぞれ`docs/`、`assets/`、`distribution/`で管理する

## ビルド・テスト・開発コマンド

Java 25でGradle Wrapperを実行する
対象ごとのJava Toolchainは必要に応じてGradleが取得する

- `./gradlew '-Ptarget=1.21.1-fabric' :1.21.1-fabric:build`: 一つの対象をビルドして成果物を検証する
- `./gradlew '-Ptarget=1.21.1-fabric' :core:test`: JUnit 5の高速な単体テストを実行する
- `./gradlew buildAndCollect`: 全対象をビルドし、検証済みJarを`build/libs/`へ収集する
- `./gradlew '-Ptarget=1.21.1-fabric' :1.21.1-fabric:runClient`: 開発用クライアントを起動する
- `./scripts/Invoke-ClientTests.ps1 -Target '1.21.1-fabric'`: 指定対象のゲーム内テストを実行する 前提条件と再開方法は`tests/README.md`を参照する
- `docker compose -f tests/e2e/compose.yaml build`と`docker compose -f tests/e2e/compose.yaml run --rm client`: X11経由の外部入力を検証する

## コーディング規約と命名

JavaとKotlinは4空白でインデントし、開き波括弧を宣言と同じ行へ置く
既存のimport順を維持し、型には`UpperCamelCase`、メソッドとフィールドには`lowerCamelCase`、定数には`UPPER_SNAKE_CASE`を使う
パッケージは`com.github.yuu1111`配下に置く
FormatterやLinterは設定されていないため、周辺コードへ合わせ、互換分岐を必要な範囲に限定する

## テスト方針

JUnitクラスは`*Test`、テストメソッドは`heldKeyCannotCopyAgainUntilReleased`のように振る舞いを表す名前にする
クライアントテストのケース名にはkebab-caseを使う
Loader非依存の処理には`core`の単体テスト、Minecraftとの統合にはクライアントテストを追加する
数値のカバレッジ基準はないが、回帰修正とバージョン境界の変更では影響する対象を実行する

## コミットとPull Request

コミット件名は`Fix legacy Forge startup and metadata verification`のように、簡潔な命令形のsentence caseとし、末尾に句読点を付けない
一つのコミットには関連する変更だけを含める
Pull Requestには変更内容と互換性への影響、実行した対象とコマンド、関連Issueを記載する
見た目を変更した場合だけスクリーンショットを添付し、Build workflowを成功させる
クライアントテストやE2Eテストを省略した場合は明記する
