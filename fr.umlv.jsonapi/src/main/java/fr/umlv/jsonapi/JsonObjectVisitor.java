package fr.umlv.jsonapi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("preview")
public interface JsonObjectVisitor {
  JsonObjectVisitor visitMemberObject(String name);
  JsonArrayVisitor visitMemberArray(String name);
  void visitMemberString(String name, String value);
  void visitMemberNumber(String name, int value);
  void visitMemberNumber(String name, long value);
  void visitMemberNumber(String name, double value);
  void visitMemberNumber(String name, BigInteger value);
  void visitMemberNumber(String name, BigDecimal value);
  void visitMemberBoolean(String name, boolean value);
  void visitMemberNull(String name);
  void visitEndObject();

  default JsonObjectVisitor adding(String name, Object value) {
    Objects.requireNonNull(name);
    if (value instanceof JsonObject object) {
      return adding(name, object);
    }
    if (value instanceof Map<?,?> map) {
      return adding(name, map);
    }
    if (value instanceof JsonArray<?> array) {
      return adding(name, array);
    }
    if (value instanceof List<?> list) {
      return adding(name, list);
    }
    if (value instanceof String string) {
      return adding(name, string);
    }
    if (value instanceof Integer intValue) {
      return adding(name, (int) intValue);
    }
    if (value instanceof Long longValue) {
      return adding(name, (long) longValue);
    }
    if (value instanceof Double doubleValue) {
      return adding(name, (double) doubleValue);
    }
    if (value instanceof BigInteger bigInteger) {
      return adding(name, bigInteger);
    }
    if (value instanceof BigDecimal bigDecimal) {
      return adding(name, bigDecimal);
    }
    if (value instanceof Float floatValue) {
      return adding(name, (float) floatValue);
    }
    if (value instanceof Boolean booleanValue) {
      return adding(name, (boolean) booleanValue);
    }
    if (value instanceof Short shortValue) {
      return adding(name, (short) shortValue);
    }
    if (value instanceof Byte byteValue) {
      return adding(name, (byte) byteValue);
    }
    if (value == null) {
      return addingNull(name);
    }
    throw new IllegalArgumentException("invalid value " + value);
  }
  default JsonObjectVisitor adding(String name, JsonObject object) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(object);
    var objVisitor = visitMemberObject(name);
    if (objVisitor != null) {
      object.accept(objVisitor);
    }
    return this;
  }
  default JsonObjectVisitor adding(String name, Map<?,?> map) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(map);
    var objVisitor = visitMemberObject(name);
    if (objVisitor != null) {
      objVisitor.addingAll(map);
    }
    return this;
  }
  default JsonObjectVisitor adding(String name, JsonArray<?> array) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(array);
    var arrayVisitor = visitMemberArray(name);
    if (arrayVisitor != null) {
      array.accept(arrayVisitor);
    }
    return this;
  }
  default JsonObjectVisitor adding(String name, List<?> list) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(list);
    var arrayVisitor = visitMemberArray(name);
    if (arrayVisitor != null) {
      arrayVisitor.addingAll(list);
    }
    return this;
  }
  default JsonObjectVisitor adding(String name, String value) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(value);
    visitMemberString(name, value);
    return this;
  }
  default JsonObjectVisitor adding(String name, int value) {
    Objects.requireNonNull(name);
    visitMemberNumber(name, value);
    return this;
  }
  default JsonObjectVisitor adding(String name, long value) {
    Objects.requireNonNull(name);
    visitMemberNumber(name, value);
    return this;
  }
  default JsonObjectVisitor adding(String name, double value) {
    Objects.requireNonNull(name);
    visitMemberNumber(name, value);
    return this;
  }
  default JsonObjectVisitor adding(String name, BigInteger value) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(value);
    visitMemberNumber(name, value);
    return this;
  }
  default JsonObjectVisitor adding(String name, BigDecimal value) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(value);
    visitMemberNumber(name, value);
    return this;
  }
  default JsonObjectVisitor adding(String name, boolean value) {
    Objects.requireNonNull(name);
    visitMemberBoolean(name, value);
    return this;
  }
  default JsonObjectVisitor addingNull(String name) {
    Objects.requireNonNull(name);
    visitMemberNull(name);
    return this;
  }

  default JsonObjectVisitor addingAll(Map<?, ?> map) {
    map.forEach((key, value) -> adding((String) key, value));  // implicit nullcheck
    return this;
  }
  default JsonObjectVisitor addingAll(Record record) {
    try {
      for(var component: record.getClass().getRecordComponents()) {  // implicit nullcheck
        adding(component.getName(), component.getAccessor().invoke(record));
      }
      return this;
    } catch (IllegalAccessException e) {
      throw (IllegalAccessError)new IllegalAccessError().initCause(e);
    } catch (InvocationTargetException e) {
      throw rethrow(e.getCause());
    }
  }
  private static RuntimeException rethrow(Throwable cause) {
    if (cause instanceof RuntimeException) {
      throw (RuntimeException) cause;
    }
    if (cause instanceof Error) {
      throw (Error) cause;
    }
    throw new UndeclaredThrowableException(cause);
  }
}