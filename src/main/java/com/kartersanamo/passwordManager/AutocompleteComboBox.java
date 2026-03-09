package com.kartersanamo.passwordManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * An autocomplete combo box that allows users to select from existing entries
 * or add new ones. When typing a value that doesn't exist, it shows an
 * "Add [value]" option at the bottom.
 */
public class AutocompleteComboBox extends JComboBox<String> {

    private final List<String> allItems;
    private boolean isAdjusting = false;

    public AutocompleteComboBox(List<String> items) {
        super();
        this.allItems = new ArrayList<>(items);

        setEditable(true);
        setMaximumRowCount(10);

        // Custom editor setup
        JTextField editor = (JTextField) getEditor().getEditorComponent();

        // Add document listener to filter items as user types
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateList();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateList();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateList();
            }
        });

        // Handle Enter key to add new items
        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleEnterKey();
                }
            }
        });

        // Initialize with all items
        updateComboBoxItems(allItems);
    }

    private void updateList() {
        if (isAdjusting) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            isAdjusting = true;

            JTextField editor = (JTextField) getEditor().getEditorComponent();
            String text = editor.getText();

            if (text.isEmpty()) {
                // Show all items when empty
                updateComboBoxItems(allItems);
            } else {
                // Filter items based on typed text
                List<String> filteredItems = new ArrayList<>();
                String lowerText = text.toLowerCase();

                for (String item : allItems) {
                    if (item.toLowerCase().contains(lowerText)) {
                        filteredItems.add(item);
                    }
                }

                // Check if the typed text exactly matches an existing item
                boolean exactMatch = filteredItems.stream()
                    .anyMatch(item -> item.equalsIgnoreCase(text));

                // Add "Add new" option if text doesn't exactly match
                if (!text.trim().isEmpty() && !exactMatch) {
                    filteredItems.add("Add \"" + text + "\"");
                }

                updateComboBoxItems(filteredItems);
            }

            // Restore the text and caret position
            editor.setText(text);
            editor.setCaretPosition(text.length());

            // Show popup if there are filtered results
            if (getItemCount() > 0) {
                setPopupVisible(true);
            }

            isAdjusting = false;
        });
    }

    private void updateComboBoxItems(List<String> items) {
        removeAllItems();
        for (String item : items) {
            addItem(item);
        }
    }

    private void handleEnterKey() {
        String selected = (String) getSelectedItem();

        if (selected != null && selected.startsWith("Add \"") && selected.endsWith("\"")) {
            // Extract the actual value from 'Add "value"'
            String newValue = selected.substring(5, selected.length() - 1);
            addNewItem(newValue);
        }
    }

    @Override
    public void setSelectedItem(Object item) {
        if (item instanceof String itemStr) {
            // Check if user selected "Add new" option
            if (itemStr.startsWith("Add \"") && itemStr.endsWith("\"")) {
                // Extract the actual value from 'Add "value"'
                String newValue = itemStr.substring(5, itemStr.length() - 1);
                addNewItem(newValue);
                return;
            }
        }

        super.setSelectedItem(item);
    }

    private void addNewItem(String newValue) {
        if (newValue == null || newValue.trim().isEmpty()) {
            return;
        }

        // Add to the main list if not already present
        if (!allItems.contains(newValue)) {
            allItems.add(newValue);
            allItems.sort(String.CASE_INSENSITIVE_ORDER);
        }

        // Update the combo box
        isAdjusting = true;
        updateComboBoxItems(allItems);

        // Select the newly added item
        super.setSelectedItem(newValue);

        // Update the editor text
        JTextField editor = (JTextField) getEditor().getEditorComponent();
        editor.setText(newValue);

        isAdjusting = false;

        // Hide the popup
        setPopupVisible(false);
    }

    /**
     * Get the currently selected or typed text value.
     */
    public String getSelectedText() {
        Object selected = getSelectedItem();
        if (selected != null) {
            String selectedStr = selected.toString();

            // If it's an "Add" option, extract the value
            if (selectedStr.startsWith("Add \"") && selectedStr.endsWith("\"")) {
                return selectedStr.substring(5, selectedStr.length() - 1);
            }

            return selectedStr;
        }

        JTextField editor = (JTextField) getEditor().getEditorComponent();
        return editor.getText();
    }

    /**
     * Set the text value in the editor.
     */
    public void setSelectedText(String text) {
        if (text != null) {
            isAdjusting = true;

            // Check if the item exists in the list
            if (allItems.contains(text)) {
                setSelectedItem(text);
            } else {
                // Just set the editor text without adding to list
                JTextField editor = (JTextField) getEditor().getEditorComponent();
                editor.setText(text);
            }

            isAdjusting = false;
        }
    }

    /**
     * Get all items in the autocomplete list.
     */
    public List<String> getAllItems() {
        return new ArrayList<>(allItems);
    }
}

