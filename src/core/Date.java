package core;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Date {
	public final LocalDate date;

	public Date() {
		this.date = LocalDate.now();
	}

	public Date(int year, int month, int day) throws DateTimeException {
		this.date = LocalDate.of(year, month, day);
	}

	public Date(String yyyymmdd) throws DateTimeException {
		String s = yyyymmdd.trim();
		try {
			int yearEnd = s.length() - 4;
			int year = Integer.parseInt(s.substring(0, yearEnd));
			int month = Integer.parseInt(s.substring(yearEnd, yearEnd + 2));
			int day = Integer.parseInt(s.substring(yearEnd + 2));
			this.date = LocalDate.of(year, month, day);
		} catch (IndexOutOfBoundsException | NumberFormatException | DateTimeException e) {
			throw new DateTimeException("Failed to parse YYYYMMDD date: " + yyyymmdd, e);
		}
	}

	public int getYear() {
		return date.getYear();
	}

	public int getMonth() {
		return date.getMonthValue();
	}

	public int getDay() {
		return date.getDayOfMonth();
	}

	public int daysBetween(Date other) {
		return (int)ChronoUnit.DAYS.between(date, other.date);
	}

	@Override
	public String toString() {
		return String.format("%04d%02d%02d", getYear(), getMonth(), getDay());
	}
}
