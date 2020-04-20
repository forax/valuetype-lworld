package fr.umlv.valuetype;

public class StackOverflowACMP {
  @FunctionalInterface
  interface Chain {
    String content();
  }

  private static Chain $lambda$Proxy(int value, Chain chain) {
    return new inline Chain() {
      @Override
      public String content() {
        return (chain == null)? "text": chain.content() + " -> " + value;
      }
    };
  }

  public static void main(String[] args) {
    Chain chain = null;
    Chain chain2 = null;
    /*for(var i = 0; i < 10_000_000; i++) {
      chain = $lambda$Proxy(i, chain);
      chain2 = $lambda$Proxy(i + 1, chain2);
    }*/
    chain = $lambda$Proxy(0, chain);
    chain2 = $lambda$Proxy(1, chain2);

    System.out.println(chain == chain2);
  }
}
