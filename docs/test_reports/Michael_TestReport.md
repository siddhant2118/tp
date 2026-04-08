# Test Report — Michael (Member 5: Infrastructure & Shell Session Stress Testing)

**Scope:** Virtual shell sessions (interactive & non-interactive), VFS internals, CLI REPL, Storage layer, CommandRegistry, CommandResult.

**Methodology:** Black-box stress testing from a user-facing perspective, targeting edge cases in path resolution, parsing, piping/redirection, command chaining, permissions, environment management, quoting/escaping, and error handling. All tests were executed via `java -jar build/libs/LinuxLingo.jar exec "<command>"` (one-shot mode) unless otherwise noted.

---

## Test Category 1: Path Resolution Edge Cases

### Test 1.1: Multiple consecutive slashes
**Command:** `cd ////home////user && pwd`
**Expected:** Should resolve to `/home/user` (extra slashes ignored).
**Result:** ✅ PASS

**Actual Output:**
```
/home/user
```

### Test 1.2: Tilde with trailing characters (`~foo`)
**Command:** `cd ~foo`
**Expected:** Error or treat as literal directory name. In bash, `~foo` refers to user `foo`'s home.
**Result:** ✅ PASS (treated as literal directory name, not found → error)

**Actual Output:**
```
cd: No such file or directory: ~foo
```

### Test 1.3: Excessive parent traversal (`/../../../..`)
**Command:** `cd /../../../.. && pwd`
**Expected:** Should stop at root `/`.
**Result:** ✅ PASS

**Actual Output:**
```
/
```

### Test 1.4: Empty path
**Command:** `cd "" && pwd`
**Expected:** Should go to home directory or produce an error.
**Result:** ✅ PASS (goes to home)

**Actual Output:**
```
/home/user
```

### Test 1.5: Path with spaces and special characters
**Command:** `mkdir "my dir" && cd "my dir" && pwd`
**Expected:** Should correctly create and navigate to a directory with spaces.
**Result:** ✅ PASS

**Actual Output:**
```
/my dir
```

### Test 1.6: `cd -` without previous directory
**Command:** `cd -` (fresh session)
**Expected:** Should produce an error or go to home.
**Result:** ✅ PASS

**Actual Output:**
```
cd: OLDPWD not set
```

### Test 1.7: `cd` to a file (not a directory)
**Command:** `touch /tmp/file.txt && cd /tmp/file.txt`
**Expected:** Error: not a directory.
**Result:** ✅ PASS

**Actual Output:**
```
cd: not a directory: /tmp/file.txt
```

### Test 1.8: Resolve path with `.` components
**Command:** `cd /./home/./user/. && pwd`
**Expected:** Should resolve to `/home/user`.
**Result:** ✅ PASS

**Actual Output:**
```
/home/user
```

---

## Test Category 2: Quoting, Escaping & Tokenization

### Test 2.1: Unterminated double quote
**Command:** `echo "hello world` (missing closing quote)
**Expected:** Error about unterminated quote, or at minimum not crash.
**Result:** ⚠️ MINOR — No error, silently treats as closed. Prints `hello world`.

**Actual Output:**
```
hello world
```

### Test 2.2: Unterminated single quote
**Command:** `echo 'hello world` (missing closing quote)
**Expected:** Error about unterminated quote, or at minimum not crash.
**Result:** ⚠️ MINOR — No error, silently treats as closed. Prints `hello world`.

**Actual Output:**
```
hello world
```

### Test 2.3: Empty quotes as argument
**Command:** `echo "" ""`
**Expected:** Should output a space (two empty args separated by space). In bash, `echo "" ""` outputs ` ` (space).
**Result:** ⚠️ BUG — Empty quotes are silently dropped, producing just a bare newline.

**Actual Output:**
```
(empty line)
```

### Test 2.4: Backslash at end of line
**Command:** `echo hello\`
**Expected:** Should print `hello` (trailing backslash escapes nothing) or `hello\`.
**Result:** ✅ PASS (trailing backslash treated as literal)

**Actual Output:**
```
hello\
```

### Test 2.5: Nested quotes
**Command:** `echo "it's a 'test'"`
**Expected:** Should print `it's a 'test'`.
**Result:** ✅ PASS

**Actual Output:**
```
it's a 'test'
```

### Test 2.6: Backslash inside double quotes
**Command:** `echo "hello\"world"`
**Expected:** In bash, prints `hello"world`. LinuxLingo doesn't support backslash escape inside double quotes.
**Result:** ⚠️ MINOR — Prints `hello\world` (backslash kept literally, quote ends at `\"`).

**Actual Output:**
```
hello\world
```

### Test 2.7: Single-quoted pipe operator
**Command:** `echo 'hello | world'`
**Expected:** Should print `hello | world` literally, NOT pipe.
**Result:** ✅ PASS

**Actual Output:**
```
hello | world
```

---

## Test Category 3: Piping & Redirection Edge Cases

### Test 3.1: Pipe with no right-hand command
**Command:** `echo hello |`
**Expected:** Error about missing command.
**Result:** 🐛 BUG — No error; silently prints `hello` as if pipe doesn't exist.

**Actual Output:**
```
hello
```

### Test 3.2: Pipe with no left-hand command
**Command:** `| cat`
**Expected:** Error about missing command.
**Result:** ⚠️ MINOR — No "missing command" error; `cat` runs with no stdin.

**Actual Output:**
```
cat: reading from stdin is not supported in LinuxLingo. Provide a filename or use piping.
```

### Test 3.3: Output redirection with no filename
**Command:** `echo hello >`
**Expected:** Error about missing filename.
**Result:** 🐛 BUG — No error; redirect silently ignored, `hello` printed to terminal.

**Actual Output:**
```
hello
```

### Test 3.4: Input redirection from non-existent file
**Command:** `cat < nonexistent_file.txt`
**Expected:** Error: file not found (graceful error message).
**Result:** 🐛 **CRITICAL BUG** — Uncaught `VfsException` crashes the application with a full stack trace.

**Actual Output:**
```
Exception in thread "main" linuxlingo.shell.vfs.VfsException: No such file or directory: nonexistent_file.txt
        at linuxlingo.shell.vfs.VirtualFileSystem.resolve(VirtualFileSystem.java:72)
        at linuxlingo.shell.vfs.VirtualFileSystem.readFile(VirtualFileSystem.java:359)
        at linuxlingo.shell.ShellSession.runPlan(ShellSession.java:305)
        at linuxlingo.shell.ShellSession.executePlanSilent(ShellSession.java:225)
        at linuxlingo.shell.ShellSession.executeOnce(ShellSession.java:190)
        at linuxlingo.LinuxLingo.handleExec(LinuxLingo.java:120)
        at linuxlingo.LinuxLingo.handleOneShot(LinuxLingo.java:79)
        at linuxlingo.LinuxLingo.main(LinuxLingo.java:63)
```

### Test 3.5: Double redirection
**Command:** `echo hello > file1.txt > file2.txt`
**Expected:** In bash, `file1.txt` is created empty and `file2.txt` gets content. Last redirection wins.
**Result:** ⚠️ MINOR — Only `file2.txt` is created with content. `file1.txt` is not created at all (parser only keeps last redirect target).

**Actual Output:**
```
(cat file1.txt → "No such file or directory")
(cat file2.txt → "hello")
```

### Test 3.6: Append to non-existent file
**Command:** `echo hello >> newfile.txt && cat newfile.txt`
**Expected:** Should create the file and write `hello`.
**Result:** ✅ PASS

**Actual Output:**
```
hello
```

### Test 3.7: Long pipe chain
**Command:** `echo "a b c d" | cat | cat | cat | cat | cat | cat | cat | cat | cat | cat`
**Expected:** Should print `a b c d` after passing through 10 cats.
**Result:** ✅ PASS

**Actual Output:**
```
a b c d
```

### Test 3.8: Redirect and pipe combined
**Command:** `echo hello | tee output.txt | wc -w`
**Expected:** Should print `1` and write `hello` to output.txt.
**Result:** ✅ PASS

**Actual Output:**
```
1
```

### Test 3.9: Input redirection with pipe
**Command:** `echo "line1" > input.txt && cat < input.txt | wc -l`
**Expected:** Should print `1`.
**Result:** ⚠️ POSSIBLE BUG — Prints `2` instead of `1`. This is likely a `wc` implementation issue (counts trailing newline as extra line) rather than an infrastructure bug.

**Actual Output:**
```
2
```

---

## Test Category 4: Command Chaining (&&, ||, ;)

### Test 4.1: && with failing command
**Command:** `cat nonexistent.txt && echo "should not print"`
**Expected:** Second command should NOT execute.
**Result:** ✅ PASS

**Actual Output:**
```
cat: No such file or directory: nonexistent.txt
```

### Test 4.2: || with failing command
**Command:** `cat nonexistent.txt || echo "fallback"`
**Expected:** Second command SHOULD execute and print "fallback".
**Result:** ✅ PASS

**Actual Output:**
```
cat: No such file or directory: nonexistent.txt
fallback
```

### Test 4.3: Semicolon with failing command
**Command:** `cat nonexistent.txt ; echo "always runs"`
**Expected:** Second command should execute regardless.
**Result:** ✅ PASS

**Actual Output:**
```
cat: No such file or directory: nonexistent.txt
always runs
```

### Test 4.4: Mixed chaining — `&&` then `;` (critical bash semantics test)
**Command:** `cat nonexistent.txt && echo "skipped" ; echo "should still run"`
**Expected (bash):** `echo "should still run"` SHOULD execute because `;` is unconditional.
**Result:** 🐛 **BUG** — `echo "should still run"` is NOT executed. The `&&` operator uses `break` to terminate the entire for-loop, which also skips subsequent `;`-separated segments.

**Actual Output:**
```
cat: No such file or directory: nonexistent.txt
(no further output — "should still run" never executes)
```

**Root Cause:** In `ShellSession.runPlan()`, the `&&` handler does `break` instead of `continue`, killing the entire execution plan including subsequent `;` segments.

### Test 4.5: `||` then `;`
**Command:** `echo "ok" || echo "skip" ; echo "always"`
**Expected (bash):** `echo "skip"` is skipped (first succeeded), but `echo "always"` runs unconditionally.
**Result:** 🐛 **BUG** — Same root cause as 4.4. `echo "always"` is NOT executed. The `||` operator uses `break` when the previous command succeeded, terminating all subsequent segments.

**Actual Output:**
```
ok
(no further output — "always" never executes)
```

### Test 4.6: Triple `&&` chain
**Command:** `echo "a" && echo "b" && echo "c"`
**Expected:** All three should print: `a`, `b`, `c`.
**Result:** 🐛 **BUG** — Only `c` (the last segment) is printed. Intermediate segment stdout is silently discarded in one-shot mode.

**Actual Output:**
```
c
```

**Root Cause:** `handleExec()` in `LinuxLingo.java` only prints the final `CommandResult`'s stdout. Intermediate segment outputs (from `echo "a"` and `echo "b"`) are never printed because `runPlan()` only returns the last result, and in exec mode the intermediate segments' stdout isn't printed anywhere.

### Test 4.7: `&&` followed by `||`
**Command:** `echo "ok" && cat nonexistent.txt || echo "recovered"`
**Expected (bash):** Prints "ok", then error, then "recovered".
**Result:** ⚠️ PARTIAL — `echo "ok"`'s output is lost (same intermediate stdout issue), but the `||` fallback works correctly.

**Actual Output:**
```
cat: No such file or directory: nonexistent.txt
recovered
```

---

## Test Category 5: Glob Expansion Edge Cases

### Test 5.1: Glob with no matches
**Command:** `ls *.xyz`
**Expected:** Error or literal `*.xyz` (in bash, depends on `failglob`/`nullglob`).
**Result:** ✅ PASS (glob kept as literal when no matches → ls reports "no such file")

**Actual Output:**
```
ls: No such file or directory: *.xyz
```

### Test 5.2: Glob with special regex characters
**Command:** `touch "file[1].txt" && ls`
**Expected:** File with brackets in name should be created.
**Result:** ✅ PASS

**Actual Output:**
```
home/
tmp/
etc/
file[1].txt
```

### Test 5.3: Glob in quoted string (should NOT expand)
**Command:** `echo "*.txt"`
**Expected:** Should print `*.txt` literally.
**Result:** ✅ PASS

**Actual Output:**
```
*.txt
```

### Test 5.4: Question mark glob
**Command:** `touch a.txt b.txt c.txt && ls ?.txt`
**Expected:** Should list all three files matching `?.txt`.
**Result:** 🐛 BUG — Glob expands to `a.txt` but `ls` treats it as a non-directory file path, producing an error.

**Actual Output:**
```
ls: Not a directory: a.txt
```

**Root Cause:** When glob expands `?.txt` to file names like `a.txt`, `ls` tries to list them as directories. The `ls` command doesn't handle being passed file paths from glob expansion correctly — it should display the file info rather than erroring.

### Test 5.5: Glob `*` in root directory
**Command:** `ls /*`
**Expected:** Should list contents of all top-level directories.
**Result:** 🐛 BUG — Glob expands to include `/etc/hostname` (a file), and `ls` errors on it.

**Actual Output:**
```
ls: Not a directory: /etc/hostname
```

---

## Test Category 6: Permission System Stress Tests

### Test 6.1: Remove all permissions and try to read
**Command:** `echo "secret" > /tmp/secret.txt && chmod 000 /tmp/secret.txt && cat /tmp/secret.txt`
**Expected:** Permission denied error.
**Result:** ✅ PASS

**Actual Output:**
```
cat: Permission denied: /tmp/secret.txt
```

### Test 6.2: Remove write permission and try to write via redirect
**Command:** `echo "data" > /tmp/test.txt && chmod 444 /tmp/test.txt && echo "more" >> /tmp/test.txt`
**Expected:** Permission denied error.
**Result:** 🐛 **CRITICAL BUG** — Uncaught `VfsException` crashes the application with a full stack trace when output redirection (`>>`) targets a read-only file.

**Actual Output:**
```
Exception in thread "main" linuxlingo.shell.vfs.VfsException: Permission denied: /tmp/test.txt
        at linuxlingo.shell.vfs.VirtualFileSystem.writeFile(VirtualFileSystem.java:390)
        at linuxlingo.shell.ShellSession.runPlan(ShellSession.java:341)
        ...
```

**Root Cause:** The output redirection handler in `runPlan()` calls `vfs.writeFile()` without a try-catch for `VfsException`. When the file is read-only, the uncaught exception propagates all the way up and crashes the app.

### Test 6.3: Invalid octal permission
**Command:** `touch /tmp/t.txt && chmod 888 /tmp/t.txt`
**Expected:** Error about invalid permission mode.
**Result:** ✅ PASS

**Actual Output:**
```
chmod: invalid mode: 888
```

### Test 6.4: 4-digit octal (sticky bit)
**Command:** `touch /tmp/t.txt && chmod 1755 /tmp/t.txt`
**Expected:** Error (only 3-digit supported) or handle sticky bit.
**Result:** ✅ PASS (correctly rejects 4-digit octal)

**Actual Output:**
```
chmod: invalid mode: 1755
```

### Test 6.5: Invalid symbolic permission
**Command:** `touch /tmp/t.txt && chmod xyz /tmp/t.txt`
**Expected:** Error about invalid mode.
**Result:** ✅ PASS

**Actual Output:**
```
chmod: invalid mode: xyz
```

### Test 6.6: chmod on non-existent file
**Command:** `chmod 755 /tmp/ghost.txt`
**Expected:** Error: file not found.
**Result:** ✅ PASS

**Actual Output:**
```
chmod: No such file or directory: /tmp/ghost.txt
```

### Test 6.7: Remove execute on directory, then try to cd into it
**Command:** `mkdir /tmp/noexec && chmod 644 /tmp/noexec && cd /tmp/noexec`
**Expected:** In real Unix, `cd` requires execute permission on directory. Should deny.
**Result:** ✅ PASS

**Actual Output:**
```
cd: permission denied: /tmp/noexec
```

---

## Test Category 7: VFS Structural Edge Cases

### Test 7.1: Delete the current working directory
**Command:** `mkdir /tmp/workdir && cd /tmp/workdir && rm -r /tmp/workdir && pwd`
**Expected:** Should error or at least indicate that CWD no longer exists.
**Result:** 🐛 BUG — `pwd` still returns `/tmp/workdir` even though the directory has been deleted. The working directory reference is stale/dangling.

**Actual Output:**
```
/tmp/workdir
```

### Test 7.2: Move directory into itself
**Command:** `mkdir /tmp/selfmove && mv /tmp/selfmove /tmp/selfmove/inside`
**Expected:** Error: cannot move directory into itself.
**Result:** 🐛 **BUG** — No error. The directory is silently orphaned from the VFS tree and becomes inaccessible.

**Actual Output:**
```
(no output, no error)
```

**Verification:** Running `ls /tmp` after the self-move shows `selfmove` is gone — the node is disconnected from the tree.

### Test 7.3: Copy directory into itself
**Command:** `mkdir /tmp/selfcopy && cp -r /tmp/selfcopy /tmp/selfcopy/inside`
**Expected:** Error or infinite recursion protection.
**Result:** ⚠️ MINOR — No error, silently succeeds. Creates an `inside` subdirectory within `selfcopy` (shallow copy of the empty dir, so no infinite recursion in this case). However, with contents this could be problematic.

**Actual Output:**
```
(no output, no error)
```

### Test 7.4: Create file in root directory
**Command:** `touch /rootfile.txt && ls /`
**Expected:** Should create file in root.
**Result:** ✅ PASS

**Actual Output:**
```
home/
tmp/
etc/
rootfile.txt
```

### Test 7.5: Delete root directory
**Command:** `rm -rf /`
**Expected:** Should be prevented (root is protected).
**Result:** ✅ PASS

**Actual Output:**
```
rm: Cannot delete root directory
```

### Test 7.6: Very deep directory nesting
**Command:** `mkdir -p /a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t && cd /a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t && pwd`
**Expected:** Should work correctly.
**Result:** ✅ PASS

**Actual Output:**
```
/a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t
```

### Test 7.7: mkdir existing directory (no -p)
**Command:** `mkdir /tmp`
**Expected:** Error: directory already exists.
**Result:** ✅ PASS

**Actual Output:**
```
mkdir: Directory already exists: /tmp
```

### Test 7.8: touch existing file
**Command:** `echo "content" > /tmp/existing.txt && touch /tmp/existing.txt && cat /tmp/existing.txt`
**Expected:** Content should be preserved (touch does not overwrite).
**Result:** ✅ PASS

**Actual Output:**
```
content
```

### Test 7.9: cat a directory
**Command:** `cat /tmp`
**Expected:** Error: is a directory.
**Result:** ✅ PASS

**Actual Output:**
```
cat: Is a directory: /tmp
```

### Test 7.10: mv root
**Command:** `mv / /newroot`
**Expected:** Error: cannot move root.
**Result:** ✅ PASS

**Actual Output:**
```
mv: Cannot move root directory
```

---

## Test Category 8: Environment Save/Load/Reset

### Test 8.1: Save with special characters in name
**Command:** `save my@env!`
**Expected:** Error about invalid characters.
**Result:** ✅ PASS

**Actual Output:**
```
save: invalid environment name: my@env!
```

### Test 8.2: Save empty name
**Command:** `save`
**Expected:** Error about missing name.
**Result:** ✅ PASS

**Actual Output:**
```
save: usage: save <name>
```

### Test 8.3: Load non-existent environment
**Command:** `load nonexistent_env`
**Expected:** Error: environment not found.
**Result:** ✅ PASS

**Actual Output:**
```
load: Environment not found: nonexistent_env
```

### Test 8.4: Save, modify VFS, load — verify restore
**Command:** `mkdir /tmp/testdir && save test-env && rm -r /tmp/testdir && load test-env && ls /tmp`
**Expected:** After load, `/tmp/testdir` should exist again.
**Result:** ✅ PASS

**Actual Output:**
```
testdir/
```

### Test 8.5: Reset verification
**Command:** `mkdir /tmp/custom && touch /tmp/custom/file.txt && reset && ls /`
**Expected:** After reset, VFS returns to default state with only `home/`, `tmp/`, `etc/`.
**Result:** ✅ PASS

**Actual Output:**
```
home/
tmp/
etc/
```

### Test 8.6: Delete non-existent environment
**Command:** `envdelete ghost_env`
**Expected:** Error: environment not found.
**Result:** ✅ PASS

**Actual Output:**
```
envdelete: environment not found: ghost_env
```

### Test 8.7: Save and load preserves working directory
**Command:** `cd /home/user && save wd-test && cd / && load wd-test && pwd`
**Expected:** Should restore working directory to `/home/user`.
**Result:** ✅ PASS

**Actual Output:**
```
/home/user
```

---

## Test Category 9: Non-Interactive (One-Shot) Mode

### Test 9.1: Basic exec
**Command (terminal):** `java -jar build/libs/LinuxLingo.jar exec "echo hello"`
**Expected:** Prints `hello` and exits.
**Result:** ✅ PASS

**Actual Output:**
```
hello
```

### Test 9.2: exec with empty command
**Command (terminal):** `java -jar build/libs/LinuxLingo.jar exec ""`
**Expected:** No output or error, graceful exit.
**Result:** ✅ PASS

**Actual Output:**
```
(no output)
```

### Test 9.3: exec with pipe
**Command (terminal):** `java -jar build/libs/LinuxLingo.jar exec "echo hello world | wc -w"`
**Expected:** Prints `2`.
**Result:** ✅ PASS

**Actual Output:**
```
2
```

### Test 9.4: exec with environment flag but no command
**Command (terminal):** `java -jar build/libs/LinuxLingo.jar exec -e myenv`
**Expected:** Should print an error like "exec -e: missing command after environment name".
**Result:** 🐛 BUG — The `-e` flag is treated as the command to execute (instead of as a flag). This is because `args.length == 3` doesn't satisfy the `>= 4` check, so the `-e myenv` is concatenated and executed as a shell command.

**Actual Output:**
```
WARNING: Command not found: '-e'
-e: command not found
Did you mean 'cd'?
-e: command not found
Did you mean 'cd'?
```

### Test 9.5: Unknown top-level command
**Command (terminal):** `java -jar build/libs/LinuxLingo.jar foobar`
**Expected:** Should print help/usage info.
**Result:** ✅ PASS

**Actual Output:**
```
Unknown command: foobar
Usage: java -jar LinuxLingo.jar [shell|exec|exam]
```

### Test 9.6: exec with no arguments
**Command (terminal):** `java -jar build/libs/LinuxLingo.jar exec`
**Expected:** Should error gracefully.
**Result:** ✅ PASS

**Actual Output:**
```
exec: missing command
```

### Test 9.7: Multiple arguments without exec
**Command (terminal):** `java -jar build/libs/LinuxLingo.jar arg1 arg2 arg3`
**Expected:** Should print help/usage.
**Result:** ✅ PASS

**Actual Output:**
```
Unknown command: arg1
Usage: java -jar LinuxLingo.jar [shell|exec|exam]
```

---

## Test Category 10: REPL Input Edge Cases

### Test 10.1–10.2: Empty / whitespace-only input
**Expected:** Should silently ignore and re-prompt.
**Result:** ✅ PASS (tested via exec — returns empty result)

### Test 10.3: Very long input line (5000 characters)
**Command:** `echo <5000 'a' characters>`
**Expected:** Should handle without crashing.
**Result:** ✅ PASS (output is 5001 bytes)

### Test 10.4: Non-existent command in shell
**Command:** `thisdoesnotexist`
**Expected:** Error: command not found.
**Result:** ✅ PASS (also provides "Did you mean" suggestion)

**Actual Output:**
```
thisdoesnotexist: command not found
```

### Test 10.5: Invalid flag
**Command:** `ls -z`
**Expected:** Should error about unknown flag.
**Result:** ✅ PASS

**Actual Output:**
```
ls: invalid option -- z
```

### Test 10.6: Variable expansion $?
**Command:** `echo $?`
**Expected:** Should print the exit code of the last command (0 if first command).
**Result:** ✅ PASS

**Actual Output:**
```
0
```

### Test 10.7: Variable expansion with undefined variable
**Command:** `echo $UNDEFINED_VAR`
**Expected:** Should print the literal `$UNDEFINED_VAR` or empty string.
**Result:** ✅ PASS (keeps literal)

**Actual Output:**
```
$UNDEFINED_VAR
```

### Test 10.8: help command inside shell
**Command:** `help`
**Expected:** Should list available shell commands.
**Result:** ✅ PASS (lists all 35 commands with descriptions)

---

## Test Category 11: Alias & History Edge Cases

### Test 11.1: Alias with pipe in value
**Command:** `alias countfiles='ls | wc -l' && countfiles`
**Expected:** Should execute the piped alias.
**Result:** 🐛 BUG — Alias set in a chained command is NOT visible to subsequent commands in the same command line. `countfiles` is reported as "command not found".

**Actual Output:**
```
countfiles: command not found
```

**Note:** Also tested with `;` separator — same result. Aliases only take effect in subsequent separate command invocations (separate lines in interactive mode), not within the same parsed plan.

### Test 11.2: Circular alias
**Command:** `alias a='b' && alias b='a' && a`
**Expected:** Should detect circular alias and not infinite loop.
**Result:** 🐛 BUG (two issues):
1. Same alias-not-persisting issue as 11.1 makes this test inconclusive via chaining.
2. Even if aliases were set (in interactive mode), circular aliases result in "command not found" with no specific circular-alias error message. The `resolveAlias()` method detects the cycle and returns the raw value, which then fails lookup — but the error message doesn't indicate the circular alias problem.

**Actual Output:**
```
a: command not found
Did you mean 'cat'?
```

### Test 11.3: Alias overriding built-in command
**Command:** `alias ls='echo hacked' && ls`
**Expected:** Should print `hacked` instead of listing files.
**Result:** 🐛 BUG — Same alias persistence issue. `ls` after `&&` doesn't see the alias set in the previous segment.

**Actual Output:**
```
ls: command not found
```

**Note:** In this case, the alias resolution changes the command name from `ls` to `echo`, but since aliases don't persist across segments in the same plan, `ls` is looked up directly. Even worse: the alias doesn't just fail — `ls` (a built-in command!) becomes "not found" because the alias resolution attempt interferes.

### Test 11.4: unalias non-existent alias
**Command:** `unalias nonexistent`
**Expected:** Error or silent success.
**Result:** ✅ PASS

**Actual Output:**
```
unalias: nonexistent: not found
```

### Test 11.5: history -c then history
**Command:** `history -c && history`
**Expected:** History should be cleared.
**Result:** ✅ PASS (no output after clearing)

**Actual Output:**
```
(no output)
```

### Test 11.6: Alias with empty value
**Command:** `alias empty=''`
**Expected:** Should not crash.
**Result:** ✅ PASS (alias created silently)

---

## Cross-Cutting Issue: Doubled Error Messages in One-Shot Mode

**Observation:** All error messages in one-shot (`exec`) mode are printed **twice**.

**Example:** `java -jar LinuxLingo.jar exec "cat no_such_file"` outputs:
```
cat: No such file or directory: no_such_file
cat: No such file or directory: no_such_file
```

**Root Cause:** Error output flows through two paths:
1. `ShellSession.runPlan()` prints stderr immediately via `ui.println(result.getStderr())` (line ~337).
2. `LinuxLingo.handleExec()` also prints `ui.printError(result.getStderr())` on the returned result.

Both paths fire, causing every error to appear twice.

---

## Bug Summary

| Bug # | Category | Test Case | Description | Severity |
|-------|----------|-----------|-------------|----------|
| 1 | Piping/Redirect | 3.4 | **Input redirection (`<`) from non-existent file causes uncaught `VfsException` crash** with full stack trace. The `vfs.readFile()` call in `runPlan()` is not wrapped in try-catch. | **Critical** |
| 2 | Permissions | 6.2 | **Output redirection (`>>`) to a read-only file causes uncaught `VfsException` crash** with full stack trace. The `vfs.writeFile()` call in the redirect handler of `runPlan()` is not wrapped in try-catch. | **Critical** |
| 3 | Command Chaining | 4.4 | **`&&` uses `break` instead of `continue`**, causing all subsequent segments (including `;`-separated ones) to be skipped when a command fails. In bash, `;` is unconditional and should always execute regardless of prior `&&`/`\|\|` results. | **High** |
| 4 | Command Chaining | 4.5 | **`\|\|` uses `break` instead of `continue`** when the previous command succeeds, killing all subsequent segments including `;`-separated ones. Same root cause as Bug #3. | **High** |
| 5 | Command Chaining | 4.6 | **Intermediate segment stdout is silently discarded in one-shot mode.** `echo "a" && echo "b" && echo "c"` only prints `c`. `handleExec()` only prints the final `CommandResult`'s stdout. | **High** |
| 6 | One-Shot Mode | Cross-cutting | **All error messages printed twice in exec mode.** `runPlan()` prints stderr inline, and `handleExec()` prints stderr from the returned result. | **Medium** |
| 7 | VFS Structure | 7.2 | **Moving a directory into itself silently orphans it** from the VFS tree. No error is produced. `mv /tmp/selfmove /tmp/selfmove/inside` results in `selfmove` vanishing entirely. Should produce "cannot move directory into a subdirectory of itself". | **Medium** |
| 8 | VFS Structure | 7.1 | **Deleting CWD leaves a dangling working directory reference.** `pwd` still returns the path of the deleted directory. Should at minimum reset CWD to `/` or parent. | **Medium** |
| 9 | Piping/Redirect | 3.1, 3.3 | **Pipe with no right-hand command and redirect with no filename are silently ignored** instead of producing an error. Parser drops incomplete operators without warning. | **Medium** |
| 10 | One-Shot Mode | 9.4 | **`exec -e myenv` (without a command) treats `-e` as the command** to execute instead of showing a usage error. The condition `args.length >= 4` is too strict — should handle the 3-arg case with a proper error. | **Low** |
| 11 | Alias | 11.1, 11.3 | **Aliases set within a command line do not persist to subsequent segments** in the same parsed plan. `alias x='y' && x` fails because alias resolution happens before segment execution on each iteration, but the alias store is only updated during execution. | **Low** |
| 12 | Tokenization | 2.3 | **Empty quoted arguments (`""`, `''`) are silently dropped** by the tokenizer. In bash, `echo "" ""` outputs a space (two empty args); LinuxLingo outputs a bare newline. | **Low** |
| 13 | Tokenization | 2.1, 2.2 | **Unterminated quotes are silently closed** at end of input with no error or warning. Could lead to confusing behavior when users forget to close quotes. | **Low** |
