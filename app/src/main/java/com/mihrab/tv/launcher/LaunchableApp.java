package com.mihrab.tv.launcher;

import android.content.ComponentName;
import android.graphics.drawable.Drawable;

public final class LaunchableApp {
  public final String key, label, category;
  public final ComponentName component;
  public final Drawable icon;

  public LaunchableApp(ComponentName component, String label, Drawable icon, String category) {
    this.component = component;
    this.key = component.flattenToString();
    this.label = label;
    this.icon = icon;
    this.category = category;
  }
}
