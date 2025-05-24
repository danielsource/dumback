package core;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class Log {
	enum Level { DEBUG, INFO, ERROR }

	private static final long TRUNCATION_SIZE = 2*1024*1024;
	private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final String INITIAL_MESSAGE = String.format("# Dumback log%n");

	private final Path logPath;

	Log(Path logPath) {
		this.logPath = logPath;

		try {
			if (!Files.exists(logPath))
				Files.writeString(logPath, INITIAL_MESSAGE, StandardOpenOption.CREATE);
			else if (Files.size(logPath) > TRUNCATION_SIZE)
				truncateLog();
		} catch (IOException e) {
			String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
			String message = String.format("Couldn't write '%s': %s",
					logPath, e.getMessage());
			System.err.printf("[%s] LOG ERROR - %s%n", timestamp, message, e.getMessage());
			throw new RuntimeException(message, e);
		}
	}

	void debug(String format, Object... args) {
		log(Level.DEBUG, format, args);
	}

	void info(String format, Object... args) {
		log(Level.INFO, format, args);
	}

	void error(String format, Object... args) {
		log(Level.ERROR, format, args);
	}

	private void log(Level level, String format, Object... args) {
		String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		String message;

		pw.printf("[%s] %-5s - %s%n", timestamp, level, String.format(format, args));

		if (level != Level.DEBUG) {
			if (args.length > 0 && args[args.length-1] instanceof Throwable)
				((Throwable)args[args.length-1]).printStackTrace(pw);
			pw.flush();
			message = sw.toString();
			System.err.print(message);
		} else {
			pw.flush();
			message = sw.toString();
		}

		try {
			Files.writeString(logPath, message, StandardOpenOption.APPEND);
		} catch (IOException e) {
			System.err.printf("[%s] LOG ERROR - %s%n",
					timestamp, e.getMessage());
		}
	}

	private void truncateLog() throws IOException {
		String content = Files.readString(logPath);
		int keep = (int)Math.min(content.length(), TRUNCATION_SIZE / 2);
		int start = content.lastIndexOf("\n", content.length() - keep);

		String truncated = INITIAL_MESSAGE;
		if (start == -1)
			truncated += content;
		else
			truncated += content.substring(start + 1);

		Files.writeString(logPath, truncated, StandardOpenOption.TRUNCATE_EXISTING);
	}
}
