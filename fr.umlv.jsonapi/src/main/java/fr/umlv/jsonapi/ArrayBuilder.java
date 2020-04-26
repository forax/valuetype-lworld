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

public final class ArrayBuilder implements ArrayVisitor {
  private final List<Object> list;
  final BuilderConfig config;
  private final Consumer<List<Object>> postOp;

  ArrayBuilder(BuilderConfig config, Consumer<List<Object>> postOp) {
    this.list = requireNonNull(config.listSupplier().get());
    this.config = config;
    this.postOp = postOp;
  }

  ArrayBuilder(BuilderConfig config) {
    this(config, __ -> {});
  }

  public ArrayBuilder(Supplier<? extends Map<String, Object>> mapSupplier,
      UnaryOperator<Map<String, Object>> transformMapOp,
      Supplier<? extends List<Object>> listSupplier,
      UnaryOperator<List<Object>> transformListOp) {
    this(new BuilderConfig(mapSupplier, transformMapOp, listSupplier, transformListOp));
  }
  public ArrayBuilder(Supplier<? extends Map<String, Object>> mapSupplier, Supplier<? extends List<Object>> listSupplier) {
    this(new BuilderConfig(mapSupplier, listSupplier));
  }
  public ArrayBuilder() {
    this(BuilderConfig.DEFAULT);
  }


  @Override
  public boolean equals(Object o) {
    return o instanceof ArrayBuilder builder && list.equals(builder.list);
  }
  @Override
  public int hashCode() {
    return list.hashCode();
  }

  @Override
  public String toString() {
    return list.stream().map(String::valueOf).collect(Collectors.joining(", ", "[", "]"));
  }

  public ArrayBuilder add(Object value) {
    requireNonNull(value);
    list.add(value);
    return this;
  }

  public ArrayBuilder addAll(List<?> list) {
    list.forEach(this.list::add);  // implicit nullcheck
    return this;
  }
  public ArrayBuilder addAll(Object... values) {
    for(var value: values) {   // implicit nullcheck
      list.add(value);
    }
    return this;
  }

  public List<Object> toList() {
    var resultList = config.transformListOp().apply(list);
    postOp.accept(resultList);
    return resultList;
  }

  @Override
  public ObjectBuilder visitObject() {
    return new ObjectBuilder(config, list::add);
  }

  @Override
  public ArrayBuilder visitArray() {
    return new ArrayBuilder(config, list::add);
  }

  @Override
  public Void visitValue(JsonValue value) {
    list.add(value.asObject());
    return null;
  }

  @Override
  public List<Object> visitEndArray(Object unused) {
    return toList();
  }

  static Object visitList(List<?> list, ArrayVisitor arrayVisitor) {
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
          ObjectBuilder.visitMap(map, visitor);
          visitor.visitEndObject();
        }
        continue;
      }
      if (element instanceof List<?> _list) {
        var visitor = arrayVisitor.visitArray();
        if (visitor != null) {
          visitList(_list, visitor);
          visitor.visitEndArray(null);
        }
        continue;
      }
      throw new IllegalStateException("invalid element " + element);
    }
    return arrayVisitor.visitEndArray(null);
  }

  public Object accept(ArrayVisitor arrayVisitor) {
    requireNonNull(arrayVisitor);
    return visitList(list, arrayVisitor);
  }
}
