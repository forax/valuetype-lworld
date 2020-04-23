package fr.umlv.jsonapi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings({"preview", "ChainOfInstanceofChecks",
    "OverloadedMethodsWithSameNumberOfParameters"})
public interface JsonObjectVisitor {
  JsonObjectVisitor visitMemberObject(String name);
  JsonArrayVisitor visitMemberArray(String name);
  void visitMemberText(String name, JsonText text);
  void visitMemberNumber(String name, JsonNumber number);
  void visitMemberConstant(String name, JsonConstant constant);
  void visitEndObject();

  default JsonObjectVisitor adding(String name, JsonElement element) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(element);
    if (element instanceof JsonObject object) {
      return adding(name, object);
    }
    if (element instanceof JsonArray array) {
      return adding(name, array);
    }
    if (element instanceof JsonText text) {
      return adding(name, text);
    }
    if (element instanceof JsonNumber number) {
      return adding(name, number);
    }
    if (element instanceof JsonConstant constant) {
      return adding(name, constant);
    }
    throw new IllegalArgumentException("invalid element " + element);
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
  default JsonObjectVisitor adding(String name, JsonArray array) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(array);
    var arrayVisitor = visitMemberArray(name);
    if (arrayVisitor != null) {
      array.accept(arrayVisitor);
    }
    return this;
  }
  default JsonObjectVisitor adding(String name, JsonText text) {
    Objects.requireNonNull(name);
    visitMemberText(name, text);
    return this;
  }
  default JsonObjectVisitor adding(String name, JsonNumber number) {
    Objects.requireNonNull(name);
    visitMemberNumber(name, number);
    return this;
  }
  default JsonObjectVisitor adding(String name, JsonConstant constant) {
    Objects.requireNonNull(name);
    visitMemberConstant(name, constant);
    return this;
  }

  default JsonObjectVisitor adding(String name, Object o) {
    Objects.requireNonNull(name);
    if (o == null) {
      return adding(name, JsonConstant.NULL);
    }
    if (o instanceof JsonElement element) {
      return adding(name, element);
    }
    if (o instanceof String value) {
      return adding(name, value);
    }
    if (o instanceof Integer intValue) {
      return adding(name, (int) intValue);
    }
    if (o instanceof Long longValue) {
      return adding(name, (long) longValue);
    }
    if (o instanceof Double doubleValue) {
      return adding(name, (double) doubleValue);
    }
    if (o instanceof Float floatValue) {
      return adding(name, (float) floatValue);
    }
    if (o instanceof Boolean value) {
      return adding(name, (boolean)value);
    }
    if (o instanceof Short shortValue) {
      return adding(name, (short) shortValue);
    }
    if (o instanceof Byte byteValue) {
      return adding(name, (byte) byteValue);
    }
    if (o instanceof Record record) {
      return adding(name, record);
    }
    if (o instanceof Map<?,?> map) {
      return adding(name, map);
    }
    if (o instanceof Iterable<?> list) {
      return adding(name, list);
    }
    throw new IllegalArgumentException("invalid object " + o);
  }
  default JsonObjectVisitor adding(String name, String string) {
    return adding(name, new JsonText(string));
  }
  default JsonObjectVisitor adding(String name, int value) {
    return adding(name, JsonNumber.from(value));
  }
  default JsonObjectVisitor adding(String name, long value) {
    return adding(name, JsonNumber.from(value));
  }
  default JsonObjectVisitor adding(String name, double value) {
    return adding(name, JsonNumber.from(value));
  }
  default JsonObjectVisitor adding(String name, boolean value) {
    return adding(name, value? JsonConstant.TRUE: JsonConstant.FALSE);
  }
  default JsonObjectVisitor adding(String name, Record record) {
    var objectVisitor = visitMemberObject(name);
    if (objectVisitor != null) {
      objectVisitor.addingAll(record);
      objectVisitor.visitEndObject();
    }
    return this;
  }
  default JsonObjectVisitor adding(String name, Map<?, ?> map) {
    var objectVisitor = visitMemberObject(name);
    if (objectVisitor != null) {
      objectVisitor.addingAll(map);
      objectVisitor.visitEndObject();
    }
    return this;
  }
  default JsonObjectVisitor adding(String name, Iterable<?> iterable) {
    var arrayVisitor = visitMemberArray(name);
    if (arrayVisitor != null) {
      arrayVisitor.addingAll(iterable);
      arrayVisitor.visitEndArray();
    }
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