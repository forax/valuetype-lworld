package fr.umlv.jsonapi;

import static java.util.Objects.requireNonNull;

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

public final class JsonObjectBuilder implements JsonObjectVisitor {
  private final Map<String, Object> map;
  private final Factory factory;
  private final Consumer<Map<String, Object>> postOp;


  record Factory(Supplier<? extends Map<String, Object>> mapSupplier,
                 UnaryOperator<Map<String, Object>> transformMapOp,
                 Supplier<? extends List<Object>> listSupplier,
                 UnaryOperator<List<Object>> transformListOp) {
    public Factory {
      requireNonNull(mapSupplier, "mapSupplier");
      requireNonNull(mapSupplier, "transformMapOp");
      requireNonNull(mapSupplier, "listSupplier");
      requireNonNull(mapSupplier, "transformListOp");
    }
  }

  JsonObjectBuilder(Factory factory, Consumer<Map<String, Object>> postOp) {
    this.map = requireNonNull(factory.mapSupplier.get());
    this.factory = factory;
    this.postOp = postOp;
  }

  public JsonObjectBuilder(Supplier<? extends Map<String, Object>> mapSupplier,
      UnaryOperator<Map<String, Object>> transformMapOp,
      Supplier<? extends List<Object>> listSupplier,
      UnaryOperator<List<Object>> transformListOp) {
    this(new Factory(mapSupplier, transformMapOp, listSupplier, transformListOp), __ -> {});
  }
  public JsonObjectBuilder(Supplier<? extends Map<String, Object>> mapSupplier, Supplier<? extends List<Object>> listSupplier) {
    this(mapSupplier, UnaryOperator.identity(), listSupplier, UnaryOperator.identity());
  }
  public JsonObjectBuilder() {
    this(HashMap::new, ArrayList::new);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof JsonObjectBuilder builder && map.equals(builder.map);
  }
  @Override
  public int hashCode() {
    return map.hashCode();
  }

  @Override
  public String toString() {
    return map.entrySet().stream().map(e -> '"' + e.getKey() + "\": " + e.getValue()).collect(Collectors.joining(", ", "{", "}"));
  }

  public JsonObjectBuilder add(String name, Object value) {
    requireNonNull(name);
    map.put(name, value);
    return this;
  }

  public JsonObjectBuilder addAll(Map<String, ?> map) {
    map.forEach(this.map::put);  // implicit nullcheck
    return this;
  }

  public Map<String, Object> toMap() {
    var resultMap = factory.transformMapOp.apply(map);
    postOp.accept(resultMap);
    return resultMap;
  }

  @Override
  public JsonObjectBuilder visitMemberObject(String name) {
    requireNonNull(name);
    return new JsonObjectBuilder(factory, _map -> map.put(name, _map));
  }

  @Override
  public JsonArrayBuilder visitMemberArray(String name) {
    requireNonNull(name);
    return new JsonArrayBuilder(factory, list -> map.put(name, list));
  }

  @Override
  public void visitMemberValue(String name, JsonValue value) {
    requireNonNull(name);
    map.put(name, value.asObject());
  }

  @Override
  public Map<String, Object> visitEndObject() {
    return toMap();
  }

  static Object visitMap(Map<?,?> map, JsonObjectVisitor objectVisitor) {
    for(var entry: map.entrySet()) {
      var name = (String) entry.getKey();
      var element = entry.getValue();

      if (element == null) {
        objectVisitor.visitMemberValue(name, JsonValue.nullValue());
        continue;
      }
      if (element instanceof Boolean value) {
        objectVisitor.visitMemberValue(name, JsonValue.from(value));
        continue;
      }
      if (element instanceof Integer value) {
        objectVisitor.visitMemberValue(name, JsonValue.from(value));
        continue;
      }
      if (element instanceof Long value) {
        objectVisitor.visitMemberValue(name, JsonValue.from(value));
        continue;
      }
      if (element instanceof Double value) {
        objectVisitor.visitMemberValue(name, JsonValue.from(value));
        continue;
      }
      if (element instanceof String value) {
        objectVisitor.visitMemberValue(name, JsonValue.from(value));
        continue;
      }
      if (element instanceof BigInteger value) {
        objectVisitor.visitMemberValue(name, JsonValue.from(value));
        continue;
      }
      if (element instanceof BigDecimal value) {
        objectVisitor.visitMemberValue(name, JsonValue.from(value));
        continue;
      }
      if (element instanceof Map<?,?> _map) {
        var visitor = objectVisitor.visitMemberObject(name);
        if (visitor != null) {
          visitMap(_map, visitor);
          visitor.visitEndObject();
        }
        continue;
      }
      if (element instanceof List<?> list) {
        var visitor = objectVisitor.visitMemberArray(name);
        if (visitor != null) {
          JsonArrayBuilder.visitList(list, visitor);
          visitor.visitEndArray();
        }
        continue;
      }
      throw new IllegalStateException("invalid element " + element + " for name " + name);
    }
    return objectVisitor.visitEndObject();
  }

  public Object accept(JsonObjectVisitor objectVisitor) {
    requireNonNull(objectVisitor);
     return visitMap(map, objectVisitor);
  }
}
