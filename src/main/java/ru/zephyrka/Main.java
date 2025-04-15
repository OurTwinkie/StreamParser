package ru.zephyrka;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import ru.zephyrka.Utils.Parser;
import ru.zephyrka.Utils.Stream;
import ru.zephyrka.gui.MainFrame;

import javax.swing.*;
import java.io.FileWriter;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatMacDarkLaf());
        } catch (Exception ex) {
            System.err.println("Couldn't apply style :(");
        }
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);

            frame.startButton.addActionListener(e -> {
                new Thread(() -> {
                    try {
                        Parser.startParsing(
                                frame.twitchClientIdField.getText(),
                                frame.twitchAuthTokenField.getText(),
                                frame.kickClientIdField.getText(),
                                frame.kickClientSecretField.getText(),
                                frame.youtubeApiKeyField.getText(),
                                Integer.parseInt(frame.minViewersField.getText()),
                                Integer.parseInt(frame.maxViewersField.getText()),
                                Integer.parseInt(frame.targetCountField.getText()),
                                frame.twitchCheckBox.isSelected(),
                                frame.youtubeCheckBox.isSelected(),
                                frame.kickCheckBox.isSelected()
                        );
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();
            });

            frame.saveButton.addActionListener(e -> {
                JFileChooser fileChooser = new JFileChooser();
                if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    try (FileWriter writer = new FileWriter(fileChooser.getSelectedFile())) {
                        for (Stream stream : Parser.getCollectedStreams()) {
                            writer.write(stream.toString() + "\n");
                        }
                        System.out.println("Saved!");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });
        });
    }
}
