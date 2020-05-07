package fr.umlv.valuetype;

import static java.util.Objects.checkIndex;
import static java.util.Objects.requireNonNull;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

@__inline__
public class FourElementsArray<E> {
  private final E e0;
  private final E e1;
  private final E e2;
  private final E e3;
  
	private FourElementsArray(E e0, E e1, E e2, E e3) {
		this.e0 = e0;
		this.e1 = e1;
		this.e2 = e2;
		this.e3 = e3;
	}
  
  public int size() {
  	return 4;
  }
  
  @SuppressWarnings("unchecked")
	public E get(int index) {
  	checkIndex(index, 4);
  	if(OFFSETS == null) {
  		return (E)UNSAFE.getObject(this, BASE + index * STRIDE);
  	}
  	return (E)UNSAFE.getObject(this, OFFSETS[index]);
  }
  
  public static <E> FourElementsArray<E> of(E e0, E e1, E e2, E e3) {
  	requireNonNull(e0);
  	requireNonNull(e1);
  	requireNonNull(e2);
  	requireNonNull(e3);
  	return new FourElementsArray<>(e0, e1, e2, e3);
  }
  
  private static final Unsafe UNSAFE;
  private static final long[] OFFSETS;
  private static final long BASE, STRIDE;
  static {
  	Unsafe unsafe;
  	long[] offsets;
  	try {
  		Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
  		theUnsafe.setAccessible(true);
  		unsafe = (Unsafe) theUnsafe.get(null);
  		
  		offsets = new long[] {
  		  unsafe.objectFieldOffset(FourElementsArray.class.getDeclaredField("e0")),
  		  unsafe.objectFieldOffset(FourElementsArray.class.getDeclaredField("e1")),
  		  unsafe.objectFieldOffset(FourElementsArray.class.getDeclaredField("e2")),
  		  unsafe.objectFieldOffset(FourElementsArray.class.getDeclaredField("e3"))
  		};
		} catch (IllegalAccessException | NoSuchFieldException e) {
			throw new AssertionError(e);
		}
  	
  	// check if the offsets are linear
  	var base = 0L;
  	var stride = offsets[1] - offsets[0];
  	if (offsets[2] - offsets[1] == stride && offsets[3] - offsets[2] == stride) {
  		base = offsets[0];
  		offsets = null;
  	}
  	
  	OFFSETS = offsets;
  	BASE = base;
  	STRIDE = stride;
  	UNSAFE = unsafe;
  }
}
