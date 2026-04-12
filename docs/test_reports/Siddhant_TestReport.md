# Test Report - Siddhant

**Scope:** UG Features 6–12 (file operations + text processing). Commands tested: `cat`, `echo` (redirection), `cp`, `diff`, `head`, `tail`, `grep`, `wc`, `rm`, `ls`.

**Method:** One-shot mode via `java -jar build/libs/LinuxLingo.jar exec "<command>"`.

---

## File Operations

### Test 1.1: `cat` displays file content

**Command:** `mkdir /tmp/sid && echo hello > /tmp/sid/a.txt && cat /tmp/sid/a.txt`

**Expected:** `hello`

**Actual:**

```text
hello
```

**Result:** PASS

### Test 1.2: `cp` + `diff` (no differences)

**Command:** `mkdir /tmp/sid && echo hello > /tmp/sid/a.txt && cp /tmp/sid/a.txt /tmp/sid/b.txt && diff /tmp/sid/a.txt /tmp/sid/b.txt`

**Expected:** No output (files identical)

**Actual:**

```text
<no output>
```

**Result:** PASS

### Test 1.3: `rm` removes file

**Command:** `mkdir /tmp/sid && echo hello > /tmp/sid/a.txt && cp /tmp/sid/a.txt /tmp/sid/b.txt && rm /tmp/sid/b.txt && ls /tmp/sid`

**Expected:** Only `a.txt` remains

**Actual:**

```text
a.txt
```

**Result:** PASS

---

## Text Processing

### Test 2.1: `sort` orders lines

**Command:** `mkdir /tmp/sid && echo -e -n "b\na\na" > /tmp/sid/c.txt && sort /tmp/sid/c.txt`

**Expected:**

```text
a
a
b
```

**Actual:**

```text
a
a
b
```

**Result:** PASS

### Test 2.2: `head` shows first N lines

**Command:** `mkdir /tmp/sid && echo -e -n "b\na\na" > /tmp/sid/c.txt && head -n 2 /tmp/sid/c.txt`

**Expected:**

```text
b
a
```

**Actual:**

```text
b
a
```

**Result:** PASS

### Test 2.3: `tail` shows last N lines

**Command:** `mkdir /tmp/sid && echo -e -n "b\na\na" > /tmp/sid/c.txt && tail -n 1 /tmp/sid/c.txt`

**Expected:**

```text
a
```

**Actual:**

```text
a
```

**Result:** PASS

### Test 2.4: `grep -n` shows line numbers

**Command:** `mkdir /tmp/sid && echo -e -n "b\na\na" > /tmp/sid/c.txt && grep -n a /tmp/sid/c.txt`

**Expected:**

```text
2:a
3:a
```

**Actual:**

```text
2:a
3:a
```

**Result:** PASS

### Test 2.5: `wc` counts lines/words/chars

**Command:** `mkdir /tmp/sid && echo -e -n "b\na\na" > /tmp/sid/c.txt && wc /tmp/sid/c.txt`

**Expected:** 2 lines, 3 words, 5 chars

**Actual:**

```text
2 3 5 /tmp/sid/c.txt
```

**Result:** PASS
