# PE-D Issue Review

---

## Functionality Bugs

### 1. [#184] 输入 Unicode 箭头字符 → 导致程序意外退出

- **Type:** Functionality
- **Problem:** 在主菜单 `linuxlingo>` 提示符输入 `→`（Unicode 箭头字符）后，程序直接输出 `Goodbye!` 并退出，而非显示错误信息并保持在提示符。
- **How to reproduce:** 启动程序 → 在 `linuxlingo>` 提示符输入 `→` → 按 Enter
- **Expected behavior:** 显示 `→: command not found`，程序保持在提示符等待下一条命令。
- **Response:** ✅ 已确认为 bug。程序不应因任何合法的用户输入而意外退出。根本原因可能是 JLine 读取 Unicode 字符时返回了 `null`，MainParser 或 ShellSession 将 `null` 输入视为 EOF 并触发退出流程。需要在输入读取层添加 null-safety 检查并正确处理 Unicode 输入。

### 2. [#195] `cp -r` 允许将根目录复制到子目录中，创建名为 `/` 的非法目录

- **Type:** Functionality
- **Problem:** `cp -r / .` 可以把根目录复制到当前目录中，导致创建一个名为 `/` 的目录，与路径分隔符冲突，造成后续路径解析完全混乱。
- **How to reproduce:** `cd /home/user` → `cp -r / .` → `ls`
- **Expected behavior:** 禁止创建名为 `/` 的文件/目录；检测并阻止将源目录递归复制到其子目录中。
- **Response:** ✅ 已确认为 bug。真实 Linux 的 `cp -r` 会阻止将目录复制到自身的子目录中（`cp: cannot copy a directory, '/', into itself, '.'`）。VFS 层应禁止创建名为 `/` 的子节点，`cp -r` 命令应检测源路径是目标路径的祖先并报错。与 #175 属于同一类问题。

### 3. [#175] `cp -r` 未阻止将父目录复制到子目录

- **Type:** Functionality
- **Problem:** 在真实 Linux 中 `cp -r` 会阻止将父目录复制到子目录（避免无限递归），但本程序不会阻止此操作。
- **How to reproduce:** 在 shell 中将一个父目录用 `cp -r` 复制到其子目录
- **Expected behavior:** 报错 `cannot copy a directory into itself`。
- **Response:** ✅ 已确认为 bug，与 #195 属于同一问题（`cp -r` 缺少递归复制的祖先检测）。将合并修复。

### 4. [#188] `rm -rf ~/` 可删除当前工作目录（Home 目录）

- **Type:** Functionality
- **Problem:** 用户在 `/home/user` 目录中执行 `rm -rf ~/` 会删除整个 Home 目录，导致后续命令（如 `ls`）报错 `No such file or directory: /home/user`，用户处于"幽灵目录"中。
- **How to reproduce:** `cd /home/user` → `rm -rf ~/` → `ls`
- **Expected behavior:** 阻止删除当前工作目录或 Home 目录，或至少优雅地报错。
- **Response:** ✅ 已确认为 bug。在真实 Linux 中，`rm -rf ~/` 确实可以删除 home 目录（这不是 Linux 会阻止的），但用户会陷入"幽灵目录"状态是我们模拟器的问题。应在 `rm` 命令中检测并阻止删除当前工作目录及其祖先目录，或在删除后自动将 CWD 回退到最近的存在的祖先目录。

### 5. [#190] `rm -rf ~/` 在存在同名 `~` 目录时仍删除 Home 目录

- **Type:** Functionality
- **Problem:** 创建一个名为 `~` 的字面量目录后，`rm -rf ~/` 仍然展开波浪号删除真正的 Home 目录，而非删除字面量 `~` 目录。
- **How to reproduce:** `mkdir ~/~/` → `cd ~/~/` → `rm -rf ~/` → `ls`
- **Expected behavior:** 应优先考虑字面路径解析，或阻止删除正在使用的 Home 目录。
- **Response:** ❌ 不是独立 bug，是 #188 的重复。波浪号 `~` 在路径开头（`~/`）被展开为 `/home/user` 是标准 Linux shell 行为，这一行为本身是正确的。如果用户希望引用名为 `~` 的字面目录，应使用 `./~` 或用引号包裹。核心问题（删除 home 目录导致幽灵状态）已在 #188 中处理。

### 6. [#187] 未限制路径深度和文件名长度，导致 UI 崩溃

- **Type:** Functionality
- **Problem:** 没有对目录嵌套深度和文件名长度设置上限。用户可以创建 50+ 层目录和 200+ 字符的文件名，导致路径解析失败、tree 显示截断、CLI 布局完全破坏。
- **How to reproduce:** `mkdir -p a/a/a/a/...`（50+ 层）→ `touch aaa...`（200+ 字符）→ 尝试查看或导航
- **Expected behavior:** 强制执行标准文件系统限制（文件名 255 字符，路径 4096 字符），超限时显示清晰错误。
- **Response:** ✅ 已确认为 bug。虽然正常教学使用不会触及极端长度，但程序应当能够优雅地处理边界输入而非崩溃或产生不可预期的行为（如路径解析失败、`tree` 显示截断、CLI 布局破坏）。这属于防御性编程的范畴——程序不应允许用户将 VFS 置于不可恢复的状态。修复方案：在 VFS 的 `createFile` 和 `createDirectory` 中添加文件名长度上限（255 字符）和路径深度上限（合理值如 50 层），超限时返回清晰的错误信息。

### 7. [#185] 反斜杠 `\` 在路径中的处理不符合 Linux 标准

- **Type:** Functionality
- **Problem:** `mkdir` 接受含反斜杠的路径，但内部解析不一致——反斜杠被去除或误解析，导致错误信息中路径显示不正确。
- **How to reproduce:** `mkdir a/b/\a/b/`
- **Expected behavior:** 将 `\` 视为字面字符（标准 Linux 行为），或明确拒绝。
- **Response:** ❌ 不视为 bug。UG 的 Notes about the command format 中明确记载："Outside of quotes, a backslash (`\`) escapes the next character, treating it as a literal." 这正是标准 shell 行为——反斜杠在引号外作为转义字符使用。`mkdir a/b/\a/b/` 中 `\a` 被解析为字面字符 `a`（反斜杠转义了 `a`），因此实际路径是 `a/b/a/b/`。这与真实 bash 行为一致。如果用户希望在文件名中包含字面反斜杠，应使用 `\\` 或将路径用单引号包裹。tester 对此行为的理解有误。

### 8. [#180] 文件名中含 `/` 导致删除时找不到文件

- **Type:** Functionality
- **Problem:** `touch "rm -rf /"` 可以创建含 `/` 的文件名，但 `rm "rm -rf /"` 时路径逻辑将 `/` 当作目录分隔符处理，导致找不到文件。
- **How to reproduce:** `touch "rm -rf /"` → `ls`（显示 `rm -rf`）→ `rm "rm -rf /"`
- **Expected behavior:** 能正确删除该文件，或在创建时就阻止含 `/` 的文件名。
- **Response:** ✅ 已确认为 bug。在真实 Linux 中，文件名不能包含 `/`（这是 POSIX 强制要求的）。`touch "rm -rf /"` 中 `/` 被 VFS 的 `normalizePath` 解析为路径分隔符，导致不一致行为。应在 VFS 的 `createFile` 和 `createDirectory` 中验证最终的文件/目录名组件不包含 `/` 字符，拒绝非法文件名。

### 9. [#179] `rm` 无法删除以连字符 `-` 开头的文件

- **Type:** Functionality
- **Problem:** `touch -file-name-` 可以创建文件，但 `rm -rf -file-name-` 时解析器将文件名当作 flag 处理，报告 `rm: missing operand`。
- **How to reproduce:** `touch -file-name-` → `rm -rf -file-name-`
- **Expected behavior:** 文件 `-file-name-` 被成功删除（应支持 `--` 终止 flag 解析）。
- **Response:** ✅ 已确认为 bug。真实 Linux 中所有命令都支持 `--` 作为选项终止符，之后的所有参数均被视为位置参数（operand）而非选项。目前 `rm`、`cp`、`ls` 等命令的 flag 解析逻辑不支持 `--`。应在所有支持 flag 的命令中添加 `--` 支持。与 #178 属于同一类问题。

### 10. [#178] `ls` 将位置参数中以 `-` 开头的文本误当作选项

- **Type:** Functionality
- **Problem:** `ls rm -rf /` 中 `-rf` 被当作 `ls` 的 flag，报错 `ls: invalid option -- r`，而非将其视为文件名参数。
- **How to reproduce:** `touch rm` → `ls rm -rf /`
- **Expected behavior:** 正确区分选项和位置参数。
- **Response:** ✅ 已确认为 bug，与 #179 属于同一问题（缺少 `--` 选项终止符支持）。`ls rm -rf /` 在真实 Linux 中，`-rf` 也会被解析为 `ls` 的选项——这在真实 Linux 中行为相同。但核心问题是缺少 `--` 支持来让用户显式终止选项解析（如 `ls -- -rf`）。将合并修复。

### 11. [#176] Unicode 字符支持不完整

- **Type:** Functionality
 (tester 标注 VeryLow，但被重新标记为 High)
- **Problem:** 在 shell 中使用 Unicode 字符时输出不正确，与标准 Linux 行为不一致。
- **How to reproduce:** 在 shell 中输入含 Unicode 字符的命令
- **Response:** ✅ 已确认为 bug。根据附图所示的复现步骤，Unicode 字符在 shell 中确实存在处理不正确的问题，行为与标准 Linux 不一致。虽然 Java 的 `String` 类型天然支持 Unicode，但 JLine 终端交互层和 ShellParser 的 tokenizer 可能未正确处理多字节 Unicode 字符的输入和输出。与 #184 相关但不完全重复——#184 是 Unicode 导致退出，本 issue 是 Unicode 在正常命令执行中输出不正确。需排查 JLine 配置、ShellParser tokenizer 及命令输出层对 Unicode 字符的处理。

### 12. [#174] `echo -e` 的转义序列不生效

- **Type:** Functionality
 (tester 标注 Low，被重新标记为 High)
- **Problem:** `echo -e` 应当解释 `\n`、`\t`、`\b`、`\a` 等转义序列，但实际上只有 `\\` 能被正确处理，其他转义序列均被原样输出。
- **How to reproduce:** `echo -e "line1\nline2"`（应输出两行，实际输出 `line1\nline2`）
- **Expected behavior:** `-e` 标志下 `\n` 输出换行，`\t` 输出制表符等。
- **Response:** ✅ 已确认为 bug。UG 明确记载 `echo -e` 支持 `\n`、`\t`、`\\`、`\a`、`\b` 转义序列，代码中 `EchoCommand` 也实现了对应的转义处理逻辑。问题在于 `ShellParser` 的 tokenizer 在双引号内将 `\n` 中的反斜杠作为转义字符消费掉，导致 `echo` 命令收到的参数已经是 `line1nline2`（反斜杠被去掉了），而不是 `line1\nline2`。需要调整 tokenizer 在双引号内对反斜杠的处理方式，或让 `echo -e` 在 tokenizer 之前获取原始字符串。

### 13. [#186] `grep -c` 匹配为 0 时无输出（应显示 `0`）

- **Type:** Functionality
- **Problem:** `grep -c hello test3.txt`（test3.txt 中无 "hello"）不输出任何内容，直接返回提示符。
- **How to reproduce:** `echo "no match here" > test3.txt` → `grep -c hello test3.txt`
- **Expected behavior:** 输出 `0`。
- **Response:** ✅ 已确认为 bug。真实 Linux `grep -c` 即使匹配数为 0 也会输出 `0`（或 `filename:0`）。当前代码在匹配数为 0 时跳过了输出。与 #189 属于同一问题，将合并修复。

### 14. [#189] `grep -c` 多文件搜索时静默省略匹配为 0 的文件

- **Type:** Functionality
- **Problem:** `grep -c hello test1.txt test2.txt test3.txt` 只输出 `test1.txt:1`，省略了匹配数为 0 的文件。
- **How to reproduce:** 创建三个文件，只有 test1.txt 含 "hello" → `grep -c hello test1.txt test2.txt test3.txt`
- **Expected behavior:** 输出所有文件的计数，包括 `test2.txt:0` 和 `test3.txt:0`。
- **Response:** ✅ 已确认为 bug，与 #186 属于同一问题。`grep -c` 应始终输出每个文件的匹配计数（包括 0），将合并修复。

### 15. [#197] 空字符串 `""` 被静默当作 `/` 处理

- **Type:** Functionality
- **Problem:** `mkdir ""` 报错 `Directory already exists:`（末尾空），`cd ""` 静默不做任何操作。空字符串参数未被正确验证。
- **How to reproduce:** `cd /home` → `mkdir ""`
- **Expected behavior:** 报错提示路径无效或需要目录名。
- **Response:** ✅ 已确认为 bug。空字符串 `""` 经过 VFS 的 `normalizePath` 后被当作当前目录解析（拼接为 `cwd + "/" + ""`，split 后为空），导致行为等同于操作当前目录。真实 bash 中 `mkdir ""` 会报 `mkdir: cannot create directory '': No such file or directory`，`cd ""` 不做任何操作（这点与真实 bash 一致）。应在命令执行前对空字符串参数进行验证并报错。

### 16. [#177] `cp` 不带 `-r` 复制目录时错误信息重复 "cp"

- **Type:** Functionality
- **Problem:** 不带 `-r` 复制目录时，错误信息中出现两个 "cp"（如 `cp: cp: omitting directory 'xxx'`）。
- **How to reproduce:** 不使用 `-r` 标志执行 `cp` 复制目录
- **Expected behavior:** 错误信息中只出现一个 `cp:` 前缀。
- **Response:** ✅ 已确认为 bug。这是 `CpCommand` 中错误消息拼接的简单问题——VFS 层已经返回了带 `cp:` 前缀的错误消息，`CpCommand` 又额外加了一个 `cp:` 前缀。修复方案：移除重复的前缀。

### 17. [#182] `history` 错误信息中 "arguments" 拼写错误为 "arguemnts"

- **Type:** Functionality
- **Problem:** `history -c 1` 报错 `history: too many arguemnts`，"arguments" 拼写错误。
- **How to reproduce:** `history -c 1`
- **Expected behavior:** `history: too many arguments`
- **Response:** ✅ 已确认为 bug。`HistoryCommand` 中的错误消息字符串 `"arguemnts"` 拼写错误，应为 `"arguments"`。简单的 typo 修复。

---

## User Experience Issues

### 18. [#192] `alias ll=ls -la`（无引号）报错而非按 UG 所述静默处理

- **Type:** User Experience
- **Problem:** UG 明确说明 `alias ll=ls -la`（不加引号）应将 `ll` 别名为 `ls`，并静默忽略 `-la`。但实际行为是报错 `alias: too many arguments`。这是 UG 描述与实际行为的矛盾。
- **How to reproduce:** `alias ll=ls -la`
- **Expected behavior:** 按 UG 描述，创建 `ll` → `ls` 的别名并忽略 `-la`；或者修改 UG 说明。
- **Response:** ✅ 已确认为 bug（代码与 UG 不一致）。UG 中 alias 章节的 Note 明确写道："`alias ll=ls -la` will only alias `ll` to `ls`, silently ignoring `-la`"。但代码中 `AliasCommand` 在 `args.length > 1` 时直接返回 `"alias: too many arguments"`。修复方案：修改代码使其匹配 UG 的描述——当有多余参数时，仅取第一个参数中 `=` 后的值作为别名值，静默忽略其余参数。

### 19. [#191] 无法识别的 token（如 `then`）静默吞掉后续有效命令

- **Type:** User Experience
- **Problem:** `echo "hello" > u.txt then sort u.txt` 中 `then` 之后的所有内容被静默丢弃，无任何输出或错误信息。
- **How to reproduce:** `echo "hello" > u.txt then sort u.txt`
- **Expected behavior:** 报语法错误 `unexpected token 'then'`，或将 `then` 视为 `echo` 的参数。
- **Response:** ✅ 已确认为 UX bug。虽然 `then` 在 bash 中不是通用关键字（仅在 `if`/`for`/`while` 上下文中），但当前行为的核心问题是**静默丢弃用户输入**——重定向 `> u.txt` 之后的 `then sort u.txt` 被 ShellParser 放入同一 segment 作为 `echo` 的参数，但由于重定向，所有输出被写入文件，用户在终端上看不到任何反馈，也不知道 `sort u.txt` 没有被执行。对于教学工具而言，静默丢弃用户意图是不可接受的——应当至少给出提示信息。修复方案：当解析器发现重定向操作符后仍有非操作符 token 时，可以将它们作为参数传递（匹配 bash 行为），但输出时应在终端上给出提示，或者考虑在 UG 中明确说明重定向的行为。

### 20. [#193] 错误的拼写纠正建议：输入 `1` 建议 `cd`

- **Type:** User Experience
- **Problem:** 输入 `1` 作为命令时，系统建议 `Did you mean 'cd'?`。虽然 Levenshtein 距离 ≤ 2，但这个建议毫无意义。
- **How to reproduce:** 在 shell 中输入 `1`
- **Expected behavior:** 对无意义的输入不显示建议，或提高建议阈值。
- **Response:** ❌ 不视为 bug。命令建议功能使用 Levenshtein 编辑距离 ≤ 2 的阈值，这是 DG 中明确记载的设计决策。`1` 到 `cd` 的编辑距离恰好为 2（替换 `1`→`c`，插入 `d`），满足阈值条件。虽然该建议对人类来说语义上不直观，但这是基于编辑距离算法的固有特性。将阈值改为 1 会导致许多有用的建议（如 `grpe`→`grep`，距离 2）失效。可以考虑添加额外的启发式规则（如要求输入长度 ≥ 2 才给出建议），但这属于 feature enhancement 而非 bug。

### 21. [#183] `history` 输入无效 flag 时错误信息具有误导性

- **Type:** User Experience
- **Problem:** `history -l` 报错 `history: numeric argument required` 而非 `invalid option -- l`。
- **How to reproduce:** `history -l`
- **Expected behavior:** `history: invalid option -- l`
- **Response:** ✅ 已确认为 bug。`HistoryCommand` 对非 `-c` 的参数一律尝试 `parseInt()`，非数字字符串抛出 `NumberFormatException` 后报 `numeric argument required`，而没有区分"以 `-` 开头的无效选项"和"非数字字符串"两种情况。应先检查参数是否以 `-` 开头且不是 `-c`，如果是则报 `invalid option`。

### 22. [#181] `history` 参数超过 32 位整数范围时错误信息有误导性

- **Type:** User Experience
- **Problem:** `history 12345679872`（超 int 范围）报错 `numeric argument required`，暗示输入非数字，实际上输入确实是数字。
- **How to reproduce:** `history 12345679872`
- **Expected behavior:** `numeric argument out of range`
- **Response:** ✅ 已确认为 bug。`HistoryCommand` 使用 `Integer.parseInt()` 解析参数，超过 32 位整数范围时抛出 `NumberFormatException`，错误信息 `numeric argument required` 具有误导性（暗示输入不是数字）。应使用 `Long.parseLong()` 先尝试解析，如果值超出合理范围则报 `numeric argument out of range`。

---

## Feature Suggestions

### 23. [#173] 缺少中途退出考试的功能

- **Type:** Feature
- **Problem:** 用户进入考试后无法中途退出，只能反复输入 `quit` 或按 Enter 跳过每道题直到结束。对于目标用户（CS 学生）来说体验不佳。
- **Expected behavior:** 提供一个命令（如 `exit` 或 `abort`）可以在考试过程中随时退出，直接显示已答题目的得分。
- **Response:** ✅ 已确认为合理的功能建议。当前 UG 中提到可以用 `quit` 跳过单个问题，但没有提供中途退出整个考试的方式。将添加 `abort` 命令支持，在考试过程中输入 `abort` 可立即结束考试并显示已答题目的得分。

---

## UG (User Guide) Issues

### 24. [#196] UG 中 `wc` 命令缺少示例输出

- **Type:** UG
- **Problem:** UG 中 `wc` 命令的示例只写了 `wc readme.txt – show line, word, and character counts`，没有展示实际输出格式。用户无法从 UG 中得知输出的三个数字分别代表什么。
- **Expected behavior:** 添加示例输出，如 `1  3 16 readme.txt`，并说明各列含义。
- **Response:** ✅ 接受。将在 UG 中为 `wc` 命令添加示例输出，明确展示输出格式和各列含义。

### 25. [#194] UG 中 `ls -a` 示例输出与 `ls` 完全相同，无法体现 `-a` 的作用

- **Type:** UG
- **Problem:** UG 中 `ls -a` 的示例输出（`home/ tmp/ etc/`）与 `ls` 完全一致，读者无法理解 `-a` 标志的用途。
- **Expected behavior:** 添加包含隐藏文件的示例，或注明默认 VFS 中无隐藏文件。
- **Response:** ✅ 接受。UG 中 `ls -a` 的示例确实无法体现其作用。将添加注释说明默认 VFS 中没有隐藏文件（以 `.` 开头的文件），因此 `ls -a` 和 `ls` 的输出相同，同时补充一个包含隐藏文件的示例场景。

### 26. [#170] UG 排版需要改进——内容在页面之间被截断

- **Type:** UG
- **Problem:** UG 中部分章节内容被分割到两个页面上（PDF 格式），使读者难以连贯阅读。
- **Expected behavior:** 在 PDF 渲染中确保每个章节/命令说明不被分页打断。
- **Response:** ✅ 接受，但优先级较低。UG 以 Markdown 格式编写，PDF 渲染的分页控制取决于 Markdown→PDF 转换工具的能力。将尝试通过调整章节结构和添加 CSS page-break 规则来改善。

### 27. [#171] UG 可读性需改善——命令与描述不易区分

- **Type:** UG
- **Problem:** UG 全文文字颜色和字体区分度不够，命令语法与文字描述难以快速区分，尤其对 Linux 新手不友好。
- **Expected behavior:** 使用不同颜色/字体样式来突出命令、格式说明和示例。
- **Response:** ✅ 接受，但优先级较低。将审查 UG 的 Markdown 格式，确保命令语法使用 code 格式（反引号）一致标记，示例使用代码块，并考虑添加更多视觉区分。

### 28. [#169] UG 缺少终端截图等视觉元素

- **Type:** UG
- **Problem:** 作为教育类应用，UG 中缺少终端截图，用户无法直观看到命令执行后的终端实际效果。
- **Expected behavior:** 为关键命令添加终端截图，帮助用户了解预期输出。
- **Response:** ✅ 接受，但优先级较低。将为关键功能（shell 入口、exam 流程、典型命令输出）添加终端截图。

---

## DG (Developer Guide) Issues

### 29. [#172] DG 排版需要改进——内容在页面之间被截断

- **Type:** DG
- **Problem:** 与 UG 同样的问题，DG 中部分章节内容被分割到两个页面上，影响阅读连贯性。
- **Expected behavior:** 确保每个章节不被页面分割打断。
- **Response:** ✅ 接受，与 #170 属于同一类问题（PDF 排版），将一并处理。
