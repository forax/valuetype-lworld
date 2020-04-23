package fr.umlv.jsonapi;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Array;
import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("preview")
public final class JsonObject extends AbstractMap<String, Object> implements JsonElement, JsonObjectVisitor {
  private static final class Shape {
    private final HashMap<String, Integer> slotMap;
    private final ArrayList<String> names;
    private final HashMap<String, JsonObject.Shape> forwardMap;  // if null => frozen
    private JsonObject.Shape frozen;

    private Shape(HashMap<String, Integer> slotMap, ArrayList<String> names, HashMap<String, JsonObject.Shape> forwardMap) {
      this.slotMap = slotMap;
      this.names = names;
      this.forwardMap = forwardMap;
    }

    private int slot(String name) {
      return slotMap.getOrDefault(name, -1);
    }

    private JsonObject.Shape forward(String name) {
      return forwardMap.computeIfAbsent(name, key -> {
        var slot = slotMap.size();
        var newSlotMap = new HashMap<>(slotMap);
        newSlotMap.put(key, slot);
        var newNames = new ArrayList<>(names);
        newNames.add(name);
        return new Shape(newSlotMap, newNames, new HashMap<>());
      });
    }

    private boolean isFrozen() {
      return forwardMap == null;
    }
    private JsonObject.Shape frozen() {
      if (frozen != null) {
        return frozen;
      }
      return frozen = new Shape(slotMap, names, null);
    }

    private static final ThreadLocal<JsonObject.Shape> ROOTS = ThreadLocal.withInitial(
        () -> new Shape(new HashMap<>(), new ArrayList<>(), new HashMap<>()));
  }

  private JsonObject.Shape shape = Shape.ROOTS.get();
  private Object[] array;

  public JsonObject() {
    array = JsonArray.OBJECT_EXEMPLAR;
  }
  public JsonObject(Map<? extends String, ?> map) {
    array = new Object[map.size()];  // implicit nullcheck
    addingAll(map);
  }
  public JsonObject(Record record) {
    Objects.requireNonNull(record);
    array = JsonArray.OBJECT_EXEMPLAR;
    addingAll(record);
  }


  @Override
  public boolean equals(Object o) {
    if (o instanceof JsonObject jsonObject) {
      if (shape != jsonObject.shape && shape.frozen != jsonObject.shape) {
        return false;
      }
      var size = shape.slotMap.size();
      return Arrays.equals(array, 0, size, jsonObject.array, 0, size);
    }
    return o instanceof Map<?,?> map && equalsMap(map);
  }
  private boolean equalsMap(Map<?,?> map) {
    var size = shape.slotMap.size();
    for(var i = 0; i < size; i++) {
      var name = shape.names.get(i);
      var value = array[i];
      if (value != null) {
        if (!value.equals(map.get(name))) {
          return false;
        }
      } else {  // value == null
        if (!map.containsKey(name) || map.get(name) != null) {
          return false;
        }
      }
    }
    return true;
  }
  @Override
  public int hashCode() {
    var size = shape.slotMap.size();
    var result = 1;
    for(var i = 0; i < (size & 0x7FFFFFFF); i++) {
      var value = array[i];
      result = 31 * result + (value == null ? 0 : value.hashCode());
    }
    return result;
  }
  @Override
  public String toString() {
    var printer = new JsonPrinter();
    accept(printer.visitObject());
    return printer.toString();
  }


  @Override
  public JsonObject adding(String name, JsonElement element) {
    JsonObjectVisitor.super.adding(name, element);
    return this;
  }
  @Override
  public JsonObject adding(String name, JsonObject object) {
    JsonObjectVisitor.super.adding(name, object);
    return this;
  }
  @Override
  public JsonObject adding(String name, JsonArray array) {
    JsonObjectVisitor.super.adding(name, array);
    return this;
  }
  @Override
  public JsonObject adding(String name, JsonText text) {
    JsonObjectVisitor.super.adding(name, text);
    return this;
  }
  @Override
  public JsonObject adding(String name, JsonNumber number) {
    JsonObjectVisitor.super.adding(name, number);
    return this;
  }
  @Override
  public JsonObject adding(String name, JsonConstant constant) {
    JsonObjectVisitor.super.adding(name, constant);
    return this;
  }


  @Override
  public JsonObject adding(String name, Object value) {
    JsonObjectVisitor.super.adding(name, value);
    return this;
  }
  @Override
  public JsonObject adding(String name, String value) {
    JsonObjectVisitor.super.adding(name, value);
    return this;
  }
  @Override
  public JsonObject adding(String name, int value) {
    JsonObjectVisitor.super.adding(name, value);
    return this;
  }
  @Override
  public JsonObject adding(String name, long value) {
    JsonObjectVisitor.super.adding(name, value);
    return this;
  }
  @Override
  public JsonObject adding(String name, double value) {
    JsonObjectVisitor.super.adding(name, value);
    return this;
  }
  @Override
  public JsonObject adding(String name, boolean value) {
    JsonObjectVisitor.super.adding(name, value);
    return this;
  }
  @Override
  public JsonObject adding(String name, Map<?,?> map) {
    JsonObjectVisitor.super.adding(name, map);
    return this;
  }
  @Override
  public JsonObject adding(String name, Record record) {
    JsonObjectVisitor.super.adding(name, record);
    return this;
  }
  @Override
  public JsonObject adding(String name, Iterable<?> list) {
    JsonObjectVisitor.super.adding(name, list);
    return this;
  }


  @Override
  public JsonObject addingAll(Map<?, ?> map) {
    JsonObjectVisitor.super.addingAll(map);
    return this;
  }
  @Override
  public JsonObject addingAll(Record record) {
    JsonObjectVisitor.super.addingAll(record);
    return this;
  }


  public JsonObject freeze() {
    if (shape.isFrozen()) {  // idempotent
      return this;
    }
    shape = shape.frozen();  // freeze the object
    return this;
  }


  private void safeAdd(String name, JsonElement value) {
    if (shape.isFrozen()) {
      throw new IllegalStateException("object is frozen");
    }
    var slot = shape.slot(name);
    if (slot != -1) {
      // twice the same name, last win :(
      array[slot] = value;
      return;
    }
    var size = shape.slotMap.size();
    if (array.length == size) {
      array = Arrays.copyOf(array, Math.max(8, (int)(size * 1.5)));
    }
    array[size] = value;
    shape = shape.forward(name);
  }
  @Override
  public JsonObjectVisitor visitMemberObject(String name) {
    Objects.requireNonNull(name);
    var object = new JsonObject();
    safeAdd(name, object);
    return object;
  }
  @Override
  public JsonArrayVisitor visitMemberArray(String name) {
    Objects.requireNonNull(name);
    var array = new JsonArray();
    safeAdd(name, array);
    return array;
  }
  @Override
  public void visitMemberText(String name, JsonText text) {
    Objects.requireNonNull(name);
    safeAdd(name, text);
  }
  @Override
  public void visitMemberNumber(String name, JsonNumber number) {
    Objects.requireNonNull(name);
    safeAdd(name, number);
  }
  @Override
  public void visitMemberConstant(String name, JsonConstant constant) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(constant);
    safeAdd(name, constant);
  }
  @Override
  public void visitEndObject() {
    freeze();
  }


  public void accept(JsonObjectVisitor visitor) {
    Objects.requireNonNull(visitor);
    var size = shape.names.size();
    for(var i = 0; i < size; i++) {
      var name = shape.names.get(i);
      var value = array[i];
      if (value instanceof JsonObject object) {
        var objVisitor = visitor.visitMemberObject(name);
        if (objVisitor != null) {
          object.accept(objVisitor);
        }
        continue;
      }
      if (value instanceof JsonArray array) {
        var arrayVisitor = visitor.visitMemberArray(name);
        if (arrayVisitor != null) {
          array.accept(arrayVisitor);
        }
        continue;
      }
      if (value instanceof JsonText text) {
        visitor.visitMemberText(name, text);
        continue;
      }
      if (value instanceof JsonNumber number) {
        visitor.visitMemberNumber(name, number);
        continue;
      }
      if (value instanceof JsonConstant constant) {
        visitor.visitMemberConstant(name, constant);
        continue;
      }
      throw new AssertionError("invalid value");
    }
    visitor.visitEndObject();
  }


  public <R extends Record> R toRecord(Lookup lookup, Class<R> type) {
    Objects.requireNonNull(lookup);
    Objects.requireNonNull(type);
    var components = type.getRecordComponents();
    if (components.length != shape.slotMap.size()) {
      throw new IllegalStateException("wrong number of components");
    }
    var arguments = new Object[components.length];
    var componentsTypes = new Class<?>[components.length];
    for(var i = 0; i < components.length; i++) {
      var component = components[i];
      var name = component.getName();
      var slot = shape.slot(name);
      if (slot == -1) {
        throw new IllegalStateException("no value for component " + name);
      }
      arguments[i] = array[i];
      componentsTypes[i] = component.getType();
    }
    MethodHandle constructor;
    try {
      constructor = lookup.findConstructor(type, MethodType.methodType(void.class, componentsTypes));
    } catch (NoSuchMethodException e) {
      throw (NoSuchMethodError)new NoSuchMethodError().initCause(e);
    } catch (IllegalAccessException e) {
      throw (IllegalAccessError)new IllegalAccessError().initCause(e);
    }
    try {
      return type.cast(constructor.invokeWithArguments(arguments));
    } catch (Throwable throwable) {
      throw rethrow(throwable);
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

  @Override
  public int size() {
    return shape.slotMap.size();
  }
  @Override
  public Object get(Object key) {
    return getOrDefault(key, null);
  }
  @Override
  public Object getOrDefault(Object key, Object defaultValue) {
    if (!(key instanceof String name)) {
      return defaultValue;
    }
    var slot = shape.slot(name);
    if (slot == -1) {
      return defaultValue;
    }
    return array[slot];
  }
  public int getOrDefaultInt(String name, int defaultValue) {
    Objects.requireNonNull(name);
    var slot = shape.slot(name);
    if (slot == -1) {
      return defaultValue;
    }
    return (Integer)array[slot];
  }
  public long getOrDefaultLong(String name, long defaultValue) {
    Objects.requireNonNull(name);
    var slot = shape.slot(name);
    if (slot == -1) {
      return defaultValue;
    }
    return (Long)array[slot];
  }
  public double getOrDefaultDouble(String name, double defaultValue) {
    Objects.requireNonNull(name);
    var slot = shape.slot(name);
    if (slot == -1) {
      return defaultValue;
    }
    return (Double)array[slot];
  }
  public boolean getOrDefaultBoolean(String name, boolean defaultValue) {
    Objects.requireNonNull(name);
    var slot = shape.slot(name);
    if (slot == -1) {
      return defaultValue;
    }
    return (Boolean)array[slot];
  }
  public boolean isNull(String name) {
    Objects.requireNonNull(name);
    var slot = shape.slot(name);
    if (slot == -1) {
      return false;
    }
    return array[slot] == null;
  }

  @Override
  public boolean containsKey(Object key) {
    return key instanceof String name && shape.slot(name) != -1;
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    var size = this.size();
    var array = this.array;
    var shape = this.shape;
    @__inline__ class EntrySet implements Set<Entry<String, Object>> {
      @Override
      public int size() {
        return size;
      }
      @Override
      public boolean isEmpty() {
        return size == 0;
      }

      @Override
      public boolean contains(Object o) {
        for(var i = 0; i < size; i++) {
          if (Objects.equals(array[i], o)) {
            return true;
          }
        }
        return false;
      }
      @Override
      public boolean containsAll(Collection<?> collection) {
        for(var element: collection) {
          if (!contains(element)) {
            return false;
          }
        }
        return true;
      }

      @Override
      public boolean add(Entry<String, Object> stringObjectEntry) {
        adding(stringObjectEntry.getKey(), stringObjectEntry.getValue());
        return true;
      }
      @Override
      public boolean addAll(Collection<? extends Entry<String, Object>> collection) {
        if (collection.isEmpty()) {
          return false;
        }
        for(var entry: collection) {
          adding(entry.getKey(), entry.getValue());
        }
        return true;
      }

      @Override
      public Iterator<Entry<String, Object>> iterator() {
        return new Iterator<>() {
          private int index;

          @Override
          public boolean hasNext() {
            return index < size;
          }

          @Override
          public Entry<String, Object> next() {
            if (!hasNext()) {
              throw new NoSuchElementException();
            }
            return Map.entry(shape.names.get(index), array[index++]);
          }
        };
      }

      @Override
      public Object[] toArray() {  //FIXME should return entries
        var newArray = new Object[size];
        System.arraycopy(array, 0, newArray, 0, size);
        return newArray;
      }
      @Override
      @SuppressWarnings("unchecked")
      public <T> T[] toArray(T[] anArray) {  //FIXME should return entries
        if (anArray.length < size) {
          anArray = (T[])Array.newInstance(anArray.getClass().componentType(), size);
        }
        System.arraycopy(array, 0, anArray, 0, size);
        if (anArray.length > size) {
          anArray[size] = null;
        }
        return anArray;
      }

      @Override
      public boolean remove(Object o) {
        throw new UnsupportedOperationException();
      }
      @Override
      public boolean retainAll(Collection<?> collection) {
        throw new UnsupportedOperationException();
      }
      @Override
      public boolean removeAll(Collection<?> collection) {
        throw new UnsupportedOperationException();
      }
      @Override
      public void clear() {
        throw new UnsupportedOperationException();
      }
    }
    return new EntrySet();
  }
}