package fr.umlv.jsonapi.bind;

import static java.util.Objects.requireNonNull;
import static java.util.function.UnaryOperator.identity;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.bind.Specs.ClassSpec;
import fr.umlv.jsonapi.bind.Specs.CollectionSpec;
import fr.umlv.jsonapi.bind.Specs.StreamSpec;
import fr.umlv.jsonapi.bind.Specs.ValueSpec;
import fr.umlv.jsonapi.builder.BuilderConfig;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
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
 * <p>By example to decode a JSON string as a {@link java.time.LocalDate} and vice-veras
 * <pre>
 * Binder binder = new Binder(lookup());
 * Spec stringSpec = binder.spec(String.class);
 * Spec localDateSpec = stringSpec.convert(new Converter() {
 *   public JsonValue convertTo(JsonValue value) {    // used when reading JSON
 *     // see as an opaque object (i.e. not a JSON type)
 *     return JsonValue.fromOpaque(LocalDate.parse(value.stringValue()));
 *   }
 *   public JsonValue convertFrom(JsonValue object) {  // used when writing JSON
 *     // convert to a JSON String
 *     return JsonValue.from(object.toString());
 *   }
 * });
 * binder.register(SpecFinder.associate(LocalDate.class, localDateSpec));
 * record Order(LocalDate date) { }
 * String json = """
 *   { "date": "2007-12-03" }
 *   """;
 * Order order = binder.read(json, Order.class);  // decode from JSON
 * String json2 = binder.write(order);            // encode as JSON
 * </pre>
 */
public /*sealed*/ interface Spec /*add permits clause*/ {

  /**
   * Wrap the current spec into a JSON array, the result will a {@link java.util.List}.
   * @return a new spec that see a JSON array as a List.
   */
  default Spec array() { return array(ArrayList::new, Collections::unmodifiableList); }

  default Spec array(Supplier<? extends List<Object>> supplier, UnaryOperator<List<Object>> op) {
    requireNonNull(supplier);
    requireNonNull(op);
    return new CollectionSpec(this, new ArrayLayout<List<Object>>() {
      @Override
      public List<Object> newBuilder() {
        return supplier.get();
      }

      @Override
      public List<Object> addObject(List<Object> builder, Object object) {
        builder.add(object);
        return builder;
      }

      @Override
      public List<Object> addArray(List<Object> builder, Object array) {
        builder.add(array);
        return builder;
      }

      @Override
      public List<Object> addValue(List<Object> builder, JsonValue value) {
        builder.add(value.asObject());
        return builder;
      }

      @Override
      public Object build(List<Object> builder) {
        return op.apply(builder);
      }
    });
  }

  /**
   * Wrap the current spec into a JSON object, the result will a {@link java.util.Map}.
   * @return a new spec that see a JSON object as a Map.
   */
  default Spec object() {
    return object(HashMap::new, Collections::unmodifiableMap);
  }

  default Spec object(Supplier<? extends Map<String, Object>> supplier, UnaryOperator<Map<String, Object>> op) {
    requireNonNull(supplier);
    requireNonNull(op);
    return new ClassSpec("Map", null, new ObjectLayout<Map<String, Object>>() {
      @Override
      public Spec memberSpec(String memberName) {
        return Spec.this;
      }

      @Override
      public Map<String, Object> newBuilder() {
        return supplier.get();
      }

      @Override
      public Map<String, Object> addObject(Map<String, Object> builder, String memberName, Object object) {
        builder.put(memberName, object);
        return builder;
      }

      @Override
      public Map<String, Object> addArray(Map<String, Object> builder, String memberName, Object array) {
        builder.put(memberName, array);
        return builder;
      }

      @Override
      public Map<String, Object> addValue(Map<String, Object> builder, String memberName, JsonValue value) {
        builder.put(memberName, value.asObject());
        return builder;
      }

      @Override
      public Object build(Map<String, Object> builder) {
        return op.apply(builder);
      }

      @Override
      public void accept(Object object, MemberVisitor memberVisitor) {
        @SuppressWarnings("unchecked")
        var map = (Map<String, Object>) object;
        map.forEach(memberVisitor::visitMember);
      }
    });
  }

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
   * @throws IllegalArgumentException if the spec doesn't allow the kind of visitor requested or
   *         do not allow a non null delegate
   *
   * @see Binder#read(Reader, Spec, BuilderConfig)
   */
  default <V> V createBindVisitor(Class<? extends V> visitorType) {
    requireNonNull(visitorType);
    if (this instanceof CollectionSpec collectionSpec) {
      return visitorType.cast(new BindCollectionVisitor(collectionSpec));
    }
    if (this instanceof StreamSpec streamSpec) {
      return visitorType.cast(new BindStreamVisitor(streamSpec));
    }
    if (this instanceof ClassSpec classSpec) {
      return visitorType.cast(new BindClassVisitor(classSpec));
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


  /**
   * Convert a JSON type to a Java type (in both direction)
   */
  interface Converter {
    /**
     * Receive a JSON value by example from reading a file and
     * convert it to a JSON value for the binder.
     *
     * @param value a JSON value that comes from a JSON fragment
     * @return a JSON value for the binder which allows more types
     *
     * @see JsonValue#fromAny(Object)
     * @see JsonValue#fromOpaque(Object)
     */
    JsonValue convertTo(JsonValue value);

    /**
     * Receive a Java object seen as a JSON value and convert it
     * to a JSON value by example writable to a JSON file
     * @param object a Java object seen as a JSON value
     * @return a JSON value that can be written into a JSON fragment
     */
    JsonValue convertFrom(JsonValue object);
  }

  interface ArrayLayout<B> {
    B newBuilder();
    B addObject(B builder, Object object);
    B addArray(B builder, Object array);
    B addValue(B builder, JsonValue value);
    Object build(B builder);

    //void accept(Object object, Consumer<? super Object> valueVisitor);
  }

    /**
   * Abstraction that represent a Java class that can be decoded from JSON and encoded to JSON
   *
   * <p>The way to decode a JSON object to any class is to use a builder like API which
   * works for both mutable and immutable builder. Obviously, the builder doesn't have to be
   * a real builder, it can be an array of object, a map or whatever you want.
   *
   * The decoding of the member values of an object is done by calling
   * {@link #memberSpec(String)} to get the spec of each member.
   *
   * <p>The API works that way
   * <ul>
   *   <li>{@link #newBuilder()} is called first to create a builder
   *   <li>{@link #addObject(Object, String, Object)}, {@link #addArray(Object, String, Object)}
   *       and {@link #addValue(Object, String, JsonValue)} are called each time a memeber
   *       of the JSON object the class represent is seen
   *   <li>{@link #build(Object)} is called at the end to transform the builder value to
   *       the real object
   * </ul>
   *
   * <p>The way to encode a JSON object is to receive a visitor that should be called
   *    for each member of the class (the pair field name/field value).
   *    The method {@link #accept(Object, MemberVisitor)} is called with an instance of
   *    the class to encode to JSON and the visitor to call on a members.
   *    When encoding, the {@link #memberSpec(String) member spec} is not used because
   *    it represent the compile time type of a member, the dynamic type of the member value
   *    carry more information.
   *
   * @param <B> the type of the builder
   */
  interface ObjectLayout<B> {

    /**
     * Returns the compile time information of a member as a {@link Spec spec}.
     * @param memberName the name of the member
     * @return the spec of the object member
     */
    Spec memberSpec(String memberName);

    /**
     * Creates a builder to decode this object
     * @return a builder used to decode this object
     */
    B newBuilder();

    /**
     * Add a Java object decoded from a JSON object as member into the builder.
     * Given that the builder can be immutable, the builder has to be returned
     *
     * @param builder the builder that will store the member value
     * @param memberName the name of the member
     * @param object the value of the member
     * @return either the same builder or a new builder to continue the decoding
     */
    B addObject(B builder, String memberName, Object object);

    /**
     * Add a Java object decoded from a JSON array as member into the builder.
     * Given that the builder can be immutable, the builder has to be returned
     *
     * @param builder the builder that will store the member value
     * @param memberName the name of the member
     * @param array the value of the member
     * @return either the same builder or a new builder to continue the decoding
     */
    B addArray(B builder, String memberName, Object array);

    /**
     * Add a JSON value as member into the builder.
     * Given that the builder can be immutable, the builder has to be returned
     *
     * @param builder the builder that will store the member value
     * @param memberName the name of the member
     * @param value the value of the member
     * @return either the same builder or a new builder to continue the decoding
     */
    B addValue(B builder, String memberName, JsonValue value);

    /**
     * Create the final object from the builder, if the object is immutable,
     * it can be the builder itself.
     *
     * @param builder the builder containing all the previously decoded values
     * @return a Java object representing the JSON object fully decoded
     */
    Object build(B builder);

    /**
     * Called to encode a Java object as a JSON object
     *
     * @param object a Java object
     * @param memberVisitor a visitor with one method
     *        {@link MemberVisitor#visitMember(String, Object)} that should be called once
     *        per members with the member value
     */
    void accept(Object object, MemberVisitor memberVisitor);

    /**
     * A visitor of the member of a Java object
     */
    @FunctionalInterface
    interface MemberVisitor {

      /**
       * Called once per object member to indicate the name of the member and its value.
       * @param name the name of the member
       * @param value the value of the member
       */
      void visitMember(String name, Object value);
    }

    // should be a private instance method, but IntelliJ has a bug :(
    private static ObjectLayout<Object> convert(ObjectLayout<Object> layout, Converter converter) {
      return new ObjectLayout<>() {
        @Override
        public Spec memberSpec(String memberName) {
          return layout.memberSpec(memberName);
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