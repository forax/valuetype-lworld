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
import java.util.List;
import java.util.function.Consumer;

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
 * </pre>
 *
 * As a builder, it can be mutated using the methods {@link #add(Object)},
 * {@link #addAll(Object...)} or/and {@link #addAll(List)}.
 *
 * <p>
 * Example to generate a JSON from an ArrayBuilder, the method {@link #replay(ArrayVisitor)},
 * replay the sequence of visits from the builder values
 * <pre>
 * ArrayBuilder arrayBuilder = new ArrayBuilder()
 *     .addAll("Jolene", "Joleene", "Joleeeene");
 * JsonPrinter printer = new JsonPrinter();
 * String text = arrayBuilder.replay(printer).toString();
 * </pre>
 */
public final class ArrayBuilder implements ArrayVisitor {
  private final List<Object> list;
  final BuilderConfig config;
  private final ArrayVisitor delegate;
  private final Consumer<List<Object>> postOp;

  ArrayBuilder(BuilderConfig config, ArrayVisitor delegate, Consumer<List<Object>> postOp) {
    this.list = requireNonNull(config.listSupplier.get());
    this.delegate = delegate;
    this.config = config;
    this.postOp = postOp;
  }

  ArrayBuilder(BuilderConfig config, List<Object> list) {
    this.list = list;
    this.config = config;
    this.delegate = null;
    this.postOp = __ -> {};
  }

  ArrayBuilder(BuilderConfig config, ArrayVisitor delegate) {
    this(config, delegate, __ -> {});
  }

  /**
   * Creates an array builder that uses a {@link java.util.HashMap} and {@link java.util.ArrayList}
   * to store a JSON object and a JSON array respectively.
   *
   * @see BuilderConfig
   */
  public ArrayBuilder() {
    this(BuilderConfig.DEFAULT, (ArrayVisitor) null);
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
    return replay(new JsonPrinter()).toString();
  }

  /**
   * Add any object to the builder.
   *
   * If the class of {code value} is not one of boolean, int, long, double, String, BigInteger,
   * java.util.List or java.util.Map, the method {@link #replay(ArrayVisitor)} will
   * consider it as an {@link JsonValue#fromOpaque(Object) opaque value}.
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
   * Add several values to the builder.
   *
   * @param list add all the values of the list into the builder.
   * @return itself
   *
   * @see #add(Object)
   */
  public ArrayBuilder addAll(List<?> list) {
    list.forEach(this.list::add);  // implicit null check
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
    for(var value: values) {   // implicit null check
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
   * @see BuilderConfig
   */
  public List<Object> toList() {
    return config.transformListOp.apply(list);
  }

  @Override
  public VisitorMode visitStartArray() {
    if (delegate != null) {
      delegate.visitStartArray();
    }
    return PUSH;
  }

  @Override
  public ObjectVisitor visitObject() {
    if (delegate != null) {
      var objectVisitor = delegate.visitObject();
      if (objectVisitor == null) {
        return null;
      }
      return new PostOpsObjectVisitor<>(objectVisitor, list::add);
    }
    return new ObjectBuilder(config, null, list::add);
  }

  @Override
  public ArrayVisitor visitArray() {
    if (delegate != null) {
      var arrayVisitor = delegate.visitArray();
      if (arrayVisitor == null) {
        return null;
      }
      return new PostOpsArrayVisitor<>(arrayVisitor, list::add);
    }
    return new ArrayBuilder(config, null, list::add);
  }

  @Override
  public Object visitValue(JsonValue value) {
    if (delegate != null) {
      var object = delegate.visitValue(value);  // pull mode
      list.add(object);
      return object;
    }
    var object = value.asObject();
    list.add(object);
    return object;
  }

  @Override
  public List<Object> visitEndArray() {
    if (delegate != null) {
      delegate.visitArray();  // ignore the resulting value (pull mode)
    }
    var resultList = toList();
    postOp.accept(resultList);
    return resultList;
  }

  /**
   * Replay all the values as visits to the ArrayVisitor specified
   * as argument.
   *
   * If this builder contains values that are not primitive for JSON,
   * those values will be visited as {@link JsonValue#fromOpaque(Object) opaque value}.
   *
   * @param arrayVisitor the array visitor that will receive all the visits
   * @return the value returned by the call to {@link ArrayVisitor#visitEndArray()}
   *         on the array visitor provided as argument
   *         
   * @see #add(Object)
   */
  public Object replay(ArrayVisitor arrayVisitor) {
    requireNonNull(arrayVisitor);
    return Acceptors.acceptIterable(list, arrayVisitor);
  }
}
