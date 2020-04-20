package fr.umlv.valuetype;

import java.util.stream.IntStream;

public class Substituable {
  @__inline__
  private static final /*inline*/ class Link {
    private final int value;
    private final Object next;
    private final Object next2;
    
    private Link(int value, Object next) {
      this.value = value;
      this.next = next;
      this.next2 = next;
    }
    
    private static Object times(int count) {
      return IntStream.range(0, count).boxed().reduce(null, (acc, index) -> new Link(index, acc), (l1, l2) -> { throw null; });
    }
  }
  
  
  public static void main(String[] args) {
    var l = Link.times(100);
    
    System.out.println(l == l);
    //System.out.println(java.lang.invoke.ValueBootstrapMethods.isSubstitutable(l, l));
  }
}
