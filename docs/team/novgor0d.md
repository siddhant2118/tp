# Vihaan - Project Portfolio Page

## Overview

LinuxLingo is a command-line application for learning Linux commands through an interactive shell simulator and a built-in quiz system. It is built for Computer Science students who want to build confidence with the Linux command line within a safe, in-memory virtual file system that never touches real files.

My contributions centred on the core shell infrastructure: the input parsing pipeline, the command execution engine, and the alias and history management system that together make the simulator feel like a real shell.

---

## Summary of Contributions

**Code contributed:** [RepoSense Dashboard](https://nus-cs2113-ay2526-s2.github.io/tp-dashboard/?search=Novgor0d&breakdown=true)

---

### Features Implemented

**Shell Parser (`ShellParser`)**

Implemented the full parsing pipeline that transforms raw user input into structured execution plans. This was a non-trivial component requiring careful design across three layers:

- *Tokenizer* — a character-by-character state machine handling whitespace, single quotes, double quotes, and backslash escaping. Correctly recognises all shell operators (`|`, `||`, `>`, `>>`, `<`, `&&`, `;`).
- *Segment builder* — splits the token stream into command segments, each carrying a command name, arguments, an optional output redirect, and an optional input redirect.
- *Execution plan constructor* — composes segments into a structured `ParsedPlan` consumed by the execution engine, enforcing the invariant that `operators.size()` is always exactly `segments.size() - 1`.

The implementation required resolving non-trivial edge cases such as operator lookahead for `|` vs `||` and `>` vs `>>`, correct handling of quoted whitespace, and graceful error reporting for empty or malformed input. A dedicated `flushCurrentToken()` helper was extracted to eliminate repeated token-emission logic across tokenizer states.

**Alias Management (`alias`, `unalias`)**

- `alias` supports listing all current aliases, defining new ones (`alias NAME=VALUE`), and inspecting a specific alias by name.
- `unalias` supports removing one or more aliases by name, or clearing all with `-a`. Implements partial-success semantics: valid names are removed and errors are reported only for names not found, so a single bad argument does not abort the whole command.
- Input validation is strict: extra arguments after a valid alias definition are rejected with a clear error message, preventing silent misconfiguration.
- Alias resolution is integrated directly into the `ShellSession` execution engine, with circular alias detection to prevent infinite loops.

**Command History (`history`)**

- Records every command entered in the current session, including commands that fail, providing an accurate audit trail.
- `history N` shows the last N entries; `history -c` clears the log.
- The `history` command itself is recorded in the log (a subtle correctness point that required an explicit fix).

**Shell Session Enhancements**

- Implemented the core REPL loop in `ShellSession`, including blank-line skipping, `exit` handling, and structured command dispatch.
- Added `||` (OR) operator support for command chaining in the execution engine.
- Added input redirection (`<`) support in the execution engine.
- Fixed a bug where empty argument lists were incorrectly passed to `cd` and `ls`, causing unexpected behaviour.
- Improved `HelpCommand` output format for better readability.

---

### Contributions to the User Guide

- Wrote the **Alias Command** section, documenting all three modes (`alias`, `alias NAME`, `alias NAME=VALUE`), alias chaining behaviour, and the full `unalias` usage.
- Wrote the **History Command** section, covering `history`, `history N`, and `history -c`, with a note that failed commands and the `history` call itself are both recorded.
- Added screenshots for all three commands.


### Contributions to the Developer Guide

- Wrote the **Setting Up / Getting Started** section for new contributors.
- Wrote the **Shell Parsing and Execution** implementation section, covering the tokenizer's state machine (including the `NORMAL` / `IN_SINGLE_QUOTE` / `IN_DOUBLE_QUOTE` states), the lookahead behaviour for `||` vs `|` and `>>` vs `>`, and the `Expecting` flag mechanism for redirect targets.
- Wrote the **Command Execution with Piping and Redirection** section, explaining how `runPlan()` chains segments using `pipedStdin` and `lastExitCode`, and includes a worked walkthrough of `echo hello | grep h > output.txt` through all three stages.
- Updated the `ShellSession` documentation to reflect the modified execution model.
- Added the following UML diagrams: `ParsingPipelineActivity.puml`, `ShellParserSequence.puml`, `ParsedPlanClassDiagram.puml`, `RunPlanSequence.puml`.

---

### Contributions to Team-Based Tasks

- Code Reviews: [#89](https://github.com/AY2526S2-CS2113-T10-2/tp/pull/89), [#90](https://github.com/AY2526S2-CS2113-T10-2/tp/pull/90), [#126](https://github.com/AY2526S2-CS2113-T10-2/tp/pull/126), [#154](https://github.com/AY2526S2-CS2113-T10-2/tp/pull/154)
- Managed issues and milestone tracking for the shell module.
- Modified `gradle.properties` to suppress noisy execution progress output, improving the build experience for all team members.

- Manually tested features 1–5 of the User Guide (written by teammates) to identify inaccuracies and bugs before the final release.
---

### Review / Mentoring Contributions

- Wrote tests covering `ShellParser`, `AliasCommand`, `UnaliasCommand`, and `HistoryCommand`, expanding coverage of the shell subsystem substantially across multiple PRs.
- Added the initial test suite for `ShellParser.parse()`, covering single commands, multi-argument commands, pipe and AND operators, and null/empty input edge cases.

---

### Contributions Beyond the Project Team

- Reported 14 bugs in another team's product during the PE Dry Run: [PE-D Bug Reports](http://github.com/NUS-CS2113-AY2526-S2/ped-Novgor0d).

---

**Key design decisions worth noting for evaluators:** The `ShellParser` is the architectural centrepiece of the shell subsystem — every command the user types passes through it before anything else runs. Getting quoting, escaping, and operator disambiguation right required iterative refinement across many commits and is the kind of low-level infrastructure that is invisible when it works but breaks everything when it doesn't. The alias and history features, while more straightforward in isolation, required tight integration with the execution engine and careful attention to correctness in edge cases (circular resolution, partial-success semantics, self-recording of `history`).

---

Changes from the previous version:

- **DG diagrams**: listed all four UML diagrams by name so evaluators can see the scope of diagram work directly.
- **UG section**: added the `alias NAME=VALUE` format detail and the alias chaining note, which are distinctive enough to mention; also added the manual testing of teammates' features 1–5.
- **Team tasks**: added issue/milestone management of the shell module and the cross-module PR reviews (exam module), since those show breadth beyond just your own features.
- **Kept it tight**: didn't repeat the commit list or pad with low-value items like single-line fixes that are already visible on RepoSense.