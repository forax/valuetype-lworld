/*package fr.umlv.valuetype;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings("static-method")
class ArrayTests {
  @SuppressWarnings("unused")
  private static Stream<Object[]> createLists() {
      return Stream.of(
          new Object[0],
          new Integer[] { 1, 2, 42},
          IntStream.range(0, 10_000).boxed().toArray(Integer[]::new),
          new IntBox[] { IntBox.valueOf(1), IntBox.valueOf(2), IntBox.valueOf(42) },
          IntStream.range(0, 10_000).mapToObj(IntBox::valueOf).toArray(IntBox[]::new)
          );
  }
  
  @ParameterizedTest
  @MethodSource("createLists")
  void testSizeAndIsEmpty(Object[] objects) {
    var array = Array.wrap(objects);
    var list = List.of(objects);
    assertAll(
        () -> assertEquals(list.isEmpty(), array.isEmpty()),
        () -> assertEquals(list.size(), array.size())
        );
  }
}*/

