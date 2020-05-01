package fr.umlv.jsonapi.bind;

import static java.util.Objects.requireNonNull;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.bind.Binder.BindingException;
import fr.umlv.jsonapi.builder.ArrayBuilder;
import fr.umlv.jsonapi.builder.BuilderConfig;
import fr.umlv.jsonapi.builder.ObjectBuilder;

import fr.umlv.jsonapi.internal.RootVisitor;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

final class Specs {
  private Specs() {
    throw new AssertionError();
  }

  record ValueSpec(String name, Converter converter) implements Spec {
    @Override
    public String toString() {
      return name;
    }

    private void acceptValue(Object value, ArrayVisitor visitor) {
      var jsonValue = JsonValue.fromAny(value);
      visitor.visitValue(convertFrom(jsonValue));
    }
    private void acceptMember(String name, Object value, ObjectVisitor visitor) {
      var jsonValue = JsonValue.fromAny(value);
      visitor.visitMemberValue(name, convertFrom(jsonValue));
    }

    private JsonValue convertFrom(JsonValue value) {
      if (converter == null) {
        return value;
      }
      return converter.convertFrom(value);
    }

    private JsonValue convertTo(JsonValue jsonValue) {
      if (converter == null) {
        return jsonValue;
      }
      return converter.convertTo(jsonValue);
    }

    public Spec convertWith(Converter converter) {
      var currentConverter = this.converter;
      var newConverter = (currentConverter == null)?
          converter:
          new Converter() {
            @Override
            public JsonValue convertTo(JsonValue value) {
              return converter.convertTo(currentConverter.convertTo(value));
            }
            @Override
            public JsonValue convertFrom(JsonValue object) {
              return currentConverter.convertFrom(converter.convertFrom(object));
            }
          };
      return new ValueSpec(name + ".convert()", newConverter);
    }
  }

  record ArraySpec(Spec component) implements Spec {
    @Override
    public String toString() {
      return component + ".array()";
    }

    ObjectVisitor newObjectFrom(ArrayBuilder arrayBuilder) {
      if (component instanceof ObjectSpec objectSpec) {
        return new BindObjectVisitor(objectSpec, (ObjectBuilder) arrayBuilder.visitObject());
      }
      if (component instanceof ClassSpec classSpec) {
        return new BindClassVisitor(classSpec, BuilderConfig.extract(arrayBuilder), arrayBuilder::add);
      }
      throw new BindingException("invalid component spec for an object " + component);
    }
    ArrayVisitor newArrayFrom(ArrayBuilder arrayBuilder) {
      if (component instanceof ArraySpec arraySpec) {
        return new BindArrayVisitor(arraySpec, (ArrayBuilder) arrayBuilder.visitArray());
      }
      if (component instanceof StreamSpec streamSpec) {
        return new BindStreamVisitor(streamSpec, BuilderConfig.extract(arrayBuilder), arrayBuilder::add);
      }
      throw new BindingException("invalid component spec for an array " + component);
    }
  }
  record StreamSpec(Spec component, Function<? super Stream<Object>, ?> aggregator) implements Spec {
    @Override
    public String toString() {
      return component + ".stream(aggregator)";
    }

    ObjectVisitor newObjectFrom(BuilderConfig config) {
      if (component instanceof ObjectSpec objectSpec) {
        return new BindObjectVisitor(objectSpec, config.newObjectBuilder());
      }
      if (component instanceof ClassSpec classSpec) {
        return new BindClassVisitor(classSpec, config);
      }
      throw new BindingException("invalid component spec for an object " + component);
    }
    ArrayVisitor newArrayFrom(BuilderConfig config) {
      if (component instanceof ArraySpec arraySpec) {
        return new BindArrayVisitor(arraySpec, config.newArrayBuilder());
      }
      if (component instanceof StreamSpec streamSpec) {
        return new BindStreamVisitor(streamSpec, config);
      }
      throw new BindingException("invalid component spec for an array " + component);
    }
  }
  record ObjectSpec(Spec component, Predicate<? super String> filter) implements Spec {
    @Override
    public String toString() {
      return component + ".object()" + (filter == null? "": ".filter()");
    }

    ObjectVisitor newMemberObjectFrom(String name, ObjectBuilder objectBuilder) {
      if (component instanceof ObjectSpec objectSpec) {
        return new BindObjectVisitor(objectSpec, (ObjectBuilder) objectBuilder.visitMemberObject(name));
      }
      if (component instanceof ClassSpec classSpec) {
        return new BindClassVisitor(classSpec, BuilderConfig.extract(objectBuilder), o -> objectBuilder.add(name, o));
      }
      throw new BindingException("invalid component spec for an object " + component);
    }
    ArrayVisitor newMemberArrayFrom(String name, ObjectBuilder objectBuilder) {
      if (component instanceof ArraySpec arraySpec) {
        return new BindArrayVisitor(arraySpec, (ArrayBuilder) objectBuilder.visitMemberArray(name));
      }
      if (component instanceof StreamSpec streamSpec) {
        return new BindStreamVisitor(streamSpec, BuilderConfig.extract(objectBuilder), o -> objectBuilder.add(name, o));
      }
      throw new BindingException("invalid component spec for an array " + component);
    }

    public ObjectSpec filterWith(Predicate<? super String> predicate) {
      var filter = this.filter;
      Predicate<? super String> newFilter = (filter == null)? predicate: name -> predicate.test(name) && filter.test(name);
      return new ObjectSpec(component, newFilter);
    }
  }

  record ClassSpec(String name, Predicate<? super String>filter, ObjectLayout<?>objectLayout) implements Spec {
    @Override
    public String toString() {
      return name;
    }

    ObjectVisitor newMemberObject(String name, BuilderConfig config, Consumer<Object> postOp) {
      var spec = objectLayout.memberSpec(name);
      if (spec instanceof ObjectSpec objectSpec) {
        return new BindObjectVisitor(objectSpec, config.newObjectBuilder(), postOp);
      }
      if (spec instanceof ClassSpec classSpec) {
        return new BindClassVisitor(classSpec, config, postOp);
      }
      throw new BindingException("invalid component spec for an object " + spec + " for element " + name);
    }
    ArrayVisitor newMemberArray(String name, BuilderConfig config, Consumer<Object> postOp) {
      var spec = objectLayout.memberSpec(name);
      if (spec instanceof ArraySpec arraySpec) {
        return new BindArrayVisitor(arraySpec, config.newArrayBuilder(), postOp);
      }
      if (spec instanceof StreamSpec streamSpec) {
        return new BindStreamVisitor(streamSpec, config, postOp);
      }
      throw new BindingException("invalid component spec for an array " + spec + " for element " + name);
    }

    Object accept(Object value, Binder binder, ObjectVisitor objectVisitor) {
      objectVisitor.visitStartObject();
      objectLayout.accept(value, (name, elementValue) -> acceptMember(name, elementValue, binder, objectVisitor));
      return objectVisitor.visitEndObject();
    }

    public ClassSpec filterWith(Predicate<? super String> predicate) {
       var filter = this.filter;
       Predicate<? super String> newFilter = (filter == null)? predicate: name -> predicate.test(name) && filter.test(name);
       return new ClassSpec(name + ".filter()", newFilter, objectLayout);
    }
  }

  static JsonValue convert(ArraySpec spec, JsonValue value) {
    var elementSpec = spec.component;
    if (elementSpec instanceof ValueSpec valueSpec) {
      return valueSpec.convertTo(value);
    }
    throw new BindingException(spec + " can not convert " + value + " to " + elementSpec);
  }
  static JsonValue convert(StreamSpec spec, JsonValue value) {
    var elementSpec = spec.component;
    if (elementSpec instanceof ValueSpec valueSpec) {
      return valueSpec.convertTo(value);
    }
    throw new BindingException(spec + " can not convert " + value + " to " + elementSpec);
  }
  static JsonValue convert(ObjectSpec spec, String name, JsonValue value) {
    var elementSpec = spec.component;
    if (elementSpec instanceof ValueSpec valueSpec) {
      return valueSpec.convertTo(value);
    }
    throw new BindingException(spec + "." + name + " can not convert " + value + " to " + elementSpec);
  }

  static JsonValue convert(ClassSpec spec, String name, JsonValue value) {
    var elementSpec = spec.objectLayout.memberSpec(name);
    if (elementSpec instanceof ValueSpec valueSpec) {
      return valueSpec.convertTo(value);
    }
    throw new BindingException(spec + "." + name + " can not convert " + value + " to " + elementSpec);
  }


  static Object acceptRoot(Object value, Binder binder, RootVisitor visitor) {
    requireNonNull(value);  // help the JIT :)
    if (value instanceof Iterable<?> iterable) {
      var arrayVisitor = visitor.visitArray();
      if (arrayVisitor == null) {
        return null;
      }
      return acceptIterable(iterable, binder, arrayVisitor);
    }
    if (value instanceof Iterator<?> iterator) {
      var arrayVisitor = visitor.visitArray();
      if (arrayVisitor == null) {
        return null;
      }
      return acceptIterator(iterator, binder, arrayVisitor);
    }
    if (value instanceof Stream<?> stream) {
      var arrayVisitor = visitor.visitArray();
      if (arrayVisitor == null) {
        return null;
      }
      return acceptStream(stream, binder, arrayVisitor);
    }
    if (value instanceof Map<?, ?> map) {
      var objectVisitor = visitor.visitObject();
      if (objectVisitor == null) {
        return null;
      }
      return acceptMap(map, binder, objectVisitor);
    }
    var spec = binder.spec(value.getClass());
    if (spec instanceof ClassSpec classSpec) {
      var objectVisitor = visitor.visitObject();
      if (objectVisitor == null) {
        return null;
      }
      return classSpec.accept(value, binder, objectVisitor);
    }
    throw new BindingException("can not accept " + value + " of spec " + spec);
  }

  static void acceptValue(Object value, Binder binder, ArrayVisitor visitor) {
    if (value == null) {
      visitor.visitValue(JsonValue.nullValue());
      return;
    }
    if (value instanceof Iterable<?> list) {
      var arrayVisitor = visitor.visitArray();
      if (arrayVisitor != null) {
        acceptIterable(list, binder, arrayVisitor);
      }
      return;
    }
    if (value instanceof Iterator<?> iterator) {
      var arrayVisitor = visitor.visitArray();
      if (arrayVisitor != null) {
        acceptIterator(iterator, binder, arrayVisitor);
      }
      return;
    }
    if (value instanceof Stream<?> stream) {
      var arrayVisitor = visitor.visitArray();
      if (arrayVisitor != null) {
        acceptStream(stream, binder, arrayVisitor);
      }
      return;
    }
    if (value instanceof Map<?,?> map) {
      var objectVisitor = visitor.visitObject();
      if (objectVisitor != null) {
        acceptMap(map, binder, objectVisitor);
      }
      return;
    }
    var spec = binder.spec(value.getClass());
    if (spec instanceof ClassSpec classSpec) {
      var objectVisitor = visitor.visitObject();
      if (objectVisitor != null) {
        classSpec.accept(value, binder, objectVisitor);
      }
      return;
    }
    if (spec instanceof ValueSpec valueSpec) {
      valueSpec.acceptValue(value, visitor);
      return;
    }
    visitor.visitValue(JsonValue.fromAny(value));
  }

  static void acceptMember(String name, Object value, Binder binder, ObjectVisitor visitor) {
    if (value == null) {
      visitor.visitMemberValue(name, JsonValue.nullValue());
      return;
    }
    if (value instanceof Iterable<?> list) {
      var arrayVisitor = visitor.visitMemberArray(name);
      if (arrayVisitor != null) {
        acceptIterable(list, binder, arrayVisitor);
      }
      return;
    }
    if (value instanceof Iterator<?> iterator) {
      var arrayVisitor = visitor.visitMemberArray(name);
      if (arrayVisitor != null) {
        acceptIterator(iterator, binder, arrayVisitor);
      }
      return;
    }
    if (value instanceof Stream<?> stream) {
      var arrayVisitor = visitor.visitMemberArray(name);
      if (arrayVisitor != null) {
        acceptStream(stream, binder, arrayVisitor);
      }
      return;
    }
    if (value instanceof Map<?,?> map) {
      var objectVisitor = visitor.visitMemberObject(name);
      if (objectVisitor != null) {
        acceptMap(map, binder, objectVisitor);
      }
      return;
    }
    var spec = binder.spec(value.getClass());
    if (spec instanceof ClassSpec classSpec) {
      var objectVisitor = visitor.visitMemberObject(name);
      if (objectVisitor != null) {
        classSpec.accept(value, binder, objectVisitor);
      }
      return;
    }
    if (spec instanceof ValueSpec valueSpec) {
      valueSpec.acceptMember(name, value, visitor);
      return;
    }
    visitor.visitMemberValue(name, JsonValue.fromAny(value));
  }

  private static Object acceptIterable(Iterable<?> iterable, Binder binder, ArrayVisitor arrayVisitor) {
    arrayVisitor.visitStartArray();
    for(var item: iterable) {
      acceptValue(item, binder, arrayVisitor);
    }
    return arrayVisitor.visitEndArray();
  }

  private static Object acceptIterator(Iterator<?> iterator, Binder binder, ArrayVisitor arrayVisitor) {
    arrayVisitor.visitStartArray();
    while(iterator.hasNext()) {
      acceptValue(iterator.next(), binder, arrayVisitor);
    }
    return arrayVisitor.visitEndArray();
  }

  private static Object acceptStream(Stream<?> stream, Binder binder, ArrayVisitor arrayVisitor) {
    arrayVisitor.visitStartArray();
    stream.forEach(item -> acceptValue(item, binder, arrayVisitor));
    return arrayVisitor.visitEndArray();
  }

  private static Object acceptMap(Map<?,?> map, Binder binder, ObjectVisitor objectVisitor) {
    objectVisitor.visitStartObject();
    map.forEach((key, value) -> {
      var name = key.toString();
      acceptMember(name, value, binder, objectVisitor);
    });
    return objectVisitor.visitEndObject();
  }
}
