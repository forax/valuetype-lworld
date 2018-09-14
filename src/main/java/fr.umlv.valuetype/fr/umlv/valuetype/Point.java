package fr.umlv.valuetype;

public final __ByValue class Point {
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
    Point p = Point.default;
    p = __WithField(p.x, x);
    p = __WithField(p.y, y);
    return p;
  }
  
  public static void main(String[] args) {
    var p = new Point(2, 3);
    System.out.println(p);
    System.out.println(p.hashCode());
    System.out.println(p.equals(Point.of(2, 3)));
  }
}
