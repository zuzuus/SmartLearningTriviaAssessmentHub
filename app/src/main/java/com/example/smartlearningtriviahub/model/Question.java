package com.example.smartlearningtriviahub.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * MODEL (MVC): one trivia question downloaded from the Open Trivia DB API.
 * Implements Serializable so the whole question list survives screen rotation.
 */
public class Question implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String questionText;
    private final String category;
    private final String difficulty;
    private final String correctAnswer;
    private final ArrayList<String> options;   // 4 shuffled choices
    private String userAnswer;                 // null until the user picks one

    public Question(String questionText, String category, String difficulty,
                    String correctAnswer, List<String> options) {
        this.questionText = questionText;
        this.category = category;
        this.difficulty = difficulty;
        this.correctAnswer = correctAnswer;
        this.options = new ArrayList<>(options);
    }

    // ---- Getters used by Data Binding in activity_quiz.xml ----
    public String getQuestionText() {
        return questionText;
    }

    public String getCategoryLabel() {
        return category.toUpperCase(Locale.ROOT) + " · " + difficulty.toUpperCase(Locale.ROOT);
    }

    public List<String> getOptions() {
        return options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public String getUserAnswer() {
        return userAnswer;
    }

    public void setUserAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
    }

    public boolean isAnswered() {
        return userAnswer != null;
    }

    public boolean isCorrect() {
        return correctAnswer.equals(userAnswer);
    }
}
