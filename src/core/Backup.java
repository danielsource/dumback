package core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class Backup {
	private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

	private Log log;

	Backup(Log log) {
		this.log = log;
	}

	void create(Path dest, List<Path> sourceDirs) throws IOException {
		if (!Files.exists(dest)) {
			log.debug("Creating destination directory: '%s'", dest);
			Files.createDirectories(dest);
		}

		if (!Files.isDirectory(dest)) {
			throw new IOException("Not a directory");
		}

		String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
		Path zipPath = dest.resolve("dumback_" + timestamp + ".zip");
		Path md5Path = dest.resolve("dumback_" + timestamp + ".md5");

		log.debug("Creating archive: '%s'", zipPath);

		try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
			for (Path dir : sourceDirs)
				zipDir(zos, dir);
		}

		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			log.error("%s", e);
			throw new RuntimeException(e);
		}
		byte digest[] = md.digest(Files.readAllBytes(zipPath));
		Files.writeString(md5Path,
				byteToHex(digest)
					.append("  ")
					.append(zipPath.getFileName())
					.toString());

		log.debug("The archive and checksum have been created");
		log.debug("END Creating archive");
	}

	Map<Path,Boolean> checkIntegrity(Path dest) throws IOException {
		Map<Path,Boolean> results = new HashMap<>();
		Files.list(dest)
			.filter(path -> {
				String s = path.getFileName().toString();
				return s.startsWith("dumback_") && s.endsWith(".zip");
			})
			.forEach(zip -> {
				String zipName = zip.getFileName().toString();
				Path md5 = dest.resolve(zipName.replace(".zip", ".md5"));
				try {
					String parts[] = Files.readString(md5).split("  ");
					MessageDigest md = MessageDigest.getInstance("MD5");
					byte digest[] = md.digest(Files.readAllBytes(zip));
					String sum = byteToHex(digest).toString();
					log.debug("md5sum: %s %s %s", dest, sum, parts[0]);
					boolean valid = parts[0].equals(sum);
					results.put(zip, valid);
				} catch (IOException e) {
					log.error("The file '%s' does not have a valid .md5: %s",
							zip, e.getMessage());
					results.put(zip, false);
				} catch (Exception e) {
					log.error("When checking integrity of '%s': %s",
							zip, e.getMessage());
					results.put(zip, false);
				}
			});
		return results;
	}

	private void zipDir(ZipOutputStream zos, Path dir) throws IOException {
		log.debug("Zipping '%s':", dir);
		Files.walk(dir).forEach(path -> {
			try {
				if (!Files.isDirectory(path)) {
					String f = dir.relativize(path).toString();
					log.debug("  %s", f);
					zos.putNextEntry(new ZipEntry(f));
					Files.copy(path, zos);
					zos.closeEntry();
				}
			} catch (IOException e) {
				log.error("When zipping '%s': %s", dir, e);
				throw new RuntimeException(e);
			}
		});
	}

	private static StringBuilder byteToHex(byte bytes[]) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes)
			sb.append(String.format("%02x", b));
		return sb;
	}
}
