package fr.umlv.jsonapi;

import java.util.Map;
import java.util.Objects;

@SuppressWarnings({"preview", "OverloadedMethodsWithSameNumberOfParameters"})
public interface JsonArrayVisitor extends JsonVisitor {
  @Override
  JsonObjectVisitor visitObject();
  @Override
  JsonArrayVisitor visitArray();
  void visitText(JsonText text);
  void visitNumber(JsonNumber number);
  void visitConstant(JsonConstant constant);
  void visitEndArray();

  default JsonArrayVisitor adding(JsonElement element) {
    Objects.requireNonNull(element);
    if (element instanceof JsonObject object) {
      return adding(object);
    }
    if (element instanceof JsonArray array) {
      return adding(array);
    }
    if (element instanceof JsonText text) {
      return adding(text);
    }
    if (element instanceof JsonNumber number) {
      return adding(number);
    }
    if (element instanceof JsonConstant constant) {
      return adding(constant);
    }
    throw new IllegalArgumentException("invalid element " + element);
  }
  default JsonArrayVisitor adding(JsonObject object) {
    Objects.requireNonNull(object);
    var objVisitor = visitObject();
    if (objVisitor != null) {
      object.accept(objVisitor);
    }
    return this;
  }
  default JsonArrayVisitor adding(JsonArray array) {
    Objects.requireNonNull(array);
    var arrayVisitor = visitArray();
    if (arrayVisitor != null) {
      array.accept(arrayVisitor);
    }
    return this;
  }
  default JsonArrayVisitor adding(JsonText value) {
    visitText(value);
    return this;
  }
  default JsonArrayVisitor adding(JsonNumber number) {
    visitNumber(number);
    return this;
  }
  default JsonArrayVisitor adding(JsonConstant constant) {
    Objects.requireNonNull(constant);
    visitConstant(constant);
    return this;
  }

  default JsonArrayVisitor adding(Object value) {
    if (value == null) {
      return adding(JsonConstant.NULL);
    }
    if (value instanceof JsonElement element) {
      return adding(element);
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
    if (value instanceof Record record) {
      return adding(record);
    }
    if (value instanceof Map<?,?> map) {
      return adding(map);
    }
    if (value instanceof Iterable<?> iterable) {
      return adding(iterable);
    }
    throw new IllegalArgumentException("invalid value " + value);
  }

  default JsonArrayVisitor adding(String value) {
    Objects.requireNonNull(value);
    visitText(new JsonText(value));
    return this;
  }
  default JsonArrayVisitor adding(int value) {
    visitNumber(JsonNumber.from(value));
    return this;
  }
  default JsonArrayVisitor adding(long value) {
    visitNumber(JsonNumber.from(value));
    return this;
  }
  default JsonArrayVisitor adding(double value) {
    visitNumber(JsonNumber.from(value));
    return this;
  }
  default JsonArrayVisitor adding(boolean value) {
    visitConstant(value? JsonConstant.TRUE: JsonConstant.FALSE);
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
  default JsonArrayVisitor adding(Iterable<?> list) {
    Objects.requireNonNull(list);
    var arrayVisitor = visitArray();
    if (arrayVisitor != null) {
      arrayVisitor.addingAll(list);
    }
    return this;
  }
  default JsonArrayVisitor adding(Record record) {
    Objects.requireNonNull(record);
    var objVisitor = visitObject();
    if (objVisitor != null) {
      objVisitor.addingAll(record);
    }
    return this;
  }

  default JsonArrayVisitor addingAll(Iterable<?> iterable) {
    for(var value: iterable) {  // implicit null check
      adding(value);
    }
    return this;
  }
}