import java.util.Objects;

public final value class NullVT {
  private final boolean value;
  
  private NullVT() {
    value = false;
    throw new AssertionError();
  }

  public static NullVT of() {
    return __MakeDefault NullVT();
  }
  
  public static void foo(NullVT vt) {
    Objects.requireNonNull(vt);
  }
}
