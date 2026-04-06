package linuxlingo.exam;

import java.util.Objects;

import linuxlingo.cli.Ui;
import linuxlingo.exam.question.PracQuestion;
import linuxlingo.exam.question.Question;

/**
 * Handles the single-random-question mode: pick one random question
 * from any topic and present it using collaborators.
 */
class RandomQuestionMode {

    @FunctionalInterface
    interface PracHandler {
        boolean handle(PracQuestion question);
    }

    private final QuestionBank questionBank;
    private final Ui ui;
    private final QuestionInteraction interaction;
    private final PracHandler pracHandler;

    RandomQuestionMode(QuestionBank questionBank, Ui ui,
                       QuestionInteraction interaction, PracHandler pracHandler) {
        this.questionBank = Objects.requireNonNull(questionBank, "questionBank must not be null");
        this.ui = Objects.requireNonNull(ui, "ui must not be null");
        this.interaction = Objects.requireNonNull(interaction, "interaction must not be null");
        this.pracHandler = Objects.requireNonNull(pracHandler, "pracHandler must not be null");
    }

    /**
     * Single random question mode: present one question from any topic.
     */
    void runOneRandom() {
        Question q = questionBank.getRandomQuestion();
        if (q == null) {
            ui.println("No questions available.");
            return;
        }

        if (q instanceof PracQuestion pq) {
            ui.println("[Q1/1] " + q.present());
            pracHandler.handle(pq);
        } else {
            interaction.presentSingleQuestion(q, 1, 1);
        }
    }
}

