public final value class IntBox {
  private final int value;

  private IntBox() {
    this.value = 0;
  }

  @Override
  public String toString() {
    return "" + value;
  }

  public int compareTo(IntBox box) {
    return Integer.compare(value, box.value);
  }
  
  public IntBox add(IntBox box) {
    return IntBox.valueOf(value + box.value);
  }
  
  public static IntBox valueOf(int value) {
    IntBox box = __MakeDefault IntBox();
    box = __WithField(box.value, value);
    return box;
  }

  private static IntBox sum(IntBox n) {
    IntBox sum = IntBox.valueOf(0);
    for(IntBox i = IntBox.valueOf(0); i.compareTo(n) < 0; i = i.add(IntBox.valueOf(1))) {
      sum = sum.add(i);
    }
    return sum;
  }
  
  public static void main(String[] args) {
    for(int i = 0; i < 100_000; i++) {
      sum(IntBox.valueOf(i));
    }
  }
}
