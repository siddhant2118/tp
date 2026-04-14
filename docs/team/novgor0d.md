# Vihaan - Project Portfolio Page

## Overview

LinuxLingo is a command-line application for learning Linux commands through an interactive shell simulator and a built-in quiz system. It is built for Computer Science students who want to build confidence with the Linux command line within a safe, in-memory virtual file system that never touches real files.
My contributions centred on the core shell infrastructure: the input parsing pipeline, the command execution engine, and the alias and history management system that together make the simulator feel like a real shell.

---

## Summary of Contributions

**Code contributed:** [RepoSense Dashboard](https://nus-cs2113-ay2526-s2.github.io/tp-dashboard/?search=Novgor0d&breakdown=true)

---

### Features Implemented

- **Shell Parser (`ShellParser`):** Implemented the full parsing pipeline that transforms raw user input into structured execution plans across three layers: a character-by-character tokenizer (handling quotes, backslash escaping, and all shell operators `|`, `||`, `>`, `>>`, `<`, `&&`, `;`); a segment builder that groups tokens into command segments with redirect info; and an execution plan constructor producing a `ParsedPlan` that enforces the invariant `operators.size() == segments.size() - 1`.
- **Alias Management (`alias`, `unalias`):** `alias` supports listing all aliases, defining new ones (`alias NAME=VALUE` with surrounding quotes stripped), and inspecting a specific alias. `unalias` removes aliases by name or clears all with `-a` (rejected if combined with names). Alias resolution is integrated into the execution engine with circular alias detection.
- **Command History (`history`):** Records all commands entered in the session, including failed ones. Supports `history N` (last N entries, parsed as `long` with bounds checking to avoid integer overflow) and `history -c` to clear.
- **Shell Session Enhancements:** Implemented the core REPL loop in `ShellSession` (blank-line skipping, `exit` handling, command dispatch); added `||` operator support and input redirection (`<`) to the execution engine; fixed a bug where empty argument lists were passed to `cd` and `ls`; improved `HelpCommand` output formatting.
- **Shell Utility Classes:** `Preconditions` (`requireNonNull`, `requireNonBlank`) eliminates repeated null checks across `ShellParser`, `Token`, `Segment`, and `ParsedPlan`. `ExitCodes` centralises exit codes (`1`, `2`, `127`), preventing magic numbers from being scattered across the codebase.

**Testing and Code Quality**

- Wrote tests for `ShellSession`, `ShellParser`, `AliasCommand`, `UnaliasCommand`, and `HistoryCommand`, covering edge cases including null/empty input, pipe and AND operators, partial-success unalias, and self-recording of `history`.
- Refactored code across the shell package applying DRY and SLAP, including extracting `flushCurrentToken()`, `buildSegment()`, `shouldSkipSegment()`, and `precedingOperatorIsNotPipe()`.
- Wrote Javadoc for classes and methods in `linuxlingo.shell`, including methods written by teammates within the same package.

**Contributions to the User Guide**

- Wrote the **Alias Command** section (all three modes, alias chaining, full `unalias` usage) and the **History Command** section (all flags, note on failed commands and self-recording). Added screenshots for all three commands.
- Manually tested UG features 1–5 (written by teammates) to identify inaccuracies before the final release.

**Contributions to the Developer Guide**

- Wrote the **Setting Up / Getting Started** section, the **Shell Parsing and Execution** implementation section (tokenizer states, lookahead behaviour, redirect-expect mechanism), and the **Command Execution with Piping and Redirection** section (including a worked walkthrough of `echo hello | grep h > output.txt`).
- Updated `ShellSession` documentation and added 4 UML diagrams: `ParsingPipelineActivity.puml` (activity), `ShellParserSequence.puml` (sequence), `ParsedPlanClassDiagram.puml` (class), `RunPlanSequence.puml` (sequence).

**Contributions to Team-Based Tasks**

- Code reviews: [#89](https://github.com/AY2526S2-CS2113-T10-2/tp/pull/89), [#90](https://github.com/AY2526S2-CS2113-T10-2/tp/pull/90), [#126](https://github.com/AY2526S2-CS2113-T10-2/tp/pull/126), [#154](https://github.com/AY2526S2-CS2113-T10-2/tp/pull/154).
- Managed issues and milestone tracking for the shell module and v2.1 milestone.
- Modified `gradle.properties` to suppress noisy execution progress output.

**Contributions Beyond the Project Team**

- Reported 14 bugs in another team's product during the PE Dry Run: [PE-D Bug Reports](http://github.com/NUS-CS2113-AY2526-S2/ped-Novgor0d).