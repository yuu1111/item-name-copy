# クライアント自動テスト

- Java 25でGradle Wrapperを実行する
- ビルド対象の実クライアントを順番に起動し、LLMや画像判定を使わずに検証する
- PowerShell 7と各対象のJavaが必要
- デスクトップ環境とOpenGLが必要で、LinuxのCIではXvfbとMesaを使う
- テスト中はOSのクリップボードを使い、終了時に元の文字列へ戻す

```powershell
./scripts/Invoke-ClientTests.ps1
./scripts/Invoke-ClientTests.ps1 -Target '1.21.1-fabric'
./scripts/Invoke-ClientTests.ps1 -Resume
```

## 実行と判定

- 汎用ランナーは`minecraft-client-testkit`、ItemNameCopyのクライアントテストは`tests/client`でビルドし、配布Jarには含めない
- 対象一覧は`Get-BuildMatrix.ps1`から読み、`-Target`で絞り込む
- `-Resume`はソースとビルド設定のハッシュが一致する成功結果だけを再利用する
- Fabric APIを追加せず、本体と同じリソース読み込み条件で検証する
- 専用の実行ディレクトリに、シード値1、構造物なしのスーパーフラットワールドを毎回作成する
  - ピースフル、チートON、Mob生成・昼夜変化・天候変化OFFで開始する
  - サバイバルの検証後にクリエイティブへ切り替える
- コンテナ画面の描画でホバー対象を確認し、KeyboardHandlerへキーイベントを渡す
  - 1.12.2ではForgeのGUIキーイベントとLWJGLの入力状態を使う
  - OSのキー操作は合成せず、ゲーム内の入力判定へテスト中の状態を渡す
  - 本体のコピー処理、Mixin、クリップボードへの書き込み、翻訳通知は実装をそのまま使う
- コピー内容、空スロット、カスタム名、リピート抑止、キー登録と再割り当て、Ctrlなしの操作、レシピ検索、クリエイティブ検索、翻訳通知を検証する
- ワールドの生成方式、難易度、チート許可と3つのゲームルールは、統合サーバーの実値を検証する
- 個別結果は`versions/<対象>/build/reports/client-test/`の`results.json`と`TEST-client.xml`へ出力する
- 集計結果は`build/runtime-verification/matrix.json`へ出力し、各対象の実行ログへのパスも保存する
- 失敗、レポート欠落、テスト0件はGradleタスクの失敗とし、起動を含め5分でタイムアウトする
- 実行ディレクトリと生成ワールドは`versions/<対象>/build/client-test/run/`に残る

## 検証範囲

- [Dockerの外部入力検証](e2e/README.md)では、別スイートでX11経由のホバーとCtrl+Cを確認する
- Windowsの物理キー入力と、操作設定に表示されるカテゴリの見た目は別途Computer Useまたは手動で確認する
- チェスト・かまど・金床、他Modの画面、マルチプレイはこのテストの対象外
- [Client tests workflow](../.github/workflows/client-test.yml)は手動起動でき、ログとレポートを保存する
