package fr.umlv.valuetype;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.IntFunction;

@__inline__
public /*inline*/ final class ValueList<E> {
  private final ArrayAccess<E> access;
  private final E[] array;
  
  public interface ArrayAccess<E> {
    E[] newArray(int capacity);
    E get(E[] array, int index);
    void set(E[] array, int index, E element);
    E[] copyOf(E[] array, int capacity);
    
    static <E> ArrayAccess<E> ofObject() {
      return ReferenceArrayAccess.create();
    }
  }
  
  @__inline__
  private static /*inline*/ final class ReferenceArrayAccess<E> implements ArrayAccess<E> {
    //private final boolean empty;  // fake field [not needed anymore]
    
    private ReferenceArrayAccess() {
      //this.empty = false;
    }
    
    static <T> ReferenceArrayAccess<T> create() {
      //return ReferenceArrayAccess<T>.default;
      return new ReferenceArrayAccess<>();
    }
    
    @Override
    public void set(E[] array, int index, E element) {
      array[index] = element;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public E[] newArray(int capacity) {
      return (E[])new Object[capacity];
    }
    
    @Override
    public E get(E[] array, int index) {
      return array[index];
    }
    
    @Override
    public E[] copyOf(E[] array, int capacity) {
      return Arrays.copyOf(array, capacity);
    }
  }
  
  private ValueList(ArrayAccess<E> access, E[] array) {
    this.access = access;
    this.array = array;
  }
  
  public static <T> ValueList<T> empty(ArrayAccess<T> access) {
    return create(access, access.newArray(0));
  }
  
  @SafeVarargs
  public static <T> ValueList<T> of(ArrayAccess<T> access, T... elements) {
    return create(access, access.copyOf(elements, elements.length));
  }
  
  private static <T> ValueList<T> create(ArrayAccess<T> access, T[] array) {
    return new ValueList<>(access, array);
  }
  
  public int size() {
    return array.length;
  }
  
  public E get(int index) {
    return access.get(array, index);
  }
  
  public ValueList<E> append(E element) {
    var newArray = access.copyOf(array, array.length + 1);
    newArray[array.length] = element;
    return create(access, newArray);
  }
  
  public void forEach(Consumer<? super E> consumer) {
    var length = array.length;
    for(var i = 0; i < length; i++) {
      consumer.accept(access.get(array, i));
    }
  }
  
  public <V> V reduce(V initial, BiFunction<? super V, ? super E, ? extends V> accumulate) {
    var v = initial;
    var length = array.length;
    for(var i = 0; i < length; i++) {
      v = accumulate.apply(v, access.get(array, i));
    }
    return v;
  }
  
  public static <T> ValueList<T> generate(ArrayAccess<T> access, int count, IntFunction<? extends T> generator) {
    var array = access.newArray(count);
    for(var i = 0; i < count; i++) {
      access.set(array, i, generator.apply(i));
    }
    return create(access, array);
  }
}

