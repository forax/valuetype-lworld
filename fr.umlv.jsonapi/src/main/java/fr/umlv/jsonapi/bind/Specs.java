package fr.umlv.jsonapi.bind;

import fr.umlv.jsonapi.ArrayBuilder;
import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.BuilderConfig;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectBuilder;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.bind.Binder.BindingException;
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

  record ClassSpec(String name, Predicate<? super String>filter, ClassLayout<?> classLayout) implements Spec {
    @Override
    public String toString() {
      return name;
    }

    ObjectVisitor newMemberObject(String name, BuilderConfig config, Consumer<Object> postOp) {
      var spec = classLayout.elementSpec(name);
      if (spec instanceof ObjectSpec objectSpec) {
        return new BindObjectVisitor(objectSpec, config.newObjectBuilder(), postOp);
      }
      if (spec instanceof ClassSpec classSpec) {
        return new BindClassVisitor(classSpec, config, postOp);
      }
      throw new BindingException("invalid component spec for an object " + spec + " for element " + name);
    }

    ArrayVisitor newMemberArray(String name, BuilderConfig config, Consumer<Object> postOp) {
      var spec = classLayout.elementSpec(name);
      if (spec instanceof ArraySpec arraySpec) {
        return new BindArrayVisitor(arraySpec, config.newArrayBuilder(), postOp);
      }
      if (spec instanceof StreamSpec streamSpec) {
        return new BindStreamVisitor(streamSpec, config, postOp);
      }
      throw new BindingException("invalid component spec for an array " + spec + " for element " + name);
    }

    public ClassSpec filterWith(Predicate<? super String> predicate) {
       var filter = this.filter;
       Predicate<? super String> newFilter = (filter == null)? predicate: name -> predicate.test(name) && filter.test(name);
       return new ClassSpec(name + ".filter()", newFilter, classLayout);
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
    var elementSpec = spec.classLayout.elementSpec(name);
    if (elementSpec instanceof ValueSpec valueSpec) {
      return valueSpec.convertTo(value);
    }
    throw new BindingException(spec + "." + name + " can not convert " + value + " to " + elementSpec);
  }
}
