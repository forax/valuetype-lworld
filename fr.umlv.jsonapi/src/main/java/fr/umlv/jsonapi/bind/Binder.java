package fr.umlv.jsonapi.bind;

import static java.util.Objects.requireNonNull;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.JsonReader;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.VisitorMode;
import fr.umlv.jsonapi.builder.BuilderConfig;
import java.io.IOException;
import java.io.Reader;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/**
 * A binder is able to creates a tree of Java objetcs from a JSON text.
 *
 * A binder associate an object {@link Spec} for each JSON value, a {@link Spec} instance
 * knows how to serialize/deserialize a JSON value.
 * Those {@link Spec} instances are created lazily when calling the method {@link #spec(Type)}
 * and stored inside the binder for reuse.
 *
 * The creation of a {@link Spec} instance is delegated to several {@link SpecFinder}s
 * that are {@link #register(SpecFinder) registered} to the binder.
 * When {@link #spec(Type) looking} up for a spec, the binder will start by asking to the most
 * recently registered {@link SpecFinder} to the first registered.
 * So adding a new {@link SpecFinder} allows to override the previous ones.
 *
 * The method {@link #spec(Type)} is idempotent which means that once a Spec has been associated
 * to a type, it can not be changed, even by {@link #register(SpecFinder) registering}
 * a new {@link SpecFinder}.
 *
 * There are two ways to create a binder, to get a binder with the default spec finders
 * already registered use the constructor {@link Binder#Binder(Lookup)}.
 * To get a binder with no spec finders use {@link Binder#noDefaults()}.
 *
 * <p>To transform a JSON text to an object, the class Binder provide several variants of the method
 * {@link #read(Reader, Spec, BuilderConfig)} and {@link #stream(Reader, Spec, BuilderConfig)}.
 *
 * By example, to read a simple record
 * <pre>
 * Binder binder = new Binder(MethodHandles.lookup());
 * String json = """
 *   { "x": 42, "y": 33 }
 *   """;
 * record Point(int x, int y) { }
 * Point point = binder.read(json, Point.class);
 * </pre>
 *
 * <p>to get an array of object from a JSON text
 * <pre>
 *  Binder binder = new Binder(lookup());
 *  String json = """
 *    [ { "x": 42, "y": 33 }, { "x": 4, "y": 17 } ]
 *    """;
 *  record Point(int x, int y) { }
 *  List&lt;Point&gt; points = binder.read(json, Point.class, IN_ARRAY);
 *  </pre>
 *
 * <p>to get a {@link Stream} of object from a JSON text
 * <pre>
 * Binder binder = new Binder(lookup());
 * String json = """
 *   [ { "x": 42, "y": 33 }, { "x": 4, "y": 17 } ]
 *   """;
 * record Point(int x, int y) { }
 * Stream&lt;Point&gt; stream = binder.stream(json, Point.class);
 * </pre>
 *
 * @see Spec
 * @see SpecFinder
 */
public final class Binder {

  /**
   * Thrown when a mapping error occurs between a JSON fragment and the typed information
   * known by the binder.
   */
  public static final class BindingException extends RuntimeException {
    /**
     * Create a binding exception with an error message
     * @param message the error message
     */
    public BindingException(String message) {
      super(message);
    }

    /**
     * Create a binding exception with an error message
     * @param message the error message
     * @param cause the cause of the exception
     */
    public BindingException(String message, Throwable cause) {
      super(message, cause);
    }

    /**
     * Create a binding with an exception as cause
     * @param cause the cause of the exception
     */
    public BindingException(Throwable cause) {
      super(cause);
    }
  }

  private final ClassValue<Spec> specMap = new ClassValue<>() {
    @Override
    protected Spec computeValue(Class<?> type) {
      var spec = lookup(type, Binder.this.finders);
      if (spec != null) {
        return spec;
      }
      throw new BindingException("no finder can resolve type " + type.getName());
    }
  };
  private final CopyOnWriteArrayList<SpecFinder> finders = new CopyOnWriteArrayList<>();

  private Binder() {
    // empty
  }

  /**
   * Creates a binder with default {@link SpecFinder}s pre-{@link #register(SpecFinder) registered}.
   * @param lookup the security context that will be used to load the class necessary when
   *               {@link #read(Reader, Spec, BuilderConfig) reading} a JSON fragment.
   *
   * @see java.lang.invoke.MethodHandles#lookup()
   * @see #noDefaults()
   */
  public Binder(Lookup lookup) {
    requireNonNull(lookup);
    finders.add(newRecordSpecFinder(lookup));
  }

  /**
   * Creates a binder with no default {@link SpecFinder}s
   * @return a fresh new binder.
   *
   * @see #Binder(Lookup)
   */
  public static Binder noDefaults() {
    return new Binder();
  }

  /**
   * Creates a spec finder able to read/write any records.
   * The returned spec finder is not {@link #register(SpecFinder) registered} to the binder.
   *
   * @param lookup the security context that will be used to load the class necessary when
   *               {@link #read(Reader, Spec, BuilderConfig) reading} a JSON fragment.
   * @return a spec finder able to read/write records.
   */
  public SpecFinder newRecordSpecFinder(Lookup lookup) {
    requireNonNull(lookup);
    return SpecFinders.newRecordFinder(lookup, this);
  }

  /**
   * Register a spec finder to the current binder.
   * When {@link #spec(Type) looking up} for a spec, the most registered spec finder
   * will be used first, the fist registered spec finder will be used last.
   *
   * @param finder a spec finder to register
   * @return itself
   */
  public Binder register(SpecFinder finder) {
    requireNonNull(finder);
    finders.add(finder);
    return this;
  }

  private static Spec lookup(Class<?> type, List<SpecFinder> finders) {
    if (type == boolean.class || type == int.class || type == long.class || type == double.class
        || type == String.class || type == BigInteger.class || type == BigDecimal.class
        || type == Object.class) {
      return Spec.valueClass(type.getName(), null);
    }
    for(var i = finders.size(); --i >= 0; ) {
      var optSpec = finders.get(i).findSpec(type);
      if (optSpec.isPresent()) {
        return optSpec.orElseThrow();
      }
    }
    return null;
  }

  /**
   * Return a spec finder that is equivalent to all the spec finders that have been registered.
   * Spec finders registered after this method call wil not be taking into account.
   * @return a spec finder that is equivalent to all the spec finders that have been registered.
   */
  public SpecFinder specFinder() {
    var finders = List.of(this.finders.toArray(SpecFinder[]::new));
    return type -> Optional.ofNullable(lookup(type, finders));
  }

  /**
   * Returns the spec associated to the type by asking all the
   * {@link #register(SpecFinder) registered} spec finders (last registered first) to find
   * the type.
   *
   * This method is idempotent which means that if once it has returned a spec for a type,
   * it will always return the same spec for the same type.
   *
   * @param type a Java type
   * @return the spec associated with the type
   * @throws BindingException if no spec finder claims that type.
   */
  public Spec spec(Type type) throws BindingException {
    requireNonNull(type);
    if (type instanceof Class<?> clazz) {
      return specForClass(clazz);
    }
    if (type instanceof ParameterizedType parameterizedType) {
      var rawType = parameterizedType.getRawType();
      var actualTypeArguments = parameterizedType.getActualTypeArguments();
      if (rawType == Map.class) {
        if (actualTypeArguments[0] != String.class) {  //FIXME wildcard ?
          throw new BindingException("can not decode " + type.getTypeName());
        }
        return spec(actualTypeArguments[1]).object();
      }
      if (rawType == List.class) {
        return spec(actualTypeArguments[0]).array();
      }
    }
    throw new BindingException("can not decode unknown type " + type.getTypeName());
  }
  private Spec specForClass(Class<?> type) {
    return specMap.get(type);
  }

  static final BuilderConfig DEFAULT_CONFIG = BuilderConfig.defaults();

  /**
   * A type only used to type the method {@link Binder#read(Reader, Class, ObjectToken)}
   * @see Binder#IN_OBJECT
   */
  public interface ObjectToken { }

  /**
   * A type only used to type the method {@link Binder#read(Reader, Class, ArrayToken)}
   * @see Binder#IN_ARRAY
   */
  public interface ArrayToken { }

  /**
   * Constant to indicate that the binder should read an object of the type taken as parameter
   * @see Binder#read(Reader, Class, ObjectToken)
   */
  public static final ObjectToken IN_OBJECT = null;

  /**
   * Constant to indicate that the binder should read an array of the type taken as parameter
   * @see Binder#read(Reader, Class, ArrayToken)
   */
  public static final ArrayToken IN_ARRAY = null;

  /**
   * Read a JSON fragment and convert it to an instance of the type in parameter.
   * This method is a convenient method for {@link #read(Reader, Spec, BuilderConfig)}.
   *
   * @param reader the reader containing the JSON fragment
   * @param type the type of the instance that should be returned
   * @param <T> the type of the instance that should be returned
   * @return a new instance containing all the JSON data typed as Java values
   * @throws IOException if an IO error occurs.
   */
  public <T> T read(Reader reader, Class<? extends T> type) throws IOException {
    requireNonNull(reader);
    requireNonNull(type);
    return read(reader, type, DEFAULT_CONFIG);
  }

  /**
   * Read a JSON fragment and convert it to an instance of the type in parameter.
   * This method is a convenient method for {@link #read(Reader, Spec, BuilderConfig)}.
   *
   * @param reader the reader containing the JSON fragment
   * @param type the type of the instance that should be returned
   * @param config the builder configuration used to create untyped JSON object and array
   *               as {@link Map} and {@link List}.
   * @param <T> the type of the instance that should be returned
   * @return a new instance containing all the JSON data typed as Java values
   * @throws IOException if an IO error occurs.
   */
  public <T> T read(Reader reader, Class<? extends T> type, BuilderConfig config) throws IOException {
    requireNonNull(reader);
    requireNonNull(type);
    return type.cast(read(reader, specForClass(type), config));
  }

  /**
   * Read a JSON array and convert it to a list of instances of the type in parameter.
   * This method is a convenient method for {@link #read(Reader, Spec, BuilderConfig)}.
   *
   * @param reader the reader containing the JSON fragment
   * @param type the type of the instance that should be returned
   * @param __ should be {@link #IN_ARRAY}
   * @param <T> the type of the instance that should be returned
   * @return a new instance containing all the JSON data typed as Java values
   * @throws IOException if an IO error occurs.
   *
   * @see #stream(Reader, Class)
   */
  @SuppressWarnings("unchecked")
  public <T> List<T> read(Reader reader, Class<? extends T> type, @SuppressWarnings("unused") ArrayToken __) throws IOException {
    requireNonNull(reader);
    requireNonNull(type);
    return (List<T>) read(reader, specForClass(type).array(), DEFAULT_CONFIG);
  }

  /**
   * Read a JSON object and convert it to a map of instances of the type in parameter.
   * This method is a convenient method for {@link #read(Reader, Spec, BuilderConfig)}.
   *
   * @param reader the reader containing the JSON fragment
   * @param type the type of the instance that should be returned
   * @param __ should be {@link #IN_OBJECT}
   * @param <T> the type of the instance that should be returned
   * @return a new instance containing all the JSON data typed as Java values
   * @throws IOException if an IO error occurs.
   */
  @SuppressWarnings("unchecked")
  public <T> Map<String, T> read(Reader reader, Class<? extends T> type, @SuppressWarnings("unused") ObjectToken __) throws IOException {
    requireNonNull(reader);
    requireNonNull(type);
    return (Map<String, T>) read(reader, specForClass(type).object(), DEFAULT_CONFIG);
  }

  /**
   * Read a JSON fragment and convert it to a Java instance using the spec in parameter.
   *
   * @param reader the reader containing the JSON fragment
   * @param spec the spec, a representation of how to decode a JSON value to a Java instance.
   * @param config the builder configuration used to create untyped JSON object and array
   *               as {@link Map} and {@link List}.
   * @return a new instance containing all the JSON data typed as Java values
   * @throws IOException if an IO error occurs.
   */
  public static Object read(Reader reader, Spec spec, BuilderConfig config) throws IOException {
    requireNonNull(reader);
    requireNonNull(spec);
    requireNonNull(config);
    return JsonReader.parse(reader, spec.createBindVisitor(Object.class, config, null));
  }

  /**
   * Read a JSON text and convert it to an instance of the type in parameter.
   * This method is a convenient method for {@link #read(String, Spec, BuilderConfig)}.
   *
   * @param text the text containing the JSON
   * @param type the type of the instance that should be returned
   * @param <T> the type of the instance that should be returned
   * @return a new instance containing all the JSON data typed as Java values
   */
  public <T> T read(String text, Class<? extends T> type) {
    requireNonNull(text);
    requireNonNull(type);
    return read(text, type, DEFAULT_CONFIG);
  }

  /**
   * Read a JSON text and convert it to an instance of the type in parameter.
   * This method is a convenient method for {@link #read(String, Spec, BuilderConfig)}.
   *
   * @param text the text containing the JSON
   * @param type the type of the instance that should be returned
   * @param config the builder configuration used to create untyped JSON object and array
   *               as {@link Map} and {@link List}.
   * @param <T> the type of the instance that should be returned
   * @return a new instance containing all the JSON data typed as Java values
   */
  public <T> T read(String text, Class<? extends T> type, BuilderConfig config) {
    requireNonNull(text);
    requireNonNull(type);
    return type.cast(read(text, specForClass(type), config));
  }

  /**
   * Read a JSON text and convert it to a list of instances of the type in parameter.
   * This method is a convenient method for {@link #read(String, Spec, BuilderConfig)}.
   *
   * @param text the text containing the JSON
   * @param type the type of the instance that should be returned
   * @param __ should be {@link #IN_ARRAY}
   * @param <T> the type of the instance that should be returned
   * @return a new instance containing all the JSON data typed as Java values
   *
   * @see #stream(String, Class)
   */
  @SuppressWarnings("unchecked")
  public <T> List<T> read(String text, Class<? extends T> type, @SuppressWarnings("unused") ArrayToken __) {
    requireNonNull(text);
    requireNonNull(type);
    return (List<T>) read(text, specForClass(type).array(), DEFAULT_CONFIG);
  }

  /**
   * Read a JSON object and convert it to a map of instances of the type in parameter.
   * This method is a convenient method for {@link #read(Reader, Spec, BuilderConfig)}.
   *
   * @param text the text containing the JSON
   * @param type the type of the instance that should be returned
   * @param __ should be {@link #IN_OBJECT}
   * @param <T> the type of the instance that should be returned
   * @return a new instance containing all the JSON data typed as Java values
   */
  @SuppressWarnings("unchecked")
  public <T> Map<String, T> read(String text, Class<? extends T> type, @SuppressWarnings("unused") ObjectToken __) {
    requireNonNull(text);
    requireNonNull(type);
    return (Map<String, T>) read(text, specForClass(type).object(), DEFAULT_CONFIG);
  }

  /**
   * Read a JSON text and convert it to a Java instance using the spec in parameter.
   *
   * @param text the text containing the JSON
   * @param spec the spec, a representation of how to decode a JSON value to a Java instance.
   * @param config the builder configuration used to create untyped JSON object and array
   *               as {@link Map} and {@link List}.
   * @return a new instance containing all the JSON data typed as Java values
   */
  public static Object read(String text, Spec spec, BuilderConfig config) {
    requireNonNull(text);
    requireNonNull(spec);
    requireNonNull(config);
    return JsonReader.parse(text, spec.createBindVisitor(Object.class, config, null));
  }

  /**
   * Read a JSON file and convert it to an instance of the type in parameter.
   * This method is a convenient method for {@link #read(Path, Spec, BuilderConfig)}.
   *
   * @param path the path to the file containing the JSON
   * @param type the type of the instance that should be returned
   * @param <T> the type of the instance that should be returned
   * @return a new instance containing all the JSON data typed as Java values
   * @throws IOException if an IO error occurs.
   */
  public <T> T read(Path path, Class<? extends T> type) throws IOException {
    requireNonNull(path);
    requireNonNull(type);
    return read(path, type, DEFAULT_CONFIG);
  }

  /**
   * Read a JSON file and convert it to an instance of the type in parameter.
   * This method is a convenient method for {@link #read(Path, Spec, BuilderConfig)}.
   *
   * @param path the path to the file containing the JSON
   * @param type the type of the instance that should be returned
   * @param config the builder configuration used to create untyped JSON object and array
   *               as {@link Map} and {@link List}.
   * @param <T> the type of the instance that should be returned
   * @return a new instance containing all the JSON data typed as Java values
   * @throws IOException if an IO error occurs.
   */
  public <T> T read(Path path, Class<? extends T> type, BuilderConfig config) throws IOException {
    requireNonNull(path);
    requireNonNull(type);
    return type.cast(read(path, specForClass(type), config));
  }

  /**
   * Read a JSON file and convert it to list of instances of the type in parameter.
   * This method is a convenient method for {@link #read(Path, Spec, BuilderConfig)}.
   *
   * @param path the path to the file containing the JSON
   * @param type the type of the instance that should be returned
   * @param __ should be {@link #IN_ARRAY}
   * @param <T> the type of the instance that should be returned
   * @return a new instance containing all the JSON data typed as Java values
   * @throws IOException if an IO error occurs.
   *
   * @see #stream(Path, Class)
   */
  @SuppressWarnings("unchecked")
  public <T> List<T> read(Path path, Class<? extends T> type, @SuppressWarnings("unused") ArrayToken __) throws IOException {
    requireNonNull(path);
    requireNonNull(type);
    return (List<T>) read(path, specForClass(type).array(), DEFAULT_CONFIG);
  }

  /**
   * Read a JSON object and convert it to a map of instances of the type in parameter.
   * This method is a convenient method for {@link #read(Reader, Spec, BuilderConfig)}.
   *
   * @param path the path to the file containing the JSON
   * @param type the type of the instance that should be returned
   * @param __ should be {@link #IN_OBJECT}
   * @param <T> the type of the instance that should be returned
   * @return a new instance containing all the JSON data typed as Java values
   * @throws IOException if an IO error occurs.
   */
  @SuppressWarnings("unchecked")
  public <T> Map<String, T> read(Path path, Class<? extends T> type, @SuppressWarnings("unused") ObjectToken __) throws IOException {
    requireNonNull(path);
    requireNonNull(type);
    return (Map<String, T>) read(path, specForClass(type).object(), DEFAULT_CONFIG);
  }

  /**
   * Read a JSON file and convert it to a Java instance using the spec in parameter.
   *
   * @param path the path to the file containing the JSON
   * @param spec the spec, a representation of how to decode a JSON value to a Java instance
   * @param config the builder configuration used to create untyped JSON object and array
   *               as {@link Map} and {@link List}.
   * @return a new instance containing all the JSON data typed as Java values
   * @throws IOException if an IO error occurs.
   */
  public static Object read(Path path, Spec spec, BuilderConfig config) throws IOException {
    requireNonNull(path);
    requireNonNull(spec);
    requireNonNull(config);
    return JsonReader.parse(path, spec.createBindVisitor(Object.class, config, null));
  }

  /**
   * Read a JSON array as a stream of instances of the type in parameter.
   * This method is a convenient method for {@link #stream(Reader, Spec, BuilderConfig)}.
   *
   * @param reader the reader containing the JSON fragment
   * @param type the type of the instance that should be returned
   * @param <T> the type of the instance that should be returned
   * @return a new instance containing all the JSON data typed as Java values
   * @throws IOException if an IO error occurs when opening the file.
   *
   * @see #read(Reader, Class, ArrayToken)
   */
  public <T> Stream<T> stream(Reader reader, Class<? extends T> type) throws IOException {
    requireNonNull(reader);
    requireNonNull(type);
    return stream(reader, type, DEFAULT_CONFIG);
  }

  /**
   * Read a JSON array as a stream of instances of the type in parameter.
   * This method is a convenient method for {@link #stream(Reader, Spec, BuilderConfig)}.
   *
   * @param reader the reader containing the JSON fragment
   * @param type the type of the instance that should be returned
   * @param config the builder configuration used to create untyped JSON object and array
   *               as {@link Map} and {@link List}.
   * @param <T> the type of the instance that should be returned
   * @return a new instance containing all the JSON data typed as Java values
   * @throws IOException if an IO error occurs when opening the file.
   *
   * @see #read(Reader, Class, ArrayToken)
   */
  public <T> Stream<T> stream(Reader reader, Class<? extends T> type, BuilderConfig config) throws IOException {
    requireNonNull(reader);
    requireNonNull(type);
    requireNonNull(config);
    return stream(reader, specForClass(type), config).map(type::cast);
  }

  /**
   * Read a JSON array as a stream of instances using the spec in parameter.
   *
   * @param reader the reader containing the JSON fragment
   * @param spec the spec, a representation of how to decode a JSON value to a Java instance
   * @param config the builder configuration used to create untyped JSON object and array
   *               as {@link Map} and {@link List}.
   * @return a new instance containing all the JSON data typed as Java values
   * @throws IOException if an IO error occurs when opening the file.
   *
   * @see #read(Reader, Class, ArrayToken)
   */
  public static Stream<Object> stream(Reader reader, Spec spec, BuilderConfig config) throws IOException {
    requireNonNull(reader);
    requireNonNull(spec);
    requireNonNull(config);
    return JsonReader.stream(reader, arrayForStreamVisitor(spec, config));
  }

  /**
   * Read a JSON array as a stream of instances of the type in parameter.
   * This method is a convenient method for {@link #stream(Reader, Spec, BuilderConfig)}.
   *
   * @param text the text containing the JSON
   * @param type the type of the instance that should be returned
   * @param <T> the type of the instance that should be returned
   * @return a new instance containing all the JSON data typed as Java values
   *
   * @see #read(Reader, Class, ArrayToken)
   */
  public <T> Stream<T> stream(String text, Class<? extends T> type) {
    requireNonNull(text);
    requireNonNull(type);
    return stream(text, type, DEFAULT_CONFIG);
  }

  /**
   * Read a JSON array as a stream of instances of the type in parameter.
   * This method is a convenient method for {@link #stream(Reader, Spec, BuilderConfig)}.
   *
   * @param text the text containing the JSON
   * @param type the type of the instance that should be returned
   * @param config the builder configuration used to create untyped JSON object and array
   *               as {@link Map} and {@link List}.
   * @param <T> the type of the instance that should be returned
   * @return a new instance containing all the JSON data typed as Java values
   *
   * @see #read(Reader, Class, ArrayToken)
   */
  public <T> Stream<T> stream(String text, Class<? extends T> type, BuilderConfig config)  {
    requireNonNull(text);
    requireNonNull(type);
    requireNonNull(config);
    return stream(text, specForClass(type), config).map(type::cast);
  }

  /**
   * Read a JSON array as a stream of instances using the spec in parameter.
   *
   * @param text the text containing the JSON
   * @param spec the spec, a representation of how to decode a JSON value to a Java instance
   * @param config the builder configuration used to create untyped JSON object and array
   *               as {@link Map} and {@link List}.
   * @return a new instance containing all the JSON data typed as Java values
   *
   * @see #read(Reader, Class, ArrayToken)
   */
  public static Stream<Object> stream(String text, Spec spec, BuilderConfig config) {
    requireNonNull(text);
    requireNonNull(spec);
    requireNonNull(config);
    return JsonReader.stream(text, arrayForStreamVisitor(spec, config));
  }

  /**
   * Read a JSON array as a stream of instances of the type in parameter.
   * This method is a convenient method for {@link #stream(Reader, Spec, BuilderConfig)}.
   *
   * @param path the path to the file containing the JSON
   * @param type the type of the instance that should be returned
   * @param <T> the type of the instance that should be returned
   * @return a new instance containing all the JSON data typed as Java values
   * @throws IOException if an IO error occurs when opening the file.
   *
   * @see #read(Reader, Class, ArrayToken)
   */
  public <T> Stream<T> stream(Path path, Class<? extends T> type) throws IOException {
    requireNonNull(path);
    requireNonNull(type);
    return stream(path, type, DEFAULT_CONFIG);
  }

  /**
   * Read a JSON array as a stream of instances of the type in parameter.
   * This method is a convenient method for {@link #stream(Reader, Spec, BuilderConfig)}.
   *
   * @param path the path to the file containing the JSON
   * @param type the type of the instance that should be returned
   * @param config the builder configuration used to create untyped JSON object and array
   *               as {@link Map} and {@link List}.
   * @param <T> the type of the instance that should be returned
   * @return a new instance containing all the JSON data typed as Java values
   * @throws IOException if an IO error occurs when opening the file.
   *
   * @see #read(Reader, Class, ArrayToken)
   */
  public <T> Stream<T> stream(Path path, Class<? extends T> type, BuilderConfig config) throws IOException {
    requireNonNull(path);
    requireNonNull(type);
    requireNonNull(config);
    return stream(path, specForClass(type), config).map(type::cast);
  }

  /**
   * Read a JSON array as a stream of instances using the spec in parameter.
   *
   * @param path the path to the file containing the JSON
   * @param spec the spec, a representation of how to decode a JSON value to a Java instance
   * @param config the builder configuration used to create untyped JSON object and array
   *               as {@link Map} and {@link List}.
   * @return a new instance containing all the JSON data typed as Java values
   * @throws IOException if an IO error occurs when opening the file.
   *
   * @see #read(Reader, Class, ArrayToken)
   */
  public static Stream<Object> stream(Path path, Spec spec, BuilderConfig config) throws IOException {
    requireNonNull(path);
    requireNonNull(spec);
    requireNonNull(config);
    return JsonReader.stream(path, arrayForStreamVisitor(spec, config));
  }

  private static ArrayVisitor arrayForStreamVisitor(Spec spec, BuilderConfig config) {
    return new ArrayVisitor() {
      @Override
      public VisitorMode mode() {
        return VisitorMode.PULL;
      }
      @Override
      public ObjectVisitor visitObject() {
        return spec.createBindVisitor(ObjectVisitor.class, config, null);
      }
      @Override
      public ArrayVisitor visitArray() {
        return spec.createBindVisitor(ArrayVisitor.class, config, null);
      }
      @Override
      public Object visitValue(JsonValue value) {
        return value.asObject();
      }
      @Override
      public Object visitEndArray() {
        return null;
      }
    };
  }
}
