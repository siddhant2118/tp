package linuxlingo.exam;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import linuxlingo.exam.question.Question;
import linuxlingo.storage.QuestionParser;
import linuxlingo.storage.Storage;
import linuxlingo.storage.StorageException;

/**
 * Loads question bank files, organizes them by topic, and serves questions
 * to {@link ExamSession}.
 *
 * <p><b>Owner: D</b></p>
 *
 * <h3>Data flow</h3>
 * <pre>
 * data/questions/navigation.txt  -->
 * data/questions/permissions.txt --> QuestionParser --> QuestionBank (topic -> List&lt;Question&gt;)
 * data/questions/...             -->
 * </pre>
 *
 * <h3>Topic naming</h3>
 * The topic name is derived from the file name (without {@code .txt}),
 * e.g. {@code navigation.txt} → topic "navigation".
 */
public class QuestionBank {
    private static final Logger LOGGER = Logger.getLogger(QuestionBank.class.getName());
    private final Map<String, List<Question>> topics;

    public QuestionBank() {
        this.topics = new LinkedHashMap<>();
    }

    /**
     * Load all {@code .txt} question bank files from the given directory.
     *
     * @param directory path to the questions directory (e.g. {@code data/questions/})
     */
    public void load(Path directory) {
        Path validatedDirectory = Objects.requireNonNull(directory, "directory must not be null");
        List<Path> files = Storage.listFiles(validatedDirectory, ".txt");
        for (Path file : files) {
            try {
                String topic = QuestionParser.getTopicName(file);
                List<Question> questions = QuestionParser.parseFile(file);
                if (!questions.isEmpty()) {
                    topics.put(topic, new ArrayList<>(questions));
                } else {
                    LOGGER.log(Level.FINE, "Skipping empty question bank file: {0}", file);
                }
            } catch (StorageException e) {
                LOGGER.log(Level.WARNING, "Failed to load question bank file: " + file, e);
            }
        }
        assert topics.values().stream().noneMatch(List::isEmpty) : "Loaded topic lists should not be empty";
    }

    /** Return a sorted list of all loaded topic names. */
    public List<String> getTopics() {
        List<String> sorted = new ArrayList<>(topics.keySet());
        Collections.sort(sorted);
        return sorted;
    }

    /** Internal helper to get the underlying list of questions for a topic. */
    private List<Question> rawTopicQuestions(String topic) {
        return topics.getOrDefault(topic, Collections.emptyList());
    }

    /** Return all questions for a topic, or empty list if unknown. */
    public List<Question> getQuestions(String topic) {
        Objects.requireNonNull(topic, "topic must not be null");
        return new ArrayList<>(rawTopicQuestions(topic));
    }

    /**
     * Return up to {@code count} questions from a topic, optionally shuffled.
     *
     * @param topic  the topic name
     * @param count  maximum number of questions to return
     * @param random whether to shuffle before selecting
     * @return list of questions (may be smaller than count if topic has fewer)
     */
    public List<Question> getQuestions(String topic, int count, boolean random) {
        List<Question> available = new ArrayList<>(getQuestions(topic));
        if (available.isEmpty() || count <= 0) {
            return new ArrayList<>();
        }
        if (random) {
            Collections.shuffle(available);
        }
        int limit = Math.min(count, available.size());
        return new ArrayList<>(available.subList(0, limit));
    }

    /** Return the number of questions in a topic. */
    public int getQuestionCount(String topic) {
        Objects.requireNonNull(topic, "topic must not be null");
        return rawTopicQuestions(topic).size();
    }

    /** Check whether the given topic has been loaded. */
    public boolean hasTopic(String topic) {
        Objects.requireNonNull(topic, "topic must not be null");
        return topics.containsKey(topic);
    }

    /** Internal helper to return a flattened list of all questions across topics. */
    private List<Question> allQuestions() {
        List<Question> all = new ArrayList<>();
        for (List<Question> questions : topics.values()) {
            all.addAll(questions);
        }
        return all;
    }

    /**
     * Return one random question from all topics, or null if none available.
     */
    public Question getRandomQuestion() {
        List<Question> all = allQuestions();
        if (all.isEmpty()) {
            return null;
        }
        return all.get(new Random().nextInt(all.size()));
    }
}
