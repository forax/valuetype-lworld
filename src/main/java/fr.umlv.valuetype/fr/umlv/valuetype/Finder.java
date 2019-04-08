package fr.umlv.valuetype;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

public interface Finder<E extends Comparable<? super E>> {
  boolean find(E element);
  
  public static <E extends Comparable<? super E>> Finder<E> empty() {
    return __ -> false;
  }
  
  public static <E extends Comparable<? super E>> Finder<E> of(E element, Finder<? super E> left, Finder<? super E> right) {
    //return element::equals;
    return new value Finder<>() {
      public boolean find(E e) {
        if (e.equals(element)) {
          return true;
        }
        Finder<? super E> finder = (e.compareTo(element) < 0)? left: right;
        return finder.find(e);
      }
    };
  }
  
  public static void main(String[] args) {
    var finder = of("c",
        of("a", empty(), empty()),
        of("e", empty(), empty()));
    
  }
}
