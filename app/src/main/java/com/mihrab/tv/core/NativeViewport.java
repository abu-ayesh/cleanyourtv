package com.mihrab.tv.core;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/** A dp design canvas: preserve the approved aspect ratio and letterbox other displays. */
public final class NativeViewport extends ViewGroup {
  public NativeViewport(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onMeasure(int widthSpec, int heightSpec) {
    int width = MeasureSpec.getSize(widthSpec), height = MeasureSpec.getSize(heightSpec);
    setMeasuredDimension(width, height);
    float density = getResources().getDisplayMetrics().density;
    View stage = getChildAt(0);
    stage.measure(
        MeasureSpec.makeMeasureSpec(Math.round(1600 * density), MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(Math.round(900 * density), MeasureSpec.EXACTLY));
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    View stage = getChildAt(0);
    float scale =
        Math.min(
            getWidth() / (float) stage.getMeasuredWidth(),
            getHeight() / (float) stage.getMeasuredHeight());
    int x = Math.round((getWidth() - stage.getMeasuredWidth() * scale) / 2);
    int y = Math.round((getHeight() - stage.getMeasuredHeight() * scale) / 2);
    stage.setPivotX(0);
    stage.setPivotY(0);
    stage.setScaleX(scale);
    stage.setScaleY(scale);
    stage.layout(x, y, x + stage.getMeasuredWidth(), y + stage.getMeasuredHeight());
  }
}
