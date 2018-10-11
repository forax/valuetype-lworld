package fr.umlv.valuetype;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
class BitArrayTests {
  @Test
  void testEmpty() {
    var bits = new BitArray(5_000);
    for(int i = 0; i < 5_000; i++) {
      assertFalse(bits.get(i));
    }
  }
  
  @Test
  void testNoOverflow() {
    var bits = new BitArray(64);
    for(int i = 0; i < 32; i++) {
      bits.set(i);
    }
    assertFalse(bits.get(32));
  }
  
  @Test
  void testSet() {
    var bits = new BitArray(5_000);
    for(int i = 0; i < 5_000; i++) {
      bits.set(i);
      assertTrue(bits.get(i));
    }
  }
  
  @Test
  void testClear() {
    var bits = new BitArray(5_000);
    for(int i = 0; i < 5_000; i++) {
      bits.set(i);
      bits.clear(i);
      assertFalse(bits.get(i));
    }
  }
}

