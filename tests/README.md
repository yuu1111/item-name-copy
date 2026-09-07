# クライアント自動テスト

- Java 25でGradle Wrapperを実行する
- Minecraft 1.21.1 Fabricの実クライアントを起動し、LLMや画像判定を使わずに検証する
- デスクトップ環境とOpenGLが必要で、LinuxのCIではXvfbとMesaを使う
- テスト中はOSのクリップボードを使い、終了時に元の文字列へ戻す

```powershell
./gradlew '-Ptarget=1.21.1-fabric' :1.21.1-fabric:runClientTest
```

## 実行と判定

- テスト用Modは専用ソースセットに置き、配布Jarには含めない
- Fabric APIを追加せず、本体と同じリソース読み込み条件で検証する
- 専用の実行ディレクトリに、シード値1、構造物なしのスーパーフラットワールドを毎回作成する
  - ピースフル、チートON、Mob生成・昼夜変化・天候変化OFFで開始する
  - サバイバルの検証後にクリエイティブへ切り替える
- コンテナ画面の描画処理でスロットへカーソルを合わせ、KeyboardHandlerへキーイベントを渡す
  - OSのキー操作は合成せず、入力欄のCtrl判定だけをテスト中の修飾キー状態へ置き換える
  - 本体のコピー処理、Mixin、クリップボードへの書き込み、翻訳通知は実装をそのまま使う
- コピー内容、空スロット、カスタム名、リピート抑止、修飾キー、レシピ検索、クリエイティブ検索、翻訳通知を検証する
- 結果は`versions/1.21.1-fabric/build/reports/client-test/`の`results.json`と`TEST-client.xml`へ出力する
- 失敗、レポート欠落、テスト0件はGradleタスクの失敗とし、起動を含め5分でタイムアウトする
- 実行ディレクトリと生成ワールドは`versions/1.21.1-fabric/build/client-test/run/`に残る

## 検証範囲

- OSからの物理キー入力、左右Ctrlの区別、画面の見た目は別途Computer Useまたは手動で確認する
- チェスト・かまど・金床、他Modの画面、マルチプレイ、他のMinecraft版とLoaderはこのテストの対象外
- [Client tests workflow](../.github/workflows/client-test.yml)は手動起動でき、ログとレポートを保存する
