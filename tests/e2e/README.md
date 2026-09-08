# Dockerによる外部入力検証

- Linuxコンテナ内にXvfbの専用画面を作り、xdotoolのXTEST入力で操作する
- ホストの画面、X11ソケット、クリップボード、Dockerソケットはコンテナへ渡さない
- LinuxではDocker EngineとCompose、WindowsではLinuxコンテナを実行できるDocker環境を使う
- 標準設定はCPU 2、メモリ上限6GB、共有メモリ256MB
- 描画はMesaのソフトウェア描画を使い、GPUの割当てを前提にしない

## 実行

リポジトリのルートで実行する

```sh
docker compose -f tests/e2e/compose.yaml build
docker compose -f tests/e2e/compose.yaml run --rm client
```

- 標準コマンドはGLFWの入力検証
  - マウスの座標、Ctrl+Cの修飾値、キー解放、日本語を含むクリップボードを検証する
  - Minecraft内のアイテムコピーを検証するシナリオとは分ける
- 依存JarはComposeの名前付きボリュームへ保存する
- 結果、操作と応答、画面画像、描画環境は`build/docker-e2e/`へ保存する
- 受入条件を満たさない場合は終了コードを非0にする
- ソース変更後はイメージを再ビルドする ホストのビルド出力やGradleキャッシュは持ち込まない

## アプリとの接続

- `runtime/session.sh`は画面とドライバーを起動し、指定されたコマンドの終了コードを返す
- アプリ固有の準備と状態検証はアダプターが持ち、共通ランタイムへゲーム名や画面構造を埋め込まない
- `runtime/serve-x11.sh`はファイル経由の操作要求を受け、`runtime/x11-driver.sh`へ渡す
- ドライバーの応答は操作コマンドの成否を示す アプリ側で受信した状態を別に検証する
- Java用の`java/dev/e2e/driver/FileDriver.java`は接続例であり、通信自体にJavaやLLMを必要としない

### 操作要求

- `$E2E_CONTROL/request.json`へ一時ファイルからatomic renameで発行する
- `id`は1以上の単調増加する整数、`pid`はコンテナ内の対象プロセスID
- 1回に1要求とし、同じ`id`の`response.json`を受け取ってから次の要求を発行する
- ドライバーは対象PIDの可視ウィンドウがちょうど1つの場合だけ操作する
- 座標は対象ウィンドウのクライアント領域のピクセル値

```json
{"id":1,"pid":123,"action":"hover","x":120,"y":100}
```

| action | 追加フィールド | 操作 |
| --- | --- | --- |
| hover | x、y | 対象内へマウスを移動 |
| hotkey | keys | `ctrl+c`などのキーを押下・解放 |
| clipboard | expected | X11クリップボードの文字列を照合 |
| screenshot | なし | 対象ウィンドウのPNGを保存 |

- 応答は`id`、`exitCode`、`message`を持つ
- 各操作は15秒でタイムアウトし、Java側の応答待機は20秒で失敗する
- 実行コマンド全体の制限は600秒で、`E2E_TIMEOUT_SECONDS`で変更できる
- キー操作は`--window`によるSendEvent配送を使わず、専用画面内で対象を前景にして送る
- Java用の`submit`と`poll`は非同期の進行に使う 検証用の`call`は待機中もGLFWのイベント処理を進める

## 検証範囲

- Linuxの入力経路を検証し、Windowsの入力経路の成功として再利用しない
- GLFWの検証はアプリのコピー処理を代用しない
- MinecraftのJavaテスト基盤との接続は、基盤の分割作業後に追加する
- キャッシュの識別にはアプリの実行内容に加え、OS、Java、ドライバー、シナリオ、イメージの識別情報を含める
- 全対象への展開前にMinecraft代表版で描画負荷と入力の成立を確認する
