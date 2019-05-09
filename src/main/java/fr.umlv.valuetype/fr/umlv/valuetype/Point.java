package fr.umlv.valuetype;

@__inline__
public final /*inline*/ class Point {
  public final int x;
  public final int y;

  public Point() {
    this(0, 0);
  }
  
  public Point(int x, int y) {
    this.x = x;
    this.y = y;
  }
  
  public static Point of(int x, int y) {
    return new Point(x, y);
  }
  
  public static void main(String[] args) {
    var p = new Point(2, 3);
    System.out.println(p);
    System.out.println(p.hashCode());
    System.out.println(p.equals(Point.of(2, 3)));
  }
}
