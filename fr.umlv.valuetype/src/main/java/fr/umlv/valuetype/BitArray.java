package fr.umlv.valuetype;

import java.util.Objects;

@__inline__
public /*inline*/ class BitArray {
  private static final int BITS_IN_INT = 32;

  private final int capacity;
  private final int[] array;
  
  public BitArray(int capacity) {
    this.capacity = capacity;
    array = new int[1 + capacity / BITS_IN_INT];
  }
  
  public boolean get(int index) {
    Objects.checkIndex(index, capacity);
    return (array[index / BITS_IN_INT] & (1 << index)) != 0;
  }
  
  public void set(int index) {
    Objects.checkIndex(index, capacity);
    array[index / BITS_IN_INT] |= (1 << index);
  }
  
  public void clear(int index) {
    Objects.checkIndex(index, capacity);
    array[index / BITS_IN_INT] &= ~(1 << index);
  }
}
