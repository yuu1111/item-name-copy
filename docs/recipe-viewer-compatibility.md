# レシピビューアー互換性

## 対応範囲

ItemNameCopyは、レシピビューアーが公開するクライアントAPIからカーソル下のアイテムを取得する
レシピ、アイテム一覧、お気に入りにある疑似スロットを対象とし、アイテム以外のFluidなどはコピーしない

| Mod | ItemNameCopyの対応範囲 | 利用するAPI |
| --- | --- | --- |
| EMI | Minecraft 1.18.2以降でEMIが配布されている版 | `EmiApi.getHoveredStack(false)`、`EmiApi.isSearchFocused()` |
| REI | Minecraft 1.17以降でREIが配布されている版 | `ScreenRegistry.getFocusedStack(...)`、`Slot.getCurrentEntry()`、`REIRuntime`のOverlayと検索欄 |

EMIの公開APIは、最初の対応ブランチである1.18.2から現在のブランチまで、ホバー中の`EmiStackInteraction`と検索欄のフォーカスを取得できる
配布されているMinecraft版とLoaderは[EMIのVersions](https://modrinth.com/mod/emi/versions)で確認する

REIの`ScreenRegistry`によるFocused Stack APIと`Slot` Widget APIは、REI 6.xのMinecraft 1.17から現在のブランチまで維持されている
REI 4.xと5.xには同じ公開APIがないため、Minecraft 1.14から1.16.5のREIは対応範囲に含めない
配布状況は[REIのVersions](https://modrinth.com/mod/rei/versions)と[REIの公式Repository](https://github.com/shedaniel/RoughlyEnoughItems)で確認する

## 任意依存の分離

`RecipeViewerAccess`だけがEMIとREIのAPI境界を担当する
各API classとmethodは初回利用時に実行環境から検出し、対象Modがない場合や対応外のAPI世代では無効なAdapterへ切り替える
このため、ItemNameCopyのJarにはEMI、REI、Architectury API、Cloth Config APIを同梱せず、Mod metadataにも依存関係を追加しない

一般的な連携Modでは、EMIはLoader別artifactの`api` classifier、REIはLoader別のAPI artifactを`compileOnly`で利用できる
一方、このRepositoryはMinecraftとLoaderの組み合わせごとに多数のJarを生成し、レシピビューアーが配布されていない版もビルドする
全Nodeへ異なるAPI artifactを固定する代わりに、名前を保つレシピビューアー側の公開APIだけを反射的に呼び出し、Minecraftの`ItemStack`へ変換した後は通常スロットと同じ表示名取得処理を使う

APIの根拠は[EMI 1.18.2の`EmiApi`](https://github.com/emilyploszaj/emi/blob/1.18.2/src/main/java/dev/emi/emi/api/EmiApi.java)、[EMIの導入手順](https://github.com/emilyploszaj/emi/wiki/Getting-Started-Guide)、[REI 6.xの`ScreenRegistry`](https://github.com/shedaniel/RoughlyEnoughItems/blob/6.x-1.17/api/src/main/java/me/shedaniel/rei/api/client/registry/screen/ScreenRegistry.java)、[REIの依存設定](https://github.com/shedaniel/RoughlyEnoughItems#choosing-the-correct-artifact-to-depend-on)を参照した

## 失敗時の扱い

APIが存在しない、または互換性のない版ではレシピビューアー連携だけを無効にする
通常スロットのコピー、ItemNameCopy単体での起動、レシピビューアー側の起動を妨げない
検索欄にフォーカスがある場合は通常のコピー操作を優先し、ItemNameCopyはクリップボードを上書きしない
