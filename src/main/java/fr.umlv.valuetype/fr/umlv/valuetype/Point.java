package fr.umlv.valuetype;

public final __ByValue class Point {
  public final int x;
  public final int y;

  public Point() {
    this.x = 0;
    this.y = 0;
    throw new AssertionError();
  }
  
  public static Point of(int x, int y) {
    Point p = __MakeDefault Point();
    p = __WithField(p.x, x);
    p = __WithField(p.y, y);
    return p;
  }
  
  public static void main(String[] args) {
    Point p = Point.of(2, 3);
    System.out.println(p);
    System.out.println(p.hashCode());
    System.out.println(p.equals(Point.of(2, 3)));
  }
}
