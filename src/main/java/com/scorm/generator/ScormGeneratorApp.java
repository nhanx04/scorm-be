package com.scorm.generator;

import com.scorm.generator.gui.QuizEditorPanel;
import com.scorm.generator.model.Quiz;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

/**
 * Main application class for SCORM Package Generator
 */
public class ScormGeneratorApp extends JFrame {
    private QuizEditorPanel quizEditorPanel;
    private ScormPackageExporter exporter;
    private JMenuBar menuBar;
    private JToolBar toolBar;

    public ScormGeneratorApp() {
        this.exporter = new ScormPackageExporter();
        initializeComponents();
        setupUI();
        bindEvents();
    }

    private void initializeComponents() {
        quizEditorPanel = new QuizEditorPanel();

        // Create menu bar
        menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem newMenuItem = new JMenuItem("New Quiz");
        JMenuItem openMenuItem = new JMenuItem("Open Quiz");
        JMenuItem saveMenuItem = new JMenuItem("Save Quiz");
        JMenuItem exportMenuItem = new JMenuItem("Export SCORM Package");
        JMenuItem exitMenuItem = new JMenuItem("Exit");

        fileMenu.add(newMenuItem);
        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exportMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutMenuItem = new JMenuItem("About");
        helpMenu.add(aboutMenuItem);

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);

        // Create toolbar
        toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton newButton = new JButton("New");
        JButton saveButton = new JButton("Save");
        JButton exportButton = new JButton("Export SCORM");

        toolBar.add(newButton);
        toolBar.addSeparator();
        toolBar.add(saveButton);
        toolBar.addSeparator();
        toolBar.add(exportButton);

        // Bind menu and toolbar events
        newMenuItem.addActionListener(e -> newQuiz());
        newButton.addActionListener(e -> newQuiz());

        saveMenuItem.addActionListener(e -> saveQuiz());
        saveButton.addActionListener(e -> saveQuiz());

        exportMenuItem.addActionListener(e -> exportScormPackage());
        exportButton.addActionListener(e -> exportScormPackage());

        exitMenuItem.addActionListener(e -> exitApplication());
        aboutMenuItem.addActionListener(e -> showAbout());
    }

    private void setupUI() {
        setTitle("SCORM Package Generator - Tạo gói SCORM 2004");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Use default look and feel
        }

        // Layout
        setLayout(new BorderLayout());
        setJMenuBar(menuBar);
        add(toolBar, BorderLayout.NORTH);
        add(quizEditorPanel, BorderLayout.CENTER);

        // Status bar
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        statusBar.add(new JLabel("Sẵn sàng"));
        add(statusBar, BorderLayout.SOUTH);

        // Window properties
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(800, 600));

        // Set application icon
        try {
            setIconImage(createDefaultIcon());
        } catch (Exception e) {
            // Ignore if icon creation fails
        }
    }

    private void bindEvents() {
        // Window closing event
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                exitApplication();
            }
        });
    }

    private void newQuiz() {
        int result = JOptionPane.showConfirmDialog(
                this,
                "Bạn có muốn tạo bài kiểm tra mới? Dữ liệu hiện tại sẽ bị mất.",
                "Tạo mới",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            Quiz newQuiz = new Quiz();
            newQuiz.setTitle("Bài kiểm tra mới");
            quizEditorPanel.setQuiz(newQuiz);
        }
    }

    private void saveQuiz() {
        // For now, just show a message. In a full implementation,
        // you would serialize the quiz to JSON or XML
        JOptionPane.showMessageDialog(
                this,
                "Chức năng lưu file sẽ được implement trong phiên bản tiếp theo.",
                "Thông báo",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void exportScormPackage() {
        Quiz quiz = quizEditorPanel.getQuiz();

        // Validate quiz
        if (!validateQuiz(quiz)) {
            return;
        }

        // Choose save location
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Lưu SCORM Package");
        fileChooser.setSelectedFile(new File(exporter.generatePackageName(quiz)));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("ZIP files", "zip"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File tempFile = fileChooser.getSelectedFile();

            // Ensure .zip extension
            if (!tempFile.getName().toLowerCase().endsWith(".zip")) {
                tempFile = new File(tempFile.getAbsolutePath() + ".zip");
            }

            final File selectedFile = tempFile;

            // Export in background thread
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    exporter.exportPackage(quiz, selectedFile);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get(); // Check for exceptions
                        JOptionPane.showMessageDialog(
                                ScormGeneratorApp.this,
                                "SCORM Package đã được tạo thành công!\n" +
                                        "File: " + selectedFile.getAbsolutePath(),
                                "Thành công",
                                JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(
                                ScormGeneratorApp.this,
                                "Lỗi khi tạo SCORM Package:\n" + e.getMessage(),
                                "Lỗi",
                                JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    }
                }
            };

            worker.execute();
        }
    }

    private boolean validateQuiz(Quiz quiz) {
        if (quiz.getTitle() == null || quiz.getTitle().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tiêu đề bài kiểm tra.", "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (quiz.getQuestions().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng thêm ít nhất một câu hỏi.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        for (int i = 0; i < quiz.getQuestions().size(); i++) {
            var question = quiz.getQuestions().get(i);
            if (question.getText() == null || question.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Câu hỏi " + (i + 1) + " không có nội dung.", "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }

            if (question.getAnswers().size() < 2) {
                JOptionPane.showMessageDialog(this, "Câu hỏi " + (i + 1) + " phải có ít nhất 2 đáp án.", "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }

            boolean hasCorrectAnswer = false;
            for (var answer : question.getAnswers()) {
                if (answer.getText() == null || answer.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Câu hỏi " + (i + 1) + " có đáp án trống.", "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                if (answer.isCorrect()) {
                    hasCorrectAnswer = true;
                }
            }

            if (!hasCorrectAnswer) {
                JOptionPane.showMessageDialog(this, "Câu hỏi " + (i + 1) + " phải có ít nhất một đáp án đúng.", "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }

        return true;
    }

    private void exitApplication() {
        int result = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc chắn muốn thoát?",
                "Xác nhận thoát",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(
                this,
                "SCORM Package Generator v1.0\n" +
                        "Tạo gói SCORM 2004 từ câu hỏi và đáp án\n\n" +
                        "Phát triển bởi: AI Assistant\n" +
                        "Hỗ trợ: SCORM 2004 4th Edition",
                "Về chương trình",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private Image createDefaultIcon() {
        // Create a simple icon
        int size = 32;
        java.awt.image.BufferedImage icon = new java.awt.image.BufferedImage(size, size,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = icon.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw background
        g2d.setColor(new Color(102, 126, 234));
        g2d.fillRoundRect(2, 2, size - 4, size - 4, 8, 8);

        // Draw "S" letter
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        FontMetrics fm = g2d.getFontMetrics();
        String text = "S";
        int x = (size - fm.stringWidth(text)) / 2;
        int y = (size - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(text, x, y);

        g2d.dispose();
        return icon;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new ScormGeneratorApp().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Lỗi khởi động ứng dụng: " + e.getMessage(),
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
