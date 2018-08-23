
public __ByValue class LambdaDesugar {
  private final int value;
  public LambdaDesugar() { this.value = 0; }
  
  public static void main(String[] args) {
    Runnable r = () -> {
      System.out.println("hello lambda inside a value type");
    };
    r.run();
  }
}
