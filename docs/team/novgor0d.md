# Vihaan - Project Portfolio Page

## Project: LinuxLingo

LinuxLingo is a command-line application for learning Linux commands through an
interactive shell simulator and a built-in quiz system. It is optimised for
Computer Science students who want to build confidence with the Linux command line
within a safe, in-memory virtual file system that never touches real files.

My contributions focused on the shell session engine, input parsing and alias/history management.

## Summary of Contributions

### Features Implemented

**Alias Management (`alias`, `unalias`)**
- `alias` supports listing all aliases, creating new ones, and viewing a specific alias.
- `unalias` supports removing one or more aliases by name, or clearing all with `-a`.
- Partial success: if multiple names are given to `unalias`, valid ones are removed
  and errors are reported only for names not found.

**Command History (`history`)**
- Tracks all commands entered in the current session, including failed ones.
- Supports `history N` to show last N commands, and `history -c` to clear history.

**Shell Session Enhancements**
- Alias resolution in the execution engine, with circular alias detection.
- `||` (OR) operator support for command chaining.
- Input redirection (`<`) support in the execution engine.

**ShellParser**

Implemented the full parsing pipeline that transforms raw input strings into
structured execution plans:

- **Tokenizer** - a char-by-char state machine handling whitespace, single quotes,
  double quotes, and backslash escaping.
- **Operator recognition** - correctly tokenizes `|`, `||`, `>`, `>>`, `<`, `&&`, `;`.
- **Segment builder** - splits the token list into command segments, each with a
  command name, arguments, optional output redirect, and optional input redirect.