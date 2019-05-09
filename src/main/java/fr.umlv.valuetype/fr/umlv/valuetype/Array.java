package fr.umlv.valuetype;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

@__inline__
public final /*inline*/ class Array<E> implements List<E> {
  private final E[] elements;

  private Array(E[] elements) {
    this.elements = elements;
  }

  public static <E> Array<E> wrap(E[] elements) {
    return new Array<>(Objects.requireNonNull(elements));
  }

  @SafeVarargs
  public static <E> Array<E> of(E... elements) {
    return wrap(elements);
  }

  public int length() {
    return elements.length;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Array) {
      Array<?> array = (Array<?>)obj;
      return Arrays.equals(elements, array.elements);
    }
    if (obj instanceof List) {
      return equalsList((List<?>)obj);
    }
    return false;
  }
  private boolean equalsList(List<?> list) {
    Iterator<?> it = list.iterator();
    for(var element: elements) {
      if (!it.hasNext()) {
        return false;
      }
      Object other = it.next();
      if (!Objects.equals(element, other)) {
        return false;
      }
    }
    return !it.hasNext();
  }
  
  @Override
  public int hashCode() {
    return Arrays.hashCode(elements);
  }
  
  @Override
  public String toString() {
    return Arrays.toString(elements);
  }

  @Override
  public int size() {
    return elements.length;
  }

  @Override
  public boolean isEmpty() {
    return elements.length == 0;
  }

  @Override
  public E get(int index) {
    return elements[index];
  }
  
  @Override
  public E set(int index, E element) {
    var old = elements[index];
    elements[index] = element;
    return old;
  }
  
  @Override
  public int indexOf(Object o) {
    int length = elements.length;
    for(int i = 0; i < length; i++) {
      if (Objects.equals(o, elements[i])) {
        return i;
      }
    }
    return -1;
  }
  
  @Override
  public int lastIndexOf(Object o) {
    Objects.requireNonNull(o);
    for(int i = elements.length; --i >= 0;) {
      if (Objects.equals(o, elements[i])) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public boolean contains(Object o) {
    Objects.requireNonNull(o);
    for(var element: elements) {
      if (Objects.equals(o, element)) {
        return true;
      }
    }
    return false;
  }
  
  @Override
  public boolean containsAll(Collection<?> collection) {
    for (var other: collection) {
      if (!contains(other)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Iterator<E> iterator() {
    return new Iterator<>() {
      private int index;
      
      @Override
      public boolean hasNext() {
        return index < elements.length;
      }
      @Override
      public E next() {
        try {
          return elements[index++];
        } catch(ArrayIndexOutOfBoundsException e) {
          throw new NoSuchElementException();
        }
      }
    };
  }
  
  @Override
  public ListIterator<E> listIterator() {
    return listIterator(0);
  }
  @Override
  public ListIterator<E> listIterator(int index) {
    return Arrays.asList(elements).listIterator(index);
  }
  @Override
  public List<E> subList(int fromIndex, int toIndex) {
    return Arrays.asList(elements).subList(fromIndex, toIndex);
  }
  
  @Override
  public Object[] toArray() {
    return Arrays.copyOf(elements, elements.length, Object[].class);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T[] toArray(T[] a) {
    E[] elements = this.elements;
    int length = elements.length;
    if (a.length < length) {
      return (T[])Arrays.copyOf(elements, length, a.getClass());
    }
    System.arraycopy(elements, 0, a, 0, length);
    if (a.length > length) {
      a[length] = null;
    }
    return a;
  }

  @Override
  public boolean add(E e) {
    throw new UnsupportedOperationException("operation not supported");
  }
  @Override
  public void add(int index, E element) {
    throw new UnsupportedOperationException("operation not supported");
  }
  @Override
  public boolean addAll(Collection<? extends E> c) {
    throw new UnsupportedOperationException("operation not supported");
  }
  @Override
  public boolean addAll(int index, Collection<? extends E> c) {
    throw new UnsupportedOperationException("operation not supported");
  }
  @Override
  public void clear() {
    throw new UnsupportedOperationException("operation not supported");
  }
  @Override
  public E remove(int index) {
    throw new UnsupportedOperationException("operation not supported");
  }
  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException("operation not supported");
  }
  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException("operation not supported");
  }
  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException("operation not supported");
  }
  @Override
  public boolean removeIf(Predicate<? super E> filter) {
    throw new UnsupportedOperationException("operation not supported");
  }
  
  @Override
  public void forEach(Consumer<? super E> action) {
    for(var element: elements) {
      action.accept(element);
    }
  }
  
  @Override
  public void replaceAll(UnaryOperator<E> operator) {
    for(int i = 0; i < elements.length; i++) {
      elements[i] = operator.apply(elements[i]);
    }
  }
  
  @Override
  public void sort(Comparator<? super E> comparator) {
    Arrays.sort(elements, comparator);
  }
  
  @Override
  public Spliterator<E> spliterator() {
    return Spliterators.spliterator(elements, Spliterator.ORDERED);
  }
}
