package fr.umlv.jsonapi;

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.RandomAccess;

@SuppressWarnings("preview")
public final class JsonArray extends AbstractList<JsonElement> implements JsonElement, JsonArrayVisitor {
  private JsonElement[] array;  // if FROZEN_ARRAY => frozen
  private int size;   // if negative => frozen

  private JsonArray(JsonElement[] array) {
    this.array = array;
  }
  public JsonArray() {
    this(OBJECT_EXEMPLAR);
  }
  public JsonArray(List<?> list) {
    array = new JsonElement[list.size()];   // implicit nullcheck
    addingAll(list);
  }


  @Override
  public boolean equals(Object o) {
    return (o instanceof JsonArray jsonArray
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
      if (!Objects.equals(array[i], list.get(i))) {
        return false;
      }
    }
    return true;
  }
  private boolean equalsIterator(List<?> list, int size) {
    var it = list.iterator();
    for(var i = 0; i < size; i++) {
      if (!Objects.equals(array[i], it.next())) {
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
  public JsonArray adding(JsonElement element) {
    JsonArrayVisitor.super.adding(element);
    return this;
  }
  @Override
  public JsonArray adding(JsonObject object) {
    JsonArrayVisitor.super.adding(object);
    return this;
  }
  @Override
  public JsonArray adding(JsonArray array) {
    JsonArrayVisitor.super.adding(array);
    return this;
  }
  @Override
  public  JsonArray adding(JsonText value) {
    JsonArrayVisitor.super.adding(array);
    return this;
  }
  @Override
  public  JsonArray adding(JsonNumber number) {
    JsonArrayVisitor.super.adding(array);
    return this;
  }
  @Override
  public  JsonArray adding(JsonConstant constant) {
    JsonArrayVisitor.super.adding(array);
    return this;
  }


  @Override
  public JsonArray adding(Object value) {
    JsonArrayVisitor.super.adding(value);
    return this;
  }
  @Override
  public JsonArray adding(String value) {
    JsonArrayVisitor.super.adding(value);
    return this;
  }
  @Override
  public JsonArray adding(int value) {
    JsonArrayVisitor.super.adding(value);
    return this;
  }
  @Override
  public JsonArray adding(long value) {
    JsonArrayVisitor.super.adding(value);
    return this;
  }
  @Override
  public JsonArray adding(double value) {
    JsonArrayVisitor.super.adding(value);
    return this;
  }
  @Override
  public JsonArray adding(boolean value) {
    JsonArrayVisitor.super.adding(value);
    return this;
  }
  @Override
  public JsonArray adding(Record record) {
    JsonArrayVisitor.super.adding(record);
    return this;
  }
  @Override
  public JsonArray adding(Map<?,?> map) {
    JsonArrayVisitor.super.adding(map);
    return this;
  }
  @Override
  public JsonArray adding(Iterable<?> iterable) {
    JsonArrayVisitor.super.adding(iterable);
    return this;
  }


  @Override
  public JsonArray addingAll(Iterable<?> iterable) {
    JsonArrayVisitor.super.addingAll(iterable);
    return this;
  }

  private boolean isFrozen() {
    return size < 0 || array == FROZEN_ARRAY;
  }
  public JsonArray freeze() {
    if (isFrozen()) {
      return this;  // idempotent
    }
    // freeze: if size == 0, use a specific array, otherwise use the sign bit
    if (size == 0) {
      array = FROZEN_ARRAY;
    } else {
      size = -size;
    }
    return this;
  }

  private void safeAppend(JsonElement element) {
    if (isFrozen()) {
      throw new IllegalStateException("array is frozen");
    }
    if (array.length == size) {
      array = Arrays.copyOf(array, Math.max(8, (int)(size * 1.5)));
    }
    array[size++] = element;
  }
  @Override
  public JsonObjectVisitor visitObject() {
    var object =  new JsonObject();
    safeAppend(object);
    return object;
  }
  @Override
  public JsonArrayVisitor visitArray() {
    var array = new JsonArray();
    safeAppend(array);
    return array;
  }
  @Override
  public void visitText(JsonText text) {
    safeAppend(text);
  }
  @Override
  public void visitNumber(JsonNumber number) {
    safeAppend(number);
  }
  @Override
  public void visitConstant(JsonConstant value) {
    safeAppend(value);
  }
  @Override
  public void visitEndArray() {
    freeze();
  }

  @Override
  public int size() {
    return size & 0x7FFFFFFF;
  }

  @Override
  public JsonElement get(int index) {
    Objects.checkIndex(0, size & 0x7FFFFFFF);
    return array[index];
  }
  private <E extends JsonElement> E get(int index, Class<? extends E> type) {
    Objects.checkIndex(0, size & 0x7FFFFFFF);
    return type.cast(array[index]);
  }
  public JsonObject getObject(int index) {
    return get(index, JsonObject.class);
  }
  public JsonArray getArray(int index) {
    return get(index, JsonArray.class);
  }
  public JsonText getText(int index) {
    return get(index, JsonText.class);
  }
  public JsonNumber getNumber(int index) {
    return get(index, JsonNumber.class);
  }
  public JsonConstant getConstant(int index) {
    return get(index, JsonConstant.class);
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
      if (value instanceof JsonArray array) {
        var arrayVisitor = visitor.visitArray();
        if (arrayVisitor != null) {
          array.accept(arrayVisitor);
        }
        continue;
      }
      if (value instanceof JsonText text) {
        visitor.visitText(text);
        continue;
      }
      if (value instanceof JsonNumber number) {
        visitor.visitNumber(number);
        continue;
      }
      if (value instanceof JsonConstant constant) {
        visitor.visitConstant(constant);
        continue;
      }
      throw new AssertionError("invalid value");
    }
    visitor.visitEndArray();
  }


  static final JsonElement[] OBJECT_EXEMPLAR = new JsonElement[0];
  private static final JsonElement[] FROZEN_ARRAY = new JsonElement[0];

  public static JsonArray of(Object... elements) {
    var array = new JsonArray(new JsonElement[elements.length]);  // implicit nullcheck
    for(var element: elements) {
      array.adding(element);
    }
    return array.freeze();
  }
  @SafeVarargs
  public static <E extends JsonElement> JsonArray of(E... elements) {
    // ask for flattening if it's an array of JsonNumber or JsonText
    var array = (JsonElement[]) Array.newInstance(elements.getClass().getComponentType(), elements.length);  // implicit nullcheck
    var jsonArray = new JsonArray(array);
    for(var element: elements) {
      jsonArray.adding(element);
    }
    return jsonArray.freeze();
  }
}