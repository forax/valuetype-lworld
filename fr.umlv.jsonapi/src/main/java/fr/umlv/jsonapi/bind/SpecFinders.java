package fr.umlv.jsonapi.bind;

import static java.lang.invoke.MethodType.methodType;

import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.bind.Spec.ClassInfo;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.HashMap;
import java.util.Optional;

final class SpecFinders {
  private SpecFinders() {
    throw new AssertionError();
  }

  static SpecFinder newRecordFinder(Lookup lookup, Binder binder) {
    return type -> {
      var components = type.getRecordComponents();
      if (components == null) {
        return Optional.empty();
      }
      var length = components.length;
      record RecordElement(int index, Spec spec) { /* empty */ }
      var constructorTypes = new Class<?>[length];
      var componentMap = new HashMap<String, RecordElement>();
      for(var i = 0; i < length; i++) {
        var component = components[i];
        constructorTypes[i] = component.getType();

        var componentType = component.getGenericType();
        var componentSpec = binder.spec(componentType);
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
      return Optional.of(Spec.typedClass(type.getSimpleName(), new ClassInfo<Object[]>() {
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
