package com.mihrab.tv.core;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

/** Holds one wallpaper and one preblurred map. Decoding happens on the activity worker. */
public final class WallpaperView extends View {
  private Bitmap wallpaper, glass;
  private int scene = 5;
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
  private static final float[] POSITIONS = {.54f, .56f, .55f, .52f, .5f, .54f};
  private static final float[] DIM = {.02f, .04f, .04f, .15f, .1f, .12f};

  public WallpaperView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public void setScene(int id, Bitmap image, Bitmap blurred) {
    scene = id;
    wallpaper = image;
    glass = blurred;
    invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    canvas.drawColor(Color.rgb(13, 20, 32));
    if (wallpaper == null) return;
    float scale =
        Math.max(
            getWidth() / (float) wallpaper.getWidth(), getHeight() / (float) wallpaper.getHeight());
    float w = wallpaper.getWidth() * scale, h = wallpaper.getHeight() * scale;
    canvas.drawBitmap(
        wallpaper,
        null,
        new RectF(
            (getWidth() - w) / 2,
            (getHeight() - h) * POSITIONS[scene - 1],
            (getWidth() + w) / 2,
            (getHeight() - h) * POSITIONS[scene - 1] + h),
        paint);
    shade(canvas, getWidth(), getHeight(), 0, 0);
    float density = getResources().getDisplayMetrics().density;
    for (float x = 0; x < getWidth() * .75f; x += 91 * density) {
      paint.setColor(Color.argb(Math.round(6 * (1 - x / (getWidth() * .75f))), 196, 212, 225));
      for (float y = 0; y < getHeight(); y += 83 * density) canvas.drawCircle(x, y, density, paint);
    }
    paint.setColor(Color.WHITE);
  }

  public void drawGlass(Canvas canvas, View owner, float radius, boolean enabled) {
    int width = owner.getWidth(), height = owner.getHeight();
    Path clip = new Path();
    clip.addRoundRect(new RectF(0, 0, width, height), radius, radius, Path.Direction.CW);
    int save = canvas.save();
    canvas.clipPath(clip);
    if (enabled && glass != null) {
      float x = 0, y = 0;
      View current = owner;
      while (current != getParent() && current != null) {
        x += current.getLeft();
        y += current.getTop();
        current = current.getParent() instanceof View ? (View) current.getParent() : null;
      }
      canvas.drawBitmap(glass, null, new RectF(-x, -y, getWidth() - x, getHeight() - y), paint);
      shade(canvas, getWidth(), getHeight(), -x, -y);
    } else {
      canvas.drawColor(Color.rgb(12, 24, 39));
    }
    canvas.restoreToCount(save);
  }

  private void shade(Canvas c, float w, float h, float x, float y) {
    paint.setShader(
        new LinearGradient(
            0,
            y,
            0,
            y + h,
            new int[] {0x8c050c17, 0x21050c17, 0x33050c17, 0x99050c17},
            new float[] {0, .35f, .6f, 1},
            Shader.TileMode.CLAMP));
    c.drawRect(0, 0, c.getWidth(), c.getHeight(), paint);
    paint.setShader(
        new LinearGradient(
            x + w, 0, x + w * .25f, 0, 0x59050c17, 0x0f050c17, Shader.TileMode.CLAMP));
    c.drawRect(0, 0, c.getWidth(), c.getHeight(), paint);
    paint.setShader(null);
    c.drawColor(Color.argb(Math.round(DIM[scene - 1] * 255), 3, 10, 20));
  }
}
