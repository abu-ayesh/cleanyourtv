package com.mihrab.tv.core;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;

/** CSS-like light-catching glass; shared bitmap sampling replaces costly live backdrop blur. */
public final class GlassDrawable extends Drawable {
  private final View owner;
  private final WallpaperView wallpaper;
  private final float radius, density;
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final int tint;
  private boolean glass = true, gold;

  public GlassDrawable(View owner, WallpaperView wallpaper, float radius, int tint) {
    this.owner = owner;
    this.wallpaper = wallpaper;
    this.tint = tint;
    density = owner.getResources().getDisplayMetrics().density;
    this.radius = radius * density;
  }

  public void setGlass(boolean value) {
    glass = value;
    invalidateSelf();
  }

  public void setGold(boolean value) {
    gold = value;
    invalidateSelf();
  }

  @Override
  public void draw(Canvas c) {
    RectF box = new RectF(getBounds());
    wallpaper.drawGlass(c, owner, radius, glass);
    paint.setColor(glass ? tint : 0xee0c1827);
    c.drawRoundRect(box, radius, radius, paint);
    paint.setShader(
        new LinearGradient(
            0,
            0,
            box.width(),
            box.height(),
            new int[] {0x24ffffff, 0x06ffffff, 0x0bcadef5},
            new float[] {0, .42f, 1},
            Shader.TileMode.CLAMP));
    c.drawRoundRect(box, radius, radius, paint);
    paint.setShader(null);
    if (owner.isFocused()) {
      paint.setColor(0x3c80d6ff);
      c.drawRoundRect(box, radius, radius, paint);
    }
    if (gold) {
      paint.setColor(0x1aefd6a9);
      c.drawRoundRect(box, radius, radius, paint);
    }
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(density);
    paint.setColor(gold ? 0x80eed9b4 : 0x40f0f4ff);
    box.inset(density / 2, density / 2);
    c.drawRoundRect(box, radius, radius, paint);
    if (owner.isFocused()) {
      paint.setColor(0xffb6e9ff);
      paint.setStrokeWidth(2 * density);
      box.inset(-4 * density, -4 * density);
      c.drawRoundRect(box, radius + 4 * density, radius + 4 * density, paint);
    }
    paint.setStyle(Paint.Style.FILL);
  }

  @Override
  public boolean isStateful() {
    return true;
  }

  @Override
  protected boolean onStateChange(int[] state) {
    invalidateSelf();
    return true;
  }

  @Override
  public void setAlpha(int alpha) {}

  @Override
  public void setColorFilter(ColorFilter filter) {}

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }
}
