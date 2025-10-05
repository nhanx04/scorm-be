package com.scorm.generator.model;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents a question with multiple choice answers
 */
public class Question {
    private String id;
    private String text;
    private List<Answer> answers;
    private int correctAnswerIndex;
    private String explanation;

    public Question() {
        this.answers = new ArrayList<>();
    }

    public Question(String id, String text) {
        this.id = id;
        this.text = text;
        this.answers = new ArrayList<>();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<Answer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<Answer> answers) {
        this.answers = answers;
    }

    public void addAnswer(Answer answer) {
        this.answers.add(answer);
    }

    public int getCorrectAnswerIndex() {
        return correctAnswerIndex;
    }

    public void setCorrectAnswerIndex(int correctAnswerIndex) {
        this.correctAnswerIndex = correctAnswerIndex;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Answer getCorrectAnswer() {
        if (correctAnswerIndex >= 0 && correctAnswerIndex < answers.size()) {
            return answers.get(correctAnswerIndex);
        }
        return null;
    }

    @Override
    public String toString() {
        return "Question{" +
                "id='" + id + '\'' +
                ", text='" + text + '\'' +
                ", answers=" + answers.size() +
                ", correctAnswerIndex=" + correctAnswerIndex +
                '}';
    }
}
