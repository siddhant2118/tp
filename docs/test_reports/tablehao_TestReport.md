# Test Report - tablehao

---

## Glob Expansion

Single line command to create some files and directories for testing.

`mkdir -p dir1/nested dir2 && touch a.txt b.txt c.log file1 file2 fileA fileB test.c test.cpp README.md .hidden .env data1.csv data2.csv data_final.csv script.sh notes.txt archive.tar.gz img01.png img02.png img10.png dir1/a.txt dir1/b.log dir1/nested/deep.txt dir2/test1.txt dir2/test2.log`

### Test Case 1.1: Hidden Files

**Command:** `ls *`  
**Expected:** Only non-hidden file/directory should be listed.  
**Actual**: Hidden file/directory are listed and the order of files look incorrect. `/dir2` should contain only `test1.txt` and `test2.log`.

```bash
user@linuxlingo:/home/user$ ls *
.env
.hidden
README.md
a.txt
archive.tar.gz
b.txt
c.log
data1.csv
data2.csv
data_final.csv

dir1:
nested/
a.txt
b.log

dir2:
test1.txt
test2.log
file1
file2
fileA
fileB
img01.png
img02.png
img10.png
notes.txt
script.sh
test.c
test.cpp
```

### Test Case 1.2: Directory Matching

**Command:** `ls dir1/*`  
**Expected:** List everything in `dir1/` including the nested folder.  
**Actual:** `deep.txt` is repeated twice for some reason.

```bash
user@linuxlingo:/home/user$ ls dir1/*
a.txt
b.log

/home/user/dir1/nested:
deep.txt
deep.txt
```

### Test Case 1.3: Empty String

**Command:** `ls ""`  
**Expected:** Return nothing or error.  
**Actual:** Behaves exactly like `ls`.

--- 

## Variable Expansion

### Test Case 2.1: Escaping $

**Command:** `echo \$HOME`  
**Expected:** `$HOME`  
**Actual:** `$` was not escaped and actual home path was printed.

```bash
user@linuxlingo:/home/user$ echo \$HOME
/home/user
```
