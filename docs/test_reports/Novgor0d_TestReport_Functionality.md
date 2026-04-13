# Test Report (Core Functionality Testing) — Features 1–5: Navigation, File Operations, Text Processing, Piping/Redirection, Command Chaining

**Scope:** Shell simulator commands covering navigation, file operations, text processing, piping, redirection, and command chaining operators.

**Methodology:** Black-box testing from a user-facing perspective. All tests were executed via `java -jar build/libs/LinuxLingo.jar exec "<command>"` (one-shot mode) on Windows PowerShell.

**Note:**  This testing iteration focuses primarily on validating core functionality using standard input scenarios, and does not extensively cover edge cases or unexpected inputs.

---

## Test Category 1: Navigation Commands

### Test 1.1: `pwd` at default working directory

**Command:** `pwd`
**Expected:** `/`
**Result:** PASS

```text
/
```

---

### Test 1.2: `cd` to absolute path then `pwd`

**Command:** `cd /home/user && pwd`
**Expected:** `/home/user`
**Result:** PASS

```text
/home/user
```

---

### Test 1.3: `cd ..` from root

**Command:** `cd .. && pwd`
**Expected:** `/` (parent of root is root)
**Result:**  PASS

```text
/
```

---

### Test 1.4: `cd -` returns to previous directory

**Command:** `cd /home/user && cd .. && cd - && pwd`
**Expected:** `/home/user`
**Result:**  PASS

```text
/home/user
```

---

### Test 1.5: `cd ~` goes to home

**Command:** `cd ~ && pwd`
**Expected:** `/home/user`
**Result:**  PASS

```text
/home/user
```

---

### Test 1.6: `cd` into nonexistent directory

**Command:** `cd nonexistent`
**Expected:** Error message indicating directory not found
**Result:**  PASS

```text
cd: No such file or directory: nonexistent
```

---

### Test 1.7: `ls` at root

**Command:** `ls`
**Expected:** Lists `home/`, `tmp/`, `etc/`
**Result:**  PASS

```text
home/
tmp/
etc/
```

---

### Test 1.8: `ls -a` (show hidden files)

**Command:** `ls -a`
**Expected:** Same as `ls` since no hidden files exist by default 
**Result:**  PASS

```text
home/
tmp/
etc/
```

---

### Test 1.9: `ls -l` (long format)

**Command:** `ls -l`
**Expected:** Long listing with permissions, owner, size, and name for each entry
**Result:**  PASS

```text
drwxr-xr-x  1 user user  0  home/
drwxrwxrwx  1 user user  0  tmp/
drwxr-xr-x  1 user user  0  etc/
```

---

### Test 1.10: `ls` on nonexistent path

**Command:** `ls /nonexistent`
**Expected:** Error message
**Result:**  PASS

```text
ls: No such file or directory: /nonexistent
```

---

## Test Category 2: File Operation Commands

### Test 2.1: `touch` creates a single file

**Command:** `touch test.txt && ls`
**Expected:** `test.txt` appears in listing
**Result:**  PASS

```text
home/
tmp/
etc/
test.txt
```

---

### Test 2.2: `touch` creates multiple files at once

**Command:** `touch a.txt b.txt c.txt && ls`
**Expected:** All three files appear
**Result:**  PASS

```text
home/
tmp/
etc/
a.txt
b.txt
c.txt
```

---

### Test 2.3: `mkdir` creates a directory

**Command:** `mkdir newdir && ls`
**Expected:** `newdir/` appears
**Result:**  PASS

```text
home/
tmp/
etc/
newdir/
```

---

### Test 2.4: `mkdir -p` creates nested directories

**Command:** `mkdir -p a/b/c && ls a/b`
**Expected:** `c/` listed inside `a/b`
**Result:**  PASS

```text
c/
```

---

### Test 2.5: `rm` removes a file

**Command:** `touch f.txt && rm f.txt && ls`
**Expected:** `f.txt` absent from listing
**Result:**  PASS

```text
home/
tmp/
etc/
```

---

### Test 2.6: `rm` on directory without `-r` flag

**Command:** `mkdir d && rm d`
**Expected:** Error requiring `-r` flag
**Result:**  PASS

```text
rm: Cannot remove directory without -r: d
```

---

### Test 2.7: `rm -r` removes a directory recursively

**Command:** `mkdir d && rm -r d && ls`
**Expected:** `d/` absent from listing
**Result:**  PASS

```text
home/
tmp/
etc/
```

---

### Test 2.8: `echo` with output redirect, then `cat`

**Command:** `echo hello > file.txt && cat file.txt`
**Expected:** `hello` printed
**Result:**  PASS

```text
hello
```

---

### Test 2.9: `cp` copies file contents correctly

**Command:** `echo hello > a.txt && cp a.txt b.txt && cat b.txt`
**Expected:** `hello` in copied file
**Result:**  PASS

```text
hello
```

---

### Test 2.10: `mv` renames file

**Command:** `echo hello > a.txt && mv a.txt b.txt && ls`
**Expected:** `b.txt` present, `a.txt` absent
**Result:**  PASS

```text
home/
tmp/
etc/
b.txt
```

---

### Test 2.11: `diff` on two different files

**Command:** `echo line1 > a.txt && echo line2 > b.txt && diff a.txt b.txt`
**Expected:** Shows lines removed from `a.txt` and added in `b.txt`
**Result:**  PASS

```text
--- a.txt
+++ b.txt
-line1
+line2
```

---

### Test 2.12: `diff` on two identical files

**Command:** `echo same > a.txt && echo same > b.txt && diff a.txt b.txt`
**Expected:** No output (files are identical)
**Result:**  PASS

```text
(no output)
```

---

### Test 2.13: `tee` writes to file and also outputs to stdout

**Command:** `echo hello | tee out.txt && cat out.txt`
**Expected:** `hello` printed twice (once from tee stdout, once from cat)
**Result:**  PASS

```text
hello

hello
```

---

### Test 2.14: `cat` on nonexistent file

**Command:** `cat nonexistent.txt`
**Expected:** Error message
**Result:**  PASS

```text
cat: No such file or directory: nonexistent.txt
```

---

### Test 2.15: `echo -n` suppresses trailing newline

**Command:** `echo -n hello`
**Expected:** `hello` with no trailing newline
**Result:**  PASS

```text
hello
```

---

### Test 2.16: `echo -e` interprets escape sequences

**Command:** `echo -e line1\\nline2`
**Expected:** Two lines: `line1` and `line2`
**Result:**  PASS

```text
line1
line2
```

---

## Test Category 3: Text Processing Commands

### Test 3.1: `head -n 3` shows first 3 lines

**Command:** `echo -e 'a\nb\nc\nd\ne' > f.txt && head -n 3 f.txt`
**Expected:** `a`, `b`, `c`
**Result:**  PASS

```text
a
b
c
```

---

### Test 3.2: `tail -n 2` shows last 2 lines

**Command:** `echo -e 'a\nb\nc\nd\ne' > f.txt && tail -n 2 f.txt`
**Expected:** `d`, `e`
**Result:** ️ PARTIAL — outputs `e` and a blank line instead of `d` and `e`

```text
e

```

**Note:** `echo -e` appends a trailing newline, creating an implicit empty line at the end of the file. `tail -n 2` counts this empty line as one of the last two entries, displacing `d`. This is consistent behaviour given the VFS stores the trailing newline, but may be surprising to users. No mitigation is documented.

---

### Test 3.3: `grep` finds matching lines

**Command:** `echo -e 'apple\nbanana\napple' > f.txt && grep apple f.txt`
**Expected:** Two lines: `apple`, `apple`
**Result:**  PASS

```text
apple
apple
```

---

### Test 3.4: `grep -i` case-insensitive matching

**Command:** `echo -e 'Apple\nbanana' > f.txt && grep -i apple f.txt`
**Expected:** `Apple`
**Result:**  PASS

```text
Apple
```

---

### Test 3.5: `grep -v` inverted match

**Command:** `echo -e 'apple\nbanana' > f.txt && grep -v apple f.txt`
**Expected:** `banana`
**Result:**  PASS

```text
banana
```

---

### Test 3.6: `grep -c` count of matching lines

**Command:** `echo -e 'apple\nbanana' > f.txt && grep -c apple f.txt`
**Expected:** `1`
**Result:**  PASS

```text
1
```

---

### Test 3.7: `sort` alphabetically

**Command:** `echo -e 'apple\nbanana\napple' > f.txt && sort f.txt`
**Expected:** `apple`, `apple`, `banana`
**Result:**  PASS

```text
apple
apple
banana
```

---

### Test 3.8: `sort -u` removes duplicates

**Command:** `echo -e 'apple\nbanana\napple' > f.txt && sort -u f.txt`
**Expected:** `apple`, `banana`
**Result:** PASS

```text
apple
banana
```

**Note:** Same trailing newline issue as Test 3.2 — the empty string from the trailing newline is treated as a unique entry and survives deduplication. Expected two entries, got three.

---

### Test 3.9: `uniq` removes adjacent duplicates

**Command:** `echo -e 'apple\napple\nbanana' > f.txt && uniq f.txt`
**Expected:** `apple`, `banana`
**Result:**  PASS

```text
apple
banana
```

---

### Test 3.10: `uniq -c` counts occurrences

**Command:** `echo -e 'apple\napple\nbanana' > f.txt && uniq -c f.txt`
**Expected:** `2 apple`, `1 banana`
**Result:** PARTIAL — also counts trailing empty line as `1 (blank)`

```text
      2 apple
      1 banana
      1 
```

**Note:** Consistent with Test 3.2 — trailing newline artifact.

---

### Test 3.11: `wc` full count

**Command:** `echo hello world > f.txt && wc f.txt`
**Expected:** `1 2 12 f.txt` (1 line, 2 words, 12 chars including newline)
**Result:**  PASS

```text
 1  2 12 f.txt
```

---

### Test 3.12: `wc -w` word count only

**Command:** `echo hello world > f.txt && wc -w f.txt`
**Expected:** `2 f.txt`
**Result:**  PASS

```text
2 f.txt
```

---

### Test 3.13: `find` by name pattern

**Command:** `touch /home/a.txt && find /home -name '*.txt'`
**Expected:** `/home/a.txt`
**Result:**  PASS

```text
/home/a.txt
```

---

### Test 3.14: `find` by type directory and name

**Command:** `find / -type d -name tmp`
**Expected:** `/tmp`
**Result:**  PASS

```text
/tmp
```

---

## Test Category 4: Piping and Redirection

### Test 4.1: Basic pipe

**Command:** `echo hello | wc -w`
**Expected:** `1`
**Result:**  PASS

```text
1
```

---

### Test 4.2: Output redirect (`>`) creates and writes file

**Command:** `echo hello > out.txt && cat out.txt`
**Expected:** `hello`
**Result:**  PASS

```text
hello
```

---

### Test 4.3: Append redirect (`>>`) appends to existing file

**Command:** `echo first > out.txt && echo second >> out.txt && cat out.txt`
**Expected:** `first` then `second`
**Result:**  PASS

```text
first
second
```

---

### Test 4.4: Input redirect (`<`) reads from file

**Command:** `echo hello > in.txt && wc -w < in.txt`
**Expected:** `1`
**Result:**  PASS

```text
1
```

---

### Test 4.5: Multi-stage pipeline

**Command:** `echo -e 'c\nb\na' | sort | head -n 1`
**Expected:** `a`
**Result:**  PASS

```text
a
```

---

### Test 4.6: Unrecognised word after redirect treated as argument

**Command:** `echo hello > out.txt then sort out.txt`
**Expected:** Per UG, `then` is not a separator — likely written to file or produces an error
**Result:**  UNEXPECTED BEHAVIOUR — emits a warning but does not document this in the UG

```text
warning: 'then' is not a command separator, treated as argument
```

**Note:** The UG has no mention of this warning message. The behaviour (treating `then` as an argument to `echo`) is reasonable, but the warning message format is undocumented. It is unclear whether `hello then sort out.txt` was written to the file or just `hello`.

---

### Test 4.7: Redirect with no target file — syntax error

**Command:** `echo hello >`
**Expected:** Syntax error
**Result:**  PASS

```text
syntax error: missing filename for redirect
```

---

### Test 4.8: Input redirect from nonexistent file

**Command:** `cat < nonexistent.txt`
**Expected:** Error message about missing file
**Result:**  PASS — error printed, segment skipped

```text
No such file or directory: nonexistent.txt
redirect failed
```

---

## Test Category 5: Command Chaining

### Test 5.1: `&&` runs second command when first succeeds

**Command:** `echo a && echo b`
**Expected:** `a` then `b`
**Result:**  PASS

```text
a

b
```

---

### Test 5.2: `&&` skips second command when first fails

**Command:** `notacmd && echo skipped`
**Expected:** Error for `notacmd`, `skipped` not printed
**Result:**  PASS

```text
notacmd: command not found
```

---

### Test 5.3: `||` runs second command when first fails

**Command:** `notacmd || echo fallback`
**Expected:** Error for `notacmd`, then `fallback`
**Result:**  PASS

```text
notacmd: command not found
fallback
```

---

### Test 5.4: `||` skips second command when first succeeds

**Command:** `echo ok || echo skipped`
**Expected:** `ok` only
**Result:**  PASS

```text
ok
```

---

### Test 5.5: `;` runs all commands unconditionally

**Command:** `echo a ; echo b ; echo c`
**Expected:** `a`, `b`, `c`
**Result:**  PASS

```text
a

b

c
```

---

### Test 5.6: `;` continues after failure

**Command:** `notacmd ; echo still runs`
**Expected:** Error for `notacmd`, then `still runs`
**Result:**  PASS

```text
notacmd: command not found
still runs
```

---

### Test 5.7: Chaining `mkdir && cd && pwd`

**Command:** `mkdir d && cd d && pwd`
**Expected:** `/d`
**Result:** PASS

```text
/d
```

---

### Test 5.8: `||` and `&&` combined precedence

**Command:** `cat nope || echo caught && echo done`
**Expected:** `nope` fails → `echo caught` runs → `echo done` runs (since caught succeeded)
**Result:** PASS

```text
caught

done
```

---

## Summary

| Category | Total Tests | Pass   | Partial/Unexpected | Fail | Not Captured |
|---|---|--------|--------------------|---|---|
| 1. Navigation | 10 | 10     | 0                  | 0 | 0 |
| 2. File Operations | 16 | 16     | 0                  | 0 | 0 |
| 3. Text Processing | 14 | 12     | 2                  | 0 | 0 |
| 4. Piping & Redirection | 8 | 7      | 1                  | 0 | 0 |
| 5. Command Chaining | 8 | 7      | 0                  | 0 | 1 |
| **Total** | **56** | **52** | **3**              | **0** | **1** |

---

## Issues Found

| ID    | Severity | Description |
|-------|---|---|
| BUG-1 | Low | `echo -e` with `\n` sequences produces a trailing empty line in the VFS. This causes `tail -n N`, and `uniq -c` to include a spurious blank entry. Consistent across commands but not documented. |
| BUG-2 | Low | Undocumented warning message `warning: 'then' is not a command separator, treated as argument` — behaviour is reasonable but absent from the UG. File contents after this command were not verified. |

**Note:** In Test 1.8, executing `ls -a` does not display hidden files. The User Guide should explicitly state whether ls -a is supported and define the handling of hidden files in the shell simulator exists or not.

