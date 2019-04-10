package fr.umlv.valuetype;

import java.util.stream.IntStream;

public @__value__ class SecurityToken {
  private final long id1;
  private final long id2;
  
  public SecurityToken(long id1, long id2) {
    if (id1 != id2) {
      throw new IllegalArgumentException();
    }
    this.id1 = id1;
    this.id2 = id2;
  }
  
  public void check() {
    if (id1 != id2) {
      throw new IllegalStateException();
    }
  }
  
  public static void main(String[] args) {
    var tokens = new SecurityToken[1];
    IntStream.range(0, 2).forEach(id -> {
      new Thread(() -> {
        for(;;) {
          tokens[0].check();
          tokens[0] = new SecurityToken(id, id);
        }
      }).start();
    });
  }
}
