package fr.umlv.valuetype;

import java.util.Objects;

@__value__
public /*value*/ class BitArray {
  private final int capacity;
  private final int[] array;
  
  public BitArray(int capacity) {
    this.capacity = capacity;
    array = new int[1 + capacity / 32];
  }
  
  public boolean get(int index) {
    Objects.checkIndex(index, capacity);
    return (array[index / 32] & (1 << index)) != 0;
  }
  
  public void set(int index) {
    Objects.checkIndex(index, capacity);
    array[index / 32] |= (1 << index);
  }
  
  public void clear(int index) {
    Objects.checkIndex(index, capacity);
    array[index / 32] &= ~(1 << index);
  }
}
