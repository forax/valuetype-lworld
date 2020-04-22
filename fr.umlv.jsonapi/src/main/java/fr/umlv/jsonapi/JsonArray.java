package fr.umlv.jsonapi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.RandomAccess;

@SuppressWarnings("preview")
public final class JsonArray<E> extends AbstractList<E> implements JsonArrayVisitor {
  private Object[] array;  // if FROZEN_ARRAY => frozen
  private int size;   // if negative => frozen

  private JsonArray(Object[] array) {
    this.array = array;
  }
  public JsonArray() {
    this(OBJECT_EXEMPLAR);
  }
  public JsonArray(List<? extends E> list) {
    array = new Object[list.size()];   // implicit nullcheck
    addingAll(list);
  }


  @Override
  public boolean equals(Object o) {
    return (o instanceof JsonArray<?> jsonArray
        && (size & 0x7FFFFFFF) == (jsonArray.size & 0x7FFFFFFF)
        && Arrays.equals(array, 0, size, jsonArray.array, 0, size))
        || (o instanceof List<?> list && equalsList(list));
  }
  private boolean equalsList(List<?> list) {
    var size = this.size & 0x7FFFFFFF;
    if (size != list.size()) {
      return false;
    }
    if (!(list instanceof RandomAccess)) {
      return equalsIterator(list, size);
    }
    for(var i = 0; i < size; i++) {
      if (!Objects.equals(unwrap(array[i]), list.get(i))) {
        return false;
      }
    }
    return true;
  }
  private boolean equalsIterator(List<?> list, int size) {
    var it = list.iterator();
    for(var i = 0; i < size; i++) {
      if (!Objects.equals(unwrap(array[i]), it.next())) {
        return false;
      }
    }
    return true;
  }
  @Override
  public int hashCode() {
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
    accept(printer.visitArray());
    return printer.toString();
  }


  @Override
  public JsonArray<E> adding(Object value) {
    JsonArrayVisitor.super.adding(value);
    return this;
  }
  @Override
  public JsonArray<E> adding(JsonObject object) {
    JsonArrayVisitor.super.adding(object);
    return this;
  }
  @Override
  public JsonArray<E> adding(Map<?,?> map) {
    JsonArrayVisitor.super.adding(map);
    return this;
  }
  @Override
  public JsonArray<E> adding(JsonArray<?> array) {
    JsonArrayVisitor.super.adding(array);
    return this;
  }
  @Override
  public JsonArray<E> adding(List<?> list) {
    JsonArrayVisitor.super.adding(list);
    return this;
  }
  @Override
  public JsonArray<E> adding(String value) {
    JsonArrayVisitor.super.adding(value);
    return this;
  }
  @Override
  public JsonArray<E> adding(int value) {
    JsonArrayVisitor.super.adding(value);
    return this;
  }
  @Override
  public JsonArray<E> adding(long value) {
    JsonArrayVisitor.super.adding(value);
    return this;
  }
  @Override
  public JsonArray<E> adding(double value) {
    JsonArrayVisitor.super.adding(value);
    return this;
  }
  @Override
  public JsonArray<E> adding(BigInteger value) {
    JsonArrayVisitor.super.adding(value);
    return this;
  }
  @Override
  public JsonArray<E> adding(BigDecimal value) {
    JsonArrayVisitor.super.adding(value);
    return this;
  }
  @Override
  public JsonArray<E> adding(boolean value) {
    JsonArrayVisitor.super.adding(value);
    return this;
  }
  @Override
  public JsonArray<E> addingNull() {
    JsonArrayVisitor.super.addingNull();
    return this;
  }

  @Override
  public JsonArray<E> addingAll(List<?> list) {
    JsonArrayVisitor.super.addingAll(list);
    return this;
  }


  private void safeAppend(Object object) {
    if (isFrozen()) {
      throw new IllegalStateException("array is frozen");
    }
    if (array.length == size) {
      array = Arrays.copyOf(array, Math.max(8, (int)(size * 1.5)));
    }
    array[size++] = object;
  }

  private boolean isFrozen() {
    return size < 0 || array == FROZEN_ARRAY;
  }
  public JsonArray<E> freeze() {
    if (isFrozen()) {
      throw new IllegalStateException("already frozen");
    }
    // freeze: if size == 0, use a specific array, otherwise use the sign bit
    if (size == 0) {
      array = FROZEN_ARRAY;
    } else {
      size = -size;
    }
    return this;
  }


  @Override
  public JsonObjectVisitor visitObject() {
    var object =  new JsonObject();
    safeAppend(object);
    return object;
  }
  @Override
  public JsonArrayVisitor visitArray() {
    var array = new JsonArray<>();
    safeAppend(array);
    return array;
  }
  @Override
  public void visitString(String value) {
    Objects.requireNonNull(value);
    safeAppend(value);
  }
  @Override
  public void visitNumber(int value) {
    safeAppend(new PrimitiveInt(value));
  }
  @Override
  public void visitNumber(long value) {
    safeAppend(new PrimitiveLong(value));
  }
  @Override
  public void visitNumber(double value) {
    safeAppend(new PrimitiveDouble(value));
  }
  @Override
  public void visitNumber(BigInteger value) {
    Objects.requireNonNull(value);
    safeAppend(value);
  }
  @Override
  public void visitNumber(BigDecimal value) {
    Objects.requireNonNull(value);
    safeAppend(value);
  }
  @Override
  public void visitBoolean(boolean value) {
    safeAppend(value? PrimitiveBoolean.TRUE: PrimitiveBoolean.FALSE);
  }
  @Override
  public void visitNull() {
    safeAppend(null);
  }
  @Override
  public void visitEndArray() {
    freeze();
  }

  @Override
  public int size() {
    return size & 0x7FFFFFFF;
  }

  @SuppressWarnings("unchecked")
  private E unwrap(Object value) {
    if (value instanceof JsonArray.PrimitiveInt primitiveInt) {
      return (E)(Integer)primitiveInt.value;
    }
    if (value instanceof JsonArray.PrimitiveLong primitiveLong) {
      return (E)(Long)primitiveLong.value;
    }
    if (value instanceof JsonArray.PrimitiveDouble primitiveDouble) {
      return (E)(Double)primitiveDouble.value;
    }
    if (value instanceof JsonArray.PrimitiveBoolean primitiveBoolean) {
      return (E)(Boolean)primitiveBoolean.value;
    }
    return (E)value;
  }
  @Override
  public E get(int index) {
    Objects.checkIndex(0, size & 0x7FFFFFFF);
    return unwrap(array[index]);
  }
  public long getInt(int index) {
    Objects.checkIndex(0, size & 0x7FFFFFFF);
    return ((JsonArray.PrimitiveInt)array[index]).value;
  }
  public long getLong(int index) {
    Objects.checkIndex(0, size & 0x7FFFFFFF);
    return ((JsonArray.PrimitiveLong)array[index]).value;
  }
  public double getDouble(int index) {
    Objects.checkIndex(0, size & 0x7FFFFFFF);
    return ((JsonArray.PrimitiveDouble)array[index]).value;
  }
  public boolean getBoolean(int index) {
    Objects.checkIndex(0, size & 0x7FFFFFFF);
    return ((JsonArray.PrimitiveBoolean)array[index]).value;
  }


  public void accept(JsonArrayVisitor visitor) {
    Objects.requireNonNull(visitor);
    var size = this.size & 0x7FFFFFFF;
    for(var i = 0; i < size; i++) {
      var value = array[i];
      if (value instanceof JsonObject object) {
        var objVisitor = visitor.visitObject();
        if (objVisitor != null) {
          object.accept(objVisitor);
        }
        continue;
      }
      if (value instanceof JsonArray<?> array) {
        var arrayVisitor = visitor.visitArray();
        if (arrayVisitor != null) {
          array.accept(arrayVisitor);
        }
        continue;
      }
      if (value instanceof String string) {
        visitor.visitString(string);
        continue;
      }
      if (value instanceof JsonArray.PrimitiveInt primitiveInt) {
        visitor.visitNumber(primitiveInt.value);
        continue;
      }
      if (value instanceof JsonArray.PrimitiveLong primitiveLong) {
        visitor.visitNumber(primitiveLong.value);
        continue;
      }
      if (value instanceof JsonArray.PrimitiveDouble primitiveDouble) {
        visitor.visitNumber(primitiveDouble.value);
        continue;
      }
      if (value instanceof BigInteger bigInteger) {
        visitor.visitNumber(bigInteger);
        continue;
      }
      if (value instanceof BigDecimal bigDecimal) {
        visitor.visitNumber(bigDecimal);
        continue;
      }
      if (value instanceof JsonArray.PrimitiveBoolean primitiveBoolean) {
        visitor.visitBoolean(primitiveBoolean.value);
        continue;
      }
      if (value == null) {
        visitor.visitNull();
        continue;
      }
      throw new IllegalStateException("invalid value " + value);
    }
    visitor.visitEndArray();
  }


  static final Object[] OBJECT_EXEMPLAR = new Object[0];
  private static final Object[] FROZEN_ARRAY = new Object[0];

  public static JsonArray<Object> objectArray() {
    return new JsonArray<>();
  }
  public static <T> JsonArray<T> objectArray(T[] emptyArray) {
    if (emptyArray.length != 0) {  // implicit nullcheck
      throw new IllegalArgumentException();
    }
    return new JsonArray<>(emptyArray);
  }
  public static JsonArray<Long> intArray() {
    return new JsonArray<>(PrimitiveInt.EXEMPLAR);
  }
  public static JsonArray<Long> longArray() {
    return new JsonArray<>(PrimitiveLong.EXEMPLAR);
  }
  public static JsonArray<Long> doubleArray() {
    return new JsonArray<>(PrimitiveDouble.EXEMPLAR);
  }
  public static JsonArray<Long> booleanArray() {
    return new JsonArray<>(PrimitiveBoolean.EXEMPLAR);
  }

  @SafeVarargs
  public static <E> JsonArray<E> of(E... elements) {
    var array = new JsonArray<E>(new Object[elements.length]);  // implicit nullcheck
    for(var element: elements) {
      array.adding(element);
    }
    return array.freeze();
  }


  private static final @__inline__ class PrimitiveInt {
    private final int value;

    private PrimitiveInt(int value) { this.value = value; }

    private static final JsonArray.PrimitiveInt[] EXEMPLAR = new JsonArray.PrimitiveInt[0];
  }
  private static final @__inline__ class PrimitiveLong {
    private final long value;

    private PrimitiveLong(long value) { this.value = value; }

    private static final JsonArray.PrimitiveLong[] EXEMPLAR = new JsonArray.PrimitiveLong[0];
  }
  private static final @__inline__ class PrimitiveDouble {
    private final double value;

    private PrimitiveDouble(double value) { this.value = value; }

    private static final JsonArray.PrimitiveDouble[] EXEMPLAR = new JsonArray.PrimitiveDouble[0];
  }
  private static final @__inline__ class PrimitiveBoolean {
    private final boolean value;

    private PrimitiveBoolean(boolean value) { this.value = value; }

    private static final JsonArray.PrimitiveBoolean TRUE = new PrimitiveBoolean(true);
    private static final JsonArray.PrimitiveBoolean FALSE = new PrimitiveBoolean(false);
    private static final JsonArray.PrimitiveBoolean[] EXEMPLAR = new JsonArray.PrimitiveBoolean[0];
  }
}