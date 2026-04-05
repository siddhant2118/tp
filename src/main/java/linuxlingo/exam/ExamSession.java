package linuxlingo.exam;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import linuxlingo.cli.Ui;
import linuxlingo.exam.question.PracQuestion;
import linuxlingo.exam.question.Question;
import linuxlingo.shell.ShellSession;
import linuxlingo.shell.vfs.VirtualFileSystem;

/**
 * Orchestrates an exam session — interactive topic selection, direct-mode,
 * and single-random-question mode.
 *
 * <p><b>Owner: D</b></p>
 *
 * <h3>Three entry points</h3>
 * <ul>
 *   <li>{@link #startInteractive()} — prompt for topic and count, run exam, show results.</li>
 *   <li>{@link #startWithArgs(String, int, boolean)} — exam on a specified topic (CLI args).</li>
 *   <li>{@link #runOneRandom()} — present one random question, then return.</li>
 * </ul>
 *
 * <h3>PRAC question flow</h3>
 * For practical questions, a temporary {@link ShellSession} with a fresh
 * {@link VirtualFileSystem} is created via the {@code vfsFactory}. The user
 * types commands until "done", then the VFS is checked against checkpoints.
 *
 * @see QuestionBank
 * @see ExamResult
 */
public class ExamSession {
    private static final Logger LOGGER = Logger.getLogger(ExamSession.class.getName());
    private final QuestionBank questionBank;
    private final Ui ui;
    private final Supplier<VirtualFileSystem> vfsFactory;

    /**
     * @param bank       the loaded question bank
     * @param ui         the UI for all I/O
     * @param vfsFactory factory to create a fresh VFS for PRAC questions
     *                   (typically {@code VirtualFileSystem::new})
     */
    public ExamSession(QuestionBank bank, Ui ui, Supplier<VirtualFileSystem> vfsFactory) {
        this.questionBank = Objects.requireNonNull(bank, "questionBank must not be null");
        this.ui = Objects.requireNonNull(ui, "ui must not be null");
        this.vfsFactory = Objects.requireNonNull(vfsFactory, "vfsFactory must not be null");
    }

    /**
     * Interactive mode: prompt user for topic, count, run exam, display results.
     */
    public void startInteractive() {
        List<String> topics = questionBank.getTopics();
        if (topics.isEmpty()) {
            ui.println("No question banks available.");
            return;
        }

        listTopics();
        String topicInput = ui.readLine("Select topic (number or name): ");
        if (topicInput == null) {
            LOGGER.fine("Topic selection input was null");
            ui.println("Invalid topic selection.");
            return;
        }

        String selectedTopic = null;
        String trimmedTopicInput = topicInput.trim();
        try {
            int index = Integer.parseInt(trimmedTopicInput);
            if (index >= 1 && index <= topics.size()) {
                selectedTopic = topics.get(index - 1);
            }
        } catch (NumberFormatException e) {
            if (questionBank.hasTopic(trimmedTopicInput)) {
                selectedTopic = trimmedTopicInput;
            }
        }

        if (selectedTopic == null) {
            LOGGER.log(Level.INFO, "Invalid interactive topic selection: {0}", trimmedTopicInput);
            ui.println("Invalid topic selection.");
            return;
        }

        int total = questionBank.getQuestionCount(selectedTopic);
        String countInput = ui.readLine("How many questions? (1-" + total + ", default: all): ");

        int count;
        if (countInput != null && !countInput.trim().isEmpty()) {
            try {
                count = Integer.parseInt(countInput.trim());
            } catch (NumberFormatException e) {
                LOGGER.log(Level.INFO, "Invalid interactive count ''{0}'', defaulting to total", countInput);
                count = total;
            }
        } else {
            count = total;
        }

        count = Math.max(1, Math.min(count, total));
        List<Question> questions = questionBank.getQuestions(selectedTopic, count, false);
        ExamResult result = runExam(questions);
        ui.println("\n" + result.display());
    }

    /**
     * Direct mode: run exam on specified topic with given parameters (from CLI args).
     *
     * @param topic  topic name
     * @param count  max questions (≤0 means all)
     * @param random whether to shuffle
     */
    public void startWithArgs(String topic, int count, boolean random) {
        if (topic == null || topic.trim().isEmpty()) {
            throw new IllegalArgumentException("topic must not be null or blank");
        }
        if (!questionBank.hasTopic(topic)) {
            LOGGER.log(Level.INFO, "Topic not found in startWithArgs: {0}", topic);
            ui.println("Invalid topic selection.");
            listTopics();
            return;
        }
        int total = questionBank.getQuestionCount(topic);
        if (count <= 0) {
            count = total;
        }
        count = Math.min(count, total);
        List<Question> questions = questionBank.getQuestions(topic, count, random);
        ExamResult result = runExam(questions);
        ui.println("\n" + result.display());
    }

    /**
     * Single random question mode: present one question from any topic.
     */
    public void runOneRandom() {
        Question q = questionBank.getRandomQuestion();
        if (q == null) {
            ui.println("No questions available.");
            return;
        }
        presentQuestion(q, 1, 1);
    }

    /**
     * Print all available topics with question counts (numbered list).
     */
    public void listTopics() {
        List<String> topics = questionBank.getTopics();
        if (topics.isEmpty()) {
            ui.println("No topics available.");
            return;
        }
        ui.println("Available topics:");
        for (int i = 0; i < topics.size(); i++) {
            String topic = topics.get(i);
            int count = questionBank.getQuestionCount(topic);
            ui.println("  " + (i + 1) + ". " + topic + " (" + count + " questions)");
        }
    }

    // ─── Private helpers ─────────────────────────────────────────

    /**
     * Run through a list of questions, accumulate results.
     */
    private ExamResult runExam(List<Question> questions) {
        Objects.requireNonNull(questions, "questions must not be null");
        ExamResult result = new ExamResult();
        for (int i = 0; i < questions.size(); i++) {
            Question q = Objects.requireNonNull(questions.get(i), "question must not be null");
            int index = i + 1;
            int total = questions.size();

            if (q instanceof PracQuestion pq) {
                ui.println("[Q" + index + "/" + total + "] " + q.present());
                boolean correct = handlePracQuestion(pq);
                result.addResult(q, "", correct);
            } else {
                presentNonPracQuestion(q, index, total, result);
            }
        }
        return result;
    }

    private void presentNonPracQuestion(Question q, int index, int total, ExamResult result) {
        ui.println("[Q" + index + "/" + total + "] " + q.present());
        String userAnswer = ui.readLine("Your answer: ");
        if (userAnswer == null || userAnswer.trim().equalsIgnoreCase("quit")) {
            LOGGER.log(Level.FINE, "Question skipped by user at index {0}", index);
            result.addResult(q, "", false);
            return;
        }

        boolean correct = q.checkAnswer(userAnswer);
        if (correct) {
            ui.println("✓ Correct!");
        } else {
            ui.println("✗ Incorrect.");
        }
        ui.println("Explanation: " + q.getExplanation());
        result.addResult(q, userAnswer, correct);
    }

    /**
     * Present a single question in single-random-question mode.
     *
     * <p>This is used by {@link #runOneRandom()} and supports both PRAC and
     * non-PRAC questions. For PRAC questions it opens a temporary shell via
     * {@link #handlePracQuestion(PracQuestion)}; for others it performs the
     * same interaction flow as {@link #presentNonPracQuestion(Question, int, int, ExamResult)}
     * but without accumulating results into an {@link ExamResult}.</p>
     *
     * @return {@code true} if the answer/VFS was correct, {@code false} otherwise
     */
    private boolean presentQuestion(Question q, int index, int total) {
        Objects.requireNonNull(q, "question must not be null");
        if (index <= 0 || total <= 0) {
            throw new IllegalArgumentException("index and total must be positive");
        }

        ui.println("[Q" + index + "/" + total + "] " + q.present());

        if (q instanceof PracQuestion pq) {
            return handlePracQuestion(pq);
        }

        String userAnswer = ui.readLine("Your answer: ");
        if (userAnswer == null || userAnswer.trim().equalsIgnoreCase("quit")) {
            LOGGER.log(Level.FINE, "Question skipped by user at index {0}", index);
            return false;
        }

        boolean correct = q.checkAnswer(userAnswer);
        if (correct) {
            ui.println("✓ Correct!");
        } else {
            ui.println("✗ Incorrect.");
        }
        ui.println("Explanation: " + q.getExplanation());
        return correct;
    }

    /**
     * Handle a PRAC question: open a temporary shell, then check VFS.
     *
     * @param q the practical question
     * @return true if VFS matches all checkpoints
     */
    private boolean handlePracQuestion(PracQuestion q) {
        Objects.requireNonNull(q, "prac question must not be null");
        ui.println(">> Entering Shell Simulator...");
        ui.println("   Complete the task and type 'exit' when finished.\n");

        VirtualFileSystem tempVfs = vfsFactory.get();
        if (tempVfs == null) {
            throw new IllegalStateException("vfsFactory returned null VirtualFileSystem");
        }
        ShellSession tempSession = new ShellSession(tempVfs, ui);
        tempSession.start();

        boolean correct = q.checkVfs(tempVfs);
        if (correct) {
            ui.println("✓ Correct!");
        } else {
            ui.println("✗ Incorrect.");
        }
        ui.println("Explanation: " + q.getExplanation());
        return correct;
    }
}
