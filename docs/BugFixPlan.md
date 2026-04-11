# Bug Fix Plan

本文档基于 PE-D Issue Review 的分析结果，列出所有已确认的 true positive bugs 的修复计划。
相似或重复的 issue 已合并为统一的修复项。

---

## 修复优先级说明

- **P0 (Critical):** 导致程序崩溃或退出的问题
- **P1 (High):** 功能性错误，与真实 Linux 行为不符且影响正常使用
- **P2 (Medium):** 错误信息不准确或用户体验问题
- **P3 (Low):** 文档改进、低优先级功能增强

---

## Fix 1: Unicode 输入导致程序意外退出 [P0]

**关联 Issue:** #184

**问题根因:**
JLine 在读取某些 Unicode 字符（如 `→`）时可能返回 `null` 或抛出异常，`MainParser`/`ShellSession` 的 REPL 循环将 `null` 输入视为 EOF 并触发程序退出。

**修复方案:**
1. 在 `ShellSession.start()` 和 `MainParser` 的 REPL 循环中，增加对 JLine `readLine()` 返回 `null` 的区分处理：
   - 如果是真正的 EOF（用户按 Ctrl+D），则正常退出。
   - 如果是 JLine 解析错误导致的 `null`，则打印错误提示并继续循环。
2. 在 `ShellLineReader` 中包裹 JLine 调用，捕获可能的异常，返回空字符串或错误提示而非 `null`。

**涉及文件:**
- `ShellSession.java` — `start()` 方法
- `ShellLineReader.java` — `readLine()` 方法
- `MainParser.java` — REPL 循环

**测试:**
- 输入各种 Unicode 字符（`→`、`←`、`中文`、`emoji`）验证不会退出。
- 验证 Ctrl+D (EOF) 仍然正常退出。

---

## Fix 2: `cp -r` 缺少递归复制祖先检测 [P1]

**关联 Issue:** #195, #175

**问题根因:**
`CpCommand` 和 VFS 的 `copyNode()` 没有检测源路径是否是目标路径的祖先（或反之），导致：
- `cp -r / .` 可以将根目录复制到子目录中，创建名为 `/` 的非法子节点。
- `cp -r parent/ parent/child/` 导致无限递归风险。

**修复方案:**
1. 在 `CpCommand.execute()` 中，递归复制前检测：
   - 源路径的绝对路径是否是目标路径的前缀（祖先），如果是则报错 `cp: cannot copy a directory, 'X', into itself, 'Y'`。
   - 源路径和目标路径是否相同，如果是则报错 `cp: 'X' and 'Y' are the same file`。
2. 在 VFS 的 `Directory.addChild()` 中，验证子节点名称不能是 `/`（空字符串或仅含 `/`）。

**涉及文件:**
- `CpCommand.java` — `execute()` 方法
- `VirtualFileSystem.java` 或 `Directory.java` — `addChild()` / `copyNode()` 方法

**测试:**
- `cp -r / .` → 报错。
- `cp -r /home /home/user/backup` → 报错。
- `cp -r dir1 dir2`（正常情况）→ 正常复制。

---

## Fix 3: `rm` 删除当前工作目录导致幽灵状态 [P1]

**关联 Issue:** #188（#190 是其重复）

**问题根因:**
`RmCommand` 不检查被删除的目录是否是当前工作目录（CWD）或其祖先。删除后 CWD 指向一个不存在的路径，后续所有基于 CWD 的操作都会失败。

**修复方案:**
在 `RmCommand.execute()` 中，删除目录前检查：
1. 将要删除的路径解析为绝对路径。
2. 检查 CWD 的绝对路径是否以该路径为前缀（即 CWD 位于该目录内部或就是该目录）。
3. 如果是，报错 `rm: cannot remove 'X': current working directory is inside this directory`。

或者采用更宽松的策略：允许删除，但在删除后自动将 CWD 回退到最近的仍然存在的祖先目录（模拟真实 Linux 的行为）。考虑到这是教学工具，**建议采用阻止删除的策略**，更安全且易于理解。

**涉及文件:**
- `RmCommand.java` — `execute()` 方法

**测试:**
- `cd /home/user && rm -rf /home/user` → 报错。
- `cd /home/user && rm -rf /home` → 报错。
- `cd / && rm -rf /home/user` → 正常删除（CWD 不在被删目录内）。

---

## Fix 4: VFS 文件名验证缺失 [P1]

**关联 Issue:** #180, #197（部分涉及 #195）

**问题根因:**
VFS 的 `createFile()` 和 `createDirectory()` 不验证文件名的合法性：
- 不检查文件名是否包含 `/`（POSIX 禁止）。
- 不检查文件名是否为空字符串。
- 不检查文件名是否为 `.` 或 `..`（保留名）。

**修复方案:**
1. 在 `VirtualFileSystem` 中添加一个 `validateFileName(String name)` 方法：
   - 名称为空或仅含空白 → 抛出 `VfsException("invalid file name")`。
   - 名称包含 `/` → 抛出 `VfsException("invalid character '/' in file name")`。
   - 名称为 `.` 或 `..` → 根据上下文判断（通常不应创建这些名称的文件）。
2. 在 `createFile()`、`createDirectory()`、`Directory.addChild()` 中调用此验证。
3. 针对 #197 的 `""` 问题：在 `mkdir`、`touch` 等命令中，对空字符串参数提前报错。

**涉及文件:**
- `VirtualFileSystem.java` — 添加 `validateFileName()` 方法，修改 `createFile()`/`createDirectory()`
- `Directory.java` — `addChild()` 方法
- `MkdirCommand.java`、`TouchCommand.java` — 添加空字符串参数检查

**测试:**
- `touch "a/b"` → 报错（文件名含 `/`）。
- `mkdir ""` → 报错（空文件名）。
- `touch ""` → 报错。
- `touch normal.txt` → 正常。

---

## Fix 5: 命令缺少 `--` 选项终止符支持 [P1]

**关联 Issue:** #179, #178

**问题根因:**
所有命令的 flag 解析逻辑都不支持 `--` 作为选项终止符。以 `-` 开头的文件名参数会被误解析为命令选项。

**修复方案:**
在所有支持 flag 的命令（`rm`、`cp`、`ls`、`mkdir`、`grep`、`chmod`、`head`、`tail`、`sort`、`uniq`、`wc`、`find`、`cat`、`echo`）的 flag 解析循环中：
1. 遇到 `--` 时设置 `endOfOptions = true` 标志并跳过该参数。
2. 当 `endOfOptions` 为 `true` 时，所有后续 `-` 开头的参数都作为位置参数处理。

建议提取一个通用的 `ArgParser` 工具类来统一处理 flag 解析和 `--` 支持，避免在每个命令中重复实现。

**涉及文件:**
- 所有支持 flag 的 Command 类（约 14 个文件）
- （可选）新建 `ArgParser.java` 工具类

**测试:**
- `touch -- -file` → 创建名为 `-file` 的文件。
- `rm -- -file` → 删除名为 `-file` 的文件。
- `ls -- -rf` → 列出名为 `-rf` 的文件。

---

## Fix 6: `echo -e` 转义序列不生效 [P1]

**关联 Issue:** #174

**问题根因:**
`ShellParser` 的 tokenizer 在 `NORMAL` 状态下将 `\` 作为转义字符处理（消费反斜杠并将下一个字符作为字面量）。当用户输入 `echo -e "line1\nline2"` 时，反斜杠在 tokenizer 阶段就被处理掉了，`echo` 命令收到的参数是 `line1nline2` 而非 `line1\nline2`，导致 `echo -e` 的转义处理无法工作。

**修复方案:**
问题在于 tokenizer 的 `NORMAL` 状态下对反斜杠的处理过于激进。在真实 bash 中：
- 在**双引号内**，只有 `\$`、`` \` ``、`\"``、`\\`、`\newline` 这几个组合中反斜杠才有转义含义，其他 `\x` 组合（如 `\n`、`\t`）保留原样（反斜杠 + 字符都保留）。
- 在**引号外**，反斜杠转义下一个字符。

修复 `ShellParser` 的 `IN_DOUBLE_QUOTE` 状态：
1. 在双引号内遇到 `\` 时，peek 下一个字符。
2. 如果下一个字符是 `$`、`` ` ``、`"`、`\`、换行符之一，则消费反斜杠并转义。
3. 否则，**保留反斜杠和下一个字符原样**（`\n` → 字面 `\n` 两个字符）。

这样 `echo -e "line1\nline2"` 中 `\n` 在 tokenizer 阶段保留，`EchoCommand` 的 `-e` 模式可以正确处理转义。

**涉及文件:**
- `ShellParser.java` — `tokenize()` 方法中 `IN_DOUBLE_QUOTE` 状态的反斜杠处理

**测试:**
- `echo -e "line1\nline2"` → 输出两行。
- `echo -e "tab\there"` → 输出制表符分隔的文本。
- `echo -e "backslash\\"` → 输出一个反斜杠。
- `echo "hello\nworld"` → 输出 `hello\nworld`（不带 `-e` 不转义，但 `\n` 应被保留）。
- `echo hello\ world` → 输出 `hello world`（引号外的反斜杠转义行为不变）。

---

## Fix 7: `grep -c` 匹配数为 0 时不输出 [P1]

**关联 Issue:** #186, #189

**问题根因:**
`GrepCommand` 在 `-c` 模式下，只在匹配行数 > 0 时才输出计数。真实 `grep -c` 对每个文件都输出计数，包括 0。

**修复方案:**
修改 `GrepCommand` 中 `-c` 模式的输出逻辑：
1. 对每个文件，无论匹配行数是否为 0，都输出计数行。
2. 单文件时输出格式：`0`（直接输出数字）。
3. 多文件时输出格式：`filename:0`。

**涉及文件:**
- `GrepCommand.java` — `-c` 模式的输出部分

**测试:**
- `echo "no match" > f.txt && grep -c hello f.txt` → 输出 `0`。
- 创建三个文件，只有一个包含匹配 → `grep -c pattern f1 f2 f3` → 三行输出，包括 `f2:0` 和 `f3:0`。

---

## Fix 8: `cp` 错误信息重复 "cp" 前缀 [P2]

**关联 Issue:** #177

**问题根因:**
`CpCommand` 在构造错误消息时添加了 `"cp: "` 前缀，但 VFS 的 `copyNode()` 方法返回的错误消息已经包含了 `"cp: omitting directory"` 前缀，导致最终输出 `"cp: cp: omitting directory 'xxx'"`。

**修复方案:**
检查 `CpCommand` 中所有构造 `CommandResult.error()` 的地方，确保不会重复添加 `"cp: "` 前缀。有两种修复方式：
- 方式 A：在 VFS 层的错误消息中去掉 `"cp: "` 前缀（VFS 不应知道调用者是哪个命令）。
- 方式 B：在 `CpCommand` 中不再额外添加前缀。

**推荐方式 A**，因为 VFS 作为底层组件不应包含命令名前缀。

**涉及文件:**
- `CpCommand.java` — 错误消息构造
- `VirtualFileSystem.java` — `copyNode()` 中的错误消息（如适用）

**测试:**
- `cp dir1 dir2`（不带 `-r`）→ 错误消息只有一个 `cp:` 前缀。

---

## Fix 9: `history` 命令的多个错误信息问题 [P2]

**关联 Issue:** #182, #183, #181

**问题根因:**
`HistoryCommand` 存在三个问题：
1. `"arguemnts"` 拼写错误（#182）。
2. 无效 flag（如 `-l`）被当作数字参数解析，报错 `numeric argument required` 而非 `invalid option`（#183）。
3. 超过 int 范围的数字报错 `numeric argument required` 而非 `out of range`（#181）。

**修复方案:**
重构 `HistoryCommand.execute()` 的参数解析逻辑：
1. 修正 `"arguemnts"` → `"arguments"`。
2. 参数解析优先级：
   - 如果参数是 `-c`：清除历史。
   - 如果参数以 `-` 开头且不是 `-c`：报错 `history: invalid option -- <char>`。
   - 如果参数是数字字符串：
     - 先用 `Long.parseLong()` 尝试解析。
     - 如果超出 int 范围 → 报错 `history: <value>: numeric argument out of range`。
     - 如果是负数 → 报错 `history: <value>: invalid count`。
   - 如果不是数字 → 报错 `history: <value>: numeric argument required`。

**涉及文件:**
- `HistoryCommand.java` — `execute()` 方法

**测试:**
- `history -c 1` → `history: too many arguments`。
- `history -l` → `history: invalid option -- l`。
- `history 12345679872` → `history: 12345679872: numeric argument out of range`。
- `history abc` → `history: abc: numeric argument required`。
- `history 5` → 正常显示最近 5 条。

---

## Fix 10: `alias` 命令与 UG 描述不一致 [P2]

**关联 Issue:** #192

**问题根因:**
UG 说明 `alias ll=ls -la`（不加引号）应将 `ll` 别名为 `ls`，静默忽略 `-la`。但代码中 `AliasCommand` 对 `args.length > 1` 直接返回 `"alias: too many arguments"`。

**修复方案:**
修改 `AliasCommand.execute()` 使其行为与 UG 一致：
1. 当 `args.length > 1` 时，不再直接报错。
2. 仅处理第一个参数（`args[0]`），检查是否包含 `=`：
   - 包含 `=`：正常创建别名，静默忽略后续参数。
   - 不包含 `=`：显示该别名的值（查看别名），静默忽略后续参数。

**涉及文件:**
- `AliasCommand.java` — `execute()` 方法

**测试:**
- `alias ll=ls -la` → 创建 `ll` → `ls` 的别名（不报错）。
- `alias ll='ls -la'` → 创建 `ll` → `ls -la` 的别名（已有的正确行为）。

---

## Fix 11: 考试系统缺少中途退出功能 [P2]

**关联 Issue:** #173

**问题根因:**
`ExamSession` 和 `QuestionInteraction` 的循环中没有处理"中途终止整个考试"的命令。用户只能用 `quit` 逐题跳过。

**修复方案:**
1. 在 `QuestionInteraction` 中，当用户输入 `abort` 时，设置一个 `examAborted` 标志并跳出当前题目。
2. 在 `ExamSession` 的题目循环中检查 `examAborted` 标志，如果为 `true` 则跳出循环。
3. 无论是正常完成还是 abort，都调用 `ExamResult` 显示已答题目的得分（只统计已回答的题目）。
4. 更新 UG 说明，记录 `abort` 命令的功能。

**涉及文件:**
- `QuestionInteraction.java` — 答题逻辑
- `ExamSession.java` — 题目循环
- `ExamResult.java` — 得分计算（可能需要支持部分完成）

**测试:**
- 开始 10 题考试，答 3 题后输入 `abort` → 显示 3 题的得分，返回主菜单。
- 正常完成考试 → 行为不变。

---

## Fix 12: VFS 文件名长度和路径深度限制 [P2]

**关联 Issue:** #187

**问题根因:**
VFS 没有对文件名长度和目录嵌套深度设置上限。用户可以创建极长文件名（200+ 字符）或极深目录（50+ 层），导致路径解析失败、`tree` 显示截断、CLI 布局破坏等不可恢复的状态。虽然正常教学使用不会触及极限，但程序应能优雅地处理边界输入。

**修复方案:**
1. 在 `VirtualFileSystem` 的 `validateFileName()` 方法中（Fix 4 新增）添加文件名长度检查：
   - 文件名长度 > 255 字符 → 报错 `File name too long`（与 Linux `ENAMETOOLONG` 对齐）。
2. 在 `createDirectory()` / `createFile()` 中添加路径深度检查：
   - 计算当前目录深度（从根到目标位置的层数），如果超过合理上限（如 50 层）→ 报错 `Directory nesting too deep`。
3. 可选：在 `normalizePath()` 中添加总路径长度检查（4096 字符上限）。

**涉及文件:**
- `VirtualFileSystem.java` — `createFile()`、`createDirectory()`、`validateFileName()`

**测试:**
- `touch aaa...`（256+ 字符文件名）→ 报错 `File name too long`。
- `mkdir -p a/a/a/...`（51+ 层）→ 报错 `Directory nesting too deep`。
- 正常长度文件名和路径 → 正常工作。

---

## Fix 13: Unicode 字符在 shell 中输出不正确 [P2]

**关联 Issue:** #176（与 #184 相关但不重复）

**问题根因:**
JLine 终端交互层和/或 ShellParser 的 tokenizer 未正确处理多字节 Unicode 字符的输入和输出。#184 是 Unicode 导致程序退出的问题（已在 Fix 1 中处理），本 issue 是 Unicode 字符在正常命令执行中输出不正确（如乱码、截断或丢失字符）。

**修复方案:**
1. 检查 `ShellLineReader` 中 JLine 的 Terminal/LineReader 配置，确保编码设置为 UTF-8。
2. 检查 `ShellParser.tokenize()` 中的字符遍历逻辑是否正确处理 Unicode surrogate pairs（Java `String.charAt()` 对 BMP 外字符返回 surrogate pair）。
3. 检查各 Command 的输出路径（`Ui.printMessage()` 等）是否使用了正确的字符编码。
4. 在 `ShellSession` 中，确保 JLine 的 `Terminal` 使用 `StandardCharsets.UTF_8`。

**涉及文件:**
- `ShellLineReader.java` — JLine Terminal/LineReader 初始化
- `ShellParser.java` — `tokenize()` 字符遍历
- `ShellSession.java` — Terminal 配置
- `Ui.java` — 输出编码

**测试:**
- `echo 你好世界` → 输出 `你好世界`。
- `touch 文件.txt && ls` → 正常显示 `文件.txt`。
- `echo "emoji: 🎉"` → 正确输出 emoji。
- 各种 Unicode 字符作为文件名和内容的 CRUD 操作均正常。

---

## Fix 14: 重定向后的额外 token 被静默处理，缺乏用户反馈 [P2]

**关联 Issue:** #191

**问题根因:**
当用户输入 `echo "hello" > u.txt then sort u.txt` 时，ShellParser 的 `buildPlan()` 在处理完重定向 `> u.txt` 后，将 `then`、`sort`、`u.txt` 作为 `echo` 的额外参数放入同一个 segment。由于输出被重定向到文件，用户在终端上看不到任何反馈，也不知道 `sort u.txt` 没有被执行。对于教学工具来说，静默丢弃用户意图是不可接受的 UX 问题。

**修复方案:**
方案 A（推荐）：修改 ShellParser 使重定向后的额外 token 行为与 bash 一致（作为命令参数），但在命令执行时，如果输出被重定向且存在 `then`/`do`/`done`/`fi` 等常见控制流关键字作为参数，输出一条警告提示（如 `warning: 'then' is not a command separator, treated as argument`）。

方案 B：在 ShellParser 中，当重定向操作符后的 token 不是下一个操作符（`|`、`&&`、`;`）时，检查是否存在看起来像控制流关键字的 token，并输出提示。

**涉及文件:**
- `ShellParser.java` — `buildPlan()` 方法
- （或）执行引擎中检测可疑参数并输出警告

**测试:**
- `echo "hello" > u.txt then sort u.txt` → 给出提示信息（如警告 `then` 不是命令分隔符）。
- `echo hello > u.txt` → 正常重定向，无警告。
- `echo hello world > u.txt` → 正常，无警告。

---

## Fix 15: UG 文档改进 [P3]

**关联 Issue:** #196, #194, #170, #171, #169

**修复内容:**
1. **#196 — `wc` 添加示例输出：** 在 `wc` 章节添加具体的输出示例（如 `1  3 16 readme.txt`）并标注各列含义（lines, words, characters, filename）。
2. **#194 — `ls -a` 示例改进：** 添加注释说明默认 VFS 无隐藏文件，并补充一个 `touch .hidden && ls -a` 的示例来展示 `-a` 的效果。
3. **#170 & #172 — PDF 排版：** 在 Markdown 中添加 CSS page-break 规则（`<div style="page-break-before: always;"></div>`），避免重要章节被分页打断。
4. **#171 — 可读性：** 审查全文确保命令语法使用反引号标记一致。
5. **#169 — 终端截图：** 为 shell 入口、exam 流程添加终端截图。

**涉及文件:**
- `docs/UserGuide.md`
- `docs/DeveloperGuide.md`

---

## 修复顺序建议

按优先级和依赖关系，建议的修复顺序如下：

| 顺序 | Fix | 优先级 | 预估工作量 | 说明 |
|------|-----|--------|-----------|------|
| 1 | Fix 1 (Unicode 退出) | P0 | 小 | 最高优先级，阻止程序崩溃 |
| 2 | Fix 9 (history 错误信息) | P2 | 小 | 简单 typo + 逻辑修复，快速完成 |
| 3 | Fix 8 (cp 重复前缀) | P2 | 小 | 简单字符串修复 |
| 4 | Fix 4 (文件名验证) | P1 | 中 | 基础设施改进，其他 fix 可能依赖 |
| 5 | Fix 12 (文件名/路径限制) | P2 | 小 | 在 Fix 4 基础上扩展，一并完成 |
| 6 | Fix 7 (grep -c 输出) | P1 | 小 | 逻辑修复 |
| 7 | Fix 6 (echo -e 转义) | P1 | 中 | 需要修改 tokenizer，需谨慎测试 |
| 8 | Fix 13 (Unicode 输出) | P2 | 中 | 排查 JLine 编码配置和 tokenizer |
| 9 | Fix 2 (cp -r 祖先检测) | P1 | 中 | 需要路径比较逻辑 |
| 10 | Fix 3 (rm 删除 CWD) | P1 | 中 | 需要 CWD 路径检查 |
| 11 | Fix 5 (-- 终止符) | P1 | 大 | 涉及多个命令文件 |
| 12 | Fix 10 (alias 行为) | P2 | 小 | 简单逻辑修改 |
| 13 | Fix 14 (重定向后 token 警告) | P2 | 中 | 需要解析器层面的检测逻辑 |
| 14 | Fix 11 (考试退出) | P2 | 中 | 涉及考试流程修改 |
| 15 | Fix 15 (UG/DG 文档) | P3 | 中 | 文档工作 |
