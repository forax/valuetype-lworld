package fr.umlv.jsonapi.bind;

import static java.lang.invoke.MethodType.methodType;
import static java.util.Objects.requireNonNull;

import fr.umlv.jsonapi.ArrayBuilder;
import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.JsonReader;
import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.ObjectBuilder;
import fr.umlv.jsonapi.ObjectVisitor;
import java.io.IOException;
import java.io.Reader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class Binder {
  public /*sealed*/ interface Spec {
    default Spec array() { return new ArraySpec(this); }
    default Spec object() { return new ObjectSpec(this); }

    static Spec objectClass(String name, ClassInfo<?> classInfo) {
      return new ClassSpec(name, classInfo);
    }
    static Spec valueClass(String name, UnaryOperator<JsonValue> converter) {
      return new ValueSpec(name, converter);
    }
  }

  private record ValueSpec(String name, UnaryOperator<JsonValue> converter) implements Spec {
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
        return new BindClassVisitor(classSpec, arrayBuilder::add);
      }
      throw new IllegalStateException("invalid component for an object " + component);
    }

    ArrayVisitor newArrayFrom(ArrayBuilder arrayBuilder) {
      if (component instanceof ArraySpec arraySpec) {
        return new BindArrayVisitor(arraySpec, arrayBuilder.visitArray(), __ -> {});
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
        return new BindClassVisitor(classSpec, o -> objectBuilder.add(name, o));
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

  public interface ClassInfo<B> {
    Spec elementSpec(String name);

    B newBuilder();
    B addObject(B builder, String name, Object object);
    B addArray(B builder, String name, Object array);
    B addValue(B builder, String name, JsonValue value);
    Object build(B builder);
  }

  record ClassSpec(String name, ClassInfo<?>classInfo) implements Spec {
    @Override
    public String toString() {
      return name;
    }

    ObjectVisitor newMemberObject(String name, Consumer<Object> postOp) {
      var spec = classInfo.elementSpec(name);
      if (spec instanceof ObjectSpec objectSpec) {
        return new BindObjectVisitor(objectSpec, new ObjectBuilder(), postOp);
      }
      if (spec instanceof ClassSpec classSpec) {
        return new BindClassVisitor(classSpec, postOp);
      }
      throw new IllegalStateException("invalid component for an object " + spec + " for element " + name);
    }

    ArrayVisitor newMemberArray(String name, Consumer<Object> postOp) {
      var spec = classInfo.elementSpec(name);
      if (spec instanceof ArraySpec arraySpec) {
        return new BindArrayVisitor(arraySpec, new ArrayBuilder(), postOp);
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

  @FunctionalInterface
  public interface SpecFinder {
    Optional<Spec> findSpec(Class<?> type);

    default SpecFinder filter(Predicate<? super Class<?>> predicate) {
      return type -> {
        if (!predicate.test(type)) {
          return Optional.empty();
        }
        return findSpec(type);
      };
    }

    static SpecFinder from(Map<Class<?>, Spec> specMap) {
      return type -> Optional.ofNullable(specMap.get(type));
    }

    static SpecFinder recordFinder(Lookup lookup, Binder binder) {
      requireNonNull(lookup);
      requireNonNull(binder);
      return type -> {
        var components = type.getRecordComponents();
        if (components == null) {
          return Optional.empty();
        }
        int length = components.length;
        record RecordElement(int index, Spec spec) {}
        var constructorTypes = new Class<?>[length];
        var componentMap = new HashMap<String, RecordElement>();
        for(var i = 0; i < length; i++) {
          var component = components[i];
          constructorTypes[i] = component.getType();

          var componentType = component.getGenericType();
          var componentSpec = binder.findSpecOrThrow(componentType);
          componentMap.put(component.getName(), new RecordElement(i, componentSpec));
        }

        MethodHandle constructor;
        try {
          constructor = lookup.findConstructor(type, methodType(void.class, constructorTypes));
        } catch (NoSuchMethodException e) {
          throw (NoSuchMethodError) new NoSuchMethodError().initCause(e);
        } catch (IllegalAccessException e) {
          throw (IllegalAccessError) new IllegalAccessError().initCause(e);
        }
        return Optional.of(Spec.objectClass(type.getSimpleName(), new ClassInfo<Object[]>() {
          private RecordElement element(String name) {
            var recordElement = componentMap.get(name);
            if (recordElement == null) {
              throw new IllegalStateException("no element " + name + " for class " + type);
            }
            return recordElement;
          }

          @Override
          public Spec elementSpec(String name) {
            return element(name).spec;
          }

          @Override
          public Object[] newBuilder() {
            return new Object[length];
          }

          @Override
          public Object[] addObject(Object[] builder, String name, Object object) {
            builder[element(name).index] = object;
            return builder;
          }
          @Override
          public Object[] addArray(Object[] builder, String name, Object array) {
            builder[element(name).index] = array;
            return builder;
          }
          @Override
          public Object[] addValue(Object[] builder, String name, JsonValue value) {
            builder[element(name).index] = value.asObject();
            return builder;
          }

          @Override
          public Object build(Object[] builder) {
            try {
              return constructor.invokeWithArguments(builder);
            } catch(RuntimeException | Error e) {
              throw e;
            } catch (Throwable throwable) { // a record constructor can not throw a checked exception !
              throw new AssertionError(throwable);
            }
          }
        }));
      };
    }
  }

  private static final class NoSpecFoundException extends RuntimeException {
    private NoSpecFoundException() {
      super(null, null, false, false);
    }

    private static final NoSpecFoundException INSTANCE = new NoSpecFoundException();
  }

  private final ClassValue<Spec> specMap = new ClassValue<Spec>() {
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

  private Spec findSpecOrThrow(Type type) {
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
    } catch(NoSpecFoundException e) {
      return Optional.empty();
    }
  }


  public /*sealed*/ interface ArrayToken { }
  private record ArrayTokenEmpty() implements ArrayToken { }

  public /*sealed*/ interface ObjectToken {}
  private record ObjectTokenEmpty() implements ObjectToken { }

  public static final ArrayToken ARRAY = new ArrayTokenEmpty();
  public static final ObjectToken OBJECT = new ObjectTokenEmpty();


  public <T> T read(Reader reader, Class<T> type) throws IOException {
    requireNonNull(reader);
    requireNonNull(type);
    return type.cast(read(reader, findSpecOrThrow(type)));
  }
  @SuppressWarnings("unchecked")
  public <T> List<T> read(Reader reader, Class<T> type, ArrayToken _1) throws IOException {
    requireNonNull(reader);
    requireNonNull(type);
    return (List<T>) read(reader, findSpecOrThrow(type).array());
  }
  @SuppressWarnings("unchecked")
  public <T> Map<String, T> read(Reader reader, Class<T> type, ObjectToken _1) throws IOException {
    requireNonNull(reader);
    requireNonNull(type);
    return (Map<String, T>) read(reader, findSpecOrThrow(type).object());
  }
  public static Object read(Reader reader, Spec spec) throws IOException {
    requireNonNull(reader);
    requireNonNull(spec);
    if (spec instanceof ObjectSpec objectSpec) {
      var visitor = new BindObjectVisitor(objectSpec);
      return JsonReader.parse(reader, visitor);
    }
    if (spec instanceof ArraySpec arraySpec) {
      var visitor = new BindArrayVisitor(arraySpec);
      return JsonReader.parse(reader, visitor);
    }
    if (spec instanceof ClassSpec classSpec) {
      var visitor = new BindClassVisitor(classSpec);
      return JsonReader.parse(reader, visitor);
    }
    throw new AssertionError();
  }

  public <T> T read(String text, Class<T> type) {
    requireNonNull(text);
    requireNonNull(type);
    return type.cast(read(text, findSpecOrThrow(type)));
  }
  @SuppressWarnings("unchecked")
  public <T> List<T> read(String text, Class<T> type, ArrayToken _1) {
    requireNonNull(text);
    requireNonNull(type);
    return (List<T>) read(text, findSpecOrThrow(type).array());
  }
  @SuppressWarnings("unchecked")
  public <T> Map<String, T> read(String text, Class<T> type, ObjectToken _1) {
    requireNonNull(text);
    requireNonNull(type);
    return (Map<String, T>) read(text, findSpecOrThrow(type).object());
  }
  public static Object read(String text, Spec spec) {
    requireNonNull(text);
    requireNonNull(spec);
    if (spec instanceof ObjectSpec objectSpec) {
      var visitor = new BindObjectVisitor(objectSpec);
      return JsonReader.parse(text, visitor);
    }
    if (spec instanceof ArraySpec arraySpec) {
      var visitor = new BindArrayVisitor(arraySpec);
      return JsonReader.parse(text, visitor);
    }
    if (spec instanceof ClassSpec classSpec) {
      var visitor = new BindClassVisitor(classSpec);
      return JsonReader.parse(text, visitor);
    }
    throw new AssertionError();
  }

  public <T> T read(Path path, Class<T> type) throws IOException {
    requireNonNull(path);
    requireNonNull(type);
    return type.cast(read(path, findSpecOrThrow(type)));
  }
  @SuppressWarnings("unchecked")
  public <T> List<T> read(Path path, Class<T> type, ArrayToken _1) throws IOException {
    requireNonNull(path);
    requireNonNull(type);
    return (List<T>) read(path, findSpecOrThrow(type).array());
  }
  @SuppressWarnings("unchecked")
  public <T> Map<String, T> read(Path path, Class<T> type, ObjectToken _1) throws IOException {
    requireNonNull(path);
    requireNonNull(type);
    return (Map<String, T>) read(path, findSpecOrThrow(type).object());
  }
  public static Object read(Path path, Spec spec) throws IOException {
    requireNonNull(path);
    requireNonNull(spec);
    if (spec instanceof ObjectSpec objectSpec) {
      var visitor = new BindObjectVisitor(objectSpec);
      return JsonReader.parse(path, visitor);
    }
    if (spec instanceof ArraySpec arraySpec) {
      var visitor = new BindArrayVisitor(arraySpec);
      return JsonReader.parse(path, visitor);
    }
    if (spec instanceof ClassSpec classSpec) {
      var visitor = new BindClassVisitor(classSpec);
      return JsonReader.parse(path, visitor);
    }
    throw new AssertionError();
  }
}
