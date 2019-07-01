package fr.umlv.valuetype.specialization;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;

import fr.umlv.valuetype.Point;

public class FastList<T> extends AbstractList<T> {
  private final Species<FastList<T>> __SPECIES__;
  private T[] elements;
  private int size;
  
  public FastList(Species<T> _T) {
    this.__SPECIES__ = Species.raw(FastList.class).with(_T);
    elements = _T.newArray(16);
  }

  @Override
  public boolean add(T e) {
    __SPECIES__.getTypeArgument(0).checkCast(e);
    if (size == elements.length) {
      elements = Arrays.copyOf(elements, 2 * size);
    }
    elements[size++] = e;
    return true;
  }
  
  @Override
  public T get(int index) {
    return elements[index];
  }

  @Override
  public int size() {
    return size;
  }
  
  public static void main(String[] args) {
    var list = new FastList<>(Species.raw(Point.class.asPrimaryType()));
    list.add(new Point(1, 1));
    
    @SuppressWarnings("unchecked")
    var list2 = (FastList<Object>)(FastList<?>)list;
    list2.add(new Point(2, 2));
    //list2.add("oops");
    
    list2.forEach(System.out::println);
  }
}
