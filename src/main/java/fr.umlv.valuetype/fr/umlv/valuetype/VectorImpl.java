package fr.umlv.valuetype;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.stream.IntStream.range;

import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.Objects;

class VectorImpl {
  static VarHandle[] accessors(Lookup lookup) {
    Class<?> declaringClass = lookup.lookupClass();
    String className = declaringClass.getName();
    int length = className.charAt(className.length() - 1) - '0';
    return range(0, length).mapToObj(i -> {
        try {
          return lookup.findVarHandle(declaringClass, "v" + i, int.class);
        } catch(NoSuchFieldException | IllegalAccessException e) {
          throw (NoSuchFieldError)new NoSuchFieldError().initCause(e);
        }
      }).toArray(VarHandle[]::new);
  }
  
  @__inline__
  static final /*inline*/ class VectorInt1 implements Vector {
    private final int v0;

    private VectorInt1(int v0) {
      this.v0 = v0;
    }

    static VectorInt1 zero() {
      //return VectorInt1.default;
      return new VectorInt1(0);
    }
    
    static VectorInt1 of(int v0) {
      return new VectorInt1(v0);
    }
    
    @Override
    public String toString() {
      return "[" + v0 + ']';
    }
    
    @Override
    public int length() {
      return 1;
    }
    @Override
    public int get(int index) {
      Objects.checkIndex(index, 1);
      return v0;
    }
    
    @Override
    public Vector apply(Vector vector, Op op) {
      VectorInt1 impl = (VectorInt1)vector;
      return VectorInt1.of(op.apply(this.v0, impl.v0));
    }
  }
  
  @__inline__
  static final /*inline*/ class VectorInt2 implements Vector {
    private final int v0;
    private final int v1;

    private static final VarHandle[] ACCESSORS = accessors(lookup());
    
    private VectorInt2(int v0, int v1) {
      this.v0 = v0;
      this.v1 = v1;
    }

    static VectorInt2 zero() {
      return new VectorInt2(0, 0);
    }
    
    public static VectorInt2 of(int v0, int v1) {
      return new VectorInt2(v0, v1);
    }
    
    @Override
    public String toString() {
      return "[" + v0 + ", " + v1 + ']';
    }
    
    @Override
    public int length() {
      return 2;
    }
    @Override
    public int get(int index) {
      return (int)ACCESSORS[index].get();
    }
    
    @Override
    public Vector apply(Vector vector, Op op) {
      VectorInt2 impl = (VectorInt2)vector;
      int[] a = { this.v0, this.v1 };
      int[] b = { impl.v0, impl.v1 };
      int[] result = { 0, 0 };
      for(int i = 0; i < 2; i++) {
        result[i] = op.apply(a[i], b[i]);
      }
      return VectorInt2.of(result[0], result[1]);
    }
  }
  
  @__inline__
  static final /*inline*/ class VectorInt3 implements Vector {
    private final int v0;
    private final int v1;
    private final int v2;
    
    private static final VarHandle[] ACCESSORS = accessors(lookup());
    
    private VectorInt3(int v0, int v1, int v2) {
      this.v0 = v0;
      this.v1 = v1;
      this.v2 = v2;
    }

    static VectorInt3 zero() {
      //return VectorInt3.default;
      return new VectorInt3(0, 0, 0);
    }
    
    public static VectorInt3 of(int v0, int v1, int v2) {
      return new VectorInt3(v0, v1, v2);
    }
    
    @Override
    public String toString() {
      return "[" + v0 + ", " + v1 + ", " + v2 + ']';
    }
    
    @Override
    public int length() {
      return 3;
    }
    @Override
    public int get(int index) {
      return (int)ACCESSORS[index].get();
    }
    
    @Override
    public Vector apply(Vector vector, Op op) {
      VectorInt3 impl = (VectorInt3)vector;
      int[] a = { this.v0, this.v1, this.v2 };
      int[] b = { impl.v0, impl.v1, impl.v2 };
      int[] result = { 0, 0, 0 };
      for(int i = 0; i < 3; i++) {
        result[i] = op.apply(a[i], b[i]);
      }
      return VectorInt3.of(result[0], result[1], result[2]);
    }
  }
  
  @__inline__
  static final /*inline*/ class VectorInt4 implements Vector {
    private final int v0;
    private final int v1;
    private final int v2;
    private final int v3;

    private static final VarHandle[] ACCESSORS = accessors(lookup());
    
    private VectorInt4(int v0, int v1, int v2, int v3) {
      this.v0 = v0;
      this.v1 = v1;
      this.v2 = v2;
      this.v3 = v3;
    }

    static VectorInt4 zero() {
      //return VectorInt4.default;
      return new VectorInt4(0, 0, 0, 0);
    }
    
    public static VectorInt4 of(int v0, int v1, int v2, int v3) {
      return new VectorInt4(v0, v1, v2, v3);
    }
    
    @Override
    public String toString() {
      return "[" + v0 + ", " + v1 + ", " + v2 + ", " + v3 + ']';
    }
    
    @Override
    public int length() {
      return 4;
    }
    @Override
    public int get(int index) {
      return (int)ACCESSORS[index].get();
    }
    
    @Override
    public Vector apply(Vector vector, Op op) {
      VectorInt4 impl = (VectorInt4)vector;
      int[] a = { this.v0, this.v1, this.v2, this.v3 };
      int[] b = { impl.v0, impl.v1, impl.v2, impl.v3 };
      int[] result = { 0, 0, 0, 0 };
      for(int i = 0; i < 4; i++) {
        result[i] = op.apply(a[i], b[i]);
      }
      return VectorInt4.of(result[0], result[1], result[2], result[3]);
    }
  }
  
  @__inline__
  static final /*inline*/ class VectorBig implements Vector {
    private final int[] values;
    
    private VectorBig(int[] values) {
      this.values = values;
    }

    static VectorBig zero(int length) {
      return of(new int[length]);
    }
    
    static VectorBig of(int[] values) {
      return new VectorBig(values);
    }
    
    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof VectorBig)) {
        return false;
      }
      VectorBig vector = (VectorBig)obj;
      return Arrays.equals(values, vector.values);
    }
    
    @Override
    public int hashCode() {
      return Arrays.hashCode(values);
    }
    
    @Override
    public String toString() {
      return Arrays.toString(values);
    }
    
    @Override
    public int length() {
      return values.length;
    }
    @Override
    public int get(int index) {
      return values[index];
    }
    
    @Override
    public Vector apply(Vector vector, Op op) {
      VectorBig impl = (VectorBig)vector;
      int length = this.values.length;
      if (impl.values.length != length) {
        throw new ArithmeticException("wrong length");
      }
      int[] newValues = new int[length];
      for(int i = 0; i < length; i++) {
        newValues[i] = op.apply(values[i], impl.values[i]);
      }
      return VectorBig.of(newValues);
    }
  }
}
