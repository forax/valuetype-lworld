package fr.umlv.valuetype;

public value class UnsignedInt implements Comparable<UnsignedInt> {
  private final int value;

  private UnsignedInt(int value) {
    this.value = value;
  }

  public static UnsignedInt of(String s) throws NumberFormatException {
    return new UnsignedInt(Integer.parseUnsignedInt(s));
  }
  
  public static UnsignedInt ofUnsigned(int i) {
    if (i < 0) {
      throw new IllegalArgumentException("i is not positive");
    }
    return new UnsignedInt(i);
  }
  public static UnsignedInt ofUnsigned(long l) {
    if (l < 0 || l > 0xFFFFFFFFL) {
      throw new IllegalArgumentException("l is not positive or too big");
    }
    return new UnsignedInt((int)l);
  }
  
  public static UnsignedInt zero() {
    return new UnsignedInt(0);
  }
  
  public int asSigned() {
    return value;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof UnsignedInt)) {
      return false;
    }
    return value == ((UnsignedInt) obj).value;
  }

  @Override
  public int hashCode() {
    return value;
  }
  
  @Override
  public String toString() {
    return Integer.toUnsignedString(value);
  }
  
  @Override
  public int compareTo(UnsignedInt unsignedInt) {
    return Integer.compareUnsigned(value, unsignedInt.value);
  }
  
  public UnsignedInt add(UnsignedInt unsignedInt) {
    return new UnsignedInt(value + unsignedInt.value);
  }
  
  public UnsignedInt increment() {
    return new UnsignedInt(value + 1);
  }

  public UnsignedInt subtract(UnsignedInt unsignedInt) {
    return new UnsignedInt(value - unsignedInt.value);
  }
  
  public UnsignedInt decrement() {
    return new UnsignedInt(value - 1);
  }
  
  public UnsignedInt multiply(UnsignedInt unsignedInt) {
    return new UnsignedInt(value * unsignedInt.value);
  }
  
  public UnsignedInt divide(UnsignedInt unsignedInt) {
    return new UnsignedInt(Integer.divideUnsigned(value, unsignedInt.value));
  }
  
  public UnsignedInt remainder(UnsignedInt unsignedInt) {
    return new UnsignedInt(Integer.remainderUnsigned(value, unsignedInt.value));
  }
  
  public static void main(String[] args) {
    System.out.println(UnsignedInt.ofUnsigned(Integer.MAX_VALUE).add(UnsignedInt.ofUnsigned(1)));
  }
}
