package gui;

import java.awt.*;
import java.io.*;
import javax.swing.*;
import javax.swing.text.*;

class SwingConsoleStream extends OutputStream {
	private final JTextPane textPane;
	private final Color color;
	private final StringBuilder buffer = new StringBuilder();

	SwingConsoleStream(JTextPane textPane, Color color) {
		this.textPane = textPane;
		this.color = color;
	}

	@Override
	public void write(int b) {
		buffer.append((char) b);
		if (b == '\n') {
			SwingUtilities.invokeLater(() -> {
				StyledDocument doc = textPane.getStyledDocument();
				Style style = textPane.addStyle("ColorStyle", null);
				StyleConstants.setForeground(style, color);
				try {
					doc.insertString(doc.getLength(), buffer.toString(), style);
				} catch (BadLocationException e) {
					throw new RuntimeException(e);
				}
				buffer.setLength(0);
			});
		}
	}
}
