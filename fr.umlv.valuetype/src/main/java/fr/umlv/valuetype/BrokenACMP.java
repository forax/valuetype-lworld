package fr.umlv.valuetype;

public class BrokenACMP {
  @__inline__ static class Box {
    private final Object box;
    private final String s;

    Box(String s, Object box) {
      this.s = s;
      this.box = box;
    }
  }

  public static void main(String[] args) {
    Object o = new Box("text", new Box("text2", null));
    Object o2 = new Box("text", new Box("text", null));

    boolean result;
    var start = System.currentTimeMillis();
    result = o == o2;
    var end =  System.currentTimeMillis();
    System.out.println(result + " " + (end - start) + " ms");
  }
}
