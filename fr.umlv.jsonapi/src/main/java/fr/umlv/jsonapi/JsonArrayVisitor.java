package fr.umlv.jsonapi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("preview")
public interface JsonArrayVisitor extends JsonVisitor {
  @Override
  JsonObjectVisitor visitObject();
  @Override
  JsonArrayVisitor visitArray();
  void visitString(String value);
  void visitNumber(int value);
  void visitNumber(long value);
  void visitNumber(double value);
  void visitNumber(BigInteger value);
  void visitBoolean(boolean value);
  void visitNull();
  void visitEndArray();

  default JsonArrayVisitor adding(Object value) {
    if (value instanceof JsonObject object) {
      return adding(object);
    }
    if (value instanceof Map<?,?> map) {
      return adding(map);
    }
    if (value instanceof JsonArray<?> array) {
      return adding(array);
    }
    if (value instanceof List<?> list) {
      return adding(list);
    }
    if (value instanceof String string) {
      return adding(string);
    }
    if (value instanceof Integer intValue) {
      return adding((int) intValue);
    }
    if (value instanceof Long longValue) {
      return adding((long) longValue);
    }
    if (value instanceof Double doubleValue) {
      return adding((double) doubleValue);
    }
    if (value instanceof BigInteger bigInteger) {
      return adding(bigInteger);
    }
    if (value instanceof Float floatValue) {
      return adding((float) floatValue);
    }
    if (value instanceof Boolean booleanValue) {
      return adding((boolean) booleanValue);
    }
    if (value instanceof Short shortValue) {
      return adding((short) shortValue);
    }
    if (value instanceof Byte byteValue) {
      return adding((byte) byteValue);
    }
    if (value == null) {
      return addingNull();
    }
    throw new IllegalArgumentException("invalid value " + value);
  }
  default JsonArrayVisitor adding(JsonObject object) {
    Objects.requireNonNull(object);
    var objVisitor = visitObject();
    if (objVisitor != null) {
      object.accept(objVisitor);
    }
    return this;
  }
  default JsonArrayVisitor adding(Map<?,?> map) {
    Objects.requireNonNull(map);
    var objVisitor = visitObject();
    if (objVisitor != null) {
      objVisitor.addingAll(map);
    }
    return this;
  }
  default JsonArrayVisitor adding(JsonArray<?> array) {
    Objects.requireNonNull(array);
    var arrayVisitor = visitArray();
    if (arrayVisitor != null) {
      array.accept(arrayVisitor);
    }
    return this;
  }
  default JsonArrayVisitor adding(List<?> list) {
    Objects.requireNonNull(list);
    var arrayVisitor = visitArray();
    if (arrayVisitor != null) {
      arrayVisitor.addingAll(list);
    }
    return this;
  }
  default JsonArrayVisitor adding(String value) {
    Objects.requireNonNull(value);
    visitString(value);
    return this;
  }
  default JsonArrayVisitor adding(int value) {
    visitNumber(value);
    return this;
  }
  default JsonArrayVisitor adding(long value) {
    visitNumber(value);
    return this;
  }
  default JsonArrayVisitor adding(double value) {
    visitNumber(value);
    return this;
  }
  default JsonArrayVisitor adding(BigInteger value) {
    Objects.requireNonNull(value);
    visitNumber(value);
    return this;
  }
  default JsonArrayVisitor adding(boolean value) {
    visitBoolean(value);
    return this;
  }
  default JsonArrayVisitor addingNull() {
    visitNull();
    return this;
  }

  default JsonArrayVisitor addingAll(List<?> list) {
    for(var value: list) {  // implicit null check
      adding(value);
    }
    return this;
  }
}