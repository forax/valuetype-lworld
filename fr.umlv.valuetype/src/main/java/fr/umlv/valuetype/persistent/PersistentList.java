package fr.umlv.valuetype.persistent;

import static java.lang.Math.max;
import static java.lang.Thread.currentThread;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.IntStream.range;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntFunction;

@__inline__
public final class PersistentList<T> implements List<T> {
  private static final int DEFAULT_SIZE = 16;

  private final Thread ownerThread;
  private final int size;
  private final T[] array;

  private PersistentList(Thread ownerThread, T[] array, int size) {
    this.ownerThread = ownerThread;
    this.array = array;
    this.size = size;
  }

  public static <T> PersistentList<T> from(IntFunction<? extends T[]> arrayCreator) {
    var array = arrayCreator.apply(0);
    if (array.length != 0) {
      throw new IllegalArgumentException("array creator not implemented correctly");
    }
    return new PersistentList<>(currentThread(), array, 0);
  }

  public static <T> PersistentList<T> generate(
      IntFunction<? extends T[]> arrayCreator, int size, IntFunction<? extends T> generator) {
    var array = arrayCreator.apply(0);
    if (array.length != 0) {
      throw new IllegalArgumentException("array creator not implemented correctly");
    }
    var newArray = Arrays.copyOf(array, size);
    Arrays.setAll(newArray, generator);
    return new PersistentList<>(currentThread(), newArray, size);
  }

  public static <T> PersistentList<T> of(T element) {
    requireNonNull(element);
    @SuppressWarnings("unchecked")
    var array = (T[]) Array.newInstance(element.getClass(), 1);
    array[0] = element;
    return new PersistentList<>(currentThread(), array, 1);
  }

  @SafeVarargs
  public static <T> PersistentList<T> of(T... elements) {
    var array = Arrays.copyOf(elements, elements.length);
    for(var element: array) {
      requireNonNull(element);
    }
    return new PersistentList<>(currentThread(), array, array.length);
  }

  private static void checkOwnerThread(Thread ownerThread) {
    if (currentThread() != ownerThread) {
      throw new IllegalStateException("invalid owner thread");
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof  PersistentList) {
      var list = (PersistentList<?>) obj;
      if (size != list.size) {
        return false;
      }
      for(var i = 0; i < size; i++) {
        if (!array[i].equals(list.array[i])) {
          return false;
        }
      }
      return true;
    }
    if (obj instanceof List) {
      return equalsList(obj);
    }
    return false;
  }

  private boolean equalsList(Object o) {
    var iterator = ((List<?>) o).iterator();
    return Arrays.stream(array, 0, size).allMatch(e -> e.equals(iterator.next()));
  }

  @Override
  public int hashCode() {
    var size = this.size;
    var array = this.array;
    var hashCode = 1;
    for (var i = 0; i < size; i++) {
      hashCode = 31 * hashCode + array[i].hashCode();
    }
    return hashCode;
  }

  @Override
  public String toString() {
    return stream().map(Object::toString).collect(joining(", ", "[", "]"));
  }

  public PersistentList<T> append(T element) {
    requireNonNull(element);
    checkOwnerThread(ownerThread);
    var array = this.array;
    var length = array.length;
    var size = this.size;
    if (size == length) {
      return resize(array, length, element);
    }
    if (array[size] != null) {
      return rearrange(array, size, element);
    }
    array[size] = element;
    return new PersistentList<>(ownerThread, array, size + 1);
  }

  private PersistentList<T> resize(T[] array, int length, T element) {
    var newSize = max(DEFAULT_SIZE, length << 1);
    var newArray = Arrays.copyOf(array, newSize);
    newArray[length] = element;
    return new PersistentList<>(ownerThread, newArray, length + 1);
  }

  private PersistentList<T> rearrange(T[] array, int size, T element) {
    @SuppressWarnings("unchecked")
    var newArray = (T[]) Array.newInstance(array.getClass().getComponentType(), array.length);
    System.arraycopy(array, 0, newArray, 0, size);
    newArray[size] = element;
    return new PersistentList<>(ownerThread, newArray, size + 1);
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public boolean contains(Object o) {
    requireNonNull(o);
    return Arrays.stream(array, 0, size).anyMatch(e -> e.equals(o));
  }

  public Cursor<T> cursor() {
    return new ListCursor<>(array, 0, size);
  }

  private static class ListCursor<T> implements Cursor<T> {
    private final T[] array;
    private final int index;
    private final int size;


    private ListCursor(T[] array, int index, int size) {
      this.array = array;
      this.index = index;
      this.size = size;
    }

    @Override
    public boolean hasNext() {
      return index < size;
    }

    @Override
    public T element() {
      if (index == size) {
        throw new NoSuchElementException();
      }
      return array[index];
    }

    @Override
    public Cursor<T> next() {
      if (index == size) {
        throw new IllegalStateException();
      }
      return new ListCursor<>(array, index + 1, size);
    }
  }

  @Override
  public Iterator<T> iterator() {
    return new PersistentListIterator<>(size, array);
  }

  private static final class PersistentListIterator<T> implements Iterator<T> {
    private final T[] array;
    private int index;
    private final int size;

    private PersistentListIterator(int size, T[] array) {
      this.size = size;
      this.array = array;
    }

    @Override
    public boolean hasNext() {
      return index < size;
    }

    @Override
    public T next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return array[index++];
    }

    @Override
    public void forEachRemaining(Consumer<? super T> action) {
      var array = this.array;
      var index = this.index;
      var size = this.size;
      for(;index < size; index++) {
        action.accept(array[index]);
      }
      this.index = index;
    }
  }

  @Override
  public Object[] toArray() {
    var newArray = new Object[size];
    System.arraycopy(array, 0, newArray, 0, size);
    return newArray;
  }

  @Override
  public <E> E[] toArray(E[] a) {
    requireNonNull(a);
    if (a.length < size) {
      @SuppressWarnings("unchecked")
      var newArray = (E[]) Array.newInstance(a.getClass().getComponentType(), size);
      System.arraycopy(array, 0, newArray, 0, size);
      return newArray;
    }
    System.arraycopy(array, 0, a, 0, size);
    if (a.length > size) {
      a[size] = null;
    }
    return a;
  }

  @Override
  public boolean add(T t) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    requireNonNull(c);
    return Arrays.stream(array, 0, size)
        .collect(toCollection(() -> new HashSet<>(size)))
        .containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends T> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(int index, Collection<? extends T> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public T get(int index) {
    return array[index];
  }

  @Override
  public T set(int index, T element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(int index, T element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public T remove(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int indexOf(Object o) {
    requireNonNull(o);
    return range(0, size).filter(i -> array[i].equals(o)).findFirst().orElse(-1);
  }

  @Override
  public int lastIndexOf(Object o) {
    requireNonNull(o);
    var last = this.size - 1;
    return range(0, size).map(i -> last - i).filter(i -> array[i].equals(o)).findFirst().orElse(-1);
  }

  @Override
  public ListIterator<T> listIterator() {
    return listIterator(0);
  }

  @Override
  public ListIterator<T> listIterator(int index) {
    return new PersistentListListIterator<>(array, index, size);
  }

  @Override
  public List<T> subList(int fromIndex, int toIndex) {
    Objects.checkFromToIndex(fromIndex, toIndex, size);
    return List.copyOf(Arrays.asList(array)).subList(fromIndex, toIndex);
  }

  private static final class PersistentListListIterator<T> implements ListIterator<T> {
    private final T[] array;
    private int index;
    private final int size;

    private PersistentListListIterator(T[] array, int index, int size) {
      this.array = array;
      this.index = index;
      this.size = size;
    }

    @Override
    public boolean hasNext() {
      return index < size;
    }

    @Override
    public T next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return array[index++];
    }

    @Override
    public boolean hasPrevious() {
      return index > 0;
    }

    @Override
    public T previous() {
      if (!hasPrevious()) {
        throw new NoSuchElementException();
      }
      return array[--index];
    }

    @Override
    public int nextIndex() {
      return index;
    }

    @Override
    public int previousIndex() {
      return index - 1;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void set(T t) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void add(T t) {
      throw new UnsupportedOperationException();
    }
  }
}
