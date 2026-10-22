package com.mihrab.tv.data;

import org.json.JSONArray;

public final class HomeSnapshot {
  public final JSONArray hadiths, verses;
  public final String retrieved;
  public final boolean demoPrayerTimes = true;
  public final String[] prayerNames = {"الفجر", "الظهر", "العصر", "المغرب", "العشاء"};
  public final String[] prayerTimes = {"05:26", "12:24", "15:36", "18:00", "19:17"};

  public HomeSnapshot(JSONArray hadiths, JSONArray verses, String retrieved) {
    this.hadiths = hadiths;
    this.verses = verses;
    this.retrieved = retrieved;
  }
}
