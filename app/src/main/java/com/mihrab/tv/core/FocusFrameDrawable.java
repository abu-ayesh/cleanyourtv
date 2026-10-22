package com.mihrab.tv.core;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Focus indication for text controls whose resting background is transparent or gold. */
public final class FocusFrameDrawable extends Drawable {
  private final View owner;
  private final Drawable resting;
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final float density;

  public FocusFrameDrawable(View owner, Drawable resting) {
    this.owner = owner;
    this.resting = resting;
    density = owner.getResources().getDisplayMetrics().density;
  }

  @Override
  public void draw(Canvas canvas) {
    if (resting != null) {
      resting.setBounds(getBounds());
      resting.draw(canvas);
    }
    if (owner.isFocused()) {
      RectF box = new RectF(getBounds());
      box.inset(density, density);
      paint.setColor(0x1cb6e9ff);
      paint.setStyle(Paint.Style.FILL);
      canvas.drawRoundRect(box, 9 * density, 9 * density, paint);
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(2 * density);
      paint.setColor(0xffb6e9ff);
      canvas.drawRoundRect(box, 9 * density, 9 * density, paint);
    }
  }

  @Override
  public void getOutline(Outline outline) {
    if (resting != null) {
      resting.setBounds(getBounds());
      resting.getOutline(outline);
    }
  }

  @Override
  public boolean isStateful() {
    return true;
  }

  @Override
  protected boolean onStateChange(int[] state) {
    if (resting != null) resting.setState(state);
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
