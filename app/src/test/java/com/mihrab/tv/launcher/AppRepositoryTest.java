package com.mihrab.tv.launcher;

import static org.junit.Assert.*;

import org.junit.Test;

public class AppRepositoryTest {
  @Test
  public void rejectsFactoryAndEngineeringEntryPoints() {
    assertFalse(AppRepository.safeComponent("com.cvte.tv.hkcfac", "MainActivity"));
    assertFalse(AppRepository.safeComponent("com.example", "FactoryMenu"));
    assertFalse(AppRepository.safeComponent("com.mediatek.engineermode", "MainActivity"));
    assertTrue(AppRepository.safeComponent("com.example.player", "MainActivity"));
  }

  @Test
  public void packageLabelsCannotInjectBidiOverridesOrLineSeparators() {
    assertEquals("Player", AppRepository.cleanLabel("\u202ePlay\u2028er\n", "fallback"));
    assertEquals("fallback", AppRepository.cleanLabel("\u202e\n", "fallback"));
    assertEquals(80, AppRepository.cleanLabel("x".repeat(100), "fallback").length());
  }
}
