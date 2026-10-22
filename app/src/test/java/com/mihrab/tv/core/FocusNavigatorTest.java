package com.mihrab.tv.core;

import static org.junit.Assert.*;

import java.io.File;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.*;

public class FocusNavigatorTest {
  @Test
  public void dockEdgesPageBeforeTakingDiagonalHeaderCandidates() {
    assertTrue(FocusNavigator.appBoundary("app:last", "appearance", FocusNavigator.LEFT));
    assertTrue(FocusNavigator.appBoundary("app:first", "app:first", FocusNavigator.RIGHT));
    assertFalse(FocusNavigator.appBoundary("app:first", "app:second", FocusNavigator.LEFT));
    assertFalse(FocusNavigator.appBoundary("app:first", "verse", FocusNavigator.UP));
  }

  private final List<FocusNavigator.Node> row =
      List.of(
          new FocusNavigator.Node("right", 400, 100, 560, 210),
              new FocusNavigator.Node("middle", 210, 100, 380, 210),
          new FocusNavigator.Node("left", 20, 100, 190, 210),
              new FocusNavigator.Node("above", 210, 0, 380, 50));

  @Test
  public void physicalDirectionsRemainCorrectInRtl() {
    assertEquals("middle", FocusNavigator.next(row, "right", FocusNavigator.LEFT));
    assertEquals("right", FocusNavigator.next(row, "middle", FocusNavigator.RIGHT));
    assertEquals("above", FocusNavigator.next(row, "middle", FocusNavigator.UP));
    assertEquals("middle", FocusNavigator.next(row, "above", FocusNavigator.DOWN));
  }

  @Test
  public void edgesDoNotLoseFocus() {
    assertEquals("right", FocusNavigator.next(row, "right", FocusNavigator.RIGHT));
    assertEquals("left", FocusNavigator.next(row, "left", FocusNavigator.LEFT));
  }

  @Test
  public void removedPackageHasDeterministicFallback() {
    assertEquals(
        "nav_home",
        FocusNavigator.restore(List.of("nav_home", "app:new"), "app:removed", "nav_home"));
    assertEquals(
        "app:new", FocusNavigator.restore(List.of("nav_home", "app:new"), "app:new", "nav_home"));
  }

  @Test
  public void sameRowWinsOverNearerDiagonal() {
    List<FocusNavigator.Node> nodes =
        List.of(
            new FocusNavigator.Node("from", 0, 0, 20, 20),
            new FocusNavigator.Node("diagonal", 22, 21, 42, 41),
            new FocusNavigator.Node("sameRow", 50, 0, 70, 20));
    assertEquals("sameRow", FocusNavigator.next(nodes, "from", FocusNavigator.RIGHT));
  }

  private final List<FocusNavigator.Node> home = new ArrayList<>();

  private float dim(Element el, String name) {
    String value = el.getAttribute("android:" + name);
    return value.endsWith("dp") ? Float.parseFloat(value.substring(0, value.length() - 2)) : 0;
  }

  private void walk(Element el, float x, float y) {
    float left = x + dim(el, "layout_marginLeft"), top = y + dim(el, "layout_marginTop");
    if (el.getAttribute("style").equals("@style/Control.Mihrab")) {
      String key = el.getAttribute("android:id").replace("@+id/", "");
      home.add(
          new FocusNavigator.Node(
              key, left, top, left + dim(el, "layout_width"), top + dim(el, "layout_height")));
    }
    NodeList children = el.getChildNodes();
    for (int i = 0; i < children.getLength(); i++)
      if (children.item(i) instanceof Element) walk((Element) children.item(i), left, top);
  }

  @Test
  public void allHomeControlsReachableFromInitialFocus() throws Exception {
    walk(
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(new File("src/main/res/layout/activity_home.xml"))
            .getDocumentElement(),
        0,
        0);
    for (int i = 0; i < 5; i++)
      home.add(
          new FocusNavigator.Node(
              "prayer:" + i, 81 + (4 - i) * 217, 464, 289 + (4 - i) * 217, 539));
    for (int i = 0; i < 8; i++)
      home.add(
          new FocusNavigator.Node(
              "app:" + i, 67 + (7 - i) * 184.875f, 747, 238.875f + (7 - i) * 184.875f, 859));
    Set<String> seen = new HashSet<>();
    ArrayDeque<String> queue = new ArrayDeque<>();
    queue.add("nav_home");
    while (!queue.isEmpty()) {
      String key = queue.remove();
      if (!seen.add(key)) continue;
      for (int direction = 0; direction < 4; direction++) {
        String next = FocusNavigator.next(home, key, direction);
        if (!seen.contains(next)) queue.add(next);
      }
    }
    Set<String> unreachable = new HashSet<>();
    for (FocusNavigator.Node node : home) if (!seen.contains(node.key)) unreachable.add(node.key);
    assertTrue("Unreachable controls: " + unreachable, unreachable.isEmpty());
  }
}
