package ru.zephyrka.gui;

import java.io.OutputStream;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class ConsoleOutputStream extends OutputStream {
    private final JTextArea console;

    public ConsoleOutputStream(JTextArea console) {
        this.console = console;
    }

    @Override
    public void write(int b) {
        SwingUtilities.invokeLater(() -> {
            console.append(String.valueOf((char) b));
            console.setCaretPosition(console.getDocument().getLength());
        });
    }
}
