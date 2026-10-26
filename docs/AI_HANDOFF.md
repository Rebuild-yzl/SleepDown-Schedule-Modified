# 当前工作交接

更新：2026-10-20，Asia/Shanghai。此页保存维护者主工作目录的当前交接，历史过程引用报告；不代表其他 worktree 的任务已结束。接手前完整阅读 [AGENTS.md](../AGENTS.md)，再核对实际 Git、PR、用户新指令与本页是否一致。流程和交接字段见 [DEVELOPMENT_WORKFLOW.md](DEVELOPMENT_WORKFLOW.md)。

给下一位助手的简短入口：

> 先读 AGENTS.md 和 docs/AI_HANDOFF.md，核对当前分支、工作树及 PR 状态，接着完成未完事项。沿用可核实的用户授权和已有成果，按本次变化选择验证；遇到差异先查事实并更新交接。

## 个人修改版当前交接（2026-10-26）

> 下面「当前分支与目标」起是上游主仓（CourseSchedule）的历史交接，仍按原样保留；本修改版的现状只看这一节。

**分支**：`dev/todo-and-countdown-events`，从 `modified` 派生的线性历史，**尚未合并回 `modified`**（按 AGENTS.md：等用户明确要 PR 或合并再动）。

| 提交 | 内容 |
| --- | --- |
| `edf94f23` | 计划文档：五项 Dock、完成态灰划线、象限内滚动 |
| `f70eb4b9` | 数据层：`todo_items` / `countdown_events`、Room 47→48、规则与单测 |
| `3eccd1e8` | 五项 Dock + 待办/倒数日页面接进首页 |
| `b788deb3` | 修页面盖住首页页头的层级问题 |
| `3b047b60` | 交接：记下「复用原版控件」「共用加号」两条要求 |
| `292f8952` | 两页改用公共控件、加号与首页共用、完成不重排、倒数日跨年顺延 |
| `57f4d34d` | 文档：实现现状、控件映射、下一步 |
| `7479d023` | 待办 / 倒数日改为首页页内槽位，共用壁纸与切换动效 |
| `a90805f6` | 交接：槽位架构、验证与剩余缺口 |
| `238a2df6` | 卡片复用课程玻璃、Dock 尺寸随项数推导 |
| `7b4c1437` | 待办日期分页、共用首页顶栏、Dock 与页内容同宽 |
| `c8c06884` | 待办逐日显示、状态文案、创建默认今天、菜单拆分 |
| 本次提交（`git log -1`） | 可见性终点改为 max(截止日, 今天/完成日)，已完成按当页回溯并标「当时逾期」，截止日不得早于创建日 |

**本次修正：材质复用、Dock 尺寸与切换性能（2026-10-26 晚）**

用户指出三个问题：新页面「空间是黑色的」、没有真正复用原版材质；Dock 高度变小但加号没变、切到设置页加号从条后漏边；页面切换有明显卡顿。逐条处理：

1. **材质复用**：象限卡与倒数卡原来用 `SettingsGroup` → `LiquidPanel`，它按 `appUsesDarkTheme(config)` 取固定的 0x1C1C1E / White 表面色，和首页卡片的颜色、透明度、模糊/折射、字体缩放完全无关——深色配置下看起来就是黑块。现在两页改用 `glass/ui/CourseGlassCard`（与日/周课程卡同一张玻璃、同一条壁纸模糊管线、同一个 `LocalCourseTextBackground` 投影），文字改用 `core/ui/text/CourseCardText`；`CourseCardText` 补了 `textDecoration` 参数以保留完成态划线。
2. **Dock 与加号**：新增 `feature/home/HomeDockMetrics.kt`，按项数推导条高（5 项 46dp、每少一项 +4dp）、单元宽（50/46 比例）、圆角（=高/2）与加号直径（=条高）；`HomeDockTabs`、`ParallelHomeDock`、`DockItem`、`SleepDownFloatingAddButton(size = …)` 共用这一套。设置页没有加号时，加号按 split 缩到 0.62 并淡出，不再整块藏在半透明条后面漏边。
3. **切换**：`homePageMotion` 改为 plain slide（页面级一次平移，不再做日/周的逐卡错位），两页 `retainContent = true` 空闲 200ms 后预热合成，隐藏页的日期/秒表时钟按 `LocalHomePaneVisible` 停走。

**性能测量（模拟器 `emulator-5554`，Pixel_10a / Android 17，release 包）**

- 方法：`dumpsys gfxinfo reset` 后连续切 6 次页面取汇总；再用 `gfxinfo framestats` 拆 UI 线程 / RenderThread。
- 结果：页面切换 25%（p50 29ms、p90 81ms）；逐帧 **UI 线程均值 15.2ms（p90 47ms）、RenderThread 均值 38.6ms（p90 90.7ms）、整帧 57.6ms**。首页↔设置（跨 Activity）对照只有 6.7%。
- 结论：瓶颈在 GPU 合成大块玻璃表面，不在重组；两页都是整屏玻璃，所以「视图↔待办」与「待办↔倒数日」几乎一样慢。
- 已试并**撤回**：让正在离开的页冻结玻璃采样（`coordinatesFrozen` + 固定 `sampleKey`）反而更差（32.6%、p50 34ms），说明成本是每帧合成大块玻璃层，不是重算采样，这个方向不要重复。
- 未做、需要取舍：①切换期间把两页录成 `GraphicsLayer` 快照再平移 / 交叉淡入（真正的位图合成，最可能根治）；②把四个象限 / 倒数列表合成一张玻璃面板。两者都改变视觉或交互，先不要擅自选。
- 用户贴的「Glass 场景六阶段生命周期」与当前代码不符：`GlassRenderPhase` 只有 Preparing / Moving / Live / Closing / Released，`synchronizePhase` 只由 `startupAnimation` 驱动；页面切换根本不进这个状态机，不存在「每次切页完整拆建玻璃场景」。

**备份导入（用户提供 `docs/plans/SleepDown-Backup-20261025-1826.sleepdown`）**

- 备份是 `formatVersion = 1`、`sourceDatabaseVersion = 47`（1.2.7.1 导出），当前 Room 48 可迁移恢复；**不是改数据库导致的**。
- 之前失败的真实原因：`am start` 用 `file:///sdcard/Download/...` 把 adb push 的文件直接交给应用，`ContentResolver.openInputStream` 抛 `EACCES`；改用 MediaStore 的 `content://media/external/file/103` + `--grant-read-uri-permission` 后预览正常（「来自 SleepDown 1.2.7.1」），「替换并恢复」成功，课表与设置已替换。
- 该备份不入库：`.gitignore` 增加 `/docs/plans/*.sleepdown`。

**模拟器已验证**

- 视图 / 待办 / 倒数日 三页共用同一张备份壁纸，象限卡与倒数卡都呈现课程卡那层半透明玻璃（颜色/透明度跟随设置），不再是黑块
- Dock 与加号同高（46dp）、同圆角；设置页加号平滑缩退、无漏边
- 待办页加号菜单 → 新建待办 → 编辑器；取消后不再自动弹出；返回键从待办回视图
- `logcat` 无 `FATAL` / `AndroidRuntime`

**待办的周期显示与状态文案（2026-10-26 晚，第三批）**

- 日期页可见性 `todoVisibleOn`：创建日到截止日（含）每天出现，已完成也出现；逾期未完成的继续每天出现，直到完成；逾期已完成的停在截止日之后。创建日取 `createdAt` 的本地日期，缺失（旧数据）时不做前置限制。
- 文案：未完成按当前页日期倒数（`今天 到期` / `明天 还有一天` / `后天 还有两天` / `本周` / `本月` / `下个月` / `M月 还有N天`，1–9 用中文数字）；已完成显示真实经过时间（`已完成 今天` / `已完成 昨天` / `已完成 15天前`）；逾期显示 `已逾期 N天`。
- 新建待办默认截止到今天；编辑已有项仍可保留「无截止日期」。
- 入口拆分：Dock 加号只放「新建待办」，顶栏三点只放「已完成」，按 `homeMenuFromDock` 选择 `todoNewActions` / `todoOverflowActions`。
- 顺手把陈旧单测 `HomeSwitchMotionTest` 从布尔两槽改成整型槽位 API；`TodoCountdownRulesTest` 增加逐日可见性与新文案断言。
- 模拟器验证：10/26 今天的 `今天 到期`、`已完成 今天`；10/27 出现 `已逾期 1天 / 2天`；10/25 仍能看到创建于更早、截止在 10/25 的已完成项；加号菜单只剩新建，三点菜单只剩已完成。

**待办可见性与状态文案定稿（2026-10-26 晚，第四批）**

用户逐条确认后的规则，实现以 `todoVisibleOn` + `todoDueMeta` / `todoDoneMeta` 为准：

- **可见性**：起点是创建日，终点分两种——未完成取 `max(截止日, 今天)`（逾期项延续到今天，未来的页不显示，直到勾选完成；无截止日则一直显示）；已完成取 `max(截止日, 完成日)`（按期完成的看到截止日，逾期完成的看到完成日）。已完成项在创建日…截止日之间照常出现，不因完成而提前消失。
- **已完成文案以当页为基准**：`e = 页日期 − 完成日`，`e<0` → `已完成 N天后`，`e=0` → `已完成 今天`，`e=1` → `已完成 昨天`，`e≥2` → `已完成 N天前`；页日期晚于截止日时追加 ` 当时逾期`（例：`已完成 1天后 当时逾期`）。已完成列表没有页日期，基准取今天，「当时逾期」看完成那一刻是否已过截止日。
- **未完成文案仍以当页倒数**：`今天 到期` / `明天 还有一天` / `后天 还有两天` / `本周|本月|下个月|明年X月|YYYY年M月 还有N天` / 逾期页 `已逾期 N天`。1–9 天用中文数字，10 天以上以及所有逾期、完成天数用阿拉伯数字。
- **跨年**：次年写 `明年X月 还有N天`，相差 ≥ 2 年写完整年份 `2028年1月 还有N天`。
- **换行**：状态副标题允许软换行（`maxLines = 2`），不再单行截断。
- **创建侧约束**：截止日不得早于创建日——新建时最小可选日是今天，编辑时是该待办原来的创建日；选到更早的日期时「确定」不可用并提示 `不能早于创建日 X月Y日`。
- 单测 `TodoCountdownRulesTest` 覆盖六种可见性分支、`已完成 N天后` 与 `当时逾期` 文案、跨年月份写法。

**待办页日期分页与共用顶栏（2026-10-26 晚，第二批）**

- 待办内容改为 `HorizontalPager`（锚点 10_000、落页后才提交日期、`scheduleDayNavigationRange` 夹取），与日视图同一套左右滑动；没有截止日期的待办固定留在「今天」这一页，不会因为分页消失。
- 顶栏直接复用 `AppTopBar` / `HomeDateTitle`：`Screen.Todo` 走与首页相同的分支，日期取 `todoDisplayDate`，不是今天时显示「点击此处回到本日」，点一下回今天；铅笔（个性化）与更多菜单原样沿用；倒数日仍保留自带页头。
- 「已完成」从页头按钮移到更多菜单：`todoAddActions` 增加 `ic_check` 项，用一次性 `todoCompletedPending` 在菜单收起后打开弹窗；页头标题、日期行、7 天周条全部删除。
- Dock：`HomeDockMetrics.forCount(count, availableWidth)` 按「条 + 8dp 间距 + 加号 = 页内容宽（屏宽 − 32dp）」反解条高（411dp 宽机型约 56.5dp），项宽、图标、字号与加号直径一起放大；`ParallelHomeDock` 与 `FloatingDock` 都传宽度预算，窄窗口自动缩回。
- 模拟器验证：左右滑动切换日期、顶栏「点击此处回到本日 / 10月27日 周二」可点击返回；更多菜单「新建待办 / 已完成」、已完成弹窗；Dock 与象限卡左右对齐（各 16dp），加号与条同高。

**本轮架构调整：两页进入首页页内（2026-10-26）**

之前两页是 `ScheduleAppUi` 内容区里 `zIndex(90f)` 的兄弟层，自带不透明底色，所以背景与转场都和日/周视图不一致。现在改为：

1. `HomeSwitchMotion` 由「布尔两槽」泛化为「整型槽位」（`initialSlot` / `animateTo(slot)` / `retains(slot)` / `cornerFraction` 按最近整数计算，`homeSwitchLayer(motion, slot)` 平移 `(slot - progress) * width`）。日↔周、首页↔设置继续走布尔重载，行为不变。
2. 首页内容新增一层 `homePageMotion`：**槽位 0 = 课表（内部再分日/周）、槽位 1 = 待办、槽位 2 = 倒数日**，三者都挂在首页壁纸之上；Dock 仍是唯一导航，`screen` 决定槽位，**没有新增模式切换 UI**。
3. 两页去掉不透明底色与 `DetailTopBar`：标题/日期/周条改用 `homeForegroundColor` + `HomeReadableText`（壁纸自适应字色与柔和投影），「已完成」改成玻璃胶囊；象限卡 / 倒数卡改用 `CourseGlassCard`（同一套课程材质与壁纸 Backdrop）。
4. 首页顶栏（第1周 / 日期 / 铅笔 / 三个点）在切到待办/倒数日时随页滑动淡出：`screen is Screen.Home || homePageMotion.moving` 时保留合成、`alpha = 1 - progress`，落位后整块卸载，不会再抢页面顶部点击。
5. 顺带修掉一个真实 bug：加号 →「新建待办」原来用累加计数，退出再进页面会重复弹出编辑器；改成一次性 `composerPending` + `onComposerShown()` 消费。

**模拟器已验证（`emulator-5554` / Pixel_10a / Android 17，壁纸用 `docs/plans/壁纸.jpg`）**

- 视图 / 课程表 / 待办 / 倒数日 四项切换都是同一套滑动 + 交叉淡入，三页共用同一张壁纸（截图可见壁纸透过象限卡与倒数卡）
- 待办页：2×2 象限与壁纸叠合（页头与周条在后续批次已删除，改为复用首页顶栏 + 左右滑动切日）；加号菜单 → 新建待办 → 编辑器（玻璃弹层）；取消/返回后不再自动弹窗
- 倒数日页：Hero 每秒走字（`61 天 23:54:48`）、卡片玻璃、跨年顺延（`364` / `61` 天）
- 返回键从待办回视图；设置页与首页之间互切正常；`logcat` 无 `FATAL`/`AndroidRuntime`

**分支收尾（2026-10-26 晚）**

- 分支 `dev/todo-and-countdown-events` 已整理成可提交状态：功能与文档提交线性排列，工作树干净，`tmp/` 已加入 `.gitignore`。
- 最终验证：`TodoCountdownRulesTest` 通过；`assembleGithubRelease` 成功并在 `emulator-5554` 覆盖安装。
- PR 目标为 `modified`；PR 正文已生成，等待用户手动提交，**不自行合并**。

**下一步（按优先级）**

0. 切页卡顿：先和用户确认方向（快照转场 / 合并玻璃面板）再动手；模拟器 25% 抖动、RenderThread 均值 38.6ms，纯 UI 侧优化已到边界。
1. ColorOS 原生路径：`ColorOsParallelHomeDock` 仍按 3 项渲染、`canOpen`/`expanded` 用 `selectedIndex < 2`、`select` 钳到 0..2，`colorOsOrdinaryDockWidth()` 也未同步；ColorOS 真机上「待办/倒数日」会落到错误索引。
2. 象限滚动边缘 24dp 渐隐（计划 4.1）仍未实现。
3. 亮壁纸上文字对比度：现在与首页一致（`HomeReadableText` 柔和投影），但极端亮部（`壁纸.jpg` 顶部白字）仍偏弱，可按需加局部压暗。
4. `HomeMode` / `HomeStartMode` / Room 默认值**没有**扩展：用户明确不需要模式切换功能，当前页也不持久化（与日/周一致，重启回到 `defaultHomeMode`）。
5. `AppDatabaseMigrationTest.migrate47To48` 仍未跑 androidTest；浅色主题、横屏 / 平板、真机未验收。
6. `docs/plans/design-reference-todo-countdown.html` 仍是改造前的低保真稿（页头 / 周条已删、改为日期分页），稿内已加勘误说明，正式同步待办。

**已知偏差**

- 待办 / 倒数日不是 `HomeMode` 值，而是首页内容层里的独立槽位；好处是不影响 `homeMode == Week` 的大量课程逻辑（编辑器来源判定、周编辑、助手等）。
- 行内删除仍是「左滑 + 确认弹窗」；两张新表不参与 `.sleepdown` v1 备份。

**待办与倒数日的备份支持（暂缓，2026-10-26 用户决定）**

- 现状：`.sleepdown` v1 的 `BackupData` 只有 schedules / widgetAppearances / sharedPeriodSchemes；`feature/backup` 包内没有任何 `todo` 引用，恢复事务也只替换课表相关表，不会删除或写入 `todo_items` / `countdown_events`。所以备份带不走这两类数据，但恢复也不会清掉它们。
- 若之后要做：按 `docs/migration/BACKUP_FORMAT_V1.md` §8「明确定义为 optional 的新字段：v1 parser 忽略」增加两个可选列表（稳定 ID `todo_<uuid>` / `event_<uuid>`），保持 `dataVersion = 1`；恢复仍走 Replace，需要把两张表纳入替换集并在预览里写明；或者把 `dataVersion` 升到 2、新版接受 1..2，让旧版明确拒绝而不是静默丢段。测试需覆盖新包→新库、旧包→新库、脏行处理。

**验证命令**

```powershell
$env:JAVA_HOME='D:\JetBrains\Android Studio\jbr'
$env:GRADLE_USER_HOME='C:\Users\TableATK\.gradle'
.\gradlew.bat compileGithubReleaseKotlin
.\gradlew.bat assembleGithubRelease
.\gradlew.bat :app:testGithubDebugUnitTest --tests '*TodoCountdownRulesTest*'
adb -P 5038 -s emulator-5554 install -r app/build/outputs/apk/github/release/app-github-release.apk
```

**代码地图**

- 切换器：`feature/home/HomeSwitchMotion.kt`（`HomeSwitchMotion(slot counts)`、`rememberHomeSwitchMotion(slot = …)`、`HomeSwitchPane(motion, slot = …)`、布尔重载保留）
- 宿主：`app/ui/ScheduleAppUi.kt`（`homePageMotion`、首页页内 `Box` + 三个 `HomeSwitchPane`、顶栏淡出、`todoComposerPending` / `countdownComposerPending`）
- 页面：`feature/todo/TodoPage.kt`、`feature/countdown/CountdownPage.kt`（`backdrop` 参数、`composerPending` + `onComposerShown`、`HomeReadableText` 页头）
- 数据 / 规则：`model/TodoModels.kt`、`data/local/TodoDaos.kt`、`AppDatabase.kt`（version 48）、`feature/todo/TodoRules.kt`（`settledDoneIds`）、`feature/countdown/CountdownRules.kt`（可选 `now`）
- Dock：`feature/home/HomeDockMetrics.kt`（项数→条高/单元宽/圆角/加号直径）、`HomeDockTabs.kt`（五项）、`ParallelHomeDock.kt`（`addCapable = selectedIndex < 4`、加号随 split 缩退）
- 材质：`glass/ui/CourseGlassCard.kt`（页面卡片复用）、`core/ui/text/CourseCardText.kt`（已补 `textDecoration`）

**可复用控件（本轮实际用到的入口）**

| 用途 | 入口 |
| --- | --- |
| 壁纸自适应文字 | `feature/home/day/HomeReadability.kt` `HomeReadableText` / `homeForegroundColor` |
| 页面槽位与切换 | `feature/home/HomeSwitchMotion.kt` `rememberHomeSwitchMotion(slot)` + `HomeSwitchPane(slot)` |
| 居中液态弹窗 / 确认框 | `SleepDownDialog.kt`: `LiquidAlertDialog`、`DialogLiquidButton`、`DialogCapsuleField` |
| 编辑器外壳 | `SleepDownQuickSheet.kt:140 SleepDownPickerDialog`（`contentTransitionKey` 分页） |
| 分组卡片 / 行 / 开关 / 滑动删除 | `SettingsGroups.kt SettingsGroup`、`SettingsRows.kt`（`SettingsNavigationRow` / `SettingsToggleRow` / `SettingsSwipeDeleteRow`） |
| 日期 / 时间轮 | `SettingsTimePickers.kt` `SettingsDatePickerContent` / `SettingsTimePickerContent` |
| Dock 加号 | `SleepDownFloatingAddButton` + `ParallelHomeDock` + `HomeAnchoredMorphOverlayHost` |

## 当前分支与目标

| 对象 | 核对结果 |
| --- | --- |
| 主工作目录 | 仓库根目录 `CourseSchedule`；当前分支 `codex/nexio-glass-parity` |
| 关联 PR | [#78](https://github.com/xiaomanjun233/SleepDown-Schedule/pull/78)，目标 `main`，Draft，未合并 |
| 最新正式版 | `v1.2.7.1`，源码 `bdc380ee6796de6c633fe2688120a77c9fe8356f`，应用 `1.2.7.1` / 35，双平台正式发布、GitHub latest 已确认。r1 已按用户要求撤下，双平台旧标签与源码 `fa93ae65` 保留 |
| main | `a320f6a8`，实际 `1.2.7_beta5` / 34，是正式版祖先；`1.2.7.1` 发布提交比其多 106 个提交 |
| 已提交的运行时代码锚点 | 最新运行时修复 `5d123ac4`（修订号比较），发布提交 `bdc380ee`；此前界面运行时锚点 `c3bb034c`。`844ba11d` 统一时间呈现，`e52d9918` 保留隐藏节次摘要，`1b111ad8` 统一附件/预览文字，`e7312a99` 修正 Dock 间距与触觉，`90a0fb3e` / `c3bb034c` 修正快速设置布局；`47b37ae7` 包含 Popup 锚点、日视图上传及课程外观修订 |
| 当前目标 | 已解决 `1.2.7` 收不到 r1 更新：以旧版能识别的 `1.2.7.1` / 35 双平台正式补发，并修复后续修订号比较。r1 已撤下；用户明确批准本次使用既有发布脚本连接。原版更新函数对真实 Gitee 列表验证通过，两种 Beta 设置均选中本版。未安装或实机验收。详见[更新识别与发布记录](reports/2026-10-20-update-revision-compatibility.md) |

从 main 建立本任务分支后，只在任务分支快进到正式版。#78 携带上一正式版的 64 个已发布提交及之后的修复，`1.2.7.1` 发布源码相对 main 共多 106 个提交，不能当作只含几处界面修改的小 PR。main 与发布基线差异尚未实际收口。新独立任务的分支选择按开发流程处理，不继续从历史 `develop` 或 `exp` 开发。

本轮交接、协作文档和草稿 CI 规则位于 #78 分支，main 尚未同步。其他机器续做本任务应先获取并核对该分支。已核对 main 受保护，仓库允许 merge、squash、rebase 三种合并方式；GitHub 集成读取保护细则返回 403，本轮没有据旧文档断言具体审批数量，也未改动仓库设置。

## 已完成与关键结论

- 本轮更新识别：`1.2.7` 与 r1 已发布客户端忽略修订后缀，已以旧版能识别的 `1.2.7.1` 正式补发。当前代码按数字版本、发布阶段、阶段序号、修订号排序；12 项定向测试及原版比较器 8 项兼容场景通过。额外对真实 Gitee 更新列表运行原版选择函数，两种 Beta 设置都选中 `1.2.7.1`，旧版满足更新条件，新版不会重复提示。四份公开 APK 回读一致；r1 已撤下，源码标签保留。
- 课程玻璃改为共享全分辨率壁纸前缀与逐卡 Lens，处理课程详情展开时 producer 自订阅造成的持续更新；保留必要的共享 Offscreen 栅格边界。用户反馈左右切周已有明显改善，未作 FPS 测量。
- 横屏凝光顶部胶囊保留原生 Segment。其内部 TextView 会消费触控，容器 OnTouchListener 不能观察完整序列；现用透明 FrameLayout 的 `dispatchTouchEvent` 观察 DOWN 和分发后的 UP/CANCEL，抬手再导航。已审计本地 HD 侧栏组件职责，不能拿侧栏或双栏控件直接替代顶部胶囊。
- 本轮继续保留 Segment 实例与弹簧，设置/课表明暗变化采用原地 refresh。导航锁由宿主控制，避免 `setEnabled(false)` 触发原生取消动画；刷新后重申无外框协议。
- 侧栏/底栏切换按钮拆出独立点击区域；个性化收紧标题间距，一级与二级标题按用户要求同为 titleMedium，以字重区分。
- 已关闭发布内容覆盖的旧草稿 #57、#61、#65，保留分支和历史。草稿 CI 跳过规则已在 #78 实际验证，等待可评审时运行完整 CI。
- 恢复 `AgentImportWorkspace.kt` 的原有 7 行作息校验，修复连续失败的那项云端测试。规则、模板和文档入口本轮已统一，Miuix 构建说明改为唯一完整补丁。
- AI 加号改用模型选单同款 Popup Morph；导入保留旧视觉到弹窗卸载后再展示新课表。新导入使用彩色模式，着色 50%、模糊 35%、折射 50%，其他参数对齐已有标记点。
- 自动教学周始终开启，详细/快速设置和 AI 的手动切换入口已移除。旧字段继续兼容，缺少开学日期时保留原周并提示补充；新建、配置保存、启动和备份恢复均接 Repository 规范化。
- 教务搜索框采用 ModulateAlpha 保持原生模糊采样，底部渐变同步入场，避免逐帧重复设置原生功能按钮文字。
- 附件 Popup 使用真实按钮中心返回并左边缘对齐，系统图片/文件选择器等退出完成后再打开。日视图助理接入现有文件确认、提取、校验和预览链路。无壁纸可使用单色/渐变，周视图已结束课程保留原色，个性化去掉冗余分类标题。
- 所有来源的真实课程时钟按当前作息精确匹配：完整对应时显示节次，偏移与部分覆盖保留真实时间；旧导入无需重导或改库。编辑器未改时间的确认保留源时钟和编号，周视图完整时间按实际列宽排版。
- 附件展开菜单使用面板主题前景，预览卡片使用公共课程文字。液态 Dock 加号与导航间距对齐原生 12dp 并恢复点击轻触反馈；快速设置统一行边距、短数值宽度及状态/日期说明布局，顶栏标题适配剩余宽度并保持单行。

## 验证与产物：按代码锚点区分

| 对象 | 实际证据与边界 |
| --- | --- |
| `1.2.7.1` 正式发布包 | 完整 `assembleGithubRelease` 通过，3m58s，159 项中 32 执行、127 复用，R8、资源压缩、lintVital 和正式 v2 签名齐全。身份为 `1.2.7.1` / 35，证书 SHA-256 `0f2b50df…`。12 项定向测试、原版比较器 8 场景及真实 Gitee 列表选择验证通过。双平台正式发布、公开附件回读通过；未安装。副本 `tmp/review-apks/SleepDown-1.2.7.1-20261020.apk`，7,591,259 字节，SHA-256 `3c920a97b319567bd367a482ccd493f55d59aa8c3dfe54b5dcd49c153b83fc8f` |
| 上一轮界面代码包 | `c3bb034c` 完整 `assembleGithubRelease` 通过，3m30s，159 个任务中 21 个执行、138 个复用；保留 R8、资源压缩、lintVital 和正式 v2 签名，包含对应轮次的界面修订 |
| `1.2.7-r1` 发布包 | `fa93ae65` 的完整 `assembleGithubRelease` 通过，4m24s，159 个任务中 21 个执行、138 个复用；R8、资源压缩、lintVital 与正式 v2 签名齐全，证书 SHA-256 `0f2b50df…`。Manifest 为 `1.2.7-r1` / 35。GitHub 与 Gitee 四份公开 APK 已流式回读，大小与 SHA-256 与本地一致；发布说明三处逐字节一致。详见[发布记录](2026-10-19-stable-1.2.7-r1-release.md) |
| 本轮时间校验 | `e52d9918` 的 `CoursePeriodAlignmentTest` 19 项通过，0 失败/错误/跳过；含编译 2m50s，覆盖无原始快照的旧导入、多作息并存、连堂与写回保留；未跑完整套件 |
| AI 暂存校验 | 恢复后仅运行 `AgentImportWorkspaceTest.overlappingAndGappedPeriodsAreRejectedBeforePublishingAStage`，1 项通过、0 失败；含编译 6m02s，没有重跑 623 项套件 |
| 草稿 CI | [run 39191153304](https://github.com/xiaomanjun233/SleepDown-Schedule/actions/runs/39191153304)，提交 `856d9b91`，三个 job 均 skipped；只证明草稿门控生效，不表示完整 CI 通过 |
| 上轮日期/默认值 | 对应 `0f019f1d`：现有 `ScheduleTermBoundaryTest` 8 项与 `ImportedCourseCardDefaultsTest` 1 项通过，共 9 项；含编译 3m37s，未跑完整套件；本轮导航样式修订未重跑这些测试 |
| 安装与实机 | 2026-10-19 23:04:03 经项目 ADB 5038 通道覆盖安装最终包到 USB 设备 PLJ110（Android 17），返回 Success，系统核对为 `1.2.7` / 35；保留数据、未自动启动。界面效果待用户验收，见[本轮报告](reports/2026-10-19-standard-clock-ui-feedback.md) |

最新包本机副本为上表的 `SleepDown-1.2.7.1-20261020.apk`，已随双平台 Release 发布。旧 r1 副本 `tmp/review-apks/SleepDown-1.2.7-r1-20261019.apk`（`a9bc1b4b…`）保留，对应发布已撤下。此前 `standard-clock-ui`（`0152a70d…`）、`popup-day-file-cards`（`0cac9908…`）、`navigation-full-capsule`（`41f6768d…`）、`navigation-import-defaults`（`501c3455…`）及 `COUI-gesture-dispatch` 副本也均为旧包。临时产物不入库，其他机器可能没有文件；需要新包时重新构建，不把旧包改名充当新构建。

## 下一步

1. 等待用户对 `1.2.7.1`（继承 r1 全部修复）的实机验收与反馈：旧课表时间/节次、窄列完整时间、附件菜单与日视图上传、无壁纸配色、液态加号间距/振动及快速设置标题/行排版。对应测试与 Release 构建已通过；没有自动 UI 或性能验收，不据构建或发布成功替代真实效果判断。
2. 本次更新送达修复和双平台补发已完成，无待发布候选。自动检查沿用每日一次规则；用户可在设置中手动检查 `1.2.7.1`。如反馈仍无法发现更新，再依据实际请求、渠道与设备证据定位，不重复修改已验证的版本排序。
3. 如需继续修复，围绕真实场景定位后追加独立提交，按风险构建；不要重复已排除的容器触控监听方案，也不要上传系统反编译源码。
4. 达到可评审状态后再转 Ready for review，检查整个 PR 的 CI 和未完成项；草稿 skipped 不替代该步骤。
5. 获得合并授权后处理 main 回流，默认用保留提交祖先关系的 merge commit；合并后核对正式版源码可从 main 到达，再从新 main 开下一任务。合并不包含 Release 授权。本轮已发布但未回流，`main` 与发布基线差异仍未收口。

## 用户要求与授权来源摘要

来源是本任务会话的用户指令；下列内容方便检索原消息，不替代原始授权：

- “先整理一下混乱的工作目录……和最新 release 对应上，然后执行该任务”：对应正式版基线与渲染任务。
- “不需要进行奇奇怪怪的测试……打包 release……自己测试”及后续“先交付 APK”：保持最小充分验证，先交付 APK；用户随后明确要求“安装”，已覆盖安装本轮最新包，未自动启动。
- 后续横屏滑选、侧栏按钮触控和标题调整：继续同一修复任务，当前仍待实机反馈。
- “帮我整理一下仓库的情况 github 一直给我发 pr run”：对应已完成的旧 PR 关闭、CI 与失败项修复。
- 本轮“整理……他人贡献……创建 PR 和提交合并规范……切换其他 AI……接手”：统一协作文档与交接。没有获得实际合并 #78、发布、改写标签或删除其他工作树的新增授权。
- 最新“继续改……横屏凝光 tabbar……AI 加号……口令导入……自动计算当前周……新导入彩色模式……教务搜索框闪烁”：继续六项体验修订，按既有交付要求打包供用户测试。
- 随后“安装”已于 13:08 完成；“查看手机最新截图……柔性反馈和发光被限定在内框”及“而且亮度太亮”：读取用户指定的最新截图，继续修正导航绘制与光效，沿用签名 Release 打包安装交付。
- 最新“继续修改……加号 popup 收回错位……左边缘对齐……无壁纸单色……分类大标题……周视图今天上过的课不用变灰”；补充明确要求“日视图助理也支持上传文件”。继续同一 PR 和既有签名 Release 安装交付，不把上传入口误解为另造解析链路。
- 后续截图反馈时间截断、GDCP 多校区作息，并明确“不要只适配这一个学校……按这类情况标准化处理”“升级之后原来的导入好的也要一起更新显示方式”；同时要求上传 Popup 文字反色、导入预览文字、液态加号与 Dock 间距/点击振动及快速设置文字对齐/换行修复。只更新呈现，保留原始课程时间与用户数据。
- 最新“以当前代码状态 不要改 versioncode 将版本名字改为 1.2.7-r1 并按项目规范书写更新日志 然后重新打包 上传 github/gitee 双平台 release”：完成 `1.2.7-r1` 修复版构建与双平台正式发布。明确约束是 `versionCode` 不改（保持 35），并明确该版属正式 release 而非 pre-release；发布对象为本轮代码状态，未合并 `main`。
- 2026-10-20 本会话用户要求“昨晚发布了1.2.7-r1 但是1.2.7收不到此项更新 你帮解决一下”，选择 `1.2.7.1` 并要求“你把r1的发布给下了吧”。随后明确回复“允许，使用现有脚本连接撤下 r1”及“允许，使用现有脚本双平台补发 1.2.7.1”，授权本次 GitHub 插件能力缺口的例外、任务提交推送、新标签与双平台正式补发。两项均已完成，源码标签保留；不包含合并 main 或其他发布授权。

原会话无法访问时，先完成可逆的核对与本地工作；需要新的远端写入、合并或发布时确认可追溯授权，不能把本页视为一份无限授权。

## 必须保留的本地内容

- stash 按名称 `workspace-before-nexio-glass-2026-10-18` 定位，其中 AI 作息校验已单独恢复并提交，其他内容与原 stash 保留；不要整份 pop 或 drop。另有 `backup: untracked app sources before home menu morph`，未处理。
- 最初未跟踪素材保存在本机忽略目录 `tmp/workspace-preserved-20261018/`；不是可随意删除的构建垃圾。
- `git worktree list` 中还有其他任务，包括教务适配与历史性能/转场工作。本页不表示这些工作树干净、停用或可以删除。
- 本轮文档整理期间出现未跟踪的 `app/src/main/assets/shiguang_warehouse-main/scripts/__pycache__/school_index_pb2.cpython-312.pyc`，属于生成缓存，未纳入文档提交，也未清理其他进程产生的文件。后续以 `git status` 核对，不应据此宣称源码有待提交修改。
- 私有后端、签名与用户数据不进入交接附件、仓库或日志。机器专用的 JDK/ADB/构建参数见 `AGENTS.md`，新环境需核对实际路径。

## 按需阅读的证据

- [正式版基线整理](reports/2026-10-18-workspace-release-baseline.md)
- [首轮玻璃渲染](reports/2026-10-18-nexio-glass-parity.md)与[展开/材质反馈](reports/2026-10-18-glass-feedback-followup.md)
- [横屏输入首轮](reports/2026-10-19-landscape-tab-input.md)与[HD 组件及最终分发诊断](reports/2026-10-19-coui-hd-dispatch-audit.md)：前者是当时尝试，后者修正根因
- [PR、Actions 与 main 回流盘点](reports/2026-10-19-repository-pr-actions-cleanup.md)
- [最新导航、Popup、导入与默认值修订](reports/2026-10-19-navigation-import-feedback.md)
- [横屏凝光整条胶囊反馈与亮度修订](reports/2026-10-19-navigation-shell-feedback.md)
- [助理 Popup、日视图文件上传与课程外观修订](reports/2026-10-19-popup-day-file-card-feedback.md)
- [通用多作息显示与界面反馈](reports/2026-10-19-standard-clock-ui-feedback.md)
