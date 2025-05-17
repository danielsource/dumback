package core;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

class Log {
	enum Level { DEBUG, INFO, ERROR }

	private static final long TRUNCATION_SIZE = 2*1024*1024;
	private static final String INITIAL_MESSAGE = String.format("# Dumback log%n");

	private final Path logPath;

	Log(Path logPath) throws IOException {
		this.logPath = logPath;

		if (!Files.exists(logPath))
			Files.writeString(logPath, INITIAL_MESSAGE, StandardOpenOption.CREATE);
		else if (Files.size(logPath) > TRUNCATION_SIZE)
			truncateLog();
		else
			Files.writeString(logPath, String.format("%n"), StandardOpenOption.APPEND);
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
		String timestamp = LocalDateTime.now().format(Core.TIMESTAMP_FORMAT);
		String message = String.format(format, args);
		StringWriter sw = new StringWriter(message.length());
		PrintWriter pw = new PrintWriter(sw);

		pw.printf("[%s] %-5s - %s%n", timestamp, level, message);

		if (level == Level.ERROR) {
			if (args.length > 0 && args[args.length-1] instanceof Throwable)
				((Throwable)args[args.length-1]).printStackTrace(pw);
			pw.flush();
			System.err.printf(sw.toString());
		} else {
			pw.flush();
		}

		try {
			Files.writeString(logPath, sw.toString(), StandardOpenOption.APPEND);
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
