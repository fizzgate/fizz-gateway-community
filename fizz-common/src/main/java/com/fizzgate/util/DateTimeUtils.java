/*
 *  Copyright (C) 2020 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fizzgate.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.fizzgate.util.Consts.DP;

/**
 * @author hongqiaowei
 */

public abstract class DateTimeUtils {

	private static       Map<String, DateTimeFormatter> dateTimeFormatterMap = new HashMap<>();

	private static       ZoneId                         defaultZone          = ZoneId.systemDefault();

	@Deprecated
	private static final String                         zeroTimeSuffix       = " 00:00:00.000";

	private DateTimeUtils() {
	}

	public static DateTimeFormatter getDateTimeFormatter(String pattern) {
		DateTimeFormatter f = dateTimeFormatterMap.get(pattern);
		if (f == null) {
			f = DateTimeFormatter.ofPattern(pattern);
			dateTimeFormatterMap.put(pattern, f);
		}
		return f;
	}

	public static long toMillis(LocalDate ld) {
		LocalDateTime ldt = ld.atStartOfDay();
		return toMillis(ldt);
	}

	public static long toMillis(LocalDateTime ldt) {
		return ldt.atZone(defaultZone).toInstant().toEpochMilli();
	}

	@Deprecated
	public static long toMillis(String dateTime, String... pattern) {
		if (dateTime.length() == 10) {
			dateTime += zeroTimeSuffix;
		}
		String p = DP.DP23;
		if (pattern.length != 0) {
			p = pattern[0];
		}
		DateTimeFormatter f = getDateTimeFormatter(p);
		LocalDateTime ldt = LocalDateTime.parse(dateTime, f);
		return toMillis(ldt);
	}

	public static long toMillis(String dateTime, String pattern) {
		DateTimeFormatter f = getDateTimeFormatter(pattern);
		LocalDateTime ldt = LocalDateTime.parse(dateTime, f);
		return toMillis(ldt);
	}

	public static LocalDate transform(Date date) {
		return date.toInstant().atZone(defaultZone).toLocalDate();
	}

	public static LocalDateTime transform(long l) {
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(l), defaultZone);
	}

	public static LocalDateTime localDateTimeFrom(Date date) {
		return date.toInstant().atZone(defaultZone).toLocalDateTime();
	}

	public static Date from(Instant i) {
		return new Date(i.toEpochMilli());
	}

	public static Date from(LocalDate localDate) {
		return Date.from(localDate.atStartOfDay().atZone(defaultZone).toInstant());
	}

	public static Date from(LocalDateTime localDateTime) {
		return Date.from(localDateTime.atZone(defaultZone).toInstant());
	}

	@Deprecated
	public static String convert(long mills, String... pattern) {
		String p = DP.DP10;
		if (pattern.length != 0) {
			p = pattern[0];
		}
		LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(mills), defaultZone);
		DateTimeFormatter f = getDateTimeFormatter(p);
		return ldt.format(f);
	}

	public static String convert(long mills, String pattern) {
		LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(mills), defaultZone);
		DateTimeFormatter f = getDateTimeFormatter(pattern);
		return ldt.format(f);
	}

	@Deprecated
	public static String convert(LocalDate date, String... pattern) {
		String p = DP.DP10;
		if (pattern.length != 0) {
			p = pattern[0];
		}
		DateTimeFormatter f = getDateTimeFormatter(p);
		return date.format(f);
	}

	public static String convert(LocalDate date, String pattern) {
		DateTimeFormatter f = getDateTimeFormatter(pattern);
		return date.format(f);
	}

	@Deprecated
	public static String convert(LocalDateTime localDateTime, String... pattern) {
		String p = DP.DP23;
		if (pattern.length != 0) {
			p = pattern[0];
		}
		DateTimeFormatter f = getDateTimeFormatter(p);
		return localDateTime.format(f);
	}

	public static String convert(LocalDateTime localDateTime, String pattern) {
		DateTimeFormatter f = getDateTimeFormatter(pattern);
		return localDateTime.format(f);
	}

	public static List<String> datesBetween(String start, String end) {
		LocalDate sd = LocalDate.parse(start);
		LocalDate ed = LocalDate.parse(end);
		long dist = ChronoUnit.DAYS.between(sd, ed);
		if (dist == 0) {
			return Collections.emptyList();
		} else if (dist < 0) {
			LocalDate d = ed;
			ed = sd;
			sd = d;
			dist = Math.abs(dist);
		}
		long max = dist + 1;
		return Stream.iterate(sd, d -> {
			return d.plusDays(1);
		}).limit(max).map(LocalDate::toString).collect(Collectors.toList());
	}

	public static List<LocalDate> datesBetween(LocalDate sd, LocalDate ed) {
		long numOfDaysBetween = ChronoUnit.DAYS.between(sd, ed);
		return IntStream.iterate(0, i -> i + 1)
						.limit(numOfDaysBetween)
						.mapToObj(sd::plusDays)
						.collect(Collectors.toList());
	}

	public static LocalDate beforeNow(long offsetDays) {
		return LocalDate.now().minusDays(offsetDays);
	}

	public static LocalDateTime beforeNowNoTime(long offsetDays) {
		return LocalDate.now().minusDays(offsetDays).atStartOfDay();
	}

	@Deprecated
	public static LocalDateTime time2zero(LocalDateTime ldt) {
		return ldt.withHour(0).withMinute(0).withSecond(0).with(ChronoField.MILLI_OF_SECOND, 0);
	}

	public static long get10sTimeWinStart(int n) {
		LocalDateTime now = LocalDateTime.now().with(ChronoField.MILLI_OF_SECOND, 0);
		int sec = now.getSecond();
		long interval;
		if (sec > 49) {
			interval = sec - 50;
		} else if (sec > 39) {
			interval = sec - 40;
		} else if (sec > 29) {
			interval = sec - 30;
		} else if (sec > 19) {
			interval = sec - 20;
		} else if (sec > 9) {
			interval = sec - 10;
		} else {
			interval = sec;
		}
		long millis = toMillis(now);
		return millis - interval * 1000 - (n - 1) * 10L * 1000;
	}

    /*
    void iterateBetweenDatesJava8(LocalDate start, LocalDate end) {
        for (LocalDate date = start; date.isBefore(end); date = date.plusDays(1)) {
            processDate(date);
        }
    }
    */
}
