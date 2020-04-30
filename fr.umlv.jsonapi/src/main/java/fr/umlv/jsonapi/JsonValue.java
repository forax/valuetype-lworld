package fr.umlv.jsonapi;

import static java.util.Objects.requireNonNull;

import java.math.BigInteger;
import java.util.Objects;

public final @__inline__ class JsonValue {
  private final Kind kind;
  private final long data;
  private final Object box;

  public enum Kind {
    NULL(void.class),
    TRUE(boolean.class),
    FALSE(boolean.class),
    INT(int.class),
    LONG(long.class),
    DOUBLE(double.class),
    STRING(String.class),
    BIG_INTEGER(BigInteger.class),
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

  @Override
  public String toString() {
    return switch(kind) {
      case INT, LONG -> Long.toString(data);
      case DOUBLE -> Double.toString(Double.longBitsToDouble(data));
      case STRING -> '"' + (String) box + '"';   // FIXME escape
      default -> box.toString();
    };
  }

  public Kind kind() {
    return kind;
  }
  public Class<?> type() {
    return kind.type;
  }

  public boolean isNull() {
    return kind == Kind.NULL;
  }
  public boolean isTrue() {
    return kind == Kind.TRUE;
  }
  public boolean isFalse() {
    return kind == Kind.FALSE;
  }
  public boolean isBoolean() {
    return kind.type == boolean.class;
  }
  public boolean isInt() {
    return kind == Kind.INT;
  }
  public boolean isLong() {
    return kind == Kind.LONG;
  }
  public boolean isDouble() {
    return kind == Kind.DOUBLE;
  }
  public boolean isString() {
    return kind == Kind.STRING;
  }
  public boolean isBigInteger() {
    return kind == Kind.BIG_INTEGER;
  }
  public boolean isOpaque() { return kind == Kind.OPAQUE; }

  public boolean booleanValue() {
    if (kind.type != boolean.class) {
      throw new IllegalStateException("not a boolean: " + this);
    }
    return isTrue();
  }
  public int intValue() {
    if (kind != Kind.INT) {
      throw new IllegalStateException("not an int: " + this);
    }
    return (int) data;
  }
  public long longValue() {
    if (kind != Kind.LONG) {
      throw new IllegalStateException("not a long: " + this);
    }
    return data;
  }
  public double doubleValue() {
    if (kind != Kind.DOUBLE) {
      throw new IllegalStateException("not a double: " + this);
    }
    return Double.longBitsToDouble(data);
  }
  public String stringValue() {
    if (kind != Kind.STRING) {
      throw new IllegalStateException("not a String: " + this);
    }
    return (String) box;
  }
  public BigInteger bigIntegerValue() {
    if (kind != Kind.BIG_INTEGER) {
      throw new IllegalStateException("not a BigInteger: " + this);
    }
    return (BigInteger) box;
  }

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

  public double convertToDouble() {
    return switch(kind) {
      case INT -> (int)data;
      case LONG-> data;
      case DOUBLE -> Double.longBitsToDouble(data);
      case BIG_INTEGER -> ((BigInteger) box).doubleValue();
      default -> throw new IllegalStateException("not a numeric value " + this);
    };
  }

  public String convertToString() {
    return switch(kind) {
      case NULL -> null;
      case INT, LONG -> Long.toString(data);
      case DOUBLE -> Double.toString(Double.longBitsToDouble(data));
      case STRING -> (String) box;
      default -> box.toString();
    };
  }


  public static JsonValue nullValue() {
    return new JsonValue(Kind.NULL, 0, "null");
  }
  public static JsonValue trueValue() {
    return new JsonValue(Kind.TRUE, 0, "true");
  }
  public static JsonValue falseValue() {
    return new JsonValue(Kind.FALSE, 0, "false");
  }

  public static JsonValue from(boolean value) { return value? trueValue(): falseValue(); }
  public static JsonValue from(int value) {
    return new JsonValue(Kind.INT, value, null);
  }
  public static JsonValue from(long value) {
    return new JsonValue(Kind.LONG, value, null);
  }
  public static JsonValue from(double value) {
    return new JsonValue(Kind.DOUBLE, Double.doubleToLongBits(value), null);
  }
  public static JsonValue from(String value) {
    requireNonNull(value);
    return new JsonValue(Kind.STRING, 0, value);
  }
  public static JsonValue from(BigInteger value) {
    requireNonNull(value);
    return new JsonValue(Kind.BIG_INTEGER, 0, value);
  }
  public static JsonValue fromOpaque(Object value) {
    requireNonNull(value);
    return new JsonValue(Kind.OPAQUE, 0, value);
  }
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
