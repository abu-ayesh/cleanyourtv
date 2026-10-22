package com.mihrab.tv.core;

import java.util.List;

/** Physical arrow directions, independent of locale and testable without an Android runtime. */
public final class FocusNavigator {
  public static final int LEFT = 0, RIGHT = 1, UP = 2, DOWN = 3;

  public static final class Node {
    public final String key;
    public final float left, top, right, bottom;

    public Node(String key, float left, float top, float right, float bottom) {
      this.key = key;
      this.left = left;
      this.top = top;
      this.right = right;
      this.bottom = bottom;
    }

    float x() {
      return (left + right) / 2;
    }

    float y() {
      return (top + bottom) / 2;
    }
  }

  public static String next(List<Node> nodes, String key, int direction) {
    Node from = null;
    for (Node n : nodes) if (n.key.equals(key)) from = n;
    if (from == null) return nodes.isEmpty() ? null : nodes.get(0).key;
    Node best = null;
    double bestScore = Double.MAX_VALUE;
    for (Node n : nodes) {
      if (n == from) continue;
      float dx = n.x() - from.x(), dy = n.y() - from.y();
      boolean horizontal = direction == LEFT || direction == RIGHT;
      float primary = horizontal ? dx : dy;
      if (direction == LEFT || direction == UP) primary = -primary;
      if (primary <= 1) continue;
      boolean beam =
          horizontal
              ? n.bottom > from.top && n.top < from.bottom
              : n.right > from.left && n.left < from.right;
      float side = horizontal ? Math.abs(dy) : Math.abs(dx);
      double score = (beam ? 0 : 1_000_000) + primary * primary + side * side * 4;
      if (score < bestScore) {
        best = n;
        bestScore = score;
      }
    }
    return best == null ? key : best.key;
  }

  public static String restore(List<String> keys, String saved, String fallback) {
    if (saved != null && keys.contains(saved)) return saved;
    if (keys.contains(fallback)) return fallback;
    return keys.isEmpty() ? null : keys.get(0);
  }

  /** The dock pages horizontally; a diagonal header candidate must not steal its edge. */
  public static boolean appBoundary(String current, String candidate, int direction) {
    return current.startsWith("app:")
        && (direction == LEFT || direction == RIGHT)
        && (candidate.equals(current) || !candidate.startsWith("app:"));
  }

  private FocusNavigator() {}
}
