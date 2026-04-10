# LinuxLingo Developer Guide

- [Acknowledgements](#acknowledgements)
- [Setting Up, Getting Started](#setting-up-getting-started)
- [Design](#design)
  - [Architecture](#architecture)
  - [CLI Component](#cli-component)
  - [Shell Component](#shell-component)
  - [Exam Component](#exam-component)
  - [Storage Component](#storage-component)
  - [VFS (Virtual File System) Component](#vfs-virtual-file-system-component)
- [Implementation](#implementation)
  - [Shell Parsing and Execution](#shell-parsing-and-execution)
  - [Command Execution with Piping and Redirection](#command-execution-with-piping-and-redirection)
  - [Alias Resolution and Variable Expansion](#alias-resolution-and-variable-expansion)
  - [Glob Expansion](#glob-expansion)
  - [Command Suggestion ("Did you mean?")](#command-suggestion-did-you-mean)
  - [VFS Environment Persistence](#vfs-environment-persistence)
  - [Exam Session Flow](#exam-session-flow)
  - [Exam Component — Practical Questions](#exam-component--practical-questions)
  - [Question Parsing and Loading](#question-parsing-and-loading)
  - [Resource Extraction on First Run](#resource-extraction-on-first-run)
- [Appendix A: Product Scope](#appendix-a-product-scope)
- [Appendix B: User Stories](#appendix-b-user-stories)
- [Appendix C: Non-Functional Requirements](#appendix-c-non-functional-requirements)
- [Appendix D: Glossary](#appendix-d-glossary)
- [Appendix E: Instructions for Manual Testing](#appendix-e-instructions-for-manual-testing)

---

## Acknowledgements

- [AddressBook-Level3 (AB3)](https://se-education.org/addressbook-level3/) — Project structure and Developer Guide format adapted from SE-EDU.
- [PlantUML](https://plantuml.com/) — Used for UML diagram generation.
- [Gradle Shadow Plugin](https://github.com/johnrengelman/shadow) — Used for building fat JARs.
- [JLine 3](https://github.com/jline/jline3) — Used for tab-completion and command history in the interactive shell.

---

## Setting Up, Getting Started

**Prerequisites:**

1. JDK 17 or above.
2. Gradle 7.x (wrapper included — use `./gradlew`).

**Building the project:**

```shell
./gradlew build
```

**Running the application:**

```shell
./gradlew run
```

**Running tests:**

```shell
./gradlew test
```

---

## Design

### Architecture

The **Architecture Diagram** below gives a high-level overview of LinuxLingo.

![Architecture Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/ArchitectureDiagram.puml)

**Main components of the architecture:**

`LinuxLingo` (the main class) is in charge of app launch. At startup, it:

- Extracts bundled question bank resources to disk (via `ResourceExtractor`).
- Loads the `QuestionBank` from `data/questions/`.
- Creates the shared `VirtualFileSystem`, `ShellSession`, and `ExamSession`.
- Delegates to `MainParser` for interactive mode, or handles one-shot CLI commands directly.

The bulk of the app's work is done by the following components:

| Component | Responsibility |
| --------- | -------------- |
| **CLI** | Handles user I/O (`Ui`) and top-level command dispatch (`MainParser`). |
| **Shell** | Parses and executes shell commands in a simulated Linux environment. |
| **Exam** | Manages exam sessions — question presentation, answer checking, scoring. |
| **Storage** | Reads/writes data on the real file system (question banks, VFS snapshots). |
| **VFS** | In-memory virtual file system that all shell commands operate on. |

**How the components interact with each other:**

The following sequence diagram shows the interactions when the user launches the app in interactive mode and types `shell`:

![Architecture Sequence Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/ArchitectureSequenceDiagram.puml)

---

### CLI Component

The CLI component consists of two classes: `Ui` and `MainParser`.

![CLI Class Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/CliClassDiagram.puml)

**`Ui`** is the single point of contact for all user-facing I/O. It wraps `Scanner` for input and `PrintStream` for output. All components use `Ui` instead of directly calling `System.in`/`System.out`, making testing easier (injectable streams).

**`MainParser`** implements the top-level REPL loop. It reads user input and dispatches to one of: `shell` (enter Shell Simulator), `exam` (start an exam), `exec` (one-shot shell command), `help`, or `exit`/`quit`.

---

### Shell Component

The Shell component handles command parsing, execution, and the interactive REPL. It is the largest component in LinuxLingo.

The following class diagram shows the key classes. For clarity, only representative command implementations are shown; the full set of 36 commands follows the same `Command` interface.

![Shell Class Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/ShellClassDiagram.puml)

The Shell component:

- Uses `ShellParser` to tokenize and parse raw input into a `ParsedPlan` (a list of `Segment` objects connected by operators).
- `ShellSession` iterates through segments, looking up each command name in `CommandRegistry`, executing them, and chaining results via pipes, `&&`, `||`, or `;`.
- Before command lookup, `ShellSession` resolves aliases, expands combined flags (e.g., `-la` → `-l -a`), expands glob patterns against the VFS, and expands shell variables (`$USER`, `$HOME`, `$PWD`).
- Each `Command` implementation receives the current `ShellSession` (for VFS access and session state), parsed arguments, and optional piped stdin. It returns a `CommandResult` containing stdout, stderr, and an exit code.

**Supported commands (36 total):**

| Category | Commands |
| -------- | -------- |
| Navigation | `cd`, `ls`, `pwd` |
| File Operations | `mkdir`, `touch`, `rm`, `cp`, `mv`, `cat`, `echo`, `diff`, `tee` |
| Text Processing | `head`, `tail`, `grep`, `find`, `wc`, `sort`, `uniq` |
| Permissions | `chmod` |
| Information | `man`, `tree`, `which`, `whoami`, `date` |
| Alias & History | `alias`, `unalias`, `history` |
| Environment | `save`, `load`, `reset`, `envlist`, `envdelete` |
| Utility | `help`, `clear` |

---

### Exam Component

The Exam component handles question presentation, answer checking, and score tracking.

![Exam Class Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/ExamClassDiagram.puml)

The Exam component:

- `QuestionBank` loads question data files from `data/questions/` (via `QuestionParser`) and organizes them by topic.
- `ExamSession` orchestrates exam sessions with three entry points: interactive mode, direct CLI args, and single-random-question mode.
- Three question types are supported: `McqQuestion` (multiple choice), `FitbQuestion` (fill in the blank), and `PracQuestion` (practical — verified by checking VFS state against `Checkpoint` objects).
- `ExamResult` tracks per-question outcomes and computes scores.

---

### Storage Component

The Storage component handles all real disk I/O operations.

![Storage Class Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/StorageClassDiagram.puml)

The Storage component:

- `Storage` provides static utility methods for file read/write, directory creation, and path management. All persistent data lives under `data/`.
- `VfsSerializer` converts VFS snapshots to/from a custom `.env` text format, enabling users to save and load shell environments.
- `QuestionParser` parses `.txt` question bank files into `Question` objects using a pipe-delimited format.
- `ResourceExtractor` copies bundled question bank files from the JAR to `data/questions/` on first run.

---

### VFS (Virtual File System) Component

The VFS component provides an in-memory simulated Linux file system.

![VFS Class Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/VfsClassDiagram.puml)

Key design decisions for VFS:

- **All path operations go through `VirtualFileSystem`** — shell commands never use `java.io` or `java.nio.file` for simulated files. This ensures a fully isolated, deterministic simulation.
- The VFS uses a **tree structure** of `FileNode` objects. `Directory` holds a `LinkedHashMap` of children for ordered, O(1) lookup by name.
- **`Permission`** models Unix 9-character permission strings (`rwxr-xr-x`), supporting both octal and symbolic notation for `chmod`.
- **`deepCopy()`** is provided at every level (VFS, Directory, RegularFile) to enable snapshot-based features (e.g., creating a temp VFS for PRAC exam questions, saving environments).
- The default VFS tree contains `/home/user`, `/tmp`, and `/etc` (with a `hostname` file).

The following object diagram illustrates a concrete VFS state after the user has created a file `hello.txt` under `/home/user`:

![VFS Object Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/VfsObjectDiagram.puml)

---

## Implementation

This section describes some noteworthy details on how certain features are implemented.

### Shell Parsing and Execution

The shell parsing pipeline transforms the raw user input string like `echo hello | grep h > out.txt` into a structured execution plan (`ParsedPlan`) that the execution engine can act on. Understanding this pipeline is essential before working on any feature that involves parsing or command execution.


**Parsing Pipeline Overview: (High Level)**

![Parsing Pipeline Activity Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/ParsingPipelineActivity.puml)

The `ShellParser.parse()` method runs the input through two stages: **tokenization**, then **plan building**

![Shell Parser Sequence Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/ShellParserSequence.puml)

#### Stage 1: Tokenization
The tokenizer reads the input one character at a time using a state machine with three states:

| State | Behaviour                                                                                                                                                                                     |
|---|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `NORMAL` | Accumulates characters into tokens; whitespace flushes the current token.<br/> Special characters (` \| `, `>`, `>>`, `&&`, `;`) produce operator tokens. <br/>Quote characters switch state. |
| `IN_SINGLE_QUOTE` | All characters are literal until the closing `'`                                                                                                                                              |
| `IN_DOUBLE_QUOTE` | All characters are literal until the closing `"`                                                                                                                                              |

After tokenization, the token list is split into `Segment` objects at inter-segment operators (`PIPE`, `AND`, `SEMICOLON` , `OR`). 
Within each segment, `REDIRECT` / `APPEND` / `INPUT_REDIRECT` tokens consume the next `WORD` token as the redirect target file.

The parser handles two lookahead cases during tokenization in `NORMAL` state: `||` and `>>` are
distinguished from `|` and `>` by peeking at the next character before emitting a token.

A lone `&` character (not followed by another `&`) is treated as a literal word character
rather than an operator, which matches standard shell behaviour.

#### Stage 2: Plan Building
Once the flat token list is produced, `buildPlan()` walks through it and groups tokens into `Segment` objects. Each `Segment` holds a command name, its arguments, and optional redirect information. Operator tokens (`PIPE`, `AND`, `SEMICOLON`, `OR`) act as delimiters between segments and are recorded separately in the `operators` list.

The parser maintains an `Expecting` flag to handle redirect targets: when a `REDIRECT` or `APPEND` token is seen, the very next `WORD` token is consumed as the redirect file path rather than as a command argument. The same pattern applies to `INPUT_REDIRECT` (`<`).

The result is a `ParsedPlan` with the following invariant, enforced by an assertion:

> `operators.size()` is always exactly `segments.size() - 1`

This means a plan with three segments always has exactly two operators connecting them, making the execution engine's iteration straightforward.

**Execution engine (`ShellSession.runPlan()`):**

All parsing ultimately feeds into `runPlan()`, the core method that iterates the `ParsedPlan` and chains commands together. It is worth understanding its structure because most enhancements to the shell (new operators, alias resolution, input redirect) are implemented here.

The engine tracks two pieces of state across iterations: `pipedStdin` (the stdout of the previous command, forwarded when the operator was `PIPE`) and `lastExitCode` (used to evaluate `&&` and `||` conditions). The loop processes one `Segment` per iteration:

![Run Plan Sequence Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/RunPlanSequence.puml)

**Key Behaviours**

**Redirect consumes stdout:** After a redirect, the result is replaced with an empty success.  
  Example:  
  `echo hello > file | grep h`  
  → `grep` receives empty stdin (matching standard shell behaviour).

**Exit propagation:** The `shouldExit()` flag on `CommandResult` causes `runPlan()` to set  
  `running = false` and break the loop immediately.

The following sequence diagram shows how `echo hello | grep h > output.txt` is executed:

![Echo Grep Pipe Execution Sequence](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/EchoGrepPipeSequence.puml)

<!-- When a command name is not found in the registry, `runPlan()` calls `suggestCommand()`
before printing the error. This method computes the Levenshtein edit distance between
the mistyped input and every registered command name, returning a "Did you mean 'X'?"
hint if the closest match is within distance 2. Glob patterns in arguments (containing
`*` or `?`) are expanded against the VFS via `expandGlobs()` before the command receives
them, if no VFS paths match the pattern, the literal argument is passed through unchanged. -->

**Operator semantics:**

| Operator | Symbol | Behavior |
| -------- | ------ | -------- |
| `PIPE` | &#124; | stdout of segment N becomes stdin of segment N+1 |
| `AND` | `&&` | Segment N+1 runs only if segment N succeeded (exit code 0) |
| `OR` | `\|\|` | Segment N+1 runs only if segment N failed (exit code ≠ 0) |
| `SEMICOLON` | `;` | Segment N+1 always runs regardless of exit code |

---

### Command Execution with Piping and Redirection

All 36 commands follow the same implementation pattern:

1. Parse flags and arguments from `args[]`.
2. Determine input source: file arguments take priority over piped `stdin`.
3. Call VFS methods on `session.getVfs()`.
4. Catch `VfsException` → return `CommandResult.error(...)`.
5. Return `CommandResult.success(output)`.

The following activity diagram shows the input resolution logic for commands that support both file arguments and piped stdin (e.g., `cat`, `head`, `tail`, `grep`, `sort`, `uniq`, `wc`):

![Input Resolution Activity Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/InputResolutionActivity.puml)

**Input redirection** (`<`) is handled by the execution engine before command execution: if a segment has an `inputRedirect` file, the engine reads that file's content from the VFS and passes it as `stdin` to the command.

---

### Alias Resolution and Variable Expansion

**Aliases** allow users to define shortcuts for commonly used commands (e.g., `alias ll='ls -la'`). The alias system is implemented in `ShellSession`:

- Aliases are stored in a `LinkedHashMap<String, String>` within `ShellSession`.
- Before each command lookup, `resolveAlias()` repeatedly resolves the command name through the alias map. A visited set prevents infinite loops from circular alias definitions.
- Aliases persist only within the current shell session and are not saved across restarts.

**Variable expansion** supports a limited set of shell variables:

| Variable | Value |
| -------- | ----- |
| `$USER` | `user` |
| `$HOME` | `/home/user` |
| `$PWD` | Current working directory |
| `$?` | Exit code of the last command |

Variables inside single-quoted strings are not expanded (single-quoted tokens are marked with a `\0` prefix during tokenization). The `expandVariablesInString()` method scans each argument character-by-character, recognises `$` followed by an alphanumeric name or `?`, and substitutes the resolved value.

---

### Glob Expansion

Glob patterns (`*` and `?`) in command arguments are expanded against the VFS before the command receives them:

1. **Detection:** Each argument is checked for `*` or `?` characters. Single-quoted tokens (marked with a `\0` prefix) skip expansion entirely.
2. **Matching:** For patterns without a path separator (e.g., `*.txt`), only immediate children of the current directory are matched. For patterns with path separators (e.g., `/home/*.txt`), `VirtualFileSystem.findByName()` searches the specified subtree.
3. **Fallback:** If no VFS paths match a glob pattern, the literal pattern is passed through unchanged (standard shell behaviour).

Matching uses `VirtualFileSystem.matchesWildcard()`, which converts `*` → `.*` and `?` → `.` to build a regex.

---

### Command Suggestion ("Did you mean?")

When a command name is not found in the registry, `ShellSession.suggestCommand()` computes the Levenshtein edit distance between the mistyped input and every registered command name using dynamic programming. If the closest match has an edit distance ≤ 2, a hint like `Did you mean 'ls'?` is displayed alongside the error.

---

### VFS Environment Persistence

Users can save and load VFS snapshots through the `save`, `load`, `reset`, `envlist`, and `envdelete` commands. The `VfsSerializer` handles the conversion.

**Save/Load flow:**

![Save Load Sequence Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/SaveLoadSequence.puml)

**`.env` file format:**

```text
# LinuxLingo Virtual File System Snapshot
# Saved: 2026-03-27T14:30:00
# Working Directory: /home/user
#
# Format: TYPE | PATH | PERMISSIONS | CONTENT

DIR  | /              | rwxr-xr-x
DIR  | /home          | rwxr-xr-x
FILE | /etc/hostname  | rw-r--r-- | linuxlingo
```

Content escaping rules: `\n` → newline, `\|` → literal pipe, `\\` → literal backslash.

---

### Exam Session Flow

The exam module supports three entry modes and three question types.

**Interactive exam flow:**

![Exam Session Sequence Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/ExamSessionSequence.puml)

**PRAC question handling:**

For practical questions, `ExamSession.handlePracQuestion()`:

1. Creates a fresh `VirtualFileSystem` via the `vfsFactory` supplier.
2. Creates a temporary `ShellSession` with this VFS.
3. Calls `tempSession.start()` — the user types shell commands.
4. When the user types `exit`, the temporary session ends.
5. Calls `PracQuestion.checkVfs(tempVfs)` which verifies each `Checkpoint` (expected path + node type, content, or permissions).

---

### Exam Component — Practical Questions

This section documents a planned enhancement to the Exam component’s practical (`PRAC`) questions: configurable VFS setup and richer checkpoint validation. The goal is to support more realistic, multi-step shell scenarios while keeping the public Exam API stable for the rest of the system.

The enhancement is implemented via two extensions:

1. `PracQuestion.SetupItem` — declarative VFS setup instructions applied before the user starts typing commands.
2. Extended `Checkpoint` semantics — new node checks (`NOT_EXISTS`, `CONTENT_EQUALS`, `PERM`) to validate more aspects of the final VFS state.

These changes are internal to the exam module and transparent to callers such as `ExamSession` and `QuestionBank`.

---

#### 1. Class-Level Design

At a high level, the flow for a `PRAC` question becomes:

1. `ExamSession` creates a fresh `VirtualFileSystem` via `vfsFactory`.
2. `ExamSession` asks the `PracQuestion` to apply any configured setup into that VFS.
3. `ExamSession` starts a temporary `ShellSession` over the prepared VFS and lets the user perform the task.
4. When the user exits the shell, `PracQuestion.checkVfs()` verifies all `Checkpoint`s against the final VFS state.

The enhancement is implemented by extending the existing `PracQuestion` and `Checkpoint` classes; no new top-level types are introduced.

**Key classes (Exam sub-package):**

- `PracQuestion` (in `linuxlingo.exam.question`)
  - Already represents a practical question, with:
    - `List<Checkpoint> checkpoints`
    - `List<SetupItem> setupItems`
  - Enhancement: implement `applySetup(VirtualFileSystem vfs)` using `setupItems`.

- `PracQuestion.SetupItem` (inner static class)
  - Already declared with:
    - `SetupType` enum: `MKDIR`, `FILE`, `PERM`
    - Fields: `type`, `path`, `value`
  - Enhancement: define concrete semantics for each `SetupType` and enforce them in `applySetup`.

- `Checkpoint` (in `linuxlingo.exam`)
  - Already exposes `matches(VirtualFileSystem vfs)` and `NodeType` enum.
  - Enhancement: implement additional `NodeType`s:
    - `NOT_EXISTS` — assert that a path is absent.
    - `CONTENT_EQUALS` — assert file content equality.
    - `PERM` — assert file/dir permissions.

- `ExamSession` (in `linuxlingo.exam`)
  - Already orchestrates `PRAC` questions via `handlePracQuestion(PracQuestion q)`.
  - Enhancement: call `q.applySetup(tempVfs)` before starting the `ShellSession`.

This keeps the public interface between modules unchanged: the CLI, Shell, and Storage components continue to treat the Exam module as a black box that exposes `ExamSession` and `QuestionBank` only.

---

#### 2. Class Diagram (PracQuestion V2)

![PracQuestion Class Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/PracQuestionClassDiagram.puml)

---

#### 3. Sequence Flow — PRAC Question with Setup

![PracQuestion Sequence Flow](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/PracQuestionSequence.puml)

---

#### 4. Activity Diagram — Applying Setup Items
![PracQuestion Setup Activity Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/PracQuestionSetupActivity.puml)

---

#### 5. Rationale and Alternatives

**Why introduce `SetupItem`?**

- Many realistic shell exercises require a non-trivial starting state (pre-existing directories, files, permissions).
- Encoding the initial environment directly in `VirtualFileSystem` from outside the exam module would tightly couple Exam to VFS internals.
- `SetupItem` provides a small, declarative DSL for “what the environment should look like before the user starts”, which is:
  - Easy to generate from question bank text via `QuestionParser`.
  - Easy to reason about and test at the `PracQuestion` level.

**Why extend `Checkpoint.NodeType`?**

- Existing `DIR` and `FILE` checks only validate presence and node kind.
- New types enable richer questions:
  - `NOT_EXISTS`: “Remove this file/directory.”
  - `CONTENT_EQUALS`: “Edit a file to contain specific content.”
  - `PERM`: “Set the permission of this file to 755.”
- This keeps the final-state validation declarative and human-readable in the question bank.

**Alternative 1: Global VFS Templates**

One alternative was to define named “VFS templates” elsewhere (e.g., serialized trees stored by the Storage component), and let `PracQuestion` refer to them by ID.

- **Pros:**
  - Reuse complex environments across multiple questions.
  - Potentially faster to load via `VfsSerializer`.
- **Cons:**
  - Introduces a tight coupling between Exam and Storage/VFS serialization.
  - Makes individual questions harder to understand without looking up the template definition.
- **Reason rejected:** For this project scale, per-question declarative setup keeps the exam bank self-contained and easier for new contributors to maintain.

**Alternative 2: Hard-coded Setup per Question**

Another option was to provide a dedicated subclass or factory per practical question that mutates the VFS imperatively.

- **Pros:**
  - Maximum flexibility (any arbitrary VFS mutation is possible).
- **Cons:**
  - Scales poorly — new questions require new Java code.
  - Blurs separation between content (questions) and logic (exam engine).
- **Reason rejected:** Conflicts with the existing question-bank-driven design, where content lives in `.txt` files, not in code.

---

#### 6. Impact on Other Components

- **CLI Component**
  - No changes required. The `exam` command continues to delegate to `ExamSession`, which hides all internal PRAC details.

- **Shell Component**
  - No public API changes; `ShellSession` is still created with a `VirtualFileSystem` and `Ui`.
  - Only observable difference is that the initial VFS contents for a `PRAC` question may now be non-empty.

- **Storage Component**
  - `QuestionParser` will parse additional fields for `PRAC` question setup, constructing the appropriate `SetupItem` instances.
  - No changes are required in `Storage` itself.

- **Testing**
  - Unit tests can construct `PracQuestion` instances with explicit `setupItems` and `checkpoints`, then run:
    - `applySetup(vfs)`
    - mutate `vfs` as if a user had run commands
    - assert on `checkVfs(vfs)` results
  - This isolates exam logic from the interactive shell during automated testing.

---

#### 7. Extension Points

This design leaves room for future growth:

- **Additional `SetupType` values** (e.g., `COPY`, `MOVE`) can be added without modifying existing question banks.
- **Additional `NodeType` values** can be introduced to validate more sophisticated conditions (e.g., ownership, timestamps).
- A future version could allow **difficulty-based setup** (e.g., more complex initial states for `HARD` questions) by inspecting `difficulty` within `applySetup`.

---

### Question Parsing and Loading

Question bank files use a pipe-delimited format. `QuestionParser` processes each line into typed `Question` objects.
The question bank parsing feature is implemented by QuestionParser, which reads plain-text .txt files from the data/questions directory via Storage.readLines(Path) and converts each non-comment, non-blank line into a concrete Question object. 
Each line is pipe-delimited into up to six fields: TYPE | DIFFICULTY | QUESTION_TEXT | ANSWER | OPTIONS | EXPLANATION. 
QuestionParser normalises the type and difficulty (defaulting invalid difficulty values to MEDIUM), then dispatches to parseMcq, parseFitb, or parsePrac based on the TYPE field.
MCQ options are parsed into a LinkedHashMap<Character, String> to preserve display order, FITB answers are split on unescaped | (with \| treated as a literal pipe), and PRAC answers are currently parsed into simple Checkpoint objects, with a v2.0 hook for optional setup items. 
Malformed lines are skipped with a logged warning instead of failing the entire file, and an assertion ensures that the resulting question list contains no null entries. 
We chose a pipe-separated text format instead of JSON/YAML to keep the files compact, easy to edit, and diff-friendly for contributors. 
Alternatives such as embedding questions directly in Java code or using a more complex DSL were rejected because they would make non-developer contributions harder and tightly couple content with implementation; 
the current design keeps parsing logic centralized in QuestionParser and lets the rest of the exam module work purely with typed Question objects.
**Data flow:**

![Question Parsing Activity Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/QuestionParsingActivity.puml)

**Question bank line format:**

```text
TYPE | DIFFICULTY | QUESTION_TEXT | ANSWER | OPTIONS | EXPLANATION
```

- **MCQ** answer: single letter (e.g., `B`). Options: `A:text B:text C:text D:text`.
- **FITB** answer: accepted answers separated by `|` (e.g., `pwd|PWD`). Escaped pipes (`\|`) are treated as literal pipe characters.
- **PRAC** answer: checkpoints as `path:TYPE` pairs (e.g., `/home/project:DIR,/home/readme.txt:FILE`). Optional setup items in the OPTIONS field (semicolon-separated).

---

### Resource Extraction on First Run

`ResourceExtractor` ensures that bundled question bank files are available on disk.

![Resource Extraction Activity Diagram](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/AY2526S2-CS2113-T10-2/tp/master/docs/diagrams/ResourceExtractionActivity.puml)

This design respects user customizations — once the questions directory exists, bundled resources are never overwritten. The five bundled question files cover: `file-management`, `navigation`, `permissions`, `piping-redirection`, and `text-processing`.

---

## Appendix A: Product Scope

### Target User Profile

- Computer Science students learning Linux command-line basics.
- Prefer an interactive, hands-on approach over reading documentation.
- Comfortable typing commands in a terminal-like interface.
- Want a safe sandbox environment to practice Linux commands without affecting real systems.
- Desire immediate feedback on their Linux command knowledge through quizzes.

### Value Proposition

LinuxLingo provides an interactive Linux shell simulator combined with a quiz system, allowing students to:

- Practice Linux commands (navigation, file operations, text processing, permissions) in a safe virtual file system.
- Test their knowledge through multiple question types (MCQ, fill-in-the-blank, practical).
- Save and restore VFS environments for continued practice.
- Learn without needing access to a real Linux machine.

---

## Appendix B: User Stories

| Priority | As a … | I want to … | So that I can … |
| -------- | ------ | ----------- | --------------- |
| `***` | new user | see a help menu | learn what commands are available |
| `***` | student | practice basic navigation commands (cd, ls, pwd) | become familiar with Linux file system navigation |
| `***` | student | create and manipulate files and directories | learn file management in Linux |
| `***` | student | take an exam on a specific topic | test my knowledge of Linux commands |
| `***` | student | see my exam score after completing a quiz | know how well I understand the material |
| `**` | student | use piping to chain commands | understand how data flows between commands |
| `**` | student | use output redirection (>, >>) | learn how to save command output to files |
| `**` | student | use input redirection (<) | learn how to feed file content into commands |
| `**` | student | save my VFS environment | continue practicing from where I left off |
| `**` | student | load a previously saved environment | restore my practice workspace |
| `**` | student | practice practical questions in a real shell | apply my knowledge hands-on |
| `**` | student | use text processing commands (grep, sort, wc) | learn data manipulation on the command line |
| `**` | student | change file permissions with chmod | understand Linux permission model |
| `**` | student | define command aliases | create shortcuts for frequently used commands |
| `**` | student | view my command history | recall and re-use previous commands |
| `**` | student | use glob patterns (*.txt) | match multiple files at once |
| `**` | student | use conditional execution (&&, \|\|) | understand command chaining logic |
| `**` | student | use shell variables ($USER, $HOME, $PWD) | understand how variables work in a shell |
| `*` | student | take a random question | get a quick knowledge check |
| `*` | student | list and delete saved environments | manage my saved workspaces |
| `*` | student | read manual pages with `man` | learn about a command's usage |
| `*` | student | see a directory tree with `tree` | visualize the file system structure |
| `*` | student | get "Did you mean?" suggestions on typos | quickly correct mistyped commands |
| `*` | student | use tab-completion in the shell | type commands more efficiently |
| `*` | student | compare files with `diff` | learn to find differences between files |
| `*` | student | use `tee` to save and display output | understand pipeline data capture |

---

## Appendix C: Non-Functional Requirements

1. **Portability:** Should work on any mainstream OS (Windows, Linux, macOS) with Java 17 or above installed.
2. **Performance:** All shell commands should execute in under 100ms. VFS operations should handle file systems with up to 1000 nodes without noticeable lag.
3. **Usability:** A user familiar with basic Linux commands should be able to use the shell simulator without consulting documentation.
4. **Reliability:** The application should handle all invalid inputs gracefully (no crashes) and provide descriptive error messages.
5. **Testability:** All components should be unit-testable in isolation. The `Ui` class accepts injectable I/O streams for test harness use.
6. **Data Integrity:** VFS snapshots saved to disk must be losslessly restorable. Escaping rules must preserve file content containing newlines, pipes, and backslashes.
7. **Single-user:** The application is designed for single-user use and does not need to handle concurrent access.

---

## Appendix D: Glossary

| Term | Definition |
| ---- | ---------- |
| **VFS** | Virtual File System — an in-memory tree structure simulating a Linux file system. No real files on disk are created or modified by shell commands. |
| **Shell Session** | An interactive REPL where users type Linux-like commands that operate on the VFS. |
| **Exam Session** | A quiz session where users answer questions about Linux commands. |
| **MCQ** | Multiple Choice Question — presents options A/B/C/D; user selects one. |
| **FITB** | Fill In The Blank — user types a free-form answer checked against accepted answers. |
| **PRAC** | Practical question — user performs tasks in a temporary shell; VFS state is verified against checkpoints. |
| **Checkpoint** | An expected condition on a VFS path (existence, type, content, or permissions) used to verify PRAC question answers. |
| **SetupItem** | A declarative VFS initialization instruction (create directory, create file, set permissions) applied before a PRAC question begins. |
| **Segment** | A single command with its arguments and optional redirect info, part of a `ParsedPlan`. |
| **ParsedPlan** | The structured result of parsing a shell input: a list of Segments connected by operators. |
| **Environment (.env)** | A text file storing a serialized VFS snapshot and working directory, saved under `data/environments/`. |
| **Piping** | Connecting the stdout of one command to the stdin of the next using the pipe character (&#124;). |
| **Redirection** | Directing command output to a file (`>`/`>>`) or reading input from a file (`<`). |
| **Glob** | A wildcard pattern (`*`, `?`) used to match multiple file names in the VFS. |
| **Alias** | A user-defined shortcut for a command name, stored in the shell session. |
| **Question Bank** | A collection of question files (`.txt`) organized by topic under `data/questions/`. |
| **Mainstream OS** | Windows, Linux, macOS. |

---

## Appendix E: Instructions for Manual Testing

> **Note:** These instructions provide a starting point for testers. Testers are expected to do more exploratory testing.

### Launch and Shutdown

1. **Initial launch**
   - Ensure Java 17+ is installed.
   - Build the project: `./gradlew shadowJar`
   - Run: `java -jar build/libs/tp.jar`
   - Expected: Welcome banner and `linuxlingo>` prompt are displayed.

2. **Help command**
   - Input: `help`
   - Expected: List of available top-level commands (shell, exam, exec, help, exit) is displayed.

3. **Exit**
   - Input: `exit` (or `quit`)
   - Expected: Prints "Goodbye!" and application terminates.

### Shell Simulator

1. **Entering the shell**
   - Input: `shell`
   - Expected: Welcome message and shell prompt `user@linuxlingo:/$` are displayed.

2. **Basic navigation**
   - `pwd` → `/`
   - `cd /home/user` → prompt changes to `user@linuxlingo:/home/user$`
   - `cd -` → returns to `/`, prints `/`
   - `cd ~` → navigates to `/home/user`
   - `ls -la` → lists files with permissions, types, and sizes

3. **File and directory operations**
   - `mkdir testdir` → no output (success)
   - `mkdir -p a/b/c` → creates nested directories
   - `ls` → `testdir` appears in listing
   - `touch testdir/hello.txt` → no output (success)
   - `echo "Hello World" > testdir/hello.txt` then `cat testdir/hello.txt` → `Hello World`
   - `cp testdir/hello.txt testdir/copy.txt` → file copied
   - `mv testdir/copy.txt testdir/moved.txt` → file renamed
   - `rm -r testdir` → directory and contents removed

4. **Piping**
   - `echo "line1" | cat` → `line1`
   - `echo "apple" | grep apple` → `apple`

5. **Output redirection**
   - `echo "test output" > /tmp/out.txt` then `cat /tmp/out.txt` → `test output`
   - `echo "more" >> /tmp/out.txt` then `cat /tmp/out.txt` → `test output` followed by `more`

6. **Input redirection**
   - `echo "hello" > /tmp/in.txt` then `cat < /tmp/in.txt` → `hello`
   - `grep hello < /tmp/in.txt` → `hello`

7. **Conditional execution**
   - `echo success && echo "also runs"` → both `success` and `also runs` printed
   - `ls /nonexistent && echo "no"` → error message only, `no` is NOT printed
   - `ls /nonexistent || echo "fallback"` → error message, then `fallback` printed
   - `echo a ; echo b` → both `a` and `b` printed on separate lines

8. **Text processing**
   - `echo "hello world" > /tmp/test.txt` then `grep hello /tmp/test.txt` → `hello world`
   - `wc /tmp/test.txt` → line, word, and character counts
   - `sort` / `uniq` / `head` / `tail` — verify expected output on multi-line input

9. **Permissions**
   - `touch /tmp/secret.txt` then `chmod 000 /tmp/secret.txt` then `cat /tmp/secret.txt` → `Permission denied` error

10. **Glob patterns**
    - `touch a.txt b.txt c.log` then `ls *.txt` → lists `a.txt b.txt` only
    - `ls ?.txt` → lists `a.txt b.txt` (single-character match)

11. **Shell variables**
    - `echo $USER` → `user`
    - `echo $HOME` → `/home/user`
    - `echo $PWD` → current working directory

12. **Aliases**
    - `alias ll="ls -la"` then `ll` → same output as `ls -la`
    - `unalias ll` then `ll` → `ll: command not found`

13. **Command history**
    - Run a few commands, then `history` → numbered list of previous commands
    - `!!` → re-runs the last command
    - `!3` → re-runs command number 3

14. **Info commands**
    - `man ls` → displays usage info for `ls`
    - `tree` → displays directory tree from current directory

15. **Diff and tee**
    - `echo "a" > /tmp/f1.txt` and `echo "b" > /tmp/f2.txt` then `diff /tmp/f1.txt /tmp/f2.txt` → shows differences
    - `echo "hello" | tee /tmp/tee.txt` → prints `hello` AND writes it to `/tmp/tee.txt`

16. **Command suggestion**
    - `mkdi` → `Did you mean: mkdir?`
    - `gre` → `Did you mean: grep?`

17. **Exiting the shell**
    - `exit` → returns to `linuxlingo>` prompt

### Environment Management

1. **Save environment**
   - Enter shell, create some files/directories.
   - `save myenv` → environment saved message
   - `envlist` → `myenv` appears in the list

2. **Load environment**
   - `reset` → VFS reset to default state
   - `load myenv` → previously created files/directories are restored

3. **Delete environment**
   - `envdelete myenv` → environment deleted message
   - `envlist` → `myenv` no longer appears

### Exam Module

1. **Interactive exam**
   - Input (at main prompt): `exam`
   - Expected: List of topics is displayed with question counts.
   - Select a topic by number.
   - Enter number of questions (or press Enter for all).
   - Expected: Questions are presented one at a time with feedback after each.
   - Expected: Final score summary (e.g., `Score: 7/10 (70%)`).

2. **Direct exam with CLI args**
   - Input: `exam -t navigation -n 3`
   - Expected: 3 questions from the "navigation" topic.

3. **Random question**
   - Input: `exam -random`
   - Expected: One random question is presented.

4. **List topics**
   - Input: `exam -topics`
   - Expected: All available topics listed with question counts.

5. **PRAC question** (if available in question bank)
   - When a PRAC question appears, a temporary shell session opens.
   - Perform the required task (e.g., `mkdir /home/project`).
   - Type `exit` to submit.
   - Expected: Feedback on whether the VFS matches the expected state.

### One-Shot Execution

1. **Basic exec**
   - Input (at main prompt): `exec "echo hello"`
   - Expected: `hello` is printed.

2. **Exec with saved environment**
   - First save an environment in the shell (e.g., `save testenv`).
   - Input: `exec -e testenv "ls"`
   - Expected: Directory listing from the saved environment.

### Error Handling

1. **Unknown command in shell**
   - Input: `unknowncmd`
   - Expected: `unknowncmd: command not found`

2. **Invalid path**
   - Input: `cd /nonexistent/path`
   - Expected: `cd: No such file or directory: /nonexistent/path`

3. **Missing operand**
   - Input: `grep`
   - Expected: `grep: missing pattern`

4. **Invalid exam topic**
   - Input: `exam -t nonexistent`
   - Expected: `Invalid topic selection.` followed by available topics list.
