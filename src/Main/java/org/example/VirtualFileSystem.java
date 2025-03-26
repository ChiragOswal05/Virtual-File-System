package org.example;

import javax.swing.*;  // Importing Swing for GUI components
import java.awt.*;  // Importing AWT for layout management
import java.awt.event.*;  // Importing AWT event handling
import java.sql.*;  // Importing SQL for database operations

public class VirtualFileSystem extends JFrame {
    // Database connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/VirtualFileSystemDB";
    private static final String DB_USER = "root";  // Change to your MySQL username if different
    private static final String DB_PASSWORD = "Chirag05@Chirag05@";  // Change if you have set a password

    // GUI components
    private JTextArea outputArea;  // Displays messages and file details
    private JTextField fileNameField;  // Input field for file name
    private JComboBox<String> fileTypeComboBox;  // Dropdown to select file type
    private JComboBox<String> fileSelectionComboBox;  // Dropdown to select file for deletion

    public VirtualFileSystem() {
        // Setting up the main window
        setTitle("Virtual File System");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new FlowLayout());  // Using FlowLayout for UI components

        // Initializing components
        fileNameField = new JTextField(15);
        String[] fileTypes = {"txt", "pdf", "docx", "jpg", "png", "mp3", "mp4"};  // Supported file types
        fileTypeComboBox = new JComboBox<>(fileTypes);
        fileSelectionComboBox = new JComboBox<>();
        JButton createFileBtn = new JButton("Create File");  // Button to create a file
        JButton deleteFileBtn = new JButton("Delete File");  // Button to delete a file
        JButton viewFilesBtn = new JButton("View Files");  // Button to view all stored files
        outputArea = new JTextArea(15, 50);  // Text area to display output
        outputArea.setEditable(false);  // Making text area read-only

        // Adding event listeners for buttons
        createFileBtn.addActionListener(e -> createFile());
        deleteFileBtn.addActionListener(e -> deleteFile());
        viewFilesBtn.addActionListener(e -> viewFiles());
        fileTypeComboBox.addActionListener(e -> updateFileSelection());

        // Adding components to the window
        add(new JLabel("File Name:"));
        add(fileNameField);
        add(new JLabel("File Type:"));
        add(fileTypeComboBox);
        add(createFileBtn);
        add(new JLabel("Select File to Delete:"));
        add(fileSelectionComboBox);
        add(deleteFileBtn);
        add(viewFilesBtn);
        add(new JScrollPane(outputArea));  // Adding output area with scroll functionality

        initializeDatabase();  // Initialize database if not exists
        updateFileSelection();  // Update the dropdown for file selection

        setVisible(true);  // Make the window visible
    }

    // Method to establish a database connection
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    // Method to initialize database table
    private void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            // Creating table if it does not exist
            stmt.execute("CREATE TABLE IF NOT EXISTS files ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "name VARCHAR(255) NOT NULL, "
                    + "type VARCHAR(50) NOT NULL, "
                    + "UNIQUE(name, type))");  // Prevents duplicate file names within the same type
        } catch (SQLException e) {
            outputArea.setText("Database Error: " + e.getMessage());
        }
    }

    // Method to create a new file entry in the database
    private void createFile() {
        String fileName = fileNameField.getText().trim();  // Get file name from input
        String fileType = (String) fileTypeComboBox.getSelectedItem();  // Get selected file type

        if (fileName.isEmpty() || fileType.isEmpty()) {
            outputArea.setText("Error: File name and type cannot be empty!");
            return;
        }

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO files (name, type) VALUES (?, ?)")) {
            stmt.setString(1, fileName);
            stmt.setString(2, fileType);
            stmt.executeUpdate();  // Execute the insert query

            outputArea.setText("File '" + fileName + "." + fileType + "' created successfully!");
            fileNameField.setText("");  // Reset file name field
            updateFileSelection();  // Refresh the file selection dropdown
        } catch (SQLException e) {
            outputArea.setText("Error: A file with the same name and type already exists!");
        }
    }

    // Method to delete a selected file from the database
    private void deleteFile() {
        String fileType = (String) fileTypeComboBox.getSelectedItem();  // Get selected file type
        String selectedFile = (String) fileSelectionComboBox.getSelectedItem();  // Get selected file name

        if (selectedFile != null) {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM files WHERE name = ? AND type = ?")) {
                stmt.setString(1, selectedFile);
                stmt.setString(2, fileType);
                stmt.executeUpdate();  // Execute delete query

                outputArea.setText("File '" + selectedFile + "." + fileType + "' deleted successfully!");
                updateFileSelection();  // Refresh file selection dropdown
            } catch (SQLException e) {
                outputArea.setText("Error: " + e.getMessage());
            }
        } else {
            outputArea.setText("Error: No file selected!");
        }
    }

    // Method to display all stored files in the output area
    private void viewFiles() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name, type FROM files")) {
            StringBuilder sb = new StringBuilder("Files in Virtual File System:\n");
            while (rs.next()) {
                sb.append("File: ").append(rs.getString("name")).append(".").append(rs.getString("type")).append("\n");
            }
            outputArea.setText(sb.length() > 0 ? sb.toString() : "No files in the system.");
        } catch (SQLException e) {
            outputArea.setText("Error: " + e.getMessage());
        }
    }

    // Method to update the file selection dropdown when a new file is created or deleted
    private void updateFileSelection() {
        fileSelectionComboBox.removeAllItems();  // Clear existing items
        String fileType = (String) fileTypeComboBox.getSelectedItem();  // Get selected file type

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT name FROM files WHERE type = ?")) {
            stmt.setString(1, fileType);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                fileSelectionComboBox.addItem(rs.getString("name"));  // Add file names to dropdown
            }
        } catch (SQLException e) {
            outputArea.setText("Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new VirtualFileSystem();  // Run the application
    }
}
