package gui;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import javax.swing.*;
import javax.swing.text.*;

class SwingConsoleStream extends OutputStream {
	private final JTextPane textPane;
	private final Color color;
	private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

	SwingConsoleStream(JTextPane textPane, Color color) {
		this.textPane = textPane;
		this.color = color;
	}

	@Override
	public void write(int b) {
		buffer.write(b);
		if (b == '\n') {
			SwingUtilities.invokeLater(() -> {
				String text = buffer.toString(StandardCharsets.UTF_8);
				StyledDocument doc = textPane.getStyledDocument();
				Style style = textPane.addStyle("ColorStyle", null);
				StyleConstants.setForeground(style, color);
				try {
					doc.insertString(doc.getLength(), text, style);
				} catch (BadLocationException e) {
					throw new RuntimeException(e);
				}
				buffer.reset();
			});
		}
	}
}
