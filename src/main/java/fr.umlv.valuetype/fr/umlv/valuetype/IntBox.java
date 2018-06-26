package fr.umlv.valuetype;
import static java.util.stream.Collectors.toList;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final __ByValue class IntBox implements Comparable<IntBox> {
  private final int value;

  private IntBox() {
    this.value = 0;
  }

  @Override
  public String toString() {
    return "" + value;
  }

  @Override
  public int compareTo(IntBox o) {
    return Integer.compare(value, o.value);
  }
  
  public IntBox add(IntBox box) {
    return IntBox.valueOf(value + box.value);
  }
  
  public static IntBox valueOf(int value) {
    IntBox box = __MakeDefault IntBox();
    box = __WithField(box.value, value);
    return box;
  }
  
  public int intValue() {
    return value;
  }

  private static IntBox sum(IntBox n) {
    IntBox sum = IntBox.valueOf(0);
    for(IntBox i = IntBox.valueOf(0); i.compareTo(n) < 0; i = i.add(IntBox.valueOf(1))) {
      sum = sum.add(i);
    }
    return sum;
  }
  
  public static void main(String[] args) {
    /*
    for(var box: IntStream.range(0, 10).mapToObj(IntBox::valueOf).collect(toList())) {
      System.out.println(box);
    }
    
    Comparable<IntBox> c = IntBox.valueOf(42);
    System.out.println("comparable " + c.compareTo(IntBox.valueOf(17)));
    
    System.out.println(sum(IntBox.valueOf(10)));
    */
    for(int i = 0; i < 100_000; i++) {
      sum(IntBox.valueOf(i));
    }
  }
}
