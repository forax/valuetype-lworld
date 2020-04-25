package fr.umlv.jsonapi.bind;

import static java.util.Objects.requireNonNull;

import fr.umlv.jsonapi.ArrayBuilder;
import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.BuilderConfig;
import fr.umlv.jsonapi.JsonReader;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectBuilder;
import fr.umlv.jsonapi.ObjectVisitor;

import java.io.IOException;
import java.io.Reader;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class Binder {
  record ValueSpec(String name, UnaryOperator<JsonValue> converter) implements Spec {
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

  @SuppressWarnings("serial")
  private static final class NoSpecFoundException extends RuntimeException {
    private NoSpecFoundException() {
      super(null, null, false, false);
    }

    private static final NoSpecFoundException INSTANCE = new NoSpecFoundException();
  }

  private final ClassValue<Spec> specMap = new ClassValue<>() {
    @Override
    protected Spec computeValue(Class<?> type) {
      if (type.isPrimitive() || type == Object.class || type == String.class
          || type == BigInteger.class || type == BigDecimal.class) {
        return Spec.valueClass(type.getName(), UnaryOperator.identity());
      }

      var finders = Binder.this.finders;
      for(var i = finders.size(); --i >= 0; ) {
        var optSpec = finders.get(i).findSpec(type);
        if (optSpec.isPresent()) {
          return optSpec.orElseThrow();
        }
      }
      throw NoSpecFoundException.INSTANCE;
    }
  };
  private final CopyOnWriteArrayList<SpecFinder> finders = new CopyOnWriteArrayList<>();

  private Binder() {
    // empty
  }

  public Binder(Lookup lookup) {
    requireNonNull(lookup);
    finders.add(SpecFinder.recordFinder(lookup, this));
  }

  public static Binder noDefaults() {
    return new Binder();
  }

  public Binder register(SpecFinder finder) {
    requireNonNull(finder);
    finders.add(finder);
    return this;
  }

  Spec findSpecOrThrow(Type type) {
    return findSpec(type).orElseThrow(() -> new IllegalStateException("no spec for type " + type.getTypeName() + " found"));
  }
  public Optional<Spec> findSpec(Type type) {
    requireNonNull(type);
    if (type instanceof Class<?> clazz) {
      return lookupSpec(clazz);
    }
    if (type instanceof ParameterizedType parameterizedType) {
      var rawType = parameterizedType.getRawType();
      if (rawType == Map.class) {
        //FIXME (first type argument should be a String)
        return findSpec(parameterizedType.getActualTypeArguments()[1]).map(Spec::object);
      }
      if (rawType == List.class) {
        return findSpec(parameterizedType.getActualTypeArguments()[0]).map(Spec::array);
      }
    }
    return Optional.empty();
  }
  private Optional<Spec> lookupSpec(Class<?> type) {
    try {
      return Optional.of(specMap.get(type));
    } catch(@SuppressWarnings("unused") NoSpecFoundException e) {
      return Optional.empty();
    }
  }

  static final BuilderConfig DEFAULT_CONFIG = new BuilderConfig();

  public /*sealed*/ interface ArrayToken { /* empty */ }
  private record ArrayTokenEmpty() implements ArrayToken { /* empty */ }

  public /*sealed*/ interface ObjectToken { /* empty */ }
  private record ObjectTokenEmpty() implements ObjectToken { /* empty */ }

  public static final ArrayToken ARRAY = new ArrayTokenEmpty();
  public static final ObjectToken OBJECT = new ObjectTokenEmpty();

  public <T> T read(Reader reader, Class<T> type) throws IOException {
    requireNonNull(reader);
    requireNonNull(type);
    return read(reader, type, DEFAULT_CONFIG);
  }
  public <T> T read(Reader reader, Class<T> type, BuilderConfig config) throws IOException {
    requireNonNull(reader);
    requireNonNull(type);
    return type.cast(read(reader, findSpecOrThrow(type), config));
  }
  @SuppressWarnings("unchecked")
  public <T> List<T> read(Reader reader, Class<T> type, @SuppressWarnings("unused") ArrayToken __) throws IOException {
    requireNonNull(reader);
    requireNonNull(type);
    return (List<T>) read(reader, findSpecOrThrow(type).array(), DEFAULT_CONFIG);
  }
  @SuppressWarnings("unchecked")
  public <T> Map<String, T> read(Reader reader, Class<T> type, @SuppressWarnings("unused") ObjectToken __) throws IOException {
    requireNonNull(reader);
    requireNonNull(type);
    return (Map<String, T>) read(reader, findSpecOrThrow(type).object(), DEFAULT_CONFIG);
  }
  public static Object read(Reader reader, Spec spec, BuilderConfig config) throws IOException {
    requireNonNull(reader);
    requireNonNull(spec);
    if (spec instanceof ObjectSpec objectSpec) {
      var visitor = new BindObjectVisitor(objectSpec, config.newObjectBuilder());
      return JsonReader.parse(reader, visitor);
    }
    if (spec instanceof ArraySpec arraySpec) {
      var visitor = new BindArrayVisitor(arraySpec, config.newArrayBuilder());
      return JsonReader.parse(reader, visitor);
    }
    if (spec instanceof ClassSpec classSpec) {
      var visitor = new BindClassVisitor(classSpec, config);
      return JsonReader.parse(reader, visitor);
    }
    throw new AssertionError();
  }

  public <T> T read(String text, Class<T> type) {
    requireNonNull(text);
    requireNonNull(type);
    return read(text, type, DEFAULT_CONFIG);
  }
  public <T> T read(String text, Class<T> type, BuilderConfig config) {
    requireNonNull(text);
    requireNonNull(type);
    return type.cast(read(text, findSpecOrThrow(type), config));
  }
  @SuppressWarnings("unchecked")
  public <T> List<T> read(String text, Class<T> type, @SuppressWarnings("unused") ArrayToken __) {
    requireNonNull(text);
    requireNonNull(type);
    return (List<T>) read(text, findSpecOrThrow(type).array(), DEFAULT_CONFIG);
  }
  @SuppressWarnings("unchecked")
  public <T> Map<String, T> read(String text, Class<T> type, @SuppressWarnings("unused") ObjectToken __) {
    requireNonNull(text);
    requireNonNull(type);
    return (Map<String, T>) read(text, findSpecOrThrow(type).object(), DEFAULT_CONFIG);
  }
  public static Object read(String text, Spec spec, BuilderConfig config) {
    requireNonNull(text);
    requireNonNull(spec);
    if (spec instanceof ObjectSpec objectSpec) {
      var visitor = new BindObjectVisitor(objectSpec, config.newObjectBuilder());
      return JsonReader.parse(text, visitor);
    }
    if (spec instanceof ArraySpec arraySpec) {
      var visitor = new BindArrayVisitor(arraySpec, config.newArrayBuilder());
      return JsonReader.parse(text, visitor);
    }
    if (spec instanceof ClassSpec classSpec) {
      var visitor = new BindClassVisitor(classSpec, config);
      return JsonReader.parse(text, visitor);
    }
    throw new AssertionError();
  }

  public <T> T read(Path path, Class<T> type) throws IOException {
    requireNonNull(path);
    requireNonNull(type);
    return read(path, type, DEFAULT_CONFIG);
  }
  public <T> T read(Path path, Class<T> type, BuilderConfig config) throws IOException {
    requireNonNull(path);
    requireNonNull(type);
    return type.cast(read(path, findSpecOrThrow(type), config));
  }
  @SuppressWarnings("unchecked")
  public <T> List<T> read(Path path, Class<T> type, @SuppressWarnings("unused") ArrayToken __) throws IOException {
    requireNonNull(path);
    requireNonNull(type);
    return (List<T>) read(path, findSpecOrThrow(type).array(), DEFAULT_CONFIG);
  }
  @SuppressWarnings("unchecked")
  public <T> Map<String, T> read(Path path, Class<T> type, @SuppressWarnings("unused") ObjectToken __) throws IOException {
    requireNonNull(path);
    requireNonNull(type);
    return (Map<String, T>) read(path, findSpecOrThrow(type).object(), DEFAULT_CONFIG);
  }
  public static Object read(Path path, Spec spec, BuilderConfig config) throws IOException {
    requireNonNull(path);
    requireNonNull(spec);
    if (spec instanceof ObjectSpec objectSpec) {
      var visitor = new BindObjectVisitor(objectSpec, config.newObjectBuilder());
      return JsonReader.parse(path, visitor);
    }
    if (spec instanceof ArraySpec arraySpec) {
      var visitor = new BindArrayVisitor(arraySpec, config.newArrayBuilder());
      return JsonReader.parse(path, visitor);
    }
    if (spec instanceof ClassSpec classSpec) {
      var visitor = new BindClassVisitor(classSpec, config);
      return JsonReader.parse(path, visitor);
    }
    throw new AssertionError();
  }
}
