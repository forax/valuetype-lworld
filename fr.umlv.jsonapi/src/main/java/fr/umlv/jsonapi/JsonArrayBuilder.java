package fr.umlv.jsonapi;

import static java.util.Objects.requireNonNull;

import fr.umlv.jsonapi.JsonObjectBuilder.Factory;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public final class JsonArrayBuilder implements JsonArrayVisitor {
  private final List<Object> list;
  private final Factory factory;
  private final Consumer<List<Object>> postOp;

  JsonArrayBuilder(Factory factory, Consumer<List<Object>> postOp) {
    this.list = requireNonNull(factory.listSupplier().get());
    this.factory = factory;
    this.postOp = postOp;
  }

  public JsonArrayBuilder(Supplier<? extends Map<String, Object>> mapSupplier,
      UnaryOperator<Map<String, Object>> transformMapOp,
      Supplier<? extends List<Object>> listSupplier,
      UnaryOperator<List<Object>> transformListOp) {
    this(new Factory(mapSupplier, transformMapOp, listSupplier, transformListOp), __ -> {});
  }
  public JsonArrayBuilder(Supplier<? extends Map<String, Object>> mapSupplier, Supplier<? extends List<Object>> listSupplier) {
    this(mapSupplier, UnaryOperator.identity(), listSupplier, UnaryOperator.identity());
  }
  public JsonArrayBuilder() {
    this(HashMap::new, ArrayList::new);
  }


  @Override
  public boolean equals(Object o) {
    return o instanceof JsonArrayBuilder builder && list.equals(builder.list);
  }
  @Override
  public int hashCode() {
    return list.hashCode();
  }

  @Override
  public String toString() {
    return list.stream().map(String::valueOf).collect(Collectors.joining(", ", "[", "]"));
  }

  public JsonArrayBuilder add(Object value) {
    requireNonNull(value);
    list.add(value);
    return this;
  }

  public JsonArrayBuilder addAll(List<?> list) {
    list.forEach(this.list::add);  // implicit nullcheck
    return this;
  }
  public JsonArrayBuilder addAll(Object... values) {
    for(var value: values) {   // implicit nullcheck
      list.add(value);
    }
    return this;
  }

  public List<Object> toList() {
    var resultList = factory.transformListOp().apply(list);
    postOp.accept(resultList);
    return resultList;
  }

  @Override
  public JsonObjectBuilder visitObject() {
    return new JsonObjectBuilder(factory, list::add);
  }

  @Override
  public JsonArrayBuilder visitArray() {
    return new JsonArrayBuilder(factory, list::add);
  }

  @Override
  public void visitValue(JsonValue value) {
    list.add(value.asObject());
  }

  @Override
  public List<Object> visitEndArray() {
    return toList();
  }

  static Object visitList(List<?> list, JsonArrayVisitor arrayVisitor) {
    for(Object element: list) {
      if (element == null) {
        arrayVisitor.visitValue(JsonValue.nullValue());
        continue;
      }
      if (element instanceof Boolean value) {
        arrayVisitor.visitValue(JsonValue.from(value));
        continue;
      }
      if (element instanceof Integer value) {
        arrayVisitor.visitValue(JsonValue.from(value));
        continue;
      }
      if (element instanceof Long value) {
        arrayVisitor.visitValue(JsonValue.from(value));
        continue;
      }
      if (element instanceof Double value) {
        arrayVisitor.visitValue(JsonValue.from(value));
        continue;
      }
      if (element instanceof String value) {
        arrayVisitor.visitValue(JsonValue.from(value));
        continue;
      }
      if (element instanceof BigInteger value) {
        arrayVisitor.visitValue(JsonValue.from(value));
        continue;
      }
      if (element instanceof BigDecimal value) {
        arrayVisitor.visitValue(JsonValue.from(value));
        continue;
      }
      if (element instanceof Map<?,?> map) {
        var visitor = arrayVisitor.visitObject();
        if (visitor != null) {
          JsonObjectBuilder.visitMap(map, visitor);
          visitor.visitEndObject();
        }
        continue;
      }
      if (element instanceof List<?> _list) {
        var visitor = arrayVisitor.visitArray();
        if (visitor != null) {
          visitList(list, visitor);
          visitor.visitEndArray();
        }
        continue;
      }
      throw new IllegalStateException("invalid element " + element);
    }
    return arrayVisitor.visitEndArray();
  }

  public Object accept(JsonArrayVisitor arrayVisitor) {
    requireNonNull(arrayVisitor);
    return visitList(list, arrayVisitor);
  }
}
