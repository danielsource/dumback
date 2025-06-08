package core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static core.I18n.i18n;

class Backup {
	private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
	private static final String ARCHIVE_PREFIX = "dumback_";
	private static final String ARCHIVE_SUFFIX = ".zip";

	private Log log;
	private MessageDigest md;

	Backup(Log log) {
		this.log = log;

		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			log.error("%s", e);
			throw new RuntimeException(e);
		}
	}

	void create(Path dest, List<Path> sourceDirs) throws IOException {
		if (!Files.exists(dest)) {
			log.debug("Creating destination directory: '%s'", dest);
			Files.createDirectories(dest);
		}

		if (!Files.isDirectory(dest)) {
			throw new IOException(i18n("error.Not_dir"));
		}

		String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
		Path zipPath = dest.resolve(ARCHIVE_PREFIX + timestamp + ARCHIVE_SUFFIX);
		Path md5Path = dest.resolve(ARCHIVE_PREFIX + timestamp + ".md5");

		log.debug("Creating archive: '%s'", zipPath);

		try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
			for (Path dir : sourceDirs)
				zipDir(zos, dir);
		}

		Files.writeString(md5Path,
				byteToHex(computeMd5(md, zipPath))
				.append("  ")
				.append(zipPath.getFileName())
				.append(System.lineSeparator())
				.toString());

		log.debug("The archive and checksum have been created");
		log.debug("END Creating archive");
	}

	Map<Path,Boolean> checkIntegrity(Path dest) throws IOException {
		Map<Path,Boolean> results = new HashMap<>();
		Files.list(dest)
			.filter(path -> {
				String s = path.getFileName().toString();
				return s.startsWith(ARCHIVE_PREFIX) && s.endsWith(ARCHIVE_SUFFIX);
			})
		.forEach(zip -> {
			String zipName = zip.getFileName().toString();
			Path md5 = dest.resolve(zipName.replace(".zip", ".md5"));
			try {
				String parts[] = Files.readString(md5).split("  ");
				String sum = byteToHex(computeMd5(md, zip)).toString();
				log.debug("md5sum: %s %s %s", zipName, sum, parts[0]);
				boolean valid = parts[0].equals(sum);
				results.put(zip, valid);
			} catch (IOException e) {
				log.error(i18n("error.Not_a_valid_md5"),
						zip, e.getMessage());
				results.put(zip, false);
			}
		});
		return results;
	}

	private void zipDir(ZipOutputStream zos, Path dir) throws IOException {
		log.debug("Zipping '%s':", dir);
		Path root = dir.getFileName();
		try {
			Files.walk(dir).forEach(path -> {
				try {
					if (!Files.isDirectory(path)) {
						Path par = dir.getParent();
						if (par == null)
							par = dir;
						String f = par.relativize(path).toString();
						log.debug("  %s", f);
						zos.putNextEntry(new ZipEntry(f));
						Files.copy(path, zos);
						zos.closeEntry();
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		} catch (Exception e) {
			throw new IOException(i18n("error.When_zipping", dir, e), e);
		}
	}

	void deleteOld(Path dest, int keepDays) {
		if (!Files.isDirectory(dest))
			return;

		log.debug("Checking for old backups in: '%s'", dest);

		Date today = new Date();
		List<Path> toDelete = new ArrayList<>();

		try {
			Files.list(dest)
				.filter(path -> {
					String name = path.getFileName().toString();
					return name.startsWith(ARCHIVE_PREFIX) && name.endsWith(ARCHIVE_SUFFIX);
				})
			.forEach(path -> {
				String name = path.getFileName().toString();
				int ARCHIVE_PREFIXLen = ARCHIVE_PREFIX.length();
				int underscore = name.indexOf('_', ARCHIVE_PREFIXLen);
				if (underscore == -1) {
					log.error(i18n("error.Invalid_backup_timestamp"), name);
					return;
				}
				String datePart = name.substring(ARCHIVE_PREFIXLen, underscore);
				try {
					Date fileDate = new Date(datePart);
					if (fileDate.daysBetween(today) > keepDays) {
						toDelete.add(path);
						Path md5 = dest.resolve(name.replace(ARCHIVE_SUFFIX, ".md5"));
						if (Files.exists(md5))
							toDelete.add(md5);
					}
				} catch (Exception e) {
					log.error(i18n("error.Invalid_backup_date"), datePart, name);
				}
			});

			for (Path file : toDelete) {
				try {
					Files.deleteIfExists(file);
					log.debug("Deleted old file: '%s'", file);
				} catch (IOException e) {
					log.error(i18n("error.Failed_to_delete"), file, e.getMessage());
				}
			}
		} catch (IOException e) {
			log.error(i18n("error.Failed_to_access"), dest, e.getMessage());
		}
	}


	private static byte[] computeMd5(MessageDigest md, Path filePath) throws IOException {
		byte buffer[] = new byte[1024 * 1024];
		md.reset();
		try (InputStream is = Files.newInputStream(filePath);
				DigestInputStream dis = new DigestInputStream(is, md)) {
			while (dis.read(buffer) != -1);
				}
		return md.digest();
	}

	private static StringBuilder byteToHex(byte bytes[]) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes)
			sb.append(String.format("%02x", b));
		return sb;
	}
}
