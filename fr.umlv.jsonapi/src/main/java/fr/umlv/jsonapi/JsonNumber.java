package fr.umlv.jsonapi;

public final @__inline__ class JsonNumber implements JsonValue {
  private static final long SIGNALING_NaN = 0b1111111111111000_0000000000000000_0000000000000000_0000000000000000L;

  private static boolean isDouble(long doubleValue) {
    return doubleValue != SIGNALING_NaN;
  }

  private final long doubleValue;
  private final long longValue;

  private JsonNumber(long doubleValue, long longValue) {
    this.doubleValue = doubleValue;
    this.longValue = longValue;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof JsonNumber number && doubleValue == number.doubleValue && longValue == number.longValue;
  }

  @Override
  public int hashCode() {
    return (int)(doubleValue ^ doubleValue >>> 32 ^ longValue ^ longValue >>> 32);
  }

  @Override
  public String toString() {
    var doubleValue = this.doubleValue;
    if (isDouble(doubleValue)) {
      return Double.toString(Double.longBitsToDouble(doubleValue));
    }
    return Long.toString(longValue);
  }

  @Override
  public Number value() {
    var doubleValue = this.doubleValue;
    if (isDouble(doubleValue)) {
      return Double.longBitsToDouble(doubleValue);
    }
    return longValue;
  }

  public boolean isDouble() {
    return isDouble(doubleValue);
  }
  public boolean isLong() {
    return !isDouble(doubleValue);
  }

  public double doubleValue() {
    var doubleValue = this.doubleValue;
    if (!isDouble(doubleValue)) {
      throw new IllegalStateException("the value is not a double");
    }
    return Double.longBitsToDouble(doubleValue);
  }

  public long longValue() {
    if (isDouble(doubleValue)) {
      throw new IllegalStateException("the value is not a long");
    }
    return longValue;
  }

  public boolean fitsInInt() {
    if (isDouble(doubleValue)) {
      return false;
    }
    var longValue = this.longValue;
    return longValue <= Integer.MAX_VALUE && longValue >= Integer.MIN_VALUE;
  }

  public int convertToInt() {
    if (!fitsInInt()) {
      throw new IllegalStateException("the value can not be stored into an int");
    }
    return (int) longValue;
  }

  public double convertToDouble() {
    var doubleValue = this.doubleValue;
    if (isDouble(doubleValue)) {
      return Double.longBitsToDouble(doubleValue);
    }
    return longValue;
  }

  public static JsonNumber from(long value) {
    return new JsonNumber(SIGNALING_NaN, value);
  }

  public static JsonNumber from(double value) {
    var doubleValue = Double.doubleToLongBits(value);
    assert doubleValue != SIGNALING_NaN;
    return new JsonNumber(doubleValue, 0);
  }
}
