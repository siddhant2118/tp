# Purav Mahesh – Project Portfolio Page

> CS2113 – LinuxLingo  
> GitHub: [`@puma-31`](https://github.com/puma-31)

---

## Overview

I was the primary developer and maintainer of the **Exam module** in **LinuxLingo**, an educational CLI app that teaches Linux commands through practice and exam-style questions. My work focused on designing and implementing the end-to-end exam experience, from data model and parsing, to exam sessions, grading and feedback, with emphasis on robustness, testability and a smooth CLI workflow.

---

## Summary of Contributions

- **Exam module**: Designed and implemented the core exam flow, including exam sessions, question handling, grading, and (where applicable) persistence.
- **Improved learning efficacy**: Introduced realistic exam-style practice with timed/structured sessions and detailed feedback, making LinuxLingo more suitable for revision before assessments.
- **Strengthened code quality**: Added tests, refactored parsing and session logic, and contributed UML diagrams for the Developer Guide to clarify the module’s architecture and interactions.
- **Code contributed**: [RepoSense Link](https://nus-cs2113-ay2526-s2.github.io/tp-dashboard/?search=puma-31&breakdown=true)

### Enhancements Implemented (Exam Module)

#### 1. Exam Workflow and Commands

- **What it does**: Implements the full **Exam workflow**, allowing users to start an exam session, answer a series of questions, and submit the exam to view results.
- **Key responsibilities**:
  - Designed the domain model for exams (e.g., `ExamSession`, `ExamQuestion`, `ExamResult`, etc.).
  - Implemented support for exam-related commands (e.g., `exam`, `exam -random`, etc.).
  - Integrated the Exam module with the question bank and core logic so that exams can draw from the existing pool of Linux questions.

#### 2. Exam Question Parsing and Loading

- **What it does**: Handles parsing and loading of **exam questions** from resource files for use in exam sessions.
- **Key responsibilities**:
  - Implemented parsing logic to read exam questions from text/resource files.
  - Performed validation on question data (e.g., checking for missing fields or malformed entries).
  - Produced meaningful error messages when question files are invalid, making it easier for developers to maintain the question bank.
- **Justification**: Reliable parsing ensures the Exam module can scale with additional question sets and reduces runtime errors caused by malformed data.
- **Highlights**:
  - Ensured compatibility with the project’s existing question data format (where applicable).
  - Separated parsing logic from business logic to keep the Exam module maintainable and testable.

#### 3. Exam Session Management and State Handling

- **What it does**: Manages the internal **state of an exam session**, including current question index, answered questions, and overall session lifecycle.
- **Key responsibilities**:
  - Implemented the session state machine to enforce allowed transitions (e.g., cannot submit before starting an exam, cannot start a new exam while one is in progress).
  - Defined how navigation commands (e.g., next/previous question) behave and handle edge cases (start/end of question list).
  - Added safeguards against invalid operations and provided clear feedback messages in those cases.
- **Justification**: A robust state model is essential for predictable exam behaviour and reduces user confusion.
- **Highlights**:
  - Required consideration of many edge cases (e.g., repeated submissions, quitting mid-exam, invalid command sequences).
  - Designed so other team members can reason about and extend session behaviour safely.

#### 4. Grading, Feedback, and Results Display

- **What it does**: Evaluates exam answers and presents a summary of results and feedback at the end of an exam.
- **Key responsibilities**:
  - Implemented grading logic to compute scores based on user answers and correct solutions.
  - Calculated summary statistics such as total score, percentage, and number of correctly answered questions.
  - Displayed clear, structured feedback, including which questions were answered incorrectly and what the correct answers were.
- **Justification**: Detailed feedback helps users understand their mistakes and target their revision more effectively.
- **Highlights**:
  - Balanced clarity of feedback with conciseness for the CLI environment.
  - Designed result output to be easily testable and consistent with the project’s overall output style.

#### 5. Testing and Code Quality for Exam Module

- **What it does**: Ensures the Exam module behaves correctly and remains maintainable.
- **Key responsibilities**:
  - Wrote unit and/or integration tests covering exam session logic, parsing, grading, and command handling.
  - Refactored exam-related classes to reduce duplication and improve readability.
- **Justification**: Strong test coverage protects core exam behaviour from regressions as the project evolves.
- **Highlights**:
  - Achieved meaningful coverage for critical exam paths (e.g., normal use, invalid inputs, edge cases).
  - Improved separation of concerns between parsing, business logic, and presentation.

---

### Contributions to the User Guide (UG)

I contributed primarily to documenting the **Exam module**:

- Added the **Exam section** 
- Documented edge cases and behaviour:

Links (to be filled in):
- **UG Exam section**: `[Link to User Guide section for Exam features]`

---

### Contributions to the Developer Guide (DG)

- Documented the Exam module’s design and implementation (exam entry modes, question types, and PRAC grading flow).
- Improved the **Question Parsing and Loading** section to be more structured (pipeline description, field/type-specific rules, error-handling, and rationale).
- Added/updated UML diagrams for exam behaviour, including:
  - `ExamSessionSequence.puml` (end-to-end exam flow)
  - `ExamControlCommandsSequence.puml` (non-PRAC question flow with `quit` skip and `abort` early termination)

Link:
- DG Exam section: https://ay2526s2-cs2113-t10-2.github.io/tp/DeveloperGuide.html#exam-session-flow

---

### Review Contributions

- Reviewed teammates’ pull requests: [#152](https://github.com/AY2526S2-CS2113-T10-2/tp/pull/152), [#94](https://github.com/AY2526S2-CS2113-T10-2/tp/pull/94),

---

### Contributions to Team-based Tasks

- Performed necessary general enhancements to the project infrastructure to support the Exam module (e.g., question parsing, command handling, session management).
- Performed bug testing and suggested fixes to teammates' code during development
- Maintained issue tracker for the Exam module, ensuring issues were well-defined and tracked to completion.- 

---

### Contributions beyond the project team

- bugs reported in other teams' projects during the PE-D (10 bugs): [link to PE-D repo](https://github.com/NUS-CS2113-AY2526-S2/ped-puma-31/issues)
