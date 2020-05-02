package fr.umlv.jsonapi.builder;

import static fr.umlv.jsonapi.VisitorMode.PUSH;
import static java.util.Objects.requireNonNull;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.JsonPrinter;
import fr.umlv.jsonapi.JsonReader;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.VisitorMode;
import fr.umlv.jsonapi.internal.PostOpsArrayVisitor;
import fr.umlv.jsonapi.internal.PostOpsObjectVisitor;
import java.util.Map;
import java.util.function.Consumer;
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
 * Map&lt;String, Object&gt; map = JsonReader.parse(text, objectBuilder);
 * </pre>
 *
 * As a builder, it can be mutated using the methods {@link #add(String, Object)},
 * {@link #addAll(Map)}, {@link #withObject(String, Consumer)} or
 * {@link #withArray(String, Consumer)}.
 *
 * <p>
 * Example to copy the content of a JSON object to another one by replaying the sequence of visits
 * <pre>
 * ObjectBuilder builder = new ObjectBuilder()
 *     .add("name", "Franky")
 *     .withObject("address", b -> b
 *         .add("street", "3rd")
 *         .add("city", "NY"));
 * ObjectBuilder builder2 = new ObjectBuilder();
 * builder.replay(builder2);
 * </pre>
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

  ObjectBuilder(BuilderConfig config, Map<String, Object> map) {
    this.map = map;
    this.config = config;
    this.delegate = null;
    this.postOp = __ -> {};
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
    this(BuilderConfig.DEFAULT, (ObjectVisitor) null);
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
    return replay(new JsonPrinter()).toString();
  }

  /**
   * Add a member composed of a name and any object to the builder.
   *
   * If there is already a member with the same name, this new member
   * will replace the existing one.
   *
   * If the class of {code value} is not one of boolean, int, long, double, String, BigInteger,
   * java.util.List or java.util.Map, the method {@link #replay(ObjectVisitor)} will
   * consider it as an {@link JsonValue#fromOpaque(Object) opaque value}.
   *
   * @param name name of the member to add
   * @param value value of the member to add
   * @return itself
   */
  public ObjectBuilder add(String name, Object value) {
    requireNonNull(name);
    map.put(name, value);
    return this;
  }

  /**
   * Add several members to the builder.
   *
   * @param map add all the members of the map into the builder.
   * @return itself
   *
   * @see #add(String, Object)
   */
  public ObjectBuilder addAll(Map<String, ?> map) {
    map.forEach(this.map::put);  // implicit null check
    return this;
  }

  /**
   * Create an object by specifying all its members and add it to the current builder;
   *
   * <p>Example of usage
   * <pre>
   * new ObjectBuilder()
   *   .add("name", "Franky")
   *   .withObject("address", b -> b
   *       .add("street", "3rd")
   *       .add("city", "NY"));
   * </pre>
   *
   * @param name name of the object member that is added
   * @param consumer a function that will called to create the object that will be added
   * @return itself
   */
  public ObjectBuilder withObject(String name, Consumer<? super ObjectBuilder> consumer) {
    requireNonNull(name);
    requireNonNull(consumer);
    var builder = new ObjectBuilder(config, (ObjectVisitor) null);
    consumer.accept(builder);
    add(name, builder.toMap());
    return this;
  }

  /**
   * Create an array by specifying all its values and add it to the current builder;
   *
   * <p>Example of usage
   * <pre>
   * new ObjectBuilder()
   *   .add("name", "Franky")
   *   .withArray("address", b -> b
   *       .addAll(3, 6, 8, 6, 4));
   * </pre>
   *
   * @param name name of the object member that is added
   * @param consumer a function that will called to create the object that will be added
   * @return itself
   */
  public ObjectBuilder withArray(String name, Consumer<? super ArrayBuilder> consumer) {
    requireNonNull(name);
    requireNonNull(consumer);
    var builder = new ArrayBuilder(config, (ArrayVisitor) null);
    consumer.accept(builder);
    add(name, builder.toList());
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
  public VisitorMode visitStartObject() {
    if (delegate != null) {
      delegate.visitStartObject();
    }
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

  /**
   * Replay all the elements as visits to the BuilderVisitor specified
   * as argument.
   *
   * If this builder contains values that are not primitive for JSON,
   * those values will be visited as {@link JsonValue#fromOpaque(Object) opaque value}.
   *
   * @param objectVisitor the object visitor that will receive all the visits
   * @return the value returned by the call to {@link ObjectVisitor#visitEndObject()} on the
   *         array visitor provided as argument
   */
  public Object replay(ObjectVisitor objectVisitor) {
    requireNonNull(objectVisitor);
    return Acceptors.acceptMap(map, objectVisitor);
  }
}
