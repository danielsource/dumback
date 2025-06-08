package core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import static core.I18n.i18n;

class Config {
	private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final Path configPath;
	private final Log log;

	ConfigEntries cfg = new ConfigEntries();

	Config(Path configPath, Log log) {
		this.configPath = configPath;
		this.log = log;

		try {
			if (Files.exists(configPath)) {
				readConfig();
			} else {
				writeConfig();
			}
		} catch (IOException e) {
			String message = i18n("error.Couldnt_read_or_write",
					configPath, e.getMessage());
			log.error("%s", message);
			throw new RuntimeException(message, e);
		}

		log.debug("%s", cfg);
	}

	void updateConfig(ConfigEntries cfg) {
		log.debug("Updating config: '%s' to '%s'", this.cfg, cfg);

		this.cfg = cfg;
		try {
			writeConfig();
		} catch (IOException e) {
			String message = i18n("error.Couldnt_write",
					configPath, e.getMessage());
			log.error("%s", message);
			throw new RuntimeException(message, e);
		}
	}

	private void readConfig() throws IOException {
		log.debug("Reading config: '%s'", configPath);

		String line;
		String entry[];
		boolean readDirs = false;

		Date lastBackup = null;
		Path destPath = null;
		int freqDays = 0;
		int keepDays = 0;
		List<Path> dirsToBackup = new ArrayList<>();

		try (BufferedReader r = Files.newBufferedReader(configPath)) {
			while ((line = r.readLine()) != null) {
				if (readDirs) {
					dirsToBackup.add(Path.of(line));
					continue;
				}

				line = line.trim();
				if (line.isEmpty() || line.startsWith("#"))
					continue;
				if (line.equals("[Directories]")) {
					readDirs = true;
					continue;
				}

				entry = line.split("=", 2);
				if (entry.length != 2) {
					log.error(i18n("error.Invalid_cfg_no_value"), entry[0]);
					continue;
				}
				entry[0] = entry[0].trim();
				entry[1] = entry[1].trim();

				switch (entry[0]) {
				case "lastBackup":
					try {
						lastBackup = new Date(entry[1]);
					} catch (DateTimeException e) {
						log.error(i18n("error.Invalid_cfg_date"), entry[0], entry[1]);
					}
					break;
				case "destPath":
					try {
						destPath = Path.of(entry[1]);
					} catch (InvalidPathException e) {
						log.error(i18n("error.Invalid_cfg_path"), entry[0], entry[1]);
					}
					break;
				case "freqDays":
					try {
						freqDays = Integer.parseInt(entry[1]);
					} catch (NumberFormatException e) {
						log.error(i18n("error.Invalid_cfg_number"), entry[0], entry[1]);
					}
					break;
				case "keepDays":
					try {
						keepDays = Integer.parseInt(entry[1]);
					} catch (NumberFormatException e) {
						log.error("error.Invalid_cfg_number", entry[0], entry[1]);
					}
					break;
				default:
					log.error(i18n("error.Unknown_cfg"), entry[0], entry[1]);
				}
			}
		}

		try {
			cfg = new ConfigEntries(lastBackup, destPath, freqDays, keepDays, dirsToBackup);
		} catch (IllegalArgumentException e) {
			log.error(i18n("error.Discarding_invalid_cfg"), e.getMessage());
		}

		log.debug("END Reading config");
	}

	private void writeConfig() throws IOException {
		log.debug("Writing config: '%s'", configPath);

		String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);

		try (BufferedWriter w = Files.newBufferedWriter(configPath,
					StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING)) {
			w.write(String.format("# Last edited automatically: %s%n", timestamp));

			if (cfg.lastBackup != null)
				w.write(String.format("lastBackup=%s%n", cfg.lastBackup));
			if (cfg.destPath != null)
				w.write(String.format("destPath=%s%n", cfg.destPath));
			w.write(String.format("freqDays=%d%n", cfg.freqDays));
			w.write(String.format("keepDays=%d%n", cfg.keepDays));

			if (!cfg.dirsToBackup.isEmpty()) {
				w.write(String.format("%n[Directories]%n"));
				for (Path dir : cfg.dirsToBackup)
					w.write(String.format("%s%n", dir));
			}
		}

		log.debug("END Writing config");
	}
}
