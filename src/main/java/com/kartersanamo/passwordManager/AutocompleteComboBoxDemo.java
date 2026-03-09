package com.kartersanamo.passwordManager;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * Simple test/demo class for the AutocompleteComboBox functionality.
 */
public class AutocompleteComboBoxDemo {

    static void main() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Autocomplete ComboBox Demo");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(500, 300);
            frame.setLocationRelativeTo(null);

            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(10, 10, 10, 10);

            // Title
            JLabel titleLabel = new JLabel("Autocomplete Email/Username Demo");
            titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            panel.add(titleLabel, gbc);

            // Instructions
            JLabel instructionsLabel = new JLabel("<html><i>Type to filter or add new. Try typing a new email!</i></html>");
            gbc.gridy = 1;
            panel.add(instructionsLabel, gbc);

            // Sample email list
            List<String> sampleEmails = Arrays.asList(
                "user@example.com",
                "john.doe@gmail.com",
                "jane.smith@yahoo.com",
                "admin@company.com",
                "test.user@domain.com"
            );

            // Email field
            JLabel emailLabel = new JLabel("Email/Username:");
            gbc.gridy = 2;
            gbc.gridwidth = 1;
            gbc.gridx = 0;
            gbc.weightx = 0;
            panel.add(emailLabel, gbc);

            AutocompleteComboBox emailField = new AutocompleteComboBox(sampleEmails);
            emailField.setPreferredSize(new Dimension(300, 25));
            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(emailField, gbc);

            // Show selected button
            JButton showButton = new JButton("Show Selected Value");
            gbc.gridy = 3;
            gbc.gridx = 0;
            gbc.gridwidth = 2;
            gbc.weightx = 0;
            panel.add(showButton, gbc);

            // Result area
            JTextArea resultArea = new JTextArea(5, 30);
            resultArea.setEditable(false);
            resultArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            JScrollPane scrollPane = new JScrollPane(resultArea);
            gbc.gridy = 4;
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.fill = GridBagConstraints.BOTH;
            panel.add(scrollPane, gbc);

            // Button action
            showButton.addActionListener(_ -> {
                String selected = emailField.getSelectedText();
                List<String> allItems = emailField.getAllItems();

                StringBuilder result = new StringBuilder();
                result.append("Selected Value: ").append(selected).append("\n\n");
                result.append("All Items in List (").append(allItems.size()).append("):\n");
                for (String item : allItems) {
                    result.append("  - ").append(item).append("\n");
                }

                resultArea.setText(result.toString());
            });

            frame.add(panel);
            frame.setVisible(true);

            // Show the initial state
            resultArea.setText("""
                    Features:
                    • Start typing to filter existing emails
                    • Type a new email and you'll see 'Add "your-email"'
                    • Click the 'Add' option or press Enter to add it
                    • The new email is saved for future use
                    
                    Try it out!""");
        });
    }
}

