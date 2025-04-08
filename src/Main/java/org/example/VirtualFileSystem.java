package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.File;
import java.sql.*;
import java.util.List;

public class VirtualFileSystem extends JFrame {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/VirtualFileSystemDB";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Chirag05@Chirag05@";

    private JTextArea outputArea;
    private JTextField fileNameField;
    private JComboBox<String> fileTypeComboBox;
    private JComboBox<String> fileSelectionComboBox;
    private JTextArea fileContentArea;

    public VirtualFileSystem() {
        setTitle("Virtual File System");
        setSize(700, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new FlowLayout());

        fileNameField = new JTextField(15);
        String[] fileTypes = {"txt", "pdf", "docx", "jpg", "png", "mp3", "mp4"};
        fileTypeComboBox = new JComboBox<>(fileTypes);
        fileSelectionComboBox = new JComboBox<>();

        JButton createFileBtn = new JButton("Create File");
        JButton deleteFileBtn = new JButton("Delete File");
        JButton updateFileBtn = new JButton("Update File");
        JButton viewFilesBtn = new JButton("View Files");

        outputArea = new JTextArea(10, 50);
        outputArea.setEditable(false);

        fileContentArea = new JTextArea(5, 50);

        JLabel dragDropLabel = new JLabel("Drag a file here (shortcut will be saved)");
        dragDropLabel.setPreferredSize(new Dimension(600, 50));
        dragDropLabel.setBorder(BorderFactory.createDashedBorder(Color.gray));

        dragDropLabel.setTransferHandler(new TransferHandler() {
            public boolean canImport(TransferHandler.TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            public boolean importData(TransferHandler.TransferSupport support) {
                if (!canImport(support)) return false;
                try {
                    List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    File droppedFile = files.get(0);
                    fileNameField.setText(droppedFile.getName().split("\\.")[0]);
                    String ext = droppedFile.getName().substring(droppedFile.getName().lastIndexOf(".") + 1);
                    fileTypeComboBox.setSelectedItem(ext);
                    fileContentArea.setText("Shortcut to: " + droppedFile.getAbsolutePath());
                    return true;
                } catch (Exception e) {
                    outputArea.setText("Error during file drop: " + e.getMessage());
                    return false;
                }
            }
        });

        createFileBtn.addActionListener(e -> createFile());
        deleteFileBtn.addActionListener(e -> deleteFile());
        updateFileBtn.addActionListener(e -> updateFile());
        viewFilesBtn.addActionListener(e -> viewFiles());
        fileTypeComboBox.addActionListener(e -> updateFileSelection());

        add(new JLabel("File Name:"));
        add(fileNameField);
        add(new JLabel("File Type:"));
        add(fileTypeComboBox);
        add(createFileBtn);
        add(new JLabel("File Content:"));
        add(new JScrollPane(fileContentArea));
        add(dragDropLabel);
        add(new JLabel("Select File:"));
        add(fileSelectionComboBox);
        add(updateFileBtn);
        add(deleteFileBtn);
        add(viewFilesBtn);
        add(new JScrollPane(outputArea));

        initializeDatabase();
        updateFileSelection();
        setVisible(true);
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS files ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "name VARCHAR(255) NOT NULL, "
                    + "type VARCHAR(50) NOT NULL, "
                    + "content TEXT, "
                    + "filepath VARCHAR(1000), "
                    + "UNIQUE(name, type))");
        } catch (SQLException e) {
            outputArea.setText("Database Error: " + e.getMessage());
        }
    }

    private void createFile() {
        String fileName = fileNameField.getText().trim();
        String fileType = (String) fileTypeComboBox.getSelectedItem();
        String fileContent = fileContentArea.getText();

        if (fileName.isEmpty()) {
            outputArea.setText("Error: File name cannot be empty!");
            return;
        }

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO files (name, type, content, filepath) VALUES (?, ?, ?, ?)");) {
            stmt.setString(1, fileName);
            stmt.setString(2, fileType);
            stmt.setString(3, fileContent);
            stmt.setString(4, fileContent.startsWith("Shortcut to:") ? fileContent.replace("Shortcut to: ", "") : null);
            stmt.executeUpdate();

            outputArea.setText("File '" + fileName + "." + fileType + "' created successfully!");
            fileNameField.setText("");
            fileContentArea.setText("");
            updateFileSelection();
        } catch (SQLException e) {
            outputArea.setText("Error: A file with the same name already exists!");
        }
    }

    private void deleteFile() {
        String fileType = (String) fileTypeComboBox.getSelectedItem();
        String selectedFile = (String) fileSelectionComboBox.getSelectedItem();

        if (selectedFile != null) {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM files WHERE name = ? AND type = ?")) {
                stmt.setString(1, selectedFile);
                stmt.setString(2, fileType);
                stmt.executeUpdate();

                outputArea.setText("File '" + selectedFile + "." + fileType + "' deleted successfully!");
                updateFileSelection();
            } catch (SQLException e) {
                outputArea.setText("Error: " + e.getMessage());
            }
        } else {
            outputArea.setText("Error: No file selected!");
        }
    }

    private void updateFile() {
        String selectedFile = (String) fileSelectionComboBox.getSelectedItem();
        String oldFileType = (String) fileTypeComboBox.getSelectedItem();

        if (selectedFile == null) {
            outputArea.setText("Error: No file selected for update!");
            return;
        }

        String newFileName = JOptionPane.showInputDialog(this, "Enter new file name:", selectedFile);
        if (newFileName == null || newFileName.trim().isEmpty()) {
            outputArea.setText("Update cancelled.");
            return;
        }

        String[] fileTypes = {"txt", "pdf", "docx", "jpg", "png", "mp3", "mp4"};
        String newFileType = (String) JOptionPane.showInputDialog(this, "Select new file type:", "Update File",
                JOptionPane.QUESTION_MESSAGE, null, fileTypes, oldFileType);

        if (newFileType == null) {
            outputArea.setText("Update cancelled.");
            return;
        }

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE files SET name = ?, type = ? WHERE name = ? AND type = ?")) {
            stmt.setString(1, newFileName);
            stmt.setString(2, newFileType);
            stmt.setString(3, selectedFile);
            stmt.setString(4, oldFileType);
            int rowsUpdated = stmt.executeUpdate();

            if (rowsUpdated > 0) {
                outputArea.setText("File updated successfully: " + newFileName + "." + newFileType);
            } else {
                outputArea.setText("Error: File update failed.");
            }
            updateFileSelection();
        } catch (SQLException e) {
            outputArea.setText("Error: " + e.getMessage());
        }
    }

    private void viewFiles() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name, type, content, filepath FROM files")) {
            StringBuilder sb = new StringBuilder("Files in Virtual File System:\n");
            while (rs.next()) {
                sb.append("File: ").append(rs.getString("name")).append(".").append(rs.getString("type")).append("\n");
                String shortcut = rs.getString("filepath");
                if (shortcut != null) {
                    sb.append("  Shortcut to: ").append(shortcut).append("\n");
                }
                String content = rs.getString("content");
                if (content != null && !content.startsWith("Shortcut to:")) {
                    sb.append("  Content: ").append(content).append("\n");
                }
            }
            outputArea.setText(sb.length() > 0 ? sb.toString() : "No files in the system.");
        } catch (SQLException e) {
            outputArea.setText("Error: " + e.getMessage());
        }
    }

    private void updateFileSelection() {
        fileSelectionComboBox.removeAllItems();
        String fileType = (String) fileTypeComboBox.getSelectedItem();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT name FROM files WHERE type = ?")) {
            stmt.setString(1, fileType);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                fileSelectionComboBox.addItem(rs.getString("name"));
            }
        } catch (SQLException e) {
            outputArea.setText("Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new VirtualFileSystem();
    }
}