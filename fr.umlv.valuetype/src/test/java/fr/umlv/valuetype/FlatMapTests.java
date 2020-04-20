package fr.umlv.valuetype;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
class FlatMapTests {
  @Test
  void testSimpleSize() {
    FlatMap<String, String> map = new FlatMap<>();
    assertEquals(0, map.size());
  }
  @Test
  void testSimplePutGet() {
    FlatMap<Integer, String> map = new FlatMap<>();
    map.put(1, "one");
    assertEquals(1, map.size());
    assertEquals("one", map.get(1).orElse(null));
  }
  @Test
  void testSimplePutGet2() {
    FlatMap<Integer, String> map = new FlatMap<>();
    map.put(2, new String("two"));
    assertEquals("two", map.get(2).orElse(null));
  }
  @Test
  void testSimpleMissing() {
    FlatMap<String, String> map = new FlatMap<>();
    assertFalse(map.get("bob").isPresent());
  }
  @Test
  void testSimpleReuse() {
    FlatMap<Integer, String> map = new FlatMap<>();
    map.put(2, "two");
    assertEquals("two", map.get(2).orElse(null));
    map.put(2, "three");
    assertEquals("three", map.get(2).orElse(null));
  }
  
  @Test
  void testPuts() {
    FlatMap<String, Integer> map = new FlatMap<>();
    map.put("foo", 3);
    map.put("bar", 7);
    map.put("baz", 12);
    map.put("boz", 42);
    map.put("foo2", 3);
    map.put("bar2", 7);
    map.put("baz2", 12);
    map.put("boz2", 42);
    
    assertAll(
      () -> assertEquals(3, (int)map.get("foo").orElse(-1)),
      () -> assertEquals(7, (int)map.get("bar").orElse(-1)),
      () -> assertEquals(12, (int)map.get("baz").orElse(-1)),
      () -> assertEquals(42, (int)map.get("boz").orElse(-1)),
      () -> assertEquals(3, (int)map.get("foo2").orElse(-1)),
      () -> assertEquals(7, (int)map.get("bar2").orElse(-1)),
      () -> assertEquals(12, (int)map.get("baz2").orElse(-1)),
      () -> assertEquals(42, (int)map.get("boz2").orElse(-1))
      );
  }
  
  @Test
  void testSize() {
    FlatMap<String, Integer> map = new FlatMap<>();
    assertEquals(0, map.size());
    assertEquals(-1, (int)map.get("foo").orElse(-1));
    map.put("foo", 3);
    assertEquals(1, map.size());
    assertEquals(3, (int)map.get("foo").orElse(-1));
    map.put("foo", 4);
    assertEquals(1, map.size());
    assertEquals(4, (int)map.get("foo").orElse(-1));
  }
  
  @Test
  void testZeroHash() {
    FlatMap<Integer, Integer> map = new FlatMap<>();
    map.put(0, 0);
    assertEquals(1, map.size());
    assertEquals(0, (int)map.get(0).orElse(-1));
  }
  
  @Test
  void testNullGet() {
    FlatMap<Integer, Integer> map = new FlatMap<>();
    assertThrows(NullPointerException.class, () -> {
      map.get(null);
    });
  }
  @Test
  void testNullPutKey() {
    FlatMap<Integer, String> map = new FlatMap<>();
    assertThrows(NullPointerException.class, () -> {
      map.put(null, "foo");
    });
  }
  @Test
  void testNullPutValue() {
    FlatMap<Integer, String> map = new FlatMap<>();
    assertThrows(NullPointerException.class, () -> {
      map.put(10, null);
    });
  }
  
  @Test
  public void testPutALot() {
    FlatMap<Integer, Integer> map = new FlatMap<>();
    IntStream.range(0, 100_000).forEach(i -> map.put(i, i));
    
    IntStream.range(0, 100_000).forEach(i -> {
      assertEquals(i, (int)map.get(i).orElse(-1));  
    });
  }
}
