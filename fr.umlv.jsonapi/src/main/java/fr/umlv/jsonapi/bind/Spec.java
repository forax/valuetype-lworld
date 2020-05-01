package fr.umlv.jsonapi.bind;

import static java.util.Objects.requireNonNull;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.bind.Specs.ArraySpec;
import fr.umlv.jsonapi.bind.Specs.ClassSpec;
import fr.umlv.jsonapi.bind.Specs.ObjectSpec;
import fr.umlv.jsonapi.bind.Specs.StreamSpec;
import fr.umlv.jsonapi.bind.Specs.ValueSpec;
import fr.umlv.jsonapi.builder.BuilderConfig;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A spec is an object used by the {@link Binder} to convert a JSON fragment to a Java object
 * and vice-versa.
 *
 * The easy way to get a Spec instance is to call {@link Binder#spec(Type)} with a Java type,
 * by example {@code binder.spec(String.class)} returns the spec associated to the class
 * {@code java.lang.String}.
 *
 * You can create your own Spec either
 * <ul>
 *   <li>Using the factory methods {@link Spec#newTypedObject(String, ObjectLayout)} and
 *   {@link Spec#newTypedValue(String, Converter)} for respectively create a spec of a JSON object
 *   or a spec of a JSON value.
 *   <li>Using instance methods to create a spec from an existing spec, {@link #array()},
 *   {@link #stream(Function)}, {@link #object()} which respectively create an array of the spec
 *   (a {@link java.util.List List&lt;Object&gt;}), create a stream of the spec and create
 *   an object of the spec (a {@link java.util.Map Map&lt;String, Object&gt;}).
 *   Furthermore, using {@link #convert(Converter)}, {@link #filterName(Predicate)} and
 *   {@link #mapLayout(Function)} allow to alter the JSON serialization/deserialization
 *   by transforming the value in an ad-hoc way.
 * </ul>
 *
 * <p>By example to decode a JSON string has a {@link java.time.LocalDate}
 * <pre>
 * Binder binder = new Binder(lookup());
 * Spec stringSpec = binder.spec(String.class);
 * Spec localDateSpec = stringSpec.convert(new Converter() {
 *   public JsonValue convertTo(JsonValue value) {
 *     return JsonValue.fromOpaque(LocalDate.parse(value.stringValue()));
 *   }
 * });
 * binder.register(SpecFinder.from(Map.of(LocalDate.class, localDateSpec)));
 * record Order(LocalDate date) { }
 * String json = """
 *   { "date": "2007-12-03" }
 *   """;
 * Order order = binder.read(json, Order.class);
 * </pre>
 */
public /*sealed*/ interface Spec /*add permits clause*/ {

  /**
   * Wrap the current spec into a JSON array, the result will a {@link java.util.List}.
   * @return a new spec that see a JSON array as a List.
   */
  default Spec array() { return new ArraySpec(this); }

  /**
   * Wrap the current spec into a JSON object, the result will a {@link java.util.Map}.
   * @return a new spec that see a JSON object as a Map.
   */
  default Spec object() { return new ObjectSpec(this, null); }

  /**
   * Wrap the current spec into a JSON array, the array is decoded using a {@link Stream stream}
   * and the result is the return value of the aggregator function.
   * @return a new spec that see a JSON array as a Stream.
   */
  default Spec stream(Function<? super Stream<Object>, ?> aggregator) { return new StreamSpec(this, aggregator); }

  /**
   * Returns a new spec that will apply the conversion functions when reading/writing JSON
   * @param converter the converting functions
   * @return a new spec that will apply the conversion functions when reading/writing JSON
   * @throws IllegalArgumentException is the current spec doesn't represent a Java value
   */
  default Spec convert(Converter converter) {
    requireNonNull(converter);
    if (this instanceof ValueSpec valueSpec) {
      return valueSpec.convertWith(converter);
    }
    if (this instanceof ClassSpec classSpec) {
      return classSpec.mapLayout(layout -> ObjectLayout.convert(layout, converter));
    }
    throw new IllegalArgumentException("can not apply a converter to this spec");
  }

  /**
   * Returns a new spec that allow to filter which object elements to keep
   * @param predicate a boolean function that return true to keep the object element
   * @return a new spec that allow to filter which object elements to keep
   * @throws IllegalArgumentException is the current spec doesn't represent a JSON object
   */
  default Spec filterName(Predicate<? super String> predicate) {
    requireNonNull(predicate);
    if (this instanceof ObjectSpec objectSpec) {
      return objectSpec.filterWith(predicate);
    }
    if (this instanceof ClassSpec classSpec) {
      return classSpec.filterWith(predicate);
    }
    throw new IllegalArgumentException("can not apply a filter to this spec");
  }

  /**
   * Returns a new spec that transforms the layout of the current spec.
   * @param mapper the layout transformation function
   * @return a new spec that will transforms the layout of the current spec.
   * @throws IllegalArgumentException is the current spec doesn't have a class layout
   */
  default Spec mapLayout(Function<? super ObjectLayout<Object>, ObjectLayout<?>> mapper) {
    requireNonNull(mapper);
    if (this instanceof ClassSpec classSpec) {
      @SuppressWarnings("unchecked")
      var classLayout = (ObjectLayout<Object>) classSpec.objectLayout();
      return newTypedObject(classSpec.name() + ".mapLayout()", mapper.apply(classLayout));
    }
    throw new IllegalArgumentException("can not apply a mapper to this spec");
  }


  /**
   * Returns either an {@link ObjectVisitor} or an {@link ArrayVisitor}, depending if the spec
   * represents a JSON object or a JSON array, that can be used to read a JSON fragment.
   * @param visitorType the type of visitor wanted ({@link ObjectVisitor} or {@link ArrayVisitor})
   * @param <V> the type of the visitor
   * @return a newly created {@link ObjectVisitor} or {@link ArrayVisitor}
   *
   * @throws IllegalArgumentException if the spec doesn't allow the kind of visitor requested
   *
   * @see Binder#read(Reader, Spec, BuilderConfig)
   */
  default <V> V createBindVisitor(Class<V> visitorType) {
    return createBindVisitor(visitorType, Binder.DEFAULT_CONFIG, null);
  }

  /**
   * Returns either an {@link ObjectVisitor} or an {@link ArrayVisitor}, depending if the spec
   * represents a JSON object or a JSON array, that can be used to read a JSON fragment.
   * @param visitorType the type of visitor wanted ({@link ObjectVisitor} or {@link ArrayVisitor})
   * @param config the configuration of the builder that will be used if necessary
   * @param delegate a delegate if the spec represent a {@link java.util.List} or a
   *                 {@link java.util.Map} or {code null}
   *                 (see {@link BuilderConfig#newObjectBuilder(ObjectVisitor)})
   * @param <V> the type of the visitor
   * @return a newly created {@link ObjectVisitor} or {@link ArrayVisitor}
   *
   * @throws IllegalArgumentException if the spec doesn't allow the kind of visitor requested or
   *         do not allow a non null delegate
   *
   * @see Binder#read(Reader, Spec, BuilderConfig)
   */
  default <V> V createBindVisitor(Class<? extends V> visitorType, BuilderConfig config, V delegate) {
    requireNonNull(visitorType);
    requireNonNull(config);
    if (this instanceof ObjectSpec objectSpec) {
      if (delegate != null && !(delegate instanceof ObjectVisitor)) {
        throw new IllegalArgumentException("delegate should be a subclass of ObjectVisitor or null");
      }
      return visitorType.cast(new BindObjectVisitor(objectSpec, config.newObjectBuilder((ObjectVisitor) delegate)));
    }
    if (this instanceof ArraySpec arraySpec) {
      if (delegate != null && !(delegate instanceof ArrayVisitor)) {
        throw new IllegalArgumentException("delegate should be a subclass of ArrayVisitor or null");
      }
      return visitorType.cast(new BindArrayVisitor(arraySpec, config.newArrayBuilder((ArrayVisitor) delegate)));
    }
    if (this instanceof StreamSpec streamSpec) {
      return visitorType.cast(new BindStreamVisitor(streamSpec, config));
    }
    if (this instanceof ClassSpec classSpec) {
      return visitorType.cast(new BindClassVisitor(classSpec, config));
    }
    throw new IllegalArgumentException("can not create a visitor on this spec");
  }

  /**
   * Create a spec corresponding to mapping of a JSON object to a Java object
   * @param name the name of the spec for debugging purpose
   * @param objectLayout an abstraction describing how to build the Java object
   *                    from the JSON object elements
   * @return a spec that is able to convert a JSON object to a Java object
   */
  static Spec newTypedObject(String name, ObjectLayout<?> objectLayout) {
    requireNonNull(name);
    requireNonNull(objectLayout);
    return new ClassSpec(name, null, objectLayout);
  }

  /**
   * Creates a spec corresponding to a JSON value providing a converter
   * from the JSON {@link JsonValue primitive value} to any value
   * @param name the name of the spec for debugging purpose
   * @param converter the converter to use.
   * @return a spec that is able to convert a JSON value to another one
   */
  static Spec newTypedValue(String name, Converter converter) {
    requireNonNull(name);
    return new ValueSpec(name, converter);
  }


  interface Converter {
    JsonValue convertTo(JsonValue value);
    JsonValue convertFrom(JsonValue object);
  }

  interface ObjectLayout<B> {
    Spec elementSpec(String memberName);

    B newBuilder();
    B addObject(B builder, String memberName, Object object);
    B addArray(B builder, String memberName, Object array);
    B addValue(B builder, String memberName, JsonValue value);
    Object build(B builder);

    void accept(Object object, MemberVisitor memberVisitor);

    @FunctionalInterface
    interface MemberVisitor {
      void visitMember(String name, Object value);
    }

    // should be a private instance method, but IntelliJ has a bug :(
    private static ObjectLayout<Object> convert(ObjectLayout<Object> layout, Converter converter) {
      return new ObjectLayout<>() {
        @Override
        public Spec elementSpec(String memberName) {
          return layout.elementSpec(memberName);
        }

        @Override
        public Object newBuilder() {
          return layout.newBuilder();
        }
        @Override
        public Object addObject(Object builder, String memberName, Object object) {
          return layout.addObject(builder, memberName, object);
        }
        @Override
        public Object addArray(Object builder, String memberName, Object array) {
          return layout.addArray(builder, memberName, array);
        }
        @Override
        public Object addValue(Object builder, String memberName, JsonValue value) {
          return layout.addValue(builder, memberName, value);
        }
        @Override
        public Object build(Object builder) {
          var result = layout.build(builder);
          return converter.convertTo(JsonValue.fromOpaque(result)).asObject();
        }

        @Override
        public void accept(Object object, MemberVisitor memberVisitor) {
           var result = converter.convertFrom(JsonValue.fromOpaque(object)).asObject();
           layout.accept(result, memberVisitor);
        }
      };
    }
  }
}