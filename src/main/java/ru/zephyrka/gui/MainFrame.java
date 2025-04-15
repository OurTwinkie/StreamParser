package ru.zephyrka.gui;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Properties;

public class MainFrame extends JFrame {
    private static final String CONFIG_PATH = "config/config.properties";

    public JTextField twitchClientIdField;
    public JTextField twitchAuthTokenField;
    public JTextField kickClientIdField;
    public JTextField kickClientSecretField;
    public JTextField youtubeApiKeyField;
    public JTextField minViewersField;
    public JTextField maxViewersField;
    public JTextField targetCountField;

    public JCheckBox twitchCheckBox;
    public JCheckBox youtubeCheckBox;
    public JCheckBox kickCheckBox;

    public JButton startButton;
    public JButton saveButton;
    private JButton saveConfigButton;
    public JTextArea consoleOutput;

    public MainFrame() {
        setTitle("Stream Parser");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 600);
        setLocationRelativeTo(null);

        initUI();
        loadConfig();

        System.setOut(new PrintStream(new ConsoleOutputStream(consoleOutput), true));
        System.setErr(new PrintStream(new ConsoleOutputStream(consoleOutput), true));
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel configPanel = new JPanel();
        configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.Y_AXIS));

        twitchClientIdField = createLabeledField(configPanel, "Twitch Client ID:");
        twitchAuthTokenField = createLabeledField(configPanel, "Twitch Auth Token:");
        kickClientIdField = createLabeledField(configPanel, "Kick Client ID:");
        kickClientSecretField = createLabeledField(configPanel, "Kick Client Secret:");
        youtubeApiKeyField = createLabeledField(configPanel, "YouTube API Key:");

        minViewersField = createLabeledField(configPanel, "Минимум зрителей:");
        maxViewersField = createLabeledField(configPanel, "Максимум зрителей:");
        targetCountField = createLabeledField(configPanel, "Целевое количество:");

        JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        twitchCheckBox = new JCheckBox("Twitch", true);
        youtubeCheckBox = new JCheckBox("YouTube", true);
        kickCheckBox = new JCheckBox("Kick", true);
        checkboxPanel.add(twitchCheckBox);
        checkboxPanel.add(youtubeCheckBox);
        checkboxPanel.add(kickCheckBox);

        configPanel.add(checkboxPanel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startButton = new JButton("Запустить парсинг");
        saveButton = new JButton("Сохранить данные");
        saveConfigButton = new JButton("Сохранить конфиг");
        saveConfigButton.addActionListener(e -> saveConfig());
        buttonPanel.add(startButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(saveConfigButton);

        configPanel.add(Box.createVerticalStrut(10));
        configPanel.add(buttonPanel);

        mainPanel.add(configPanel, BorderLayout.NORTH);

        consoleOutput = new JTextArea();
        consoleOutput.setEditable(false);
        consoleOutput.setFont(new Font("JetBrains Mono", Font.PLAIN, 13));
        JScrollPane scrollPane = new JScrollPane(consoleOutput);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel);
    }

    private JTextField createLabeledField(JPanel parent, String labelText) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(150, 30));
        JTextField textField = new JTextField();
        textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        panel.add(label);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(textField);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        parent.add(panel);
        parent.add(Box.createVerticalStrut(5));
        return textField;
    }

    private void saveConfig() {
        Properties props = new Properties();
        props.setProperty("twitchClientId", twitchClientIdField.getText());
        props.setProperty("twitchAuthToken", twitchAuthTokenField.getText());
        props.setProperty("kickClientId", kickClientIdField.getText());
        props.setProperty("kickClientSecret", kickClientSecretField.getText());
        props.setProperty("youtubeApiKey", youtubeApiKeyField.getText());
        props.setProperty("minViewers", minViewersField.getText());
        props.setProperty("maxViewers", maxViewersField.getText());
        props.setProperty("targetCount", targetCountField.getText());
        try {
            File configDir = new File("config");
            if (!configDir.exists()) configDir.mkdirs();
            FileOutputStream out = new FileOutputStream(CONFIG_PATH);
            props.store(out, null);
            out.close();
            System.out.println("The configuration is saved.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadConfig() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(CONFIG_PATH)) {
            props.load(in);
            twitchClientIdField.setText(props.getProperty("twitchClientId", ""));
            twitchAuthTokenField.setText(props.getProperty("twitchAuthToken", ""));
            kickClientIdField.setText(props.getProperty("kickClientId", ""));
            kickClientSecretField.setText(props.getProperty("kickClientSecret", ""));
            youtubeApiKeyField.setText(props.getProperty("youtubeApiKey", ""));
            minViewersField.setText(props.getProperty("minViewers", "10"));
            maxViewersField.setText(props.getProperty("maxViewers", "50"));
            targetCountField.setText(props.getProperty("targetCount", "1000"));
        } catch (IOException e) {
            System.out.println("The configuration file was not found. Default values are used.");
        }
    }
}
