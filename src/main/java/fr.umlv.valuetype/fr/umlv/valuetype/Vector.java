package fr.umlv.valuetype;

import fr.umlv.valuetype.VectorImpl.VectorBig;
import fr.umlv.valuetype.VectorImpl.VectorInt1;
import fr.umlv.valuetype.VectorImpl.VectorInt2;
import fr.umlv.valuetype.VectorImpl.VectorInt3;
import fr.umlv.valuetype.VectorImpl.VectorInt4;

public interface Vector {
  @FunctionalInterface
  interface Op {
    int apply(int a, int b);
  }
  
  Vector apply(Vector vector, Op op);
  
  int length();
  int get(int index);
  
  default Vector add(Vector vector) {
    return apply(vector, (a, b) -> a + b);
  }
  default Vector subtract(Vector vector) {
    return apply(vector, (a, b) -> a - b);
  }
  default Vector multiply(Vector vector) {
    return apply(vector, (a, b) -> a * b);
  }
  default Vector divide(Vector vector) {
    return apply(vector, (a, b) -> a / b);
  }
  default Vector addExact(Vector vector) {
    return apply(vector, Math::addExact);
  }
  default Vector subtractExact(Vector vector) {
    return apply(vector, Math::subtractExact);
  }
  default Vector multiplyExact(Vector vector) {
    return apply(vector, Math::multiplyExact);
  }
  
  static Vector of(int v0) {
    return VectorInt1.of(v0);
  }
  static Vector of(int v0, int v1) {
    return VectorInt2.of(v0, v1);
  }
  static Vector of(int v0, int v1, int v2) {
    return VectorInt3.of(v0, v1, v2);
  }
  static Vector of(int v0, int v1, int v2, int v3) {
    return VectorInt4.of(v0, v1, v2, v3);
  }
  
  static Vector of(int... values) {
    return switch (values.length) {
      case 1 -> VectorInt1.of(values[0]);
      case 2 -> VectorInt2.of(values[0], values[1]);
      case 3 -> VectorInt3.of(values[0], values[1], values[2]);
      case 4 -> VectorInt4.of(values[0], values[1], values[2], values[3]);
      default -> VectorBig.of(values);
    };
  }
  
  static Vector wrap(int[] values) {
    return of(values);
  }
  
  static Vector zero(int length) {
    return switch (length) {
      case 1 -> VectorInt1.zero();
      case 2 -> VectorInt2.zero();
      case 3 -> VectorInt3.zero();
      case 4 -> VectorInt4.zero();
      default -> VectorBig.zero(length);
    };
  }
}
