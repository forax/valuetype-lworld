package fr.umlv.jsonapi.bind;

import static java.util.Objects.requireNonNull;

import fr.umlv.jsonapi.BuilderConfig;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.bind.Specs.ArraySpec;
import fr.umlv.jsonapi.bind.Specs.ClassSpec;
import fr.umlv.jsonapi.bind.Specs.ObjectSpec;
import fr.umlv.jsonapi.bind.Specs.StreamSpec;
import fr.umlv.jsonapi.bind.Specs.ValueSpec;
import java.util.function.Function;
import java.util.stream.Stream;

public /*sealed*/ interface Spec /*add permits clause*/ {
  default Spec array() { return new ArraySpec(this); }
  default Spec stream(Function<? super Stream<Object>, ?> aggregator) { return new StreamSpec(this, aggregator); }
  default Spec object() { return new ObjectSpec(this); }

  default Spec convert(Converter converter) {
    requireNonNull(converter);
    if (this instanceof ValueSpec valueSpec) {
      return valueSpec.composeWith(converter);
    }
    throw new IllegalArgumentException("can not convert this spec");
  }

  default <V> V createBindVisitor(Class<V> visitorType) {
    return createBindVisitor(visitorType, Binder.DEFAULT_CONFIG);
  }
  default <V> V createBindVisitor(Class<V> visitorType, BuilderConfig config) {
    requireNonNull(visitorType);
    requireNonNull(config);
    if (this instanceof ObjectSpec objectSpec) {
      return visitorType.cast(new BindObjectVisitor(objectSpec, config.newObjectBuilder()));
    }
    if (this instanceof ArraySpec arraySpec) {
      return visitorType.cast(new BindArrayVisitor(arraySpec, config.newArrayBuilder()));
    }
    if (this instanceof StreamSpec streamSpec) {
      return visitorType.cast(new BindStreamVisitor(streamSpec, config));
    }
    if (this instanceof ClassSpec classSpec) {
      return visitorType.cast(new BindClassVisitor(classSpec, config));
    }
    throw new AssertionError();
  }

  static Spec objectClass(String name, ClassInfo<?> classInfo) {
    requireNonNull(name);
    requireNonNull(classInfo);
    return new ClassSpec(name, classInfo);
  }
  static Spec valueClass(String name, Converter converter) {
    requireNonNull(name);
    return new ValueSpec(name, converter);
  }
  
  interface ClassInfo<B> {
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