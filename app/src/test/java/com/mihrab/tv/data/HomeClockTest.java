package com.mihrab.tv.data;

import static org.junit.Assert.*;

import java.time.*;
import org.junit.Test;

public class HomeClockTest {
  @Test
  public void palestineDateChangesAtLocalMidnight() {
    Instant midnight = LocalDate.of(2026, 10, 23).atStartOfDay(HomeClock.ZONE).toInstant();
    assertNotEquals(
        HomeClock.dayIndex(midnight.minusSeconds(1), 7), HomeClock.dayIndex(midnight, 7));
    assertTrue(HomeClock.friday(midnight));
  }

  @Test
  public void dhikrSwitchesAtFourAndSixteen() {
    for (int hour : new int[] {3, 4, 15, 16, 23}) {
      Instant now = LocalDate.of(2026, 10, 22).atTime(hour, 0).atZone(HomeClock.ZONE).toInstant();
      assertEquals(hour >= 4 && hour < 16, HomeClock.morning(now));
    }
  }

  @Test
  public void formattingDoesNotMutateTimestamp() {
    Instant instant = Instant.parse("2026-10-22T08:05:00Z");
    assertTrue(HomeClock.clock(instant, true, false).matches("[0-9]{2}:[0-9]{2}"));
    assertTrue(HomeClock.clock(instant, true, true).matches("[٠-٩]{2}:[٠-٩]{2}"));
    assertEquals("2026-10-22T08:05:00Z", instant.toString());
  }

  @Test
  public void twelveHourMidnightIsTwelve() {
    assertTrue(HomeClock.time("00:05", false, false).startsWith("12:05"));
    assertEquals("23:10", HomeClock.time("23:10", true, false));
  }

  @Test
  public void clockHandlesDstTransitions() {
    var transition =
        HomeClock.ZONE.getRules().nextTransition(Instant.parse("2026-01-01T00:00:00Z"));
    assertNotNull(transition);
    Instant at = transition.getInstant();
    assertFalse(HomeClock.clock(at.minusSeconds(1), true, false).isEmpty());
    assertFalse(HomeClock.clock(at, true, true).isEmpty());
  }
}
