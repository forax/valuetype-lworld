package fr.umlv.valuetype;

@__inline__
public /*inline*/ class Pair<A, B> {
  private final A first;
  private final B second;
  
  private Pair(A first, B second) {
    this.first = first;
    this.second = second;
  }
  
  public static <A, B> Pair<A, B> of(A first, B second) {
    return new Pair<>(first, second);
  }
}
