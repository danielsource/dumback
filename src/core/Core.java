package core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import static core.I18n.i18n;

public class Core {
	private final Path appPath;

	private final Log log;
	private final Config config;
	private final Backup backup;
	private volatile boolean isBackupInProgress;
	private volatile Timer backupTimer;

	public Core() {
		String appDirname = ".dumback";
		String userHome = System.getProperty("user.home");

		try {
			appPath = Path.of(userHome, appDirname);
			Files.createDirectories(appPath);
		} catch (InvalidPathException | IOException e) {
			String message = String.format(i18n("error.Couldnt_create_dir_in"),
					appDirname, userHome, e.getMessage());
			throw new RuntimeException(message, e);
		}

		log = new Log(appPath.resolve("dumback.log"));
		config = new Config(appPath.resolve("dumback.cfg"), log);
		backup = new Backup(log);

		isBackupInProgress = false;
		initAutoBackup();

		log.debug("Dumback is initialized");
	}

	public ConfigEntries getConfig() {
		return config.cfg;
	}

	public void updateConfig(ConfigEntries cfg) {
		if (isBackupInProgress) {
			log.error(i18n("error.Backup_in_progress_cant_update_cfg"));
			return;
		}

		config.updateConfig(cfg);
		if (cfg.freqDays > 0) {
			log.info(i18n("info.Auto_backup_enabled"), cfg.freqDays);
			initAutoBackup();
		} else {
			stopAutoBackup();
		}
	}

	public void backup() {
		if (isBackupInProgress) {
			log.error(i18n("error.Backup_in_progress_cant_backup"));
			return;
		}

		ConfigEntries cfg = getConfig();

		if (cfg.destPath == null) {
			log.error(i18n("error.Configure_dest_dir"));
			return;
		}

		isBackupInProgress = true;
		try {
			backup.create(cfg.destPath, cfg.dirsToBackup);
			log.info(i18n("info.Backup_success"));
		} catch (IOException e) {
			String message = i18n("error.Couldnt_create_archive_in",
					cfg.destPath, e.getMessage());
			log.error("%s", message);
		}
		config.updateConfig(new ConfigEntries(
					new Date(),
					cfg.destPath,
					cfg.freqDays,
					cfg.keepDays,
					cfg.dirsToBackup
					));
		if (cfg.keepDays > 0)
			backup.deleteOld(cfg.destPath, cfg.keepDays);
		isBackupInProgress = false;
	}

	public Map<Path,Boolean> checkIntegrity() {
		ConfigEntries cfg = getConfig();

		if (!Files.exists(cfg.destPath))
			return null;

		try {
			return backup.checkIntegrity(cfg.destPath);
		} catch (Exception e) {
			log.error(i18n("error.When_verifying_integrity"), e.getMessage());
			return null;
		}
	}

	public void initAutoBackup() {
		ConfigEntries cfg = getConfig();

		if (backupTimer != null)
			backupTimer.cancel();

		if (cfg.destPath == null || cfg.freqDays <= 0)
			return;

		backupTimer = new Timer();
		long initialDelay = 1000;
		final long daysInMs = 86400000L;

		if (cfg.lastBackup != null) {
			long daysPassed = cfg.lastBackup.daysBetween(new Date());
			if (daysPassed < cfg.freqDays)
				initialDelay = (cfg.freqDays - daysPassed) * daysInMs;
		}

		log.debug("Automatic backup initial delay: %d ms", initialDelay);

		backupTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					if (isBackupInProgress)
						return;

					log.info(i18n("info.Auto_backup_starting"));
					backup();

					Map<Path,Boolean> result = checkIntegrity();
					if (result != null)
						result.forEach((bak, isValid) -> {
							if (!isValid)
								log.error(i18n("error.File_appear_corrupted"), bak);
						});
				} catch (Exception e) {
					log.error(i18n("error.Auto_backup_failed"), e.getMessage());
				}

			}
		}, initialDelay, cfg.freqDays * daysInMs);
	}

	public void stopAutoBackup() {
		if (backupTimer != null)
			backupTimer.cancel();
		backupTimer = null;
	}

	public void die(String message, Object... args) {
		log.error("Fatal: " + message, args);
		stopAutoBackup();
		System.exit(1);
	}
}
