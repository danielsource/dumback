package core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Map;

public class Core {
	private final Path appPath;

	private final Log log;
	private final Config config;
	private final Backup backup;

	public Core() {
		String appDirname = ".dumback";
		String userHome = System.getProperty("user.home");
		try {
			appPath = Path.of(userHome, appDirname);
			Files.createDirectories(appPath);
		} catch (InvalidPathException | IOException e) {
			String message = String.format("Couldn't create the '%s' directory in '%s': %s",
					appDirname, userHome, e.getMessage());
			throw new RuntimeException(message, e);
		}

		log = new Log(appPath.resolve("dumback.log"));
		config = new Config(appPath.resolve("dumback.cfg"), log);
		backup = new Backup(log);

		log.debug("Dumback is initialized");
	}

	public ConfigEntries getConfig() {
		return config.cfg;
	}

	public ConfigEntries updateConfig(ConfigEntries cfg) {
		config.updateConfig(cfg);
		return cfg;
	}

	public void backup() {
		try {
			backup.create(config.cfg.destPath, config.cfg.dirsToBackup);
			log.info("Backup completed successfully!");
		} catch (IOException e) {
			String message = String.format("Couldn't create a new archive in '%s': %s",
					config.cfg.destPath, e.getMessage());
			log.error("%s", message);
		}
	}

	public Map<Path,Boolean> checkIntegrity() {
		try {
			return backup.checkIntegrity(config.cfg.destPath);
		} catch (IOException e) {
			log.error("When verifying the integrity: %s", e.getMessage());
			return null;
		}
	}
}
