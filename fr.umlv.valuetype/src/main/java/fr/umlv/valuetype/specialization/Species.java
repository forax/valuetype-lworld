package fr.umlv.valuetype.specialization;

import static java.lang.invoke.MethodType.methodType;
import static java.util.Collections.reverse;
import static java.util.stream.Collectors.toList;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Species<T> {
  public Species<T> raw();
  public Class<?> rawClass();
  public int getTypeArgumentCount();
  public <I> Species<I> getTypeArgument(int index);
  public <I> Species<I> with(Species<?>... typeArguments);
  
  public T[] newArray(int length);
  public void checkCast(T e);
  
  static final ClassValue<MethodHandle> SPECIES_GETTER = new ClassValue<>() {
    @Override
    protected MethodHandle computeValue(Class<?> type) {
      Lookup lookup;
      try { // FIXME
        lookup = MethodHandles.privateLookupIn(type, MethodHandles.lookup());
      } catch (IllegalAccessException e) {
        throw (IllegalAccessError)new IllegalAccessError().initCause(e);
      }
      
      try {
        return lookup.findGetter(type, "__SPECIES__", Species.class).asType(methodType(Species.class, Object.class));
      } catch (@SuppressWarnings("unused") NoSuchFieldException e) {
        return null;
      } catch (IllegalAccessException e) {
        throw (IllegalAccessError)new IllegalAccessError().initCause(e);
      }
    }
  };
  
  @SuppressWarnings("unchecked")
  public static <T> Species<T> species(T instance) {
    var clazz = instance.getClass();
    var getter = SPECIES_GETTER.get(clazz);
    if (getter == null) {
      return (Species<T>)raw(clazz);
    }
    try {
      return (Species<T>)getter.invokeExact(instance);
    } catch (Throwable e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException)e;
      }
      if (e instanceof Error) {
        throw (Error)e;
      }
      throw new UndeclaredThrowableException(e);
    }
  }
  
  static final ClassValue<Species<?>> SPECIES = new ClassValue<>() {
    @Override
    protected Species<?> computeValue(Class<?> type) {
      var typeVariables = getTypeVariables(type);
      //FIXME
      var bounds = typeVariables.stream().map(t -> SPECIES.get((Class<?>)t.getBounds()[0])).toArray(Species<?>[]::new);
      
      return new Species<>() {
        private final HashMap<List<Species<?>>, Species<?>> specializationMap = new HashMap<>();
        
        @Override
        public Species<Object> raw() {
          return this;
        }
        @Override
        public Class<?> rawClass() {
          return type;
        }
        @Override
        public int getTypeArgumentCount() {
          return bounds.length;
        }
        @Override
        @SuppressWarnings("unchecked")
        public <I> Species<I> getTypeArgument(int index) {
          return (Species<I>)bounds[index];
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <I> Species<I> with(Species<?>... typeArguments) {
          return (Species<I>)specializationMap.computeIfAbsent(Arrays.asList(typeArguments), __ -> genericOf(this, typeArguments));
        }
        
        @Override
        public Object[] newArray(int length) {
          return (Object[])Array.newInstance(type, length);
        }
        @Override
        public void checkCast(Object e) {
          type.cast(e);
        }
        
        @Override
        public String toString() {
          return type.getName();
        }
      };
    }
  };
  
  private static List<TypeVariable<?>> getTypeVariables(Class<?> type) {
    List<TypeVariable<?>> typeVariables = Stream.<Class<?>>
    iterate(type, t -> t != null, t -> Modifier.isStatic(t.getModifiers())? null: t.getEnclosingClass())
        .flatMap(t -> Arrays.stream(t.getTypeParameters()))
        .collect(toList());
    reverse(typeVariables);
    return typeVariables;
  }
  
  @SuppressWarnings("unchecked")
  public static <T> Species<T> raw(Class<T> type) {
    if (type.isPrimitive()) {
      throw new IllegalArgumentException("type can not be a primitive");
    }
    return (Species<T>)SPECIES.get(type);
  }
  
  private static <I> Species<I> genericOf(Species<I> raw, Species<?>[] typeArguments) {
    if (raw.getTypeArgumentCount() != typeArguments.length) {
      throw new IllegalArgumentException("wrong number of type arguments " + raw + " " + Arrays.toString(typeArguments));
    }
    return new Species<>() {
      @Override
      public Species<I> raw() {
        return raw;
      }
      @Override
      public Class<?> rawClass() {
        return raw.rawClass();
      }
      @Override
      public int getTypeArgumentCount() {
        return typeArguments.length;
      }
      @Override
      @SuppressWarnings("unchecked")
      public <J> Species<J> getTypeArgument(int index) {
        return (Species<J>)typeArguments[index];
      }
      
      @Override
      public <J> Species<J> with(Species<?>... typeArguments) {
        return raw.with(typeArguments);
      }
      
      @Override
      public I[] newArray(int length) {
        return raw.newArray(length);
      }
      @Override
      public void checkCast(I instance) {
        var species = species(instance);
        if (species == this) {  // quickcheck
          return;
        }
        raw.checkCast(instance);  // raw check
        for(var i = 0; i < typeArguments.length; i++) {  // type argument checks
          if (!typeArguments[i].rawClass().isAssignableFrom(species.getTypeArgument(i).rawClass())) {
            throw new ClassCastException("can not cast " + species + " to " + this);
          }
        }
      }
      
      @Override
      public String toString() {
        return raw.toString() + Arrays.stream(typeArguments).map(Species::toString).collect(Collectors.joining(", ", "<", ">"));
      }
    };
  }
}
