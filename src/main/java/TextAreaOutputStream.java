import javax.swing.*;
import java.io.OutputStream;

class TextAreaOutputStream extends OutputStream {
    private final JTextArea jTextArea;

    public TextAreaOutputStream(JTextArea textArea) {
        this.jTextArea = textArea;
    }

    @Override
    public void write(int b) {
        jTextArea.append(String.valueOf((char) b));
    }
}