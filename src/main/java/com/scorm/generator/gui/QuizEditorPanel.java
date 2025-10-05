package com.scorm.generator.gui;

import com.scorm.generator.model.Answer;
import com.scorm.generator.model.Question;
import com.scorm.generator.model.Quiz;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Panel for editing quiz questions and answers
 */
public class QuizEditorPanel extends JPanel {
    private Quiz quiz;
    private JTextField titleField;
    private JTextArea descriptionArea;
    private JSpinner passingScoreSpinner;
    private JSpinner maxAttemptsSpinner;
    private DefaultListModel<Question> questionListModel;
    private JList<Question> questionList;
    private QuestionEditPanel questionEditPanel;
    
    public QuizEditorPanel() {
        this.quiz = new Quiz();
        this.quiz.setId(UUID.randomUUID().toString());
        initializeComponents();
        layoutComponents();
        bindEvents();
    }
    
    private void initializeComponents() {
        // Quiz info components
        titleField = new JTextField(20);
        descriptionArea = new JTextArea(3, 20);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        
        passingScoreSpinner = new JSpinner(new SpinnerNumberModel(70, 0, 100, 5));
        maxAttemptsSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
        
        // Question list components
        questionListModel = new DefaultListModel<>();
        questionList = new JList<>(questionListModel);
        questionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        questionList.setCellRenderer(new QuestionListCellRenderer());
        
        // Question edit panel
        questionEditPanel = new QuestionEditPanel();
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Top panel - Quiz info
        JPanel quizInfoPanel = createQuizInfoPanel();
        add(quizInfoPanel, BorderLayout.NORTH);
        
        // Center panel - Questions
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(createQuestionListPanel());
        splitPane.setRightComponent(questionEditPanel);
        splitPane.setDividerLocation(300);
        
        add(splitPane, BorderLayout.CENTER);
    }
    
    private JPanel createQuizInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Thông tin bài kiểm tra"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Title
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Tiêu đề:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(titleField, gbc);
        
        // Description
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Mô tả:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 1.0;
        panel.add(new JScrollPane(descriptionArea), gbc);
        
        // Passing score and max attempts
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0; gbc.weighty = 0;
        panel.add(new JLabel("Điểm đạt (%):"), gbc);
        gbc.gridx = 1;
        panel.add(passingScoreSpinner, gbc);
        
        gbc.gridx = 2;
        panel.add(new JLabel("Số lần thử tối đa:"), gbc);
        gbc.gridx = 3;
        panel.add(maxAttemptsSpinner, gbc);
        
        return panel;
    }
    
    private JPanel createQuestionListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Danh sách câu hỏi"));
        
        // Question list
        JScrollPane scrollPane = new JScrollPane(questionList);
        scrollPane.setPreferredSize(new Dimension(280, 400));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addButton = new JButton("Thêm câu hỏi");
        JButton removeButton = new JButton("Xóa câu hỏi");
        
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Button events
        addButton.addActionListener(e -> addNewQuestion());
        removeButton.addActionListener(e -> removeSelectedQuestion());
        
        return panel;
    }
    
    private void bindEvents() {
        // Update quiz when fields change
        titleField.addActionListener(e -> updateQuizFromFields());
        descriptionArea.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                updateQuizFromFields();
            }
        });
        
        // Question selection
        questionList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Question selected = questionList.getSelectedValue();
                questionEditPanel.setQuestion(selected);
            }
        });
    }
    
    private void addNewQuestion() {
        Question question = new Question();
        question.setId("Q" + (questionListModel.getSize() + 1));
        question.setText("Câu hỏi mới");
        
        // Add default answers
        for (int i = 0; i < 4; i++) {
            Answer answer = new Answer();
            answer.setId("A" + (i + 1));
            answer.setText("Đáp án " + (i + 1));
            answer.setCorrect(i == 0); // First answer is correct by default
            question.addAnswer(answer);
        }
        question.setCorrectAnswerIndex(0);
        
        questionListModel.addElement(question);
        quiz.addQuestion(question);
        
        // Select the new question
        questionList.setSelectedValue(question, true);
    }
    
    private void removeSelectedQuestion() {
        Question selected = questionList.getSelectedValue();
        if (selected != null) {
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc chắn muốn xóa câu hỏi này?",
                "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION
            );
            
            if (confirm == JOptionPane.YES_OPTION) {
                questionListModel.removeElement(selected);
                quiz.getQuestions().remove(selected);
                questionEditPanel.setQuestion(null);
            }
        }
    }
    
    private void updateQuizFromFields() {
        quiz.setTitle(titleField.getText());
        quiz.setDescription(descriptionArea.getText());
        quiz.setPassingScore((Integer) passingScoreSpinner.getValue());
        quiz.setMaxAttempts((Integer) maxAttemptsSpinner.getValue());
    }
    
    public Quiz getQuiz() {
        updateQuizFromFields();
        return quiz;
    }
    
    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
        
        // Update fields
        titleField.setText(quiz.getTitle() != null ? quiz.getTitle() : "");
        descriptionArea.setText(quiz.getDescription() != null ? quiz.getDescription() : "");
        passingScoreSpinner.setValue(quiz.getPassingScore());
        maxAttemptsSpinner.setValue(quiz.getMaxAttempts());
        
        // Update question list
        questionListModel.clear();
        for (Question question : quiz.getQuestions()) {
            questionListModel.addElement(question);
        }
        
        // Clear question edit panel
        questionEditPanel.setQuestion(null);
    }
    
    // Custom cell renderer for question list
    private static class QuestionListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof Question) {
                Question question = (Question) value;
                String text = question.getText();
                if (text.length() > 50) {
                    text = text.substring(0, 47) + "...";
                }
                setText((index + 1) + ". " + text);
            }
            
            return this;
        }
    }
}
