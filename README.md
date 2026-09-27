# 狐妖小红娘 Mod

本项目面向 Minecraft 1.20.1 Forge 47.3.0，当前代码以虚空之泪、换血功、东方一族/水蛭妖、厄喙兽和传送符系统为基础。

## 前置

- Java 17
- Minecraft Forge 1.20.1-47.3.0
- Player Animator（`playeranimator`，版本 `1.0.2-rc1+1.20`）

Player Animator 的 Forge 依赖由 `build.gradle` 自动下载，发布游戏时也需要把对应的 Player Animator 模组放入 `mods` 文件夹。

## IDEA 启动

1. 用 IDEA 打开项目根目录并导入 Gradle。
2. 等待 Gradle 同步完成。
3. 在 `Tasks -> forgegradle runs` 中运行 `runClient`。

命令行编译检查：

```powershell
.\gradlew.bat compileJava --no-daemon --console=plain
```

## 当前主要操作

- `R`：切换虚空之泪技能
- `Z`：释放虚空之泪技能
- `X`：换血功
- `U`：打开虚空之泪坐标点界面
- `P`：打开悲情之心界面
- `G`：轮换五个纯质阳炎技能槽；进入纯阳护体形态选择后，按 `G` 轮换速度型（A）/防御型（B）/均衡型（D）
- `C`：使用当前技能；切到第五技能“纯阳护体”后，按住 `Shift` 再按 `C` 进入形态选择，松开 `Shift` 使用所选形态
- `J`：进入/退出纯质阳炎冥想
- `H`：打开纯质阳炎五槽技能装配界面
- `/mjvoidtears heart set <数值>`：设置悲情之心
- `/mjleech affection set <数值>`：设置水蛭妖好感度
- `/mjblood learn`：授予换血功权限
- `/mjblood permission`：授予东方神血权限
- `/mjpurefire permission`：授予控火权限，装配一技能并解锁五技能“纯阳护体”
- `/mjpurefire unlock <1-5>`：解锁指定纯质阳炎技能槽
- `/mjpurefire proficiency set <数值>`：设置控火熟练度
- `/mjpurefire proficiency view`：查看控火熟练度和技能掩码

所有技能实体由服务端生成并同步到客户端；技能表现可以基于 Player Animator 继续扩展。
