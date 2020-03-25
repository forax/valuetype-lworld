package fr.umlv.valuetype;

import static java.util.Arrays.copyOf;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.IntFunction;

public @__inline__ final class CompactList<E> implements Iterable<E> {
	private final E[] array;
	private final E embedded0;
  private final E embedded1;
  private final E embedded2;
  private final E embedded3;
  private final int size;
  
  private CompactList(E[] array, E embedded0, E embedded1, E embedded2, E embedded3, int size) {
		this.array = array;
		this.embedded0 = embedded0;
		this.embedded1 = embedded1;
		this.embedded2 = embedded2;
		this.embedded3 = embedded3;
		this.size = size;
	}

  public int size() {
  	return size;
  }
  
	public E get(int index) {
		Objects.checkIndex(index, size);
		if (size > 4) {
			return array[index];
		}
		if (index == 0) {
			return embedded0;
  	}
		if (index == 1) {
  		return embedded1;
  	}
		if (index == 2) {
  		return embedded2;
  	}
		//if (index == 3) {
  		return embedded3;
  	//}
  }
	
	@Override
	public String toString() {
		return range(0, size).mapToObj(this::get).map(Object::toString).collect(joining(", ", "[", "]"));
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CompactList)) {
			return false;
		}
		var list = (CompactList<?>) obj;
		if (size != list.size) {
			return false;
		}
		for(var i = 0; i < size; i++) {
			if (!get(i).equals(list.get(i))) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		var hashCode = 1;
    for (var i = 0; i < size; i++) {
      hashCode = 31 * hashCode + get(i).hashCode();
    }
    return hashCode;
	}
	
	@Override
	public Iterator<E> iterator() {
		var size = this.size;
		if (size <= 4) {
			return new Iterator<>() {
				private int index;
				
				@Override
				public boolean hasNext() {
					return index < size;
				}
				@Override
				public E next() {
					try {
					  return get(index++);
					} catch(@SuppressWarnings("unused") IndexOutOfBoundsException e) {
						throw (NoSuchElementException)new NoSuchElementException("no such element").initCause(e);
					}
				}
			};
		}
		var array = this.array;
		return new Iterator<>() {
			private int index;
			
			@Override
			public boolean hasNext() {
				return index < array.length;
			}
			@Override
			public E next() {
				try {
					return array[index++];
				} catch(@SuppressWarnings("unused") ArrayIndexOutOfBoundsException e) {
					throw (NoSuchElementException)new NoSuchElementException("no such element").initCause(e);
				}
			}
		};
	}
	
	@SuppressWarnings("unchecked")
	public CompactList<E> append(E element) {
		requireNonNull(element);
		return switch(size) {
		  case 0 -> of(element);
		  case 1 -> of(embedded0, element);
		  case 2 -> of(embedded0, embedded1, element);
		  case 3 -> of(embedded0, embedded1, embedded2, element);
		  case 4 -> of((E[])new Object[] { embedded0, embedded1, embedded2, embedded3, element});
	    default -> {
	  	  var newLength = array.length + 1;
	  	  var newArray = copyOf(array, newLength);
	  	  newArray[array.length] = element;
	  	  yield new CompactList<>(newArray, null, null, null, null, newLength);
	    }
		};
	}
  
	@SuppressWarnings({"unchecked", "fallthrough"})
	public <T> T[] toArray(IntFunction<? extends T[]> arrayCreator) {
		var size = this.size;
		T[] array = arrayCreator.apply(size);  // implicit NPE
		switch(size) {
		case 0:
			return array;
		case 4:
			array[3] = (T)embedded3;
		case 3:
			array[2] = (T)embedded2;
		case 2:
			array[1] = (T)embedded1;
		case 1:
			array[0] = (T)embedded0;
			return array;
		default:
			System.arraycopy(this.array, 0, array, 0, size);
			return array;
		}
	}
	
  public static <E> CompactList<E> of() {
  	return new CompactList<>(null, null, null, null, null, 0);
  }
  public static <E> CompactList<E> of(E e1) {
  	requireNonNull(e1);
  	return new CompactList<>(null, e1, null, null, null, 1);
  }
  public static <E> CompactList<E> of(E e1, E e2) {
  	requireNonNull(e1);
  	requireNonNull(e2);
  	return new CompactList<>(null, e1, e2, null, null, 2);
  }
  public static <E> CompactList<E> of(E e1, E e2, E e3) {
  	requireNonNull(e1);
  	requireNonNull(e2);
  	requireNonNull(e3);
  	return new CompactList<>(null, e1, e2, e3, null, 3);
  }
  public static <E> CompactList<E> of(E e1, E e2, E e3, E e4) {
  	requireNonNull(e1);
  	requireNonNull(e2);
  	requireNonNull(e3);
  	requireNonNull(e4);
  	return new CompactList<>(null, e1, e2, e3, e4, 4);
  }
  @SafeVarargs
  public static <E> CompactList<E> of(E... elements) {
		return switch (elements.length) {
			case 0 -> of();
			case 1 -> of(elements[0]);
			case 2 -> of(elements[0], elements[1]);
			case 3 -> of(elements[0], elements[1], elements[2]);
			case 4 -> of(elements[0], elements[1], elements[2], elements[3]);
			default -> ofArray(elements);
		};
  }
  private static <E> CompactList<E> ofArray(E[] elements) {
  	var length = elements.length;
		for (var element : elements) {
			requireNonNull(element);
		}
  	return new CompactList<>(copyOf(elements, length), null, null, null, null, length);
  }
}
