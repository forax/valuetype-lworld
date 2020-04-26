package fr.umlv.jsonapi.bind;

import fr.umlv.jsonapi.ArrayBuilder;
import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.BuilderConfig;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectBuilder;
import fr.umlv.jsonapi.ObjectVisitor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

final class Specs {
  private Specs() {
    throw new AssertionError();
  }

  record ValueSpec(String name, UnaryOperator<JsonValue>converter) implements Spec {
    @Override
    public String toString() {
      return name;
    }

    private JsonValue convert(JsonValue value) {
      return converter.apply(value);
    }
  }

  record ArraySpec(Spec component) implements Spec {
    @Override
    public String toString() {
      return component + ".array()";
    }

    ObjectVisitor newObjectFrom(ArrayBuilder arrayBuilder) {
      if (component instanceof ObjectSpec objectSpec) {
        return new BindObjectVisitor(objectSpec, arrayBuilder.visitObject());
      }
      if (component instanceof ClassSpec classSpec) {
        return new BindClassVisitor(classSpec, BuilderConfig.from(arrayBuilder), arrayBuilder::add);
      }
      throw new IllegalStateException("invalid component for an object " + component);
    }

    ArrayVisitor newArrayFrom(ArrayBuilder arrayBuilder) {
      if (component instanceof ArraySpec arraySpec) {
        return new BindArrayVisitor(arraySpec, arrayBuilder.visitArray());
      }
      if (component instanceof StreamSpec streamSpec) {
        return new BindStreamVisitor(streamSpec, BuilderConfig.from(arrayBuilder), arrayBuilder::add);
      }
      throw new IllegalStateException("invalid component for an array " + component);
    }
  }
  record StreamSpec(Spec component, Function<? super Stream<Object>, ?>aggregator) implements Spec {
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
      throw new IllegalStateException("invalid component for an object " + component);
    }

    ArrayVisitor newArrayFrom(BuilderConfig config) {
      if (component instanceof ArraySpec arraySpec) {
        return new BindArrayVisitor(arraySpec, config.newArrayBuilder());
      }
      if (component instanceof StreamSpec streamSpec) {
        return new BindStreamVisitor(streamSpec, config);
      }
      throw new IllegalStateException("invalid component for an array " + component);
    }
  }
  record ObjectSpec(Spec component) implements Spec {
    @Override
    public String toString() {
      return component + ".object()";
    }

    ObjectVisitor newMemberObjectFrom(String name, ObjectBuilder objectBuilder) {
      if (component instanceof ObjectSpec objectSpec) {
        return new BindObjectVisitor(objectSpec, objectBuilder.visitMemberObject(name));
      }
      if (component instanceof ClassSpec classSpec) {
        return new BindClassVisitor(classSpec, BuilderConfig.from(objectBuilder), o -> objectBuilder.add(name, o));
      }
      throw new IllegalStateException("invalid component for an object " + component);
    }

    ArrayVisitor newMemberArrayFrom(String name, ObjectBuilder objectBuilder) {
      if (component instanceof ArraySpec arraySpec) {
        return new BindArrayVisitor(arraySpec, objectBuilder.visitMemberArray(name));
      }
      if (component instanceof StreamSpec streamSpec) {
        return new BindStreamVisitor(streamSpec, BuilderConfig.from(objectBuilder), o -> objectBuilder.add(name, o));
      }
      throw new IllegalStateException("invalid component for an array " + component);
    }
  }

  record ClassSpec(String name, ClassInfo<?>classInfo) implements Spec {
    @Override
    public String toString() {
      return name;
    }

    ObjectVisitor newMemberObject(String name, BuilderConfig config, Consumer<Object> postOp) {
      var spec = classInfo.elementSpec(name);
      if (spec instanceof ObjectSpec objectSpec) {
        return new BindObjectVisitor(objectSpec, config.newObjectBuilder(), postOp);
      }
      if (spec instanceof ClassSpec classSpec) {
        return new BindClassVisitor(classSpec, config, postOp);
      }
      throw new IllegalStateException("invalid component for an object " + spec + " for element " + name);
    }

    ArrayVisitor newMemberArray(String name, BuilderConfig config, Consumer<Object> postOp) {
      var spec = classInfo.elementSpec(name);
      if (spec instanceof ArraySpec arraySpec) {
        return new BindArrayVisitor(arraySpec, config.newArrayBuilder(), postOp);
      }
      if (spec instanceof StreamSpec streamSpec) {
        return new BindStreamVisitor(streamSpec, config, postOp);
      }
      throw new IllegalStateException("invalid component for an array " + spec + " for element " + name);
    }
  }

  static JsonValue convert(ArraySpec spec, JsonValue value) {
    var elementSpec = spec.component;
    if (elementSpec instanceof ValueSpec valueSpec) {
      return valueSpec.convert(value);
    }
    throw new IllegalStateException(spec + " can not convert " + value + " to " + elementSpec);
  }
  static JsonValue convert(StreamSpec spec, JsonValue value) {
    var elementSpec = spec.component;
    if (elementSpec instanceof ValueSpec valueSpec) {
      return valueSpec.convert(value);
    }
    throw new IllegalStateException(spec + " can not convert " + value + " to " + elementSpec);
  }
  static JsonValue convert(ObjectSpec spec, String name, JsonValue value) {
    var elementSpec = spec.component;
    if (elementSpec instanceof ValueSpec valueSpec) {
      return valueSpec.convert(value);
    }
    throw new IllegalStateException(spec + "." + name + " can not convert " + value + " to " + elementSpec);
  }

  static JsonValue convert(ClassSpec spec, String name, JsonValue value) {
    var elementSpec = spec.classInfo.elementSpec(name);
    if (elementSpec instanceof ValueSpec valueSpec) {
      return valueSpec.convert(value);
    }
    throw new IllegalStateException(spec + "." + name + " can not convert " + value + " to " + elementSpec);
  }
}
