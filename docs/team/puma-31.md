# Purav Mahesh – Project Portfolio Page

> **CS2113 – LinuxLingo**  
> GitHub: [`@puma-31`](https://github.com/puma-31)

## Overview

I was the **primary developer and maintainer** of the **Exam module** in **LinuxLingo**, an educational CLI app that teaches Linux commands through practice and exam-style questions. LinuxLingo provides an interactive shell simulator combined with a quiz system, allowing students to practice Linux commands safely in a virtual file system and test their knowledge through multiple question types. My work focused on designing and implementing the **end-to-end exam experience**, from the question data model and parsing pipeline, through exam sessions and interactive workflows, to grading, feedback, and results. The implementation emphasizes robustness, comprehensive testing, maintainability, and a seamless CLI user experience.

## Summary of Contributions

### Code Contributed
- **Link:** [RepoSense Dashboard](https://nus-cs2113-ay2526-s2.github.io/tp-dashboard/?search=puma-31&breakdown=true)

### Enhancements Implemented

#### 1. **Exam Workflow and Commands**

- **Scope:** Designed and implemented exam orchestration system with three entry modes (interactive, direct CLI args, random question).
- **Key Implementation:** `ExamSession`, `ExamResult`, `QuestionInteraction`, `RandomQuestionMode` with command parser (`ExamCommandParser`) supporting strict validation and preventing duplicate flags.
- **Technical Details:** Manages state transitions across three question types (MCQ, FITB, PRAC) with context-aware control commands (`quit`, `abort`, `exit`).

#### 2. **Question Parsing and Loading**

- **Scope:** Built robust, extensible parsing pipeline for loading exam questions from pipe-delimited text files.
- **Key Implementation:** Type-specific parsers (`parseMcq()`, `parseFitb()`, `parsePrac()`) supporting three question formats with validation and graceful error handling (skips malformed lines with warnings).
- **Technical Details:** Handles regex-based splitting, escaped pipes (`\|`), checkpoint parsing (5 types), and setup item parsing for PRAC initialization (`MKDIR`, `FILE`, `PERM`).

#### 3. **Exam Session Management and State Handling**

- **Scope:** Implemented state machine orchestrating exam sessions from start to finish with three control flows (MCQ/FITB answers, PRAC shell sessions).
- **Key Implementation:** `ExamSession.runExam()` orchestration loop, `QuestionInteraction` for centralized answer collection, VFS factory pattern for PRAC isolation.
- **Technical Details:** Defensive programming (null checks, state validation), supports partial completion (abort mid-exam), handles question iteration and result accumulation.

#### 4. **Grading, Feedback, and Results Display**

- **Scope:** Implemented question grading and comprehensive exam results reporting with polymorphic type handling.
- **Key Implementation:** `Question.checkAnswer()` with type-specific logic — `McqQuestion` (case-insensitive A-D matching), `FitbQuestion` (exact string match), `PracQuestion` (VFS verification).
- **Technical Details:** Calculates score, percentage, letter grade; displays structured feedback (✓/✗ + explanation) with CLI-formatted output; handles edge cases (multiple accepted answers, partial correctness).

#### 5. **Practical Questions with VFS Verification**

- **Scope:** Designed and implemented PRAC question system enabling hands-on, auto-graded shell exercises with automatic VFS verification.
- **Key Implementation:** `PracQuestion` with `Checkpoint` list supporting 5 verification types (DIR, FILE, NOT_EXISTS, CONTENT_EQUALS, PERM); `SetupItem` system for VFS pre-configuration.
- **Technical Details:** Factory pattern creates fresh VFS per question; polymorphic checkpoint matching validates file content and permissions; setup items parsed from question bank (MKDIR, FILE, PERM).

#### 6. **Testing and Code Quality**

- **Scope:** Ensured exam module reliability through unit tests and refactoring.
- **Key Implementation:** Tests covering parsing, grading, session logic; edge cases (invalid inputs, malformed questions, skipped/aborted exams); separation of concerns (parsing, business logic, UI layer).
- **Technical Details:** Refactored to eliminate duplication; achieved high coverage for critical exam paths (normal flow, error cases, edge cases).

### Contributions to the User Guide (UG)

I authored the **Exam System** section documenting: exam modes & commands (four invocation formats with examples), interactive flow, in-exam control commands (quit/abort/exit with context-dependent behavior table for MCQ/FITB vs. PRAC), question types with answer instructions, PRAC workflow examples, topic management, and edge case handling for invalid inputs and numeric validation.

Link: [Exam System – User Guide](https://ay2526s2-cs2113-t10-2.github.io/tp/UserGuide.html#exam-system)

### Contributions to the Developer Guide (DG)

**Sections:** Exam Session Flow, Exam Component – Practical Questions, Question Parsing and Loading

Authored comprehensive architectural and implementation documentation (~400 lines): exam module architecture (three entry points, orchestration flow, integration), question types & grading logic, control command design decisions, question parsing pipeline (tokenization, type-specific rules, error handling, design rationale), PRAC questions & setup items (checkpoint system with 5 node types, SetupItem with 3 types, design alternatives), and checkpoint verification examples.

**8 UML Diagrams:** `ExamClassDiagram.puml`, `PracQuestionClassDiagram.puml` (class diagrams); `ExamSessionSequence.puml`, `ExamControlCommandsSequence.puml`, `PracQuestionSequence.puml`, `McqFitbAnswerInteractionSequence.puml` (sequence diagrams); `PracQuestionSetupActivity.puml`, `McqFitbLineParsingActivity.puml` (activity diagrams).

Link: [Exam Session Flow – Developer Guide](https://ay2526s2-cs2113-t10-2.github.io/tp/DeveloperGuide.html#exam-session-flow)

### Review Contributions

- **Code Reviews:** Reviewed pull requests [#152](https://github.com/AY2526S2-CS2113-T10-2/tp/pull/152), [#212](https://github.com/AY2526S2-CS2113-T10-2/tp/pull/212) and [#94](https://github.com/AY2526S2-CS2113-T10-2/tp/pull/94)

### Contributions to Team-based Tasks

- **Infrastructure:** Implemented question parsing framework, exam command handling, VFS factory pattern supporting all exam features
- **Issue Management:** Tracked and prioritized exam module work through GitHub Issues
- **Testing & Bug Fixes:** Performed thorough bug testing, identified edge cases, and suggested fixes to teammates' code
- **General Enhancements:** Project infrastructure enhancements supporting the exam module

### Contributions beyond the Project Team

- **Bugs Reported (PE-D):** Identified and reported 10 bugs in other teams' projects during the Practical Exam D phase: [PE-D Bug Reports](https://github.com/NUS-CS2113-AY2526-S2/ped-puma-31/issues)
