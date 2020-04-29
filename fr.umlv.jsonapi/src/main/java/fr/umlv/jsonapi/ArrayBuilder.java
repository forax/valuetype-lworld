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

/**
 * An untyped builder of a representation of a JSON array.
 *
 * All primitive values use the usual Java classes (Boolean, Integer, Long, etc),
 * arrays are constructed with this kind of builder and objects are constructed with a
 * {@link ObjectBuilder}.
 *
 * This class is able to gather all the visited values on a JSON array and build a java.util.List.
 * It implements the interface {@link ArrayVisitor}, so can be used by a {@link JsonReader}.
 * <p>
 * Example to create a list of strings using an ArrayBuilder
 * <pre>
 * String text = """
 *   [ "Jolene", "Joleene", "Joleeeene" ]
 *   """;
 * ArrayBuilder arrayBuilder = new ArrayBuilder();
 * List&lt;Object&gt; list = JsonReader.parse(text, arrayBuilder);
 * assertEquals(List.of("Jolene", "Joleene", "Joleeeene"), list);
 * </pre>
 *
 * As a builder, it can be mutated using the methods {@link #add(Object)},
 * {@link #addAll(Object...)} or/and {@link #addAll(List)}.
 *
 * <p>
 * Example to generate a JSON from an ArrayBuilder
 * <pre>
 * ArrayBuilder arrayBuilder = new ArrayBuilder()
 *     .add("Jolene")
 *     .addAll("Joleene", "Joleeeene");
 * JsonPrinter printer = new JsonPrinter();
 * arrayBuilder.accept(printer::visitArray);
 * assertEquals("""
 *   [ "Jolene", "Joleene", "Joleeeene" ]\
 *   """, printer.toString());
 * </pre>
 *
 * The example above is a little ridiculous because,
 * calling {@link ArrayBuilder#toString()} also work !
 */
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

  /**
   * Creates an array builder that takes as arguments the implementations of Map and List
   * and some post transformations.
   * The post transformations are executed once all the elements/values have been seen.
   *
   * By example,
   * <pre>
   *   new ArrayBuilder(HashMap::new, Map::copyOf, ArrayList::new, List::copyOf)
   * </pre>
   * creates a builder that will create an immutable Map for any JSON objects
   * and an immutable List for any JSON arrays.
   *
   * @see BuilderConfig#newArrayBuilder()
   */
  public ArrayBuilder(Supplier<? extends Map<String, Object>> mapSupplier,
      UnaryOperator<Map<String, Object>> transformMapOp,
      Supplier<? extends List<Object>> listSupplier,
      UnaryOperator<List<Object>> transformListOp) {
    this(new BuilderConfig(mapSupplier, transformMapOp, listSupplier, transformListOp));
  }

  /**
   * Creates an array builder that takes as arguments the implementations of Map and List
   * that should be used.
   *
   * By example, to keep the insertion order, one can use a {@link java.util.LinkedHashMap}
   * as {@link Map} implementation.
   * <pre>
   *    new ArrayBuilder(LinkedHashMap::new, ArrayList::new)
   * </pre>
   *
   * @see BuilderConfig#newArrayBuilder()
   */
  public ArrayBuilder(Supplier<? extends Map<String, Object>> mapSupplier, Supplier<? extends List<Object>> listSupplier) {
    this(new BuilderConfig(mapSupplier, listSupplier));
  }

  /**
   * Creates an array builder that uses a {@link java.util.HashMap} and {@link java.util.ArrayList}
   * to store a JSON object and a JSON array respectively.
   *
   * @see BuilderConfig#newArrayBuilder()
   */
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

  /**
   * Add any object to the builder.
   *
   * If the class of {code value} is not one of boolean, int, long, double, String, BigInteger,
   * BigDecimal, java.util.List or java.util.Map, the method {@link #accept(Supplier)} will fail
   * to work because there is no JSON mapping.
   *
   * @param value add this value to the builder.
   * @return itself
   */
  public ArrayBuilder add(Object value) {
    requireNonNull(value);
    list.add(value);
    return this;
  }

  /**
   * Add several objects to the builder.
   *
   * @param list add all the values of the list into the builder.
   * @return itself
   *
   * @see #add(Object)
   */
  public ArrayBuilder addAll(List<?> list) {
    list.forEach(this.list::add);  // implicit nullcheck
    return this;
  }
  /**
   * Add several objects to the builder.
   *
   * @param values add all the values into the builder.
   * @return itself
   *
   * @see #add(Object)
   */
  public ArrayBuilder addAll(Object... values) {
    for(var value: values) {   // implicit nullcheck
      list.add(value);
    }
    return this;
  }

  /**
   * Return a list from the underlying list of this builder.
   * Apply the {@code transformListOp} and return the result so the returned list
   * may be a different list from the underlying list.
   *
   * @return a list (newly created or not)
   *
   * @see #ArrayBuilder(Supplier, UnaryOperator, Supplier, UnaryOperator)
   */
  public List<Object> toList() {
    return config.transformListOp().apply(list);
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
  public List<Object> visitEndArray() {
    var resultList = toList();
    postOp.accept(resultList);
    return resultList;
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
        }
        continue;
      }
      if (element instanceof List<?> _list) {
        var visitor = arrayVisitor.visitArray();
        if (visitor != null) {
          visitList(_list, visitor);
        }
        continue;
      }
      throw new IllegalStateException("invalid element " + element);
    }
    return arrayVisitor.visitEndArray();
  }

  public Object accept(Supplier<? extends ArrayVisitor> supplier) {
    requireNonNull(supplier);
    return visitList(list, requireNonNull(supplier.get()));
  }
}
