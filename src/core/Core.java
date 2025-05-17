package core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;

public class Core {
	public static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final Path appPath;

	private final Log log;
	private final Config config;
	private final Archive archive;

	public Core() {
		String appDirname = ".dumback";
		String userHome = System.getProperty("user.home");
		try {
			appPath = Path.of(userHome, appDirname);
			Files.createDirectories(appPath);
		} catch (InvalidPathException | IOException e) {
			throw new RuntimeException(
					String.format("Couldn't create the '%s' directory in '%s'",
						appDirname, userHome, appDirname), e);
		}

		Path logPath = appPath.resolve("dumback.log");
		try {
			log = new Log(appPath.resolve(logPath));
		} catch (IOException e) {
			throw new RuntimeException(
					String.format("Error reading or writing to '%s'",
						logPath), e);
		}

		Path configPath = appPath.resolve("dumback.cfg");
		try {
			config = new Config(configPath, log);
		} catch (IOException e) {
			throw new RuntimeException(
					String.format("Error reading or writing to '%s'",
						configPath), e);
		}

		archive = null;
	}

	public ConfigEntries getConfig() {
		return config.cfg;
	}

	public ConfigEntries updateConfig(ConfigEntries cfg) throws IOException {
		config.updateConfig(cfg);
		return cfg;
	}
}
