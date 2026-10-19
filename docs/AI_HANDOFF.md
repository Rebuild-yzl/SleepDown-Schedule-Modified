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
| 已提交的运行时代码锚点 | `955236e6`：COUI 事件分发修复；`8dcb2ddf`：恢复 AI 暂存作息校验；后续协作文档以 `git log` 核对 |
| 当前目标 | 渲染/横屏交互等待用户验收；仓库 PR/CI 已收口，本轮统一贡献、提交、合并与换助手交接规范 |

从 main 建立本任务分支后，只在任务分支快进到正式版。因此 #78 还携带 64 个已发布提交，不能把它当作只含几处界面修改的小 PR。main 与发布基线差异尚未实际收口。新独立任务的分支选择按开发流程处理，不继续从历史 `develop` 或 `exp` 开发。

本轮交接、协作文档和草稿 CI 规则位于 #78 分支，main 尚未同步。其他机器续做本任务应先获取并核对该分支。已核对 main 受保护，仓库允许 merge、squash、rebase 三种合并方式；GitHub 集成读取保护细则返回 403，本轮没有据旧文档断言具体审批数量，也未改动仓库设置。

## 已完成与关键结论

- 课程玻璃改为共享全分辨率壁纸前缀与逐卡 Lens，处理课程详情展开时 producer 自订阅造成的持续更新；保留必要的共享 Offscreen 栅格边界。用户反馈左右切周已有明显改善，未作 FPS 测量。
- 横屏凝光顶部胶囊保留原生 Segment。其内部 TextView 会消费触控，容器 OnTouchListener 不能观察完整序列；现用透明 FrameLayout 的 `dispatchTouchEvent` 观察 DOWN 和分发后的 UP/CANCEL，抬手再导航。已审计本地 HD 侧栏组件职责，不能拿侧栏或双栏控件直接替代顶部胶囊。
- 侧栏/底栏切换按钮拆出独立点击区域；个性化收紧标题间距，一级与二级标题按用户要求同为 titleMedium，以字重区分。
- 已关闭发布内容覆盖的旧草稿 #57、#61、#65，保留分支和历史。草稿 CI 跳过规则已在 #78 实际验证，等待可评审时运行完整 CI。
- 恢复 `AgentImportWorkspace.kt` 的原有 7 行作息校验，修复连续失败的那项云端测试。规则、模板和文档入口本轮已统一，Miuix 构建说明改为唯一完整补丁。

## 验证与产物：按代码锚点区分

| 对象 | 实际证据与边界 |
| --- | --- |
| 渲染/交互 APK | `955236e6` 完整 `assembleGithubRelease` 通过，5m32s，保留 R8、资源压缩、lintVital 和正式 v2 签名；`c609b385` 记录交付 |
| AI 暂存校验 | 恢复后仅运行 `AgentImportWorkspaceTest.overlappingAndGappedPeriodsAreRejectedBeforePublishingAStage`，1 项通过、0 失败；含编译 6m02s，没有重跑 623 项套件 |
| 草稿 CI | [run 39191153304](https://github.com/xiaomanjun233/SleepDown-Schedule/actions/runs/39191153304)，提交 `856d9b91`，三个 job 均 skipped；只证明草稿门控生效，不表示完整 CI 通过 |
| 本轮协作文档 | 核对内容、链接与 diff；没有 Android 代码改动，无需重建 APK |
| 安装与实机 | 用户最后选择“先交付 APK”，该包未安装、未启动；横屏滑选及视觉效果仍待用户验收 |

已交付包的本机副本：`tmp/review-apks/SleepDown-1.2.7-COUI-gesture-dispatch-20261019.apk`，7,574,871 字节，SHA-256 `11b826b0c0e30d8c3e85d1a533c8dbdb6c7b6079a51bfa738899c3abdaac15ac`。它是 `1.2.7` / 35，**不包含其后恢复的 AI 作息校验**。临时产物不入库，其他机器可能没有此文件；需要新包时从所选代码重新构建，不把旧包改名充当新构建。

## 下一步

1. 根据用户对已交付包的反馈继续 #78，重点确认液态玻璃课程详情展开、横屏原生滑选、侧栏按钮触控与个性化标题。用户未要求安装前不重复索要设备连接。
2. 如需继续修复，围绕真实场景定位后追加独立提交，按风险构建；不要重复已排除的容器触控监听方案，也不要上传系统反编译源码。
3. 达到可评审状态后再转 Ready for review，检查整个 PR 的 CI 和未完成项；草稿 skipped 不替代该步骤。
4. 获得合并授权后处理 main 回流，默认用保留提交祖先关系的 merge commit；合并后核对正式版源码可从 main 到达，再从新 main 开下一任务。合并不包含 Release 授权。

## 用户要求与授权来源摘要

来源是本任务会话的用户指令；下列内容方便检索原消息，不替代原始授权：

- “先整理一下混乱的工作目录……和最新 release 对应上，然后执行该任务”：对应正式版基线与渲染任务。
- “不需要进行奇奇怪怪的测试……打包 release……自己测试”及后续“先交付 APK”：保持最小充分验证，最后一轮只交包，没有延用更早的安装请求去安装该包。
- 后续横屏滑选、侧栏按钮触控和标题调整：继续同一修复任务，当前仍待实机反馈。
- “帮我整理一下仓库的情况 github 一直给我发 pr run”：对应已完成的旧 PR 关闭、CI 与失败项修复。
- 本轮“整理……他人贡献……创建 PR 和提交合并规范……切换其他 AI……接手”：统一协作文档与交接。没有获得实际合并 #78、发布、改写标签或删除其他工作树的新增授权。

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
