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
 *   <li>Using the factory methods {@link Spec#typedClass(String, ClassLayout)} and
 *   {@link Spec#valueClass(String, Converter)} for respectively create a spec of a JSON object
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
  default Spec array() { return new ArraySpec(this); }
  default Spec stream(Function<? super Stream<Object>, ?> aggregator) { return new StreamSpec(this, aggregator); }
  default Spec object() { return new ObjectSpec(this, null); }

  default Spec convert(Converter converter) {
    requireNonNull(converter);
    if (this instanceof ValueSpec valueSpec) {
      return valueSpec.convertWith(converter);
    }
    if (this instanceof ClassSpec classSpec) {
      return classSpec.mapLayout(layout -> ClassLayout.convert(layout, converter));
    }
    throw new IllegalArgumentException("can not apply a converter to this spec");
  }

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

  default Spec mapLayout(Function<? super ClassLayout<Object>, ClassLayout<?>> mapper) {
    requireNonNull(mapper);
    if (this instanceof ClassSpec classSpec) {
      @SuppressWarnings("unchecked")
      var classLayout = (ClassLayout<Object>) classSpec.classLayout();
      return typedClass(classSpec.name() + ".mapLayout()", mapper.apply(classLayout));
    }
    throw new IllegalArgumentException("can not apply a mapper to this spec");
  }

  default <V> V createBindVisitor(Class<V> visitorType) {
    return createBindVisitor(visitorType, Binder.DEFAULT_CONFIG, null);
  }
  default <V> V createBindVisitor(Class<V> visitorType, BuilderConfig config, V delegate) {
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
    throw new AssertionError();
  }

  static Spec typedClass(String name, ClassLayout<?> classLayout) {
    requireNonNull(name);
    requireNonNull(classLayout);
    return new ClassSpec(name, null, classLayout);
  }
  static Spec valueClass(String name, Converter converter) {
    requireNonNull(name);
    return new ValueSpec(name, converter);
  }
  
  interface ClassLayout<B> {
    Spec elementSpec(String name);

    B newBuilder();
    B addObject(B builder, String name, Object object);
    B addArray(B builder, String name, Object array);
    B addValue(B builder, String name, JsonValue value);
    Object build(B builder);

    // should be a private instance method, but IntelliJ has a bug :(
    private static ClassLayout<Object> convert(ClassLayout<Object> layout, Converter converter) {
      return new ClassLayout<>() {
        @Override
        public Spec elementSpec(String name) {
          return layout.elementSpec(name);
        }
        @Override
        public Object newBuilder() {
          return layout.newBuilder();
        }
        @Override
        public Object addObject(Object builder, String name, Object object) {
          return layout.addObject(builder, name, object);
        }
        @Override
        public Object addArray(Object builder, String name, Object array) {
          return layout.addArray(builder, name, array);
        }
        @Override
        public Object addValue(Object builder, String name, JsonValue value) {
          return layout.addValue(builder, name, value);
        }

        @Override
        public Object build(Object builder) {
          return converter.convertTo(JsonValue.fromOpaque(layout.build(builder))).asObject();
        }
      };
    }
  }

  interface Converter {
    JsonValue convertTo(JsonValue value);
    //JsonValue convertFrom(JsonValue value);
  }
}