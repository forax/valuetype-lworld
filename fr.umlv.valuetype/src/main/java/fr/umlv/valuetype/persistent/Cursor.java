package fr.umlv.valuetype.persistent;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;

public interface Cursor<E> {
  boolean hasNext();
  E element();
  Cursor<E> next();

  static <E> Cursor<E> from(Iterator<? extends E> iterator) {
    @__inline__
    final class CursorImpl implements Cursor<E> {
      private final E element;

      private CursorImpl(E element) {
        this.element = Objects.requireNonNull(element);
      }

      @Override
      public boolean hasNext() {
        return element != null;
      }

      @Override
      public E element() {
        if (element == null) {
          throw new NoSuchElementException();
        }
        return element;
      }

      @Override
      public Cursor<E> next() {
        if (element == null) {
          throw new NoSuchElementException();
        }
        if (iterator.hasNext()) {
          return new CursorImpl(iterator.next());
        }
        return CursorImpl.default;
      }
    }

    if (iterator.hasNext()) {
      return new CursorImpl(iterator.next());
    }
    return CursorImpl.default;
  }

  default Iterator<E> iteratorRemaining() {
    return new Iterator<>() {
      private Cursor<E> cursor = Cursor.this;

      @Override
      public boolean hasNext() {
        return cursor.hasNext();
      }

      @Override
      public E next() {
        if (!cursor.hasNext()) {
          throw new NoSuchElementException();
        }
        var element = cursor.element();
        cursor = cursor.next();
        return element;
      }

      @Override
      public void forEachRemaining(Consumer<? super E> action) {
        var cursor = this.cursor;
        for(; cursor.hasNext(); cursor = cursor.next()) {
          action.accept(cursor.element());
        }
        this.cursor = cursor;
      }
    };
  }
}
