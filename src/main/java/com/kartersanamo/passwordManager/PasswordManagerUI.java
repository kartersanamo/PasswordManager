package com.kartersanamo.passwordManager;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.net.URI;
import java.util.List;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PasswordManagerUI extends JFrame {

    // Modern Dark Theme Colors
    private static final Color BACKGROUND_DARK = new Color(18, 18, 18);
    private static final Color SURFACE_DARK = new Color(30, 30, 30);
    private static final Color SURFACE_LIGHTER = new Color(45, 45, 45);
    private static final Color SURFACE_HOVER = new Color(55, 55, 55);
    private static final Color PRIMARY_COLOR = new Color(99, 102, 241); // Indigo
    private static final Color ACCENT_COLOR = new Color(139, 92, 246); // Purple
    private static final Color SUCCESS_COLOR = new Color(34, 197, 94);
    private static final Color DANGER_COLOR = new Color(239, 68, 68);
    private static final Color TEXT_PRIMARY = new Color(240, 240, 240);
    private static final Color TEXT_SECONDARY = new Color(160, 160, 160);
    private static final Color BORDER_COLOR = new Color(60, 60, 60);

    private static final String HOME_PAGE_ID = "home";

    private final DefaultTableModel tableModel;
    private final JTable passwordTable;
    private final PasswordDatabase database;
    private final TableRowSorter<DefaultTableModel> sorter;
    private List<PasswordDatabase.PasswordEntry> allPasswords;
    private final java.util.Set<Integer> revealedPasswordRows = new java.util.HashSet<>();

    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pageContainer = new JPanel(pageLayout);
    private final Deque<String> pageStack = new ArrayDeque<>();
    private final Map<String, JPanel> dynamicPages = new HashMap<>();
    private int pageCounter = 0;

    public PasswordManagerUI() {
        this(null); // Default constructor for backwards compatibility
    }
    
    public PasswordManagerUI(String secretCode) {
        super("Password Manager");
        this.database = new PasswordDatabase();
        this.allPasswords = new ArrayList<>();

        if (!database.isConnected()) {
            SwingUtilities.invokeLater(this::showDatabaseConnectionHelpDialog);
        }
        
        // Check if this is the first time or if secret code has changed
        if (secretCode != null) {
            handleSecretCodeValidation(secretCode);
        }

        // Check for corrupted passwords from an old encryption scheme
        int corruptedCount = database.checkForCorruptedPasswords(false);
        if (corruptedCount > 0) {
            System.err.println("WARNING: Found " + corruptedCount + " password(s) that cannot be decrypted.");
            System.err.println("This likely happened due to an encryption key change.");
            
            // Ask user if they want to remove corrupted entries
            SwingUtilities.invokeLater(() -> {
                int choice = JOptionPane.showConfirmDialog(
                    null,
                    "Found " + corruptedCount + " password entries that cannot be decrypted.\n" +
                    "This can happen after encryption updates.\n\n" +
                    "Would you like to remove these corrupted entries?\n" +
                    "(You'll need to re-add those passwords manually)",
                    "Corrupted Password Entries Detected",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                
                if (choice == JOptionPane.YES_OPTION) {
                    database.checkForCorruptedPasswords(true);
                    loadPasswords(); // Reload the table
                    JOptionPane.showMessageDialog(
                        this,
                        "Corrupted entries have been removed.\nPlease re-add those passwords.",
                        "Cleanup Complete",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                }
            });
        }

        // Set up the frame with modern look
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND_DARK);

        // Add window listener to close database connection
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                database.close();
            }
        });

        // Create main panel with dark theme
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(BACKGROUND_DARK);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Create modern title panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(BACKGROUND_DARK);
        titlePanel.setBorder(new EmptyBorder(0, 0, 15, 0));
        
        JLabel titleLabel = new JLabel("Password Manager");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(TEXT_PRIMARY);
        
        JLabel subtitleLabel = new JLabel("Karter Sanamo");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(TEXT_SECONDARY);
        
        JPanel titleTextPanel = new JPanel(new BorderLayout(0, 5));
        titleTextPanel.setBackground(BACKGROUND_DARK);
        titleTextPanel.add(titleLabel, BorderLayout.NORTH);
        titleTextPanel.add(subtitleLabel, BorderLayout.CENTER);
        
        titlePanel.add(titleTextPanel, BorderLayout.WEST);
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // Create modern search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        searchPanel.setBackground(SURFACE_DARK);
        searchPanel.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(12, BORDER_COLOR),
            new EmptyBorder(8, 12, 8, 12)
        ));
        
        JTextField searchField = new JTextField(35);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setBackground(SURFACE_LIGHTER);
        searchField.setForeground(TEXT_PRIMARY);
        searchField.setCaretColor(TEXT_PRIMARY);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(8, BORDER_COLOR),
            new EmptyBorder(8, 12, 8, 12)
        ));
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

        JButton clearSearchButton = createModernButton("Clear", SURFACE_LIGHTER, TEXT_SECONDARY);
        clearSearchButton.addActionListener(_ -> searchField.setText(""));

        searchPanel.add(searchField);
        searchPanel.add(clearSearchButton);

        // Create modern styled table
        String[] columnNames = {"Website/Service", "URL", "Username/Email", "Password", "Notes"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        passwordTable = new JTable(tableModel);
        passwordTable.setRowHeight(45);
        passwordTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        passwordTable.setShowGrid(false);
        passwordTable.setIntercellSpacing(new Dimension(0, 0));
        passwordTable.setBackground(SURFACE_DARK);
        passwordTable.setForeground(TEXT_PRIMARY);
        passwordTable.setSelectionBackground(SURFACE_HOVER);
        passwordTable.setSelectionForeground(TEXT_PRIMARY);
        passwordTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        // Custom cell renderer for modern look
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(isSelected ? SURFACE_HOVER : (row % 2 == 0 ? SURFACE_DARK : new Color(35, 35, 35)));
                c.setForeground(TEXT_PRIMARY);
                ((JLabel) c).setBorder(new EmptyBorder(8, 12, 8, 12));
                return c;
            }
        };
        
        for (int i = 0; i < passwordTable.getColumnCount(); i++) {
            passwordTable.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
        
        // Style table header
        JTableHeader header = passwordTable.getTableHeader();
        header.setBackground(SURFACE_LIGHTER);
        header.setForeground(TEXT_PRIMARY);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_COLOR));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 40));
        
        DefaultTableCellRenderer headerRenderer = (DefaultTableCellRenderer) header.getDefaultRenderer();
        headerRenderer.setHorizontalAlignment(JLabel.LEFT);
        headerRenderer.setBorder(new EmptyBorder(0, 12, 0, 12));

        // Add sorter for search functionality
        sorter = new TableRowSorter<>(tableModel);
        passwordTable.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(passwordTable);
        scrollPane.setBorder(new RoundedBorder(12, BORDER_COLOR));
        scrollPane.getViewport().setBackground(SURFACE_DARK);
        scrollPane.setBackground(SURFACE_DARK);
        
        // Style scrollbar
        styleScrollBar(scrollPane.getVerticalScrollBar());
        styleScrollBar(scrollPane.getHorizontalScrollBar());

        // Create center panel with search and table
        JPanel centerPanel = new JPanel(new BorderLayout(0, 15));
        centerPanel.setBackground(BACKGROUND_DARK);
        centerPanel.add(searchPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Create modern button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 15));
        buttonPanel.setBackground(BACKGROUND_DARK);

        JButton addButton = createModernButton("Add Password", PRIMARY_COLOR, TEXT_PRIMARY);
        JButton editButton = createModernButton("Edit", SURFACE_LIGHTER, TEXT_PRIMARY);
        JButton deleteButton = createModernButton("Delete", DANGER_COLOR, TEXT_PRIMARY);
        JButton viewButton = createModernButton("Toggle View", SURFACE_LIGHTER, TEXT_PRIMARY);
        JButton copyButton = createModernButton("Copy", SUCCESS_COLOR, TEXT_PRIMARY);
        JButton openUrlButton = createModernButton("Open URL", SURFACE_LIGHTER, TEXT_PRIMARY);
        JButton changeCodeButton = createModernButton("Change Secret Code", ACCENT_COLOR, TEXT_PRIMARY);

        addButton.addActionListener(_ -> showAddPasswordDialog());
        editButton.addActionListener(_ -> editSelectedPassword());
        deleteButton.addActionListener(_ -> deleteSelectedPassword());
        viewButton.addActionListener(_ -> viewSelectedPassword());
        copyButton.addActionListener(_ -> copyPasswordToClipboard());
        openUrlButton.addActionListener(_ -> openSelectedUrl());
        changeCodeButton.addActionListener(_ -> showChangeSecretCodeDialog());

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(viewButton);
        buttonPanel.add(copyButton);
        buttonPanel.add(openUrlButton);
        buttonPanel.add(changeCodeButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        pageContainer.setBackground(BACKGROUND_DARK);
        pageContainer.add(mainPanel, HOME_PAGE_ID);
        pageStack.push(HOME_PAGE_ID);
        setContentPane(pageContainer);

        // Load existing passwords from database
        loadPasswords();
    }

    private JPanel createPageWrapper(String title, JPanel content) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(BACKGROUND_DARK);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BACKGROUND_DARK);
        topBar.setBorder(new EmptyBorder(12, 12, 8, 12));

        JButton closeButton = createModernButton("X", SURFACE_LIGHTER, TEXT_PRIMARY);
        closeButton.setPreferredSize(new Dimension(44, 34));
        closeButton.addActionListener(_ -> popPage());

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(TEXT_PRIMARY);

        topBar.add(closeButton, BorderLayout.WEST);
        topBar.add(titleLabel, BorderLayout.CENTER);

        JScrollPane contentScroll = new JScrollPane(content);
        contentScroll.setBorder(BorderFactory.createEmptyBorder());
        contentScroll.getViewport().setBackground(BACKGROUND_DARK);
        contentScroll.setBackground(BACKGROUND_DARK);
        styleScrollBar(contentScroll.getVerticalScrollBar());
        styleScrollBar(contentScroll.getHorizontalScrollBar());

        wrapper.add(topBar, BorderLayout.NORTH);
        wrapper.add(contentScroll, BorderLayout.CENTER);
        return wrapper;
    }

    private void pushPage(String title, JPanel content) {
        String pageId = "page_" + (++pageCounter);
        JPanel wrapped = createPageWrapper(title, content);
        dynamicPages.put(pageId, wrapped);
        pageContainer.add(wrapped, pageId);
        pageStack.push(pageId);
        pageLayout.show(pageContainer, pageId);
        revalidate();
        repaint();
    }

    private void popPage() {
        if (pageStack.size() <= 1) {
            return;
        }

        String currentPage = pageStack.pop();
        JPanel panel = dynamicPages.remove(currentPage);
        if (panel != null) {
            pageContainer.remove(panel);
        }

        pageLayout.show(pageContainer, pageStack.peek());
        revalidate();
        repaint();
    }

    // Helper method to create modern styled buttons
    private JButton createModernButton(String text, Color bgColor, Color fgColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isPressed()) {
                    g2.setColor(bgColor.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(bgColor.brighter());
                } else {
                    g2.setColor(bgColor);
                }
                
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                
                super.paintComponent(g);
            }
        };
        
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(fgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(10, 20, 10, 20));
        
        return button;
    }
    
    // Helper method to style scrollbars
    private void styleScrollBar(JScrollBar scrollBar) {
        scrollBar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = SURFACE_LIGHTER;
                this.thumbDarkShadowColor = SURFACE_LIGHTER;
                this.thumbHighlightColor = SURFACE_HOVER;
                this.thumbLightShadowColor = SURFACE_LIGHTER;
                this.trackColor = SURFACE_DARK;
                this.trackHighlightColor = SURFACE_DARK;
            }
            
            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }
            
            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }
            
            private JButton createZeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }
            
            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(thumbColor);
                g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2, 
                    thumbBounds.width - 4, thumbBounds.height - 4, 8, 8);
                g2.dispose();
            }
            
            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(trackColor);
                g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
                g2.dispose();
            }
        });
        scrollBar.setPreferredSize(new Dimension(12, 12));
    }

    /**
     * Validates the secret code against the stored hash and handles migration if needed.
     */
    private void handleSecretCodeValidation(String secretCode) {
        String storedHash = database.getSecretCodeHash();

        if (storedHash == null) {
            // First time setup - store the hash
            String hash = PasswordHasher.hashPassword(secretCode);
            database.setSecretCodeHash(hash);
            System.out.println("First time setup: Secret code hash stored");
        } else {
            // Check if secret code has changed
            if (!PasswordHasher.verifyPassword(storedHash, secretCode)) {
                // Secret code has changed - need to migrate passwords
                System.out.println("Secret code has changed - migration needed");
                handleSecretCodeMigration(secretCode, storedHash);
            } else {
                System.out.println("Secret code verified successfully");
            }
        }
    }

    /**
     * Handles migration of passwords when secret code changes.
     */
    private void handleSecretCodeMigration(String newSecretCode, String oldHash) {
        SwingUtilities.invokeLater(() -> {
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBackground(BACKGROUND_DARK);
            panel.setBorder(new EmptyBorder(15, 15, 15, 15));

            JLabel messageLabel = new JLabel("<html><body style='width: 350px'>" +
                "<h2 style='color: #F0F0F0'>Secret Code Changed</h2>" +
                "<p style='color: #A0A0A0'>The secret code has been changed in the application code.<br><br>" +
                "To access your existing passwords, please enter your <b>OLD</b> secret code.<br>" +
                "All passwords will be re-encrypted with the new secret code.</p>" +
                "</body></html>");
            messageLabel.setForeground(TEXT_PRIMARY);

            JPasswordField oldCodeField = new JPasswordField(20);
            oldCodeField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            oldCodeField.setBackground(SURFACE_LIGHTER);
            oldCodeField.setForeground(TEXT_PRIMARY);
            oldCodeField.setCaretColor(TEXT_PRIMARY);
            oldCodeField.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(8, BORDER_COLOR),
                new EmptyBorder(8, 12, 8, 12)
            ));

            JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
            inputPanel.setBackground(BACKGROUND_DARK);
            JLabel inputLabel = createStyledLabel("Old Secret Code:");
            inputLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            inputPanel.add(inputLabel, BorderLayout.NORTH);
            inputPanel.add(oldCodeField, BorderLayout.CENTER);

            panel.add(messageLabel, BorderLayout.NORTH);
            panel.add(inputPanel, BorderLayout.CENTER);

            // Style the option pane
            UIManager.put("OptionPane.background", BACKGROUND_DARK);
            UIManager.put("Panel.background", BACKGROUND_DARK);

            int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Password Migration Required",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
            );

            if (result == JOptionPane.OK_OPTION) {
                String oldSecretCode = new String(oldCodeField.getPassword());

                // Verify old secret code
                if (PasswordHasher.verifyPassword(oldHash, oldSecretCode)) {
                    // Perform migration
                    boolean success = database.reEncryptAllPasswords(oldSecretCode, newSecretCode);

                    if (success) {
                        JOptionPane.showMessageDialog(
                            this,
                            "Migration successful!\nAll passwords have been re-encrypted with the new secret code.",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                        loadPasswords(); // Reload with new encryption
                    } else {
                        JOptionPane.showMessageDialog(
                            this,
                            "Migration failed. Please check the console for errors.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        "Incorrect old secret code. Cannot migrate passwords.\n" +
                        "Your passwords are still encrypted with the old code.",
                        "Authentication Failed",
                        JOptionPane.ERROR_MESSAGE
                    );
                    // Exit the application since we can't decrypt passwords
                    System.exit(1);
                }
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    "Migration cancelled. The application will close.\n" +
                    "Change the secret code back to access your passwords.",
                    "Migration Cancelled",
                    JOptionPane.WARNING_MESSAGE
                );
                System.exit(0);
            }
        });
    }

    private void loadPasswords() {
        // Clear existing rows and revealed passwords tracking
        tableModel.setRowCount(0);
        revealedPasswordRows.clear();

        // Load from database
        allPasswords = database.getAllPasswords();
        for (PasswordDatabase.PasswordEntry entry : allPasswords) {
            tableModel.addRow(new Object[]{
                entry.service(),
                entry.url(),
                entry.username(),
                "••••••••",
                entry.notes()
            });
        }
    }

    private void addPassword(String service, String username, String password, String url, String notes) {
        if (database.addPassword(service, username, password, url, notes)) {
            loadPasswords(); // Reload table
        } else {
            JOptionPane.showMessageDialog(this,
                "Failed to add password to database.",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createPasswordFieldWithToggle(JPasswordField passwordField) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.setBackground(SURFACE_LIGHTER);
        panel.setBorder(new RoundedBorder(8, BORDER_COLOR));
        
        passwordField.setBackground(SURFACE_LIGHTER);
        passwordField.setForeground(TEXT_PRIMARY);
        passwordField.setCaretColor(TEXT_PRIMARY);
        passwordField.setBorder(new EmptyBorder(8, 12, 8, 8));
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        panel.add(passwordField, BorderLayout.CENTER);

        JButton toggleButton = new JButton("👁");
        toggleButton.setPreferredSize(new Dimension(40, passwordField.getPreferredSize().height));
        toggleButton.setFocusable(false);
        toggleButton.setToolTipText("Show/Hide password");
        toggleButton.setBackground(SURFACE_LIGHTER);
        toggleButton.setForeground(TEXT_SECONDARY);
        toggleButton.setBorderPainted(false);
        toggleButton.setContentAreaFilled(false);
        toggleButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

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
    
    private JTextField createStyledTextField() {
        final int columns = 20;
        JTextField field = new JTextField(columns);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBackground(SURFACE_LIGHTER);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(8, BORDER_COLOR),
            new EmptyBorder(8, 12, 8, 12)
        ));
        return field;
    }
    
    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(TEXT_SECONDARY);
        return label;
    }
    
    private void styleComboBox(AutocompleteComboBox comboBox) {
        comboBox.setBackground(SURFACE_LIGHTER);
        comboBox.setForeground(TEXT_PRIMARY);
        comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    }
    
    private void showStyledMessageDialog(Component parent, String message, String title, int messageType) {
        UIManager.put("OptionPane.background", BACKGROUND_DARK);
        UIManager.put("Panel.background", BACKGROUND_DARK);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        JOptionPane.showMessageDialog(parent, message, title, messageType);
    }
    
    private JCheckBox createStyledCheckBox(String text) {
        final boolean selected = true;
        JCheckBox checkBox = new JCheckBox(text, selected);
        checkBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        checkBox.setForeground(TEXT_PRIMARY);
        checkBox.setBackground(SURFACE_DARK);
        checkBox.setFocusPainted(false);
        return checkBox;
    }

    private void showAddPasswordDialog() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        JTextField serviceField = createStyledTextField();
        AutocompleteComboBox usernameField = new AutocompleteComboBox(database.getUniqueUsernames());
        styleComboBox(usernameField);
        JPasswordField passwordField = new JPasswordField(20);
        JPanel passwordPanel = createPasswordFieldWithToggle(passwordField);
        JTextField urlField = createStyledTextField();
        JTextField notesField = createStyledTextField();

        JButton generateButton = createModernButton("Generate", ACCENT_COLOR, TEXT_PRIMARY);
        generateButton.setToolTipText("Generate a strong password");
        generateButton.addActionListener(_ -> openPasswordGeneratorPage(passwordField::setText));

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; gbc.gridwidth = 1;
        panel.add(createStyledLabel("Website/Service:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        panel.add(serviceField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; gbc.gridwidth = 1;
        panel.add(createStyledLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        panel.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; gbc.gridwidth = 1;
        panel.add(createStyledLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 1;
        panel.add(passwordPanel, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(generateButton, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0; gbc.gridwidth = 1;
        panel.add(createStyledLabel("URL:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        panel.add(urlField, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0; gbc.gridwidth = 1;
        panel.add(createStyledLabel("Notes:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        panel.add(notesField, gbc);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(BACKGROUND_DARK);
        JButton saveButton = createModernButton("Save", PRIMARY_COLOR, TEXT_PRIMARY);
        JButton cancelButton = createModernButton("Cancel", SURFACE_LIGHTER, TEXT_SECONDARY);

        saveButton.addActionListener(_ -> {
            String service = serviceField.getText().trim();
            String username = usernameField.getSelectedText().trim();
            String password = new String(passwordField.getPassword());
            String url = urlField.getText().trim();
            String notes = notesField.getText().trim();

            if (!service.isEmpty() && !username.isEmpty() && !password.isEmpty()) {
                addPassword(service, username, password, url, notes);
                popPage();
            } else {
                showStyledMessageDialog(this,
                    "Please fill in service, username, and password fields.",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
            }
        });

        cancelButton.addActionListener(_ -> popPage());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 8, 8, 8);
        panel.add(buttonPanel, gbc);

        pushPage("Add Password", panel);
    }

    private void editSelectedPassword() {
        int selectedRow = passwordTable.getSelectedRow();
        if (selectedRow == -1) {
            showStyledMessageDialog(this, "Please select a password to edit.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = passwordTable.convertRowIndexToModel(selectedRow);
        PasswordDatabase.PasswordEntry entry = allPasswords.get(modelRow);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        JTextField serviceField = createStyledTextField();
        serviceField.setText(entry.service());
        AutocompleteComboBox usernameField = new AutocompleteComboBox(database.getUniqueUsernames());
        styleComboBox(usernameField);
        usernameField.setSelectedText(entry.username());
        JPasswordField passwordField = new JPasswordField(entry.password(), 20);
        JPanel passwordPanel = createPasswordFieldWithToggle(passwordField);
        JTextField urlField = createStyledTextField();
        urlField.setText(entry.url());
        JTextField notesField = createStyledTextField();
        notesField.setText(entry.notes());

        JButton generateButton = createModernButton("Generate", ACCENT_COLOR, TEXT_PRIMARY);
        generateButton.setToolTipText("Generate a strong password");
        generateButton.addActionListener(_ -> openPasswordGeneratorPage(passwordField::setText));

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; gbc.gridwidth = 1;
        panel.add(createStyledLabel("Website/Service:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        panel.add(serviceField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; gbc.gridwidth = 1;
        panel.add(createStyledLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        panel.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; gbc.gridwidth = 1;
        panel.add(createStyledLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 1;
        panel.add(passwordPanel, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(generateButton, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0; gbc.gridwidth = 1;
        panel.add(createStyledLabel("URL:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        panel.add(urlField, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0; gbc.gridwidth = 1;
        panel.add(createStyledLabel("Notes:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        panel.add(notesField, gbc);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(BACKGROUND_DARK);
        JButton saveButton = createModernButton("Save", PRIMARY_COLOR, TEXT_PRIMARY);
        JButton cancelButton = createModernButton("Cancel", SURFACE_LIGHTER, TEXT_SECONDARY);

        saveButton.addActionListener(_ -> {
            String service = serviceField.getText().trim();
            String username = usernameField.getSelectedText().trim();
            String password = new String(passwordField.getPassword());
            String url = urlField.getText().trim();
            String notes = notesField.getText().trim();

            if (database.updatePassword(entry.id(), service, username, password, url, notes)) {
                loadPasswords();
                popPage();
            } else {
                showStyledMessageDialog(this,
                    "Failed to update password in database.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(_ -> popPage());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 8, 8, 8);
        panel.add(buttonPanel, gbc);

        pushPage("Edit Password", panel);
    }

    private void deleteSelectedPassword() {
        int selectedRow = passwordTable.getSelectedRow();
        if (selectedRow == -1) {
            showStyledMessageDialog(this, "Please select a password to delete.",
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

            if (database.deletePassword(entry.id())) {
                loadPasswords(); // Reload table
            } else {
                showStyledMessageDialog(this,
                    "Failed to delete password from database.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void viewSelectedPassword() {
        int selectedRow = passwordTable.getSelectedRow();
        if (selectedRow == -1) {
            showStyledMessageDialog(this, "Please select a password to view.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Convert view row to model row (for filtered table)
        int modelRow = passwordTable.convertRowIndexToModel(selectedRow);
        PasswordDatabase.PasswordEntry entry = allPasswords.get(modelRow);

        // Toggle password visibility in the table
        if (revealedPasswordRows.contains(modelRow)) {
            // Hide the password
            revealedPasswordRows.remove(modelRow);
            tableModel.setValueAt("••••••••", modelRow, 3);
        } else {
            // Reveal the password
            revealedPasswordRows.add(modelRow);
            tableModel.setValueAt(entry.password(), modelRow, 3);
        }
    }

    private void copyPasswordToClipboard() {
        int selectedRow = passwordTable.getSelectedRow();
        if (selectedRow == -1) {
            showStyledMessageDialog(this, "Please select a password to copy.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Convert view row to model row (for filtered table)
        int modelRow = passwordTable.convertRowIndexToModel(selectedRow);
        PasswordDatabase.PasswordEntry entry = allPasswords.get(modelRow);

        java.awt.datatransfer.StringSelection stringSelection =
            new java.awt.datatransfer.StringSelection(entry.password());
        java.awt.datatransfer.Clipboard clipboard =
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);

        showStyledMessageDialog(this, "Password copied to clipboard!",
            "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void openSelectedUrl() {
        int selectedRow = passwordTable.getSelectedRow();
        if (selectedRow == -1) {
            showStyledMessageDialog(this, "Please select an entry to open its URL.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = passwordTable.convertRowIndexToModel(selectedRow);
        PasswordDatabase.PasswordEntry entry = allPasswords.get(modelRow);
        String rawUrl = entry.url();

        if (rawUrl == null || rawUrl.isBlank()) {
            showStyledMessageDialog(this, "This entry does not have a URL yet.",
                "Missing URL", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String normalizedUrl = rawUrl.trim();
        if (!normalizedUrl.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            normalizedUrl = "https://" + normalizedUrl;
        }

        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                showStyledMessageDialog(this,
                    "Desktop browser integration is not available on this system.",
                    "Cannot Open URL", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Desktop.getDesktop().browse(new URI(normalizedUrl));
        } catch (Exception e) {
            showStyledMessageDialog(this,
                "Could not open URL: " + normalizedUrl,
                "Invalid or Unavailable URL", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Shows a dialog to change the secret code and re-encrypt all passwords.
     */
    private void showChangeSecretCodeDialog() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BACKGROUND_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        JLabel titleLabel = new JLabel("<html><body style='width: 400px'>" +
            "<h2 style='color: #F0F0F0'>Change Secret Code</h2>" +
            "<p style='color: #A0A0A0'>This will update the code in Main.java and re-encrypt all passwords.<br>" +
            "Make sure to update the SECRET_CODE constant in your code!</p>" +
            "</body></html>");

        JLabel oldLabel = createStyledLabel("Current Secret Code:");
        JPasswordField oldCodeField = new JPasswordField(20);
        stylePasswordField(oldCodeField);

        JLabel newLabel = createStyledLabel("New Secret Code:");
        JPasswordField newCodeField = new JPasswordField(20);
        stylePasswordField(newCodeField);

        JLabel confirmLabel = createStyledLabel("Confirm New Code:");
        JPasswordField confirmCodeField = new JPasswordField(20);
        stylePasswordField(confirmCodeField);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0;
        panel.add(oldLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(oldCodeField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        panel.add(newLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(newCodeField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        panel.add(confirmLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(confirmCodeField, gbc);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(BACKGROUND_DARK);
        JButton changeButton = createModernButton("Change Code", PRIMARY_COLOR, TEXT_PRIMARY);
        JButton cancelButton = createModernButton("Cancel", SURFACE_LIGHTER, TEXT_SECONDARY);

        changeButton.addActionListener(_ -> {
            String oldCode = new String(oldCodeField.getPassword());
            String newCode = new String(newCodeField.getPassword());
            String confirmCode = new String(confirmCodeField.getPassword());

            if (oldCode.isEmpty() || newCode.isEmpty() || confirmCode.isEmpty()) {
                showStyledMessageDialog(this, "All fields are required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!newCode.equals(confirmCode)) {
                showStyledMessageDialog(this, "New codes don't match.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String storedHash = database.getSecretCodeHash();
            if (storedHash != null && !PasswordHasher.verifyPassword(storedHash, oldCode)) {
                showStyledMessageDialog(this, "Current secret code is incorrect.", "Authentication Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean success = database.reEncryptAllPasswords(oldCode, newCode);

            if (success) {
                PasswordHasher.setMasterPassword(newCode);
                popPage();
                showStyledMessageDialog(this,
                    "Secret code changed successfully!\n\n" +
                    "IMPORTANT: Update the SECRET_CODE constant in Main.java to:\n" +
                    "private static final String SECRET_CODE = \"" + newCode + "\";\n\n" +
                    "All passwords have been re-encrypted.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
                loadPasswords();
            } else {
                showStyledMessageDialog(this,
                    "Failed to change secret code. Check console for errors.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(_ -> popPage());

        buttonPanel.add(changeButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 8, 8, 8);
        panel.add(buttonPanel, gbc);

        pushPage("Change Secret Code", panel);
    }

    private void stylePasswordField(JPasswordField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBackground(SURFACE_LIGHTER);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(8, BORDER_COLOR),
            new EmptyBorder(8, 12, 8, 12)
        ));
    }

    private void openPasswordGeneratorPage(Consumer<String> onUse) {
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(BACKGROUND_DARK);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Preview panel at top
        JPanel previewPanel = new JPanel(new BorderLayout(8, 8));
        previewPanel.setBackground(SURFACE_DARK);
        previewPanel.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(12, BORDER_COLOR),
            new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel previewLabel = new JLabel("Generated Password");
        previewLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        previewLabel.setForeground(TEXT_SECONDARY);
        
        JTextField previewField = new JTextField();
        previewField.setFont(new Font("JetBrains Mono", Font.BOLD, 16));
        previewField.setEditable(false);
        previewField.setHorizontalAlignment(JTextField.CENTER);
        previewField.setBackground(SURFACE_LIGHTER);
        previewField.setForeground(PRIMARY_COLOR);
        previewField.setCaretColor(PRIMARY_COLOR);
        previewField.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(8, BORDER_COLOR),
            new EmptyBorder(12, 12, 12, 12)
        ));

        JButton regenerateButton = createModernButton("Regenerate", ACCENT_COLOR, TEXT_PRIMARY);

        JPanel previewTopPanel = new JPanel(new BorderLayout());
        previewTopPanel.setBackground(SURFACE_DARK);
        previewTopPanel.add(previewLabel, BorderLayout.WEST);
        
        JPanel previewContentPanel = new JPanel(new BorderLayout(8, 8));
        previewContentPanel.setBackground(SURFACE_DARK);
        previewContentPanel.add(previewField, BorderLayout.CENTER);
        previewContentPanel.add(regenerateButton, BorderLayout.EAST);
        
        previewPanel.add(previewTopPanel, BorderLayout.NORTH);
        previewPanel.add(previewContentPanel, BorderLayout.CENTER);

        mainPanel.add(previewPanel, BorderLayout.NORTH);

        // Mode selection
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        modePanel.setBackground(SURFACE_DARK);
        modePanel.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(12, BORDER_COLOR),
            new EmptyBorder(8, 12, 8, 12)
        ));
        
        ButtonGroup modeGroup = new ButtonGroup();
        JRadioButton charModeRadio = new JRadioButton("Character-based", true);
        JRadioButton wordModeRadio = new JRadioButton("Word-based");
        
        charModeRadio.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        wordModeRadio.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        charModeRadio.setForeground(TEXT_PRIMARY);
        wordModeRadio.setForeground(TEXT_PRIMARY);
        charModeRadio.setBackground(SURFACE_DARK);
        wordModeRadio.setBackground(SURFACE_DARK);
        charModeRadio.setFocusPainted(false);
        wordModeRadio.setFocusPainted(false);
        
        modeGroup.add(charModeRadio);
        modeGroup.add(wordModeRadio);
        modePanel.add(charModeRadio);
        modePanel.add(wordModeRadio);

        // Options panel
        JPanel optionsPanel = new JPanel(new GridBagLayout());
        optionsPanel.setBackground(SURFACE_DARK);
        optionsPanel.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(12, BORDER_COLOR),
            new EmptyBorder(15, 15, 15, 15)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // Character-based options
        JLabel lengthLabel = createStyledLabel("Password Length: 16");
        lengthLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        JSlider lengthSlider = new JSlider(4, 64, 16);
        lengthSlider.setMajorTickSpacing(10);
        lengthSlider.setMinorTickSpacing(2);
        lengthSlider.setPaintTicks(true);
        lengthSlider.setPaintLabels(true);
        lengthSlider.setBackground(SURFACE_DARK);
        lengthSlider.setForeground(TEXT_SECONDARY);

        JCheckBox uppercaseCheck = createStyledCheckBox("Uppercase (A-Z)");
        JCheckBox lowercaseCheck = createStyledCheckBox("Lowercase (a-z)");
        JCheckBox digitsCheck = createStyledCheckBox("Digits (0-9)");
        JCheckBox specialCheck = createStyledCheckBox("Special (!@#$%^&*...)" );

        // Word-based options
        JLabel wordCountLabel = createStyledLabel("Number of Words: 4");
        wordCountLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        wordCountLabel.setEnabled(false);
        
        JSlider wordCountSlider = new JSlider(2, 8, 4);
        wordCountSlider.setMajorTickSpacing(2);
        wordCountSlider.setMinorTickSpacing(1);
        wordCountSlider.setPaintTicks(true);
        wordCountSlider.setPaintLabels(true);
        wordCountSlider.setBackground(SURFACE_DARK);
        wordCountSlider.setForeground(TEXT_SECONDARY);
        wordCountSlider.setEnabled(false);

        JLabel separatorLabel = createStyledLabel("Separator:");
        separatorLabel.setEnabled(false);
        
        JComboBox<String> separatorCombo = new JComboBox<>(new String[]{"-", "_", ".", " ", ""});
        separatorCombo.setBackground(SURFACE_LIGHTER);
        separatorCombo.setForeground(TEXT_PRIMARY);
        separatorCombo.setEnabled(false);

        JCheckBox wordCapitalCheck = createStyledCheckBox("Capitalize words");
        wordCapitalCheck.setEnabled(false);
        JCheckBox wordDigitsCheck = createStyledCheckBox("Add numbers");
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
        JPanel centerPanel = new JPanel(new BorderLayout(0, 12));
        centerPanel.setBackground(BACKGROUND_DARK);
        centerPanel.add(modePanel, BorderLayout.NORTH);
        centerPanel.add(optionsPanel, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Quick presets
        JPanel presetsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        presetsPanel.setBackground(SURFACE_DARK);
        presetsPanel.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(12, BORDER_COLOR),
            new EmptyBorder(10, 12, 10, 12)
        ));

        JButton strongButton = createModernButton("Strong (20)", SURFACE_LIGHTER, TEXT_PRIMARY);
        JButton defaultButton = createModernButton("Default (16)", SURFACE_LIGHTER, TEXT_PRIMARY);
        JButton simpleButton = createModernButton("Simple (12)", SURFACE_LIGHTER, TEXT_PRIMARY);
        JButton pinButton = createModernButton("PIN (6)", SURFACE_LIGHTER, TEXT_PRIMARY);
        JButton wordsButton = createModernButton("Words (4)", SURFACE_LIGHTER, TEXT_PRIMARY);

        presetsPanel.add(strongButton);
        presetsPanel.add(defaultButton);
        presetsPanel.add(simpleButton);
        presetsPanel.add(pinButton);
        presetsPanel.add(wordsButton);

        mainPanel.add(presetsPanel, BorderLayout.SOUTH);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 15));
        buttonPanel.setBackground(BACKGROUND_DARK);
        JButton useButton = createModernButton("Use This Password", PRIMARY_COLOR, TEXT_PRIMARY);
        JButton cancelButton = createModernButton("Cancel", SURFACE_LIGHTER, TEXT_SECONDARY);

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
            onUse.accept(previewField.getText());
            popPage();
        });

        cancelButton.addActionListener(_ -> popPage());

        buttonPanel.add(useButton);
        buttonPanel.add(cancelButton);

        // Main layout
        JPanel containerPanel = new JPanel(new BorderLayout(0, 0));
        containerPanel.setBackground(BACKGROUND_DARK);
        containerPanel.add(mainPanel, BorderLayout.CENTER);
        containerPanel.add(buttonPanel, BorderLayout.SOUTH);

        autoGenerate.run();
        pushPage("Password Generator", containerPanel);
    }

    public static void launch(String masterPassword) {
        // Initialize the encryption key with the master password (secret code)
        PasswordHasher.setMasterPassword(masterPassword);
        
        SwingUtilities.invokeLater(() -> {
            PasswordManagerUI pm = new PasswordManagerUI(masterPassword);
            pm.setVisible(true);
        });
    }

    private void showDatabaseConnectionHelpDialog() {
        String err = database.getLastConnectionError();
        String details = (err == null || err.isBlank()) ? "Unknown connection error." : err;
        String proxyStatus = database.getLastProxyStartupStatus();
        if (proxyStatus == null || proxyStatus.isBlank()) {
            proxyStatus = "Not attempted.";
        }
        String cloudflareHostname = CloudflareTcpProxyManager.getEffectiveHostname();
        String mode = database.getConnectionMode();
        boolean cloudflareMode = database.isCloudflareProxyMode();
        String modeInstructions = cloudflareMode
            ? "Mode: " + mode + " (Cloudflare tunnel).\n" +
              "Manual fallback command:\n" +
              "cloudflared access tcp --hostname " + cloudflareHostname + " --url localhost:13306\n\n" +
              "If prompted, complete Cloudflare Access login in your browser.\n\n"
            : "Mode: " + mode + " (direct MySQL).\n" +
              "Make sure TCP/3306 is reachable from this laptop and your server firewall allows this laptop's public IP.\n\n";
        showStyledMessageDialog(this,
            "Could not connect to the password database.\n\n" +
            modeInstructions +
            "Proxy startup status: " + proxyStatus + "\n\n" +
            "Connection error: " + details,
            "Database Connection Required",
            JOptionPane.WARNING_MESSAGE);
    }

    // Custom rounded border for modern UI
    static class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color color;
        
        RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }
        
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.draw(new RoundRectangle2D.Double(x, y, width - 1, height - 1, radius, radius));
            g2.dispose();
        }
        
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(radius / 2, radius / 2, radius / 2, radius / 2);
        }
        
        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.right = insets.top = insets.bottom = radius / 2;
            return insets;
        }
    }
}
