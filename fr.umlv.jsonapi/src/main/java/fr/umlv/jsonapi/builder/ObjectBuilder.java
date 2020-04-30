package fr.umlv.jsonapi.builder;

import static fr.umlv.jsonapi.VisitorMode.PUSH;
import static java.util.Objects.requireNonNull;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.JsonReader;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.VisitorMode;
import fr.umlv.jsonapi.filter.PostOpsArrayVisitor;
import fr.umlv.jsonapi.filter.PostOpsObjectVisitor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * An untyped builder of a representation of a JSON object.
 *
 * All primitive values use the usual Java classes (Boolean, Integer, Long, etc),
 * arrays are constructed with an {@link ArrayBuilder} and objects are constructed with a
 * builder of this kind.
 *
 * This class is able to gather all the visited elements on a JSON object and build a
 * java.util.Map. It implements the interface {@link ObjectVisitor}, so can be used
 * by a {@link JsonReader}.
 * <p>
 * Example to create a java.util.Map using an ObjectBuilder
 * <pre>
 * String text = """
 *   { "name": "Franky", "address": {  "street": "3rd", "city": "NY" }  }
 *   """;
 * ObjectBuilder objectBuilder = new ObjectBuilder();
 * Map<String, Object></String,> map = JsonReader.parse(text, objectBuilder);
 * assertEquals(
 *         Map.of("name", "Franky",
 *                "address", Map.of("street", "3rd", "city", "NY")),
 *         map);
 * </pre>
 *
 * As a builder, it can be mutated using the methods {@link #add(String, Object)},
 * or {@link #addAll(Map)}.
 * Moreover there is a special method {@link #with(String, Consumer)} to create nested objets
 *
 * <p>
 * Example to generate two JSON objects using an ObjectBuilder
 * <pre>
 * ObjectBuilder objectBuilder = new BuilderConfig(LinkedHashMap::new, ArrayList::new)
 *     .newObjectBuilder()
 *     .add("name", "Franky")
 *     .with("address", b -> b
 *         .add("street", "3rd")
 *         .add("city", "NY"));
 * JsonPrinter printer = new JsonPrinter();
 * objectBuilder.accept(printer::visitObject);
 * assertEquals("""
 *   { "name": "Franky", "address": { "street": "3rd", "city": "NY" } }\
 *   """, printer.toString());
 * </pre>
 *
 * The example above is a little ridiculous because,
 * calling {@link ObjectBuilder#toString()} also work !
 */
public final class ObjectBuilder implements ObjectVisitor {
  private final Map<String, Object> map;
  final BuilderConfig config;
  private final ObjectVisitor delegate;
  private final Consumer<Map<String, Object>> postOp;

  ObjectBuilder(BuilderConfig config, ObjectVisitor delegate, Consumer<Map<String, Object>> postOp) {
    this.map = requireNonNull(config.mapSupplier.get());
    this.config = config;
    this.delegate = delegate;
    this.postOp = postOp;
  }

  ObjectBuilder(BuilderConfig config, ObjectVisitor delegate) {
    this(config, delegate, __ -> {});
  }

  /**
   * Creates an object builder that uses a {@link java.util.HashMap} and {@link java.util.ArrayList}
   * to store a JSON object and a JSON array respectively.
   *
   * @see BuilderConfig
   */
  public ObjectBuilder() {
    this(BuilderConfig.DEFAULT, null);
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

  /**
   * Add an element composed of a name and any object to the builder.
   *
   * If there is already an element with the same name, this new element
   * will replace the existing one.
   *
   * If the class of {code value} is not one of boolean, int, long, double, String, BigInteger,
   * java.util.List or java.util.Map, the method {@link #accept(Supplier)} will
   * consider it as an {@link JsonValue#fromOpaque(Object) opaque value}.
   *
   * @param name name of the element to add
   * @param value value of the element to add
   * @return itself
   */
  public ObjectBuilder add(String name, Object value) {
    requireNonNull(name);
    map.put(name, value);
    return this;
  }

  /**
   * Add several eleemnts to the builder.
   *
   * @param map add all the elements of the map into the builder.
   * @return itself
   *
   * @see #add(String, Object)
   */
  public ObjectBuilder addAll(Map<String, ?> map) {
    map.forEach(this.map::put);  // implicit nullcheck
    return this;
  }

  /**
   * Create an object by specifying all its elements and add it to the current builder;
   *
   * <p>Example of usage
   * <pre>
   * new ObjectBuilder()
   *   .add("name", "Franky")
   *   .with("address", b -> b
   *       .add("street", "3rd")
   *       .add("city", "NY"));
   * </pre>
   *
   * @param name name of the object element that is added
   * @param consumer a function that will called to create the object that will be added
   * @return itself
   */
  public ObjectBuilder with(String name, Consumer<? super ObjectBuilder> consumer) {
    requireNonNull(name);
    requireNonNull(consumer);
    var builder = new ObjectBuilder(config, null);
    consumer.accept(builder);
    add(name, builder.toMap());
    return this;
  }

  /**
   * Return a map from the underlying map of this builder.
   * Apply the {@code transformMapOp} and return the result so the returned map
   * may be a different map from the underlying map.
   *
   * @return a map (newly created or not)
   *
   * @see BuilderConfig
   */
  public Map<String, Object> toMap() {
    return config.transformMapOp.apply(map);
  }

  @Override
  public VisitorMode mode() {
    return PUSH;
  }

  @Override
  public ObjectVisitor visitMemberObject(String name) {
    requireNonNull(name);
    if (delegate != null) {
      var objectVisitor = delegate.visitMemberObject(name);
      if (objectVisitor == null) {
        return null;
      }
      return new PostOpsObjectVisitor<>(objectVisitor, _map -> map.put(name, _map));
    }
    return new ObjectBuilder(config, null, _map -> map.put(name, _map));
  }

  @Override
  public ArrayVisitor visitMemberArray(String name) {
    requireNonNull(name);
    if (delegate != null) {
      var arrayVisitor = delegate.visitMemberArray(name);
      if (arrayVisitor == null) {
        return null;
      }
      return new PostOpsArrayVisitor<>(arrayVisitor, list -> map.put(name, list));
    }
    return new ArrayBuilder(config, null, list -> map.put(name, list));
  }

  @Override
  public Void visitMemberValue(String name, JsonValue value) {
    requireNonNull(name);
    if (delegate != null) {
      var object = delegate.visitMemberValue(name, value);
      map.put(name, object);
      return null;
    }
    var object = value.asObject();
    map.put(name, object);
    return null;
  }

  @Override
  public Map<String, Object> visitEndObject() {
    if (delegate != null) {
      delegate.visitEndObject();
    }
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
      if (element instanceof Map<?,?> _map) {
        var visitor = objectVisitor.visitMemberObject(name);
        if (visitor != null) {
          visitMap(_map, visitor);
        }
        continue;
      }
      if (element instanceof List<?> list) {
        var visitor = objectVisitor.visitMemberArray(name);
        if (visitor != null) {
          ArrayBuilder.visitList(list, visitor);
        }
        continue;
      }
      // unknown value, send it as opaque value
      objectVisitor.visitMemberValue(name, JsonValue.fromOpaque(element));
    }
    return objectVisitor.visitEndObject();
  }

  /**
   * Replay all the elements as visits to the BuilderVisitor specified
   * as argument.
   *
   * If this builder contains values that are not primitive for JSON,
   * those values will be visited as {@link JsonValue#fromOpaque(Object) opaque value}.
   *
   * @param supplier a supplier of the object visitor that will receive
   *                 all the visits
   * @return the value returned by the call to {@link ObjectVisitor#visitEndObject()} on the
   *         array visitor provided as argument
   */
  public Object accept(Supplier<? extends ObjectVisitor> supplier) {
    requireNonNull(supplier);
    return visitMap(map, requireNonNull(supplier.get()));
  }
}
