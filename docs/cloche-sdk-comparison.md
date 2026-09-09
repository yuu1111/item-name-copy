# Clocheと理想のMinecraft互換SDKの比較

## 目的

この文書では、コメントによるソース前処理を使わず、Minecraft版とLoaderをまたいでコードを共有する基盤として、[Cloche](https://github.com/terrarium-earth/cloche)と理想のSDKを比較する
理想のSDKは、Minecraft版、Loader、実行環境などの軸に応じてSharedソース断片を組み合わせ、各ターゲットを独立したJavaまたはKotlinのコンパイル単位として検証するGradle基盤を指す

ItemNameCopyの対応ターゲットは`versions/<minecraft>-<loader>/gradle.properties`を正本とし、この文書へ一覧を複製しない

## 結論

Clocheは、複数のMinecraft版とLoaderを同一プロジェクトで扱い、共通API、ターゲット別ソース、JavaのExpect/Actual、テスト、実行構成、公開variantを提供するため、理想のSDKに最も近い既存基盤である

一方、理想のSDKで中心となるのは、任意のターゲット集合へMinecraft依存ソースを共有できる中間Shared層である
Clocheの公開ドキュメントでは、全ターゲット向けの共通APIと個別ターゲットのソース構成は示されているが、`mc-data-components`や`legacy-forge`のような任意の中間Shared層をDAGとして定義する方法は確認できない

新しいSDKを直ちに一から実装するのではなく、Cloche上でItemNameCopyの一部を構成する試作を行い、中間Shared層と対象Toolchainを拡張できるか判断する
Clocheの内部モデルを再利用できる場合は拡張を優先し、ソース共有モデルまたは必要なToolchainと両立しない場合は独立したGradle Pluginとして実装する

## 比較

| 観点 | Cloche | 理想のSDK |
| --- | --- | --- |
| ターゲット | Minecraft版とLoaderの組み合わせ | Minecraft版、Loader、環境、Mappingを持つ組み合わせ |
| 共通コード | 全ターゲットの共通APIとターゲット別ソース | 任意のターゲット集合へ共有するソース断片 |
| 中間共有 | 公開例では明示されていない | 名前付きShared断片をDAGで構成 |
| バージョン差分 | ターゲット別ディレクトリとExpect/Actual | Shared断片とターゲット固有断片で表現 |
| コメント前処理 | 必要としない | 採用しない |
| Java連携 | `@Expect`と`@Actual`を提供 | InterfaceまたはExpect/Actualを選択可能 |
| Kotlin連携 | Kotlin Multiplatform機能を提供 | JavaとKotlinで同じSharedグラフを利用 |
| Minecraft APIの検証 | 共通APIを生成し、ターゲットごとにコンパイル | Sharedソースをすべての利用ターゲットでコンパイル |
| Loader統合 | Clocheが各ターゲットを構成 | Toolchain Adapterが構成 |
| テスト | Source Setと構成ごとのテストを提供 | Shared Contract Testを子ターゲットすべてで実行 |
| 実行構成 | ターゲットごとのrunを生成 | ターゲットごとのrunを生成 |
| メタデータ | Loader別メタデータを生成 | Sharedリソースとターゲット値から生成 |
| ライブラリ公開 | 消費側に合うGradle variantを選択 | Minecraft版、Loader、環境に合うvariantを選択 |
| Toolchain範囲 | 採用前に実対象で検証が必要 | ItemNameCopyの全対象を要件とする |

## ソースモデル

### Cloche

Clocheは一つのGradleプロジェクトに複数ターゲットを定義し、ターゲット名をディレクトリへ対応させる
公開例では次のような構成を取る

```text
src/
  fabric/
    1.20.1/
    1.21.1/
  neoforge/
    1.21.1/
```

複数ターゲットがある場合は、それらに共通するAPIから共通Jarを生成する
LoaderやMinecraft版に固有の処理はターゲット別Source Setへ配置し、Javaでは[jvm-multiplatform](https://github.com/terrarium-earth/jvm-multiplatform)の`@Expect`と`@Actual`を利用できる

このモデルは、全体で共有できる処理と完全に個別な処理を分ける用途に適している
複数だがすべてではないターゲットに同じMinecraft依存ソースを共有する場合、公開されたDSLだけで中間層を表現できるか検証が必要となる

### 理想のSDK

理想のSDKは、ターゲットをSource Setの名前ではなくShared断片の組み合わせとして定義する

```text
shared/
  common/
  client/
  mc-legacy-screen/
  mc-modern-screen/
  mc-data-components/
  fabric-input/
  forge-input/
  neoforge-input/

targets/
  1.20.1-fabric/
  1.21.1-fabric/
  1.21.1-neoforge/
```

各ターゲットは必要なShared断片を選択する

```kotlin
minecraftSdk {
    shared("common")
    shared("client") {
        dependsOn("common")
    }
    shared("mc-data-components") {
        dependsOn("client")
    }
    shared("fabric-input") {
        dependsOn("client")
    }

    target("1.21.1-fabric") {
        minecraft = "1.21.1"
        loader = fabric("...")
        use("mc-data-components", "fabric-input")
    }
}
```

Shared断片は一度だけBinaryへコンパイルするのではなく、利用する各ターゲットのMinecraftとLoaderのClasspath上で個別にコンパイルする
あるShared断片が一つのターゲットでしか成立しない場合は、そのターゲットのコンパイルが失敗し、共有範囲の誤りを検出できる

## 理想のSDKで必要な契約

### Sharedグラフ

- Shared断片は他のShared断片へ依存できる
- 依存関係は循環を許可しない
- ターゲットは選択した断片と推移的な依存先を同じコンパイル単位へ含める
- 同じ完全修飾名を複数の選択断片が提供した場合は、暗黙に上書きせず構成エラーにする
- Java、Kotlin、resources、Mixin、Access Widener、Access Transformerの共有範囲を個別に宣言できる

### ターゲット

- Minecraft版、Loader、Loader版、Mapping、Java Toolchainをターゲットが所有する
- Fabric、Forge、NeoForgeごとのGradle Plugin差分をToolchain Adapterへ閉じ込める
- 一つのGradle実行で任意のターゲットだけをビルドできる
- 全ターゲットのビルドとテストを集約するLifecycle Taskを提供する
- 対応対象は宣言から生成し、文書やCIへ別の一覧を持たせない

### API差分

- 共通ロジックは通常のInterfaceとコンストラクター注入だけでも記述できる
- 同じ完全修飾名の型やStatic Methodが必要な場合に限りExpect/Actualを利用する
- 実装がないExpectと、対応するExpectがないActualをコンパイル前に検出する
- RuntimeでMinecraft版を判定せず、対象Jarへ必要な実装だけを含める
- SDK固有AnnotationやRuntimeライブラリを利用者へ強制しない構成を選択できる

### テスト

- Minecraft非依存のShared断片は通常のJUnitで一度検証できる
- Minecraft APIへ依存するShared断片は利用する全ターゲットでコンパイルする
- AdapterのContract Testを各ターゲットで再利用できる
- クライアント統合テストは対象ターゲットを明示して実行する
- ビルド成功、クライアント起動、操作結果を別の検証段階として扱う

### 公開

- Gradle Module MetadataへMinecraft版、Loader、実行環境の属性を付与する
- 消費側は同じDependency座標から互換variantを解決できる
- 互換variantがない場合は不適切なJarへFallbackせず、依存解決を失敗させる
- Sources Jarには対象variantで実際に利用したShared断片と固有断片を含める

## Clocheで先に検証する項目

Clocheの採否は機能一覧ではなく、ItemNameCopyの実対象で次を満たすかによって判断する

1. Minecraft依存の中間Shared層を、選択した複数ターゲットだけへ再利用できる
2. 同じSharedソースを異なるMinecraft Classpathで個別にコンパイルできる
3. Minecraft 1.12.2を含む対象Toolchainを同一のターゲットモデルへ統合できる
4. Fabric、Forge、NeoForgeの対象範囲で開発用runと配布Jarを生成できる
5. Mojang mappingsを利用できない対象と、非難読化された対象を同じ公開モデルで扱える
6. Mixin、Access Widener、Access Transformerを対象ごとに正しく適用できる
7. 異なるJava Toolchainを同じGradle Buildで選択できる
8. IDE上でShared断片の参照解決とターゲット固有コードの編集が成立する
9. 全ターゲットを構成した状態で、Configuration時間とIDE Sync時間が運用可能な範囲に収まる
10. 公開variantが消費側のMinecraft版とLoaderに従って一意に解決される

## 選択肢

### Clocheをそのまま採用する

全ターゲット共通とターゲット固有の二段階で十分な場合に適する
独自のToolchain管理、run生成、メタデータ生成、variant公開を保守せずに済む

中間Shared層を使わないため、同じ互換区間に属する多数のターゲット間でソースが重複しないことを確認する必要がある

### Clocheを拡張する

ClocheのVirtual Source Setとターゲットモデルが、任意の中間Shared層を表現できる場合に適する
既存のLoader統合と公開variantを再利用し、追加機能をSharedグラフの宣言と検証へ限定できる

拡張点が公開APIではない場合や、Clocheの更新ごとに内部実装へ追従する必要がある場合は、独立Pluginより保守負担が大きくなる可能性がある

### 独立したSDKを実装する

ClocheのSource Setモデルでは中間Shared層を表現できない場合、またはItemNameCopyが必要とするToolchainを同一モデルへ追加できない場合に選択する
独自SDKはMinecraft APIの抽象化ライブラリから始めず、ターゲットとShared断片を構成するGradle Pluginを最初の提供単位とする

```text
SDK Gradle Plugin
  ├─ Target model
  ├─ Shared source graph
  ├─ Toolchain adapters
  ├─ Test and run orchestration
  └─ Variant publication

Optional libraries
  ├─ Compatibility API
  ├─ Loader adapters
  └─ Client test kit
```

この境界により、利用するModはSDKの互換APIを採用せず、Sharedソース合成だけを利用することもできる

## 試作の完了条件

ItemNameCopyのClipboard、Component、入力処理から一つの互換境界を選び、コメント分岐を使わず次の構成を作る

- Minecraft APIに依存しない共通処理
- 複数のMinecraft版で共有する実装断片
- Loader間で共有する実装断片
- 一つのターゲットだけに必要な固有断片
- 各断片を組み合わせた複数ターゲットのビルド
- 同じContract Testのターゲット別実行

試作では、ソース重複量、DSLの追加量、IDEの参照解決、Gradle構成時間、ターゲット追加時の変更箇所を比較する
この結果から、Clocheの採用、Clocheへの拡張、独立SDKの実装を決定する

## 参考資料

- [Cloche](https://github.com/terrarium-earth/cloche)
- [jvm-multiplatform](https://github.com/terrarium-earth/jvm-multiplatform)
- [Multisource](https://github.com/lukebemishprojects/Multisource)
- [Prism](https://prism.leclowndu93150.dev/)
- [Modstitch Multiloader](https://github.com/isXander/modstitch-toolkit/tree/main/modstitch-multiloader)
- [Stonecutter](https://github.com/stonecutter-versioning/stonecutter)
