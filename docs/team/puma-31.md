# Purav Mahesh – Project Portfolio Page

> CS2113 – LinuxLingo  
> GitHub: [`@puma-31`](https://github.com/puma-31)

---

## Overview

I was the primary developer and maintainer of the **Exam module** in **LinuxLingo**, an educational CLI app that teaches Linux commands through practice and exam-style questions. My work focused on designing and implementing the end-to-end exam experience, from data model and parsing, to exam sessions, grading and feedback, with emphasis on robustness, testability and a smooth CLI workflow.

---

## Summary of Contributions

- **Primary owner of the Exam module**: Designed and implemented the core exam flow, including exam sessions, question handling, grading, and (where applicable) persistence.
- **Improved learning efficacy**: Introduced realistic exam-style practice with timed/structured sessions and detailed feedback, making LinuxLingo more suitable for revision before assessments.
- **Strengthened code quality**: Added tests, refactored parsing and session logic, and contributed UML diagrams for the Developer Guide to clarify the module’s architecture and interactions.
- **Code contributed**: [RepoSense Link](https://nus-cs2113-ay2526-s2.github.io/tp-dashboard/?search=&sort=groupTitle&sortWithin=title&timeframe=commit&mergegroup=&groupSelect=groupByRepos&breakdown=true&checkedFileTypes=docs~functional-code~test-code~other&since=2026-02-20T00%3A00%3A00&filteredFileName=&tabOpen=true&tabType=authorship&tabAuthor=puma-31&tabRepo=AY2526S2-CS2113-T10-2%2Ftp%5Bmaster%5D&authorshipIsMergeGroup=false&authorshipFileTypes=docs~functional-code~test-code~other&authorshipIsBinaryFileTypeChecked=false&authorshipIsIgnoredFilesChecked=false)


---

## Enhancements Implemented (Exam Module)

### 1. Exam Workflow and Commands

- **What it does**: Implements the full **Exam workflow**, allowing users to start an exam session, answer a series of questions, and submit the exam to view results.
- **Key responsibilities**:
  - Designed the domain model for exams (e.g., `ExamSession`, `ExamQuestion`, `ExamResult`, etc.).
  - Implemented exam-related commands (e.g., `exam start`, `exam next`, `exam prev`, `exam submit`, etc. — replace with the exact commands you added).
  - Integrated the Exam module with the question bank and core logic so that exams can draw from the existing pool of Linux questions.
- **Justification**: This feature enables structured, exam-like practice rather than just ad-hoc question attempts, which better mirrors real assessment conditions and helps users gauge their readiness.
- **Highlights**:
  - Required careful state management (e.g., preventing multiple concurrent exams, handling submissions at the correct time).
  - Needed tight integration with the existing CLI parser and question management components.
  - Designed to be extensible so that new exam modes or question types can be added with minimal changes to existing code.

### 2. Exam Question Parsing and Loading

- **What it does**: Handles parsing and loading of **exam questions** from resource files for use in exam sessions.
- **Key responsibilities**:
  - Implemented parsing logic to read exam questions from text/resource files.
  - Performed validation on question data (e.g., checking for missing fields or malformed entries).
  - Produced meaningful error messages when question files are invalid, making it easier for developers to maintain the question bank.
- **Justification**: Reliable parsing ensures the Exam module can scale with additional question sets and reduces runtime errors caused by malformed data.
- **Highlights**:
  - Ensured compatibility with the project’s existing question data format (where applicable).
  - Separated parsing logic from business logic to keep the Exam module maintainable and testable.

### 3. Exam Session Management and State Handling

- **What it does**: Manages the internal **state of an exam session**, including current question index, answered questions, and overall session lifecycle.
- **Key responsibilities**:
  - Implemented the session state machine to enforce allowed transitions (e.g., cannot submit before starting an exam, cannot start a new exam while one is in progress).
  - Defined how navigation commands (e.g., next/previous question) behave and handle edge cases (start/end of question list).
  - Added safeguards against invalid operations and provided clear feedback messages in those cases.
- **Justification**: A robust state model is essential for predictable exam behaviour and reduces user confusion.
- **Highlights**:
  - Required consideration of many edge cases (e.g., repeated submissions, quitting mid-exam, invalid command sequences).
  - Designed so other team members can reason about and extend session behaviour safely.

### 4. Grading, Feedback, and Results Display

- **What it does**: Evaluates exam answers and presents a summary of results and feedback at the end of an exam.
- **Key responsibilities**:
  - Implemented grading logic to compute scores based on user answers and correct solutions.
  - Calculated summary statistics such as total score, percentage, and number of correctly answered questions.
  - Displayed clear, structured feedback, including which questions were answered incorrectly and what the correct answers were.
- **Justification**: Detailed feedback helps users understand their mistakes and target their revision more effectively.
- **Highlights**:
  - Balanced clarity of feedback with conciseness for the CLI environment.
  - Designed result output to be easily testable and consistent with the project’s overall output style.

### 5. Testing and Code Quality for Exam Module

- **What it does**: Ensures the Exam module behaves correctly and remains maintainable.
- **Key responsibilities**:
  - Wrote unit and/or integration tests covering exam session logic, parsing, grading, and command handling.
  - Refactored exam-related classes to reduce duplication and improve readability.
- **Justification**: Strong test coverage protects core exam behaviour from regressions as the project evolves.
- **Highlights**:
  - Achieved meaningful coverage for critical exam paths (e.g., normal use, invalid inputs, edge cases).
  - Improved separation of concerns between parsing, business logic, and presentation.

*(You can adjust class names, command names, and details to exactly match your implementation.)*

---

## Contributions to the User Guide (UG)

I contributed primarily to documenting the **Exam module**:

- Added the **Exam section** 
- Documented edge cases and behaviour:

Links (to be filled in):
- **UG Exam section**: `[Link to User Guide section for Exam features]`

---

## Contributions to the Developer Guide (DG)

I documented the **implementation and design** of the Exam module in the Developer Guide:

- **Exam module design and implementation**

- **Question parsing and data handling**:

### UML Diagrams

I added or updated UML diagrams to clarify Exam module behaviour:

- **Exam Class Diagram** (e.g., `ExamClassDiagram.puml`):
  - Shows the relationships between `ExamSession`, question entities, and supporting classes.
- **Exam Session Sequence Diagram** (e.g., `ExamSessionSequence.puml`):
  - Illustrates the message flow for a typical exam, including `exam start`, answering questions, and `exam submit`.
- **Any related activity/sequence diagrams** (e.g., `QuestionParsingActivity.puml`):
  - Explains how exam questions are parsed and fed into the session.

Links (to be filled in):
- **DG Exam design section**: [Developer Guide section for Exam module](https://ay2526s2-cs2113-t10-2.github.io/tp/DeveloperGuide.html#exam-session-flow)

---

## Contributions to Team-Based Tasks

- Coordinated with teammates to integrate the Exam module with:
  - The main CLI command parser, ensuring exam commands follow the same patterns as existing commands.
  - The question bank so that both practice and exam modes use a consistent source of questions.
- Helped maintain project quality by:
  - Fixing exam-related issues discovered during integration and testing.
  - Ensuring exam features complied with coding standards and passed automated checks.
- Participated in discussions and decisions regarding:
  - How exams should fit into the overall user experience.
  - Naming conventions and error message styles for exam commands.

*(You can add specific examples such as sprint tasks you led or shared chores you handled.)*

---

## Review Contributions

- Reviewed teammates’ pull requests
- **PRs reviewed (non-trivial)**: `[List of PR links you reviewed]`


---

