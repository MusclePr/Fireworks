# Fireworks Paper Plugin

## 概要
Minecraft PaperMC 26.3 対応の花火演出プラグインです。
夜間に「花火」と名前が付いた防具立てから自動で花火を打ち上げます。

## 主な機能
- 夜間のみ自動で花火を打ち上げ
- フィナーレで虹色の水平64発同時発射
- コマンド `/fireworks start|stop` でワールド単位に制御
- 防具立て名「花火」で演出対象を指定
- パーミッション `fireworks.use` 対応

## コマンド
| コマンド | 説明 |
|----------|------|
| `/fireworks start [ワールド名]` | 指定ワールドで花火演出を開始 |
| `/fireworks stop [ワールド名]`  | 指定ワールドの花火演出を停止 |
| `/fireworks start [ワールド名] now` | 花火演出を開始し、そのワールドの時刻を夜（minecraft:midnight）に設定 |
| `/fireworks stop [ワールド名] now`  | 花火演出を停止し、そのワールドの時刻を昼（minecraft:noon）に設定 |

- 開始・停止はワールド単位で管理されます。
- プレイヤーが実行し、ワールド名を省略した場合は現在いるワールドが対象です（コンソールからはワールド名必須）。
- ネザー・エンドなどワールドクロックを持たないワールドでは時刻変更はスキップされます。
- コマンド補完で `start` / `stop`、ワールド名、`now` が候補に表示されます。

## パーミッション
- `fireworks.use` : コマンド実行権限

## インストール方法
1. `build/libs/Fireworks-*.jar` を Paper サーバーの `plugins` フォルダに配置
2. サーバーを再起動
3. `/fireworks start` で花火演出開始

## 動作環境
- Minecraft PaperMC 26.3
- Java 25

---

### 制作: MusclePr
