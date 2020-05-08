package fr.umlv.valuetype;

/* generates a ClassFormatError
public @__inline__ record RecordPoint(int x, int y) {
  public boolean equals(Object o) {
    return this == o;
    //return o instanceof Point p && x == p.x && y == p.y;
  }

  public int hashCode() {
    return System.identityHashCode(this);
  }

  public String toString() {
    return "Point";
  }
}
*/