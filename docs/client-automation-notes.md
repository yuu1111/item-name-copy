# Minecraftクライアント自動化の知見

## 目的と検証境界

- MinecraftのバージョンとLoaderをまたぐ実クライアント検証を、LLMや画像判定なしで再実行できるライブラリへ分離するための設計メモ
- 汎用ランナーは[`minecraft-client-testkit`](../minecraft-client-testkit)、ItemNameCopy固有の実行基盤は[`tests/client-harness`](../tests/client-harness)に置き、操作と判定の対象は[`tests/README.md`](../tests/README.md)で定義する
- Java agentでクライアントのtickへ処理を追加し、実際の画面、入力コールバック、本体のMixinやイベント、OSのクリップボードを使う
- 物理キー、左右Ctrl、画面の見た目、マルチプレイまで検証したことにはならない
- 開発起動の成功と、配布Jarを通常のLauncherで読み込んだ成功は別の証拠として扱う
- 全対象の対応状況は手書きの表に複製せず、実行結果のマトリクスと個別レポートを参照する

## 起動とクラスローダー

- Fabric、ForgeGradle、ModDevGradle、NeoGradleでは、実行ディレクトリやJVM引数の設定箇所が異なる
  - `JavaExec.workingDir`だけを指定しても、Loader側のrun設定で上書きされる場合がある
  - テスト用のoptionsと生成ワールドが、本当に専用ディレクトリへ保存されたか確認する
- ForgeGradle 7ではクラス出力とリソース出力が別々のMod候補になる構成がある
  - 本実装では`net.minecraftforge.gradle.merge-source-sets=true`を設定した
  - コンパイル成功やMod名の表示だけで、本体クラスとリソースが同じModとして読み込まれたと判定しない
  - 過去の出力ディレクトリが残る場合もあるため、起動時に使われたファイルの場所を記録する
- agentのASMをMinecraftのクラスパスへ混ぜない
  - 現在はruntimeをbootstrapへ追加し、ASMを含むtransformerは親を持たない専用クラスローダーで読み込む
  - runtimeはMinecraft型へ静的に依存せず、tickで受け取ったクライアントのクラスローダーを使う
- LWJGL 2の古いクラスへ新しい形式のStackMap frameを挿入すると、ASMが変換を拒否する
  - 1.12.2では`Class versions V1_5 or less must use F_NEW frames`が発生した
  - classfileの世代を確認し、古いクラスには不要なframeを追加しない
  - transformerの失敗が対象クラスの通常ロードへ流れても、入力差し替えが成功したことにはならない
- Javaの実行バージョンとMixinの`compatibilityLevel`は同じ値とは限らない
  - Forge 26.2のMixin 0.8.7は`JAVA_25`を認識せず、列挙値は`JAVA_21`までだった
  - 宣言値を下げた場合も、実際にクラスを読み込めるか別途起動で確認する

## 画面探索と操作

- 画面やウィジェットの具象クラス名だけで種類を判定しない
  - ワールド名やシードの入力欄が匿名クラスになっている版がある
  - `EditBox`などの祖先型まで調べ、非表示の入力欄を除外する
- ラベルが一致した要素を、そのまま押せるボタンとみなさない
  - 新しいワールド作成画面では「Generate Structures」のラベルとON/OFFボタンが別要素だった
  - 隣接要素を探す現在の実装は暫定対応とし、汎用化ではレイアウトの関係と操作可能性を分けて扱う
- ボタンの操作APIも世代で変わる
  - 引数なしの`onPress`、入力オブジェクトを取る`onPress`、旧`actionPerformed`、タブ管理API、画面の`mouseClicked`がある
  - 汎用的なフォールバックを増やす前に、対象ウィジェットの契約を確認する
- レシピブックは内部の表示フラグを直接切り替えない
  - 古い版では表示に必要な初期化が抜け、次の描画でクラッシュした
  - 実際のレシピボタンを操作し、ゲーム本来の初期化を通す
- ワールド作成は非同期で、作成ボタンの後に追加確認画面が出る場合がある
  - NeoForge 1.20.5では`ConfirmScreen`のYes/Noで停止した
  - 追加確認はテスト用ワールドの作成段階に限定して処理し、任意の確認画面を自動承認しない
- ホバー対象は画面を描画した後に更新される
  - マウス座標を更新したtickで即座にコピーせず、実際の`hoveredSlot`が目的のSlotと一致するまで待つ
  - 固定時間の待機だけに頼らず、条件付きの待機と期限を使う

## キー入力とフォーカス

- 検証したい境界へ入力を渡す
  - GLFW世代はKeyboardHandlerのコールバック、1.12.2はForgeのGUIキーイベントとLWJGLの読み取り状態を使う
  - 本体のコピー関数を直接呼ぶと、入力欄優先、イベント消費、Mixinの接続を検証できない
- キーイベントの修飾フラグだけでは、Minecraftの静的なCtrl判定は変わらない
  - 入力欄のコピー処理が参照するCtrl状態にもテスト中の値を渡す必要がある
  - 入力状態の上書きは操作中だけ有効にし、例外時にも解除する
- 入力欄自身のフォーカスと、親画面のフォーカス経路は別に管理される
  - 1.19.4とNeoForge 1.20.2では検索欄をfocusedにするだけでは文字列コピーが届かなかった
  - 親画面のfocused childをレシピブックに設定すると、実際のキー配送で検索文字列をコピーできた
- 押下、リピート、解放を分ける
  - 1回の成功だけでは押しっぱなしによる連続コピーを検出できない
  - Ctrlなし、Ctrl+Shift、Ctrl+Alt、Ctrl+Superも別の入力条件として検証する
- クリップボードはOS全体で共有される
  - 同じデスクトップ上のクライアントを並列実行しない
  - 操作前にsentinelを書き、期待する変更または未変更を確認する
  - 終了時は元の文字列へ戻し、空文字列も復元対象にする
  - 強制終了時の復元や文字列以外のクリップボード形式は、別途設計が必要

## ワールド設定と成功判定

- コマンドの送信完了を、設定成功とみなさない
  - 26.2で旧ゲームルール名のコマンドがエラーになっていたが、当初のテストは送信後の値を確認していなかった
  - コマンド実行後に統合サーバーの状態を読み、設定値をassertする
  - UI設定、コマンド、結果の状態はそれぞれ別の段階として扱う
- ゲームルールは1.21.11から名前空間付きの名前へ変更された

  | 旧名 | 1.21.11以降 |
  | --- | --- |
  | `doMobSpawning` | `minecraft:spawn_mobs` |
  | `doDaylightCycle` | `minecraft:advance_time` |
  | `doWeatherCycle` | `minecraft:advance_weather` |

  - 変更の根拠は[Minecraftの公式変更履歴](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-11)
- クライアント側のワールド情報だけでは、生成方式やチート許可を取得できない版がある
  - 統合サーバーのworld dataやlevel settingsを参照する
  - `getIntegratedServer`、`getSingleplayerServer`、`getGenerator`、`getTerrainType`、`worldGenSettings`などの差分を吸収する
- 通知は表示される文字列を比較する
  - 1.12.2では内部の通知文字列に`§r`などの装飾コードが含まれる
  - 装飾を除く比較と、クリップボードへ入れる名前の完全一致を混同しない
- テストが始まらなかった起動を成功とみなさない
  - 終了コード0でも、警告画面を閉じただけならレポートが存在しない
  - 対象ID、入力ハッシュ、予定件数、成功件数、失敗件数を検証する
  - レポート欠落、0件、途中終了、別対象のレポートを失敗にする

## リソースとLoaderメタデータ

- Fabric APIを追加すると、製品本来のリソース読み込み条件が変わる場合がある
  - テストの都合で依存を追加し、翻訳が読み込まれる問題を隠さない
  - 翻訳キーがそのまま表示される場合は、翻訳文字列の有無とリソースパックの登録を分けて調べる
- `pack.mcmeta`はMinecraftごとの形式を使う
  - 新しい版の`min_format`、`max_format`と、旧版の`pack_format`を区別する
  - 値は対象ゲームのメタデータから取得し、生成処理とJar検査を同じ定義へ結び付ける
- Gradleの入力プロパティ名を、用途の異なる展開処理で共用しない
  - Mixin用とModメタデータ用に同じ`java`入力を登録すると、一方の値を変更しても他方に上書きされ、`processResources`が更新不要と判定される場合がある
  - Mixinには`mixin_java`を使い、設定変更で出力が更新されることを確認する
- NeoForgeのMinecraft版だけからFML世代を推測しない
  - 1.20.3の`20.3.8-beta`はFML 1.0.16で、依存関係に`mandatory`を要求した
  - `type="required"`を出力すると起動前にModファイルとして拒否された
  - 実際に解決されたLoaderの依存情報も確認する
- アイコンのメタデータ名にも世代差がある
  - NeoForge 26.2では`logoFile`が警告画面を発生させ、正方形アイコンには`iconFile`を使う
  - 26.1のFML 11.0.5では`logoFile`が使われるため、新しい名前を一律に適用しない
  - 警告画面を自動で飛ばすだけでは、製品側の警告を修正したことにならない

## 汎用ライブラリへ分離する責務

- OS操作には保守されている外部ドライバーを使う
  - Cua Driverなどを交換可能なアダプターとして扱い、マウス捕捉、フォーカス制御、ウィンドウ探索、画面取得の独自実装を増やさない
  - LLMへの依存は持たず、CLIまたはSDKから決められたシナリオを実行する
  - Minecraft固有の画面識別、座標の取得、状態検証はゲーム側のアダプターに残す
  - ドライバーの操作成功だけで判定せず、ゲーム側の状態で結果を確認する
  - 背景操作に対応しない場合は停止し、ユーザーのマウスを使う前景操作へ自動で切り替えない
- 実行OSは検証結果の識別情報に含める
  - マウス捕捉、キーの修飾判定、クリップボード、ウィンドウのフォーカスにはOS差がある
  - Linuxの成功をWindowsの成功として再利用しない
  - Windows上の背景操作を優先して適合確認し、隔離したLinux環境だけでWindowsの操作干渉が解決したと判定しない
  - 同一デスクトップの背景操作と、別OS環境への隔離は別の方式として扱う
- 外部ドライバーの採否は対象ゲームで確認する
  - [Cua Driverの対応範囲](https://cua.ai/docs/reference/cua-driver/platform-support)はアプリと操作の種類ごとに異なり、Windows対応という記載だけでMinecraftの背景入力を保証しない
  - [Windows sandboxでMinecraftを動かす公式例](https://cua.ai/docs/how-to-guides/sandbox/minecraft)は存在するが、同じWindowsデスクトップ上での背景入力を証明する例ではない
  - 採用前にカーソル、フォーカス、クリップボードへの干渉と、ゲーム側で入力が受理されたことを確認する
- 起動アダプター
  - Loaderのrun設定、Java選択、専用ディレクトリ、agent注入、ログ回収を担当する
- 実行環境と機能の検出
  - クラスローダー、classfile、利用可能な画面API、入力APIを調べ、対応しない機能を明示する
- 画面操作アダプター
  - 要素探索、表示状態、フォーカス、クリック、ホバーを扱い、ゲーム固有のシナリオから分離する
- 非同期ステップ実行
  - tickを進行源に、待機条件、期限、例外、後処理を管理する
- シナリオと事後条件
  - ワールド作成やコピー操作を定義し、送った操作ではなく結果の状態を検証する
- 証拠と結果の管理
  - JSONとJUnit、対象識別、入力ハッシュ、使用した出力パス、予定件数、失敗段階を保存する
  - 診断中の変更後は古い結果を最終検証へ流用せず、確定した入力で再実行する

## 追加設計が必要な点

- 再実行の判定を対象ごとの実行内容へ変更する
  - 現状は広いソース範囲を一つのハッシュへまとめるため、旧テストの削除などでも全対象が無効になる
  - 対象のクラス、リソース、解決済み依存関係、Java・OS・入力ドライバー、テストシナリオを識別する
  - 実行内容が変わらなければ、文書、旧方式、無関係な対象の変更で成功結果を破棄しない
  - テスト基盤の変更による再確認と製品の変更による再確認を区別し、無効化理由を表示する
  - 根拠の足りない旧レポートを、新方式で成功した結果として書き換えない
- Reflectionの複数名フォールバックは、メソッド不存在と呼び出し先内部の失敗を区別する
- 起動時にフックが実際に適用されたことを確認し、差し替え失敗を最初の操作まで持ち越さない
- ビルド中や検証中にソースが変更された場合の結果を無効化する
- 入力ハッシュには実行スクリプト、対象一覧の生成処理、起動設定も含める
- 強制終了に備えたクリップボード復元と、クライアントプロセスの所有権を管理する
- 配布Jarによる起動検証を、開発クラスパスによる検証と別モードで提供する
- 新しい境界版の対応を追加したら、既存の代表版でフォーカスや入力動作の回帰を確認する

## 外部ドライバーの比較と選定

- 目的はLLM非依存の自動E2Eテスト
  - シナリオ、待機条件、合否判定はコードで固定し、推論サービスや画像を見たLLMの判断を必要としない
  - Javaの計装機構はclient harness内部に閉じ、シナリオや汎用ランナーの名前へ露出させない
- 標準実行環境はLinuxコンテナと専用X11画面にし、最初の入力ドライバーはxdotoolとする
  - WindowsではWSL2上のDocker、LinuxではDockerから同じ構成を実行する
  - ドライバーの操作要求をアプリの状態取得から分離し、別のドライバーへ交換できる境界を保つ
  - Linuxの入力経路の成功をWindowsの入力経路の成功として扱わない

| 候補 | 今回の適合性 | 選定上の制約 |
| --- | --- | --- |
| xdotool | Linuxコンテナで採用 | 専用X11画面内でXTEST入力を使う Minecraft固有の状態判定はアダプターに残す |
| Cua Driver | 背景操作は採用保留 | 0.23.2とLWJGL 3.3.3の検証でCtrl+Cの修飾値が欠落 |
| Microsoft WinApp CLI | 隔離環境内の代替候補 | hoverやdragはSendInputを使うため、同じデスクトップの入力分離には使えない |
| CursorTouch Windows-MCP | 隔離環境内の代替候補 | 画面座標のマウス操作とWindowsクリップボードを扱い、ホストからの分離機構は確認できない |
| Microsoft UFO | 今回は除外 | AIエージェント基盤としての範囲が広く、READMEのPiP機能は開発中 |
| pywinauto | 補助候補 | 実マウス操作はアクティブなデスクトップが必要で、背景のコントロール操作もアプリ依存 |

- Ctrl+Cの制約には実装上の根拠がある
  - [GLFWのWindows実装](https://github.com/glfw/glfw/blob/master/src/win32_window.c)の`getKeyMods`は`GetKeyState`を読む
  - [Cua Driverのキー入力実装](https://github.com/trycua/cua/blob/e686ee9ad32996bdfba9891852cf50a1269693ae/libs/cua-driver/rust/crates/platform-windows/src/input/keyboard.rs)は、PostMessageではその修飾状態を更新できず、SendInput経路では前景切替が必要になる理由を説明している
  - インストール済み0.23.2の`describe hotkey`にもこの制約が記載されている
  - 背景でキーを送れたという返り値だけで、Ctrl+Cの成功や非干渉を判定しない
- 比較資料
  - [WinApp CLIの入力方式](https://learn.microsoft.com/en-us/windows/apps/dev-tools/winapp-cli/ui-automation)
  - [Windows-MCPのツール](https://github.com/CursorTouch/Windows-MCP/tree/08ddee78c26182b103d62c1c84c1fbec82a280b2)
  - [UFOのPiP開発状況](https://github.com/microsoft/UFO/blob/364eb7969d392e857299ceaf14bd6057e5b00078/ufo/README.md)
  - [pywinautoのリモート実行制約](https://pywinauto.readthedocs.io/en/latest/remote_execution.html)
- 外部ドライバーと隔離環境は別に選定し、Windows VMの導入は保留する
  - CuaのWindows sandboxによるMinecraft実行例はあるが、公式のローカル例はLinux KVMまたはIntel Macを前提にしている
  - Windows Sandboxはホストとのクリップボード転送を無効化できるが、Windows Homeは対応対象外
  - 隔離方式を再検討する場合は、Windows VMのOpenGL対応、起動時間、キャッシュの永続化、ゲストのクリップボード分離を検証する
- 最小適合検証では、通常UIAアプリの成功をMinecraftの成功として代用しない
  - 起動からワールド終了までホストの前景ウィンドウを変えず、ユーザーのマウス操作を妨げない
  - 原木をホバーしてCtrl+Cし、日本語名をクリップボードから確認する
  - 検索欄優先とキー解放を確認する 同一デスクトップではOSのクリップボードを共有する
  - 旧LWJGL 2と新しいGLFWの境界で成功してから、全対象の実行方式を置き換える

## Cua Driverの背景入力検証

- 再現には`tests/probes/Invoke-GlfwBackgroundProbe.ps1`を使う
  - Windows、Javaとjavac、Cua Driverの起動済みdaemon、Gradleキャッシュ内のLWJGL 3.3.3とWindows用native Jarが必要
  - `-Driver`、`-GradleCache`、`-LwjglVersion`で配置や対象を指定できる
  - GLFWのウィンドウをフォーカスなしで表示し、30秒間キー、修飾値、ポインターイベント、フォーカス取得を記録する
  - 外部入力はCua DriverのCLIから送り、前景操作への切替やゲーム側の入力状態の書換えは行わない
  - `build/driver-probe/<LWJGL版>/`へGLFWログとドライバーの応答を保存し、受入条件を満たさなければ失敗で終了する
- Windows 11 build 26200、Cua Driver 0.23.2、LWJGL 3.3.3の結果
  - GLFW自身のバージョン文字列は`3.4.0 Win32 WGL Null EGL OSMesa VisualC DLL`
  - `hotkey`へ対象ウィンドウと`delivery_mode: background`を指定すると、応答は`effect: unverifiable`、`delivery.mode: unknown`
  - Ctrl押下、C押下、C解放、Ctrl解放の4イベントは受信したが、全イベントの`mods`は0
  - C押下中の`glfwGetKey(LEFT_CONTROL)`は1でも、Modが利用するイベントの`GLFW_MOD_CONTROL`ビットは立たない
  - フォーカス取得は0回、Ctrl+Cとして受信したイベントは0件
  - `move_cursor`のwindow targetは`invalid_action_target`となり、ホバー入力は確認できない
  - 実マウスはユーザーも動かせるため、前後の座標差だけでドライバーの干渉を断定しない
- この結果はGLFW入力経路の不適合を示す
  - 起動中のMinecraftへの初回呼び出しも`unverifiable`で、アイテムのコピー成功は未確認
  - 独立したGLFW検証は、Minecraft内のホバー、コピー、クリップボードまで通したE2E成功を意味しない
  - 修飾値をテスト側で補正すると実入力の検証にならないため、この構成を全版のE2Eへ採用しない

## Dockerランタイム

- [Dockerによる外部入力検証](../tests/e2e/README.md)を共通環境の入口とする
- 仮想画面、入力配送、操作記録、タイムアウト、プロセスの後処理をアプリ用アダプターから分離する
- Javaとの接続もファイル経由の要求・応答とし、ゲームのtickを止めずに待機できるようにする
- ホストのX11ソケットやクリップボードをコンテナへ接続しない
- イメージへホストのビルド出力やネストしたGradleキャッシュを含めない
  - `.dockerignore`の`.gradle`だけではネストしたキャッシュが残るため、`**/.gradle`も除外する
- 入力基盤のGLFW検証と、Minecraft内のアイテムコピーを通す検証は別の結果として管理する
- LinuxコンテナのGLFW検証では、LWJGL 3.3.3、Java 25、Mesa 25.2.8のllvmpipeでOpenGL 4.5のコンテキスト作成と入力を確認した
  - C押下の`mods`は2で、Ctrl+C、指定座標への移動、キー解放、日本語を含むクリップボード照合が成功する
  - 操作対象のPIDが存在しない場合は、他ウィンドウへ送らず失敗する
  - JavaとLWJGLの組合せによる警告が残るため、Minecraftの版別Java選択は別途必要
