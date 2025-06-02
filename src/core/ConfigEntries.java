package core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConfigEntries {
	public final Date lastBackup;
	public final Path destPath;
	public final int freqDays; /* zero is auto backup disabled */
	public final int keepDays; /* zero is forever */
	public final List<Path> dirsToBackup;

	public ConfigEntries(
			Date lastBackup,
			Path destPath,
			int freqDays,
			int keepDays,
			List<Path> dirsToBackup) {
		this.lastBackup = lastBackup;
		this.destPath = destPath == null ? null : destPath.normalize().toAbsolutePath();
		if (freqDays < 0)
			throw new IllegalArgumentException("'freqDays' must be greater or equal to zero");
		this.freqDays = freqDays;
		if (keepDays < 0)
			throw new IllegalArgumentException("'keepDays' must be greater or equal to zero");
		this.keepDays = keepDays;

		List<Path> l = new ArrayList<>(dirsToBackup);
		l.replaceAll(Path::normalize);
		l.replaceAll(Path::toAbsolutePath);
		this.dirsToBackup = l;
	}

	public ConfigEntries() {
		this(null, null, 0, 0, new ArrayList<>());
	}

	@Override
	public String toString() {
		return ConfigEntries.class.getName() + "[" +
			"lastBackup=" + lastBackup + "," +
			"destPath=" + destPath + "," +
			"freqDays=" + freqDays + "," +
			"keepDays=" + keepDays + "]";
	}
}
