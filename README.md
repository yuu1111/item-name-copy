# ItemNameCopy

インベントリやコンテナ画面でアイテムへカーソルを合わせ、設定したキーで表示名をクリップボードへコピーするクライアント専用Mod
コピーに成功すると、コピーした名前をアクションバーへ表示する

## 機能

- インベントリ、チェスト、作業台、かまど、金床などのスロットに対応
- 日本語名、翻訳された名前、金床などで付けた名前を表示どおりにコピー
- コピー操作をMinecraftの操作設定にある`ItemNameCopy`カテゴリから変更可能
- 既定は`Ctrl+C`で、Ctrl、Shift、Altまたは修飾キーなしの組み合わせに対応
- 長押しでは一度だけコピー
- 検索欄や金床の名前入力欄では、選択した文字列のコピーを優先
- 標準のコンテナ画面を継承するMod画面に対応
- EMI 1.18.2以降とREI 1.17以降のアイテム表示欄やレシピの疑似スロットに対応
- サーバーへの導入と、Fabric APIやArchitectury APIなどの追加Modは不要

## 導入

利用するMinecraft版とLoaderに完全一致するJarを一つ選び、クライアントの`mods`フォルダーへ入れる
Fabric、Forge、NeoForgeのJarは相互に置き換えられない

Jar名は次の形式になる

```text
item-name-copy-<Mod版>+<Loader>-mc<Minecraft版>.jar
```

ビルド対象は[versions](versions)で定義している
定義されていることやビルドが成功することだけでは、ゲーム内での動作確認が完了していることを意味しない
収集した成果物の`verification-manifest.json`で`runtimeVerification: pending`となっているJarは、配布前にゲーム内で確認する

## 使い方

空でないスロットへカーソルを合わせ、既定の`Ctrl+C`を押す
Minecraftの「設定」→「操作設定」→「キー割り当て」にある`ItemNameCopy`カテゴリから、修飾キーを含むショートカット全体を変更できる
空のスロットや対象外の画面ではクリップボードを変更せず、成功通知も表示しない

コピーするのは表示名のプレーンテキストだけで、装飾、アイテムID、個数、Lore、NBT、Data Componentは含めない
名前の前後の空白や改行はそのまま残す

ワールド画面で手に持っているアイテムと、コピー形式用の独自設定画面には対応していない
機能候補は[未実装の機能とTODO](docs/todo.md#未実装の機能)にまとめている

## ソースからビルド

Java 25でGradle Wrapperを実行する
Minecraftごとのコンパイル用JavaはToolchainで取得するため、初回はネットワーク接続が必要になる

全対象をビルドし、Jarと検証マニフェストを`build/libs`へ収集する

```powershell
./gradlew buildAndCollect
```

一つの対象だけをビルドする場合は、`versions`にあるNode名を指定する

```powershell
./gradlew '-Ptarget=1.21.1-fabric' :1.21.1-fabric:build
```

## 動作確認

開発用クライアントを起動する場合も、確認するNode名を`target`とタスクへ指定する
`runClient`はソースをコンパイルして起動するため、Jarを手動で配置する必要はない

```powershell
./gradlew '-Ptarget=1.21.1-fabric' :1.21.1-fabric:runClient
```

自動クライアントテストの実行方法と検証範囲は[tests/README.md](tests/README.md)にまとめている
マルチバージョン構成、入力処理、互換境界の設計は[設計と対応方針](docs/minecraft-copy-item-name-mod.md)を参照する
配布前の手動確認は[リリース前チェックリスト](docs/todo.md#リリース前チェックリスト)にまとめている

## ライセンス

[MIT](LICENSE)
