package fr.umlv.jsonapi;

import static java.util.Objects.requireNonNull;
import static java.util.function.UnaryOperator.identity;

import fr.umlv.jsonapi.bind.Spec;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Factory functions and transformer functions used as parameter of
 * {@link fr.umlv.jsonapi.bind.Binder#read(Path, Spec, BuilderConfig)} and to construct
 * a {@link ObjectBuilder} or an {@link ArrayBuilder}.
 *
 * Because JSON format is recursive, by example an array may contain objects and
 * an object may contain array, you have to configure the representation
 * used for an object and the representation used for an array both at the same time.
 *
 * <p>The default configuration use {@link HashMap} for representing JSON objects and
 * {@link ArrayList} for representing JSON arrays.
 * for
 * <pre>
 *   BuilderConfig config =  BuilderConfig.defaults();
 *   ObjectBuilder builder = config.newObjectBuilder();
 * </pre>
 *
 * You can configure the exact {@link Map} and {@link List} implementations to use
 * <pre>
 *   BuilderConfig config = new BuilderConfig(LinkedHashMap::new, ArrayList::new);
 *   ArrayBuilder builder = config.newArrayBuilder();
 * </pre>
 *
 * You can specify a transformation operation for each classes that will be done
 * once a builder has fully parse the object/array
 * <pre>
 *   BuilderConfig config = new BuilderConfig(LinkedHashMap::new, ArrayList::new);
 *     .withTransformOps(Collections::unmodifiableMap, Collections::unmodifiableList);
 * </pre>
 */
public class BuilderConfig {
  final Supplier<? extends Map<String, Object>> mapSupplier;
  final UnaryOperator<Map<String, Object>> transformMapOp;
  final Supplier<? extends List<Object>> listSupplier;
  final UnaryOperator<List<Object>> transformListOp;

  static final BuilderConfig DEFAULT = new BuilderConfig();

  private BuilderConfig(Supplier<? extends Map<String, Object>> mapSupplier,
      UnaryOperator<Map<String, Object>> transformMapOp,
      Supplier<? extends List<Object>> listSupplier,
      UnaryOperator<List<Object>> transformListOp) {
    this.mapSupplier = requireNonNull(mapSupplier, "mapSupplier");
    this.transformMapOp = requireNonNull(transformMapOp, "transformMapOp");
    this.listSupplier = requireNonNull(listSupplier, "listSupplier");
    this.transformListOp = requireNonNull(transformListOp, "transformListOp");
  }

  /**
   * Creates a builder config from a factory of {@link Map} and a factory of {@link List}.
   *
   * @param mapSupplier a supplier of instances of Map.
   * @param listSupplier a supplier of instances of List.
   */
  public BuilderConfig(Supplier<? extends Map<String, Object>> mapSupplier,
                       Supplier<? extends List<Object>> listSupplier) {
    this(mapSupplier, identity(), listSupplier, identity());
  }

  private BuilderConfig() {
    this(HashMap::new, ArrayList::new);
  }

  /**
   * Returns a builder configuration that use a factory of {@link HashMap} and a factory
   * of {@link ArrayList} to be used by a {@link ObjectBuilder} or an {@link ArrayBuilder}
   * @return the default builder configuration
   */
  public static BuilderConfig defaults() {
    return DEFAULT;
  }

  /**
   * Replace the current transformations operations applied on {@link Map}s and {@link List}s
   * once the object/array is fully initialized by new ones.
   *
   * @param transformMapOp the new transformation to apply for the {@link Map}s.
   * @param transformListOp the new transformation to apply for the {@link List}s.
   * @return a new builder configuration.
   *
   * @see #withTransformMapOp(UnaryOperator)
   * @see #withTransformListOp(UnaryOperator)
   */
  public BuilderConfig withTransformOps(
      UnaryOperator<Map<String, Object>> transformMapOp,
      UnaryOperator<List<Object>> transformListOp) {
    return new BuilderConfig(mapSupplier, transformMapOp, listSupplier, transformListOp);
  }

  /**
   * Replace the current transformation operation applied on {@link Map}s
   * once the object is fully initialized by a new one.
   *
   * @param transformMapOp the new transformation to apply to the {@link Map}s.
   * @return a new builder configuration.
   *
   * @see #withTransformOps(UnaryOperator, UnaryOperator)
   */
  public BuilderConfig withTransformMapOp(UnaryOperator<Map<String, Object>> transformMapOp) {
    return new BuilderConfig(mapSupplier, transformMapOp, listSupplier, transformListOp);
  }

  /**
   * Replace the current transformation operation applied on {@link List}s
   * once the array is fully initialized by a new one.
   *
   * @param transformListOp the new transformation to apply to the {@link List}s.
   * @return a new builder configuration.
   *
   * @see #withTransformOps(UnaryOperator, UnaryOperator)
   */
  public BuilderConfig withTransformListOp(UnaryOperator<List<Object>> transformListOp) {
    return new BuilderConfig(mapSupplier, transformMapOp, listSupplier, transformListOp);
  }

  /**
   * Returns a new object builder initialized with the current configuration.
   * @return a new object builder initialized with the current configuration.
   */
  public ObjectBuilder newObjectBuilder() {
    return new ObjectBuilder(this, null);
  }

  /**
   * Return a new object builder initialized with the current configuration
   * and with a {code delegate} that will be used to react to the visit methods
   * called on the object builder.
   *
   * The values returned by a call to {@link ObjectVisitor#visitMemberObject(String)},
   * {@link ObjectVisitor#visitMemberArray(String)} and
   * {@link ObjectVisitor#visitMemberValue(String, JsonValue)} are stored in the {@link Map}
   * stored in the created {@link ObjectBuilder}.
   *
   * The {code delegate} has to be a visitor in {@link VisitorMode#PULL} mode.
   *
   * @param delegate an object builder that will be used to react to the visit methods or null.
   * @return a new object builder that delegate its operation to an object visitor.
   */
  public ObjectBuilder newObjectBuilder(ObjectVisitor delegate) {
    if (delegate != null && delegate.mode() != VisitorMode.PULL) {
      throw new IllegalArgumentException("only pull mode visitors are allowed");
    }
    return new ObjectBuilder(this, delegate);
  }

  /**
   * Returns a new array builder initialized with the current configuration.
   * @return a new array builder initialized with the current configuration.
   */
  public ArrayBuilder newArrayBuilder() {
    return new ArrayBuilder(this, null);
  }


  /**
   * Return a new array builder initialized with the current configuration
   * and with a {code delegate} that will be used to react to the visit methods
   * called on the array builder.
   *
   * The values returned by a call to {@link ArrayVisitor#visitObject()},
   * {@link ArrayVisitor#visitArray()} and
   * {@link ArrayVisitor#visitValue(JsonValue)} are stored in the {@link Map}
   * stored in the created {@link ObjectBuilder}.
   *
   * The {code delegate} has to be a visitor in {@link VisitorMode#PULL} mode.
   *
   * @param delegate an object builder that will be used to react to the visit methods or null.
   * @return a new object builder that delegate its operation to an object visitor.
   */
  public ArrayBuilder newArrayBuilder(ArrayVisitor delegate) {
    if (delegate != null && delegate.mode() != VisitorMode.PULL) {
      throw new IllegalArgumentException("only pull mode visitors are allowed");
    }
    return new ArrayBuilder(this, delegate);
  }

  /**
   * Extract the configuration of an existing builder.
   * @param builder the builder which is configured with a configuration we request
   * @return the configuration of an existing builder.
   */
  public static BuilderConfig extract(ObjectBuilder builder) {
    return builder.config;   // implicit nullcheck
  }

  /**
   * Extract the configuration of an existing builder.
   * @param builder the builder which is configured with a configuration we request
   * @return the configuration of an existing builder.
   */
  public static BuilderConfig extract(ArrayBuilder builder) {
    return builder.config;  // implicit nullcheck
  }
}
