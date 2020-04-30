package fr.umlv.jsonapi.bind;

import static java.util.Objects.requireNonNull;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.BuilderConfig;
import fr.umlv.jsonapi.JsonReader;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectVisitor;
import fr.umlv.jsonapi.VisitorMode;
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
import java.util.stream.Stream;

public final class Binder {
  public static final class BindingException extends RuntimeException {
    public BindingException(String message) {
      super(message);
    }
    public BindingException(String message, Throwable cause) {
      super(message, cause);
    }
    public BindingException(Throwable cause) {
      super(cause);
    }
  }

  private final ClassValue<Spec> specMap = new ClassValue<>() {
    @Override
    protected Spec computeValue(Class<?> type) {
      var spec = lookup(type, Binder.this.finders);
      if (spec != null) {
        return spec;
      }
      throw new BindingException("no finder can resolve type " + type.getName());
    }
  };
  private final CopyOnWriteArrayList<SpecFinder> finders = new CopyOnWriteArrayList<>();

  private Binder() {
    // empty
  }

  public Binder(Lookup lookup) {
    requireNonNull(lookup);
    finders.add(newRecordSpecFinder(lookup));
  }

  public static Binder noDefaults() {
    return new Binder();
  }

  public SpecFinder newRecordSpecFinder(Lookup lookup) {
    requireNonNull(lookup);
    return SpecFinders.newRecordFinder(lookup, this);
  }

  public Binder register(SpecFinder finder) {
    requireNonNull(finder);
    finders.add(finder);
    return this;
  }

  private static Spec lookup(Class<?> type, CopyOnWriteArrayList<SpecFinder> finders) {
    if (type == boolean.class || type == int.class || type == long.class || type == double.class
        || type == String.class || type == BigInteger.class || type == BigDecimal.class
        || type == Object.class) {
      return Spec.valueClass(type.getName(), null);
    }
    for(var i = finders.size(); --i >= 0; ) {
      var optSpec = finders.get(i).findSpec(type);
      if (optSpec.isPresent()) {
        return optSpec.orElseThrow();
      }
    }
    return null;
  }

  public SpecFinder specFinder() {
    var finders = this.finders;
    return type -> Optional.ofNullable(lookup(type, finders));
  }

  public Spec spec(Type type) throws BindingException {
    requireNonNull(type);
    if (type instanceof Class<?> clazz) {
      return specForClass(clazz);
    }
    if (type instanceof ParameterizedType parameterizedType) {
      var rawType = parameterizedType.getRawType();
      var actualTypeArguments = parameterizedType.getActualTypeArguments();
      if (rawType == Map.class) {
        if (actualTypeArguments[0] != String.class) {  //FIXME wildcard ?
          throw new BindingException("can not decode " + type.getTypeName());
        }
        return spec(actualTypeArguments[1]).object();
      }
      if (rawType == List.class) {
        return spec(actualTypeArguments[0]).array();
      }
    }
    throw new BindingException("can not decode unknown type " + type.getTypeName());
  }
  private Spec specForClass(Class<?> type) {
    return specMap.get(type);
  }

  static final BuilderConfig DEFAULT_CONFIG = BuilderConfig.defaults();

  public interface ArrayToken { /* empty */ }
  public interface ObjectToken { /* empty */ }

  public static final ObjectToken IN_OBJECT = null;
  public static final ArrayToken IN_ARRAY = null;


  public <T> T read(Reader reader, Class<? extends T> type) throws IOException {
    requireNonNull(reader);
    requireNonNull(type);
    return read(reader, type, DEFAULT_CONFIG);
  }
  public <T> T read(Reader reader, Class<? extends T> type, BuilderConfig config) throws IOException {
    requireNonNull(reader);
    requireNonNull(type);
    return type.cast(read(reader, specForClass(type), config));
  }
  @SuppressWarnings("unchecked")
  public <T> List<T> read(Reader reader, Class<? extends T> type, @SuppressWarnings("unused") ArrayToken __) throws IOException {
    requireNonNull(reader);
    requireNonNull(type);
    return (List<T>) read(reader, specForClass(type).array(), DEFAULT_CONFIG);
  }
  @SuppressWarnings("unchecked")
  public <T> Map<String, T> read(Reader reader, Class<? extends T> type, @SuppressWarnings("unused") ObjectToken __) throws IOException {
    requireNonNull(reader);
    requireNonNull(type);
    return (Map<String, T>) read(reader, specForClass(type).object(), DEFAULT_CONFIG);
  }
  public static Object read(Reader reader, Spec spec, BuilderConfig config) throws IOException {
    requireNonNull(reader);
    requireNonNull(spec);
    requireNonNull(config);
    return JsonReader.parse(reader, spec.createBindVisitor(Object.class, config, null));
  }

  public <T> T read(String text, Class<? extends T> type) {
    requireNonNull(text);
    requireNonNull(type);
    return read(text, type, DEFAULT_CONFIG);
  }
  public <T> T read(String text, Class<? extends T> type, BuilderConfig config) {
    requireNonNull(text);
    requireNonNull(type);
    return type.cast(read(text, specForClass(type), config));
  }
  @SuppressWarnings("unchecked")
  public <T> List<T> read(String text, Class<? extends T> type, @SuppressWarnings("unused") ArrayToken __) {
    requireNonNull(text);
    requireNonNull(type);
    return (List<T>) read(text, specForClass(type).array(), DEFAULT_CONFIG);
  }
  @SuppressWarnings("unchecked")
  public <T> Map<String, T> read(String text, Class<? extends T> type, @SuppressWarnings("unused") ObjectToken __) {
    requireNonNull(text);
    requireNonNull(type);
    return (Map<String, T>) read(text, specForClass(type).object(), DEFAULT_CONFIG);
  }
  public static Object read(String text, Spec spec, BuilderConfig config) {
    requireNonNull(text);
    requireNonNull(spec);
    requireNonNull(config);
    return JsonReader.parse(text, spec.createBindVisitor(Object.class, config, null));
  }

  public <T> T read(Path path, Class<? extends T> type) throws IOException {
    requireNonNull(path);
    requireNonNull(type);
    return read(path, type, DEFAULT_CONFIG);
  }
  public <T> T read(Path path, Class<? extends T> type, BuilderConfig config) throws IOException {
    requireNonNull(path);
    requireNonNull(type);
    return type.cast(read(path, specForClass(type), config));
  }
  @SuppressWarnings("unchecked")
  public <T> List<T> read(Path path, Class<? extends T> type, @SuppressWarnings("unused") ArrayToken __) throws IOException {
    requireNonNull(path);
    requireNonNull(type);
    return (List<T>) read(path, specForClass(type).array(), DEFAULT_CONFIG);
  }
  @SuppressWarnings("unchecked")
  public <T> Map<String, T> read(Path path, Class<? extends T> type, @SuppressWarnings("unused") ObjectToken __) throws IOException {
    requireNonNull(path);
    requireNonNull(type);
    return (Map<String, T>) read(path, specForClass(type).object(), DEFAULT_CONFIG);
  }
  public static Object read(Path path, Spec spec, BuilderConfig config) throws IOException {
    requireNonNull(path);
    requireNonNull(spec);
    requireNonNull(config);
    return JsonReader.parse(path, spec.createBindVisitor(Object.class, config, null));
  }


  public <T> Stream<T> stream(Path path, Class<? extends T> type) throws IOException {
    requireNonNull(path);
    requireNonNull(type);
    return stream(path, type, DEFAULT_CONFIG);
  }
  public <T> Stream<T> stream(Path path, Class<? extends T> type, BuilderConfig config) throws IOException {
    requireNonNull(path);
    requireNonNull(type);
    requireNonNull(config);
    return stream(path, specForClass(type), config).map(type::cast);
  }
  public static Stream<Object> stream(Path path, Spec spec, BuilderConfig config) throws IOException {
    requireNonNull(path);
    requireNonNull(spec);
    requireNonNull(config);
    return JsonReader.stream(path, arrayForStreamVisitor(spec, config));
  }
  public <T> Stream<T> stream(String text, Class<? extends T> type) {
    requireNonNull(text);
    requireNonNull(type);
    return stream(text, type, DEFAULT_CONFIG);
  }
  public <T> Stream<T> stream(String text, Class<? extends T> type, BuilderConfig config)  {
    requireNonNull(text);
    requireNonNull(type);
    requireNonNull(config);
    return stream(text, specForClass(type), config).map(type::cast);
  }
  public static Stream<Object> stream(String text, Spec spec, BuilderConfig config) {
    requireNonNull(text);
    requireNonNull(spec);
    requireNonNull(config);
    return JsonReader.stream(text, arrayForStreamVisitor(spec, config));
  }
  public <T> Stream<T> stream(Reader reader, Class<? extends T> type) throws IOException {
    requireNonNull(reader);
    requireNonNull(type);
    return stream(reader, type, DEFAULT_CONFIG);
  }
  public <T> Stream<T> stream(Reader reader, Class<? extends T> type, BuilderConfig config) throws IOException {
    requireNonNull(reader);
    requireNonNull(type);
    requireNonNull(config);
    return stream(reader, specForClass(type), config).map(type::cast);
  }
  public static Stream<Object> stream(Reader reader, Spec spec, BuilderConfig config) throws IOException {
    requireNonNull(reader);
    requireNonNull(spec);
    requireNonNull(config);
    return JsonReader.stream(reader, arrayForStreamVisitor(spec, config));
  }

  private static ArrayVisitor arrayForStreamVisitor(Spec spec, BuilderConfig config) {
    return new ArrayVisitor() {
      @Override
      public VisitorMode mode() {
        return VisitorMode.PULL;
      }
      @Override
      public ObjectVisitor visitObject() {
        return spec.createBindVisitor(ObjectVisitor.class, config, null);
      }
      @Override
      public ArrayVisitor visitArray() {
        return spec.createBindVisitor(ArrayVisitor.class, config, null);
      }
      @Override
      public Object visitValue(JsonValue value) {
        return value.asObject();
      }
      @Override
      public Object visitEndArray() {
        return null;
      }
    };
  }
}
