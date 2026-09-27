# 虚空之泪 Blockbench 模型

文件：

- `void_tears.bbmodel`：Blockbench 原生工程，可直接打开并继续编辑。
- `void_tears.geo.json`：Bedrock/GeckoLib 风格几何导出，适合导入 Minecraft 模型项目。
- `void_tears_helmet.bbmodel`：按玩家头部尺寸制作的可穿戴护甲模型（`java_entity`，`head` 骨骼）。
- `void_tears_helmet.png`：青白色液态火焰像素纹理，已在头盔工程中绑定。
- `void_tears_helmet.geo.json`：头盔模型的 Bedrock/GeckoLib 几何简化导出。

打开方式：

1. 启动 Blockbench，使用“文件 -> 打开模型”选择 `void_tears_helmet.bbmodel`。
2. 头盔模型原点在头部中心，前方为 `-Z`；直接作为头部护甲骨骼使用时无需额外平移。
3. 结构包含 `tear_*`（左眼液态泪焰）、`crown_*`（额头尖焰）、`tail_*`（向左上方抽拉的弧形流线）和 `drop_*`（沿流线分布的独立光滴）。
4. 游戏内 `VoidTearsHelmetModel` 会让 `tail_*` 分段按相位摆动，并叠加一层左上方向残光；Blockbench 工程本身保存的是静态基准姿态。
5. `void_tears.bbmodel` 是早期的悬浮展示版，不适合直接作为头盔。

模型是原创的低多边形抽象造型，用于表达《狐妖小红娘》中“虚空之泪”的泪滴与空间裂隙意象；不包含原作贴图或受版权保护的素材。

---

# 纯质阳炎·火焰拳套

文件（由 `generate_pure_fire_gauntlet.ps1` 一键生成）：

- `pure_fire_gauntlet.bbmodel`：Blockbench 工程（64×64 贴图已内嵌），左右手各 32 个方块，分组 `left_gauntlet` / `right_gauntlet`。
- `pure_fire_gauntlet.geo.json`：Bedrock 风格几何导出。
- `pure_fire_gauntlet.png`：金甲 + 三层火焰渐变贴图（同时输出到 `src/main/resources/assets/mjmod/textures/effect/` 供游戏加载）。
- `pure_fire_gauntlet_java_snippet.txt`：`PureFireGauntletModel` 使用的方块数组片段。

坐标约定与 `PureFireFistModel` 一致：原点在拳心，`+Y` 朝指尖，`-Y` 沿小臂向上（火焰升腾方向），`-Z` 为拳面正前方。结构分层：

- `band/knuckle/cuff/fingerband/trim_*`：金甲拳套本体（层 0，只随整体呼吸）。
- `core_*`：白炽焰芯，贴近拳面（层 1）。
- `mid_*`：炽橙中焰，更高更瘦（层 2，带静态外翻卷曲）。
- `outer_*`：赤红半透明外焰，最高、卷曲最明显（层 3）。
- `spark_*`：脱离拳体的火星（层 4，游戏内绕拳盘旋）。

游戏内 `PureFireGauntletModel.animate()` 用“相位随高度递减的行波”驱动所有火焰方块：波峰自下而上扫过每根火舌，外层摆幅大于内层、火尖大于焰根，叠加火舌上蹿、明暗呼吸与火星盘旋，呈现持续向上的流动感。挂在玩家双臂上的入口是 `PureFireHandsLayer.renderHandForm`（技能 2“双手附焰”与纯质阳炎形态）。

调整造型：修改 `generate_pure_fire_gauntlet.ps1` 顶部的 `$cubeDefs` 后重新运行脚本，再把生成的数组片段同步进 `PureFireGauntletModel`（贴图会自动重新输出到两处）。注意：脚本必须带 UTF-8 BOM 保存，否则 PowerShell 5.1 按 GBK 解析中文注释会吃掉换行导致语法错误。
