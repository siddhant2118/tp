# Wang Ziling — Project Portfolio Page

## Project: LinuxLingo

LinuxLingo is a command-line application for learning Linux commands through an interactive shell simulator and a built-in quiz system. It features an in-memory Virtual File System (VFS), a shell parser supporting piping, redirection, and conditional operators, and an exam module with MCQ, fill-in-the-blank, and practical questions. It is written in Java and has about 10 kLoC.

Given below are my contributions to the project.

- **Code contributed:** [RepoSense link](https://nus-cs2113-ay2526-s2.github.io/tp-dashboard/?search=michael-wzl&breakdown=true)

- **Project infrastructure and architecture:**
  - Designed and implemented the entire project scaffold: the entry point (`LinuxLingo`), CLI layer (`Ui`, `MainParser`), full VFS engine (`VirtualFileSystem`, `FileNode`, `Directory`, `RegularFile`, `Permission`, `VfsException`), shell framework (`CommandRegistry`, `CommandResult`, `Command` interface), exam framework (`Question` base class, `Checkpoint`, `ExamResult`), and storage utilities (`Storage`, `StorageException`). This constituted 19 fully-implemented infrastructure files that all team members depended on.
  - Provided 4 reference command implementations (`EchoCommand`, `MkdirCommand`, `PwdCommand`, `TouchCommand`) as coding pattern examples for the team.
  - Authored both the v1.0 and v2.0 Development Guides with detailed per-member task breakdowns, API references, and an ownership matrix, enabling team members to implement their features independently without coordination overhead.

- **New features and enhancements:**
  - Implemented v2.0 shell infrastructure: `||` (OR) and `<` (input redirect) token support in `ShellParser`, alias/history/glob fields in `ShellSession`, JLine integration via `ShellCompleter` and `ShellLineReader`, and "Did you mean?" command suggestions.
  - Implemented v2.0 exam enhancements: new `Checkpoint` types (`NOT_EXISTS`, `CONTENT_EQUALS`, `PERM`), `SetupItem` inner class and `applySetup()` in `PracQuestion`, and enhanced `QuestionParser` for new checkpoint/setup formats.
  - Implemented `DiffCommand` and `TeeCommand`; enhanced multiple existing commands (`EchoCommand -e`, `GrepCommand -l`, `HeadCommand -N` syntax, multi-file support for `WcCommand`/`TailCommand`, etc.).
  - Fixed several bugs across the codebase: `GrepCommand` stdin filtering and `-v` flag, `VfsSerializer` empty-file deserialization, `QuestionParser` escaped-pipe handling, `ShellParser` single-quote and backslash escaping, `ChmodCommand` symbolic mode regex, `CatCommand -n` with piped input, and `DateCommand` format specifier conversion.
  - Set up JaCoCo test coverage reporting in the Gradle build.

- **Testing:**
  - Wrote the majority of the project's test suite, including unit tests for shell components (`ShellSessionTest`, `CdCommandTest`, `LsCommandTest`, `BuiltinCommandTest`), storage/exam modules (`VfsSerializerTest`, `QuestionParserTest`, `QuestionBankTest`, `ExamResultTest`), VFS infrastructure (`DirectoryTest`, `PermissionTest`, `RegularFileTest`), and 29 end-to-end integration tests. Also authored v2.0 infrastructure tests and placeholder tests for team members to enable after implementation.

- **Documentation:**
  - User Guide: Wrote the overall structure/backbone, and the full contents of the Quick Start, FAQ, Known Issues, and Command Summary sections.
  - Developer Guide: Wrote the overall structure/backbone, and the full contents of the Acknowledgements, Setting Up, Architecture (including the Architecture Diagram and Sequence Diagram), VFS Component (including VFS Class Diagram), and VFS Environment Persistence (including Save/Load Sequence Diagram) sections. Also contributed to Product Scope, User Stories, Non-Functional Requirements, Glossary, and Instructions for Manual Testing.

- **Team-based tasks:**
  - Coordinated team meetings and task distribution.
  - Created and managed GitHub issues and milestones for v1.0 and v2.0.
  - Reviewed PRs with substantive comments: #26, #90, #91, #94.
