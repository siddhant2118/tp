# Test Report — Features 1–5: Edge Cases & Unexpected Inputs

**Scope:** Navigation, File Operations, Text Processing, Piping/Redirection, Command Chaining — tested with strange, malformed, and boundary inputs.

**Methodology:** Black-box edge-case testing. Inputs chosen to be unexpected, malformed, empty, oversized, duplicated, or structurally invalid. All tests run via `java -jar build/libs/LinuxLingo.jar exec "<command>"` on Windows PowerShell.

**Note:**  This test iteration is designed to evaluate feature robustness and error handling by exercising edge cases, invalid inputs, and non-standard usage scenarios.

---

## Test Category 1: Navigation — Edge Cases

### Test 1.1: `cd` with no arguments

**Command:** `cd`
**Expected:** Goes to home directory `/home/user`
**Result:** PASS (after following up with `pwd`)

---

### Test 1.2: `cd /` goes to root

**Command:** `cd /`
**Expected:** Working directory becomes `/`
**Result:** PASS — subsequent `pwd` in same batch confirmed `/`

---

### Test 1.3: `cd` with dot-dot traversal in middle of path

**Command:** `cd /home/user/../user/../user`
**Expected:** Resolves to `/home/user`
**Result:** PASS

---

### Test 1.4: `cd ..` repeated beyond root stays at root

**Command:** `cd /home/user && cd ../../../../../../..`
**Expected:** Clamped at `/`
**Result:** PASS — output confirmed `/`

---

### Test 1.5: `cd` with empty single-quoted string

**Command:** `cd ''`
**Expected:** Remain in current directory
**Result:** PASS

```text
(no output)
```

**Note:** Empty string argument is silently ignored. Bash would treat `cd ''` as an error. Users may not realise the command had no effect.

---

### Test 1.6: `cd` with only whitespace argument

**Command:** `cd    `
**Expected:** Treated as `cd` with no argument (go home) or error
**Result:** PASS — treated as bare `cd`, goes to `/home/user`

---

### Test 1.7: `ls -l -a -R` combined flags recursive

**Command:** `ls -l -a -R`
**Expected:** Recursive long listing of all directories including hidden files
**Result:** PASS — full recursive tree printed with permissions

```text
/:
drwxr-xr-x  1 user user  0  home/
...
/etc:
-rw-r--r--  1 user user  10  hostname
```

---

### Test 1.8: `ls` on multiple nonexistent directories

**Command:** `ls doesnotexist1 doesnotexist2`
**Expected:** Error for each missing directory
**Result:** PARTIAL — only one error printed for `doesnotexist1`, `doesnotexist2` not mentioned

```text
ls: No such file or directory: doesnotexist1
```

**Note:** When multiple nonexistent paths are given, only the first error is reported. The second argument is silently ignored.

---

### Test 1.9: `pwd` with extra arguments

**Command:** `pwd pwd pwd`
**Expected:** Either error for unexpected arguments, or just prints `/`
**Result:** PASS — extra arguments silently ignored, prints `/`

```text
/
```

---

### Test 1.10: `cd -` through multiple directory changes

**Command:** `cd /tmp && cd /home && cd /tmp && cd /home && cd - && pwd`
**Expected:** `/tmp` (the directory before the last `cd /home`)
**Result:** PASS

```text
/tmp
```

---

### Test 1.11: Uppercase command `CD`

**Command:** `CD /home/user`
**Expected:** Command not found (commands are case-sensitive)
**Result:** PASS — error shown with suggestion

```text
CD: command not found
Did you mean 'cd'?
```

---

### Test 1.12: `ls` with invalid flag `-z`

**Command:** `ls -z`
**Expected:** Error for unrecognised flag
**Result:** PASS

```text
ls: invalid option -- z
```

---

## Test Category 2: File Operations — Edge Cases

### Test 2.1: `touch` with no arguments

**Command:** `touch`
**Expected:** Error for missing operand
**Result:** PASS

```text
touch: missing file operand
```

---

### Test 2.2: `touch` creates hidden file

**Command:** `touch .hiddenfile && ls -a`
**Expected:** `.hiddenfile` appears with `-a` flag
**Result:** PASS

```text
home/
tmp/
etc/
.hiddenfile
```

---

### Test 2.3: `touch` with filename containing spaces (single-quoted)

**Command:** `touch 'file with spaces.txt' && ls`
**Expected:** File named `file with spaces.txt` created
**Result:** PASS

```text
file with spaces.txt
```

---

### Test 2.4: `touch` on already-existing file

**Command:** `touch a.txt && touch a.txt && ls`
**Expected:** No error, file still exists (idempotent)
**Result:** PASS — only one `a.txt` in listing, no error

---

### Test 2.5: `mkdir` with no arguments

**Command:** `mkdir`
**Expected:** Error for missing operand
**Result:** PASS

```text
mkdir: missing operand
```

---

### Test 2.6: `mkdir` on already-existing directory

**Command:** `mkdir /tmp && ls`
**Expected:** Error since `/tmp` already exists
**Result:** PASS

```text
mkdir: Directory already exists: /tmp
```

---

### Test 2.7: `cp` source and destination are same file

**Command:** `echo hello > a.txt && cp a.txt a.txt`
**Expected:** Error — cannot copy file onto itself
**Result:** PASS

```text
cp: 'a.txt' and 'a.txt' are the same file
```

---

### Test 2.8: `rm -f` on nonexistent file

**Command:** `rm -f nonexistent.txt`
**Expected:** Silent success (`-f` suppresses errors)
**Result:** PASS — no output, no error

---

### Test 2.9: `rm` without `-f` on nonexistent file

**Command:** `rm nonexistent.txt`
**Expected:** Error message
**Result:** PASS

```text
rm: No such file or directory: nonexistent.txt
```

---

### Test 2.10: `echo >` with no text (empty redirect)

**Command:** `echo > emptyredirect.txt && cat emptyredirect.txt`
**Expected:** File created with just a newline, `cat` prints blank line
**Result:** PASS — blank line printed, file exists

```text
(blank line)
```

---

### Test 2.11: Very long single argument (200 chars)

**Command:** `echo aaa...(200 a's) > big.txt && wc -c big.txt`
**Expected:** Character count reported correctly
**Result:** PASS

```text
201 big.txt
```

**Note:** 200 characters + 1 newline = 201. Correct.

---

### Test 2.12: `diff` on two nonexistent files

**Command:** `diff nonexistent1.txt nonexistent2.txt`
**Expected:** Error for missing file(s)
**Result:** PARTIAL  — only reports error for the first file, second not checked

```text
diff: No such file or directory: nonexistent1.txt
```

---

### Test 2.13: `diff` of a file against itself

**Command:** `touch a.txt && diff a.txt a.txt`
**Expected:** No output (identical)
**Result:** PASS — no output

---

### Test 2.14: `cp` with no arguments

**Command:** `cp`
**Expected:** Usage error
**Result:** PASS

```text
cp: cp [-r] <src...> <dest>
```

---

### Test 2.15: `mv` source and destination are same file

**Command:** `mv a.txt a.txt`
**Expected:** Error — cannot move file onto itself
**Result:** PASS

```text
mv: 'a.txt' and 'a.txt' are the same file
```

---

### Test 2.16: `tee` with no file argument

**Command:** `echo hello | tee`
**Expected:** Usage error
**Result:** PASS

```text
tee: tee [-a] <file> [file2...]
```

---

### Test 2.17: `cat` with no arguments and no pipe

**Command:** `cat`
**Expected:** Either hang waiting for stdin or informative error
**Result:** PASS — informative error instead of hanging

```text
cat: reading from stdin is not supported in LinuxLingo. Provide a filename or use piping.
```

---

## Test Category 3: Text Processing — Edge Cases

### Test 3.1: `grep` on empty file

**Command:** `touch empty.txt && grep anything empty.txt`
**Expected:** No output (no matches)
**Result:** PASS — no output, no error

---

### Test 3.2: `wc` on empty file

**Command:** `touch empty.txt && wc empty.txt`
**Expected:** `0 0 0 empty.txt`
**Result:** PASS

```text
0 0 0 empty.txt
```

---

### Test 3.3: `sort` on empty file

**Command:** `touch empty.txt && sort empty.txt`
**Expected:** No output
**Result:** PASS — no output

---

### Test 3.4: `head -n 5` on empty file

**Command:** `touch empty.txt && head -n 5 empty.txt`
**Expected:** No output
**Result:** PASS — no output

---

### Test 3.5: `tail -n 5` on empty file

**Command:** `touch empty.txt && tail -n 5 empty.txt`
**Expected:** No output
**Result:** PASS — no output

---

### Test 3.6: `head -n 0` returns nothing

**Command:** `echo hello > f.txt && head -n 0 f.txt`
**Expected:** No output
**Result:** PASS — no output

---

### Test 3.7: `head -n 99999` on small file

**Command:** `echo hello > f.txt && head -n 99999 f.txt`
**Expected:** Shows all lines (just `hello`)
**Result:** PASS

```text
hello
```

---

### Test 3.8: `tail -n 0` returns nothing

**Command:** `echo hello > f.txt && tail -n 0 f.txt`
**Expected:** No output
**Result:** PASS — no output

---

### Test 3.9: `grep` with no arguments

**Command:** `grep`
**Expected:** Usage error or missing pattern error
**Result:** PASS

```text
grep: missing pattern
```

---

### Test 3.10: `grep` with empty string pattern

**Command:** `echo hello > f.txt && grep '' f.txt`
**Expected:** All lines match (empty pattern matches everything)
**Result:** PASS

```text
hello
```

---

### Test 3.11: `grep -E` with `.*` pattern (matches everything)

**Command:** `echo hello > f.txt && grep -E '.*' f.txt`
**Expected:** All lines printed
**Result:** PASS

```text
hello
```

---

### Test 3.12: `grep -E` with invalid regex `[`

**Command:** `echo hello > f.txt && grep -E '[' f.txt`
**Expected:** Error for invalid regex
**Result:** PASS

```text
grep: invalid regular expression
```

---

### Test 3.13: `wc` with no arguments

**Command:** `wc`
**Expected:** Usage error
**Result:** PASS

```text
wc: missing file operand
```

---

### Test 3.14: `sort` with no arguments

**Command:** `sort`
**Expected:** Usage error
**Result:** PASS

```text
sort: missing file operand
```

---

### Test 3.15: `head -n -1` (negative count)

**Command:** `echo hello > f.txt && head -n -1 f.txt`
**Expected:** Per UG: all lines except the last 1, so no output for a single-line file
**Result:** PASS — no output

---

### Test 3.16: `find` with no arguments (searches current directory)

**Command:** `find`
**Expected:** Lists all files/directories under current working directory
**Result:** PASS — full VFS tree printed

```text
/home
/home/user
/tmp
/etc
/etc/hostname
```

---

### Test 3.17: `find` on nonexistent directory

**Command:** `find /nonexistent`
**Expected:** Error
**Result:** PASS

```text
find: No such file or directory: /nonexistent
```

---
---

### Test 3.18: `sort -rn` on numeric data

**Command:** `echo -e '3\n1\n2' > f.txt && sort -rn f.txt`
**Expected:** `3`, `2`, `1`
**Result:** PASS

```text
3
2
1
```

---

### Test 3.19: `uniq -d` shows only duplicate adjacent lines

**Command:** `echo -e 'a\na\nb\na' > f.txt && uniq -d f.txt`
**Expected:** `a` (only the first adjacent pair qualifies; the lone `a` after `b` does not)
**Result:** PASS

```text
a
```

---

## Test Category 4: Piping and Redirection — Edge Cases

### Test 4.1: Double pipe `| |` (empty segment between pipes)

**Command:** `echo hello | | wc -w`
**Expected:** Syntax error
**Result:** PASS

```text
syntax error: unexpected end of input after operator
```

---

### Test 4.2: Leading pipe with no left-hand command

**Command:** `| echo hello`
**Expected:** Syntax error
**Result:** PASS

```text
syntax error: unexpected end of input after operator
```

---

### Test 4.3: Double redirect on same command

**Command:** `echo hello > out.txt > out2.txt ; cat out.txt ; cat out2.tx`
**Expected:** last redirect succeeds
**Result:** BUG — no output, no error, and neither file is visibly confirmed. Silent failure.

```text
cat: No such file or directory: out.txt
hello

```

**Note:** The implementation does not match expected Bash behaviour for multiple output redirections. In Bash, all redirections are processed left-to-right, resulting in out.txt being created (empty) and out2.txt receiving the output. In the current implementation, only the final redirection is applied, and earlier redirections are ignored entirely (e.g., out.txt is not created). This indicates that only a single RedirectInfo is stored per segment, causing earlier redirections to be discarded. This is undocumented.

---

### Test 4.4: Append redirect to nonexistent parent directory

**Command:** `echo hello >> nonexistentdir/out.txt`
**Expected:** Error — parent directory does not exist
**Result:** PASS

```text
No such file or directory: nonexistentdir/out.txt
redirect write failed
```

---

### Test 4.5: `echo` ignores piped stdin

**Command:** `echo hello | echo world`
**Expected:** `world` only — `echo` does not read stdin
**Result:** PASS — piped input correctly discarded by `echo`

```text
world
```

---

### Test 4.6: Long pipe chain (4 `cat` hops)

**Command:** `cat /etc/hostname | cat | cat | cat | wc -c`
**Expected:** Character count of hostname file contents (10 chars + newline = 11)
**Result:** PASS

```text
11
```

---

### Test 4.7: Redirect overwrites existing file

**Command:** `echo hello > out.txt && echo world > out.txt && cat out.txt`
**Expected:** `world` only
**Result:** PASS

```text
world
```

---

### Test 4.8: `echo` with no text redirected to file

**Command:** `echo > out.txt && cat out.txt`
**Expected:** File contains just a newline, `cat` prints blank line
**Result:** PASS — blank line printed

---

### Test 4.9: Input redirect with no command

**Command:** `< nonexistent.txt`
**Expected:** Syntax error or error about missing command
**Result:** BUG — no output at all, silently does nothing

```text
(no output)
```

**Note:** A bare `< file` with no command produces no error and no output. In bash this would be an error. The segment has no command name so it is silently skipped by the execution engine.

---

### Test 4.10: Triple grep pipe chain

**Command:** `echo hello | grep hello | grep hello | grep hello`
**Expected:** `hello`
**Result:** PASS

```text
hello
```

---

## Test Category 5: Command Chaining — Edge Cases

### Test 5.1: `||` followed by `&&` (mixed precedence)

**Command:** `cat nope || echo caught && echo done`
**Expected:** `cat` fails → `echo caught` runs → `echo done` runs (since caught succeeded)
**Result:** PASS — left-to-right evaluation confirmed

```text
cat: No such file or directory: nope
caught

done
```

---

### Test 5.2: Long `&&` chain (5 commands)

**Command:** `echo a && echo b && echo c && echo d && echo e`
**Expected:** All five outputs
**Result:** PASS

```text
a

b

c

d

e
```

---

### Test 5.3: Long `||` chain with all failures then one success

**Command:** `notacmd || notacmd2 || notacmd3 || echo finally`
**Expected:** Three errors, then `finally`
**Result:** PASS

```text
notacmd: command not found
notacmd2: command not found
notacmd3: command not found
finally
```

---

### Test 5.4: Empty segment between two semicolons `; ;`

**Command:** `echo a ; ; echo b`
**Expected:** Syntax error (empty segment between operators)
**Result:** PASS

```text
syntax error: unexpected end of input after operator
```

---

### Test 5.5: Leading `&&` with no left-hand command

**Command:** `&& echo hello`
**Expected:** Syntax error
**Result:** PASS

```text
syntax error: unexpected end of input after operator
```

---

### Test 5.6: Trailing `&&` with no right-hand command

**Command:** `echo hello &&`
**Expected:** Syntax error
**Result:** PASS

```text
syntax error: unexpected end of input after operator
```

---

### Test 5.7: Leading `||` with no left-hand command

**Command:** `|| echo hello`
**Expected:** Syntax error
**Result:** PASS

```text
syntax error: unexpected end of input after operator
```

---

### Test 5.8: Trailing `||` with no right-hand command

**Command:** `echo hello ||`
**Expected:** Syntax error
**Result:** PASS

```text
syntax error: unexpected end of input after operator
```

---

### Test 5.9: Only semicolons `;;;`

**Command:** `;;;`
**Expected:** Syntax error or empty plan
**Result:** UNEXPECTED — silently produces no output and no error

```text
(no output)
```

**Note:** Three consecutive semicolons with no commands between them produce no error. Bash would report a syntax error. The parser appears to discard all-operator input silently.

---

### Test 5.10: `&&` and `||` alternating chain

**Command:** `echo a && echo b || echo c && echo d`
**Expected:** `a` succeeds → `b` runs → `||` skips `c` (b succeeded) → `d` runs
**Result:** PASS — left-to-right evaluation confirmed

```text
a

b

d
```

---

### Test 5.11: `&&` chain with all failures, then `;` continues

**Command:** `notacmd1 && notacmd2 && notacmd3 ; echo after-semicolon`
**Expected:** `notacmd1` fails, `&&` skips notacmd2 and notacmd3, `;` still runs `echo`
**Result:** PASS

```text
notacmd1: command not found
after-semicolon
```

---

### Test 5.12: Multiple consecutive semicolons with commands

**Command:** `echo hello ; ; ; echo world`
**Expected:** Syntax error (empty segments between semicolons)
**Result:** PASS

```text
syntax error: unexpected end of input after operator
```

---

## Summary

| Category | Total Tests | Pass   | Unexpected/Partial | Bug   | 
|---|-------------|--------|--------------------|-------|
| 1. Navigation | 12          | 11     | 1                  | 0     |
| 2. File Operations | 17          | 16     | 1                  | 0     |
| 3. Text Processing | 19          | 18     | 1                  | 0     |
| 4. Piping & Redirection | 10          | 8      | 0                  | 2     |
| 5. Command Chaining | 12          | 11     | 1                  | 0     |
| **Total** | **70**      | **64** | **4**              | **2** |

---

## Bugs and Issues Found

| ID    | Severity | Category | Description                                                                                                                         |
|-------|---|---|-------------------------------------------------------------------------------------------------------------------------------------|
| BUG-1 | Medium | Piping | Double redirect `echo hello > a.txt > b.txt` — second redirect silently overwrites first in the parser. Undocumented.               |
| BUG-2 | Medium | Piping | Bare `< file` with no command silently does nothing — no error, no output. Should produce a syntax or usage error.                  |
| BUG-3 | Low | Navigation | `ls` with multiple nonexistent arguments only reports an error for the first one. Subsequent missing paths are silently ignored.    |
| BUG-4 | Low | File Operations | `diff` with two nonexistent files only reports error for the first file. Same silent-skip issue as BUG-3.                           |
| BUG-5 | Low | Chaining | `;;;` (only semicolons, no commands) produces no output and no error. Should be a syntax error consistent with `echo a ; ; echo b`. |