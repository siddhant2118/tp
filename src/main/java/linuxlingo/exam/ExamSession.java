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

            if (q instanceof PracQuestion pq) {
                boolean correct = handlePracQuestion(pq);
                result.addResult(q, "", correct);
            } else {
                ui.println("[Q" + (i + 1) + "/" + questions.size() + "] " + q.present());
                String userAnswer = ui.readLine("Your answer: ");
                if (userAnswer == null || userAnswer.trim().equalsIgnoreCase("quit")) {
                    LOGGER.log(Level.FINE, "Question skipped by user at index {0}", i + 1);
                    result.addResult(q, "", false);
                } else {
                    boolean correct = q.checkAnswer(userAnswer);
                    if (correct) {
                        ui.println("✓ Correct!");
                    } else {
                        ui.println("✗ Incorrect.");
                    }
                    ui.println("Explanation: " + q.getExplanation());
                    result.addResult(q, userAnswer, correct);
                }
            }
        }
        return result;
    }

    /**
     * Present a single question and collect the user's answer.
     *
     * <p>For MCQ and FITB: prompt "Your answer: ", check with {@code q.checkAnswer()}.</p>
     * <p>For PRAC: open a temporary shell session, then check with
     * {@code ((PracQuestion) q).checkVfs(vfs)}.</p>
     *
     * @param q     the question
     * @param index 1-based question number
     * @param total total questions in this exam
     * @return true if the user answered correctly
     */
    private boolean presentQuestion(Question q, int index, int total) {
        Objects.requireNonNull(q, "question must not be null");
        if (index <= 0 || total <= 0) {
            throw new IllegalArgumentException("index and total must be positive");
        }
        if (q instanceof PracQuestion pq) {
            ui.println("[Q" + index + "/" + total + "] " + q.present());
            return handlePracQuestion(pq);
        }
        return false;
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
        ui.println("   Complete the task and type 'done' when finished.\n");

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
