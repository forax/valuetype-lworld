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

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public /*sealed*/ interface Spec /*add permits clause*/ {
  default Spec array() { return new ArraySpec(this); }
  default Spec stream(Function<? super Stream<Object>, ?> aggregator) { return new StreamSpec(this, aggregator); }
  default Spec object() { return new ObjectSpec(this, null); }

  default Spec convert(Converter converter) {
    requireNonNull(converter);
    if (this instanceof ValueSpec valueSpec) {
      return valueSpec.convertWith(converter);
    }
    throw new IllegalArgumentException("can not convert this spec");
  }

  default Spec filterName(Predicate<? super String> predicate) {
    requireNonNull(predicate);
    if (this instanceof ObjectSpec objectSpec) {
      return objectSpec.filterWith(predicate);
    }
    if (this instanceof ClassSpec classSpec) {
      return classSpec.filterWith(predicate);
    }
    throw new IllegalArgumentException("can not filter this spec");
  }

  default Spec mapLayout(Function<? super ClassLayout<Object>, ClassLayout<?>> mapper) {
    requireNonNull(mapper);
    if (this instanceof ClassSpec classSpec) {
      @SuppressWarnings("unchecked")
      var classLayout = (ClassLayout<Object>) classSpec.classLayout();
      return typedClass(classSpec.name() + ".mapLayout()", mapper.apply(classLayout));
    }
    throw new IllegalArgumentException("can not map this spec");
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
  }

  interface Converter {
    JsonValue convertTo(JsonValue value);
    //JsonValue convertFrom(JsonValue value);
  }
}