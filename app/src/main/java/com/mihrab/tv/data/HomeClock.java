package com.mihrab.tv.data;

import java.time.*;
import java.time.chrono.HijrahDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class HomeClock {
  public static final ZoneId ZONE = ZoneId.of("Asia/Hebron");
  private static final Locale AR = Locale.forLanguageTag("ar-PS");

  public static String numbers(String text, boolean arabic) {
    if (!arabic) return text;
    StringBuilder out = new StringBuilder();
    for (char c : text.toCharArray()) out.append(c >= '0' && c <= '9' ? (char) ('٠' + c - '0') : c);
    return out.toString();
  }

  public static ZonedDateTime local(Instant now) {
    return now.atZone(ZONE);
  }

  public static String clock(Instant now, boolean h24, boolean arabic) {
    return numbers(
        local(now).format(DateTimeFormatter.ofPattern(h24 ? "HH:mm" : "hh:mm a", AR)), arabic);
  }

  public static String time(String hhmm, boolean h24, boolean arabic) {
    return numbers(
        LocalTime.parse(hhmm).format(DateTimeFormatter.ofPattern(h24 ? "HH:mm" : "hh:mm a", AR)),
        arabic);
  }

  public static String gregorian(Instant now, boolean arabic) {
    return numbers(local(now).format(DateTimeFormatter.ofPattern("EEEE، d MMMM uuuu", AR)), arabic);
  }

  public static String hijri(Instant now, boolean arabic) {
    return numbers(
        HijrahDate.from(local(now)).format(DateTimeFormatter.ofPattern("d MMMM uuuu", AR)) + " هـ",
        arabic);
  }

  public static int dayIndex(Instant now, int count) {
    return Math.floorMod(local(now).toLocalDate().toEpochDay(), count);
  }

  public static boolean morning(Instant now) {
    int hour = local(now).getHour();
    return hour >= 4 && hour < 16;
  }

  public static boolean friday(Instant now) {
    return local(now).getDayOfWeek() == DayOfWeek.FRIDAY;
  }

  private HomeClock() {}
}
