import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class InvokeBug {
  static value class Value {
    private final int value;
    
    public Value(int value) {
      this.value = value;
    }
  }
  
  public static void method(Value value) {
    // empty
  }
  
  public static void main(String[] args) throws Throwable {
    MethodHandle method = MethodHandles.lookup().findStatic(InvokeBug.class, "method", MethodType.methodType(void.class, Value.class));
    method.invokeExact(new Value(42));
  }
}
