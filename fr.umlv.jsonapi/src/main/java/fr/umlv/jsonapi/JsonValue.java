package fr.umlv.jsonapi;

import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

public final @__inline__ class JsonValue {
  private final Kind kind;
  private final long data;
  private final String string;

  public enum Kind {
    NULL(void.class),
    TRUE(boolean.class),
    FALSE(boolean.class),
    INT(int.class),
    LONG(long.class),
    DOUBLE(double.class),
    STRING(String.class),
    BIG_INTEGER(BigInteger.class),
    BIG_DECIMAL(BigDecimal.class)
    ;

    private final Class<?> type;

    Kind(Class<?> type) {
      this.type = type;
    }
  }

  private JsonValue(Kind kind, long data, String string) {
    this.kind = kind;
    this.data = data;
    this.string = string;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof JsonValue value
        && kind == value.kind
        && data == value.data
        && Objects.equals(string, value.string);
  }

  @Override
  public int hashCode() {
    if (string != null) {
      return string.hashCode();
    }
    return (int)(data ^ data >>> 32);
  }

  @Override
  public String toString() {
    return switch(kind) {
      case INT, LONG -> Long.toString(data);
      case DOUBLE -> Double.toString(Double.longBitsToDouble(data));
      case STRING -> '"' + string + '"';   // FIXME escape
      default -> string;
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
  public boolean isBigDecimal() {
    return kind == Kind.BIG_DECIMAL;
  }

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
    return string;
  }
  public BigInteger bigIntegerValue() {
    if (kind != Kind.BIG_INTEGER) {
      throw new IllegalStateException("not a BigInteger: " + this);
    }
    return new BigInteger(string);
  }
  public BigDecimal bigDecimalValue() {
    if (kind != Kind.BIG_DECIMAL) {
      throw new IllegalStateException("not a BigDecimal: " + this);
    }
    return new BigDecimal(string);
  }

  public Object asObject() {
    return switch(kind) {
      case NULL -> null;
      case TRUE -> true;
      case FALSE -> false;
      case INT -> (int)data;
      case LONG-> data;
      case DOUBLE -> Double.longBitsToDouble(data);
      case STRING -> string;
      case BIG_INTEGER -> new BigInteger(string);
      case BIG_DECIMAL -> new BigDecimal(string);
    };
  }

  public double convertToDouble() {
    return switch(kind) {
      case INT -> (int)data;
      case LONG-> data;
      case DOUBLE -> Double.longBitsToDouble(data);
      case BIG_INTEGER, BIG_DECIMAL -> Double.parseDouble(string);
      default -> throw new IllegalStateException("not a numeric value " + this);
    };
  }

  public String convertToString() {
    return switch(kind) {
      case INT, LONG -> Long.toString(data);
      case DOUBLE -> Double.toString(Double.longBitsToDouble(data));
      default -> string;
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
    return new JsonValue(Kind.BIG_INTEGER, 0, value.toString());  // implicit nullcheck
  }
  public static JsonValue from(BigDecimal value) {
    return new JsonValue(Kind.BIG_DECIMAL, 0, value.toString());  // implicit nullcheck
  }

  static JsonValue fromBigInteger(String value) {
    return new JsonValue(Kind.BIG_INTEGER, 0, value);
  }
  static JsonValue fromBigDecimal(String value) {
    return new JsonValue(Kind.BIG_DECIMAL, 0, value);
  }
}
