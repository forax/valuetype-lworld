package fr.umlv.jsonapi;

import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public final class ObjectBuilder implements ObjectVisitor {
  private final Map<String, Object> map;
  final BuilderConfig config;
  private final Consumer<Map<String, Object>> postOp;

  ObjectBuilder(BuilderConfig config, Consumer<Map<String, Object>> postOp) {
    this.map = requireNonNull(config.mapSupplier().get());
    this.config = config;
    this.postOp = postOp;
  }

  ObjectBuilder(BuilderConfig config) {
    this(config, __ -> {});
  }

  public ObjectBuilder(Supplier<? extends Map<String, Object>> mapSupplier,
      UnaryOperator<Map<String, Object>> transformMapOp,
      Supplier<? extends List<Object>> listSupplier,
      UnaryOperator<List<Object>> transformListOp) {
    this(new BuilderConfig(mapSupplier, transformMapOp, listSupplier, transformListOp));
  }
  public ObjectBuilder(Supplier<? extends Map<String, Object>> mapSupplier, Supplier<? extends List<Object>> listSupplier) {
    this(new BuilderConfig(mapSupplier, listSupplier));
  }
  public ObjectBuilder() {
    this(BuilderConfig.DEFAULT);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof ObjectBuilder builder && map.equals(builder.map);
  }
  @Override
  public int hashCode() {
    return map.hashCode();
  }

  @Override
  public String toString() {
    return map.entrySet().stream().map(e -> '"' + e.getKey() + "\": " + e.getValue()).collect(Collectors.joining(", ", "{", "}"));
  }

  public ObjectBuilder add(String name, Object value) {
    requireNonNull(name);
    map.put(name, value);
    return this;
  }

  public ObjectBuilder addAll(Map<String, ?> map) {
    map.forEach(this.map::put);  // implicit nullcheck
    return this;
  }

  public ObjectBuilder with(String name, Consumer<? super ObjectBuilder> consumer) {
    requireNonNull(name);
    requireNonNull(consumer);
    var builder = new ObjectBuilder();
    consumer.accept(builder);
    add(name, builder.toMap());
    return this;
  }

  public Map<String, Object> toMap() {
    return config.transformMapOp().apply(map);
  }

  @Override
  public ObjectBuilder visitMemberObject(String name) {
    requireNonNull(name);
    return new ObjectBuilder(config, _map -> map.put(name, _map));
  }

  @Override
  public ArrayBuilder visitMemberArray(String name) {
    requireNonNull(name);
    return new ArrayBuilder(config, list -> map.put(name, list));
  }

  @Override
  public void visitMemberValue(String name, JsonValue value) {
    requireNonNull(name);
    map.put(name, value.asObject());
  }

  @Override
  public Map<String, Object> visitEndObject() {
    var resultMap = toMap();
    postOp.accept(resultMap);
    return resultMap;
  }

  static Object visitMap(Map<?,?> map, ObjectVisitor objectVisitor) {
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
          ArrayBuilder.visitList(list, visitor);
          visitor.visitEndArray(null);
        }
        continue;
      }
      throw new IllegalStateException("invalid element " + element + " for name " + name);
    }
    return objectVisitor.visitEndObject();
  }

  public Object accept(ObjectVisitor objectVisitor) {
    requireNonNull(objectVisitor);
    return visitMap(map, objectVisitor);
  }
}
