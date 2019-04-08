package fr.umlv.valuetype;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class ReifiedList<E> implements Iterable<E> {
  private E[] array;
  private int size;
  
  @SuppressWarnings("unchecked")
  public ReifiedList(Class<E> type) {
    if (type.isPrimitive()) {
      throw new IllegalArgumentException("primitive are not supported yet");
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
  
  @__value__
  static final /*value*/ class CursorImpl<E> implements Cursor<E> {
    private final E[] array;
    private final int size;
    private final int index;
    
    private CursorImpl(E[] array, int size, int index) {
      this.array = array;
      this.size = size;
      this.index = index;
    }
    static <E> CursorImpl<E> create(E[] array, int size, int index) {
      return new CursorImpl<>(array, size, index);
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
          throw (NoSuchElementException)new NoSuchElementException().initCause(e);
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
