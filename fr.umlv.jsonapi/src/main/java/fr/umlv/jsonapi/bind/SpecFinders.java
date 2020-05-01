package fr.umlv.jsonapi.bind;

import static java.lang.invoke.MethodType.methodType;

import fr.umlv.jsonapi.JsonValue;
import fr.umlv.jsonapi.bind.Spec.ObjectLayout;
import fr.umlv.jsonapi.bind.Spec.Converter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Function;

final class SpecFinders {
  private SpecFinders() {
    throw new AssertionError();
  }

  static SpecFinder newRecordFinder(Lookup lookup, Function<? super Type, ? extends Spec> downwardFinder) {
    return type -> {
      var components = type.getRecordComponents();
      if (components == null) {
        return Optional.empty();
      }
      var length = components.length;
      record RecordElement(int index, Spec spec) {  }
      record RecordAccessor(String name, MethodHandle accessor) { }
      var constructorTypes = new Class<?>[length];
      var accessors = new RecordAccessor[length];
      var componentMap = new HashMap<String, RecordElement>();
      for(var i = 0; i < length; i++) {
        var component = components[i];
        constructorTypes[i] = component.getType();
        var componentName = component.getName();

        // record element for deserialization
        var componentType = component.getGenericType();
        var componentSpec = downwardFinder.apply(componentType);
        componentMap.put(componentName, new RecordElement(i, componentSpec));

        // record accessor for serialization
        MethodHandle accessor;
        try {
          accessor = lookup.unreflect(component.getAccessor());
        } catch (IllegalAccessException e) {
          throw new Binder.BindingException(e);
        }
        accessors[i] = new RecordAccessor(componentName, accessor);
      }

      MethodHandle constructor;
      try {
        constructor = lookup.findConstructor(type, methodType(void.class, constructorTypes));
      } catch (NoSuchMethodException | IllegalAccessException e) {
        throw new Binder.BindingException(e);
      }
      return Optional.of(Spec.newTypedObject(type.getSimpleName(), new ObjectLayout<Object[]>() {
        private RecordElement element(String name) {
          var recordElement = componentMap.get(name);
          if (recordElement == null) {
            throw new Binder.BindingException("no element " + name + " for type " + type.getTypeName());
          }
          return recordElement;
        }

        @Override
        public Spec elementSpec(String memberName) {
          return element(memberName).spec;
        }

        @Override
        public Object[] newBuilder() {
          return new Object[length];
        }
        @Override
        public Object[] addObject(Object[] builder, String memberName, Object object) {
          builder[element(memberName).index] = object;
          return builder;
        }
        @Override
        public Object[] addArray(Object[] builder, String memberName, Object array) {
          builder[element(memberName).index] = array;
          return builder;
        }
        @Override
        public Object[] addValue(Object[] builder, String memberName, JsonValue value) {
          builder[element(memberName).index] = value.asObject();
          return builder;
        }
        @Override
        public Object build(Object[] builder) {
          try {
            return constructor.invokeWithArguments(builder);
          } catch(RuntimeException | Error e) {
            throw e;
          } catch (Throwable throwable) { // a record constructor can not throw a checked exception !
            throw new Binder.BindingException(throwable);
          }
        }

        @Override
        public void accept(Object object, MemberVisitor memberVisitor) {
          for(var accessor: accessors) {
            Object elementValue;
            try {
              elementValue = accessor.accessor.invoke(object);
            } catch(RuntimeException | Error e) {
              throw e;
            } catch (Throwable throwable) { // an accessor can not throw a checked exception
              throw new Binder.BindingException(throwable);
            }
            memberVisitor.visitMember(accessor.name, elementValue);
          }
        }
      }));
    };
  }

  static SpecFinder newAnyTypesAsStringFinder() {
    return type -> Optional.of(Spec.newTypedValue(type.getTypeName(), new Converter() {
      @Override
      public JsonValue convertTo(JsonValue value) {
        throw new Binder.BindingException("no default conversion");
      }

      @Override
      public JsonValue convertFrom(JsonValue value) {
        return JsonValue.from(value.toString());   // convert to string
      }
    }));
  }
}
