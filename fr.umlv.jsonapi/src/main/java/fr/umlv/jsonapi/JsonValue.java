package fr.umlv.jsonapi;

import static java.util.Objects.requireNonNull;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Represent all the values allowed in a JSON text and also all other Java object seen
 * as a specific value named the {@link #isOpaque()} value.
 *
 * A JsonValue is created using one of the variant of the method {@code from}, or
 * using {@link #nullValue()} for {@code null}.
 * The method {@link #fromOpaque(Object)} allows to represent as a JsonValue a Java value
 * that can not be represented as a JSON value,
 *
 * The method {@link #asObject()} returns a Java object corresponding to the JsonValue.
 * For an opaque value {@code x}, {@code from(x).asValue()} is semantically an identity op.
 *
 * The method {@link #toString()} convert a JsonValue to the JSON string representation,
 * quoting the string by example. For opaque value, the method {@link Object#toString()}
 * is called and the result is between double quotes.
 */
public final @__inline__ class JsonValue {
  private final Kind kind;
  private final long data;
  private final Object box;

  /**
   * Kind of the JsonValue
   *
   * @see #kind()
   */
  public enum Kind {
    /**
     * the value {@code null}
     */
    NULL(void.class),
    /**
     * the value {@code true}
     */
    TRUE(boolean.class),
    /**
     * the value {@code false}
     */
    FALSE(boolean.class),
    /**
     * a 32 bits integer
     */
    INT(int.class),
    /**
     * a 64 bits integer
     */
    LONG(long.class),
    /**
     * a 64 bits floating point value
     */
    DOUBLE(double.class),
    /**
     * a string
     */
    STRING(String.class),
    /**
     * an arbitrary-precision integer
     */
    BIG_INTEGER(BigInteger.class),
    /**
     * A Java object that can not be represent by a JSON type
     */
    OPAQUE(Object.class)
    ;

    private final Class<?> type;

    Kind(Class<?> type) {
      this.type = type;
    }
  }

  private JsonValue(Kind kind, long data, Object box) {
    this.kind = kind;
    this.data = data;
    this.box = box;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof JsonValue value
        && kind == value.kind
        && data == value.data
        && (box instanceof String string?  // de-virtualize
              string.equals(value.box):
              Objects.equals(box, value.box));
  }

  @Override
  public int hashCode() {
    if (box != null) {
      if (box instanceof String string) {  // de-virtualize
        return string.hashCode();
      }
      return box.hashCode();
    }
    return (int)(data ^ data >>> 32);
  }

  /**
   * Returns a JSON representation of the value.
   * Opaque values are encoded as JSON strings
   * @return a JSON representation of the value
   */
  @Override
  public String toString() {
    return switch(kind) {
      case INT, LONG -> Long.toString(data);
      case DOUBLE -> Double.toString(Double.longBitsToDouble(data));
      case STRING -> '"' + (String) box + '"';   // FIXME escape
      case OPAQUE -> '"' + box.toString() + '"';
      default -> box.toString();
    };
  }

  /**
   * Returns the "JSON type" of the value
   * @return the "JSON type" of the value
   */
  public Kind kind() {
    return kind;
  }

  /**
   * Returns the Java type corresponding to the value
   * @return the Java type corresponding to the value
   */
  public Class<?> type() {
    return kind.type;
  }

  /**
   * Returns true if the value is {@code null}
   * @return true if the value is {@code null}
   */
  public boolean isNull() {
    return kind == Kind.NULL;
  }

  /**
   * Returns true if the value is {@code true}
   * @return true if the value is {@code true}
   */
  public boolean isTrue() {
    return kind == Kind.TRUE;
  }

  /**
   * Returns true if the value is {@code false}
   * @return true if the value is {@code false}
   */
  public boolean isFalse() {
    return kind == Kind.FALSE;
  }

  /**
   * Returns true if the value is the boolean value {@code true} or {@code false}
   * @return true if the value is a boolean value
   */
  public boolean isBoolean() {
    return kind.type == boolean.class;
  }

  /**
   * Returns true if the value is a 32 bits integer
   * @return true if the value is a 32 bits integer
   */
  public boolean isInt() {
    return kind == Kind.INT;
  }

  /**
   * Returns true if the value is a 64 bits integer
   * @return true if the value is a 64 bits integer
   */
  public boolean isLong() {
    return kind == Kind.LONG;
  }

  /**
   * Returns true if the value is a 64 bits floating point number
   * @return true if the value is a 64 bits floating point number
   */
  public boolean isDouble() {
    return kind == Kind.DOUBLE;
  }

  /**
   * Returns true if the value is a string
   * @return true if the value is a string
   */
  public boolean isString() {
    return kind == Kind.STRING;
  }

  /**
   * Returns true if the value is an arbitrary-precision integer
   * @return true if the value is an arbitrary-precision integer
   */
  public boolean isBigInteger() {
    return kind == Kind.BIG_INTEGER;
  }

  /**
   * Returns true if the value is a Java object that can not be represented
   * as a JSON value
   * @return true if the value is a Java object
   */
  public boolean isOpaque() { return kind == Kind.OPAQUE; }

  /**
   * returns the current value as a boolean value
   * @return the current value as a boolean value
   * @throws IllegalStateException if the current value is not a boolean value
   */
  public boolean booleanValue() {
    if (kind.type != boolean.class) {
      throw new IllegalStateException("not a boolean: " + this);
    }
    return isTrue();
  }

  /**
   * returns the current value as a 32 bits integer value
   * @return the current value as a 32 bits integer value
   * @throws IllegalStateException if the current value is not a 32 bits integer value
   */
  public int intValue() {
    if (kind != Kind.INT) {
      throw new IllegalStateException("not an int: " + this);
    }
    return (int) data;
  }

  /**
   * returns the current value as a 64 bits integer value
   * @return the current value as a 64 bits integer value
   * @throws IllegalStateException if the current value is not a 64 bits integer value
   */
  public long longValue() {
    if (kind != Kind.LONG) {
      throw new IllegalStateException("not a long: " + this);
    }
    return data;
  }

  /**
   * returns the current value as a 64 bits floating point number
   * @return the current value as a 64 bits floating point number
   * @throws IllegalStateException if the current value is not a 64 bits floating point number
   *
   * @see #convertToDouble()
   */
  public double doubleValue() {
    if (kind != Kind.DOUBLE) {
      throw new IllegalStateException("not a double: " + this);
    }
    return Double.longBitsToDouble(data);
  }

  /**
   * returns the current value as a string value
   * @return the current value as a string value
   * @throws IllegalStateException if the current value is not a string value
   */
  public String stringValue() {
    if (kind != Kind.STRING) {
      throw new IllegalStateException("not a String: " + this);
    }
    return (String) box;
  }

  /**
   * returns the current value as an arbitrary-precision integer
   * @return the current value as an arbitrary-precision integer
   * @throws IllegalStateException if the current value is not an arbitrary-precision integer
   */
  public BigInteger bigIntegerValue() {
    if (kind != Kind.BIG_INTEGER) {
      throw new IllegalStateException("not a BigInteger: " + this);
    }
    return (BigInteger) box;
  }

  /**
   * Returns the value as a Java object.
   * All primitive types are boxed, all other values are returned as is.
   * @return the value as a Java object
   */
  public Object asObject() {
    return switch(kind) {
      case NULL -> null;
      case TRUE -> true;
      case FALSE -> false;
      case INT -> (int)data;
      case LONG-> data;
      case DOUBLE -> Double.longBitsToDouble(data);
      case STRING -> (String) box;
      case BIG_INTEGER, OPAQUE -> box;
    };
  }

  /**
   * Returns all numeric values (int, long, double, big integer) converted as doubles
   * @return the value converted as double
   * @throws IllegalStateException if the current value is not a numeric value
   */
  public double convertToDouble() {
    return switch(kind) {
      case INT -> (int)data;
      case LONG-> data;
      case DOUBLE -> Double.longBitsToDouble(data);
      case BIG_INTEGER -> ((BigInteger) box).doubleValue();
      default -> throw new IllegalStateException("not a numeric value " + this);
    };
  }

  /**
   * Return the value converted to an unquoted Java string
   * @return the value converted to a Java string
   */
  public String convertToString() {
    return switch(kind) {
      case NULL -> null;
      case INT, LONG -> Long.toString(data);
      case DOUBLE -> Double.toString(Double.longBitsToDouble(data));
      case STRING -> (String) box;
      default -> box.toString();
    };
  }

  /**
   * Returns the {@code null} JSON value
   * @return the {@code null} JSON value
   */
  public static JsonValue nullValue() {
    return new JsonValue(Kind.NULL, 0, "null");
  }

  /**
   * Returns the {@code true} JSON value
   * @return the {@code true} JSON value
   */
  public static JsonValue trueValue() {
    return new JsonValue(Kind.TRUE, 0, "true");
  }

  /**
   * Returns the {@code false} JSON value
   * @return the {@code false} JSON value
   */
  public static JsonValue falseValue() {
    return new JsonValue(Kind.FALSE, 0, "false");
  }

  /**
   * Returns a JSON boolean value
   * @param value a Java boolean value
   * @return a JSON boolean value
   */
  public static JsonValue from(boolean value) {
    return value? trueValue(): falseValue();
  }

  /**
   * Returns a JSON 32 bits integer value
   * @param value a Java 32 bits integer value
   * @return a JSON 32 bits integer value
   */
  public static JsonValue from(int value) {
    return new JsonValue(Kind.INT, value, null);
  }

  /**
   * Returns a JSON 64 bits integer value
   * @param value a Java 64 bits integer value
   * @return a JSON 64 bits integer value
   */
  public static JsonValue from(long value) {
    return new JsonValue(Kind.LONG, value, null);
  }

  /**
   * Returns a JSON 64 bits floating point number
   * @param value a Java 64 bits floating point
   * @return a 64 bits integer JSON value
   */
  public static JsonValue from(double value) {
    return new JsonValue(Kind.DOUBLE, Double.doubleToLongBits(value), null);
  }

  /**
   * Returns a JSON string value
   * @param value a Java string value
   * @return a JSON string value
   */
  public static JsonValue from(String value) {
    requireNonNull(value);
    return new JsonValue(Kind.STRING, 0, value);
  }

  /**
   * Returns a JSON arbitrary-precision integer
   * @param value a Java arbitrary-precision integer
   * @return a JSON arbitrary-precision integer
   */
  public static JsonValue from(BigInteger value) {
    requireNonNull(value);
    return new JsonValue(Kind.BIG_INTEGER, 0, value);
  }

  /**
   * Returns a JSON opaque value
   * @param value any Java object
   * @return a JSON opaque value
   */
  public static JsonValue fromOpaque(Object value) {
    requireNonNull(value);
    return new JsonValue(Kind.OPAQUE, 0, value);
  }

  /**
   * Returns a JSON value that encodes any Java object to its JSON counterpart
   * @param value any Java object
   * @return a JSON value
   */
  public static JsonValue fromAny(Object value) {
    if (value == null) {
      return nullValue();
    }
    if (value instanceof Boolean booleanValue) {
      return from(booleanValue);
    }
    if (value instanceof Integer intValue) {
      return from(intValue);
    }
    if (value instanceof Long longValue) {
      return from(longValue);
    }
    if (value instanceof Double doubleValue) {
      return from(doubleValue);
    }
    if (value instanceof String stringValue) {
      return from(stringValue);
    }
    if (value instanceof BigInteger bigInteger) {
      return from(bigInteger);
    }
    return fromOpaque(value);
  }
}
