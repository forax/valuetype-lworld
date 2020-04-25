package fr.umlv.jsonapi.bind;

import static java.util.Objects.requireNonNull;

import java.util.function.UnaryOperator;

import fr.umlv.jsonapi.BuilderConfig;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.bind.Binder.ArraySpec;
import fr.umlv.jsonapi.bind.Binder.ClassSpec;
import fr.umlv.jsonapi.bind.Binder.ObjectSpec;
import fr.umlv.jsonapi.bind.Binder.ValueSpec;

public /*sealed*/ interface Spec {
  default Spec array() { return new ArraySpec(this); }
  default Spec object() { return new ObjectSpec(this); }

  default <V> V createBindVisitor(Class<V> visitorType) {
    return createBindVisitor(visitorType, Binder.DEFAULT_CONFIG);
  }
  default <V> V createBindVisitor(Class<V> visitorType, BuilderConfig config) {
    requireNonNull(visitorType);
    requireNonNull(config);
    if (this instanceof ClassSpec classSpec) {
      return visitorType.cast(new BindClassVisitor(classSpec, config));
    }
    if (this instanceof ArraySpec arraySpec) {
      return visitorType.cast(new BindArrayVisitor(arraySpec, config.newArrayBuilder()));
    }
    if (this instanceof ObjectSpec objectSpec) {
      return visitorType.cast(new BindObjectVisitor(objectSpec, config.newObjectBuilder()));
    }
    throw new IllegalArgumentException("unknown type of visitor " + visitorType.getName());
  }

  static Spec objectClass(String name, ClassInfo<?> classInfo) {
    requireNonNull(name);
    requireNonNull(classInfo);
    return new ClassSpec(name, classInfo);
  }
  static Spec valueClass(String name, UnaryOperator<JsonValue> converter) {
    requireNonNull(name);
    requireNonNull(converter);
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
}