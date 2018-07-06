package fr.umlv.valuetype;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class ReifiedList<E> implements Iterable<E> {
  private E[] array;
  private int size;
  
  @SuppressWarnings("unchecked")
  public ReifiedList(Class<E> type) {
    if (type.isPrimitive()) {
      throw new IllegalArgumentException("primitiva are not supported yet");
    }
    this.array = (E[])Array.newInstance(type, 0);
  }
  
  public int size() {
    return size;
  }
  
  public E get(int index) {
    return array[index];
  }
  
  public void add(E element) {
    var length = array.length;
    if (length == size) {
      array = Arrays.copyOf(array, (length == 0)? 1: length * 2);
    }
    array[size++] = element;
  }
  
  public interface Cursor<E> {
    E element();
    Cursor<E> next();
    
    default Iterator<E> iterator() {
      return new Iterator<>() {
        private Cursor<E> cursor = Cursor.this;
        
        @Override
        public boolean hasNext() {
          return cursor != null;
        }
        
        @Override
        public E next() {
          var element = cursor.element();
          cursor = cursor.next();
          return element;
        }
      };
    }
  }
  
  static final __ByValue class CursorImpl<E> implements Cursor<E> {
    private final E[] array;
    private final int size;
    private final int index;
    
    private CursorImpl() {
      array = null; index = size = 0;
      throw new AssertionError("fake constructor");
    }
    static <E> CursorImpl<E> create(E[] array, int size, int index) {
      var cursor = __MakeDefault CursorImpl<E>();
      cursor = __WithField(cursor.array, array);
      cursor = __WithField(cursor.size, size);
      cursor = __WithField(cursor.index, index);
      return cursor;
    }
    
    @Override
    public E element() {
      return array[index];
    }
    @Override
    public Cursor<E> next() {
      var nextIndex = index + 1;
      return (nextIndex == size)? null: CursorImpl.create(array, size, nextIndex);
    }
  }
  
  public Cursor<E> cursor() {
    return (size == 0)? null: CursorImpl.create(array, size, 0);
  }
  
  @Override
  public Iterator<E> iterator() {
    var size = this.size;
    var array = this.array;
    return new Iterator<>() {
      private int index;
      
      @Override
      public boolean hasNext() {
        return index < size;
      }
      @Override
      public E next() {
        try {
          return array[index++];
        } catch(ArrayIndexOutOfBoundsException e) {
          throw new NoSuchElementException();
        }
      }
    };
  }
  
  @Override
  public void forEach(Consumer<? super E> consumer) {
    var size = this.size;
    var array = this.array;
    for(int i = 0; i < size; i++) {
      consumer.accept(array[i]); 
    }
  }
  
  public static void main(String[] args) {
    /*
    var list = new ReifiedList<>(String.class);
    list.add("foo");
    list.add("bar");
    for(var s: list) {
      System.out.println(s);
    }
    */
    
    var list = new ReifiedList<>(IntBox.class);
    IntStream.range(0, 100_000).forEach(i -> list.add(IntBox.valueOf(i)));
    
    var sum = 0;
    for(var i: list) {
      sum += i.intValue();
    }
    System.out.println(sum);
  }
}
