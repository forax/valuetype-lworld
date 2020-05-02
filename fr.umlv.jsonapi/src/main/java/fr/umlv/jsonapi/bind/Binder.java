package fr.umlv.jsonapi.bind;

import static java.util.Objects.requireNonNull;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.JsonPrinter;
import fr.umlv.jsonapi.JsonReader;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.JsonWriter;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.VisitorMode;
import fr.umlv.jsonapi.builder.BuilderConfig;
import fr.umlv.jsonapi.internal.RootVisitor;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/**
 * A binder is able to creates a tree of Java objects from a JSON text.
 *
 * A binder associate an object {@link Spec} for each JSON value, a {@link Spec} instance
 * knows how to serialize/deserialize a JSON value.
 * Those {@link Spec} instances are created lazily when calling the method {@link #spec(Type)}
 * and stored inside the binder for reuse.
 *
 * The creation of a {@link Spec} instance is delegated to several {@link SpecFinder}s
 * that are {@link #register(SpecFinder) registered} to the binder.
 * When {@link #spec(Type) looking up} for a spec, the binder will test the first registered
 * finder first, then the second, and so on.
 *
 * The method {@link #spec(Type)} is idempotent which means that once a Spec has been associated
 * to a type, it can not be changed, even by {@link #register(SpecFinder) registering}
 * a new {@link SpecFinder}.
 *
 * There are two ways to create a binder, to get a binder with the default spec finders
 * already registered use the constructor {@link Binder#Binder(Lookup)},
 * to get a binder with no spec finders registered use {@link Binder#noDefaults()}.
 *
 * <p>To transform a JSON text to an object, it exists several variants of the method
 * {@link #read(Reader, Spec)} and {@link #stream(Reader, Spec)}.
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
 * <p>Ton transform a Java object to a JSON text, it exists several variants of the method
 * {@link #write(Writer, Object)}
 *
 * By example, to write a record
 * <pre>
 * record Person(String name, int age, boolean bald) { }
 * Person person = new Person("Doctor X", 23, false);
 * Binder binder = new Binder(lookup());
 * String text = binder.write(person);
 * </pre>
 *
 * Reading and writing objects is a not fully symmetrical process due to the fact that JSON
 * format has less information than a Java class definition.
 * So when reading a JSON text, the binder uses the information derived from the Java
 * type definition available at compile time to guide the creation of objects.
 * While when writing, the object class is available so the binder uses the dynamic type
 * of the object instead of the static type.
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
      throw new BindingException("no finders can resolve type " + type.getName());
    }
  };
  private final CopyOnWriteArrayList<SpecFinder> finders = new CopyOnWriteArrayList<>();

  private Binder() {
    // empty
  }

  /**
   * Creates a binder with default {@link SpecFinder}s pre-{@link #register(SpecFinder) registered}.
   * @param lookup the security context that will be used to load the class necessary when
   *               {@link #read(Reader, Spec) reading} a JSON fragment.
   *
   * @see java.lang.invoke.MethodHandles#lookup()
   * @see #noDefaults()
   */
  public Binder(Lookup lookup) {
    requireNonNull(lookup);
    finders.add(SpecFinders.newRecordFinder(lookup, this::spec));
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
   * Register a spec finder to the current binder after all the other spec finders.
   * When {@link #spec(Type) looking up} for a spec, the binder will test the first registered
   * finder first.
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
        || type == Boolean.class || type == Integer.class || type == Long.class || type == Double.class
        || type == String.class || type == BigInteger.class || type == BigDecimal.class
        || type == Object.class) {
      return Spec.newTypedValue(type.getName(), null);
    }
    for(var finder: finders) {
      var optSpec = finder.findSpec(type);
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
   * This method is a convenient method for {@link #read(Reader, Spec)}.
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
    return type.cast(read(reader, specForClass(type)));
  }

  /**
   * Read a JSON array and convert it to a list of instances of the type in parameter.
   * This method is a convenient method for {@link #read(Reader, Spec)}.
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
    return (List<T>) read(reader, specForClass(type).array());
  }

  /**
   * Read a JSON object and convert it to a map of instances of the type in parameter.
   * This method is a convenient method for {@link #read(Reader, Spec)}.
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
    return (Map<String, T>) read(reader, specForClass(type).object());
  }

  /**
   * Read a JSON fragment and convert it to a Java instance using the spec in parameter.
   *
   * @param reader the reader containing the JSON fragment
   * @param spec the spec, a representation of how to decode a JSON value to a Java instance.
   * @return a new instance containing all the JSON data typed as Java values
   * @throws IOException if an IO error occurs.
   */
  public static Object read(Reader reader, Spec spec) throws IOException {
    requireNonNull(reader);
    requireNonNull(spec);
    return JsonReader.parse(reader, spec.createBindVisitor(Object.class));
  }

  /**
   * Read a JSON text and convert it to an instance of the type in parameter.
   * This method is a convenient method for {@link #read(String, Spec)}.
   *
   * @param text the text containing the JSON
   * @param type the type of the instance that should be returned
   * @param <T> the type of the instance that should be returned
   * @return a new instance containing all the JSON data typed as Java values
   */
  public <T> T read(String text, Class<? extends T> type) {
    requireNonNull(text);
    requireNonNull(type);
    return type.cast(read(text, specForClass(type)));
  }

  /**
   * Read a JSON text and convert it to a list of instances of the type in parameter.
   * This method is a convenient method for {@link #read(String, Spec)}.
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
    return (List<T>) read(text, specForClass(type).array());
  }

  /**
   * Read a JSON object and convert it to a map of instances of the type in parameter.
   * This method is a convenient method for {@link #read(Reader, Spec)}.
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
    return (Map<String, T>) read(text, specForClass(type).object());
  }

  /**
   * Read a JSON text and convert it to a Java instance using the spec in parameter.
   *
   * @param text the text containing the JSON
   * @param spec the spec, a representation of how to decode a JSON value to a Java instance.
   * @return a new instance containing all the JSON data typed as Java values
   */
  public static Object read(String text, Spec spec) {
    requireNonNull(text);
    requireNonNull(spec);
    return JsonReader.parse(text, spec.createBindVisitor(Object.class));
  }

  /**
   * Read a JSON file and convert it to an instance of the type in parameter.
   * This method is a convenient method for {@link #read(Path, Spec)}.
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
    return type.cast(read(path, specForClass(type)));
  }

  /**
   * Read a JSON file and convert it to list of instances of the type in parameter.
   * This method is a convenient method for {@link #read(Path, Spec)}.
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
    return (List<T>) read(path, specForClass(type).array());
  }

  /**
   * Read a JSON object and convert it to a map of instances of the type in parameter.
   * This method is a convenient method for {@link #read(Reader, Spec)}.
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
    return (Map<String, T>) read(path, specForClass(type).object());
  }

  /**
   * Read a JSON file and convert it to a Java instance using the spec in parameter.
   *
   * @param path the path to the file containing the JSON
   * @param spec the spec, a representation of how to decode a JSON value to a Java instance
   * @return a new instance containing all the JSON data typed as Java values
   * @throws IOException if an IO error occurs.
   */
  public static Object read(Path path, Spec spec) throws IOException {
    requireNonNull(path);
    requireNonNull(spec);
    return JsonReader.parse(path, spec.createBindVisitor(Object.class));
  }


  /**
   * Read a JSON array as a stream of instances of the type in parameter.
   * This method is a convenient method for {@link #stream(Reader, Spec)}.
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
    return stream(reader, specForClass(type)).map(type::cast);
  }

  /**
   * Read a JSON array as a stream of instances using the spec in parameter.
   *
   * @param reader the reader containing the JSON fragment
   * @param spec the spec, a representation of how to decode a JSON value to a Java instance
   * @return a new instance containing all the JSON data typed as Java values
   * @throws IOException if an IO error occurs when opening the file.
   *
   * @see #read(Reader, Class, ArrayToken)
   */
  public static Stream<Object> stream(Reader reader, Spec spec) throws IOException {
    requireNonNull(reader);
    requireNonNull(spec);
    return JsonReader.stream(reader, arrayForStreamVisitor(spec));
  }

  /**
   * Read a JSON array as a stream of instances of the type in parameter.
   * This method is a convenient method for {@link #stream(Reader, Spec)}.
   *
   * @param text the text containing the JSON
   * @param type the type of the instance that should be returned
   * @param <T> the type of the instance that should be returned
   * @return a new instance containing all the JSON data typed as Java values
   *
   * @see #read(Reader, Class, ArrayToken)
   */
  public <T> Stream<T> stream(String text, Class<? extends T> type)  {
    requireNonNull(text);
    requireNonNull(type);
    return stream(text, specForClass(type)).map(type::cast);
  }

  /**
   * Read a JSON array as a stream of instances using the spec in parameter.
   *
   * @param text the text containing the JSON
   * @param spec the spec, a representation of how to decode a JSON value to a Java instance
   * @return a new instance containing all the JSON data typed as Java values
   *
   * @see #read(Reader, Class, ArrayToken)
   */
  public static Stream<Object> stream(String text, Spec spec) {
    requireNonNull(text);
    requireNonNull(spec);
    return JsonReader.stream(text, arrayForStreamVisitor(spec));
  }

  /**
   * Read a JSON array as a stream of instances of the type in parameter.
   * This method is a convenient method for {@link #stream(Reader, Spec)}.
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
    return stream(path, specForClass(type)).map(type::cast);
  }

  /**
   * Read a JSON array as a stream of instances using the spec in parameter.
   *
   * @param path the path to the file containing the JSON
   * @param spec the spec, a representation of how to decode a JSON value to a Java instance
   * @return a new instance containing all the JSON data typed as Java values
   * @throws IOException if an IO error occurs when opening the file.
   *
   * @see #read(Reader, Class, ArrayToken)
   */
  public static Stream<Object> stream(Path path, Spec spec) throws IOException {
    requireNonNull(path);
    requireNonNull(spec);
    return JsonReader.stream(path, arrayForStreamVisitor(spec));
  }

  private static ArrayVisitor arrayForStreamVisitor(Spec spec) {
    return new ArrayVisitor() {
      @Override
      public VisitorMode visitStartArray() {
        return VisitorMode.PULL;
      }
      @Override
      public ObjectVisitor visitObject() {
        return spec.createBindVisitor(ObjectVisitor.class);
      }
      @Override
      public ArrayVisitor visitArray() {
        return spec.createBindVisitor(ArrayVisitor.class);
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


  /**
   * Generate calls to the visit methods of the visitor describing the Java object
   * taken as argument as a JSON object/array.
   *
   * @param value any Java object or null.
   * @param visitor either an {@link ObjectVisitor} or an {@link ArrayVisitor}
   * @return the result returned by either the method {@link ObjectVisitor#visitEndObject()} or
   *         by the method {@link ArrayVisitor#visitEndArray()} of the visitor
   */
  public Object accept(Object value, Object visitor) {
    return Specs.acceptRoot(value, this, RootVisitor.createFromOneVisitor(visitor));
  }

  /**
   * Generate calls to the visit methods of the visitor describing the Java object
   * taken as argument as a JSON object/array.
   *
   * @param value any Java object or null.
   * @param objectVisitor the visitor used if the value is a convertible to a JSON object
   * @param arrayVisitor the visitor used if the value is a convertible to a JSON array
   * @return the result returned by either the method {@link ObjectVisitor#visitEndObject()} or
   *         by the method {@link ArrayVisitor#visitEndArray()} of the visitors
   */
  public Object accept(Object value, ObjectVisitor objectVisitor, ArrayVisitor arrayVisitor) {
    requireNonNull(objectVisitor);
    requireNonNull(arrayVisitor);
    return Specs.acceptRoot(value, this, new RootVisitor(RootVisitor.BOTH, objectVisitor, arrayVisitor));
  }


  /**
   * Write a tree of Java objects as a JSON text into a {@code writer}.
   *
   * @param writer the writer to write into
   * @param value the Java object to write
   * @throws IOException if an IO error occurs
   * @throws BindingException if an object has no corresponding {@link #spec(Type) spec}
   *
   * @see #accept(Object, Object)
   * @see #spec(Type)
   */
  @SuppressWarnings("resource")
  public void write(Writer writer, Object value) throws IOException {
    requireNonNull(writer);
    requireNonNull(value);
    var jsonWriter = new JsonWriter(writer);
    try {
      accept(value, jsonWriter, jsonWriter);
    } catch(UncheckedIOException e) {
      throw e.getCause();
    }
    // don't close here because it will close the writer
  }

  /**
   * Write a tree of Java objects as a string
   *
   * @param value the Java object to write
   * @return a string in JSON format of the Java object
   * @throws BindingException if an object has no corresponding {@link #spec(Type) spec}
   *
   * @see #write(Writer, Object)
   */
  public String write(Object value) {
    requireNonNull(value);
    var printer = new JsonPrinter();
    accept(value, printer, printer);
    return printer.toString();
  }

  /**
   * Write a tree of Java objects as a JSON text into file.
   *
   * @param path the path to the file
   * @param value the Java object to write
   * @throws IOException if an IO error occurs
   * @throws BindingException if an object has no corresponding {@link #spec(Type) spec}
   *
   * @see #accept(Object, Object)
   * @see #spec(Type)
   */
  public void write(Path path, Object value) throws IOException {
    requireNonNull(path);
    requireNonNull(value);
    try(var writer = Files.newBufferedWriter(path)) {
      write(writer, value);
    }
  }
}
