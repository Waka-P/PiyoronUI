# Plain(普通)の分解メモ

既定のスタイル。グラデーション、枠、影を使わず、色の役割(role)で平らに塗るだけ。ガラスなどの飾りは、スタイルを指定したときだけ付く。

## 役割ごとの色

| 面(SurfaceRole) | 塗り | 文字・アイコン |
|---|---|---|
| ボタン(既定) | `fillSelectedBottom` | `contentSelected` |
| ボタン(主な操作) | `primary`(乗せると `primaryStrong`) | `contentOnPrimary` |
| ボタン(取り消せない操作) | `danger`(乗せると `dangerStrong`) | `contentOnPrimary` |
| 選ぶボタン | `background`、選ぶと `fillSelectedBottom` | `contentMuted`、乗せると `primary`、選ぶと `contentSelected` |
| タブの土台、カード、ひょうたん、パネル | `background` | `contentMuted` / `contentDefault` |
| タブのつまみ、ひょうたんの印 | `surface` | `contentSelected` |
| メニュー | `surface` と、`ringShadow` の細い枠(`plain.edge.width`、1) | `contentDefault` |
| メニューの項目 | 乗せたときだけ `fillSelectedBottom` | `contentDefault`、乗せると `primaryStrong`、削除は `danger` |
| 区切り線 | `background` | |

- 枠はメニューだけ。浮く面なので、背景と分けるため
- 文字・アイコンの色の決め方は Glass と同じ(Android は `baseContentColor` を共有)
- 押したときの動き、無効の薄さ、フォーカスの輪郭はスタイルによらず共通(core)
