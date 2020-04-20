package fr.umlv.valuetype;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
class OptionTests {
  @Test
  void testIsAbsent() {
    assertAll(
        () -> assertTrue(Option.empty().isAbsent()),
        () -> assertFalse(Option.of("foo").isAbsent()),
        () -> assertTrue(Option.ofNullable(null).isAbsent()),
        () -> assertFalse(Option.ofNullable("foo").isAbsent())
        );
  }
  
  @Test
  void testIsPresent() {
    assertAll(
        () -> assertFalse(Option.empty().isPresent()),
        () -> assertTrue(Option.of("foo").isPresent()),
        () -> assertFalse(Option.ofNullable(null).isPresent()),
        () -> assertTrue(Option.ofNullable("foo").isPresent())
        );
  }
  
  @Test
  void testIfPresent() {
    assertAll(
        () -> Option.empty().ifPresent(__ -> fail("oops")),
        () -> {
          var ok = new boolean[] { false };
          Option.of("foo").ifPresent(value -> ok[0] = value.equals("foo"));
          assertTrue(ok[0]);
        },
        () -> Option.ofNullable(null).ifPresent(__ -> fail("oops")),
        () -> {
          var ok = new boolean[] { false };
          Option.ofNullable("foo").ifPresent(value -> ok[0] = value.equals("foo"));
          assertTrue(ok[0]);
        });
  }
  
  
}
