package fr.umlv.valuetype;

@__inline__
public /*inline*/ class Outer {
  private final int value;
  
  public Outer(int value) {
    this.value = value;
  }
  
  @__inline__
  public /*inline*/ class Inner {
    private final int value2;
    
    public Inner(int value2) {
      this.value2 = value2;
    }
  }
  
  public static void main(String[] args) {
    var outer = new Outer(3);
    var inner = outer.new Inner(4);
    System.out.println(inner);
  }
}
