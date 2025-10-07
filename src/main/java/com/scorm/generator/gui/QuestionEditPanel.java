package com.scorm.generator.gui;

import com.scorm.generator.model.Answer;
import com.scorm.generator.model.Question;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel for editing individual questions
 */
public class QuestionEditPanel extends JPanel {
    private Question currentQuestion;
    private JTextField questionIdField;
    private JTextArea questionTextArea;
    private JTextArea explanationArea;
    private List<AnswerEditPanel> answerPanels;
    private JPanel answersContainer;
    private ButtonGroup correctAnswerGroup;

    public QuestionEditPanel() {
        this.answerPanels = new ArrayList<>();
        initializeComponents();
        layoutComponents();
    }

    private void initializeComponents() {
        questionIdField = new JTextField(15);
        questionIdField.setEditable(false);

        questionTextArea = new JTextArea(4, 30);
        questionTextArea.setLineWrap(true);
        questionTextArea.setWrapStyleWord(true);

        explanationArea = new JTextArea(3, 30);
        explanationArea.setLineWrap(true);
        explanationArea.setWrapStyleWord(true);

        answersContainer = new JPanel();
        answersContainer.setLayout(new BoxLayout(answersContainer, BoxLayout.Y_AXIS));

        correctAnswerGroup = new ButtonGroup();
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new TitledBorder("Chỉnh sửa câu hỏi"));

        // Top panel - Question info
        JPanel questionInfoPanel = createQuestionInfoPanel();
        add(questionInfoPanel, BorderLayout.NORTH);

        // Center panel - Answers
        JPanel answersPanel = createAnswersPanel();
        add(answersPanel, BorderLayout.CENTER);

        // Bottom panel - Explanation
        JPanel explanationPanel = createExplanationPanel();
        add(explanationPanel, BorderLayout.SOUTH);
    }

    private JPanel createQuestionInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Question ID
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("ID:"), gbc);
        gbc.gridx = 1;
        panel.add(questionIdField, gbc);

        // Question text
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Câu hỏi:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        panel.add(new JScrollPane(questionTextArea), gbc);

        return panel;
    }

    private JPanel createAnswersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Đáp án"));

        JScrollPane scrollPane = new JScrollPane(answersContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(400, 200));

        panel.add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addAnswerBtn = new JButton("Thêm đáp án");
        JButton removeAnswerBtn = new JButton("Xóa đáp án");

        addAnswerBtn.addActionListener(e -> addAnswer());
        removeAnswerBtn.addActionListener(e -> removeLastAnswer());

        buttonPanel.add(addAnswerBtn);
        buttonPanel.add(removeAnswerBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createExplanationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Giải thích (tùy chọn)"));

        JScrollPane scrollPane = new JScrollPane(explanationArea);
        scrollPane.setPreferredSize(new Dimension(400, 80));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    public void setQuestion(Question question) {
        this.currentQuestion = question;

        if (question == null) {
            clearFields();
            return;
        }

        // Update fields
        questionIdField.setText(question.getId());
        questionTextArea.setText(question.getText() != null ? question.getText() : "");
        explanationArea.setText(question.getExplanation() != null ? question.getExplanation() : "");

        // Update answers
        updateAnswerPanels();
    }

    private void clearFields() {
        questionIdField.setText("");
        questionTextArea.setText("");
        explanationArea.setText("");
        answersContainer.removeAll();
        answerPanels.clear();
        correctAnswerGroup = new ButtonGroup();
        revalidate();
        repaint();
    }

    private void updateAnswerPanels() {
        answersContainer.removeAll();
        answerPanels.clear();
        correctAnswerGroup = new ButtonGroup();

        if (currentQuestion != null) {
            for (int i = 0; i < currentQuestion.getAnswers().size(); i++) {
                Answer answer = currentQuestion.getAnswers().get(i);
                AnswerEditPanel answerPanel = new AnswerEditPanel(i, answer, correctAnswerGroup);
                answerPanels.add(answerPanel);
                answersContainer.add(answerPanel);

                if (i == currentQuestion.getCorrectAnswerIndex()) {
                    answerPanel.setCorrect(true);
                }
            }
        }

        revalidate();
        repaint();
    }

    private void addAnswer() {
        if (currentQuestion == null)
            return;

        Answer newAnswer = new Answer();
        newAnswer.setId("A" + (currentQuestion.getAnswers().size() + 1));
        newAnswer.setText("Đáp án mới");
        newAnswer.setCorrect(false);

        currentQuestion.addAnswer(newAnswer);
        updateAnswerPanels();
    }

    private void removeLastAnswer() {
        if (currentQuestion == null || currentQuestion.getAnswers().isEmpty())
            return;

        if (currentQuestion.getAnswers().size() <= 2) {
            JOptionPane.showMessageDialog(this, "Câu hỏi phải có ít nhất 2 đáp án!");
            return;
        }

        int lastIndex = currentQuestion.getAnswers().size() - 1;
        currentQuestion.getAnswers().remove(lastIndex);

        // Adjust correct answer index if needed
        if (currentQuestion.getCorrectAnswerIndex() >= currentQuestion.getAnswers().size()) {
            currentQuestion.setCorrectAnswerIndex(0);
        }

        updateAnswerPanels();
    }

    public void saveChanges() {
        if (currentQuestion == null)
            return;

        currentQuestion.setText(questionTextArea.getText());
        currentQuestion.setExplanation(explanationArea.getText());

        // Update answers and find correct answer
        for (int i = 0; i < answerPanels.size(); i++) {
            AnswerEditPanel panel = answerPanels.get(i);
            Answer answer = currentQuestion.getAnswers().get(i);

            answer.setText(panel.getAnswerText());
            answer.setCorrect(panel.isCorrect());

            if (panel.isCorrect()) {
                currentQuestion.setCorrectAnswerIndex(i);
            }
        }
    }

    // Inner class for editing individual answers
    private class AnswerEditPanel extends JPanel {
        private JRadioButton correctRadio;
        private JTextField answerTextField;
        private int answerIndex;

        public AnswerEditPanel(int index, Answer answer, ButtonGroup group) {
            this.answerIndex = index;

            setLayout(new BorderLayout(5, 5));
            setBorder(BorderFactory.createEtchedBorder());
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

            // Correct answer radio button
            correctRadio = new JRadioButton();
            group.add(correctRadio);
            correctRadio.addActionListener(e -> {
                if (correctRadio.isSelected() && currentQuestion != null) {
                    currentQuestion.setCorrectAnswerIndex(answerIndex);
                    saveChanges();
                }
            });

            // Answer text field
            answerTextField = new JTextField(answer.getText());
            answerTextField.addActionListener(e -> saveChanges());
            answerTextField.addFocusListener(new java.awt.event.FocusAdapter() {
                public void focusLost(java.awt.event.FocusEvent evt) {
                    saveChanges();
                }
            });

            // Layout
            JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            leftPanel.add(new JLabel("Đúng:"));
            leftPanel.add(correctRadio);
            leftPanel.add(new JLabel("Đáp án " + (index + 1) + ":"));

            add(leftPanel, BorderLayout.WEST);
            add(answerTextField, BorderLayout.CENTER);
        }

        public boolean isCorrect() {
            return correctRadio.isSelected();
        }

        public void setCorrect(boolean correct) {
            correctRadio.setSelected(correct);
        }

        public String getAnswerText() {
            return answerTextField.getText();
        }
    }
}
