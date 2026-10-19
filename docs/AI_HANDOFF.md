# 当前工作交接

更新：2026-10-19，Asia/Shanghai。此页保存维护者主工作目录的当前交接，历史过程引用报告；不代表其他 worktree 的任务已结束。接手前完整阅读 [AGENTS.md](../AGENTS.md)，再核对实际 Git、PR、用户新指令与本页是否一致。流程和交接字段见 [DEVELOPMENT_WORKFLOW.md](DEVELOPMENT_WORKFLOW.md)。

给下一位助手的简短入口：

> 先读 AGENTS.md 和 docs/AI_HANDOFF.md，核对当前分支、工作树及 PR 状态，接着完成未完事项。沿用可核实的用户授权和已有成果，按本次变化选择验证；遇到差异先查事实并更新交接。

## 当前分支与目标

| 对象 | 核对结果 |
| --- | --- |
| 主工作目录 | 仓库根目录 `CourseSchedule`；当前分支 `codex/nexio-glass-parity` |
| 关联 PR | [#78](https://github.com/xiaomanjun233/SleepDown-Schedule/pull/78)，目标 `main`，Draft，未合并 |
| 最新正式版 | `v1.2.7`，源码 `cfe8776f8c2811e54cc4ff23445cfd67f2b6af9e`，应用 `1.2.7` / 35 |
| main | `a320f6a8`，实际 `1.2.7_beta5` / 34，是正式版祖先，落后 64 个提交 |
| 已提交的运行时代码锚点 | 最新 `78f1aa94` 修正横屏凝光整条胶囊的反馈范围与亮度；此前 `0f019f1d` 含六项反馈修订，独立提交见[修订报告](reports/2026-10-19-navigation-import-feedback.md)；包含原事件分发和 AI 暂存校验修复 |
| 当前目标 | 根据手机截图修正横屏凝光内轨裁剪、整条胶囊柔性反馈与过亮的光效，已重新打包安装；此前六项体验修订继续等待用户验收 |

从 main 建立本任务分支后，只在任务分支快进到正式版。因此 #78 还携带 64 个已发布提交，不能把它当作只含几处界面修改的小 PR。main 与发布基线差异尚未实际收口。新独立任务的分支选择按开发流程处理，不继续从历史 `develop` 或 `exp` 开发。

本轮交接、协作文档和草稿 CI 规则位于 #78 分支，main 尚未同步。其他机器续做本任务应先获取并核对该分支。已核对 main 受保护，仓库允许 merge、squash、rebase 三种合并方式；GitHub 集成读取保护细则返回 403，本轮没有据旧文档断言具体审批数量，也未改动仓库设置。

## 已完成与关键结论

- 课程玻璃改为共享全分辨率壁纸前缀与逐卡 Lens，处理课程详情展开时 producer 自订阅造成的持续更新；保留必要的共享 Offscreen 栅格边界。用户反馈左右切周已有明显改善，未作 FPS 测量。
- 横屏凝光顶部胶囊保留原生 Segment。其内部 TextView 会消费触控，容器 OnTouchListener 不能观察完整序列；现用透明 FrameLayout 的 `dispatchTouchEvent` 观察 DOWN 和分发后的 UP/CANCEL，抬手再导航。已审计本地 HD 侧栏组件职责，不能拿侧栏或双栏控件直接替代顶部胶囊。
- 本轮继续保留 Segment 实例与弹簧，设置/课表明暗变化采用原地 refresh。导航锁由宿主控制，避免 `setEnabled(false)` 触发原生取消动画；刷新后重申无外框协议。
- 侧栏/底栏切换按钮拆出独立点击区域；个性化收紧标题间距，一级与二级标题按用户要求同为 titleMedium，以字重区分。
- 已关闭发布内容覆盖的旧草稿 #57、#61、#65，保留分支和历史。草稿 CI 跳过规则已在 #78 实际验证，等待可评审时运行完整 CI。
- 恢复 `AgentImportWorkspace.kt` 的原有 7 行作息校验，修复连续失败的那项云端测试。规则、模板和文档入口本轮已统一，Miuix 构建说明改为唯一完整补丁。
- AI 加号改用模型选单同款 Popup Morph；导入保留旧视觉到弹窗卸载后再展示新课表。新导入使用彩色模式，着色 50%、模糊 35%、折射 50%，其他参数对齐已有标记点。
- 自动教学周始终开启，详细/快速设置和 AI 的手动切换入口已移除。旧字段继续兼容，缺少开学日期时保留原周并提示补充；新建、配置保存、启动和备份恢复均接 Repository 规范化。
- 教务搜索框采用 ModulateAlpha 保持原生模糊采样，底部渐变同步入场，避免逐帧重复设置原生功能按钮文字。

## 验证与产物：按代码锚点区分

| 对象 | 实际证据与边界 |
| --- | --- |
| 最新 APK | `78f1aa94` 完整 `assembleGithubRelease` 通过，9m13s，保留 R8、资源压缩、lintVital 和正式 v2 签名；包含导航整条胶囊反馈、亮度修订、此前六项反馈与 AI 校验 |
| AI 暂存校验 | 恢复后仅运行 `AgentImportWorkspaceTest.overlappingAndGappedPeriodsAreRejectedBeforePublishingAStage`，1 项通过、0 失败；含编译 6m02s，没有重跑 623 项套件 |
| 草稿 CI | [run 39191153304](https://github.com/xiaomanjun233/SleepDown-Schedule/actions/runs/39191153304)，提交 `856d9b91`，三个 job 均 skipped；只证明草稿门控生效，不表示完整 CI 通过 |
| 上轮日期/默认值 | 对应 `0f019f1d`：现有 `ScheduleTermBoundaryTest` 8 项与 `ImportedCourseCardDefaultsTest` 1 项通过，共 9 项；含编译 3m37s，未跑完整套件；本轮导航样式修订未重跑这些测试 |
| 安装与实机 | 2026-10-19 13:24:58 经项目 ADB 5038 通道覆盖安装本轮最新包到 USB 设备 PLJ110（Android 17），返回 Success，系统核对为 `1.2.7` / 35；未自动启动，反馈范围及亮度仍待用户验收。截图证据与过程见[本轮报告](reports/2026-10-19-navigation-shell-feedback.md) |

最新包的本机副本：`tmp/review-apks/SleepDown-1.2.7-navigation-full-capsule-20261019.apk`，7,574,871 字节，SHA-256 `41f6768d737f9845bc5ce8e987e5f3768f7b139ccade6a795f7a5c4351d79eed`，版本 `1.2.7` / 35。此前 `navigation-import-defaults`（SHA-256 `501c3455…`）及 `COUI-gesture-dispatch` 副本均为旧包，范围不同。临时产物不入库，其他机器可能没有文件；需要新包时重新构建，不把旧包改名充当新构建。

## 下一步

1. 根据用户对最新包的反馈继续 #78：优先确认横屏顶部导航的整条胶囊形变、光斑跨内框及文字可读性；此前 AI 加号 Popup、口令导入交接、自动周次、彩色默认值和教务搜索框入场仍待用户反馈。
2. 如需继续修复，围绕真实场景定位后追加独立提交，按风险构建；不要重复已排除的容器触控监听方案，也不要上传系统反编译源码。
3. 达到可评审状态后再转 Ready for review，检查整个 PR 的 CI 和未完成项；草稿 skipped 不替代该步骤。
4. 获得合并授权后处理 main 回流，默认用保留提交祖先关系的 merge commit；合并后核对正式版源码可从 main 到达，再从新 main 开下一任务。合并不包含 Release 授权。

## 用户要求与授权来源摘要

来源是本任务会话的用户指令；下列内容方便检索原消息，不替代原始授权：

- “先整理一下混乱的工作目录……和最新 release 对应上，然后执行该任务”：对应正式版基线与渲染任务。
- “不需要进行奇奇怪怪的测试……打包 release……自己测试”及后续“先交付 APK”：保持最小充分验证，先交付 APK；用户随后明确要求“安装”，已覆盖安装本轮最新包，未自动启动。
- 后续横屏滑选、侧栏按钮触控和标题调整：继续同一修复任务，当前仍待实机反馈。
- “帮我整理一下仓库的情况 github 一直给我发 pr run”：对应已完成的旧 PR 关闭、CI 与失败项修复。
- 本轮“整理……他人贡献……创建 PR 和提交合并规范……切换其他 AI……接手”：统一协作文档与交接。没有获得实际合并 #78、发布、改写标签或删除其他工作树的新增授权。
- 最新“继续改……横屏凝光 tabbar……AI 加号……口令导入……自动计算当前周……新导入彩色模式……教务搜索框闪烁”：继续六项体验修订，按既有交付要求打包供用户测试。
- 随后“安装”已于 13:08 完成；“查看手机最新截图……柔性反馈和发光被限定在内框”及“而且亮度太亮”：读取用户指定的最新截图，继续修正导航绘制与光效，沿用签名 Release 打包安装交付。

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
