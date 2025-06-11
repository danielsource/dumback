package gui;

import core.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;
import javax.swing.*;
import static core.I18n.i18n;

public class App {
	private static Core core;
	private static JFrame frame;
	private static JLabel statusLabel;
	private static Font fontSans;
	private static Font fontMono;

	private static void setTheme(float fontScale) {
		fontSans = new Font("SansSerif", Font.PLAIN, (int)(14 * fontScale));
		fontMono = new Font("Monospaced", Font.PLAIN, (int)(12 * fontScale));

		Color bg = new Color(40, 40, 40);
		Color fg = Color.WHITE;
		Color btn = new Color(85, 85, 85);

		Enumeration<Object> keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = UIManager.get(key);
			if (value instanceof Font)
				UIManager.put(key, fontSans);
			if (value instanceof Color) {
				if (key.toString().toLowerCase().contains("background"))
					UIManager.put(key, bg);
				if (key.toString().toLowerCase().contains("foreground"))
					UIManager.put(key, fg);
			}
		}

		UIManager.put("Button.background", btn);
		UIManager.put("Button.foreground", fg);
		UIManager.put("TextArea.selectionForeground", bg);
		UIManager.put("TextArea.selectionBackground", fg);
		UIManager.put("TextPane.selectionForeground", bg);
		UIManager.put("TextPane.selectionBackground", fg);
		UIManager.put("TextField.selectionForeground", bg);
		UIManager.put("TextField.selectionBackground", fg);
	}

	private static void createAndShowGUI(float fontScale, boolean visible) {
		setTheme(fontScale);

		frame = new JFrame("Dumback");
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				core.stopAutoBackup();
				System.exit(0);
			}
		});
		frame.setIconImage(Toolkit.getDefaultToolkit().getImage(App.class.getResource("/icon.png")));

		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		int width = (int)(screen.width * 0.6);
		int height = (int)(screen.height * 0.5);
		frame.setSize(width, height);
		frame.setLocationRelativeTo(null);

		statusLabel = new JLabel(getStatusText(), SwingConstants.LEFT);
		statusLabel.setOpaque(true);
		frame.add(statusLabel, BorderLayout.NORTH);

		JPanel centerPanel = new JPanel(new BorderLayout());
		frame.add(centerPanel, BorderLayout.CENTER);

		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
		leftPanel.setPreferredSize(new Dimension((int)(width * fontScale * 0.2), height));
		centerPanel.add(leftPanel, BorderLayout.WEST);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
		buttonPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, (int)(height * 0.5)));
		leftPanel.add(buttonPanel);

		JButton btnBackup = new JButton(i18n("btn.Backup_now"));
		btnBackup.addActionListener(ev -> backupNow());

		JButton btnConfig = new JButton(i18n("btn.Configure"));
		btnConfig.addActionListener(ev -> showConfigDialog());

		JButton btnStatus = new JButton(i18n("btn.Check_status"));
		btnStatus.addActionListener(ev -> showStatusDialog());

		for (JButton btn : new JButton[]{btnBackup, btnConfig, btnStatus}) {
			btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, btn.getPreferredSize().height));
			btn.setFocusPainted(false);
			btn.setMargin(new Insets(0, 0, 0, 0));
			buttonPanel.add(btn);
			buttonPanel.add(Box.createVerticalStrut(10));
		}

		JTextPane textPane = new JTextPane() {
			@Override
			public boolean getScrollableTracksViewportWidth() {
				return false;
			}
		};
		textPane.setEditable(false);
		textPane.setFont(fontMono);

		JScrollPane scrollPane = new JScrollPane(textPane,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		centerPanel.add(scrollPane, BorderLayout.CENTER);

		if (visible) {
			PrintStream out = new PrintStream(new SwingConsoleStream(textPane, Color.WHITE));
			PrintStream err = new PrintStream(new SwingConsoleStream(textPane, Color.RED));
			System.setOut(out);
			System.setErr(err);
		}

		frame.setVisible(visible);

		ConfigEntries cfg = core.getConfig();
		if (cfg.destPath == null)
			showConfigDialog();
	}

	private static void backupNow() {
		frame.setEnabled(false);
		Cursor oldCursor = frame.getCursor();
		frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		core.backup();
		updateStatus();
		frame.setEnabled(true);
		frame.setCursor(oldCursor);
	}

	private static void showConfigDialog() {
		ConfigEntries cfg = core.getConfig();

		JDialog dialog = new JDialog(frame, i18n("cfg.Configuration"), true);
		dialog.setLayout(new BorderLayout());

		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		int width = (int)(screen.width * 0.45);
		int height = (int)(screen.height * 0.45);
		dialog.setSize(width, height);
		dialog.setLocationRelativeTo(frame);

		JPanel mainPanel = new JPanel(new BorderLayout());
		dialog.add(mainPanel, BorderLayout.CENTER);

		JPanel formPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		final float formColRatio = 0.1f;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;
		mainPanel.add(formPanel, BorderLayout.NORTH);

		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = formColRatio;
		formPanel.add(new JLabel(i18n("cfg.Destination")), gbc);

		JPanel destPanel = new JPanel(new BorderLayout());
		gbc.gridx = 1; gbc.weightx = 1 - formColRatio;
		formPanel.add(destPanel, gbc);

		JLabel destLabel = new JLabel(cfg.destPath != null ? cfg.destPath.toString() : "");
		destPanel.add(destLabel, BorderLayout.WEST);

		JButton chooseBtn = new JButton(UIManager.getIcon("FileView.directoryIcon"));
		chooseBtn.setToolTipText(i18n("cfg.Choose_directory"));
		destPanel.add(chooseBtn, BorderLayout.EAST);

		chooseBtn.addActionListener(ev -> {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION)
				destLabel.setText(chooser.getSelectedFile().getAbsolutePath());
		});

		JSpinner freqSpinner = new JSpinner(new SpinnerNumberModel(cfg.freqDays, 0, 365, 1));
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = formColRatio;
		formPanel.add(new JLabel(i18n("cfg.Frequency_days")), gbc);
		gbc.gridx = 1; gbc.weightx = 1 - formColRatio;
		formPanel.add(freqSpinner, gbc);

		JSpinner keepSpinner = new JSpinner(new SpinnerNumberModel(cfg.keepDays, 0, 3650, 1));
		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = formColRatio;
		formPanel.add(new JLabel(i18n("cfg.Keep_days")), gbc);
		gbc.gridx = 1; gbc.weightx = 1 - formColRatio;
		formPanel.add(keepSpinner, gbc);

		gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = formColRatio;
		formPanel.add(Box.createVerticalStrut(10), gbc);

		DefaultListModel<Path> dirsModel = new DefaultListModel<>();
		cfg.dirsToBackup.forEach(dirsModel::addElement);
		JList<Path> dirsList = new JList<>(dirsModel);
		JScrollPane dirsScroll = new JScrollPane(dirsList);

		JPanel dirsPanel = new JPanel(new BorderLayout());
		dirsPanel.add(new JLabel("<html><b>" + i18n("cfg.Directories") + "</b></html>"),
				BorderLayout.NORTH);
		dirsPanel.add(dirsScroll, BorderLayout.CENTER);
		mainPanel.add(dirsPanel, BorderLayout.CENTER);

		JPanel dirsBtnPanel = new JPanel();
		dirsPanel.add(dirsBtnPanel, BorderLayout.SOUTH);

		JButton addDirBtn = new JButton(UIManager.getIcon("Tree.openIcon"));
		addDirBtn.setToolTipText(i18n("cfg.Add_directory"));
		addDirBtn.addActionListener(ev -> {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION)
				dirsModel.addElement(chooser.getSelectedFile().toPath());
		});
		dirsBtnPanel.add(addDirBtn);

		JButton removeDirBtn = new JButton(UIManager.getIcon("InternalFrame.closeIcon"));
		removeDirBtn.setToolTipText(i18n("cfg.Remove_directory"));
		removeDirBtn.addActionListener(ev -> {
			int indices[] = dirsList.getSelectedIndices();
			for (int i = indices.length - 1; i >= 0; i--)
				dirsModel.remove(indices[i]);
		});
		dirsBtnPanel.add(removeDirBtn);

		JButton saveBtn = new JButton(i18n("cfg.Save"));
		saveBtn.addActionListener(ev -> {
			try {
				java.util.List<Path> dirs = new ArrayList<>();
				for (int i = 0; i < dirsModel.size(); i++)
					dirs.add(dirsModel.getElementAt(i));
				core.updateConfig(new ConfigEntries(
							cfg.lastBackup,
							Path.of(destLabel.getText()),
							(int)freqSpinner.getValue(),
							(int)keepSpinner.getValue(),
							dirs
							));
				updateStatus();
				dialog.dispose();
			} catch (IllegalArgumentException e) {
				JOptionPane.showMessageDialog(dialog, e.getMessage(),
						i18n("Invalid setting"), JOptionPane.ERROR_MESSAGE);
			}
		});
		dialog.add(saveBtn, BorderLayout.SOUTH);

		dialog.setVisible(true);
	}

	private static void showStatusDialog() {
		ConfigEntries cfg = core.getConfig();
		StringBuilder sb = new StringBuilder();

		sb.append(i18n("status.Last_backup")).append(": ")
			.append(cfg.lastBackup != null ? cfg.lastBackup.toStringFormatted() : i18n("status.Never")).append("\n\n");

		sb.append(i18n("cfg.Destination")).append(": ")
			.append(cfg.destPath != null ? cfg.destPath : i18n("status.Not_set")).append("\n\n");

		sb.append(i18n("cfg.Frequency_days")).append(": ")
			.append(cfg.freqDays > 0 ? i18n("status.Enabled") + " (" + cfg.freqDays + " " + i18n("status.days") + ")"
					: i18n("status.Disabled")).append("\n\n");

		sb.append(i18n("cfg.Keep_days")).append(": ")
			.append(cfg.keepDays > 0 ? cfg.keepDays + " " + i18n("status.days") : i18n("status.Forever")).append("\n\n");

		sb.append(i18n("cfg.Directories")).append(":\n");
		if (cfg.dirsToBackup.isEmpty())
			sb.append("  ").append(i18n("status.None_configured")).append("\n");
		else
			cfg.dirsToBackup.forEach(dir -> sb.append("  - ").append(dir).append("\n"));

		Map<Path, Boolean> integrity = core.checkIntegrity();
		if (integrity != null && !integrity.isEmpty()) {
			sb.append("\n").append(i18n("status.Integrity_check")).append(":\n");
			integrity.forEach((path, valid) -> {
				sb.append("  - ").append(path.getFileName()).append(": ")
					.append(valid ? i18n("status.Valid") : i18n("status.INVALID")).append("\n");
			});
		}

		JTextArea statusArea = new JTextArea(sb.toString());
		statusArea.setFont(fontMono);
		statusArea.setEditable(false);
		JOptionPane.showMessageDialog(frame, new JScrollPane(statusArea),
				i18n("status.Status"), JOptionPane.INFORMATION_MESSAGE);
	}

	private static void updateStatus() {
		if (statusLabel != null)
			statusLabel.setText(getStatusText());
	}

	private static String getStatusText() {
		ConfigEntries cfg = core.getConfig();
		return String.format("<html><b>%s:</b> %s<br><b>%s:</b> %s<br></html>",
				i18n("status.Last_backup"),
				cfg.lastBackup != null
				? cfg.lastBackup.daysBetween(new Date()) + " " + i18n("status.days_ago") + " (" + cfg.lastBackup.toStringFormatted() + ")"
				: i18n("status.Never"),
				i18n("status.Auto_backup"),
				cfg.freqDays > 0 ? i18n("status.Enabled") : i18n("status.Disabled"));
	}

	private static void exitWithUsage() {
		System.err.println(i18n("cmd.Usage"));
		System.err.println(i18n("cmd.About"));
		System.exit(2);
	}

	public static void main(String args[]) {
		System.setProperty("awt.useSystemAAFontSettings", "on");

		float fontScale = 1;
		String fontScaleVar = System.getenv("DUMBACK_FONT_SCALE");
		if (fontScaleVar != null) {
			try {
				fontScale = Float.parseFloat(fontScaleVar);
			} finally {
				if (fontScale < 0)
					fontScale = 1;
			}
		}

		boolean visible = true;
		if (args.length > 1) {
			exitWithUsage();
		} else if (args.length == 1) {
			if (!"-hidden".equals(args[0]))
				exitWithUsage();
			visible = false;
		}


		try {
			core = new Core(App::updateStatus);
		} catch (RuntimeException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, e.getMessage(),
					"Fatal error on initialization", JOptionPane.ERROR_MESSAGE);
			System.exit(2);
		}

		try {
			createAndShowGUI(fontScale, visible);
		} catch (RuntimeException | ExceptionInInitializerError e) {
			String message = e.getMessage();
			if (message == null && e.getCause() != null)
				message = e.getCause().toString();
			JOptionPane.showMessageDialog(null, message,
					"Fatal error", JOptionPane.ERROR_MESSAGE);
			core.die("%s", e);
		}
	}
}
