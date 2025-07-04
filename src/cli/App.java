package cli;

import core.ConfigEntries;
import core.Core;
import core.Date;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class App {
	private static Core core;
	private static Scanner sc;

	private static void displayMenu() {
		System.out.println("\n==== Dumback - simple backups ====\n");
		System.out.println("1. Perform backup now");
		System.out.println("2. Configure backup settings");
		System.out.println("3. Check backup status");
		System.out.println("4. Exit");
		System.out.print("Select an option: ");
	}

	private static void runCommandLine() {
		sc = new Scanner(System.in);

		while (true) {
			displayMenu();
			int opt = sc.nextInt();
			sc.nextLine();

			switch (opt) {
			case 1:
				backupNow();
				break;
			case 2:
				configure();
				break;
			case 3:
				checkStatus();
				break;
			case 4:
				System.out.println("\nExiting...");
				sc.close();
				return;
			default:
				System.out.println("Invalid option. Please try again.");
			}

		}
	}

	private static void backupNow() {
		System.out.println("\n==== Starting backup ====\n");

		core.backup();
	}

	private static void configure() {
		ConfigEntries cfg = core.getConfig();

		System.out.println("\n==== Backup configuration ====");

		System.out.printf("%nCurrent destination: %s%n",
				(cfg.destPath != null) ? cfg.destPath : "Not set");
		System.out.print("Enter new path (blank to keep current): ");
		String destPath = sc.nextLine();

		System.out.printf("%nAutomatic backup frequency: %s%n",
				(cfg.freqDays == 0) ? "Disabled" : cfg.freqDays + " day(s)");
		System.out.print("Enter new frequency (0 for manual only): ");
		int freqDays = sc.nextInt();

		System.out.printf("%nKeep backups: %s%n",
				(cfg.keepDays == 0) ? "Keep forever" : cfg.keepDays + " day(s)");
		System.out.print("Enter new retention days (0 for forever): ");
		int keepDays = sc.nextInt();
		sc.nextLine();

		if (cfg.dirsToBackup.isEmpty()) {
			System.out.println("\nEnter directories to backup (one directory per line, blank to finish):");
		} else {
			System.out.println("\nCurrent directories:");
			for (Path dir : cfg.dirsToBackup)
				System.out.println(dir);
			System.out.println("\nReplace the current ones (one directory per line, blank to finish/SKIP):");
		}

		List<Path> dirs = new ArrayList<>();
		String dir = sc.nextLine();
		if (!dir.isEmpty()) {
			do {
				dirs.add(Path.of(dir));
				dir = sc.nextLine();
			} while (!dir.isEmpty());
		} else {
			dirs = cfg.dirsToBackup;
		}

		try {
			core.updateConfig(new ConfigEntries(
						cfg.lastBackup,
						destPath.isEmpty() ? cfg.destPath : Path.of(destPath),
						freqDays,
						keepDays,
						dirs));
		} catch (IllegalArgumentException e) {
			System.out.println("Invalid setting, try again: " + e.getMessage());
		}
	}

	private static void checkStatus() {
		ConfigEntries cfg = core.getConfig();

		if (cfg.destPath == null) {
			System.out.println("\nPlease configure the destination directory.");
			return;
		}

		System.out.println("\n==== Backup status ====\n");
		System.out.printf("Last backup: %s%n",
				(cfg.lastBackup != null) ? cfg.lastBackup : "Never");

		System.out.printf("Destination: %s%n", cfg.destPath);

		if (cfg.freqDays > 0) {
			System.out.printf("Automatic backups: Every %d days%n", cfg.freqDays);
			System.out.printf("Keep backups: %s%n",
					(cfg.keepDays == 0) ? "Forever" : cfg.keepDays + " days");
		} else {
			System.out.println("Automatic backups: Disabled");
		}

		System.out.println("\nDirectories to backup:");
		if (!cfg.dirsToBackup.isEmpty())
			for (Path dir : cfg.dirsToBackup)
				System.out.println(dir);
		else
			System.out.println("(None configured)");

		System.out.println("\nVerifying existing backups...");
		Map<Path,Boolean> result = core.checkIntegrity();
		if (result != null)
			result.forEach((bak, isValid) -> {
				System.out.printf("%s: %s%n",
						bak, isValid ? "ok" : "INVALID!");
			});
	}

	public static void main(String args[]) {
		if (args.length > 0) {
			System.err.println("Usage: This program does not accept arguments.");
			System.err.println("About: https://github.com/danielsource/dumback.git");
			System.exit(2);
		}

		try {
			core = new Core(null);
		} catch (RuntimeException e) {
			e.printStackTrace();
			System.err.printf("Fatal error on initialization: %s%n", e.getMessage());
			System.exit(2);
		}

		try {
			runCommandLine();
		} catch (RuntimeException e) {
			core.die("%s", e);
		} finally {
			core.stopAutoBackup();
		}
	}
}
