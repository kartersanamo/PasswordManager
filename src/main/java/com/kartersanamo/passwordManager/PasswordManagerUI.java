package com.kartersanamo.passwordManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;

public class PasswordManagerUI extends JFrame {

    private final DefaultTableModel tableModel;
    private final JTable passwordTable;
    private final PasswordDatabase database;
    private final TableRowSorter<DefaultTableModel> sorter;
    private List<PasswordDatabase.PasswordEntry> allPasswords;

    public PasswordManagerUI() {
        super("Password Manager");
        this.database = new PasswordDatabase();
        this.allPasswords = new ArrayList<>();

        // Set up the frame
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Add window listener to close database connection
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                database.close();
            }
        });

        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create title panel
        JPanel titlePanel = new JPanel();
        JLabel titleLabel = new JLabel("Password Manager");
        titleLabel.setFont(new Font("Roboto", Font.BOLD, 24));
        titlePanel.add(titleLabel);
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // Create search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JLabel searchLabel = new JLabel("Search:");
        JTextField searchField = new JTextField(30);
        searchField.setToolTipText("Search across all fields");

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { search(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { search(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { search(); }

            private void search() {
                String text = searchField.getText();
                if (text.trim().isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
                }
            }
        });

        JButton clearSearchButton = new JButton("Clear");
        clearSearchButton.addActionListener(_ -> searchField.setText(""));

        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(clearSearchButton);

        // Create table
        String[] columnNames = {"Website/Service", "Username/Email", "Password", "Notes"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        passwordTable = new JTable(tableModel);
        passwordTable.setRowHeight(30);
        passwordTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Add sorter for search functionality
        sorter = new TableRowSorter<>(tableModel);
        passwordTable.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(passwordTable);

        // Create center panel with search and table
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.add(searchPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton addButton = new JButton("Add Password");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        JButton viewButton = new JButton("View Password");
        JButton copyButton = new JButton("Copy Password");

        addButton.addActionListener(_ -> showAddPasswordDialog());
        editButton.addActionListener(_ -> editSelectedPassword());
        deleteButton.addActionListener(_ -> deleteSelectedPassword());
        viewButton.addActionListener(_ -> viewSelectedPassword());
        copyButton.addActionListener(_ -> copyPasswordToClipboard());

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(viewButton);
        buttonPanel.add(copyButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Load existing passwords from database
        loadPasswords();
    }

    private void loadPasswords() {
        // Clear existing rows
        tableModel.setRowCount(0);

        // Load from database
        allPasswords = database.getAllPasswords();
        for (PasswordDatabase.PasswordEntry entry : allPasswords) {
            tableModel.addRow(new Object[]{
                entry.getService(),
                entry.getUsername(),
                "••••••••",
                entry.getNotes()
            });
        }
    }

    private void addPassword(String service, String username, String password, String notes) {
        if (database.addPassword(service, username, password, notes)) {
            loadPasswords(); // Reload table
        } else {
            JOptionPane.showMessageDialog(this,
                "Failed to add password to database.",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createPasswordFieldWithToggle(JPasswordField passwordField) {
        JPanel panel = new JPanel(new BorderLayout(2, 0));
        panel.add(passwordField, BorderLayout.CENTER);

        JButton toggleButton = new JButton("👁");
        toggleButton.setPreferredSize(new Dimension(40, passwordField.getPreferredSize().height));
        toggleButton.setFocusable(false);
        toggleButton.setToolTipText("Show/Hide password");

        final boolean[] isVisible = {false};
        toggleButton.addActionListener(_ -> {
            isVisible[0] = !isVisible[0];
            if (isVisible[0]) {
                passwordField.setEchoChar((char) 0);
                toggleButton.setText("🙈");
            } else {
                passwordField.setEchoChar('•');
                toggleButton.setText("👁");
            }
        });

        panel.add(toggleButton, BorderLayout.EAST);
        return panel;
    }

    private void showAddPasswordDialog() {
        JDialog dialog = new JDialog(this, "Add New Password", true);
        dialog.setSize(500, 350);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField serviceField = new JTextField(20);
        JTextField usernameField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);
        JPanel passwordPanel = createPasswordFieldWithToggle(passwordField);
        JTextField notesField = new JTextField(20);

        JButton generateButton = new JButton("Generate");
        generateButton.setToolTipText("Generate a strong password");
        generateButton.addActionListener(_ -> {
            String generated = showPasswordGeneratorDialog(dialog);
            if (generated != null) {
                passwordField.setText(generated);
            }
        });

        // Row 0: Service
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; gbc.gridwidth = 1;
        panel.add(new JLabel("Website/Service:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        panel.add(serviceField, gbc);

        // Row 1: Username
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; gbc.gridwidth = 1;
        panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        panel.add(usernameField, gbc);

        // Row 2: Password with eye toggle and Generate button
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; gbc.gridwidth = 1;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 1;
        panel.add(passwordPanel, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(generateButton, gbc);

        // Row 3: Notes
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0; gbc.gridwidth = 1;
        panel.add(new JLabel("Notes:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        panel.add(notesField, gbc);

        // Row 4: Buttons
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(_ -> {
            String service = serviceField.getText().trim();
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String notes = notesField.getText().trim();

            if (!service.isEmpty() && !username.isEmpty() && !password.isEmpty()) {
                addPassword(service, username, password, notes);
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog,
                    "Please fill in service, username, and password fields.",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
            }
        });

        cancelButton.addActionListener(_ -> dialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(buttonPanel, gbc);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void editSelectedPassword() {
        int selectedRow = passwordTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a password to edit.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Convert view row to model row (for filtered table)
        int modelRow = passwordTable.convertRowIndexToModel(selectedRow);
        PasswordDatabase.PasswordEntry entry = allPasswords.get(modelRow);

        JDialog dialog = new JDialog(this, "Edit Password", true);
        dialog.setSize(500, 350);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField serviceField = new JTextField(entry.getService(), 20);
        JTextField usernameField = new JTextField(entry.getUsername(), 20);
        JPasswordField passwordField = new JPasswordField(entry.getPassword(), 20);
        JPanel passwordPanel = createPasswordFieldWithToggle(passwordField);
        JTextField notesField = new JTextField(entry.getNotes(), 20);

        JButton generateButton = new JButton("Generate");
        generateButton.setToolTipText("Generate a strong password");
        generateButton.addActionListener(_ -> {
            String generated = showPasswordGeneratorDialog(dialog);
            if (generated != null) {
                passwordField.setText(generated);
            }
        });

        // Row 0: Service
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; gbc.gridwidth = 1;
        panel.add(new JLabel("Website/Service:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        panel.add(serviceField, gbc);

        // Row 1: Username
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; gbc.gridwidth = 1;
        panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        panel.add(usernameField, gbc);

        // Row 2: Password with eye toggle and Generate button
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; gbc.gridwidth = 1;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 1;
        panel.add(passwordPanel, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(generateButton, gbc);

        // Row 3: Notes
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0; gbc.gridwidth = 1;
        panel.add(new JLabel("Notes:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        panel.add(notesField, gbc);

        // Row 4: Buttons
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(_ -> {
            String service = serviceField.getText().trim();
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String notes = notesField.getText().trim();

            if (database.updatePassword(entry.getId(), service, username, password, notes)) {
                loadPasswords(); // Reload table
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog,
                    "Failed to update password in database.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(_ -> dialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(buttonPanel, gbc);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void deleteSelectedPassword() {
        int selectedRow = passwordTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a password to delete.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete this password entry?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            // Convert view row to model row (for filtered table)
            int modelRow = passwordTable.convertRowIndexToModel(selectedRow);
            PasswordDatabase.PasswordEntry entry = allPasswords.get(modelRow);

            if (database.deletePassword(entry.getId())) {
                loadPasswords(); // Reload table
            } else {
                JOptionPane.showMessageDialog(this,
                    "Failed to delete password from database.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void viewSelectedPassword() {
        int selectedRow = passwordTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a password to view.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Convert view row to model row (for filtered table)
        int modelRow = passwordTable.convertRowIndexToModel(selectedRow);
        PasswordDatabase.PasswordEntry entry = allPasswords.get(modelRow);

        JOptionPane.showMessageDialog(this,
            "Service: " + entry.getService() + "\n" +
            "Username: " + entry.getUsername() + "\n" +
            "Password: " + entry.getPassword() + "\n" +
            "Notes: " + entry.getNotes(),
            "Password Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void copyPasswordToClipboard() {
        int selectedRow = passwordTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a password to copy.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Convert view row to model row (for filtered table)
        int modelRow = passwordTable.convertRowIndexToModel(selectedRow);
        PasswordDatabase.PasswordEntry entry = allPasswords.get(modelRow);

        java.awt.datatransfer.StringSelection stringSelection =
            new java.awt.datatransfer.StringSelection(entry.getPassword());
        java.awt.datatransfer.Clipboard clipboard =
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);

        JOptionPane.showMessageDialog(this, "Password copied to clipboard!",
            "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private String showPasswordGeneratorDialog(JDialog parent) {
        JDialog dialog = new JDialog(parent, "Password Generator", true);
        dialog.setSize(500, 550);
        dialog.setLocationRelativeTo(parent);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Preview panel at top
        JPanel previewPanel = new JPanel(new BorderLayout(5, 5));
        previewPanel.setBorder(BorderFactory.createTitledBorder("Generated Password"));

        JTextField previewField = new JTextField();
        previewField.setFont(new Font("Monospaced", Font.BOLD, 14));
        previewField.setEditable(false);
        previewField.setHorizontalAlignment(JTextField.CENTER);

        JButton regenerateButton = new JButton("🔄 Regenerate");

        previewPanel.add(previewField, BorderLayout.CENTER);
        previewPanel.add(regenerateButton, BorderLayout.EAST);

        mainPanel.add(previewPanel, BorderLayout.NORTH);

        // Mode selection
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        modePanel.setBorder(BorderFactory.createTitledBorder("Password Type"));
        ButtonGroup modeGroup = new ButtonGroup();
        JRadioButton charModeRadio = new JRadioButton("Character-based", true);
        JRadioButton wordModeRadio = new JRadioButton("Word-based");
        modeGroup.add(charModeRadio);
        modeGroup.add(wordModeRadio);
        modePanel.add(charModeRadio);
        modePanel.add(wordModeRadio);

        // Options panel
        JPanel optionsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Character-based options
        JLabel lengthLabel = new JLabel("Password Length: 16");
        JSlider lengthSlider = new JSlider(4, 64, 16);
        lengthSlider.setMajorTickSpacing(10);
        lengthSlider.setMinorTickSpacing(2);
        lengthSlider.setPaintTicks(true);
        lengthSlider.setPaintLabels(true);

        JCheckBox uppercaseCheck = new JCheckBox("Uppercase (A-Z)", true);
        JCheckBox lowercaseCheck = new JCheckBox("Lowercase (a-z)", true);
        JCheckBox digitsCheck = new JCheckBox("Digits (0-9)", true);
        JCheckBox specialCheck = new JCheckBox("Special (!@#$%^&*...)", true);

        // Word-based options
        JLabel wordCountLabel = new JLabel("Number of Words: 4");
        JSlider wordCountSlider = new JSlider(2, 8, 4);
        wordCountSlider.setMajorTickSpacing(2);
        wordCountSlider.setMinorTickSpacing(1);
        wordCountSlider.setPaintTicks(true);
        wordCountSlider.setPaintLabels(true);
        wordCountSlider.setEnabled(false);

        JLabel separatorLabel = new JLabel("Separator:");
        JComboBox<String> separatorCombo = new JComboBox<>(new String[]{"-", "_", ".", " ", ""});
        separatorCombo.setEnabled(false);

        JCheckBox wordCapitalCheck = new JCheckBox("Capitalize words", true);
        wordCapitalCheck.setEnabled(false);
        JCheckBox wordDigitsCheck = new JCheckBox("Add numbers", true);
        wordDigitsCheck.setEnabled(false);

        // Add to options panel
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        optionsPanel.add(lengthLabel, gbc);
        gbc.gridy = 1;
        optionsPanel.add(lengthSlider, gbc);
        gbc.gridy = 2; gbc.gridwidth = 1;
        optionsPanel.add(uppercaseCheck, gbc);
        gbc.gridy = 3;
        optionsPanel.add(lowercaseCheck, gbc);
        gbc.gridy = 4;
        optionsPanel.add(digitsCheck, gbc);
        gbc.gridy = 5;
        optionsPanel.add(specialCheck, gbc);

        gbc.gridy = 6; gbc.gridwidth = 2;
        optionsPanel.add(new JSeparator(), gbc);

        gbc.gridy = 7;
        optionsPanel.add(wordCountLabel, gbc);
        gbc.gridy = 8;
        optionsPanel.add(wordCountSlider, gbc);
        gbc.gridy = 9; gbc.gridwidth = 1;
        optionsPanel.add(separatorLabel, gbc);
        gbc.gridx = 1;
        optionsPanel.add(separatorCombo, gbc);
        gbc.gridx = 0; gbc.gridy = 10;
        optionsPanel.add(wordCapitalCheck, gbc);
        gbc.gridy = 11;
        optionsPanel.add(wordDigitsCheck, gbc);

        // Center panel with mode and options
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.add(modePanel, BorderLayout.NORTH);
        centerPanel.add(optionsPanel, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Quick presets
        JPanel presetsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        presetsPanel.setBorder(BorderFactory.createTitledBorder("Quick Presets"));

        JButton strongButton = new JButton("Strong (20)");
        JButton defaultButton = new JButton("Default (16)");
        JButton simpleButton = new JButton("Simple (12)");
        JButton pinButton = new JButton("PIN (6)");
        JButton wordsButton = new JButton("Words (4)");

        presetsPanel.add(strongButton);
        presetsPanel.add(defaultButton);
        presetsPanel.add(simpleButton);
        presetsPanel.add(pinButton);
        presetsPanel.add(wordsButton);

        mainPanel.add(presetsPanel, BorderLayout.SOUTH);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton useButton = new JButton("Use This Password");
        JButton cancelButton = new JButton("Cancel");

        final String[] result = {null};

        // Auto-regenerate function
        Runnable autoGenerate = () -> {
            PasswordGenerator.PasswordOptions options = new PasswordGenerator.PasswordOptions();

            if (wordModeRadio.isSelected()) {
                options.setUseWords(true)
                    .setWordCount(wordCountSlider.getValue())
                    .setWordSeparator((String) separatorCombo.getSelectedItem())
                    .setUseUppercase(wordCapitalCheck.isSelected())
                    .setUseDigits(wordDigitsCheck.isSelected());
            } else {
                options.setLength(lengthSlider.getValue())
                    .setUseUppercase(uppercaseCheck.isSelected())
                    .setUseLowercase(lowercaseCheck.isSelected())
                    .setUseDigits(digitsCheck.isSelected())
                    .setUseSpecial(specialCheck.isSelected());
            }

            String password = PasswordGenerator.generate(options);
            previewField.setText(password);
        };

        // Mode switching
        charModeRadio.addActionListener(_ -> {
            lengthLabel.setEnabled(true);
            lengthSlider.setEnabled(true);
            uppercaseCheck.setEnabled(true);
            lowercaseCheck.setEnabled(true);
            digitsCheck.setEnabled(true);
            specialCheck.setEnabled(true);

            wordCountLabel.setEnabled(false);
            wordCountSlider.setEnabled(false);
            separatorLabel.setEnabled(false);
            separatorCombo.setEnabled(false);
            wordCapitalCheck.setEnabled(false);
            wordDigitsCheck.setEnabled(false);

            autoGenerate.run();
        });

        wordModeRadio.addActionListener(_ -> {
            lengthLabel.setEnabled(false);
            lengthSlider.setEnabled(false);
            uppercaseCheck.setEnabled(false);
            lowercaseCheck.setEnabled(false);
            digitsCheck.setEnabled(false);
            specialCheck.setEnabled(false);

            wordCountLabel.setEnabled(true);
            wordCountSlider.setEnabled(true);
            separatorLabel.setEnabled(true);
            separatorCombo.setEnabled(true);
            wordCapitalCheck.setEnabled(true);
            wordDigitsCheck.setEnabled(true);

            autoGenerate.run();
        });

        // Live preview updates
        lengthSlider.addChangeListener(_ -> {
            lengthLabel.setText("Password Length: " + lengthSlider.getValue());
            if (!lengthSlider.getValueIsAdjusting()) {
                autoGenerate.run();
            }
        });

        wordCountSlider.addChangeListener(_ -> {
            wordCountLabel.setText("Number of Words: " + wordCountSlider.getValue());
            if (!wordCountSlider.getValueIsAdjusting()) {
                autoGenerate.run();
            }
        });

        uppercaseCheck.addActionListener(_ -> autoGenerate.run());
        lowercaseCheck.addActionListener(_ -> autoGenerate.run());
        digitsCheck.addActionListener(_ -> autoGenerate.run());
        specialCheck.addActionListener(_ -> autoGenerate.run());
        separatorCombo.addActionListener(_ -> autoGenerate.run());
        wordCapitalCheck.addActionListener(_ -> autoGenerate.run());
        wordDigitsCheck.addActionListener(_ -> autoGenerate.run());

        regenerateButton.addActionListener(_ -> autoGenerate.run());

        // Presets
        strongButton.addActionListener(_ -> {
            charModeRadio.setSelected(true);
            charModeRadio.getActionListeners()[0].actionPerformed(null);
            lengthSlider.setValue(20);
            uppercaseCheck.setSelected(true);
            lowercaseCheck.setSelected(true);
            digitsCheck.setSelected(true);
            specialCheck.setSelected(true);
            autoGenerate.run();
        });

        defaultButton.addActionListener(_ -> {
            charModeRadio.setSelected(true);
            charModeRadio.getActionListeners()[0].actionPerformed(null);
            lengthSlider.setValue(16);
            autoGenerate.run();
        });

        simpleButton.addActionListener(_ -> {
            charModeRadio.setSelected(true);
            charModeRadio.getActionListeners()[0].actionPerformed(null);
            lengthSlider.setValue(12);
            specialCheck.setSelected(false);
            autoGenerate.run();
        });

        pinButton.addActionListener(_ -> {
            charModeRadio.setSelected(true);
            charModeRadio.getActionListeners()[0].actionPerformed(null);
            lengthSlider.setValue(6);
            uppercaseCheck.setSelected(false);
            lowercaseCheck.setSelected(false);
            specialCheck.setSelected(false);
            digitsCheck.setSelected(true);
            autoGenerate.run();
        });

        wordsButton.addActionListener(_ -> {
            wordModeRadio.setSelected(true);
            wordModeRadio.getActionListeners()[0].actionPerformed(null);
            wordCountSlider.setValue(4);
            separatorCombo.setSelectedItem("-");
            autoGenerate.run();
        });

        useButton.addActionListener(_ -> {
            result[0] = previewField.getText();
            dialog.dispose();
        });

        cancelButton.addActionListener(_ -> dialog.dispose());

        buttonPanel.add(useButton);
        buttonPanel.add(cancelButton);

        // Main layout
        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.add(mainPanel, BorderLayout.CENTER);
        containerPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(containerPanel);

        // Generate initial password
        autoGenerate.run();

        dialog.setVisible(true);

        return result[0];
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            PasswordManagerUI pm = new PasswordManagerUI();
            pm.setVisible(true);
        });
    }
}

