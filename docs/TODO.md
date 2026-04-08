# TODO — Bugs to Fix

Bugs identified from [Michael's Test Report](test_reports/Michael_TestReport.md). Issues created in [upstream](https://github.com/AY2526S2-CS2113-T10-2/tp/issues).

## Critical

- [ ] **#133 — Input redirection (`<`) from non-existent file causes VfsException crash**
  - File: `ShellSession.runPlan()` — wrap `vfs.readFile()` for input redirect in try-catch
  - Test: `cat < nonexistent_file.txt`

- [ ] **#134 — Output redirection (`>>`) to read-only file causes VfsException crash**
  - File: `ShellSession.runPlan()` — wrap `vfs.writeFile()` for output redirect in try-catch
  - Test: `chmod 444 file && echo "x" >> file`

## High

- [ ] **#135 — `&&` and `||` use `break` instead of `continue`, killing subsequent `;` segments**
  - File: `ShellSession.runPlan()` — change `break` to `continue` in `&&`/`||` handlers
  - Test: `false && echo skip ; echo "should run"`
  - Test: `echo ok || echo skip ; echo "should run"`

- [ ] **#136 — Intermediate segment stdout silently discarded in exec mode**
  - File: `ShellSession.runPlan()` / `LinuxLingo.handleExec()` — accumulate stdout across segments
  - Test: `exec "echo a && echo b && echo c"` → should print all three

## Medium

- [ ] **#137 — All error messages printed twice in exec mode**
  - File: `ShellSession.runPlan()` + `LinuxLingo.handleExec()` — remove duplicate stderr printing
  - Test: `exec "cat no_such_file"` → error should appear once

- [ ] **#138 — Moving a directory into itself silently orphans it from VFS tree**
  - File: `VirtualFileSystem.move()` — add self-move detection before performing move
  - Test: `mkdir /tmp/selfmove && mv /tmp/selfmove /tmp/selfmove/inside`

- [ ] **#139 — Pipe with no RHS and redirect with no filename are silently ignored**
  - File: `ShellParser.parse()` — detect dangling operators/redirect targets after token loop
  - Test: `echo hello |` and `echo hello >`

## Low

- [ ] **#140 — `exec -e myenv` without a command treats `-e` as the command**
  - File: `LinuxLingo.handleExec()` — add bounds check for `args.length == 3` with `-e` flag
  - Test: `java -jar LinuxLingo.jar exec -e myenv`

## Accepted Simplifications (Won't Fix)

These were flagged in testing but are **acceptable simplifications** for an educational shell simulator:

| Bug # | Description | Reason |
|-------|-------------|--------|
| 8 | Deleting CWD leaves a dangling working directory reference | Even real Linux allows `pwd` in a deleted directory. Low impact, no crash. |
| 11 | Aliases set in a command line don't persist to subsequent segments | Alias resolution is a known limitation of the simplified single-pass parser. Acceptable for an educational tool. |
| 12 | Empty quoted arguments (`""`, `''`) are silently dropped | Minor tokenizer limitation. Not critical for the tool's educational purpose. |
| 13 | Unterminated quotes are silently closed at end of input | Common simplification in educational shells. Real shells prompt for continuation, which is out of scope. |
