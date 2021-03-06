package fr.umlv.valuetype;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings("static-method")
class ArrayTests {
  private static Stream<List<Object>> withLists() {
      return Stream.of(
          new Object[0],
          new Integer[] { 1, 2, 42},
          IntStream.range(0, 10_000).boxed().toArray(Integer[]::new),
          new IntBox[] { IntBox.valueOf(1), IntBox.valueOf(2), IntBox.valueOf(42) },
          IntStream.range(0, 10_000).mapToObj(IntBox::valueOf).toArray(IntBox[]::new)
          ).map(List::of);
  }
  
  @SuppressWarnings("unused")
  private static Stream<List<Object>> withNonEmptyLists() {
    return withLists().filter(l -> !l.isEmpty());
  }
  
  @ParameterizedTest
  @MethodSource("withLists")
  void testSizeAndIsEmpty(List<Object> list) {
    var array = Array.wrap(list.toArray());
    assertAll(
        () -> assertEquals(list.isEmpty(), array.isEmpty()),
        () -> assertEquals(list.size(), array.size())
        );
  }
  
  @ParameterizedTest
  @MethodSource("withLists")
  void testContains(List<Object> list) {
    var array = Array.wrap(list.toArray());
    assertAll(
        () -> assertEquals(list.contains(3), array.contains(3)),
        () -> assertEquals(list.contains("foo"), array.contains("foo"))
        );
  }
  
  @ParameterizedTest
  @MethodSource("withNonEmptyLists")
  void testGet(List<Object> list) {
    var array = Array.wrap(list.toArray());
    assertAll(
        () -> assertEquals(list.get(0), array.get(0)),
        () -> assertEquals(list.get(list.size() - 1), array.get(array.size() - 1))
        );
  }
  
  @ParameterizedTest
  @MethodSource("withLists")
  void testHashCode(List<Object> list) {
    var array = Array.wrap(list.toArray());
    assertEquals(list.hashCode(), array.hashCode());
  }
}

