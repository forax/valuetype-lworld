package fr.umlv.jsonapi;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class JsonNumberTest {
  @Test
  public void testFromInt() {
    var number = JsonNumber.from(17);
    assertAll(
        () -> assertFalse(number.isDouble()),
        () -> assertThrows(IllegalStateException.class, number::doubleValue),
        () -> assertTrue(number.isLong()),
        () -> assertEquals(17L, number.longValue()),
        () -> assertTrue(number.fitsInInt()),
        () -> assertEquals(17, number.convertToInt()),
        () -> assertEquals("17", number.toString()),
        () -> assertEquals(17L, number.value())
    );
  }

  @Test
  public void testFromLong() {
    var number = JsonNumber.from(12345678912345L);
    assertAll(
        () -> assertFalse(number.isDouble()),
        () -> assertThrows(IllegalStateException.class, number::doubleValue),
        () -> assertTrue(number.isLong()),
        () -> assertEquals(12345678912345L, number.longValue()),
        () -> assertFalse(number.fitsInInt()),
        () -> assertThrows(IllegalStateException.class, number::convertToInt),
        () -> assertEquals("12345678912345", number.toString()),
        () -> assertEquals(12345678912345L, number.value())
    );
  }

  @Test
  public void testFromDouble() {
    var number = JsonNumber.from(4.0);
    assertAll(
        () -> assertTrue(number.isDouble()),
        () -> assertEquals(4.0, number.doubleValue()),
        () -> assertFalse(number.isLong()),
        () -> assertThrows(IllegalStateException.class, number::longValue),
        () -> assertFalse(number.fitsInInt()),
        () -> assertThrows(IllegalStateException.class, number::convertToInt),
        () -> assertEquals("4.0", number.toString()),
        () -> assertEquals(4.0, number.value())
    );
  }

  @Test
  public void testFromDoubleNaN() {
    var number = JsonNumber.from(Double.NaN);
    assertAll(
        () -> assertTrue(number.isDouble()),
        () -> assertTrue(Double.isNaN(number.doubleValue())),
        () -> assertFalse(number.isLong()),
        () -> assertThrows(IllegalStateException.class, number::longValue),
        () -> assertFalse(number.fitsInInt()),
        () -> assertThrows(IllegalStateException.class, number::convertToInt),
        () -> assertEquals("NaN", number.toString()),
        () -> assertEquals(Double.NaN, number.value())
    );
  }

  @Test
  public void testEqualsHashCodeFromLong() {
    var number1 = JsonNumber.from(34);
    var number2 = JsonNumber.from(34);
    assertAll(
        () -> assertEquals(number1, number2),
        () -> assertEquals(number1.hashCode(), number2.hashCode())
    );
  }

  @Test
  public void testEqualsHashCodeFromDouble() {
    var number1 = JsonNumber.from(8.0);
    var number2 = JsonNumber.from(8.0);
    assertAll(
        () -> assertEquals(number1, number2),
        () -> assertEquals(number1.hashCode(), number2.hashCode())
    );
  }

  @Test
  public void testEqualsHashCodeFromDoubleNaN() {
    var number1 = JsonNumber.from(Double.NaN);
    var number2 = JsonNumber.from(Double.NaN);
    assertAll(
        () -> assertEquals(number1, number2),
        () -> assertEquals(number1.hashCode(), number2.hashCode())
    );
  }
}