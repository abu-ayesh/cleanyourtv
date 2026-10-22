package com.mihrab.tv.launcher;

import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import java.text.Collator;
import java.util.*;

/** Adapted from CleanYourTV AppAdapter: TV intents first, explicit launch, own package excluded. */
public final class AppRepository {
  private final Context context;

  public AppRepository(Context context) {
    this.context = context.getApplicationContext();
  }

  public List<LaunchableApp> load() {
    PackageManager pm = context.getPackageManager();
    Map<String, LaunchableApp> unique = new LinkedHashMap<>();
    for (String category :
        new String[] {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
      for (ResolveInfo result :
          pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(category), 0)) {
        ActivityInfo info = result.activityInfo;
        if (info == null
            || !info.exported
            || !info.enabled
            || !info.applicationInfo.enabled
            || info.packageName.equals(context.getPackageName())) continue;
        if (!safeComponent(info.packageName, info.name) || unique.containsKey(info.packageName))
          continue;
        try {
          android.graphics.drawable.Drawable icon = result.loadIcon(pm);
          Bitmap image = Bitmap.createBitmap(112, 112, Bitmap.Config.ARGB_8888);
          icon.setBounds(0, 0, 112, 112);
          icon.draw(new Canvas(image));
          unique.put(
              info.packageName,
              new LaunchableApp(
                  new ComponentName(info.packageName, info.name),
                  cleanLabel(String.valueOf(result.loadLabel(pm)), info.packageName),
                  new BitmapDrawable(context.getResources(), image),
                  category));
        } catch (RuntimeException ignored) {
          /* Package may disappear during a refresh. */
        }
      }
    }
    List<LaunchableApp> apps = new ArrayList<>(unique.values());
    Collator collator = Collator.getInstance(Locale.forLanguageTag("ar"));
    apps.sort(
        (a, b) -> {
          int c = collator.compare(a.label, b.label);
          return c != 0 ? c : a.key.compareTo(b.key);
        });
    return apps;
  }

  public static boolean safeComponent(String pkg, String activity) {
    String name = (pkg + "." + activity).toLowerCase(Locale.ROOT);
    return !name.contains("factory")
        && !name.contains("hkcfac")
        && !name.contains("engineering")
        && !name.contains("engineermode");
  }

  public static String cleanLabel(String label, String fallback) {
    String cleaned = label.replaceAll("[\\p{Cc}\\p{Cf}\\p{Zl}\\p{Zp}]", "").trim();
    return cleaned.isEmpty() ? fallback : cleaned.substring(0, Math.min(cleaned.length(), 80));
  }

  public Intent intent(LaunchableApp app) {
    return new Intent(Intent.ACTION_MAIN)
        .addCategory(app.category)
        .setComponent(app.component)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
  }
}
