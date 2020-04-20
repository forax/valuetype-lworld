package fr.umlv.valuetype;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
class VectorTests {
  @Test
  void testAdd() {
    assertAll(
        () -> assertEquals(Vector.of(4), Vector.of(1).add(Vector.of(3))),
        () -> assertEquals(Vector.of(4, 6), Vector.of(1, 2).add(Vector.of(3, 4))),
        () -> assertEquals(Vector.of(4, 6, 8), Vector.of(1, 2, 3).add(Vector.of(3, 4, 5))),
        () -> assertEquals(Vector.of(4, 6, 8, 10), Vector.of(1, 2, 3, 4).add(Vector.of(3, 4, 5, 6))),
        () -> assertEquals(Vector.of(4, 6, 8, 10, 12), Vector.of(1, 2, 3, 4, 5).add(Vector.of(3, 4, 5, 6, 7)))
        );
  }
  
  @Test
  void testSubtract() {
    assertAll(
        () -> assertEquals(Vector.of(-2), Vector.of(1).subtract(Vector.of(3))),
        () -> assertEquals(Vector.of(-2, -2), Vector.of(1, 2).subtract(Vector.of(3, 4))),
        () -> assertEquals(Vector.of(-2, -2, -2), Vector.of(1, 2, 3).subtract(Vector.of(3, 4, 5))),
        () -> assertEquals(Vector.of(-2, -2, -2, -2), Vector.of(1, 2, 3, 4).subtract(Vector.of(3, 4, 5, 6))),
        () -> assertEquals(Vector.of(-2, -2, -2, -2, -2), Vector.of(1, 2, 3, 4, 5).subtract(Vector.of(3, 4, 5, 6, 7)))
        );
  }
}

