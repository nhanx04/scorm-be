package com.scorm.generator.model;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents a quiz containing multiple questions
 */
public class Quiz {
    private String id;
    private String title;
    private String description;
    private List<Question> questions;
    private int passingScore;
    private int maxAttempts;

    public Quiz() {
        this.questions = new ArrayList<>();
        this.passingScore = 70; // Default passing score
        this.maxAttempts = 3;   // Default max attempts
    }

    public Quiz(String id, String title) {
        this();
        this.id = id;
        this.title = title;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public void addQuestion(Question question) {
        this.questions.add(question);
    }

    public int getPassingScore() {
        return passingScore;
    }

    public void setPassingScore(int passingScore) {
        this.passingScore = passingScore;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getTotalQuestions() {
        return questions.size();
    }

    @Override
    public String toString() {
        return "Quiz{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", questions=" + questions.size() +
                ", passingScore=" + passingScore +
                '}';
    }
}
