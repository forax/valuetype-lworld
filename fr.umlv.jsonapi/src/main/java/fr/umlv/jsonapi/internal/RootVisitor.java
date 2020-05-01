package fr.umlv.jsonapi.internal;

import fr.umlv.jsonapi.ArrayVisitor;
import fr.umlv.jsonapi.ObjectVisitor;

public class RootVisitor {
  public static final int OBJECT = 1;
  public static final int ARRAY = 2;
  public static final int BOTH = 3;

  private int kind;
  private final ObjectVisitor objectVisitor;
  private final ArrayVisitor arrayVisitor;

  public RootVisitor(int kind, ObjectVisitor objectVisitor, ArrayVisitor arrayVisitor) {
    this.kind = kind;
    this.objectVisitor = objectVisitor;
    this.arrayVisitor = arrayVisitor;
  }

  public static RootVisitor createFromOneVisitor(Object visitor) {
    if (visitor instanceof ObjectVisitor objectVisitor) {
      return new RootVisitor(OBJECT, objectVisitor, null);
    }
    if (visitor instanceof ArrayVisitor arrayVisitor) {
      return new RootVisitor(ARRAY, null, arrayVisitor);
    }
    throw new IllegalArgumentException("unknown visitor type " + visitor);
  }

  public ObjectVisitor visitObject() {
    if ((kind & OBJECT) == 0) {
      throw new IllegalStateException("root is an object but expect an array");
    }
    return objectVisitor;
  }
  public ArrayVisitor visitArray() {
    if ((kind & ARRAY) == 0) {
      throw new IllegalStateException("root is an array but expect an object");
    }
    return arrayVisitor;
  }
}
