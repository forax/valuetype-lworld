package fr.umlv.valuetype;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
class TaggedValueTests {
  @Test
  void testInts() {
    assertAll(
      () -> assertEquals(112, TaggedValue.from(112).asInt()),
      () -> assertEquals(-17, TaggedValue.from(-17).asInt()),
      () -> assertEquals(0, TaggedValue.from(0).asInt()),
      () -> assertEquals(0, TaggedValue.zero().asInt()),
      () -> assertEquals(1, TaggedValue.one().asInt())
    );
  }
  
  @Test
  void testObjects() {
    assertAll(
      () -> assertEquals("foo", TaggedValue.from("foo").as(String.class)),
      () -> assertEquals(LocalDate.ofYearDay(2018, 57), TaggedValue.from(LocalDate.ofYearDay(2018, 57)).as(LocalDate.class)),
      () -> assertEquals(null, TaggedValue.from(null).as(Object.class))
    );
  }
  
  @Test
  void testAs() {
    assertAll(
      () -> assertThrows(Error.class, () -> TaggedValue.from("hello").asInt()),
      () -> assertThrows(Error.class, () -> TaggedValue.from(999).as(Object.class)),
      () -> assertThrows(ClassCastException.class, () -> TaggedValue.from("foo").as(Integer.class)),
      () -> assertThrows(NullPointerException.class, () -> TaggedValue.from("foo").as(null))
    );
  }
  
  @Test
  void testEquals() {
    assertAll(
      () -> assertTrue(TaggedValue.from("foo").equals(TaggedValue.from("foo"))),
      () -> assertTrue(TaggedValue.from(123).equals(TaggedValue.from(123))),
      () -> assertTrue(TaggedValue.from(null).equals(TaggedValue.from(null))),
      () -> assertFalse(TaggedValue.from("foo").equals(null)),
      () -> assertFalse(TaggedValue.from(null).equals(null))
    );
  }
  
  @Test
  void testHashCode() {
    assertAll(
      () -> assertEquals("bar".hashCode(), TaggedValue.from("bar").hashCode()),
      () -> assertEquals(Integer.hashCode(444), TaggedValue.from(444).hashCode())
    );
  }
  
  @Test
  void testToString() {
    assertAll(
      () -> assertEquals("bar", TaggedValue.from("bar").toString()),
      () -> assertEquals("777", TaggedValue.from(777).toString()),
      () -> assertEquals("null", TaggedValue.from(null).toString())
    );
  }
  
  @Test
  void testIncrement() {
    assertAll(
      () -> assertEquals(TaggedValue.from(223), TaggedValue.from(222).increment()),
      () -> assertEquals(TaggedValue.from(0), TaggedValue.from(-1).increment()),
      () -> assertThrows(Error.class, () -> TaggedValue.from("foo").increment()),
      () -> assertThrows(Error.class, () -> TaggedValue.from(Integer.MAX_VALUE).increment())
    );
  }
  
  @Test
  void testDecrement() {
    assertAll(
      () -> assertEquals(TaggedValue.from(222), TaggedValue.from(223).decrement()),
      () -> assertEquals(TaggedValue.from(0), TaggedValue.from(1).decrement()),
      () -> assertThrows(Error.class, () -> TaggedValue.from("foo").decrement()),
      () -> assertThrows(Error.class, () -> TaggedValue.from(Integer.MIN_VALUE).decrement())
    );
  }
  
  @Test
  void testNegate() {
    assertAll(
      () -> assertEquals(TaggedValue.from(-222), TaggedValue.from(222).negate()),
      () -> assertEquals(TaggedValue.from(1), TaggedValue.from(-1).negate()),
      () -> assertEquals(TaggedValue.from(0), TaggedValue.from(0).negate()),
      () -> assertEquals(TaggedValue.from(Integer.MIN_VALUE).increment(), TaggedValue.from(Integer.MAX_VALUE).negate()),
      () -> assertThrows(Error.class, () -> TaggedValue.from("foo").negate()),
      () -> assertThrows(Error.class, () -> TaggedValue.from(Integer.MIN_VALUE).negate())
    );
  }
  
  @Test
  void testAdd() {
    assertAll(
      () -> assertEquals(TaggedValue.from(110), TaggedValue.from(10).add(TaggedValue.from(100))),
      () -> assertEquals(TaggedValue.from(-980), TaggedValue.from(20).add(TaggedValue.from(-1_000))),
      () -> assertEquals(TaggedValue.from(0), TaggedValue.from(12).add(TaggedValue.from(-12))),
      () -> assertThrows(Error.class, () -> TaggedValue.from("foo").add(TaggedValue.from(12))),
      () -> assertThrows(Error.class, () -> TaggedValue.from(12).add(TaggedValue.from("bar"))),
      () -> assertThrows(Error.class, () -> TaggedValue.from("foo").add(TaggedValue.from("bar"))),
      () -> assertThrows(Error.class, () -> TaggedValue.from(Integer.MAX_VALUE).add(TaggedValue.from(100))),
      () -> assertThrows(Error.class, () -> TaggedValue.from(Integer.MIN_VALUE).add(TaggedValue.from(-100)))
    );
  }
  
  @Test
  void testSubtract() {
    assertAll(
      () -> assertEquals(TaggedValue.from(110), TaggedValue.from(10).subtract(TaggedValue.from(-100))),
      () -> assertEquals(TaggedValue.from(-980), TaggedValue.from(20).subtract(TaggedValue.from(1_000))),
      () -> assertEquals(TaggedValue.from(0), TaggedValue.from(12).subtract(TaggedValue.from(12))),
      () -> assertThrows(Error.class, () -> TaggedValue.from("foo").subtract(TaggedValue.from(12))),
      () -> assertThrows(Error.class, () -> TaggedValue.from(12).subtract(TaggedValue.from("bar"))),
      () -> assertThrows(Error.class, () -> TaggedValue.from("foo").subtract(TaggedValue.from("bar"))),
      () -> assertThrows(Error.class, () -> TaggedValue.from(Integer.MIN_VALUE).subtract(TaggedValue.from(100))),
      () -> assertThrows(Error.class, () -> TaggedValue.from(Integer.MAX_VALUE).subtract(TaggedValue.from(-100)))
    );
  }
  
  @Test
  void testMultiply() {
    assertAll(
      () -> assertEquals(TaggedValue.from(-100), TaggedValue.from(10).multiply(TaggedValue.from(-10))),
      () -> assertEquals(TaggedValue.from(0), TaggedValue.from(-20).multiply(TaggedValue.from(0))),
      () -> assertEquals(TaggedValue.from(144), TaggedValue.from(12).multiply(TaggedValue.from(12))),
      () -> assertThrows(Error.class, () -> TaggedValue.from("foo").multiply(TaggedValue.from(12))),
      () -> assertThrows(Error.class, () -> TaggedValue.from(12).multiply(TaggedValue.from("bar"))),
      () -> assertThrows(Error.class, () -> TaggedValue.from("foo").multiply(TaggedValue.from("bar"))),
      () -> assertThrows(Error.class, () -> TaggedValue.from(Integer.MIN_VALUE).multiply(TaggedValue.from(2))),
      () -> assertThrows(Error.class, () -> TaggedValue.from(Integer.MAX_VALUE).multiply(TaggedValue.from(2)))
    );
  }
}

